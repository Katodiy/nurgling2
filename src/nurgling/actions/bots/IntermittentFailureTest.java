package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.navigation.ChunkNavManager;
import nurgling.navigation.ChunkPath;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Focused stress test for areas with intermittent navigation failures.
 *
 * This bot reads a previous navtest results file to identify areas that
 * sometimes succeed and sometimes fail (intermittent failures), then
 * repeatedly tests navigation to those areas.
 *
 * When selecting the next target, it picks the area furthest from the
 * current position to maximize path complexity and exercise more of
 * the navigation system.
 */
public class IntermittentFailureTest implements Action {

    // Configuration
    private static final long TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
    private static final long DELAY_BETWEEN_TESTS_MS = 3000;
    private static final int SAVE_INTERVAL = 5;

    // Failure rate thresholds for "intermittent" (between these values)
    private static final double MIN_FAIL_RATE = 5.0;   // At least 5% failures
    private static final double MAX_FAIL_RATE = 95.0;  // At most 95% failures
    private static final int MIN_ATTEMPTS = 2;         // Need at least 2 attempts

    // State
    private final List<TestResult> results = new ArrayList<>();
    private String outputFilePath;
    private long runStartTime;
    private int totalTests = 0;
    private int passed = 0;
    private int failed = 0;
    private Set<String> targetAreaNames = new HashSet<>();
    private Map<Integer, NArea> allAreas;
    private List<NArea> targetAreas = new ArrayList<>();

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        runStartTime = System.currentTimeMillis();
        outputFilePath = generateOutputFilePath();

        gui.msg("Intermittent Failure Test starting...");

        // Find and parse previous test results
        Path previousResults = findLatestNavtestFile();
        if (previousResults == null) {
            return Results.ERROR("No previous navtest_*.json found. Run NavigationStressTest first.");
        }

        gui.msg("Reading previous results from: " + previousResults.getFileName());

        // Extract intermittent failure areas
        targetAreaNames = extractIntermittentFailures(previousResults);
        if (targetAreaNames.isEmpty()) {
            return Results.ERROR("No intermittent failures found in previous results.");
        }

        gui.msg("Found " + targetAreaNames.size() + " areas with intermittent failures");

        // Get all areas and filter to our targets
        allAreas = gui.map.glob.map.areas;
        if (allAreas == null || allAreas.isEmpty()) {
            return Results.ERROR("No areas defined");
        }

        for (NArea area : allAreas.values()) {
            if (area.name != null && targetAreaNames.contains(area.name)) {
                targetAreas.add(area);
            }
        }

        if (targetAreas.isEmpty()) {
            return Results.ERROR("None of the intermittent failure areas exist in current area list");
        }

        gui.msg("Targeting " + targetAreas.size() + " areas: " +
                targetAreas.stream().map(a -> a.name).limit(5).collect(Collectors.joining(", ")) +
                (targetAreas.size() > 5 ? "..." : ""));

        // Get ChunkNavManager
        ChunkNavManager navManager = ((NMapView) gui.map).getChunkNavManager();
        if (navManager == null || !navManager.isInitialized()) {
            return Results.ERROR("ChunkNavManager not initialized");
        }

        gui.msg("Results will be saved to: " + outputFilePath);

        try {
            while (true) {
                // Pick furthest target area
                NArea targetArea = pickFurthestArea(gui);
                if (targetArea == null) {
                    gui.msg("Could not find valid target area, retrying...");
                    Thread.sleep(1000);
                    continue;
                }

                // Run test
                TestResult result = runSingleTest(gui, navManager, targetArea);
                results.add(result);

                // Log
                String status = result.success ? "SUCCESS" : "FAILED";
                String msg = String.format("Test #%d: %s -> %s: %s (%dms, dist=%.0f)",
                        result.id, result.fromAreaName, result.toAreaName,
                        status, result.durationMs, result.distanceToTarget);
                if (!result.success) {
                    msg += " - " + result.failureReason;
                }
                gui.msg(msg);

                // Save periodically
                if (results.size() % SAVE_INTERVAL == 0) {
                    saveResults();
                    gui.msg("Saved. Pass rate: " + getPassRateString());
                }

                Thread.sleep(DELAY_BETWEEN_TESTS_MS);
            }
        } finally {
            saveResults();
            gui.msg("Intermittent Failure Test stopped. Results saved.");
            gui.msg("Total: " + totalTests + ", Passed: " + passed + ", Failed: " + failed);
            gui.msg("Pass rate: " + getPassRateString());
        }
    }

    /**
     * Find the latest navtest_*.json file in the Haven and Hearth AppData folder.
     */
    private Path findLatestNavtestFile() {
        try {
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home") + "/AppData/Roaming";
            }
            Path hnhDir = Paths.get(appData, "Haven and Hearth");

            if (!Files.exists(hnhDir)) {
                return null;
            }

            return Files.list(hnhDir)
                    .filter(p -> p.getFileName().toString().startsWith("navtest_"))
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0;
                        }
                    }))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parse previous results and extract area names with intermittent failures.
     */
    private Set<String> extractIntermittentFailures(Path resultsFile) {
        Set<String> intermittent = new HashSet<>();

        try {
            String content = new String(Files.readAllBytes(resultsFile), StandardCharsets.UTF_8);
            JSONObject data = new JSONObject(content);
            JSONArray tests = data.optJSONArray("tests");

            if (tests == null) return intermittent;

            // Count successes and failures per destination
            Map<String, int[]> destStats = new HashMap<>(); // [passed, failed]

            for (int i = 0; i < tests.length(); i++) {
                JSONObject test = tests.getJSONObject(i);
                String dest = test.optString("toAreaName", "");
                boolean skipped = test.optBoolean("skipped", false);
                boolean success = test.optBoolean("success", false);

                if (dest.isEmpty() || skipped) continue;

                destStats.computeIfAbsent(dest, k -> new int[2]);
                if (success) {
                    destStats.get(dest)[0]++;
                } else {
                    destStats.get(dest)[1]++;
                }
            }

            // Find areas with intermittent failures
            for (Map.Entry<String, int[]> entry : destStats.entrySet()) {
                int passed = entry.getValue()[0];
                int failed = entry.getValue()[1];
                int total = passed + failed;

                if (total < MIN_ATTEMPTS) continue;

                double failRate = (double) failed / total * 100;

                if (failRate >= MIN_FAIL_RATE && failRate <= MAX_FAIL_RATE) {
                    intermittent.add(entry.getKey());
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing previous results: " + e.getMessage());
        }

        return intermittent;
    }

    /**
     * Pick the target area that is furthest from current position.
     */
    private NArea pickFurthestArea(NGameUI gui) {
        Gob player = gui.map.player();
        if (player == null) return null;

        Coord2d playerPos = player.rc;
        NArea furthest = null;
        double maxDist = -1;

        for (NArea area : targetAreas) {
            Coord2d center = area.getCenter2d();
            if (center == null) continue;

            double dist = playerPos.dist(center);
            if (dist > maxDist) {
                maxDist = dist;
                furthest = area;
            }
        }

        return furthest;
    }

    private TestResult runSingleTest(NGameUI gui, ChunkNavManager navManager, NArea targetArea)
            throws InterruptedException {
        TestResult result = new TestResult();
        result.id = ++totalTests;
        result.startTime = System.currentTimeMillis();
        result.toAreaName = targetArea.name != null ? targetArea.name : "Unnamed";

        Gob player = gui.map.player();
        if (player == null) {
            result.success = false;
            result.failureReason = "NO_PLAYER";
            result.failureDetails = "Player gob is null";
            result.endTime = System.currentTimeMillis();
            result.durationMs = result.endTime - result.startTime;
            failed++;
            return result;
        }

        result.startPosition = player.rc;
        result.fromAreaName = getCurrentAreaName(gui, player.rc);

        Coord2d targetCenter = targetArea.getCenter2d();
        result.targetPosition = targetCenter;
        result.distanceToTarget = targetCenter != null ? player.rc.dist(targetCenter) : 0;

        try {
            // Check path exists first
            ChunkPath path = navManager.planToArea(targetArea);
            if (path == null) {
                result.success = false;
                result.failureReason = "NO_PATH";
                result.failureDetails = "No valid path exists";
                result.endTime = System.currentTimeMillis();
                result.durationMs = result.endTime - result.startTime;
                failed++;
                return result;
            }

            // Execute navigation
            long navStart = System.currentTimeMillis();
            Results navResult = navManager.navigateToArea(targetArea, gui);
            long navEnd = System.currentTimeMillis();

            result.durationMs = navEnd - navStart;
            result.timedOut = result.durationMs > TIMEOUT_MS;

            player = gui.map.player();
            if (player != null) {
                result.endPosition = player.rc;
            }

            if (result.timedOut) {
                result.success = false;
                result.failureReason = "TIMEOUT";
                result.failureDetails = "Navigation took " + result.durationMs + "ms";
                failed++;
            } else if (!navResult.IsSuccess()) {
                result.success = false;
                result.failureReason = "NAV_FAILED";
                result.failureDetails = "ChunkNavManager returned failure";
                failed++;
            } else {
                if (isInOrNearArea(result.endPosition, targetArea)) {
                    result.success = true;
                    passed++;
                } else {
                    result.success = false;
                    result.failureReason = "WRONG_LOCATION";
                    double dist = result.endPosition != null && targetCenter != null
                            ? result.endPosition.dist(targetCenter) : -1;
                    result.failureDetails = "Distance to target: " + String.format("%.1f", dist);
                    failed++;
                }
            }

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            result.success = false;
            result.failureReason = "EXCEPTION";
            result.failureDetails = e.getClass().getSimpleName() + ": " + e.getMessage();
            failed++;
        }

        result.endTime = System.currentTimeMillis();
        if (result.durationMs == 0) {
            result.durationMs = result.endTime - result.startTime;
        }

        return result;
    }

    private String getCurrentAreaName(NGameUI gui, Coord2d playerPos) {
        if (allAreas == null || playerPos == null) return "Unknown";

        for (NArea area : allAreas.values()) {
            if (isInOrNearArea(playerPos, area)) {
                return area.name != null ? area.name : "Unnamed";
            }
        }
        return "Unknown";
    }

    private boolean isInOrNearArea(Coord2d pos, NArea area) {
        if (pos == null || area == null) return false;

        Pair<Coord2d, Coord2d> bounds = area.getRCArea();
        if (bounds != null) {
            double margin = 50;
            return pos.x >= bounds.a.x - margin && pos.x <= bounds.b.x + margin &&
                    pos.y >= bounds.a.y - margin && pos.y <= bounds.b.y + margin;
        }

        Coord2d center = area.getCenter2d();
        if (center != null) {
            return pos.dist(center) < 150;
        }

        return false;
    }

    private String generateOutputFilePath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = sdf.format(new Date());
        String basePath;
        try {
            basePath = ((HashDirCache) ResCache.global).base + "\\..\\";
        } catch (Exception e) {
            basePath = System.getProperty("user.home") + "\\";
        }
        return basePath + "intermittent_test_" + timestamp + ".json";
    }

    private String getPassRateString() {
        if (totalTests == 0) return "N/A";
        return String.format("%.1f%% (%d/%d)", passed * 100.0 / totalTests, passed, totalTests);
    }

    private void saveResults() {
        JSONObject main = new JSONObject();

        // Run info
        JSONObject runInfo = new JSONObject();
        runInfo.put("startTime", Instant.ofEpochMilli(runStartTime).toString());
        runInfo.put("endTime", Instant.now().toString());
        runInfo.put("totalTests", totalTests);
        runInfo.put("passed", passed);
        runInfo.put("failed", failed);
        runInfo.put("passRate", totalTests > 0 ? String.format("%.1f%%", passed * 100.0 / totalTests) : "N/A");
        runInfo.put("targetAreas", new JSONArray(targetAreaNames));
        main.put("runInfo", runInfo);

        // Failure summary
        Map<String, Integer> failureCounts = new HashMap<>();
        for (TestResult r : results) {
            if (!r.success && r.failureReason != null) {
                failureCounts.merge(r.failureReason, 1, Integer::sum);
            }
        }
        main.put("failureSummary", new JSONObject(failureCounts));

        // Per-area stats
        Map<String, int[]> areaStats = new HashMap<>();
        for (TestResult r : results) {
            areaStats.computeIfAbsent(r.toAreaName, k -> new int[2]);
            if (r.success) {
                areaStats.get(r.toAreaName)[0]++;
            } else {
                areaStats.get(r.toAreaName)[1]++;
            }
        }
        JSONObject areaStatsJson = new JSONObject();
        for (Map.Entry<String, int[]> entry : areaStats.entrySet()) {
            JSONObject stat = new JSONObject();
            stat.put("passed", entry.getValue()[0]);
            stat.put("failed", entry.getValue()[1]);
            int total = entry.getValue()[0] + entry.getValue()[1];
            stat.put("passRate", total > 0 ? String.format("%.1f%%", entry.getValue()[0] * 100.0 / total) : "N/A");
            areaStatsJson.put(entry.getKey(), stat);
        }
        main.put("areaStats", areaStatsJson);

        // All test results
        JSONArray testsArray = new JSONArray();
        for (TestResult r : results) {
            testsArray.put(r.toJson());
        }
        main.put("tests", testsArray);

        try (FileWriter writer = new FileWriter(outputFilePath, StandardCharsets.UTF_8)) {
            writer.write(main.toString(2));
        } catch (IOException e) {
            System.err.println("Failed to save results: " + e.getMessage());
        }
    }

    private static class TestResult {
        int id;
        long startTime;
        long endTime;
        long durationMs;
        String fromAreaName;
        Coord2d startPosition;
        String toAreaName;
        Coord2d targetPosition;
        Coord2d endPosition;
        double distanceToTarget;
        boolean success;
        boolean timedOut;
        String failureReason;
        String failureDetails;

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("startTime", startTime);
            obj.put("endTime", endTime);
            obj.put("durationMs", durationMs);
            obj.put("fromAreaName", fromAreaName != null ? fromAreaName : "Unknown");
            obj.put("toAreaName", toAreaName != null ? toAreaName : "Unknown");
            obj.put("distanceToTarget", distanceToTarget);
            obj.put("success", success);
            obj.put("timedOut", timedOut);

            if (startPosition != null) {
                JSONObject pos = new JSONObject();
                pos.put("x", startPosition.x);
                pos.put("y", startPosition.y);
                obj.put("startPosition", pos);
            }

            if (targetPosition != null) {
                JSONObject pos = new JSONObject();
                pos.put("x", targetPosition.x);
                pos.put("y", targetPosition.y);
                obj.put("targetPosition", pos);
            }

            if (endPosition != null) {
                JSONObject pos = new JSONObject();
                pos.put("x", endPosition.x);
                pos.put("y", endPosition.y);
                obj.put("endPosition", pos);
            }

            if (failureReason != null) {
                obj.put("failureReason", failureReason);
            }
            if (failureDetails != null) {
                obj.put("failureDetails", failureDetails);
            }

            return obj;
        }
    }
}
