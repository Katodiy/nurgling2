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
import java.util.stream.Collectors;

import static nurgling.navigation.ChunkNavConfig.*;
import static nurgling.navigation.ChunkNavDebug.*;

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

    /**
     * Configuration for incremental walking toward a target.
     */
    private static class WalkConfig {
        final int maxSteps;
        final double stepDistanceTiles;
        final double closeEnoughTiles;
        final boolean tryDirectFirst;

        WalkConfig(int maxSteps, double stepDistanceTiles, double closeEnoughTiles, boolean tryDirectFirst) {
            this.maxSteps = maxSteps;
            this.stepDistanceTiles = stepDistanceTiles;
            this.closeEnoughTiles = closeEnoughTiles;
            this.tryDirectFirst = tryDirectFirst;
        }

        static final WalkConfig DEFAULT = new WalkConfig(50, 8, 3, true);
        static final WalkConfig SEGMENT = new WalkConfig(30, 6, 3, true);
        static final WalkConfig STEP_BY_STEP = new WalkConfig(20, 10, 2, true);
    }

    /**
     * Scored portal candidate for priority-based selection.
     */
    private static class ScoredPortal implements Comparable<ScoredPortal> {
        final ChunkPortal portal;
        final int score;

        ScoredPortal(ChunkPortal portal, int score) {
            this.portal = portal;
            this.score = score;
        }

        @Override
        public int compareTo(ScoredPortal other) {
            return Integer.compare(other.score, this.score); // Higher score first
        }
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
        log("Executor.run() called, path=%s, targetArea=%s",
            path != null ? path.size() + " waypoints, " + path.segments.size() + " segments" : "null",
            targetArea != null ? targetArea.name : "null");

        if (gui == null || gui.map == null || path == null) {
            log("Executor.run() FAIL - gui/map/path null");
            return Results.FAIL();
        }

        Gob player = gui.map.player();
        if (player == null) {
            log("Executor.run() FAIL - player null");
            return Results.FAIL();
        }

        if (path.hasDetailedPath()) {
            log("Following detailed path with %d segments, %d tile steps",
                path.segments.size(), path.getTotalTileSteps());
            return followDetailedPath(gui);
        }

        if (!path.isEmpty()) {
            log("No detailed path, falling back to waypoint navigation");
            return followWaypointPath(gui);
        }

        if (path.isEmpty() && targetArea != null) {
            log("Empty path with target area, navigating directly");
            return navigateToTargetArea(gui);
        }

        log("Executor.run() FAIL - path empty and no targetArea");
        return Results.FAIL();
    }

    private Results followDetailedPath(NGameUI gui) throws InterruptedException {
        int segmentIndex = 0;
        Layer currentLayer = getCurrentPlayerLayer();

        for (ChunkPath.PathSegment segment : path.segments) {
            segmentIndex++;

            ChunkNavData segmentChunk = graph.getChunk(segment.gridId);
            Layer segmentLayer = segmentChunk != null ? Layer.fromString(segmentChunk.layer) : Layer.SURFACE;
            boolean sameLayer = segmentLayer == currentLayer;

            log("Following segment %d/%d with %d steps, type=%s, currentLayer=%s, segmentLayer=%s, sameLayer=%s",
                segmentIndex, path.segments.size(), segment.steps.size(), segment.type,
                currentLayer, segmentLayer, sameLayer);

            if (sameLayer) {
                Results segResult = followSegmentTiles(segment, gui);
                if (!segResult.IsSuccess()) {
                    log("Segment %d failed", segmentIndex);
                    return Results.FAIL();
                }
            } else {
                log("Cross-layer segment - skipping walk, will traverse portal");
            }

            if (segment.type == ChunkPath.SegmentType.PORTAL) {
                log("Looking for portal to traverse after segment %d", segmentIndex);
                tickPortalTracker();

                long targetGridId = -1;
                if (segmentIndex < path.segments.size()) {
                    ChunkPath.PathSegment nextSegment = path.segments.get(segmentIndex);
                    targetGridId = nextSegment.gridId;
                    log("Need portal connecting to grid %d", targetGridId);
                }

                Results portalResult = findAndTraversePortalToGrid(gui, segment, targetGridId);
                if (!portalResult.IsSuccess()) {
                    log("Portal traversal failed - trying to continue anyway");
                }

                currentLayer = getCurrentPlayerLayer();
                log("After portal, current layer is now: %s", currentLayer);

                tickPortalTracker();
            }

            tickPortalTracker();
        }

        log("Detailed path completed successfully");
        return Results.SUCCESS();
    }

    private Layer getCurrentPlayerLayer() {
        try {
            long gridId = graph.getPlayerChunkId();
            if (gridId != -1) {
                ChunkNavData chunk = graph.getChunk(gridId);
                if (chunk != null) {
                    return Layer.fromString(chunk.layer);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return Layer.SURFACE;
    }

    /**
     * Find and traverse portal using priority-based candidate ranking.
     */
    private Results findAndTraversePortalToGrid(NGameUI gui, ChunkPath.PathSegment segment, long targetGridId) throws InterruptedException {
        Gob player = gui.map.player();
        if (player == null) return Results.FAIL();

        ChunkNavData sourceChunk = graph.getChunk(segment.gridId);
        Layer sourceLayer = sourceChunk != null ? Layer.fromString(sourceChunk.layer) : Layer.SURFACE;
        ChunkNavData targetChunk = graph.getChunk(targetGridId);
        Layer targetLayer = targetChunk != null ? Layer.fromString(targetChunk.layer) : Layer.UNKNOWN;

        String expectedPortalType = getExpectedPortalType(sourceLayer, targetLayer);
        log("Layer transition %s -> %s, expected portal type: %s",
            sourceLayer, targetLayer, expectedPortalType != null ? expectedPortalType : "any");

        ChunkNavData chunk = graph.getChunk(segment.gridId);
        if (chunk != null && !chunk.portals.isEmpty()) {
            List<ChunkPortal> portalsCopy = new ArrayList<>(chunk.portals);
            log("Checking %d recorded portals in chunk %d for connection to %d",
                portalsCopy.size(), segment.gridId, targetGridId);

            // Rank all portals by priority score
            List<ScoredPortal> rankedPortals = rankPortalCandidates(
                portalsCopy, expectedPortalType, targetGridId, targetLayer);

            // Try portals in priority order
            for (ScoredPortal scored : rankedPortals) {
                log("Trying portal %s (score=%d)", scored.portal.gobName, scored.score);
                Results result = tryTraversePortal(gui, player, scored.portal);
                if (result != null) return result;
            }
        }

        // Fallback: Look for common portal gob patterns
        Gob nearestPortal = findNearestPortalGob(gui, player);
        if (nearestPortal != null) {
            String portalName = nearestPortal.ngob != null ? nearestPortal.ngob.name : "unknown";
            log("Found nearby portal (fallback): %s", portalName);
            return traversePortalGob(gui, nearestPortal);
        }

        log("No portal found nearby");
        return Results.FAIL();
    }

    /**
     * Rank portal candidates by priority score.
     * Higher scores = better matches.
     */
    private List<ScoredPortal> rankPortalCandidates(List<ChunkPortal> portals, String expectedType,
                                                     long targetGridId, Layer targetLayer) {
        List<ScoredPortal> scored = new ArrayList<>();

        for (ChunkPortal portal : portals) {
            int score = 0;

            // Skip building gobs when transitioning to mine
            if (targetLayer.isMine() && isBuildingGob(portal.gobName)) {
                continue;
            }

            // Connects to target grid: +100
            if (portal.connectsToGridId == targetGridId) {
                score += 100;
            }

            // Has any connection recorded: +10
            if (portal.connectsToGridId != -1) {
                score += 10;
            }

            // Matches expected portal type: +50
            if (expectedType != null && portal.gobName != null &&
                portal.gobName.toLowerCase().contains(expectedType)) {
                score += 50;
            }

            if (score > 0) {
                scored.add(new ScoredPortal(portal, score));
            }
        }

        return scored.stream().sorted().collect(Collectors.toList());
    }

    private Gob findNearestPortalGob(NGameUI gui, Gob player) {
        Gob nearest = null;
        double nearestDist = Double.MAX_VALUE;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.ngob == null || gob.ngob.name == null) continue;

                String lower = gob.ngob.name.toLowerCase();
                boolean isPortal = lower.contains("door") || lower.contains("stairs") ||
                                   lower.contains("cellar") || lower.contains("ladder") ||
                                   lower.contains("entrance") ||
                                   lower.contains("stonemansion") || lower.contains("logcabin") ||
                                   lower.contains("timberhouse") || lower.contains("stonestead") ||
                                   lower.contains("greathall") || lower.contains("stonetower") ||
                                   lower.contains("windmill");

                if (isPortal) {
                    double dist = player.rc.dist(gob.rc);
                    if (dist < nearestDist && dist < MCache.tilesz.x * 10) {
                        nearestDist = dist;
                        nearest = gob;
                    }
                }
            }
        }
        return nearest;
    }

    private String getExpectedPortalType(Layer sourceLayer, Layer targetLayer) {
        if (sourceLayer == null || targetLayer == null) return null;

        // Surface/inside -> mine: use minehole
        if (!sourceLayer.isMine() && targetLayer.isMine()) {
            return "minehole";
        }
        // Mine -> surface/shallower mine: use ladder
        if (sourceLayer.isMine() && !targetLayer.isMine()) {
            return "ladder";
        }
        // Mine level changes
        if (sourceLayer.isMine() && targetLayer.isMine()) {
            if (targetLayer.getMineLevel() > sourceLayer.getMineLevel()) {
                return "minehole";  // Going deeper
            } else if (targetLayer.getMineLevel() < sourceLayer.getMineLevel()) {
                return "ladder";    // Going up
            }
        }
        // Inside -> surface: use -door suffix
        if (sourceLayer == Layer.INSIDE && targetLayer == Layer.SURFACE) {
            return "-door";
        }
        // Inside -> cellar: use cellardoor
        if (sourceLayer == Layer.INSIDE && targetLayer == Layer.CELLAR) {
            return "cellardoor";
        }
        // Cellar -> inside: use cellarstairs
        if (sourceLayer == Layer.CELLAR && targetLayer == Layer.INSIDE) {
            return "cellarstairs";
        }

        return null;
    }

    private boolean isBuildingGob(String gobName) {
        if (gobName == null) return false;
        String lower = gobName.toLowerCase();
        return lower.contains("stonemansion") || lower.contains("logcabin") ||
               lower.contains("timberhouse") || lower.contains("stonestead") ||
               lower.contains("greathall") || lower.contains("stonetower") ||
               lower.contains("windmill");
    }

    private Results tryTraversePortal(NGameUI gui, Gob player, ChunkPortal recordedPortal) throws InterruptedException {
        Gob portalGob = findGobByName(gui, recordedPortal.gobName, player.rc, MCache.tilesz.x * 30);
        if (portalGob == null) {
            log("Portal gob not found: %s", recordedPortal.gobName);
            return null;
        }

        double dist = player.rc.dist(portalGob.rc);
        log("Portal %s at dist=%.1f", recordedPortal.gobName, dist);

        if (dist > MCache.tilesz.x * 5) {
            log("Portal is far, walking toward it...");
            PathFinder pf = new PathFinder(portalGob);
            Results walkResult = pf.run(gui);
            if (!walkResult.IsSuccess()) {
                log("Failed to walk to portal via PathFinder");
                return null;
            }
        }

        return traversePortalGob(gui, portalGob);
    }

    private Gob findGobByName(NGameUI gui, String gobName, Coord2d center, double maxDist) {
        Gob closest = null;
        double closestDist = maxDist;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.ngob == null || gob.ngob.name == null) continue;

                if (gob.ngob.name.equals(gobName) || gob.ngob.name.contains(gobName) ||
                    gobName.contains(gob.ngob.name)) {
                    double dist = center.dist(gob.rc);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = gob;
                    }
                }
            }
        }
        return closest;
    }

    private Results traversePortalGob(NGameUI gui, Gob portalGob) throws InterruptedException {
        String portalName = portalGob.ngob != null ? portalGob.ngob.name : "unknown";
        log("traversePortalGob(%s) starting...", portalName);

        if (manager != null && manager.getPortalTracker() != null) {
            manager.getPortalTracker().setClickedPortal(portalGob);
        }

        log("Opening door on gob at %s...", portalGob.rc);
        try {
            NUtils.openDoorOnAGob(gui, portalGob);
        } catch (Exception e) {
            error("openDoorOnAGob threw exception: " + e.getMessage(), e);
            return Results.FAIL();
        }

        log("Waiting for map load...");
        NUtils.getUI().core.addTask(new WaitForMapLoad());
        NUtils.getUI().core.addTask(new WaitForGobStability());

        log("Portal traversal completed");
        return Results.SUCCESS();
    }

    private Results followSegmentTiles(ChunkPath.PathSegment segment, NGameUI gui) throws InterruptedException {
        if (segment.isEmpty()) {
            log("Segment is empty, returning success");
            return Results.SUCCESS();
        }

        ChunkNavData segmentChunk = graph.getChunk(segment.gridId);
        log("followSegmentTiles() - segment grid=%d, worldTileOrigin=%s, steps=%d, chunk=%s",
            segment.gridId, segment.worldTileOrigin, segment.steps.size(),
            segmentChunk != null ?
                "exists(worldTileOrigin=" + segmentChunk.worldTileOrigin + ", layer=" + segmentChunk.layer + ")" :
                "NOT FOUND");

        if (segment.worldTileOrigin == null) {
            warn("segment has null worldTileOrigin!");
            ChunkNavData chunk = graph.getChunk(segment.gridId);
            if (chunk != null && chunk.worldTileOrigin != null) {
                log("Fixed worldTileOrigin from chunk data: %s", chunk.worldTileOrigin);
                segment.worldTileOrigin = chunk.worldTileOrigin;
                for (ChunkPath.TileStep step : segment.steps) {
                    if (step.worldCoord == null && step.localCoord != null) {
                        Coord worldTile = chunk.worldTileOrigin.add(step.localCoord);
                        step.worldCoord = worldTile.mul(MCache.tilesz).add(MCache.tilehsz);
                    }
                }
            }
        }

        ChunkPath.TileStep lastStep = segment.steps.get(segment.steps.size() - 1);
        if (lastStep.worldCoord == null) {
            log("Final step has no worldCoord, cannot follow segment");
            return Results.FAIL();
        }

        Coord2d destination = lastStep.worldCoord;
        log("Segment destination: %s", destination);

        return walkTowardTarget(destination, gui, WalkConfig.SEGMENT);
    }

    /**
     * Unified method for incremental navigation toward a target.
     * Consolidates navigateIncrementally, navigateIncrementallyToSegmentEnd, and navigateToCoordStepByStep.
     */
    private Results walkTowardTarget(Coord2d target, NGameUI gui, WalkConfig config) throws InterruptedException {
        double tileSize = MCache.tilesz.x;
        double stepDistance = tileSize * config.stepDistanceTiles;

        log("walkTowardTarget to %s (maxSteps=%d, stepDist=%.0f, closeEnough=%.0f)",
            target, config.maxSteps, config.stepDistanceTiles, config.closeEnoughTiles);

        for (int step = 0; step < config.maxSteps; step++) {
            Gob player = gui.map.player();
            if (player == null) return Results.FAIL();

            double distToTarget = player.rc.dist(target);

            // Close enough - we're done
            if (distToTarget < tileSize * config.closeEnoughTiles) {
                log("Reached target at distance %.1f", distToTarget);
                return Results.SUCCESS();
            }

            // Try direct path first if configured
            if (config.tryDirectFirst) {
                PathFinder directPf = new PathFinder(target);
                Results directResult = directPf.run(gui);
                if (directResult.IsSuccess()) {
                    log("Direct path succeeded");
                    return Results.SUCCESS();
                }
            }

            // Walk incrementally toward target
            Coord2d direction = target.sub(player.rc).norm();
            double walkDist = Math.min(stepDistance, distToTarget * 0.5);
            Coord2d intermediateTarget = player.rc.add(direction.mul(walkDist));

            log("Step %d, distance=%.1f, walking toward %s", step, distToTarget, intermediateTarget);

            PathFinder stepPf = new PathFinder(intermediateTarget);
            Results stepResult = stepPf.run(gui);

            if (!stepResult.IsSuccess()) {
                // Try shorter step
                walkDist = walkDist / 2;
                if (walkDist < tileSize * 2) {
                    log("Step too short, giving up");
                    return Results.FAIL();
                }
                intermediateTarget = player.rc.add(direction.mul(walkDist));
                stepPf = new PathFinder(intermediateTarget);
                stepResult = stepPf.run(gui);

                if (!stepResult.IsSuccess()) {
                    log("Cannot make progress toward target");
                    return Results.FAIL();
                }
            }

            Thread.sleep(100);
        }

        log("Max steps reached");
        return Results.FAIL();
    }

    private Results followWaypointPath(NGameUI gui) throws InterruptedException {
        for (int i = 0; i < path.waypoints.size(); i++) {
            ChunkPath.ChunkWaypoint waypoint = path.waypoints.get(i);
            ChunkPath.ChunkWaypoint nextWaypoint = (i + 1 < path.waypoints.size()) ? path.waypoints.get(i + 1) : null;

            if (waypoint.portal != null && waypoint.type == ChunkPath.WaypointType.PORTAL_ENTRY) {
                tickPortalTracker();

                Results portalResult = traversePortal(waypoint.portal, waypoint.gridId, gui);
                if (!portalResult.IsSuccess()) {
                    gui.error("ChunkNav: Portal traversal failed");
                    return Results.FAIL();
                }

                tickPortalTracker();

                if (recorder != null && nextWaypoint != null) {
                    recorder.recordPortalTraversal(
                            waypoint.portal.gobHash,
                            waypoint.gridId,
                            nextWaypoint.gridId
                    );
                }
            } else {
                Coord2d targetCoord = getWaypointWorldCoord(waypoint, gui);
                if (targetCoord == null) {
                    if (waypoint.type == ChunkPath.WaypointType.PORTAL_EXIT) {
                        continue;
                    }
                    gui.error("ChunkNav: Waypoint grid not loaded, attempting to continue");
                    continue;
                }

                Results navResult = navigateToCoord(targetCoord, gui);
                if (!navResult.IsSuccess()) {
                    if (replanAttempts < MAX_REPLAN_ATTEMPTS) {
                        replanAttempts++;
                        gui.msg("ChunkNav: Replanning after navigation failure");
                        return replanAndContinue(gui, i);
                    }
                    return Results.FAIL();
                }
            }

            tickPortalTracker();
        }

        if (targetArea != null) {
            return navigateToTargetArea(gui);
        }

        return Results.SUCCESS();
    }

    private Results navigateToTargetArea(NGameUI gui) throws InterruptedException {
        if (targetArea == null) {
            return Results.FAIL();
        }

        log("Navigating to target area: %s", targetArea.name);

        Pair<Coord2d, Coord2d> areaBounds = targetArea.getRCArea();
        Coord2d areaCenter = targetArea.getCenter2d();

        if (areaBounds == null && areaCenter == null) {
            log("Area not visible and no stored center");
            return Results.FAIL();
        }

        if (areaBounds == null && areaCenter != null) {
            log("Area not visible, walking toward center: %s", areaCenter);
            Results walkResult = walkTowardTarget(areaCenter, gui, WalkConfig.DEFAULT);
            if (!walkResult.IsSuccess()) {
                log("Could not reach area center");
                return Results.FAIL();
            }

            int waitAttempts = 0;
            while (areaBounds == null && waitAttempts < 10) {
                areaBounds = targetArea.getRCArea();
                if (areaBounds == null) {
                    waitAttempts++;
                    Thread.sleep(200);
                }
            }
        }

        if (areaBounds == null) {
            int waitAttempts = 0;
            while (areaBounds == null && waitAttempts < 20) {
                areaBounds = targetArea.getRCArea();
                if (areaBounds == null) {
                    waitAttempts++;
                    Thread.sleep(250);
                }
            }
        }

        if (areaBounds == null) {
            log("Area still not visible after waiting");
            return Results.FAIL();
        }

        Gob currentPlayer = gui.map.player();
        if (currentPlayer == null) {
            return Results.FAIL();
        }
        Coord2d playerPos = currentPlayer.rc;

        List<Coord2d> edgePoints = getAllAreaEdgePoints(areaBounds);
        edgePoints.sort(Comparator.comparingDouble(p -> p.dist(playerPos)));

        log("Trying %d edge points", edgePoints.size());

        for (Coord2d edgePoint : edgePoints) {
            log("Trying edge point: %s", edgePoint);
            Results edgeResult = walkTowardTarget(edgePoint, gui, WalkConfig.DEFAULT);
            if (edgeResult.IsSuccess()) {
                log("Reached area edge");
                return Results.SUCCESS();
            }
        }

        log("Could not reach any area edge");
        return Results.FAIL();
    }

    private Coord2d getWaypointWorldCoord(ChunkPath.ChunkWaypoint waypoint, NGameUI gui) {
        if (waypoint.worldCoord != null) {
            return waypoint.worldCoord;
        }

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

        ChunkNavData chunk = graph.getChunk(waypoint.gridId);
        if (chunk != null && chunk.worldTileOrigin != null) {
            Coord tileCoord = chunk.worldTileOrigin.add(waypoint.localCoord);
            return tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
        }

        return null;
    }

    private Results navigateToCoord(Coord2d target, NGameUI gui) throws InterruptedException {
        Gob player = gui.map.player();
        if (player != null && player.rc.dist(target) < MCache.tilesz.x * 2) {
            return Results.SUCCESS();
        }

        PathFinder pf = new PathFinder(target);
        Results result = pf.run(gui);

        if (result.IsSuccess()) {
            return result;
        }

        try {
            MCache mcache = gui.map.glob.map;
            Coord playerTile = player.rc.floor(MCache.tilesz);
            Coord targetTile = target.floor(MCache.tilesz);

            MCache.Grid playerGrid = mcache.getgridt(playerTile);
            if (playerGrid == null) {
                return Results.FAIL();
            }

            ChunkNavData chunk = graph.getChunk(playerGrid.id);
            if (chunk == null) {
                return walkTowardTarget(target, gui, WalkConfig.STEP_BY_STEP);
            }

            Coord playerLocal = playerTile.sub(playerGrid.ul);
            Coord targetLocal = targetTile.sub(playerGrid.ul);

            boolean targetInSameChunk = targetLocal.x >= 0 && targetLocal.x < CHUNK_SIZE &&
                                        targetLocal.y >= 0 && targetLocal.y < CHUNK_SIZE;

            if (targetInSameChunk) {
                ChunkNavIntraPathfinder.IntraPath intraPath = ChunkNavIntraPathfinder.findPath(playerLocal, targetLocal, chunk);

                if (!intraPath.reachable) {
                    error("Target unreachable via walkability grid at (" + targetLocal + ")");
                    return Results.FAIL();
                }

                return followIntraChunkPath(intraPath, playerGrid, target, gui);
            } else {
                return walkTowardTarget(target, gui, WalkConfig.STEP_BY_STEP);
            }
        } catch (Exception e) {
            return walkTowardTarget(target, gui, WalkConfig.STEP_BY_STEP);
        }
    }

    private Results followIntraChunkPath(ChunkNavIntraPathfinder.IntraPath intraPath,
                                         MCache.Grid grid, Coord2d finalTarget,
                                         NGameUI gui) throws InterruptedException {
        int skipInterval = Math.max(1, intraPath.size() / 10);

        for (int i = skipInterval; i < intraPath.localPath.size(); i += skipInterval) {
            Coord localCoord = intraPath.localPath.get(i);
            Coord tileCoord = grid.ul.add(localCoord);
            Coord2d worldCoord = tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);

            PathFinder waypointPf = new PathFinder(worldCoord);
            waypointPf.run(gui);

            Gob player = gui.map.player();
            if (player != null && player.rc.dist(finalTarget) < MCache.tilesz.x * 15) {
                PathFinder directPf = new PathFinder(finalTarget);
                Results directResult = directPf.run(gui);
                if (directResult.IsSuccess()) {
                    return Results.SUCCESS();
                }
            }
        }

        PathFinder finalPf = new PathFinder(finalTarget);
        return finalPf.run(gui);
    }

    private Results traversePortal(ChunkPortal portal, long gridId, NGameUI gui) throws InterruptedException {
        log("traversePortal(%s) in grid %d", portal.gobName, gridId);

        Coord2d accessPoint = getPortalAccessPoint(portal, gridId, gui);
        if (accessPoint != null) {
            log("Navigating to access point %s", accessPoint);

            Results navResult = walkTowardTarget(accessPoint, gui, WalkConfig.DEFAULT);

            if (!navResult.IsSuccess()) {
                log("Could not reach exact access point, continuing anyway");
            }

            Thread.sleep(200);
        } else {
            log("No access point available for portal %s", portal.gobName);
        }

        Gob portalGob = findPortalGobByPosition(portal, gridId, gui);
        if (portalGob == null) {
            log("Portal not found by position, trying hash: %s", portal.gobHash);
            portalGob = Finder.findGob(portal.gobHash);
        }
        if (portalGob == null) {
            log("Portal not found by hash, trying closest by name: %s", portal.gobName);
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
        log("Found portal gob: %s at %s", portalGob.ngob.name, portalGob.rc);

        if (portal.requiresInteraction) {
            if (manager != null && manager.getPortalTracker() != null) {
                manager.getPortalTracker().setClickedPortal(portalGob);
            }

            NUtils.openDoorOnAGob(gui, portalGob);

            if (isLoadingPortal(portal.type)) {
                String exitGobName = GateDetector.getDoorPair(portal.gobName);

                NUtils.getUI().core.addTask(new WaitForMapLoad());

                if (exitGobName != null) {
                    NUtils.getUI().core.addTask(new WaitForExitPortal(exitGobName));
                }

                NUtils.getUI().core.addTask(new WaitForGobStability());
            } else {
                NUtils.getUI().core.addTask(new WaitGobModelAttrChange(portalGob, portalGob.ngob.getModelAttribute()));
            }
        }

        return Results.SUCCESS();
    }

    private Gob findPortalGobByPosition(ChunkPortal portal, long gridId, NGameUI gui) {
        if (portal.localCoord == null) {
            return null;
        }

        Coord2d expectedPos = getPortalAccessPoint(portal, gridId, gui);
        if (expectedPos == null) {
            return null;
        }

        Collection<Gob> candidates = Finder.findGobs(new NAlias(portal.gobName));
        if (candidates == null || candidates.isEmpty()) {
            log("No gobs found with name: %s", portal.gobName);
            return null;
        }

        log("Found %d candidates for %s, looking near %s", candidates.size(), portal.gobName, expectedPos);

        Gob closest = null;
        double closestDist = Double.MAX_VALUE;
        double maxDistance = MCache.tilesz.x * 15;

        for (Gob gob : candidates) {
            double dist = gob.rc.dist(expectedPos);
            log("  Candidate at %s dist=%.1f", gob.rc, dist);
            if (dist < closestDist && dist < maxDistance) {
                closestDist = dist;
                closest = gob;
            }
        }

        if (closest != null) {
            log("Best match at %s dist=%.1f", closest.rc, closestDist);
        }

        return closest;
    }

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

    private Coord2d getPortalAccessPoint(ChunkPortal portal, long gridId, NGameUI gui) {
        if (portal.localCoord == null) {
            return null;
        }

        try {
            MCache mcache = gui.map.glob.map;

            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == gridId) {
                        Coord tileCoord = grid.ul.add(portal.localCoord);
                        return tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
                    }
                }
            }

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

    private Results replanAndContinue(NGameUI gui, int failedWaypointIndex) throws InterruptedException {
        if (targetArea == null) {
            return Results.FAIL();
        }

        long currentChunkId = graph.getPlayerChunkId();
        if (currentChunkId == -1) {
            return Results.FAIL();
        }

        ChunkNavPlanner planner = new ChunkNavPlanner(graph);
        ChunkPath newPath = planner.planToArea(targetArea);

        if (newPath == null || newPath.isEmpty()) {
            return Results.FAIL();
        }

        ChunkNavExecutor newExecutor = new ChunkNavExecutor(newPath, targetArea, manager);
        newExecutor.replanAttempts = this.replanAttempts;
        return newExecutor.run(gui);
    }

    private void tickPortalTracker() {
        if (manager != null) {
            try {
                manager.tick();
            } catch (Exception e) {
                // Ignore errors
            }
        }
    }

    private List<Coord2d> getAllAreaEdgePoints(Pair<Coord2d, Coord2d> bounds) {
        List<Coord2d> points = new ArrayList<>();
        Coord2d areaMin = bounds.a;
        Coord2d areaMax = bounds.b;

        double offset = MCache.tilesz.x;
        double centerX = (areaMin.x + areaMax.x) / 2;
        double centerY = (areaMin.y + areaMax.y) / 2;

        points.add(new Coord2d(areaMin.x - offset, centerY));
        points.add(new Coord2d(areaMax.x + offset, centerY));
        points.add(new Coord2d(centerX, areaMin.y - offset));
        points.add(new Coord2d(centerX, areaMax.y + offset));

        return points;
    }

    private static class WaitForMapLoad extends NTask {
        private long startTime;
        private static final long TIMEOUT_MS = PORTAL_LOAD_TIMEOUT_MS;

        public boolean check() {
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                return true;
            }

            try {
                Gob player = NUtils.player();
                if (player != null) {
                    return System.currentTimeMillis() - startTime > 2000;
                }
            } catch (Exception e) {
                // Still loading
            }

            return false;
        }
    }

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

            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                return true;
            }

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
