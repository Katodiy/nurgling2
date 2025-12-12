package haven.res.ui.tt.drying;/* Preprocessed source code */
import haven.*;
import nurgling.NConfig;
import nurgling.conf.FontSettings;
import nurgling.conf.ItemQualityOverlaySettings;

import java.awt.*;
import java.awt.image.BufferedImage;

/* >tt: haven.res.ui.tt.drying.Drying */
@haven.FromResource(name = "ui/tt/drying", version = 3)
public class Drying extends ItemInfo implements GItem.MeterInfo, GItem.OverlayInfo<Tex>  {
    public final double done;

    // Cached settings
    private static ItemQualityOverlaySettings cachedSettings = null;
    private static long lastSettingsCheck = 0;
    private static final long SETTINGS_CHECK_INTERVAL = 200;
    private static long settingsVersion = 0;
    private static boolean forceRefresh = false;
    
    private Tex cachedOverlay = null;
    private long lastSettingsVersion = -1;
    private int lastPercent = -1;

    public Drying(Owner owner, double done) {
        super(owner);
        this.done = done;
    }

    public double meter() {
        return(done);
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
        double done = ((Number)args[1]).doubleValue() / 100.0;
        return(new Drying(owner, done));
    }
    
    public static void invalidateCache() {
        forceRefresh = true;
        settingsVersion++;
    }
    
    private static ItemQualityOverlaySettings getSettings() {
        long now = System.currentTimeMillis();
        if (forceRefresh || cachedSettings == null || now - lastSettingsCheck > SETTINGS_CHECK_INTERVAL) {
            ItemQualityOverlaySettings newSettings = 
                (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.progressOverlay);
            if (newSettings == null) {
                newSettings = new ItemQualityOverlaySettings();
                newSettings.corner = ItemQualityOverlaySettings.Corner.BOTTOM_LEFT;
                newSettings.defaultColor = new Color(234, 164, 101);
                newSettings.showBackground = true;
            }
            if (cachedSettings != newSettings || forceRefresh) {
                cachedSettings = newSettings;
                settingsVersion++;
                forceRefresh = false;
            }
            lastSettingsCheck = now;
        }
        return cachedSettings;
    }

    public Tex overlay() {
        ItemQualityOverlaySettings settings = getSettings();
        if (settings.hidden) {
            return null;
        }
        
        int currentPercent = (int)(meter() * 100);
        long currentVersion = settingsVersion;
        
        // Check cache
        if (cachedOverlay != null && lastSettingsVersion == currentVersion && lastPercent == currentPercent) {
            return cachedOverlay;
        }
        BufferedImage text = renderPercentText(currentPercent, settings);
        
        if (settings.showBackground) {
            BufferedImage bi = new BufferedImage(text.getWidth(), text.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = bi.createGraphics();
            graphics.setColor(settings.backgroundColor);
            graphics.fillRect(0, 0, bi.getWidth(), bi.getHeight());
            graphics.drawImage(text, 0, 0, null);
            graphics.dispose();
            cachedOverlay = new TexI(bi);
        } else {
            cachedOverlay = new TexI(text);
        }
        
        lastSettingsVersion = currentVersion;
        lastPercent = currentPercent;
        return cachedOverlay;
    }
    
    private BufferedImage renderPercentText(int percent, ItemQualityOverlaySettings settings) {
        FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
        Font font;
        if (fontSettings != null) {
            font = fontSettings.getFont(settings.fontFamily);
            if (font == null) {
                font = new Font("SansSerif", Font.PLAIN, UI.scale(settings.fontSize));
            } else {
                font = font.deriveFont(Font.PLAIN, UI.scale((float) settings.fontSize));
            }
        } else {
            font = new Font("SansSerif", Font.PLAIN, UI.scale(settings.fontSize));
        }
        
        String text = percent + "%";
        Text.Foundry fnd = new Text.Foundry(font, settings.defaultColor).aa(true);
        BufferedImage textImg = fnd.render(text, settings.defaultColor).img;
        
        if (settings.showOutline) {
            return outlineWithWidth(textImg, settings.outlineColor, settings.outlineWidth);
        } else {
            return textImg;
        }
    }
    
    private BufferedImage outlineWithWidth(BufferedImage img, Color outlineColor, int width) {
        if (width <= 0) return img;
        
        int w = img.getWidth();
        int h = img.getHeight();
        int padding = width;
        
        BufferedImage result = new BufferedImage(w + padding * 2, h + padding * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        BufferedImage coloredImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg = coloredImg.createGraphics();
        cg.drawImage(img, 0, 0, null);
        cg.setComposite(AlphaComposite.SrcIn);
        cg.setColor(outlineColor);
        cg.fillRect(0, 0, w, h);
        cg.dispose();
        
        for (int dx = -width; dx <= width; dx++) {
            for (int dy = -width; dy <= width; dy++) {
                if (dx != 0 || dy != 0) {
                    g.drawImage(coloredImg, padding + dx, padding + dy, null);
                }
            }
        }
        
        g.drawImage(img, padding, padding, null);
        g.dispose();
        
        return result;
    }

    public void drawoverlay(GOut g, Tex ol) {
        if (ol != null) {
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
    }

    @Override
    public boolean tick(double dt) {
        int currentPercent = (int)(meter() * 100);
        // Check if settings changed
        if (lastSettingsVersion != settingsVersion) {
            cachedOverlay = null;
            return false;
        }
        if (lastPercent != currentPercent) {
            lastPercent = currentPercent;
            return false; // Need to update overlay
        }
        return true; // No update needed
    }
}
