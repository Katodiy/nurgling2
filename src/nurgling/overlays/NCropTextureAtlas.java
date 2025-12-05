package nurgling.overlays;

import haven.*;
import haven.render.Texture;
import nurgling.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Texture atlas for crop markers - combines all crop textures into a single texture
 * to minimize texture binding and enable single draw call rendering
 */
public class NCropTextureAtlas {
    private static NCropTextureAtlas instance;
    
    private TexI atlasTexture;
    private final Map<TexI, AtlasRegion> regions = new HashMap<>();
    private boolean initialized = false;
    
    /**
     * Region within the texture atlas
     */
    public static class AtlasRegion {
        public final float u0, v0, u1, v1;
        public final int width, height;
        
        public AtlasRegion(float u0, float v0, float u1, float v1, int width, int height) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.width = width;
            this.height = height;
        }
    }
    
    private NCropTextureAtlas() {}
    
    public static NCropTextureAtlas getInstance() {
        if (instance == null) {
            synchronized (NCropTextureAtlas.class) {
                if (instance == null) {
                    instance = new NCropTextureAtlas();
                    instance.buildAtlas();
                }
            }
        }
        return instance;
    }
    
    /**
     * Build the texture atlas from all crop marker textures
     */
    private void buildAtlas() {
        if (initialized) return;
        
        // Collect all unique crop textures from public fields only
        java.util.List<TexI> textures = new ArrayList<>();
        textures.addAll(NStyle.iCropMap.values());
        
        // Add stage-specific textures by calling getCropTexI for all possible combinations
        for (int maxStage = 3; maxStage <= 6; maxStage++) {
            for (long stage = 1; stage < maxStage; stage++) {
                TexI tex = NStyle.getCropTexI(stage, maxStage);
                if (tex != null && !textures.contains(tex)) {
                    textures.add(tex);
                }
            }
        }
        
        // Remove nulls
        textures.removeIf(Objects::isNull);
        
        if (textures.isEmpty()) {
            initialized = true;
            return;
        }
        
        // Calculate atlas size - simple grid layout
        int maxWidth = 0;
        int maxHeight = 0;
        for (TexI tex : textures) {
            Coord sz = tex.sz();
            maxWidth = Math.max(maxWidth, sz.x);
            maxHeight = Math.max(maxHeight, sz.y);
        }
        
        // Calculate grid dimensions
        int cols = (int) Math.ceil(Math.sqrt(textures.size()));
        int rows = (int) Math.ceil((double) textures.size() / cols);
        
        int atlasWidth = Tex.nextp2(cols * maxWidth);
        int atlasHeight = Tex.nextp2(rows * maxHeight);
        
        // Create atlas image
        BufferedImage atlasImage = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = atlasImage.createGraphics();
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, atlasWidth, atlasHeight);
        
        // Pack textures into atlas
        int x = 0, y = 0, col = 0;
        for (TexI tex : textures) {
            Coord sz = tex.sz();
            g2d.drawImage(tex.back, x, y, null);
            
            // Calculate UV coordinates
            float u0 = (float) x / atlasWidth;
            float v0 = (float) y / atlasHeight;
            float u1 = (float) (x + sz.x) / atlasWidth;
            float v1 = (float) (y + sz.y) / atlasHeight;
            
            regions.put(tex, new AtlasRegion(u0, v0, u1, v1, sz.x, sz.y));
            
            col++;
            if (col >= cols) {
                col = 0;
                x = 0;
                y += maxHeight;
            } else {
                x += maxWidth;
            }
        }
        
        g2d.dispose();
        
        // Create atlas texture
        atlasTexture = new TexI(atlasImage);
        atlasTexture.minfilter(Texture.Filter.NEAREST).magfilter(Texture.Filter.NEAREST);
        
        initialized = true;
    }
    
    /**
     * Get the atlas texture
     */
    public TexI getAtlasTexture() {
        return atlasTexture;
    }
    
    /**
     * Get the UV region for a specific texture
     */
    public AtlasRegion getRegion(TexI texture) {
        return regions.get(texture);
    }
    
    /**
     * Check if the atlas has a region for this texture
     */
    public boolean hasRegion(TexI texture) {
        return regions.containsKey(texture);
    }
    
    /**
     * Check if atlas is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}
