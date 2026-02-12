/* Preprocessed source code */
package haven.res.ui.tt.q.quality;

/* $use: ui/tt/q/qbuff */
import haven.*;
import haven.res.ui.tt.q.qbuff.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import haven.MenuGrid.Pagina;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.conf.FontSettings;
import nurgling.conf.ItemQualityOverlaySettings;
import nurgling.conf.ItemQualityOverlaySettings.Corner;

/* >tt: Quality */
@haven.FromResource(name = "ui/tt/q/quality", version = 28)
public class Quality extends QBuff implements GItem.OverlayInfo<Tex> {
    public static boolean show = Utils.getprefb("qtoggle", false);
    NGItem ownitem = null;
    boolean withContent = false;
    
    // Cached settings for performance
    private static ItemQualityOverlaySettings cachedSettings = null;
    private static long lastSettingsCheck = 0;
    private static final long SETTINGS_CHECK_INTERVAL = 200; // Check every 200ms
    private static long settingsVersion = 0; // Increment when settings change
    private static boolean forceRefresh = false; // Flag to force refresh all overlays
    
    public Quality(Owner owner, double q) {
        super(owner, Resource.classres(Quality.class).layer(Resource.imgc, 0).scaled(), "Quality", q);
        if (owner instanceof NGItem) {
            ownitem = (NGItem) owner;
            ownitem.quality = (float) q;
        }
    }

    @Override
    public int order() {
        return 101;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
        return(new Quality(owner, ((Number)args[1]).doubleValue()));
    }
    
    private static ItemQualityOverlaySettings getSettings() {
        long now = System.currentTimeMillis();
        if (cachedSettings == null || forceRefresh || now - lastSettingsCheck > SETTINGS_CHECK_INTERVAL) {
            Object settings = NConfig.get(NConfig.Key.itemQualityOverlay);
            ItemQualityOverlaySettings newSettings;
            if (settings instanceof ItemQualityOverlaySettings) {
                newSettings = (ItemQualityOverlaySettings) settings;
            } else {
                newSettings = new ItemQualityOverlaySettings();
            }
            // Check if settings reference changed (new settings object from save) or force refresh
            if (cachedSettings != newSettings || forceRefresh) {
                settingsVersion++;
                cachedSettings = newSettings;
                forceRefresh = false;
            }
            lastSettingsCheck = now;
        }
        return cachedSettings;
    }
    
    /**
     * Call this method to force all quality overlays to refresh with new settings
     */
    public static void invalidateCache() {
        forceRefresh = true;
        settingsVersion++;
    }
    
    public static long getSettingsVersion() {
        return settingsVersion;
    }
    
    private BufferedImage renderQualityText(double quality, Color color) {
        ItemQualityOverlaySettings settings = getSettings();
        
        // Get font from settings
        FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
        Font font;
        if (fontSettings != null) {
            font = fontSettings.getFont(settings.fontFamily);
            if (font == null) {
                font = new Font("SansSerif", Font.BOLD, UI.scale(settings.fontSize));
            } else {
                font = font.deriveFont(Font.BOLD, UI.scale((float) settings.fontSize));
            }
        } else {
            font = new Font("SansSerif", Font.BOLD, UI.scale(settings.fontSize));
        }
        
        // Format quality text based on showDecimal setting
        String qualityText;
        if (settings.showDecimal) {
            qualityText = String.format("%.1f", quality);
        } else {
            qualityText = Integer.toString((int) Math.round(quality));
        }
        
        // Create a foundry with the configured font and render
        Text.Foundry fnd = new Text.Foundry(font, color).aa(true);
        BufferedImage textImg = fnd.render(qualityText, color).img;
        
        // Apply outline if enabled
        if (settings.showOutline) {
            return outlineWithWidth(textImg, settings.outlineColor, settings.outlineWidth);
        } else {
            return textImg;
        }
    }
    
    /**
     * Create outlined text with configurable width
     */
    private BufferedImage outlineWithWidth(BufferedImage img, Color outlineColor, int width) {
        if (width <= 0) {
            return img;
        }
        
        int w = img.getWidth();
        int h = img.getHeight();
        int padding = width;
        
        BufferedImage result = new BufferedImage(w + padding * 2, h + padding * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw outline by drawing the image multiple times offset in all directions
        g.setComposite(AlphaComposite.SrcOver);
        
        // Create a colored version of the image for outline
        BufferedImage coloredImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg = coloredImg.createGraphics();
        cg.drawImage(img, 0, 0, null);
        cg.setComposite(AlphaComposite.SrcIn);
        cg.setColor(outlineColor);
        cg.fillRect(0, 0, w, h);
        cg.dispose();
        
        // Draw outline in all directions
        for (int dx = -width; dx <= width; dx++) {
            for (int dy = -width; dy <= width; dy++) {
                if (dx != 0 || dy != 0) {
                    g.drawImage(coloredImg, padding + dx, padding + dy, null);
                }
            }
        }
        
        // Draw original image on top
        g.drawImage(img, padding, padding, null);
        g.dispose();
        
        return result;
    }

    public Tex overlay() {
        ItemQualityOverlaySettings settings = getSettings();
        BufferedImage text;
        
        if (ownitem != null && !ownitem.content().isEmpty()) {
            withContent = true;
            double contentQuality = ownitem.content().get(0).quality();
            text = renderQualityText(contentQuality, settings.contentColor);
        } else {
            withContent = false;
            // Use threshold-based color for item quality
            Color qualityColor = settings.getColorForQuality(q);
            text = renderQualityText(q, qualityColor);
        }
        
        if (settings.showBackground) {
            BufferedImage bi = new BufferedImage(text.getWidth(), text.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = bi.createGraphics();
            graphics.setColor(settings.backgroundColor);
            graphics.fillRect(0, 0, bi.getWidth(), bi.getHeight());
            graphics.drawImage(text, 0, 0, null);
            graphics.dispose();
            return new TexI(bi);
        } else {
            return new TexI(text);
        }
    }

    public void drawoverlay(GOut g, Tex ol) {
        ItemQualityOverlaySettings settings = getSettings();
        int pad = settings.showOutline ? settings.outlineWidth : 0;
        Coord pos;
        
        switch (settings.corner) {
            case TOP_LEFT:
                pos = new Coord(-pad, -pad);
                g.aimage(ol, pos, 0, 0);
                break;
            case TOP_RIGHT:
                pos = new Coord(g.sz().x + pad, -pad);
                g.aimage(ol, pos, 1, 0);
                break;
            case BOTTOM_LEFT:
                pos = new Coord(-pad, g.sz().y + pad);
                g.aimage(ol, pos, 0, 1);
                break;
            case BOTTOM_RIGHT:
            default:
                pos = new Coord(g.sz().x + pad, g.sz().y + pad);
                g.aimage(ol, pos, 1, 1);
                break;
        }
    }

    private double lastQuality = -1.0;
    private boolean lastWithContent = false;
    private long lastSettingsVersion = -1;
    
    @Override
    public boolean tick(double dt) {
        // Check if quality or content state changed
        boolean currentWithContent = ownitem != null && !ownitem.content().isEmpty();
        double currentQuality = currentWithContent ? ownitem.content().get(0).quality() : q;
        
        // Force settings check to update version if needed
        getSettings();
        long currentVersion = settingsVersion;
        
        // Check if settings changed (by version number)
        boolean settingsChanged = lastSettingsVersion != currentVersion;
        
        if (lastQuality != currentQuality || lastWithContent != currentWithContent || settingsChanged) {
            lastQuality = currentQuality;
            lastWithContent = currentWithContent;
            lastSettingsVersion = currentVersion;
            return false; // Need to update overlay
        }
        return true; // No update needed
    }
}
