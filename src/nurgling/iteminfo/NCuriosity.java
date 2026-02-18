package nurgling.iteminfo;

import haven.*;
import haven.resutil.Curiosity;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.conf.FontSettings;
import nurgling.conf.ItemQualityOverlaySettings;
import nurgling.styles.TooltipStyle;

import java.awt.*;
import java.awt.image.BufferedImage;

import static nurgling.NConfig.Key.is_real_time;

public class NCuriosity extends Curiosity implements GItem.OverlayInfo<Tex>{
    public static final float server_ratio = 3.29f;
    public int rm = 0;
    public transient final int lph;

    // Cached settings
    private static ItemQualityOverlaySettings cachedSettings = null;
    private static long lastSettingsCheck = 0;
    private static final long SETTINGS_CHECK_INTERVAL = 200;
    private static long settingsVersion = 0;
    private static boolean forceRefresh = false;

    private Tex cachedOverlay = null;
    private long lastSettingsVersion = -1;
    private int lastRemaining = -1;

    NGItem.MeterInfo m = null;

    private static Text.Foundry keyFoundry = null;        // Open Sans Regular for keys (11px)
    private static Text.Foundry valueFoundry = null;      // Open Sans Semibold for values (11px)

    private static Font getOpenSansRegular() {
        FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
        return fontSettings != null ? fontSettings.getFont("Open Sans") : null;
    }

    private static Font getOpenSansSemibold() {
        FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
        return fontSettings != null ? fontSettings.getFont("Open Sans Semibold") : null;
    }


    private static Text.Foundry getKeyFoundry() {
        if (keyFoundry == null) {
            Font font = getOpenSansRegular();
            int size = UI.scale(TooltipStyle.FONT_SIZE_BODY);
            if (font == null) {
                font = new Font("SansSerif", Font.PLAIN, size);
            } else {
                font = font.deriveFont(Font.PLAIN, (float) size);
            }
            keyFoundry = new Text.Foundry(font, Color.WHITE).aa(true);
        }
        return keyFoundry;
    }

    private static Text.Foundry getValueFoundry() {
        if (valueFoundry == null) {
            Font font = getOpenSansSemibold();
            int size = UI.scale(TooltipStyle.FONT_SIZE_BODY);
            if (font == null) {
                font = new Font("SansSerif", Font.BOLD, size);
            } else {
                font = font.deriveFont(Font.PLAIN, (float) size);
            }
            valueFoundry = new Text.Foundry(font, Color.WHITE).aa(true);
        }
        return valueFoundry;
    }

    /** Get the font descent for the body font (used for baseline-relative spacing) */
    private static int getBodyFontDescent() {
        Font font = getOpenSansRegular();
        int size = UI.scale(TooltipStyle.FONT_SIZE_BODY);
        if (font == null) {
            font = new Font("SansSerif", Font.PLAIN, size);
        } else {
            font = font.deriveFont(Font.PLAIN, (float) size);
        }
        java.awt.image.BufferedImage tmp = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.FontMetrics fm = tmp.getGraphics().getFontMetrics(font);
        return fm.getDescent();
    }


    public NCuriosity(Owner owner, int exp, int mw, int enc, int time) {
        super(owner, exp, mw, enc, time);
        this.lph = (exp > 0 && time > 0) ? (3600 * exp / time) : 0;
    }

    public NCuriosity(Curiosity inf) {
        this(inf.owner, inf.exp, inf.mw, inf.enc, inf.time);
    }
    
    public static void invalidateCache() {
        forceRefresh = true;
        settingsVersion++;
    }
    
    private static ItemQualityOverlaySettings getSettings() {
        long now = System.currentTimeMillis();
        if (forceRefresh || cachedSettings == null || now - lastSettingsCheck > SETTINGS_CHECK_INTERVAL) {
            ItemQualityOverlaySettings newSettings =
                (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.studyInfoOverlay);
            if (newSettings == null) {
                newSettings = new ItemQualityOverlaySettings();
                newSettings.corner = ItemQualityOverlaySettings.Corner.BOTTOM_LEFT;
                newSettings.defaultColor = new Color(255, 255, 50);
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

    /**
     * Check if compact tooltip mode is enabled (always true - compact is the only mode)
     */
    public static boolean isCompactMode() {
        return true;
    }

    @Override
    public int order() {
        // In compact mode, render first (before Name which is 0)
        return isCompactMode() ? -1 : super.order();
    }

    @Override
    public void prepare(ItemInfo.Layout l) {
        super.prepare(l);
    }

    @Override
    public void layout(ItemInfo.Layout l) {
        // NTooltip handles Name and QBuff rendering, we just render curio stats
        super.layout(l);
    }

    public BufferedImage tipimg() {
        return renderCompactTooltip();
    }

    private BufferedImage renderCompactTooltip() {
        // Calculate remaining time for display
        rm = (int)(remaining()/server_ratio);

        // Name line is now rendered by NTooltip for all items
        // We only render the curio-specific stats here

        java.util.List<BufferedImage> lines = new java.util.ArrayList<>();

        // Line 1: LP + LP/H + optionally LP/H/W
        if (exp > 0) {
            java.util.List<BufferedImage> pairs = new java.util.ArrayList<>();
            pairs.add(renderPair("LP:", Utils.thformat(exp), TooltipStyle.COLOR_LP));
            pairs.add(renderPair("LP/H:", String.valueOf(lph(this.lph)), TooltipStyle.COLOR_LPH));
            if (mw > 0 && lph > 0) {
                pairs.add(renderPair("LP/H/W:", String.valueOf(lph(this.lph / mw)), TooltipStyle.COLOR_LPH));
            }
            lines.add(composePairs(pairs));
        }

        // Line 2: Study time (real time)
        if (time > 0) {
            int realTime = (int)(time / server_ratio);
            lines.add(renderPair("Study time:", formatCompactStudyTime(realTime), TooltipStyle.COLOR_STUDY_TIME));
        }

        // Line 3: Mental weight + EXP cost
        if (mw > 0 || enc > 0) {
            java.util.List<BufferedImage> pairs = new java.util.ArrayList<>();
            if (mw > 0) {
                pairs.add(renderPair("Mental weight:", String.valueOf(mw), TooltipStyle.COLOR_MENTAL_WEIGHT));
            }
            if (enc > 0) {
                pairs.add(renderPair("EXP cost:", String.valueOf(enc), TooltipStyle.COLOR_EXP_COST));
            }
            lines.add(composePairs(pairs));
        }

        // Crop top only (keeps baseline-relative bottom position)
        java.util.List<BufferedImage> croppedLines = new java.util.ArrayList<>();
        for (BufferedImage line : lines) {
            croppedLines.add(cropTopOnly(line));
        }

        // Get font descent for baseline-relative spacing
        // Spacing = desired_baseline_to_top - descent (since bottom of image is at baseline + descent)
        int descent = getBodyFontDescent();
        int baselineSpacing = UI.scale(TooltipStyle.INTERNAL_SPACING) - descent;

        // Combine all lines with baseline-relative spacing
        // Return combined image - padding is handled by NWItem.PaddedTip for all tooltips
        return ItemInfo.catimgs(baselineSpacing, croppedLines.toArray(new BufferedImage[0]));
    }

    /** Render a key label (Open Sans Regular, white) */
    private BufferedImage renderKey(String text) {
        return getKeyFoundry().render(text, Color.WHITE).img;
    }

    /** Render a value (Open Sans Semibold, colored) */
    private BufferedImage renderValue(String text, Color color) {
        return getValueFoundry().render(text, color).img;
    }

    /** Render a key+value pair with natural spacing (key includes space after colon) */
    private BufferedImage renderPair(String key, String value, Color valueColor) {
        // Key includes trailing space for natural spacing: "LP: " + "3,834"
        BufferedImage keyImg = renderKey(key + " ");
        BufferedImage valueImg = renderValue(value, valueColor);
        int totalWidth = keyImg.getWidth() + valueImg.getWidth();
        int maxHeight = Math.max(keyImg.getHeight(), valueImg.getHeight());

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        g.drawImage(keyImg, 0, (maxHeight - keyImg.getHeight()) / 2, null);
        g.drawImage(valueImg, keyImg.getWidth(), (maxHeight - valueImg.getHeight()) / 2, null);
        g.dispose();
        return result;
    }

    /**
     * Crop top of image to first visible pixel, but keep bottom at original position.
     * This ensures baseline-relative spacing: the bottom of the image stays at a fixed
     * position relative to the baseline (baseline + descent), so spacing is measured
     * from baseline, not from descenders.
     */
    private BufferedImage cropTopOnly(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int alphaThreshold = 128; // Ignore anti-aliased pixels

        // Find top-most row with visible pixels
        int top = 0;
        topSearch:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > alphaThreshold) {
                    top = y;
                    break topSearch;
                }
            }
        }

        // If no visible pixels or no top cropping needed, return original
        if (top == 0) {
            return img;
        }

        // Crop only from the top, keep the bottom at original position
        int newHeight = height - top;
        BufferedImage cropped = new BufferedImage(width, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = cropped.getGraphics();
        g.drawImage(img, 0, 0, width, newHeight, 0, top, width, height, null);
        g.dispose();

        return cropped;
    }

    /** Compose key/value pairs horizontally with 7px gap between pairs */
    private BufferedImage composePairs(java.util.List<BufferedImage> pairs) {
        if (pairs.isEmpty()) {
            return TexI.mkbuf(new Coord(1, 1));
        }
        if (pairs.size() == 1) {
            return pairs.get(0);
        }

        int totalWidth = 0;
        int maxHeight = 0;
        int gap = 7; // gap between pairs (matches Figma design)
        for (BufferedImage img : pairs) {
            totalWidth += img.getWidth();
            maxHeight = Math.max(maxHeight, img.getHeight());
        }
        totalWidth += gap * (pairs.size() - 1);

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;
        for (int i = 0; i < pairs.size(); i++) {
            BufferedImage img = pairs.get(i);
            g.drawImage(img, x, (maxHeight - img.getHeight()) / 2, null);
            x += img.getWidth();
            if (i < pairs.size() - 1) {
                x += gap;
            }
        }
        g.dispose();
        return result;
    }


    /**
     * Get remaining time formatted for display on name line (in real time)
     */
    public String getCompactRemainingTime() {
        int remainingInGame = remaining();
        if (remainingInGame <= 0) {
            return null;
        }
        int remainingReal = (int)(remainingInGame / server_ratio);
        int totalReal = (int)(time / server_ratio);
        // Only show if study has started (remaining < total)
        if (remainingReal > 0 && remainingReal < totalReal) {
            return formatCompactTime(remainingReal);
        }
        return null;
    }

    private String formatCompactTime(int seconds) {
        if (seconds <= 0) return "00:00";
        int hours = seconds / 3600;
        int mins = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%dh %02dm", hours, mins);
        } else {
            int secs = seconds % 60;
            return String.format("%02d:%02d", mins, secs);
        }
    }

    private String formatCompactStudyTime(int seconds) {
        int hours = seconds / 3600;
        int mins = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%dh %dm", hours, mins);
        } else {
            return String.format("%dm", mins);
        }
    }

    public static int lph(int lph){
        return (Boolean)NConfig.get(is_real_time) ? ((int) (server_ratio * lph)) : lph;
    }


    public int remaining() {
        if(owner instanceof NGItem) {
            NGItem item = ((NGItem) owner);
            if(m == null)
            {
                m = ItemInfo.find(GItem.MeterInfo.class, item.info());
            }
            double meter = (m != null) ? m.meter() : 0;
            if(meter > 0) {
                long now = System.currentTimeMillis();
                long remStudy = (long) ((1.0 - meter) * time);
                long elapsed = (long) (server_ratio * (now - item.meterUpdated) / 1000);
                return (int) (remStudy - elapsed);
            }
        }
        return -1;
    }

    public boolean needUpdate(){
        if(rm>0) {
            return Math.abs(rm - (int) (remaining() / server_ratio)) > 1;
        }
        else
        {
            rm = (int) (remaining() / server_ratio);
        }
        return false;
    }

    static int[] div = {60, 60, 24};
    static String[] units = {"s", "m", "h", "d"};
    
    protected static String shorttime(int time, ItemQualityOverlaySettings.TimeFormat format) {
        if (time <= 0) return "0s";
        
        switch (format) {
            case SECONDS:
                return time + "s";
            case MINUTES:
                return (time / 60) + "m";
            case HOURS:
                return String.format("%.1fh", time / 3600.0);
            case DAYS:
                return String.format("%.1fd", time / 86400.0);
            case AUTO:
            default:
                int[] vals = new int[4];
                vals[0] = time;
                for(int i = 0; i < div.length; i++) {
                    vals[i + 1] = vals[i] / div[i];
                    vals[i] = vals[i] % div[i];
                }
                StringBuilder buf = new StringBuilder();
                int count = 0;
                for(int i = 3; i >= 0; i--) {
                    if(vals[i] > 0) {
                        if(count++ == 2)
                            break;
                        buf.append(vals[i]);
                        buf.append(units[i]);
                    }
                }
                return buf.length() > 0 ? buf.toString() : "0s";
        }
    }
    
    @Override
    public Tex overlay() {
        if(owner instanceof NGItem) {
            // Show overlay for items with remaining study time
            int rawRemaining = remaining();
            if (rawRemaining > 0) {
                ItemQualityOverlaySettings settings = getSettings();
                if (settings.hidden) {
                    return null;
                }
                float ratio = settings.studyTimeRatio > 0 ? settings.studyTimeRatio : server_ratio;
                int currentRemaining = (int) (rawRemaining / ratio);
                long currentVersion = settingsVersion;
                
                // Check if we can reuse cached overlay
                if (cachedOverlay != null && lastSettingsVersion == currentVersion && 
                    Math.abs(lastRemaining - currentRemaining) <= 1) {
                    return cachedOverlay;
                }
                
                BufferedImage text = renderTimeText(currentRemaining, settings);
                
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
                lastRemaining = currentRemaining;
                return cachedOverlay;
            }
        }
        return null;
    }
    
    private BufferedImage renderTimeText(int seconds, ItemQualityOverlaySettings settings) {
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
        
        String timeText = shorttime(seconds, settings.timeFormat);
        Text.Foundry fnd = new Text.Foundry(font, settings.defaultColor).aa(true);
        BufferedImage textImg = fnd.render(timeText, settings.defaultColor).img;
        
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

        BufferedImage result = new BufferedImage(w + width * 2, h + width * 2, BufferedImage.TYPE_INT_ARGB);
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
                    g.drawImage(coloredImg, width + dx, width + dy, null);
                }
            }
        }
        
        g.drawImage(img, width, width, null);
        g.dispose();
        
        return result;
    }


    @Override
    public void drawoverlay(GOut g, Tex data) {
        if(data != null) {
            ItemQualityOverlaySettings settings = getSettings();
            int pad = settings.showOutline ? settings.outlineWidth : 0;
            Coord pos;
            
            switch (settings.corner) {
                case TOP_LEFT:
                    pos = new Coord(-pad, -pad);
                    g.aimage(data, pos, 0, 0);
                    break;
                case TOP_RIGHT:
                    pos = new Coord(g.sz().x + pad, -pad);
                    g.aimage(data, pos, 1, 0);
                    break;
                case BOTTOM_LEFT:
                    pos = new Coord(-pad, g.sz().y + pad);
                    g.aimage(data, pos, 0, 1);
                    break;
                case BOTTOM_RIGHT:
                default:
                    pos = new Coord(g.sz().x + pad, g.sz().y + pad);
                    g.aimage(data, pos, 1, 1);
                    break;
            }
        }
    }

    @Override
    public boolean tick(double dt) {
        // Check if settings changed
        if (lastSettingsVersion != settingsVersion) {
            cachedOverlay = null;
            return false;
        }
        // Update overlay when remaining time changes
        return !needUpdate();
    }
}
