package nurgling.iteminfo;

import haven.*;
import haven.resutil.Curiosity;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.conf.FontSettings;
import nurgling.conf.ItemQualityOverlaySettings;

import java.awt.*;
import java.awt.image.BufferedImage;

import static nurgling.NConfig.Key.is_real_time;

public class NCuriosity extends Curiosity implements GItem.OverlayInfo<Tex>{
    public static final float server_ratio = 3.29f;
    public int rm = 0;
    private static final int delta = 60;
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

    public BufferedImage tipimg() {
        StringBuilder buf = new StringBuilder();
        if(exp > 0)
            buf.append(String.format("Learning points: $col[192,192,255]{%s} ($col[192,192,255]{%s}/h)\n", Utils.thformat(exp), Utils.thformat(Math.round(exp / (time / 3600.0)))));
        if(time > 0) {
            buf.append(String.format("Study time: $col[192,255,192]{%s} ($col[192,255,255]{%s})\n", timefmt(time), timefmt((int)(time/server_ratio))));
        }
        rm = (int)(remaining()/server_ratio);
        if(rm!=time)
        {
            buf.append(String.format("Remaining time: $col[192,255,192]{%s}\n", timefmt(rm)));
        }
        if(mw > 0)
            buf.append(String.format("Mental weight: $col[255,192,255]{%d}\n", mw));
        if(enc > 0)
            buf.append(String.format("Experience cost: $col[255,255,192]{%d}\n", enc));
        if(lph>0) {
            buf.append(String.format("LP/H: $col[192,255,255]{%d}\n", lph(this.lph)));
            buf.append(String.format("LP/H/Weight: $col[192,255,255]{%d}\n", lph(this.lph / mw)));
        }
        return(RichText.render(buf.toString(), 0).img);
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
    
    protected static String shorttime(int time) {
        return shorttime(time, ItemQualityOverlaySettings.TimeFormat.AUTO);
    }
    
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
