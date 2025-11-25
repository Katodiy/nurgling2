package nurgling.tools;

import haven.*;
import nurgling.NConfig;
import nurgling.widgets.NMiniMap;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Tracks explored (visible) area on the minimap.
 * Uses grid-based boolean masks for efficient storage and fast updates.
 * Each grid (100x100 tiles) has its own mask marking explored tiles.
 */
public class ExploredArea {
    // Version tracking for cache invalidation (similar to TileHighlight.seq)
    public static volatile long seq = 0;
    
    private static final int GRID_SIZE = 100; // MCache.cmaps.x
    private static final int MASK_SIZE = GRID_SIZE * GRID_SIZE;
    
    /**
     * Key for identifying a grid in a specific segment.
     */
    private static class GridKey {
        final long segmentId;
        final Coord gridCoord;  // Grid coordinate at data level 0
        
        GridKey(long segmentId, Coord gridCoord) {
            this.segmentId = segmentId;
            this.gridCoord = gridCoord;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GridKey)) return false;
            GridKey key = (GridKey) o;
            return segmentId == key.segmentId && gridCoord.equals(key.gridCoord);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(segmentId, gridCoord);
        }
    }
    
    final NMiniMap miniMap;
    
    // Main storage: grid-based masks
    private final ConcurrentHashMap<GridKey, boolean[]> gridMasks = new ConcurrentHashMap<>();
    
    // Track last update position to avoid redundant updates
    private Coord lastTileUL, lastTileBR;
    private long lastSegmentId = -1;
    
    public ExploredArea(NMiniMap miniMap) {
        this.miniMap = miniMap;
        loadFromFile();
    }
    
    /**
     * Update explored area with current view bounds.
     * This is called every tick when player moves.
     * Very fast - just sets bits in the mask, no complex Rectangle operations.
     */
    public void updateExploredTiles(Coord tileUL, Coord tileBR, long segmentId) {
        // Skip if same as last update
        if (Objects.equals(tileUL, lastTileUL) && Objects.equals(tileBR, lastTileBR) && segmentId == lastSegmentId) {
            return;
        }
        
        lastTileUL = tileUL;
        lastTileBR = tileBR;
        lastSegmentId = segmentId;
        
        // Calculate which grids are affected
        Coord gridUL = tileUL.div(GRID_SIZE);
        Coord gridBR = tileBR.sub(1, 1).div(GRID_SIZE); // Inclusive end
        
        boolean changed = false;
        
        // Update each affected grid
        for (int gy = gridUL.y; gy <= gridBR.y; gy++) {
            for (int gx = gridUL.x; gx <= gridBR.x; gx++) {
                Coord gridCoord = new Coord(gx, gy);
                GridKey key = new GridKey(segmentId, gridCoord);
                
                // Get or create mask for this grid
                boolean[] mask = gridMasks.computeIfAbsent(key, k -> new boolean[MASK_SIZE]);
                
                // Calculate tile bounds within this grid
                Coord gridTileStart = gridCoord.mul(GRID_SIZE);
                int localULX = Math.max(0, tileUL.x - gridTileStart.x);
                int localULY = Math.max(0, tileUL.y - gridTileStart.y);
                int localBRX = Math.min(GRID_SIZE, tileBR.x - gridTileStart.x);
                int localBRY = Math.min(GRID_SIZE, tileBR.y - gridTileStart.y);
                
                // Mark tiles as explored
                for (int y = localULY; y < localBRY; y++) {
                    for (int x = localULX; x < localBRX; x++) {
                        int idx = x + y * GRID_SIZE;
                        if (!mask[idx]) {
                            mask[idx] = true;
                            changed = true;
                        }
                    }
                }
            }
        }
        
        if (changed) {
            seq++;
            NConfig.needExploredUpdate();
        }
    }
    
    /**
     * Get explored mask for a specific grid.
     * Used by MinimapExploredAreaRenderer for rendering.
     * Very fast - just returns the stored mask.
     */
    public boolean[] getExploredMaskForGrid(Coord gridCoord, long segmentId, int dataLevel) {
        // At different data levels, we need to aggregate base-level grids
        if (dataLevel == 0) {
            // Direct lookup for finest level
            GridKey key = new GridKey(segmentId, gridCoord);
            return gridMasks.get(key);
        } else {
            // For coarser levels, aggregate multiple base grids
            // Each grid at level N represents 2^N x 2^N base grids
            int gridScale = (1 << dataLevel);
            Coord baseGridStart = gridCoord.mul(gridScale);
            
            boolean[] aggregatedMask = new boolean[MASK_SIZE];
            
            // Sample from base grids
            for (int gy = 0; gy < GRID_SIZE; gy++) {
                for (int gx = 0; gx < GRID_SIZE; gx++) {
                    // Map this coarse grid tile to base grid coordinates
                    int baseTileX = baseGridStart.x * GRID_SIZE + gx * gridScale;
                    int baseTileY = baseGridStart.y * GRID_SIZE + gy * gridScale;
                    
                    Coord baseGridCoord = new Coord(baseTileX / GRID_SIZE, baseTileY / GRID_SIZE);
                    GridKey baseKey = new GridKey(segmentId, baseGridCoord);
                    boolean[] baseMask = gridMasks.get(baseKey);
                    
                    if (baseMask != null) {
                        // Check if any tile in the scaled region is explored
                        int localX = baseTileX % GRID_SIZE;
                        int localY = baseTileY % GRID_SIZE;
                        boolean explored = false;
                        
                        // Sample a few tiles in the region
                        for (int sy = 0; sy < gridScale && localY + sy < GRID_SIZE && !explored; sy++) {
                            for (int sx = 0; sx < gridScale && localX + sx < GRID_SIZE && !explored; sx++) {
                                int idx = (localX + sx) + (localY + sy) * GRID_SIZE;
                                if (baseMask[idx]) {
                                    explored = true;
                                }
                            }
                        }
                        
                        if (explored) {
                            aggregatedMask[gx + gy * GRID_SIZE] = true;
                        }
                    }
                }
            }
            
            return aggregatedMask;
        }
    }
    
    /**
     * Clear all explored data.
     */
    public void clear() {
        if (!gridMasks.isEmpty()) {
            gridMasks.clear();
            lastTileUL = null;
            lastTileBR = null;
            lastSegmentId = -1;
            seq++;
            NConfig.needExploredUpdate();
        }
    }
    
    /**
     * Tick method - not needed anymore, but kept for compatibility.
     */
    public void tick(double dt) {
        // No complex processing needed - everything is handled in updateExploredTiles
    }
    
    /**
     * Load explored area from JSON file.
     */
    private void loadFromFile() {
        File file = new File(NConfig.current.path_explored);
        if (!file.exists()) {
            return;
        }
        
        try {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            }
            
            if (contentBuilder.length() == 0) {
                return;
            }
            
            JSONObject json = new JSONObject(contentBuilder.toString());
            if (!json.has("grids")) {
                return;
            }
            
            JSONArray gridsArray = json.getJSONArray("grids");
            for (int i = 0; i < gridsArray.length(); i++) {
                JSONObject gridJson = gridsArray.getJSONObject(i);
                
                long segmentId = gridJson.getLong("seg");
                int gx = gridJson.getInt("gx");
                int gy = gridJson.getInt("gy");
                
                GridKey key = new GridKey(segmentId, new Coord(gx, gy));
                
                // Decode RLE compressed mask
                String rle = gridJson.getString("mask");
                boolean[] mask = decodeRLE(rle);
                
                if (mask != null) {
                    gridMasks.put(key, mask);
                }
            }
            
            seq++;
        } catch (Exception e) {
            // Ignore load errors
        }
    }
    
    /**
     * Save explored area to JSON file.
     */
    public JSONObject toJson() {
        JSONArray gridsArray = new JSONArray();
        
        for (Map.Entry<GridKey, boolean[]> entry : gridMasks.entrySet()) {
            GridKey key = entry.getKey();
            boolean[] mask = entry.getValue();
            
            // Skip empty masks
            if (!hasAnyExploredTiles(mask)) {
                continue;
            }
            
            JSONObject gridJson = new JSONObject();
            gridJson.put("seg", key.segmentId);
            gridJson.put("gx", key.gridCoord.x);
            gridJson.put("gy", key.gridCoord.y);
            
            // Encode mask with RLE compression
            gridJson.put("mask", encodeRLE(mask));
            
            gridsArray.put(gridJson);
        }
        
        JSONObject doc = new JSONObject();
        doc.put("grids", gridsArray);
        return doc;
    }
    
    /**
     * Check if mask has any explored tiles.
     */
    private boolean hasAnyExploredTiles(boolean[] mask) {
        for (boolean tile : mask) {
            if (tile) return true;
        }
        return false;
    }
    
    /**
     * Encode boolean mask with RLE (Run-Length Encoding) for compression.
     * Format: "startBit:count1,count2,count3..." where startBit (0 or 1) indicates first value.
     */
    private String encodeRLE(boolean[] mask) {
        StringBuilder sb = new StringBuilder();
        
        // Store the starting value (0 for false, 1 for true)
        sb.append(mask[0] ? '1' : '0').append(':');
        
        boolean currentValue = mask[0];
        int count = 1;
        
        for (int i = 1; i < mask.length; i++) {
            if (mask[i] == currentValue) {
                count++;
            } else {
                sb.append(count).append(',');
                currentValue = mask[i];
                count = 1;
            }
        }
        sb.append(count); // Last run
        
        return sb.toString();
    }
    
    /**
     * Decode RLE compressed mask.
     */
    private boolean[] decodeRLE(String rle) {
        try {
            // Split by colon to get starting bit and run counts
            String[] mainParts = rle.split(":", 2);
            if (mainParts.length != 2) {
                return null;
            }
            
            // Get starting value (0 = false, 1 = true)
            boolean currentValue = mainParts[0].equals("1");
            
            // Parse run counts
            String[] parts = mainParts[1].split(",");
            boolean[] mask = new boolean[MASK_SIZE];
            
            int idx = 0;
            
            for (String part : parts) {
                int count = Integer.parseInt(part.trim());
                for (int i = 0; i < count && idx < MASK_SIZE; i++) {
                    mask[idx++] = currentValue;
                }
                currentValue = !currentValue; // Toggle
            }
            
            return mask;
        } catch (Exception e) {
            return null;
        }
    }
}
