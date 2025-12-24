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
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/**
 * Automated stress test bot for the chunk navigation system.
 * Continuously navigates to random areas and logs results to a JSON file.
 * Designed for overnight testing to catch intermittent navigation bugs.
 */
public class NavigationStressTest implements Action {

    // Configuration
    private static final long TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
    private static final long DELAY_BETWEEN_TESTS_MS = 3000; // 3 seconds between tests
    private static final int SAVE_INTERVAL = 5; // Save every N tests

    // State
    private final List<TestResult> results = new ArrayList<>();
    private String outputFilePath;
    private long runStartTime;
    private int totalTests = 0;
    private int passed = 0;
    private int failed = 0;
    private Map<Integer, NArea> allAreas;
    private final Random random = new Random();

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Initialize
        runStartTime = System.currentTimeMillis();
        outputFilePath = generateOutputFilePath();

        gui.msg("Navigation Stress Test starting...");
        gui.msg("Results will be saved to: " + outputFilePath);

        // Get all areas
        allAreas = gui.map.glob.map.areas;
        if (allAreas == null || allAreas.size() < 2) {
            return Results.ERROR("Need at least 2 areas defined to run stress test");
        }

        List<NArea> areaList = new ArrayList<>(allAreas.values());
        gui.msg("Found " + areaList.size() + " areas to test");

        // Get ChunkNavManager
        ChunkNavManager navManager = ((NMapView) gui.map).getChunkNavManager();
        if (navManager == null || !navManager.isInitialized()) {
            return Results.ERROR("ChunkNavManager not initialized");
        }

        try {
            // Main test loop - runs until interrupted
            while (true) {
                // Pick a random target area
                NArea targetArea = pickRandomArea(areaList, gui);
                if (targetArea == null) {
                    gui.msg("Could not find valid target area, retrying...");
                    Thread.sleep(1000);
                    continue;
                }

                // Run single navigation test
                TestResult result = runSingleTest(gui, navManager, targetArea);
                results.add(result);

                // Log result to chat
                String status = result.success ? "SUCCESS" : "FAILED";
                String msg = String.format("Test #%d: %s -> %s: %s (%dms)",
                        result.id, result.fromAreaName, result.toAreaName,
                        status, result.durationMs);
                if (!result.success) {
                    msg += " - " + result.failureReason;
                }
                gui.msg(msg);

                // Save periodically
                if (results.size() % SAVE_INTERVAL == 0) {
                    saveResults();
                    gui.msg("Results saved. Pass rate: " + getPassRateString());
                }

                // Delay between tests
                Thread.sleep(DELAY_BETWEEN_TESTS_MS);
            }
        } finally {
            // Always save on exit
            saveResults();
            gui.msg("Navigation Stress Test stopped. Final results saved.");
            gui.msg("Total: " + totalTests + ", Passed: " + passed + ", Failed: " + failed);
            gui.msg("Pass rate: " + getPassRateString());
        }
    }

    private NArea pickRandomArea(List<NArea> areaList, NGameUI gui) {
        if (areaList.isEmpty()) return null;

        // Try to pick an area different from current location
        Gob player = gui.map.player();
        if (player == null) return null;

        String currentAreaName = getCurrentAreaName(gui, player.rc);

        // Shuffle and find first area that's different
        List<NArea> shuffled = new ArrayList<>(areaList);
        Collections.shuffle(shuffled, random);

        for (NArea area : shuffled) {
            if (area.name != null && !area.name.isEmpty()) {
                // Prefer areas different from current
                if (!area.name.equals(currentAreaName)) {
                    return area;
                }
            }
        }

        // If all else fails, return any valid area
        for (NArea area : shuffled) {
            if (area.name != null && !area.name.isEmpty()) {
                return area;
            }
        }

        return null;
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

        // Get target position
        Coord2d targetCenter = targetArea.getCenter2d();
        result.targetPosition = targetCenter;

        try {
            // Execute navigation
            long navStart = System.currentTimeMillis();
            Results navResult = navManager.navigateToArea(targetArea, gui);
            long navEnd = System.currentTimeMillis();

            result.durationMs = navEnd - navStart;
            result.timedOut = result.durationMs > TIMEOUT_MS;

            // Get end position
            player = gui.map.player();
            if (player != null) {
                result.endPosition = player.rc;
            }

            if (result.timedOut) {
                result.success = false;
                result.failureReason = "TIMEOUT";
                result.failureDetails = "Navigation took " + result.durationMs + "ms (limit: " + TIMEOUT_MS + "ms)";
                failed++;
            } else if (!navResult.IsSuccess()) {
                result.success = false;
                result.failureReason = "NAV_FAILED";
                result.failureDetails = "ChunkNavManager returned failure";
                failed++;
            } else {
                // Verify we actually arrived
                if (isInOrNearArea(result.endPosition, targetArea)) {
                    result.success = true;
                    passed++;
                } else {
                    result.success = false;
                    result.failureReason = "WRONG_LOCATION";
                    double dist = result.endPosition != null && targetCenter != null
                            ? result.endPosition.dist(targetCenter) : -1;
                    result.failureDetails = "Player not in target area. Distance to center: " +
                            String.format("%.1f", dist);
                    failed++;
                }
            }

        } catch (InterruptedException e) {
            // Re-throw to allow bot to stop
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
            // Check if in bounds with margin
            double margin = 50;
            return pos.x >= bounds.a.x - margin && pos.x <= bounds.b.x + margin &&
                    pos.y >= bounds.a.y - margin && pos.y <= bounds.b.y + margin;
        }

        // Area not visible, check distance to stored center
        Coord2d center = area.getCenter2d();
        if (center != null) {
            return pos.dist(center) < 150; // Within 150 units
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
        return basePath + "navtest_" + timestamp + ".json";
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
        main.put("runInfo", runInfo);

        // Failure summary
        Map<String, Integer> failureCounts = new HashMap<>();
        for (TestResult r : results) {
            if (!r.success && r.failureReason != null) {
                failureCounts.merge(r.failureReason, 1, Integer::sum);
            }
        }
        main.put("failureSummary", new JSONObject(failureCounts));

        // All test results
        JSONArray testsArray = new JSONArray();
        for (TestResult r : results) {
            testsArray.put(r.toJson());
        }
        main.put("tests", testsArray);

        // Write to file
        try (FileWriter writer = new FileWriter(outputFilePath, StandardCharsets.UTF_8)) {
            writer.write(main.toString(2));
        } catch (IOException e) {
            System.err.println("Failed to save nav test results: " + e.getMessage());
        }
    }

    /**
     * Data class for individual test results.
     */
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
            obj.put("success", success);
            obj.put("timedOut", timedOut);

            if (startPosition != null) {
                JSONObject startPos = new JSONObject();
                startPos.put("x", startPosition.x);
                startPos.put("y", startPosition.y);
                obj.put("startPosition", startPos);
            }

            if (targetPosition != null) {
                JSONObject targetPos = new JSONObject();
                targetPos.put("x", targetPosition.x);
                targetPos.put("y", targetPosition.y);
                obj.put("targetPosition", targetPos);
            }

            if (endPosition != null) {
                JSONObject endPos = new JSONObject();
                endPos.put("x", endPosition.x);
                endPos.put("y", endPosition.y);
                obj.put("endPosition", endPos);
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
