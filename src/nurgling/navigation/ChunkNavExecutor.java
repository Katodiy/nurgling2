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

/**
 * Executes a ChunkPath using PathFinder for local navigation.
 * Handles portal traversals (doors, stairs) along the way.
 */
public class ChunkNavExecutor implements Action {
    private final ChunkPath path;
    private final NArea targetArea;
    private final ChunkNavGraph graph;
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
        this.manager = manager;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (gui == null || gui.map == null || path == null) {
            return Results.FAIL();
        }

        Gob player = gui.map.player();
        if (player == null) {
            return Results.FAIL();
        }

        if (path.hasDetailedPath()) {
            return followDetailedPath(gui);
        }

        if (!path.isEmpty()) {
            return followWaypointPath(gui);
        }

        if (path.isEmpty() && targetArea != null) {
            return navigateToTargetArea(gui);
        }

        return Results.FAIL();
    }

    private Results followDetailedPath(NGameUI gui) throws InterruptedException {
        int segmentIndex = 0;
        Layer currentLayer = getCurrentPlayerLayer();
        long playerGridId = graph.getPlayerChunkId();
        System.out.println("ChunkNav: Starting path execution. Player in grid " + playerGridId + ", layer " + currentLayer);

        for (ChunkPath.PathSegment segment : path.segments) {
            segmentIndex++;
            playerGridId = graph.getPlayerChunkId();

            ChunkNavData segmentChunk = graph.getChunk(segment.gridId);
            Layer segmentLayer = segmentChunk != null ? Layer.fromString(segmentChunk.layer) : Layer.OUTSIDE;
            boolean sameLayer = segmentLayer == currentLayer;

            // Calculate targetGridId for PORTAL segments (the grid we'll be in after the portal)
            long targetGridId = -1;
            if (segment.type == ChunkPath.SegmentType.PORTAL && segmentIndex < path.segments.size()) {
                ChunkPath.PathSegment nextSegment = path.segments.get(segmentIndex);
                targetGridId = nextSegment.gridId;
            }

            System.out.println("ChunkNav: Segment " + segmentIndex + "/" + path.segments.size() +
                " - type=" + segment.type +
                ", segmentGridId=" + segment.gridId +
                ", playerGridId=" + playerGridId +
                ", sameLayer=" + sameLayer +
                " (player=" + currentLayer + ", segment=" + segmentLayer + ")" +
                (targetGridId != -1 ? ", targetGridId=" + targetGridId : ""));

            // Track portal gob found during walking (for PORTAL segments)
            Gob portalGobFromWalk = null;

            if (sameLayer) {
                SegmentWalkResult segResult = followSegmentTiles(segment, gui, targetGridId);
                if (!segResult.result.IsSuccess()) {
                    System.out.println("ChunkNav: Segment " + segmentIndex + " FAILED (sameLayer walk)");
                    return Results.FAIL();
                }
                portalGobFromWalk = segResult.portalGob;
            } else if (segment.type == ChunkPath.SegmentType.PORTAL) {
                // Cross-layer segment that leads to a portal - skip the walk, we'll traverse portal next
                System.out.println("ChunkNav: Skipping cross-layer PORTAL segment walk, will traverse portal");
            } else {
                // Cross-layer WALK segment - we likely just traversed a portal and are already in target layer
                System.out.println("ChunkNav: Cross-layer WALK segment, attempting walk anyway");
                SegmentWalkResult segResult = followSegmentTiles(segment, gui, -1);
                if (!segResult.result.IsSuccess()) {
                    System.out.println("ChunkNav: Segment " + segmentIndex + " FAILED (cross-layer walk)");
                    return Results.FAIL();
                }
            }

            if (segment.type == ChunkPath.SegmentType.PORTAL) {
                tickPortalTracker();

                // If we found portal gob during walking, use it directly
                if (portalGobFromWalk != null) {
                    System.out.println("ChunkNav: Using portal gob found during walk: " +
                        (portalGobFromWalk.ngob != null ? portalGobFromWalk.ngob.name : "unknown"));
                    traversePortalGob(gui, portalGobFromWalk);
                } else {
                    System.out.println("ChunkNav: Traversing portal to target grid " + targetGridId);
                    findAndTraversePortalToGrid(gui, segment, targetGridId);
                }

                currentLayer = getCurrentPlayerLayer();
                playerGridId = graph.getPlayerChunkId();
                System.out.println("ChunkNav: After portal - now in grid " + playerGridId + ", layer " + currentLayer);
                tickPortalTracker();
            }

            tickPortalTracker();
        }

        System.out.println("ChunkNav: Path execution completed successfully");
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
        return Layer.OUTSIDE;
    }

    /**
     * Find and traverse portal using priority-based candidate ranking.
     */
    private Results findAndTraversePortalToGrid(NGameUI gui, ChunkPath.PathSegment segment, long targetGridId) throws InterruptedException {
        Gob player = gui.map.player();
        if (player == null) return Results.FAIL();

        ChunkNavData sourceChunk = graph.getChunk(segment.gridId);
        Layer sourceLayer = sourceChunk != null ? Layer.fromString(sourceChunk.layer) : Layer.OUTSIDE;
        ChunkNavData targetChunk = graph.getChunk(targetGridId);
        Layer targetLayer = targetChunk != null ? Layer.fromString(targetChunk.layer) : Layer.UNKNOWN;

        String expectedPortalType = getExpectedPortalType(sourceLayer, targetLayer);

        ChunkNavData chunk = graph.getChunk(segment.gridId);
        if (chunk != null && !chunk.portals.isEmpty()) {
            List<ChunkPortal> portalsCopy = new ArrayList<>(chunk.portals);

            // Rank all portals by priority score
            List<ScoredPortal> rankedPortals = rankPortalCandidates(
                portalsCopy, expectedPortalType, targetGridId, targetLayer);

            // Try portals in priority order
            for (ScoredPortal scored : rankedPortals) {
                Results result = tryTraversePortal(gui, player, scored.portal);
                if (result != null) return result;
            }
        }

        // Fallback: Look for common portal gob patterns
        Gob nearestPortal = findNearestPortalGob(gui, player);
        if (nearestPortal != null) {
            return traversePortalGob(gui, nearestPortal);
        }

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

    /**
     * Find a visible portal gob that connects to the target grid.
     * Checks recorded portals in the current chunk to find one that leads to targetGridId.
     * Returns the gob if it's currently visible to the player.
     * Matches by gobHash (name + gridId + position) for accuracy.
     */
    private Gob findVisiblePortalForTargetGrid(NGameUI gui, long currentGridId, long targetGridId) {
        if (targetGridId == -1) return null;

        ChunkNavData currentChunk = graph.getChunk(currentGridId);
        if (currentChunk == null || currentChunk.portals.isEmpty()) return null;

        Gob player = gui.map.player();
        if (player == null) return null;

        // Find portals that connect to the target grid
        for (ChunkPortal portal : currentChunk.portals) {
            if (portal.connectsToGridId != targetGridId) continue;
            if (portal.gobHash == null) continue;

            // Try to find this portal gob by hash (most accurate - uses name + gridId + position)
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    if (gob.ngob == null || gob.ngob.hash == null) continue;

                    // Match by gobHash - this is based on name + gridId + position
                    if (gob.ngob.hash.equals(portal.gobHash)) {
                        double dist = player.rc.dist(gob.rc);
                        if (dist < MCache.tilesz.x * 25) {
                            System.out.println("ChunkNav: Found visible portal gob by hash '" +
                                (gob.ngob.name != null ? gob.ngob.name : "unknown") +
                                "' at dist=" + String.format("%.1f", dist) +
                                " connecting to target grid " + targetGridId);
                            return gob;
                        }
                    }
                }
            }
        }

        return null;
    }

    private String getExpectedPortalType(Layer sourceLayer, Layer targetLayer) {
        if (sourceLayer == null || targetLayer == null) return null;

        // Inside -> outside: use -door suffix (exiting building)
        if (sourceLayer == Layer.INSIDE && targetLayer == Layer.OUTSIDE) {
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

        // For OUTSIDE -> OUTSIDE transitions (including mines), the portal type is
        // determined by the recorded portal connection, not by layer comparison
        return null;
    }

    private Results tryTraversePortal(NGameUI gui, Gob player, ChunkPortal recordedPortal) throws InterruptedException {
        Gob portalGob = findGobByName(gui, recordedPortal.gobName, player.rc, MCache.tilesz.x * 30);
        if (portalGob == null) {
            return null;
        }

        // For buildings, navigate to door access point first
        Coord2d accessPoint = getPortalAccessPoint(portalGob);
        if (accessPoint != null) {
            System.out.println("ChunkNav: Navigating to building door access point: " + accessPoint);
            PathFinder accessPf = new PathFinder(accessPoint);
            Results accessResult = accessPf.run(gui);
            if (!accessResult.IsSuccess()) {
                System.out.println("ChunkNav: Failed to reach building door access point, trying direct approach");
                // Fall back to direct approach
                PathFinder directPf = new PathFinder(portalGob);
                if (!directPf.run(gui).IsSuccess()) {
                    return null;
                }
            }
        } else {
            // Non-building portals - walk directly to the gob
            PathFinder pf = new PathFinder(portalGob);
            Results walkResult = pf.run(gui);
            if (!walkResult.IsSuccess()) {
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

        // Portal recording is handled automatically by PortalTraversalTracker via getLastActions()
        // No need to explicitly set the clicked portal - the tracker will detect it

        // Determine if this is a "loading" portal (cellar, stairs, mine) that needs rclickGob
        // vs a regular door that uses openDoorOnAGob
        boolean isLoadingPortal = portalName.contains("cellar") ||
                                   portalName.contains("stairs") ||
                                   portalName.contains("ladder") ||
                                   portalName.contains("minehole");

        if (isLoadingPortal) {
            // Cellar doors, stairs, ladders, mines - use simple right-click to enter
            NUtils.rclickGob(portalGob);
        } else {
            // Regular doors (building entrances) - use openDoorOnAGob
            NUtils.openDoorOnAGob(gui, portalGob);
        }

        NUtils.getUI().core.addTask(new WaitForMapLoad());
        NUtils.getUI().core.addTask(new WaitForGobStability());

        return Results.SUCCESS();
    }

    /**
     * Check if a gob is a building (has door on one side).
     */
    private boolean isBuildingGob(String gobName) {
        if (gobName == null) return false;
        String lower = gobName.toLowerCase();
        return lower.contains("stonemansion") ||
               lower.contains("logcabin") ||
               lower.contains("timberhouse") ||
               lower.contains("stonestead") ||
               lower.contains("greathall") ||
               lower.contains("stonetower") ||
               lower.contains("windmill") ||
               lower.contains("primitivetent");
    }

    /**
     * Check if a gob is an interior door (door gob inside a building).
     */
    private boolean isInteriorDoorGob(String gobName) {
        if (gobName == null) return false;
        String lower = gobName.toLowerCase();
        // Interior doors have "-door" suffix (e.g., stonemansion-door, logcabin-door)
        return lower.contains("-door");
    }

    /**
     * Calculate the access point in front of a portal gob's door.
     * - For buildings: door is on +x side of hitbox, access point is 1 tile in front
     * - For interior doors: access point is 1 tile in front using gob.a direction
     *
     * @param portalGob The portal gob (building or interior door)
     * @return Access point coordinates, or null if not applicable
     */
    private Coord2d getPortalAccessPoint(Gob portalGob) {
        if (portalGob == null || portalGob.ngob == null) return null;

        String name = portalGob.ngob.name;

        // Interior doors (1x1 gobs) - access point is 1 tile in front (from inside)
        // Door faces outward, so we need to go OPPOSITE direction to stand inside
        if (isInteriorDoorGob(name)) {
            double angle = portalGob.a;
            double accessOffset = MCache.tilesz.x;  // 1 tile in front of door
            // Use negative offset to go opposite direction (inside the building)
            double offsetX = -Math.cos(angle) * accessOffset;
            double offsetY = -Math.sin(angle) * accessOffset;

            Coord2d accessPoint = new Coord2d(portalGob.rc.x + offsetX, portalGob.rc.y + offsetY);

            System.out.println("ChunkNav: Interior door '" + name + "' access point calculated:" +
                " center=" + portalGob.rc +
                ", angle=" + String.format("%.2f", angle) +
                ", accessPoint=" + accessPoint);

            return accessPoint;
        }

        // Upstairs/downstairs - access point is 1.5 tiles away
        // upstairs: positive offset (approach from bottom, same as facing direction)
        // downstairs: negative offset (approach from top, opposite of facing direction)
        String lower = name.toLowerCase();
        if (lower.contains("upstairs") || lower.contains("downstairs")) {
            double angle = portalGob.a;
            double accessOffset = MCache.tilesz.x * 1.5;  // 1.5 tiles

            // Downstairs needs negative offset (approach from opposite side)
            boolean isDownstairs = lower.contains("downstairs");
            double sign = isDownstairs ? -1.0 : 1.0;

            double offsetX = sign * Math.cos(angle) * accessOffset;
            double offsetY = sign * Math.sin(angle) * accessOffset;

            Coord2d accessPoint = new Coord2d(portalGob.rc.x + offsetX, portalGob.rc.y + offsetY);

            System.out.println("ChunkNav: Stairs '" + name + "' access point calculated:" +
                " center=" + portalGob.rc +
                ", angle=" + String.format("%.2f", angle) +
                ", sign=" + sign +
                ", accessPoint=" + accessPoint);

            return accessPoint;
        }

        // Buildings - door is on +x side of hitbox
        if (isBuildingGob(name)) {
            nurgling.NHitBox hitBox = portalGob.ngob.hitBox;
            if (hitBox == null) return null;

            // Distance from center to door edge + 1 tile for access point
            double doorEdgeOffset = hitBox.end.x;  // Distance to door edge
            double accessOffset = doorEdgeOffset + MCache.tilesz.x;  // +1 tile in front of door

            double angle = portalGob.a;
            double offsetX = Math.cos(angle) * accessOffset;
            double offsetY = Math.sin(angle) * accessOffset;

            Coord2d accessPoint = new Coord2d(portalGob.rc.x + offsetX, portalGob.rc.y + offsetY);

            System.out.println("ChunkNav: Building '" + name + "' door access point calculated:" +
                " center=" + portalGob.rc +
                ", angle=" + String.format("%.2f", angle) +
                ", doorEdge=" + doorEdgeOffset +
                ", accessPoint=" + accessPoint);

            return accessPoint;
        }

        return null;
    }

    /**
     * Result class for followSegmentTiles that includes portal gob if found during walking.
     */
    private static class SegmentWalkResult {
        final Results result;
        final Gob portalGob;  // Portal gob found during walk (for PORTAL segments)

        SegmentWalkResult(Results result, Gob portalGob) {
            this.result = result;
            this.portalGob = portalGob;
        }

        static SegmentWalkResult success() {
            return new SegmentWalkResult(Results.SUCCESS(), null);
        }

        static SegmentWalkResult successWithPortal(Gob portal) {
            return new SegmentWalkResult(Results.SUCCESS(), portal);
        }

        static SegmentWalkResult fail() {
            return new SegmentWalkResult(Results.FAIL(), null);
        }
    }

    private SegmentWalkResult followSegmentTiles(ChunkPath.PathSegment segment, NGameUI gui, long targetGridId) throws InterruptedException {
        if (segment.isEmpty()) {
            System.out.println("ChunkNav: followSegmentTiles - segment is empty, returning SUCCESS");
            return SegmentWalkResult.success();
        }

        ChunkNavData segmentChunk = graph.getChunk(segment.gridId);

        // Get LIVE worldTileOrigin from MCache
        Coord liveWorldTileOrigin = null;
        try {
            MCache mcache = gui.map.glob.map;
            synchronized (mcache.grids) {
                System.out.println("ChunkNav: Searching MCache for gridId " + segment.gridId + " (MCache has " + mcache.grids.size() + " grids)");
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == segment.gridId) {
                        liveWorldTileOrigin = grid.ul;
                        System.out.println("ChunkNav: Found grid in MCache! worldTileOrigin=" + liveWorldTileOrigin);
                        break;
                    }
                }
                if (liveWorldTileOrigin == null) {
                    System.out.println("ChunkNav: Grid " + segment.gridId + " NOT found in MCache. Available grids:");
                    for (MCache.Grid grid : mcache.grids.values()) {
                        System.out.println("  - Grid " + grid.id + " at " + grid.ul);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("ChunkNav: Exception searching MCache: " + e.getMessage());
        }

        // Determine which worldTileOrigin to use
        Coord currentWorldTileOrigin = liveWorldTileOrigin;
        if (currentWorldTileOrigin == null && segmentChunk != null && segmentChunk.worldTileOrigin != null) {
            currentWorldTileOrigin = segmentChunk.worldTileOrigin;
            System.out.println("ChunkNav: Using stored chunk worldTileOrigin: " + currentWorldTileOrigin);
        } else if (currentWorldTileOrigin == null && segment.worldTileOrigin != null) {
            currentWorldTileOrigin = segment.worldTileOrigin;
            System.out.println("ChunkNav: Using segment worldTileOrigin: " + currentWorldTileOrigin);
        }

        if (currentWorldTileOrigin == null) {
            System.out.println("ChunkNav: FAIL - no worldTileOrigin available! " +
                "liveWorldTileOrigin=" + liveWorldTileOrigin +
                ", segmentChunk=" + (segmentChunk != null ? "exists" : "null") +
                ", segmentChunk.worldTileOrigin=" + (segmentChunk != null ? segmentChunk.worldTileOrigin : "N/A") +
                ", segment.worldTileOrigin=" + segment.worldTileOrigin);
            return SegmentWalkResult.fail();
        }

        // For PORTAL segments, check if we should look for portal gob during walking
        boolean isPortalSegment = segment.type == ChunkPath.SegmentType.PORTAL && targetGridId != -1;

        // Navigate through tile-level waypoints instead of just jumping to the end
        double tileSize = MCache.tilesz.x;
        int waypointInterval = 20;
        int currentStepIndex = 0;

        System.out.println("ChunkNav: Walking segment with " + segment.steps.size() + " steps, worldTileOrigin=" + currentWorldTileOrigin);

        while (currentStepIndex < segment.steps.size()) {
            Gob player = gui.map.player();
            if (player == null) return SegmentWalkResult.fail();

            // For PORTAL segments, check if portal gob is now visible
            if (isPortalSegment) {
                Gob visiblePortal = findVisiblePortalForTargetGrid(gui, segment.gridId, targetGridId);
                if (visiblePortal != null) {
                    // For buildings, navigate to door access point instead of gob center
                    Coord2d accessPoint = getPortalAccessPoint(visiblePortal);
                    if (accessPoint != null) {
                        System.out.println("ChunkNav: Building portal visible! Navigating to door access point: " + accessPoint);
                        PathFinder accessPf = new PathFinder(accessPoint);
                        Results accessResult = accessPf.run(gui);
                        if (accessResult.IsSuccess()) {
                            System.out.println("ChunkNav: Reached building door access point, returning with portal gob");
                            return SegmentWalkResult.successWithPortal(visiblePortal);
                        } else {
                            System.out.println("ChunkNav: Failed to reach building door access point, continuing walk");
                        }
                    } else {
                        // Non-building portal - pathfind directly to gob
                        System.out.println("ChunkNav: Portal gob visible! Switching to PathFinder(gob) for final approach");
                        PathFinder portalPf = new PathFinder(visiblePortal);
                        Results portalResult = portalPf.run(gui);
                        if (portalResult.IsSuccess()) {
                            System.out.println("ChunkNav: PathFinder(gob) succeeded, returning with portal gob");
                            return SegmentWalkResult.successWithPortal(visiblePortal);
                        } else {
                            System.out.println("ChunkNav: PathFinder(gob) failed, continuing with coordinate-based walk");
                        }
                    }
                }
            }

            int targetIndex = Math.min(currentStepIndex + waypointInterval, segment.steps.size() - 1);
            ChunkPath.TileStep targetStep = segment.steps.get(targetIndex);
            Coord worldTile = currentWorldTileOrigin.add(targetStep.localCoord);
            Coord2d waypoint = worldTile.mul(MCache.tilesz).add(MCache.tilehsz);

            double distToWaypoint = player.rc.dist(waypoint);

            // Skip if already close enough
            if (distToWaypoint < tileSize * 1.5) {
                currentStepIndex = targetIndex + 1;
                continue;
            }

            System.out.println("ChunkNav: Step " + currentStepIndex + "->" + targetIndex +
                ", localCoord=" + targetStep.localCoord +
                ", worldTile=" + worldTile +
                ", waypoint=" + waypoint +
                ", dist=" + String.format("%.1f", distToWaypoint));

            // Try PathFinder first
            PathFinder pf = new PathFinder(waypoint);
            Results pfResult = pf.run(gui);

            if (pfResult.IsSuccess()) {
                currentStepIndex = targetIndex + 1;
                continue;
            }

            System.out.println("ChunkNav: PathFinder failed for waypoint " + waypoint + ", trying smaller steps...");

            // PathFinder failed - try smaller steps along the PATH
            boolean madeProgress = false;
            for (int midIndex = currentStepIndex + 5; midIndex < targetIndex; midIndex += 5) {
                ChunkPath.TileStep midStep = segment.steps.get(midIndex);
                Coord midWorldTile = currentWorldTileOrigin.add(midStep.localCoord);
                Coord2d midWaypoint = midWorldTile.mul(MCache.tilesz).add(MCache.tilehsz);

                double midDist = player.rc.dist(midWaypoint);
                if (midDist < tileSize * 1.5) continue;

                PathFinder midPf = new PathFinder(midWaypoint);
                if (midPf.run(gui).IsSuccess()) {
                    currentStepIndex = midIndex + 1;
                    madeProgress = true;
                    break;
                }
            }

            if (!madeProgress) {
                // Try single tile steps as last resort
                for (int singleIndex = currentStepIndex + 1; singleIndex <= targetIndex; singleIndex++) {
                    ChunkPath.TileStep singleStep = segment.steps.get(singleIndex);
                    Coord singleWorldTile = currentWorldTileOrigin.add(singleStep.localCoord);
                    Coord2d singleWaypoint = singleWorldTile.mul(MCache.tilesz).add(MCache.tilehsz);

                    double singleDist = player.rc.dist(singleWaypoint);
                    if (singleDist < tileSize * 1.5) {
                        currentStepIndex = singleIndex + 1;
                        madeProgress = true;
                        break;
                    }

                    if (new PathFinder(singleWaypoint).run(gui).IsSuccess()) {
                        currentStepIndex = singleIndex + 1;
                        madeProgress = true;
                        break;
                    }
                }
            }

            if (!madeProgress) {
                System.out.println("ChunkNav: FAIL - no progress possible. Stuck at step " + currentStepIndex +
                    ", player at " + player.rc + ", target waypoint " + waypoint);
                return SegmentWalkResult.fail();
            }
        }

        System.out.println("ChunkNav: Segment walk completed successfully");
        return SegmentWalkResult.success();
    }

    /**
     * Unified method for incremental navigation toward a target.
     * Consolidates navigateIncrementally, navigateIncrementallyToSegmentEnd, and navigateToCoordStepByStep.
     */
    private Results walkTowardTarget(Coord2d target, NGameUI gui, WalkConfig config) throws InterruptedException {
        double tileSize = MCache.tilesz.x;
        double stepDistance = tileSize * config.stepDistanceTiles;

        for (int step = 0; step < config.maxSteps; step++) {
            Gob player = gui.map.player();
            if (player == null) return Results.FAIL();

            double distToTarget = player.rc.dist(target);

            // Close enough - we're done
            if (distToTarget < tileSize * config.closeEnoughTiles) {
                return Results.SUCCESS();
            }

            // Try direct path first if configured
            if (config.tryDirectFirst) {
                PathFinder directPf = new PathFinder(target);
                Results directResult = directPf.run(gui);
                if (directResult.IsSuccess()) {
                    return Results.SUCCESS();
                }
            }

            // Walk incrementally toward target
            Coord2d direction = target.sub(player.rc).norm();
            double walkDist = Math.min(stepDistance, distToTarget * 0.5);
            Coord2d intermediateTarget = player.rc.add(direction.mul(walkDist));

            PathFinder stepPf = new PathFinder(intermediateTarget);
            Results stepResult = stepPf.run(gui);

            if (!stepResult.IsSuccess()) {
                // Try shorter step
                walkDist = walkDist / 2;
                if (walkDist < tileSize * 2) {
                    return Results.FAIL();
                }
                intermediateTarget = player.rc.add(direction.mul(walkDist));
                stepPf = new PathFinder(intermediateTarget);
                stepResult = stepPf.run(gui);

                if (!stepResult.IsSuccess()) {
                    return Results.FAIL();
                }
            }

            Thread.sleep(100);
        }

        return Results.FAIL();
    }

    private Results followWaypointPath(NGameUI gui) throws InterruptedException {
        for (int i = 0; i < path.waypoints.size(); i++) {
            ChunkPath.ChunkWaypoint waypoint = path.waypoints.get(i);

            if (waypoint.portal != null && waypoint.type == ChunkPath.WaypointType.PORTAL_ENTRY) {
                tickPortalTracker();

                Results portalResult = traversePortal(waypoint.portal, waypoint.gridId, gui);
                if (!portalResult.IsSuccess()) {
                    gui.error("ChunkNav: Portal traversal failed");
                    return Results.FAIL();
                }

                tickPortalTracker();
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
                        return replanAndContinue(gui);
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

        Pair<Coord2d, Coord2d> areaBounds = targetArea.getRCArea();
        Coord2d areaCenter = targetArea.getCenter2d();

        if (areaBounds == null && areaCenter == null) {
            return Results.FAIL();
        }

        if (areaBounds == null) {
            Results walkResult = walkTowardTarget(areaCenter, gui, WalkConfig.DEFAULT);
            if (!walkResult.IsSuccess()) {
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
            return Results.FAIL();
        }

        Gob currentPlayer = gui.map.player();
        if (currentPlayer == null) {
            return Results.FAIL();
        }
        Coord2d playerPos = currentPlayer.rc;

        List<Coord2d> edgePoints = getAllAreaEdgePoints(areaBounds);
        edgePoints.sort(Comparator.comparingDouble(p -> p.dist(playerPos)));

        for (Coord2d edgePoint : edgePoints) {
            Results edgeResult = walkTowardTarget(edgePoint, gui, WalkConfig.DEFAULT);
            if (edgeResult.IsSuccess()) {
                return Results.SUCCESS();
            }
        }

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
        Coord2d accessPoint = getPortalAccessPoint(portal, gridId, gui);
        if (accessPoint != null) {
            walkTowardTarget(accessPoint, gui, WalkConfig.DEFAULT);
            Thread.sleep(200);
        }

        Gob portalGob = findPortalGobByPosition(portal, gridId, gui);
        if (portalGob == null) {
            portalGob = Finder.findGob(portal.gobHash);
        }
        if (portalGob == null) {
            Collection<Gob> candidates = Finder.findGobs(new NAlias(portal.gobName));
            if (!candidates.isEmpty()) {
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

        // Portal recording is handled automatically by PortalTraversalTracker via getLastActions()
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
        if (candidates.isEmpty()) {
            return null;
        }

        Gob closest = null;
        double closestDist = Double.MAX_VALUE;
        double maxDistance = MCache.tilesz.x * 15;

        for (Gob gob : candidates) {
            double dist = gob.rc.dist(expectedPos);
            if (dist < closestDist && dist < maxDistance) {
                closestDist = dist;
                closest = gob;
            }
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
            Coord2d basePosition = null;

            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == gridId) {
                        Coord tileCoord = grid.ul.add(portal.localCoord);
                        basePosition = tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
                        break;
                    }
                }
            }

            if (basePosition == null) {
                ChunkNavData chunk = graph.getChunk(gridId);
                if (chunk != null && chunk.worldTileOrigin != null) {
                    Coord tileCoord = chunk.worldTileOrigin.add(portal.localCoord);
                    basePosition = tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
                }
            }

            if (basePosition == null) {
                return null;
            }

            // For building-type portals, calculate an access point in front of the door
            // using the gob's facing angle (like the routes system does for mineholes)
            double accessOffset = getBuildingAccessOffset(portal.gobName);
            if (accessOffset > 0) {
                // Find the actual portal gob to get its facing angle
                Gob portalGob = findGobByName(gui, portal.gobName, basePosition, MCache.tilesz.x * 10);
                if (portalGob == null) {
                    portalGob = Finder.findGob(portal.gobHash);
                }

                if (portalGob != null) {
                    // Use the gob's angle to calculate access point in front of the door
                    // The angle 'a' is the direction the building faces
                    double angle = portalGob.a;
                    double offsetPixels = accessOffset * MCache.tilesz.x;
                    return new Coord2d(
                        basePosition.x + Math.cos(angle) * offsetPixels,
                        basePosition.y + Math.sin(angle) * offsetPixels
                    );
                }
            }

            return basePosition;
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * Get the access offset for a building type.
     * This is how many tiles in front of the door the player should stand.
     * Returns 0 for non-building portals (use door position directly).
     */
    private double getBuildingAccessOffset(String gobName) {
        if (gobName == null) return 0;
        String lower = gobName.toLowerCase();

        // Interior doors (seen from inside) - no offset needed, walk directly to door
        if (lower.contains("-door")) return 0;

        // For exterior building gobs, we need to stand OUTSIDE the door
        // Offset by 2-3 tiles in the direction the building faces
        if (lower.contains("stonemansion")) return 6;
        if (lower.contains("logcabin")) return 2;
        if (lower.contains("timberhouse")) return 2;
        if (lower.contains("stonestead")) return 2;
        if (lower.contains("greathall")) return 3;
        if (lower.contains("stonetower")) return 2;
        if (lower.contains("windmill")) return 2;
        if (lower.contains("primitivetent")) return 2;

        // Mineholes also need an offset (like routes system does)
        if (lower.contains("minehole")) return 2;

        return 0;
    }

    private Results replanAndContinue(NGameUI gui) throws InterruptedException {
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
