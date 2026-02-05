package nurgling.navigation;

import haven.Coord2d;
import haven.Pair;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.PathFinder;
import nurgling.areas.NArea;

/**
 * Helper utilities for area navigation.
 * Provides methods for checking area reachability and finding optimal paths to area corners.
 */
public class AreaNavigationHelper {
    
    /**
     * Get the 4 corners of an area as Coord2d array.
     * First tries live getRCArea(), then falls back to stored ChunkNav data.
     * @return array of 4 corners [top-left, bottom-right, bottom-left, top-right], or null if area bounds unavailable
     */
    public static Coord2d[] getAreaCorners(NArea area) {
        if (area == null) {
            return null;
        }
        
        // Try live getRCArea() first (works when area is visible)
        Pair<Coord2d, Coord2d> rcArea = area.getRCArea();
        
        if (rcArea == null) {
            // Fallback: try to get from stored ChunkNav data
            rcArea = getAreaCornersFromStoredData(area);
        }
        
        if (rcArea == null) {
            return null;
        }
        
        return new Coord2d[] {
            rcArea.a,                                        // top-left
            rcArea.b,                                        // bottom-right
            Coord2d.of(rcArea.a.x, rcArea.b.y),             // bottom-left
            Coord2d.of(rcArea.b.x, rcArea.a.y)              // top-right
        };
    }
    
    /**
     * Get area bounds from stored ChunkNav data when area is not visible.
     * Uses worldTileOrigin from recorded chunks to calculate world coordinates.
     */
    private static Pair<Coord2d, Coord2d> getAreaCornersFromStoredData(NArea area) {
        if (area == null) {
            return null;
        }
        if (area.space == null || area.space.space == null || area.space.space.isEmpty()) {
            return null;
        }
        
        try {
            // Get ChunkNavManager
            if (NUtils.getGameUI() == null || NUtils.getGameUI().map == null) {
                return null;
            }
            
            NMapView mapView = (NMapView) NUtils.getGameUI().map;
            ChunkNavManager chunkNav = mapView.getChunkNavManager();
            if (chunkNav == null || !chunkNav.isInitialized()) {
                return null;
            }
            
            ChunkNavGraph graph = chunkNav.getGraph();
            if (graph == null) {
                return null;
            }
            
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            int foundChunks = 0;
            
            for (java.util.Map.Entry<Long, NArea.VArea> entry : area.space.space.entrySet()) {
                long gridId = entry.getKey();
                NArea.VArea varea = entry.getValue();
                
                if (varea == null || varea.area == null) {
                    continue;
                }
                
                // Try to get worldTileOrigin from stored chunk data
                ChunkNavData chunk = graph.getChunk(gridId);
                if (chunk == null || chunk.worldTileOrigin == null) {
                    continue;
                }
                
                // Calculate world tile coordinates
                haven.Coord ul = chunk.worldTileOrigin.add(varea.area.ul);
                haven.Coord br = chunk.worldTileOrigin.add(varea.area.br);
                
                minX = Math.min(minX, ul.x);
                minY = Math.min(minY, ul.y);
                maxX = Math.max(maxX, br.x);
                maxY = Math.max(maxY, br.y);
                foundChunks++;
            }
            
            if (foundChunks == 0) {
                return null;
            }
            
            // Convert tile coords to world coords
            Coord2d begin = new haven.Coord(minX, minY).mul(haven.MCache.tilesz);
            Coord2d end = new haven.Coord(maxX - 1, maxY - 1).mul(haven.MCache.tilesz).add(haven.MCache.tilesz);
            
            return new Pair<>(begin, end);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Find the shortest path to any of the 4 corners of an area.
     * Plans paths to all corners in parallel and returns the shortest one.
     * Uses planToAreaCorner which works correctly across different layers/areas.
     */
    public static ChunkPath findShortestPathToAreaCorners(NArea area, ChunkNavManager chunkNav) throws InterruptedException {
        if (area == null || area.space == null || area.space.space == null || area.space.space.isEmpty()) {
            return chunkNav.planToArea(area);
        }
        
        // Plan paths to all 4 corners in parallel using planToAreaCorner (gridId + local coords)
        final ChunkPath[] paths = new ChunkPath[4];
        Thread[] threads = new Thread[4];
        
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                paths[idx] = chunkNav.planToAreaCorner(area, idx);
            });
            threads[i].start();
        }
        
        // Wait for all threads
        for (Thread t : threads) {
            t.join();
        }
        
        // Find the shortest path
        ChunkPath bestPath = null;
        float bestCost = Float.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            ChunkPath path = paths[i];
            if (path != null && path.totalCost < bestCost) {
                bestPath = path;
                bestCost = path.totalCost;
            }
        }

        if (bestPath == null) {
            return chunkNav.planToArea(area);
        }

        return bestPath;
    }
    
    /**
     * Check if any corner of the area is reachable via local pathfinding.
     * 
     * Two-stage check:
     * 1. If grid is NOT in MCache (not loaded) → return false immediately, use ChunkNav
     * 2. If grid IS in MCache → check with PathFinder, return true only if actually reachable
     */
    public static boolean isAreaReachableByLocalPF(NArea area) throws InterruptedException {
        if (area == null || NUtils.player() == null) return false;
        
        // STAGE 1: Check if grid is loaded in MCache
        // If not loaded → local PF is impossible, must use ChunkNav
        if (!area.isVisible()) {
            return false;
        }
        
        // STAGE 2: Grid is in cache, get real coordinates and check with PathFinder
        Pair<Coord2d, Coord2d> rcArea = area.getRCArea();
        if (rcArea == null) {
            return false;
        }
        
        // Get 4 corners from live data
        Coord2d[] corners = new Coord2d[] {
            rcArea.a,                                        // top-left
            rcArea.b,                                        // bottom-right
            Coord2d.of(rcArea.a.x, rcArea.b.y),             // bottom-left
            Coord2d.of(rcArea.b.x, rcArea.a.y)              // top-right
        };
        
        // Test all corners in parallel with PathFinder
        final boolean[] reachable = new boolean[4];
        Thread[] threads = new Thread[4];
        
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final Coord2d corner = corners[i];
            threads[i] = new Thread(() -> {
                try {
                    reachable[idx] = PathFinder.isAvailable(corner);
                } catch (InterruptedException e) {
                    reachable[idx] = false;
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads
        for (Thread t : threads) {
            t.join();
        }
        
        // Check if any corner is reachable
        for (boolean r : reachable) {
            if (r) return true;
        }

        return false;
    }

    /**
     * Find the nearest reachable corner of an area using local pathfinding.
     * Returns the corner coordinate that has the shortest actual path (not straight-line distance).
     * @param area The area to find corner for
     * @return The nearest reachable corner coordinate, or null if none found
     */
    public static Coord2d findNearestReachableCorner(NArea area) throws InterruptedException {
        if (area == null || NUtils.player() == null) return null;

        if (!area.isVisible()) {
            return null;
        }

        Pair<Coord2d, Coord2d> rcArea = area.getRCArea();
        if (rcArea == null) {
            return null;
        }

        // Get 4 corners from live data
        Coord2d[] corners = new Coord2d[] {
            rcArea.a,                                        // top-left
            rcArea.b,                                        // bottom-right
            Coord2d.of(rcArea.a.x, rcArea.b.y),             // bottom-left
            Coord2d.of(rcArea.b.x, rcArea.a.y)              // top-right
        };

        // Test all corners in parallel and get path costs
        final double[] pathCosts = new double[4];
        final boolean[] reachable = new boolean[4];
        Thread[] threads = new Thread[4];

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final Coord2d corner = corners[i];
            pathCosts[idx] = Double.MAX_VALUE;
            reachable[idx] = false;

            threads[i] = new Thread(() -> {
                try {
                    int cost = PathFinder.getPathCost(corner);
                    if (cost >= 0) {
                        pathCosts[idx] = cost;
                        reachable[idx] = true;
                    }
                } catch (InterruptedException e) {
                    // Leave as unreachable
                }
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread t : threads) {
            t.join();
        }

        // Find the corner with shortest actual path
        Coord2d nearestCorner = null;
        double bestCost = Double.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            if (reachable[i] && pathCosts[i] < bestCost) {
                bestCost = pathCosts[i];
                nearestCorner = corners[i];
            }
        }

        return nearestCorner;
    }
}
