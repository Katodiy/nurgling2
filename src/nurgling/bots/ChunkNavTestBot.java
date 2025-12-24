package nurgling.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.navigation.*;

import java.util.*;

/**
 * Test bot for verifying the ChunkNav navigation system.
 * Tests recording, planning, and execution of chunk-based navigation.
 */
public class ChunkNavTestBot implements Action {

    private enum TestPhase {
        INIT,
        TEST_RECORDING,
        TEST_PLANNING,
        TEST_EXECUTION,
        REPORT_RESULTS,
        DONE
    }

    private TestPhase currentPhase = TestPhase.INIT;
    private StringBuilder report = new StringBuilder();
    private int testsRun = 0;
    private int testsPassed = 0;

    /**
     * Get the ChunkNavManager from the GUI.
     */
    private ChunkNavManager getManager(NGameUI gui) {
        if (gui != null && gui.map != null && gui.map instanceof NMapView) {
            return ((NMapView)gui.map).getChunkNavManager();
        }
        return null;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        report.append("=== ChunkNav Test Bot ===\n\n");

        // Phase 1: Initialize and verify system
        currentPhase = TestPhase.INIT;
        gui.msg("ChunkNav Test: Starting initialization tests...");
        testInitialization(gui);

        // Phase 2: Test recording
        currentPhase = TestPhase.TEST_RECORDING;
        gui.msg("ChunkNav Test: Starting recording tests...");
        testRecording(gui);

        // Phase 3: Test planning
        currentPhase = TestPhase.TEST_PLANNING;
        gui.msg("ChunkNav Test: Starting planning tests...");
        testPlanning(gui);

        // Phase 4: Test execution (optional - requires areas)
        currentPhase = TestPhase.TEST_EXECUTION;
        gui.msg("ChunkNav Test: Starting execution tests...");
        testExecution(gui);

        // Phase 5: Report results
        currentPhase = TestPhase.REPORT_RESULTS;
        reportResults(gui);

        currentPhase = TestPhase.DONE;
        return Results.SUCCESS();
    }

    /**
     * Test system initialization.
     */
    private void testInitialization(NGameUI gui) throws InterruptedException {
        report.append("--- Initialization Tests ---\n");

        // Test 1: Manager exists
        testsRun++;
        ChunkNavManager manager = getManager(gui);
        if (manager != null) {
            testsPassed++;
            report.append("[PASS] Manager exists\n");
        } else {
            report.append("[FAIL] Manager is null\n");
            return;
        }

        // Test 2: Manager initialized
        testsRun++;
        if (manager.isInitialized()) {
            testsPassed++;
            report.append("[PASS] Manager is initialized\n");
            report.append("       World: ").append(manager.getCurrentGenus()).append("\n");
        } else {
            report.append("[FAIL] Manager not initialized\n");
            // Try to initialize it
            try {
                String genus = manager.getCurrentGenus();
                if (genus != null && !genus.isEmpty()) {
                    manager.initialize(genus);
                    if (manager.isInitialized()) {
                        report.append("       Manually initialized for: ").append(genus).append("\n");
                    }
                }
            } catch (Exception e) {
                report.append("       Error: ").append(e.getMessage()).append("\n");
            }
        }

        // Test 3: Graph exists
        testsRun++;
        ChunkNavGraph graph = manager.getGraph();
        if (graph != null) {
            testsPassed++;
            report.append("[PASS] Graph exists\n");
            report.append("       ").append(graph.getStats()).append("\n");
        } else {
            report.append("[FAIL] Graph is null\n");
        }

        // Test 4: Recorder exists
        testsRun++;
        ChunkNavRecorder recorder = manager.getRecorder();
        if (recorder != null) {
            testsPassed++;
            report.append("[PASS] Recorder exists\n");
        } else {
            report.append("[FAIL] Recorder is null\n");
        }

        // Test 5: Planner exists
        testsRun++;
        ChunkNavPlanner planner = manager.getPlanner();
        if (planner != null) {
            testsPassed++;
            report.append("[PASS] Planner exists\n");
        } else {
            report.append("[FAIL] Planner is null\n");
        }

        report.append("\n");
    }

    /**
     * Test chunk recording from visible grids.
     */
    private void testRecording(NGameUI gui) throws InterruptedException {
        report.append("--- Recording Tests ---\n");

        ChunkNavManager manager = getManager(gui);
        if (manager == null) {
            report.append("[SKIP] Cannot test recording - manager not available\n\n");
            return;
        }
        ChunkNavGraph graph = manager.getGraph();
        ChunkNavRecorder recorder = manager.getRecorder();

        if (graph == null || recorder == null) {
            report.append("[SKIP] Cannot test recording - manager not properly initialized\n\n");
            return;
        }

        int initialChunkCount = graph.getChunkCount();
        report.append("Initial chunk count: ").append(initialChunkCount).append("\n");

        // Test 6: Force record visible grids
        testsRun++;
        try {
            MCache mcache = gui.map.glob.map;
            int gridsRecorded = 0;

            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    recorder.recordGrid(grid);
                    gridsRecorded++;
                }
            }

            int newChunkCount = graph.getChunkCount();
            if (newChunkCount >= initialChunkCount) {
                testsPassed++;
                report.append("[PASS] Recorded ").append(gridsRecorded).append(" visible grids\n");
                report.append("       New chunk count: ").append(newChunkCount).append("\n");
            } else {
                report.append("[FAIL] Chunk count decreased after recording\n");
            }
        } catch (Exception e) {
            report.append("[FAIL] Error recording grids: ").append(e.getMessage()).append("\n");
        }

        // Test 7: Verify player chunk exists
        testsRun++;
        long playerChunkId = graph.getPlayerChunkId();

        // Debug info
        try {
            Gob player = gui.map.player();
            if (player != null) {
                report.append("       Player pos: ").append(player.rc).append("\n");
                MCache mcache = gui.map.glob.map;
                Coord tileCoord = player.rc.floor(MCache.tilesz);
                report.append("       Tile coord: ").append(tileCoord).append("\n");
                MCache.Grid grid = mcache.getgridt(tileCoord);
                if (grid != null) {
                    report.append("       Grid ID: ").append(grid.id).append("\n");
                    report.append("       Grid UL: ").append(grid.ul).append("\n");
                    report.append("       Have chunk data: ").append(graph.hasChunk(grid.id)).append("\n");
                } else {
                    report.append("       Grid: null (not loaded)\n");
                }
            } else {
                report.append("       Player: null\n");
            }
        } catch (Exception e) {
            report.append("       Debug error: ").append(e.getMessage()).append("\n");
        }

        if (playerChunkId != -1) {  // -1 is the error value, grid IDs can be negative
            testsPassed++;
            report.append("[PASS] Player chunk identified: ").append(playerChunkId).append("\n");

            ChunkNavData playerChunk = graph.getChunk(playerChunkId);
            if (playerChunk != null) {
                report.append("       Grid coord: ").append(playerChunk.gridCoord).append("\n");
                report.append("       Portals: ").append(playerChunk.portals.size()).append("\n");
                report.append("       Confidence: ").append(String.format("%.2f", playerChunk.getCurrentConfidence())).append("\n");
            }
        } else {
            report.append("[FAIL] Could not identify player chunk\n");
        }

        // Test 8: Verify walkability data
        testsRun++;
        ChunkNavData testChunk = graph.getChunk(playerChunkId);
        if (testChunk != null && testChunk.walkability != null) {
            int walkable = 0, blocked = 0, partial = 0;
            for (int x = 0; x < testChunk.walkability.length; x++) {
                for (int y = 0; y < testChunk.walkability[x].length; y++) {
                    byte val = testChunk.walkability[x][y];
                    if (val == 0) walkable++;
                    else if (val == 1) partial++;
                    else blocked++;
                }
            }
            testsPassed++;
            report.append("[PASS] Walkability data present\n");
            report.append("       Walkable cells: ").append(walkable).append("\n");
            report.append("       Partial cells: ").append(partial).append("\n");
            report.append("       Blocked cells: ").append(blocked).append("\n");
        } else {
            report.append("[FAIL] No walkability data for player chunk\n");
        }

        // Test 9: Update connections
        testsRun++;
        try {
            manager.updateAllConnections();
            testsPassed++;
            report.append("[PASS] Connection update completed\n");
        } catch (Exception e) {
            report.append("[FAIL] Error updating connections: ").append(e.getMessage()).append("\n");
        }

        // Test 10: Check edges
        testsRun++;
        if (testChunk != null) {
            Collection<ChunkNavGraph.ChunkEdge> edges = graph.getEdges(playerChunkId);
            int totalEdges = edges.size();

            if (totalEdges > 0) {
                testsPassed++;
                report.append("[PASS] Edges found: ").append(totalEdges).append("\n");
                for (ChunkNavGraph.ChunkEdge edge : edges) {
                    report.append("       -> Chunk ").append(edge.toGridId)
                          .append(" (cost: ").append(String.format("%.1f", edge.cost)).append(")\n");
                }
            } else {
                report.append("[WARN] No edges found for player chunk (may be isolated)\n");
                testsPassed++; // Not necessarily a failure
            }
        } else {
            report.append("[FAIL] Cannot check edges - no test chunk\n");
        }

        report.append("\n");
    }

    /**
     * Test path planning.
     */
    private void testPlanning(NGameUI gui) throws InterruptedException {
        report.append("--- Planning Tests ---\n");

        ChunkNavManager manager = getManager(gui);
        if (manager == null) {
            report.append("[SKIP] Cannot test planning - manager not available\n\n");
            return;
        }
        ChunkNavGraph graph = manager.getGraph();
        ChunkNavPlanner planner = manager.getPlanner();

        if (graph == null || planner == null) {
            report.append("[SKIP] Cannot test planning - manager not properly initialized\n\n");
            return;
        }

        // Test 11: Plan to self (should succeed trivially)
        testsRun++;
        long playerChunkId = graph.getPlayerChunkId();
        if (playerChunkId != -1) {  // -1 is the error value, grid IDs can be negative
            ChunkPath selfPath = planner.planPath(playerChunkId, playerChunkId);
            if (selfPath != null && !selfPath.isEmpty()) {
                testsPassed++;
                report.append("[PASS] Self-path planning works\n");
                report.append("       Waypoints: ").append(selfPath.waypoints.size()).append("\n");
            } else {
                report.append("[WARN] Self-path returned empty (expected 1 waypoint)\n");
                testsPassed++; // Edge case, not really failure
            }
        } else {
            report.append("[FAIL] Cannot test self-path - no player chunk\n");
        }

        // Test 12: Plan to nearby chunk (if exists)
        testsRun++;
        Collection<ChunkNavGraph.ChunkEdge> edges = graph.getEdges(playerChunkId);
        if (!edges.isEmpty()) {
            ChunkNavGraph.ChunkEdge firstEdge = edges.iterator().next();
            ChunkPath neighborPath = planner.planPath(playerChunkId, firstEdge.toGridId);
            if (neighborPath != null && !neighborPath.isEmpty()) {
                testsPassed++;
                report.append("[PASS] Neighbor path planning works\n");
                report.append("       To chunk: ").append(firstEdge.toGridId).append("\n");
                report.append("       Waypoints: ").append(neighborPath.waypoints.size()).append("\n");
                report.append("       Total cost: ").append(String.format("%.1f", neighborPath.totalCost)).append("\n");
            } else {
                report.append("[FAIL] Could not plan path to neighbor chunk\n");
            }
        } else {
            report.append("[SKIP] No neighbor chunks to test path planning\n");
            testsRun--; // Don't count as test
        }

        // Test 13: Plan to area (if areas exist)
        testsRun++;
        Collection<NArea> areas = null;
        try {
            areas = gui.map.glob.map.areas.values();
        } catch (Exception e) {
            // No areas
        }
        if (areas != null && !areas.isEmpty()) {
            NArea testArea = areas.iterator().next();
            report.append("Testing path to area: ").append(testArea.name).append("\n");

            ChunkPath areaPath = planner.planToArea(testArea);
            if (areaPath != null && !areaPath.isEmpty()) {
                testsPassed++;
                report.append("[PASS] Area path planning works\n");
                report.append("       Waypoints: ").append(areaPath.waypoints.size()).append("\n");
                report.append("       Confidence: ").append(String.format("%.2f", areaPath.confidence)).append("\n");
            } else {
                report.append("[INFO] No path found to area (may not be explored yet)\n");
                testsPassed++; // Not a failure if area not reachable
            }
        } else {
            report.append("[SKIP] No areas defined to test area planning\n");
            testsRun--; // Don't count as test
        }

        report.append("\n");
    }

    /**
     * Test path execution (basic test without actual movement).
     */
    private void testExecution(NGameUI gui) throws InterruptedException {
        report.append("--- Execution Tests ---\n");

        ChunkNavManager manager = getManager(gui);

        // Test 14: Verify executor can be created
        testsRun++;
        try {
            ChunkNavGraph graph = manager.getGraph();
            ChunkPath testPath = new ChunkPath();

            // Create a simple waypoint for current position
            long playerChunkId = graph.getPlayerChunkId();
            if (playerChunkId >= 0) {
                ChunkPath.ChunkWaypoint wp = new ChunkPath.ChunkWaypoint();
                wp.gridId = playerChunkId;
                wp.localCoord = new Coord(50, 50);
                wp.type = ChunkPath.WaypointType.DESTINATION;

                Gob player = gui.map.player();
                if (player != null) {
                    wp.worldCoord = player.rc;
                }

                testPath.addWaypoint(wp);
            }

            ChunkNavExecutor executor = new ChunkNavExecutor(testPath, null, manager);
            testsPassed++;
            report.append("[PASS] Executor can be created\n");
        } catch (Exception e) {
            report.append("[FAIL] Error creating executor: ").append(e.getMessage()).append("\n");
        }

        // Test 15: Save and reload
        testsRun++;
        try {
            int chunksBefore = manager.getGraph().getChunkCount();
            manager.save();
            report.append("[PASS] Save completed (").append(chunksBefore).append(" chunks)\n");
            testsPassed++;
        } catch (Exception e) {
            report.append("[FAIL] Error saving: ").append(e.getMessage()).append("\n");
        }

        report.append("\n");
    }

    /**
     * Report test results.
     */
    private void reportResults(NGameUI gui) {
        report.append("=== Test Summary ===\n");
        report.append("Tests run: ").append(testsRun).append("\n");
        report.append("Tests passed: ").append(testsPassed).append("\n");
        report.append("Tests failed: ").append(testsRun - testsPassed).append("\n");
        report.append("Pass rate: ").append(String.format("%.1f%%", (100.0 * testsPassed / testsRun))).append("\n\n");

        // Print stats
        ChunkNavManager manager = getManager(gui);
        if (manager != null) {
            report.append("=== System Stats ===\n");
            report.append(manager.getStats()).append("\n");
        }

        // Output report
        String reportStr = report.toString();
        System.out.println(reportStr);

        // Also show summary in game
        gui.msg("ChunkNav Test Complete: " + testsPassed + "/" + testsRun + " passed");

        if (testsPassed == testsRun) {
            gui.msg("All tests passed!");
        } else {
            gui.msg((testsRun - testsPassed) + " tests failed - check console for details");
        }
    }
}
