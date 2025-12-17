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
import java.util.Collection;
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

        // Execute each waypoint
        for (int i = 0; i < path.waypoints.size(); i++) {
            ChunkPath.ChunkWaypoint waypoint = path.waypoints.get(i);
            ChunkPath.ChunkWaypoint nextWaypoint = (i + 1 < path.waypoints.size()) ? path.waypoints.get(i + 1) : null;

            // Handle portal traversal - traversePortal handles its own navigation to the portal gob
            if (waypoint.portal != null && waypoint.type == ChunkPath.WaypointType.PORTAL_ENTRY) {
                // Tick portal tracker before traversal to capture pre-portal state
                tickPortalTracker();

                Results portalResult = traversePortal(waypoint.portal, waypoint.gridId, gui);
                if (!portalResult.IsSuccess()) {
                    gui.error("ChunkNav: Portal traversal failed");
                    return Results.FAIL();
                }

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
        }

        // We've traversed all waypoints/portals - now navigate to just outside the target area
        if (targetArea != null) {
            // Wait for area to become visible
            Pair<Coord2d, Coord2d> areaBounds = null;
            int waitAttempts = 0;
            while (areaBounds == null && waitAttempts < 20) {
                areaBounds = targetArea.getRCArea();
                if (areaBounds == null) {
                    waitAttempts++;
                    Thread.sleep(250);
                }
            }

            if (areaBounds == null) {
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
                // Use step-by-step navigation which can walk toward distant targets
                // This handles cases where the area is in the same grid but not visible
                Results edgeResult = navigateToCoord(edgePoint, gui);
                if (edgeResult.IsSuccess()) {
                    return Results.SUCCESS();
                }
            }

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
     * First checks if target is reachable using intra-chunk pathfinding on walkability grid.
     * If path exists, follows it through waypoints instead of walking blindly.
     */
    private Results navigateToCoord(Coord2d target, NGameUI gui) throws InterruptedException {
        // Check if we're already close enough
        Gob player = gui.map.player();
        if (player != null && player.rc.dist(target) < MCache.tilesz.x * 2) {
            return Results.SUCCESS();
        }

        // Try direct path first (target might be visible and directly reachable)
        PathFinder pf = new PathFinder(target);
        Results result = pf.run(gui);

        if (result.IsSuccess()) {
            return result;
        }

        // Direct path failed - use intra-chunk pathfinding if we have chunk data
        // First, determine what chunk we're in and what chunk the target is in
        try {
            MCache mcache = gui.map.glob.map;
            Coord playerTile = player.rc.floor(MCache.tilesz);
            Coord targetTile = target.floor(MCache.tilesz);

            MCache.Grid playerGrid = mcache.getgridt(playerTile);
            if (playerGrid == null) {
                return Results.FAIL();
            }

            // Get chunk data for intra-chunk pathfinding
            ChunkNavData chunk = graph.getChunk(playerGrid.id);
            if (chunk == null) {
                return navigateToCoordStepByStep(target, gui);
            }

            // Calculate local coordinates within the chunk
            Coord playerLocal = playerTile.sub(playerGrid.ul);
            Coord targetLocal = targetTile.sub(playerGrid.ul);

            // Check if target is in the same chunk
            boolean targetInSameChunk = targetLocal.x >= 0 && targetLocal.x < CHUNK_SIZE &&
                                        targetLocal.y >= 0 && targetLocal.y < CHUNK_SIZE;

            if (targetInSameChunk) {
                // Use intra-chunk pathfinding
                ChunkNavIntraPathfinder.IntraPath intraPath = ChunkNavIntraPathfinder.findPath(playerLocal, targetLocal, chunk);

                if (!intraPath.reachable) {
                    System.err.println("ChunkNav: Target unreachable via walkability grid at (" + targetLocal + ")");
                    return Results.FAIL();
                }

                // Follow the path through each waypoint
                return followIntraChunkPath(intraPath, playerGrid, target, gui);
            } else {
                // Target is in different chunk - walk toward chunk edge
                return navigateToCoordStepByStep(target, gui);
            }
        } catch (Exception e) {
            return navigateToCoordStepByStep(target, gui);
        }
    }

    /**
     * Follow an intra-chunk path by navigating to each waypoint.
     */
    private Results followIntraChunkPath(ChunkNavIntraPathfinder.IntraPath intraPath,
                                         MCache.Grid grid, Coord2d finalTarget,
                                         NGameUI gui) throws InterruptedException {
        // Skip first waypoint (current position) and every other waypoint for efficiency
        // We don't need to visit every cell, just enough to ensure we follow the path
        int skipInterval = Math.max(1, intraPath.size() / 10); // Visit ~10 waypoints max

        for (int i = skipInterval; i < intraPath.localPath.size(); i += skipInterval) {
            Coord localCoord = intraPath.localPath.get(i);
            Coord tileCoord = grid.ul.add(localCoord);
            Coord2d worldCoord = tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);

            PathFinder waypointPf = new PathFinder(worldCoord);
            waypointPf.run(gui);
            // Don't fail on individual waypoints - walkability data might be outdated

            // Check if we can now reach the final target directly
            Gob player = gui.map.player();
            if (player != null && player.rc.dist(finalTarget) < MCache.tilesz.x * 15) {
                PathFinder directPf = new PathFinder(finalTarget);
                Results directResult = directPf.run(gui);
                if (directResult.IsSuccess()) {
                    return Results.SUCCESS();
                }
            }
        }

        // Final approach to target
        PathFinder finalPf = new PathFinder(finalTarget);
        return finalPf.run(gui);
    }

    /**
     * Fallback: Navigate step by step when we don't have chunk data.
     */
    private Results navigateToCoordStepByStep(Coord2d target, NGameUI gui) throws InterruptedException {
        int maxSteps = 20;
        double stepDistance = MCache.tilesz.x * 10;

        for (int step = 0; step < maxSteps; step++) {
            Gob player = gui.map.player();
            if (player == null) return Results.FAIL();

            double distToTarget = player.rc.dist(target);
            if (distToTarget < MCache.tilesz.x * 2) {
                return Results.SUCCESS();
            }

            // Try direct path
            PathFinder pf = new PathFinder(target);
            Results result = pf.run(gui);
            if (result.IsSuccess()) {
                return Results.SUCCESS();
            }

            // Calculate intermediate point toward target
            Coord2d direction = target.sub(player.rc).norm();
            double walkDist = Math.min(stepDistance, distToTarget - MCache.tilesz.x);
            Coord2d intermediateTarget = player.rc.add(direction.mul(walkDist));

            PathFinder stepPf = new PathFinder(intermediateTarget);
            result = stepPf.run(gui);

            if (!result.IsSuccess()) {
                stepDistance = stepDistance / 2;
                if (stepDistance < MCache.tilesz.x * 2) {
                    return Results.FAIL();
                }
            } else {
                stepDistance = MCache.tilesz.x * 10;
            }

            Thread.sleep(200);
        }

        PathFinder pf = new PathFinder(target);
        return pf.run(gui);
    }

    /**
     * Navigate incrementally to a target that may not be visible.
     * Walks step by step, each step getting closer until the target is reachable.
     * This is essential for reaching portal access points that are far away.
     */
    private Results navigateIncrementally(Coord2d target, NGameUI gui) throws InterruptedException {
        int maxSteps = 50;  // Allow more steps for longer distances
        double stepDistance = MCache.tilesz.x * 8;  // ~8 tiles per step

        System.out.println("ChunkNav: navigateIncrementally to " + target);

        for (int step = 0; step < maxSteps; step++) {
            Gob player = gui.map.player();
            if (player == null) return Results.FAIL();

            double distToTarget = player.rc.dist(target);
            System.out.println("ChunkNav: Step " + step + ", distance to target: " + distToTarget);

            // Close enough - we're done
            if (distToTarget < MCache.tilesz.x * 3) {
                System.out.println("ChunkNav: Reached target area");
                return Results.SUCCESS();
            }

            // Try direct path first - might work if target is now visible
            PathFinder directPf = new PathFinder(target);
            Results directResult = directPf.run(gui);
            if (directResult.IsSuccess()) {
                System.out.println("ChunkNav: Direct path succeeded");
                return Results.SUCCESS();
            }

            // Calculate intermediate point toward target
            Coord2d direction = target.sub(player.rc).norm();
            double walkDist = Math.min(stepDistance, distToTarget * 0.5);  // Walk halfway or stepDistance
            Coord2d intermediateTarget = player.rc.add(direction.mul(walkDist));

            System.out.println("ChunkNav: Walking toward intermediate point " + intermediateTarget);
            PathFinder stepPf = new PathFinder(intermediateTarget);
            Results stepResult = stepPf.run(gui);

            if (!stepResult.IsSuccess()) {
                // Try shorter step
                walkDist = walkDist / 2;
                if (walkDist < MCache.tilesz.x * 2) {
                    System.out.println("ChunkNav: Step too short, giving up");
                    return Results.FAIL();
                }
                intermediateTarget = player.rc.add(direction.mul(walkDist));
                stepPf = new PathFinder(intermediateTarget);
                stepResult = stepPf.run(gui);

                if (!stepResult.IsSuccess()) {
                    System.out.println("ChunkNav: Cannot make progress toward target");
                    return Results.FAIL();
                }
            }

            // Small delay for gobs to load after moving
            Thread.sleep(100);
        }

        System.out.println("ChunkNav: Max steps reached");
        return Results.FAIL();
    }

    /**
     * Traverse a portal (door, stairs, etc.).
     * The portal's localCoord is the player access point (where to stand to use the door).
     * @param portal The portal to traverse
     * @param gridId The grid ID where the portal is located (from waypoint)
     */
    private Results traversePortal(ChunkPortal portal, long gridId, NGameUI gui) throws InterruptedException {
        System.out.println("ChunkNav: traversePortal(" + portal.gobName + ") in grid " + gridId);

        // First navigate to the recorded access point (player position when door was last used)
        // This is CRITICAL when there are multiple portals with the same name (e.g., multiple houses)
        // The access point brings the correct portal into view range
        Coord2d accessPoint = getPortalAccessPoint(portal, gridId, gui);
        if (accessPoint != null) {
            System.out.println("ChunkNav: Navigating to access point " + accessPoint);

            // Use incremental navigation - PathFinder only works for visible areas
            // We need to walk step by step towards the target
            Results navResult = navigateIncrementally(accessPoint, gui);

            if (!navResult.IsSuccess()) {
                System.out.println("ChunkNav: Could not reach exact access point, continuing anyway");
            }

            // Wait a moment for gobs to load after arriving at the access point
            Thread.sleep(200);
        } else {
            System.out.println("ChunkNav: No access point available for portal " + portal.gobName);
        }

        // Now find the portal gob to click on it
        // IMPORTANT: We must find the SPECIFIC portal that leads to our target grid
        // There may be multiple portals with the same name (e.g., multiple houses with cellardoor)
        // We use the recorded position (localCoord) to find the correct one
        Gob portalGob = findPortalGobByPosition(portal, gridId, gui);
        if (portalGob == null) {
            System.out.println("ChunkNav: Portal not found by position, trying hash: " + portal.gobHash);
            // Fallback to hash lookup (might work if hash is stable)
            portalGob = Finder.findGob(portal.gobHash);
        }
        if (portalGob == null) {
            // Final fallback: find the closest gob with matching name to player
            // Only use this if we navigated to the access point first
            System.out.println("ChunkNav: Portal not found by hash, trying closest by name: " + portal.gobName);
            Collection<Gob> candidates = Finder.findGobs(new NAlias(portal.gobName));
            if (candidates != null && !candidates.isEmpty()) {
                Gob player = NUtils.player();
                if (player != null) {
                    double closestDist = Double.MAX_VALUE;
                    for (Gob gob : candidates) {
                        double dist = gob.rc.dist(player.rc);
                        if (dist < closestDist) {
                            closestDist = dist;
                            portalGob = gob;
                        }
                    }
                }
            }
        }
        if (portalGob == null) {
            gui.error("ChunkNav: Portal gob not found: " + portal.gobName + " at position " + portal.localCoord);
            return Results.FAIL();
        }
        System.out.println("ChunkNav: Found portal gob: " + portalGob.ngob.name + " at " + portalGob.rc);

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

                // First wait for basic map load
                NUtils.getUI().core.addTask(new WaitForMapLoad());

                // Then wait for the exit gob to appear by name
                if (exitGobName != null) {
                    NUtils.getUI().core.addTask(new WaitForExitPortal(exitGobName));
                }

                // Wait for gob loading to stabilize
                NUtils.getUI().core.addTask(new WaitForGobStability());
            } else {
                // Just wait for door animation
                NUtils.getUI().core.addTask(new WaitGobModelAttrChange(portalGob, portalGob.ngob.getModelAttribute()));
            }
        }

        return Results.SUCCESS();
    }

    /**
     * Find the specific portal gob by its recorded position.
     * This is crucial when there are multiple portals with the same name (e.g., multiple houses).
     * @param portal The portal with recorded localCoord (player access point)
     * @param gridId The grid ID where the portal should be
     * @param gui The game UI
     * @return The gob closest to the recorded position with matching name, or null if not found
     */
    private Gob findPortalGobByPosition(ChunkPortal portal, long gridId, NGameUI gui) {
        if (portal.localCoord == null) {
            return null;
        }

        // Calculate the expected world position from localCoord
        Coord2d expectedPos = getPortalAccessPoint(portal, gridId, gui);
        if (expectedPos == null) {
            return null;
        }

        // Find all gobs with matching name
        Collection<Gob> candidates = Finder.findGobs(new NAlias(portal.gobName));
        if (candidates == null || candidates.isEmpty()) {
            System.out.println("ChunkNav: No gobs found with name: " + portal.gobName);
            return null;
        }

        System.out.println("ChunkNav: Found " + candidates.size() + " candidates for " + portal.gobName + ", looking near " + expectedPos);

        // Find the one closest to expected position
        // The portal access point is where the player stands when using the door
        // Portal gob (building) should be within reasonable range
        Gob closest = null;
        double closestDist = Double.MAX_VALUE;
        double maxDistance = MCache.tilesz.x * 15; // Portal should be within 15 tiles of access point

        for (Gob gob : candidates) {
            double dist = gob.rc.dist(expectedPos);
            System.out.println("ChunkNav:   Candidate at " + gob.rc + " dist=" + dist);
            if (dist < closestDist && dist < maxDistance) {
                closestDist = dist;
                closest = gob;
            }
        }

        if (closest != null) {
            System.out.println("ChunkNav: Best match at " + closest.rc + " dist=" + closestDist);
        }

        return closest;
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
            // Ignore
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
                return true;
            }

            // Look for the exit portal by name
            try {
                Gob exitGob = Finder.findGob(new NAlias(exitPortalName));
                if (exitGob != null) {
                    return true;
                }
            } catch (Exception e) {
                // Still loading
            }

            return false;
        }
    }
}
