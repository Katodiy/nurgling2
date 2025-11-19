package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batched rendering system for crop markers to reduce draw calls.
 * Collects all visible crop markers and renders them in a single instanced draw call per texture type.
 */
public class NCropMarkerBatch implements RenderTree.Node {
    private static NCropMarkerBatch instance;
    
    private final Map<TexI, List<MarkerInstance>> markersByTexture = new HashMap<>();
    private final Map<Long, MarkerInstance> activeMarkers = new ConcurrentHashMap<>();
    
    private static final int MAX_INSTANCES = 8192;
    private static boolean showCropStage = false;
    private static long lastConfigCheck = 0;
    private static boolean useAtlas = true;
    private final NCropTextureAtlas atlas = NCropTextureAtlas.getInstance();
    
    /**
     * Represents a single crop marker instance with position and texture
     */
    public static class MarkerInstance {
        public final long gobId;
        public Coord2d screenPos;
        public TexI texture;
        
        public MarkerInstance(long gobId, Coord2d pos, TexI tex) {
            this.gobId = gobId;
            this.screenPos = pos;
            this.texture = tex;
        }
    }
    
    private NCropMarkerBatch() {}
    
    public static NCropMarkerBatch getInstance() {
        if (instance == null) {
            synchronized (NCropMarkerBatch.class) {
                if (instance == null) {
                    instance = new NCropMarkerBatch();
                }
            }
        }
        return instance;
    }
    
    /**
     * Update or add a marker to the batch
     */
    public void updateMarker(long gobId, Coord2d screenPos, TexI texture) {
        if (texture == null || screenPos == null) {
            activeMarkers.remove(gobId);
            return;
        }
        
        MarkerInstance marker = activeMarkers.get(gobId);
        if (marker == null) {
            marker = new MarkerInstance(gobId, screenPos, texture);
            activeMarkers.put(gobId, marker);
        } else {
            marker.screenPos = screenPos;
            marker.texture = texture;
        }
    }
    
    /**
     * Remove a marker from the batch
     */
    public void removeMarker(long gobId) {
        activeMarkers.remove(gobId);
    }
    
    /**
     * Prepare batches for rendering - group markers by texture
     */
    private void prepareBatches() {
        markersByTexture.clear();
        
        for (MarkerInstance marker : activeMarkers.values()) {
            if (marker.texture == null) continue;
            
            markersByTexture.computeIfAbsent(marker.texture, k -> new ArrayList<>())
                           .add(marker);
        }
    }
    
    /**
     * Begin a new frame - clear the batch for fresh data
     */
    public void beginFrame() {
        activeMarkers.clear();
    }
    
    /**
     * Render all crop markers in batches
     */
    public void render(GOut g, Pipe state) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConfigCheck > 1000) {
            showCropStage = (Boolean) NConfig.get(NConfig.Key.showCropStage);
            lastConfigCheck = currentTime;
        }
        
        if (!showCropStage || activeMarkers.isEmpty()) {
            return;
        }
        
        // Use texture atlas for maximum performance
        if (useAtlas && atlas.isInitialized()) {
            renderAllWithAtlas(g);
        } else {
            // Fallback to per-texture batching
            prepareBatches();
            for (Map.Entry<TexI, List<MarkerInstance>> entry : markersByTexture.entrySet()) {
                TexI texture = entry.getKey();
                List<MarkerInstance> markers = entry.getValue();
                
                if (markers.isEmpty()) continue;
                
                renderBatch(g, texture, markers);
            }
        }
    }
    
    /**
     * Render a batch of markers with the same texture using triangles
     */
    private void renderBatch(GOut g, TexI texture, List<MarkerInstance> markers) {
        if (markers.isEmpty()) return;
        
        Coord sz = texture.sz();
        float halfW = sz.x * 0.75f;
        float halfH = sz.y * 0.75f;
        
        int batchSize = Math.min(markers.size(), MAX_INSTANCES);
        
        for (int offset = 0; offset < markers.size(); offset += batchSize) {
            int count = Math.min(batchSize, markers.size() - offset);
            // Each quad = 2 triangles = 6 vertices, each vertex has 4 floats (x, y, u, v)
            float[] vertexData = new float[count * 24];
            
            int idx = 0;
            for (int i = 0; i < count; i++) {
                MarkerInstance marker = markers.get(offset + i);
                float x = (float) marker.screenPos.x;
                float y = (float) marker.screenPos.y;
                
                float l = x - halfW;
                float r = x + halfW;
                float t = y - halfH;
                float b = y + halfH;
                
                // First triangle (top-right, bottom-right, top-left)
                vertexData[idx++] = r; vertexData[idx++] = t;
                vertexData[idx++] = 1.0f; vertexData[idx++] = 0.0f;
                
                vertexData[idx++] = r; vertexData[idx++] = b;
                vertexData[idx++] = 1.0f; vertexData[idx++] = 1.0f;
                
                vertexData[idx++] = l; vertexData[idx++] = t;
                vertexData[idx++] = 0.0f; vertexData[idx++] = 0.0f;
                
                // Second triangle (top-left, bottom-right, bottom-left)
                vertexData[idx++] = l; vertexData[idx++] = t;
                vertexData[idx++] = 0.0f; vertexData[idx++] = 0.0f;
                
                vertexData[idx++] = r; vertexData[idx++] = b;
                vertexData[idx++] = 1.0f; vertexData[idx++] = 1.0f;
                
                vertexData[idx++] = l; vertexData[idx++] = b;
                vertexData[idx++] = 0.0f; vertexData[idx++] = 1.0f;
            }
            
            g.usestate(texture.st());
            g.drawt(Model.Mode.TRIANGLES, vertexData, count * 6);
        }
        
        g.usestate(ColorTex.slot);
    }
    
    /**
     * Render all markers using texture atlas in a single draw call
     */
    private void renderAllWithAtlas(GOut g) {
        TexI atlasTexture = atlas.getAtlasTexture();
        if (atlasTexture == null) return;
        
        List<MarkerInstance> allMarkers = new ArrayList<>(activeMarkers.values());
        if (allMarkers.isEmpty()) return;
        
        int totalMarkers = allMarkers.size();
        
        for (int offset = 0; offset < totalMarkers; offset += MAX_INSTANCES) {
            int count = Math.min(MAX_INSTANCES, totalMarkers - offset);
            float[] vertexData = new float[count * 24];
            
            int idx = 0;
            for (int i = 0; i < count; i++) {
                MarkerInstance marker = allMarkers.get(offset + i);
                if (marker.texture == null) continue;
                
                NCropTextureAtlas.AtlasRegion region = atlas.getRegion(marker.texture);
                if (region == null) continue;
                
                float x = (float) marker.screenPos.x;
                float y = (float) marker.screenPos.y;
                
                float halfW = region.width * 0.75f;
                float halfH = region.height * 0.75f;
                
                float l = x - halfW;
                float r = x + halfW;
                float t = y - halfH;
                float b = y + halfH;
                
                // First triangle
                vertexData[idx++] = r; vertexData[idx++] = t;
                vertexData[idx++] = region.u1; vertexData[idx++] = region.v0;
                
                vertexData[idx++] = r; vertexData[idx++] = b;
                vertexData[idx++] = region.u1; vertexData[idx++] = region.v1;
                
                vertexData[idx++] = l; vertexData[idx++] = t;
                vertexData[idx++] = region.u0; vertexData[idx++] = region.v0;
                
                // Second triangle
                vertexData[idx++] = l; vertexData[idx++] = t;
                vertexData[idx++] = region.u0; vertexData[idx++] = region.v0;
                
                vertexData[idx++] = r; vertexData[idx++] = b;
                vertexData[idx++] = region.u1; vertexData[idx++] = region.v1;
                
                vertexData[idx++] = l; vertexData[idx++] = b;
                vertexData[idx++] = region.u0; vertexData[idx++] = region.v1;
            }
            
            g.usestate(atlasTexture.st());
            g.drawt(Model.Mode.TRIANGLES, vertexData, count * 6);
        }
        
        g.usestate(ColorTex.slot);
    }
    
    /**
     * Clear all markers
     */
    public void clear() {
        activeMarkers.clear();
        markersByTexture.clear();
    }
    
    /**
     * Get the number of active markers
     */
    public int getMarkerCount() {
        return activeMarkers.size();
    }
}
