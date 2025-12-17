package nurgling.navigation;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * Executes a ChunkPath using PathFinder for local navigation.
 * Handles portal traversals (doors, stairs) along the way.
 */
public class ChunkNavExecutor implements Action {
    private final ChunkPath path;
    private final NArea targetArea;
    private final ChunkNavGraph graph;
    private final ChunkNavRecorder recorder;
    private final ChunkNavManager manager;

    private int replanAttempts = 0;

    public ChunkNavExecutor(ChunkPath path, NArea targetArea, ChunkNavGraph graph, ChunkNavRecorder recorder) {
        this.path = path;
        this.targetArea = targetArea;
        this.graph = graph;
        this.recorder = recorder;
        this.manager = null;
    }

    public ChunkNavExecutor(ChunkPath path, NArea targetArea, ChunkNavManager manager) {
        this.path = path;
        this.targetArea = targetArea;
        this.graph = manager.getGraph();
        this.recorder = manager.getRecorder();
        this.manager = manager;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (gui == null || gui.map == null || path == null || path.isEmpty()) {
            return Results.FAIL();
        }

        Gob player = gui.map.player();
        if (player == null) {
            return Results.FAIL();
        }

        System.err.println("ChunkNavExecutor: Starting path execution with " + path.waypoints.size() + " waypoints");

        // Execute each waypoint
        for (int i = 0; i < path.waypoints.size(); i++) {
            ChunkPath.ChunkWaypoint waypoint = path.waypoints.get(i);
            ChunkPath.ChunkWaypoint nextWaypoint = (i + 1 < path.waypoints.size()) ? path.waypoints.get(i + 1) : null;
            System.err.println("ChunkNavExecutor: Processing waypoint " + i + "/" + path.waypoints.size() +
                " type=" + waypoint.type + " gridId=" + waypoint.gridId + " localCoord=" + waypoint.localCoord);

            // Handle portal traversal - traversePortal handles its own navigation to the portal gob
            if (waypoint.portal != null && waypoint.type == ChunkPath.WaypointType.PORTAL_ENTRY) {
                System.err.println("ChunkNavExecutor: Traversing portal " + waypoint.portal.gobName);
                // Tick portal tracker before traversal to capture pre-portal state
                tickPortalTracker();

                Results portalResult = traversePortal(waypoint.portal, waypoint.gridId, gui);
                if (!portalResult.IsSuccess()) {
                    gui.error("ChunkNav: Portal traversal failed");
                    return Results.FAIL();
                }

                System.err.println("ChunkNavExecutor: Portal traversal complete");

                // Tick portal tracker after traversal to detect grid change
                tickPortalTracker();

                // Record the traversal
                if (recorder != null && nextWaypoint != null) {
                    recorder.recordPortalTraversal(
                            waypoint.portal.gobHash,
                            waypoint.gridId,
                            nextWaypoint.gridId
                    );
                }
            } else {
                // Non-portal waypoint - navigate to world coordinate
                Coord2d targetCoord = getWaypointWorldCoord(waypoint, gui);
                if (targetCoord == null) {
                    // Waypoint grid not loaded, wait or skip
                    if (waypoint.type == ChunkPath.WaypointType.PORTAL_EXIT) {
                        // Expected after portal, wait for load
                        System.err.println("ChunkNavExecutor: Skipping PORTAL_EXIT waypoint (grid not loaded yet)");
                        continue;
                    }
                    gui.error("ChunkNav: Waypoint grid not loaded, attempting to continue");
                    continue;
                }

                // Navigate to waypoint using PathFinder
                Results navResult = navigateToCoord(targetCoord, gui);
                if (!navResult.IsSuccess()) {
                    // Navigation failed - attempt replan
                    if (replanAttempts < MAX_REPLAN_ATTEMPTS) {
                        replanAttempts++;
                        gui.msg("ChunkNav: Replanning after navigation failure");
                        return replanAndContinue(gui, i);
                    }
                    return Results.FAIL();
                }
            }

            // Periodically tick the portal tracker during navigation
            tickPortalTracker();
            System.err.println("ChunkNavExecutor: Waypoint " + i + " complete");
        }

        System.err.println("ChunkNavExecutor: All waypoints processed, navigating to target area edge");

        // We've traversed all waypoints/portals - now navigate to just outside the target area
        if (targetArea != null) {
            // Wait for area to become visible
            Pair<Coord2d, Coord2d> areaBounds = null;
            int waitAttempts = 0;
            while (areaBounds == null && waitAttempts < 20) {
                areaBounds = targetArea.getRCArea();
                if (areaBounds == null) {
                    waitAttempts++;
                    System.err.println("ChunkNavExecutor: Waiting for area '" + targetArea.name + "' to become visible (attempt " + waitAttempts + ")");
                    Thread.sleep(250);
                }
            }

            if (areaBounds == null) {
                System.err.println("ChunkNavExecutor: Area '" + targetArea.name + "' not visible after waiting");
                return Results.FAIL();
            }

            // Try each edge of the area until we find one we can reach
            Gob currentPlayer = gui.map.player();
            if (currentPlayer == null) {
                return Results.FAIL();
            }
            Coord2d playerPos = currentPlayer.rc;

            // Get all four edge points and try them in order of distance from player
            List<Coord2d> edgePoints = getAllAreaEdgePoints(areaBounds);
            edgePoints.sort(Comparator.comparingDouble(p -> p.dist(playerPos)));

            for (Coord2d edgePoint : edgePoints) {
                PathFinder pf = new PathFinder(edgePoint);
                Results edgeResult = pf.run(gui);
                if (edgeResult.IsSuccess()) {
                    System.err.println("ChunkNavExecutor: Reached target area '" + targetArea.name + "' edge at " + edgePoint);
                    return Results.SUCCESS();
                }
            }

            System.err.println("ChunkNavExecutor: Failed to navigate to any area edge");
            return Results.FAIL();
        }

        return Results.SUCCESS();
    }

    /**
     * Get world coordinate for a waypoint.
     * First tries pre-calculated coord, then loaded grids, then stored worldTileOrigin.
     */
    private Coord2d getWaypointWorldCoord(ChunkPath.ChunkWaypoint waypoint, NGameUI gui) {
        // Use pre-calculated world coord if available
        if (waypoint.worldCoord != null) {
            return waypoint.worldCoord;
        }

        // Try to calculate from currently loaded grid in MCache
        try {
            MCache mcache = gui.map.glob.map;
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == waypoint.gridId) {
                        Coord tileCoord = grid.ul.add(waypoint.localCoord);
                        return tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
                    }
                }
            }
        } catch (Exception e) {
            // Grid not loaded in MCache
        }

        // Fall back to stored worldTileOrigin from ChunkNavData
        ChunkNavData chunk = graph.getChunk(waypoint.gridId);
        if (chunk != null && chunk.worldTileOrigin != null) {
            Coord tileCoord = chunk.worldTileOrigin.add(waypoint.localCoord);
            return tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
        }

        return null;
    }

    /**
     * Navigate to a world coordinate using PathFinder.
     * If the target is not visible, walks toward it step by step until it becomes reachable.
     */
    private Results navigateToCoord(Coord2d target, NGameUI gui) throws InterruptedException {
        // Check if we're already close enough
        Gob player = gui.map.player();
        if (player != null && player.rc.dist(target) < MCache.tilesz.x * 2) {
            return Results.SUCCESS();
        }

        // Try direct path first
        PathFinder pf = new PathFinder(target);
        Results result = pf.run(gui);

        if (result.IsSuccess()) {
            return result;
        }

        // Direct path failed - target probably not visible
        // Walk toward target in steps to load more terrain

        int maxSteps = 20;
        double stepDistance = MCache.tilesz.x * 10; // Walk 10 tiles at a time

        for (int step = 0; step < maxSteps; step++) {
            player = gui.map.player();
            if (player == null) return Results.FAIL();

            double distToTarget = player.rc.dist(target);
            if (distToTarget < MCache.tilesz.x * 2) {
                return Results.SUCCESS(); // Close enough
            }

            // Try direct path again (target might now be visible)
            pf = new PathFinder(target);
            result = pf.run(gui);
            if (result.IsSuccess()) {
                return Results.SUCCESS();
            }

            // Calculate intermediate point toward target
            Coord2d direction = target.sub(player.rc).norm();
            double walkDist = Math.min(stepDistance, distToTarget - MCache.tilesz.x);
            Coord2d intermediateTarget = player.rc.add(direction.mul(walkDist));


            // Walk to intermediate point
            PathFinder stepPf = new PathFinder(intermediateTarget);
            result = stepPf.run(gui);

            if (!result.IsSuccess()) {
                // Can't even walk toward target - might be blocked
                // Try a shorter step
                stepDistance = stepDistance / 2;
                if (stepDistance < MCache.tilesz.x * 2) {
                    return Results.FAIL(); // Can't make progress
                }
            } else {
                // Made progress, restore step distance
                stepDistance = MCache.tilesz.x * 10;
            }

            // Small delay to let map load
            Thread.sleep(200);
        }

        // Final attempt after walking
        pf = new PathFinder(target);
        return pf.run(gui);
    }

    /**
     * Traverse a portal (door, stairs, etc.).
     * The portal's localCoord is the player access point (where to stand to use the door).
     * @param portal The portal to traverse
     * @param gridId The grid ID where the portal is located (from waypoint)
     */
    private Results traversePortal(ChunkPortal portal, long gridId, NGameUI gui) throws InterruptedException {
        // First navigate to the recorded access point (player position when door was last used)
        // This is stored in the portal's localCoord
        Coord2d accessPoint = getPortalAccessPoint(portal, gridId, gui);
        if (accessPoint != null) {
            System.err.println("ChunkNavExecutor: Navigating to portal access point at " + accessPoint);
            PathFinder accessPf = new PathFinder(accessPoint);
            Results accessResult = accessPf.run(gui);
            if (!accessResult.IsSuccess()) {
                System.err.println("ChunkNavExecutor: Failed to reach portal access point, trying direct approach");
            }
        }

        // Now find the portal gob to click on it
        Gob portalGob = Finder.findGob(portal.gobHash);
        if (portalGob == null) {
            // Hash might be synthetic (for auto-detected entrances), try finding by name
            portalGob = Finder.findGob(new NAlias(portal.gobName));
        }
        if (portalGob == null) {
            gui.error("ChunkNav: Portal gob not found: " + portal.gobName);
            return Results.FAIL();
        }

        // Interact with portal
        if (portal.requiresInteraction) {
            // Tell the portal tracker which portal we're about to click
            // This ensures correct recording even when multiple portals are nearby
            if (manager != null && manager.getPortalTracker() != null) {
                manager.getPortalTracker().setClickedPortal(portalGob);
            }

            NUtils.openDoorOnAGob(gui, portalGob);

            // Wait for map load if this is a loading portal
            if (isLoadingPortal(portal.type)) {
                // Wait for the exit portal to appear (like routes do)
                String exitGobName = GateDetector.getDoorPair(portal.gobName);
                System.err.println("ChunkNavExecutor: Waiting for exit portal: " + exitGobName);

                // First wait for basic map load
                NUtils.getUI().core.addTask(new WaitForMapLoad());

                // Then wait for the exit gob to appear by name
                if (exitGobName != null) {
                    NUtils.getUI().core.addTask(new WaitForExitPortal(exitGobName));
                }

                // Wait for gob loading to stabilize
                NUtils.getUI().core.addTask(new WaitForGobStability());

                System.err.println("ChunkNavExecutor: Map and exit portal loaded");
            } else {
                // Just wait for door animation
                NUtils.getUI().core.addTask(new WaitGobModelAttrChange(portalGob, portalGob.ngob.getModelAttribute()));
            }
        }

        return Results.SUCCESS();
    }

    /**
     * Check if a portal type causes map loading.
     */
    private boolean isLoadingPortal(ChunkPortal.PortalType type) {
        switch (type) {
            case DOOR:
            case CELLAR:
            case STAIRS_UP:
            case STAIRS_DOWN:
            case MINE_ENTRANCE:
                return true;
            case GATE:
            default:
                return false;
        }
    }

    /**
     * Get position in front of a gob.
     */
    private Coord2d getFrontOfGob(Gob gob, double offsetTiles) {
        double angle = gob.a;
        double offsetX = Math.cos(angle) * offsetTiles * MCache.tilesz.x;
        double offsetY = Math.sin(angle) * offsetTiles * MCache.tilesz.y;
        return new Coord2d(gob.rc.x + offsetX, gob.rc.y + offsetY);
    }

    /**
     * Get the world coordinate of the portal's access point.
     * The portal's localCoord stores where the player was standing when they used the door,
     * which is the accessible spot in front of the door.
     */
    private Coord2d getPortalAccessPoint(ChunkPortal portal, long gridId, NGameUI gui) {
        if (portal.localCoord == null) {
            return null;
        }

        try {
            MCache mcache = gui.map.glob.map;

            // Try loaded grid first
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == gridId) {
                        Coord tileCoord = grid.ul.add(portal.localCoord);
                        return tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
                    }
                }
            }

            // Fall back to stored worldTileOrigin from chunk data
            ChunkNavData chunk = graph.getChunk(gridId);
            if (chunk != null && chunk.worldTileOrigin != null) {
                Coord tileCoord = chunk.worldTileOrigin.add(portal.localCoord);
                return tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
            }
        } catch (Exception e) {
            System.err.println("ChunkNavExecutor: Error getting portal access point: " + e.getMessage());
        }

        return null;
    }

    /**
     * Attempt to replan and continue from current position.
     */
    private Results replanAndContinue(NGameUI gui, int failedWaypointIndex) throws InterruptedException {
        if (targetArea == null) {
            return Results.FAIL();
        }

        // Get current chunk
        long currentChunkId = graph.getPlayerChunkId();
        if (currentChunkId == -1) {  // -1 is error, grid IDs can be negative
            return Results.FAIL();
        }

        // Replan from current position
        ChunkNavPlanner planner = new ChunkNavPlanner(graph);
        ChunkPath newPath = planner.planToArea(targetArea);

        if (newPath == null || newPath.isEmpty()) {
            return Results.FAIL();
        }

        // Execute new path - use manager if available for portal tracking
        ChunkNavExecutor newExecutor;
        if (manager != null) {
            newExecutor = new ChunkNavExecutor(newPath, targetArea, manager);
        } else {
            newExecutor = new ChunkNavExecutor(newPath, targetArea, graph, recorder);
        }
        newExecutor.replanAttempts = this.replanAttempts;
        return newExecutor.run(gui);
    }

    /**
     * Tick the portal traversal tracker if available.
     */
    private void tickPortalTracker() {
        if (manager != null) {
            try {
                manager.tick();
            } catch (Exception e) {
                // Ignore errors
            }
        }
    }

    /**
     * Get points adjacent to all four edges of the area (center of each edge, one tile outside).
     */
    private List<Coord2d> getAllAreaEdgePoints(Pair<Coord2d, Coord2d> bounds) {
        List<Coord2d> points = new ArrayList<>();
        Coord2d areaMin = bounds.a;
        Coord2d areaMax = bounds.b;

        double offset = MCache.tilesz.x; // One tile outside
        double centerX = (areaMin.x + areaMax.x) / 2;
        double centerY = (areaMin.y + areaMax.y) / 2;

        // Left edge center
        points.add(new Coord2d(areaMin.x - offset, centerY));
        // Right edge center
        points.add(new Coord2d(areaMax.x + offset, centerY));
        // Top edge center
        points.add(new Coord2d(centerX, areaMin.y - offset));
        // Bottom edge center
        points.add(new Coord2d(centerX, areaMax.y + offset));

        return points;
    }

    /**
     * Simple task to wait for map loading after portal.
     */
    private static class WaitForMapLoad extends NTask {
        private long startTime;
        private static final long TIMEOUT_MS = PORTAL_LOAD_TIMEOUT_MS;

        public boolean check() {
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }

            // Check timeout
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                return true;
            }

            // Check if player exists and map is loaded
            try {
                Gob player = NUtils.player();
                if (player != null) {
                    // Give a bit more time for full load
                    return System.currentTimeMillis() - startTime > 2000;
                }
            } catch (Exception e) {
                // Still loading
            }

            return false;
        }
    }

    /**
     * Task to wait for an exit portal to appear after going through a door.
     * Similar to WaitForGobWithHash but searches by name pattern.
     */
    private static class WaitForExitPortal extends NTask {
        private final String exitPortalName;
        private long startTime;
        private static final long TIMEOUT_MS = PORTAL_LOAD_TIMEOUT_MS;

        public WaitForExitPortal(String exitPortalName) {
            this.exitPortalName = exitPortalName;
        }

        public boolean check() {
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }

            // Check timeout
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                System.err.println("WaitForExitPortal: Timeout waiting for " + exitPortalName);
                return true;
            }

            // Look for the exit portal by name
            try {
                Gob exitGob = Finder.findGob(new NAlias(exitPortalName));
                if (exitGob != null) {
                    System.err.println("WaitForExitPortal: Found exit portal " + exitPortalName);
                    return true;
                }
            } catch (Exception e) {
                // Still loading
            }

            return false;
        }
    }
}
