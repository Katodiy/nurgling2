package nurgling.widgets;

import haven.*;
import nurgling.NConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Tracking status indicator that appears in the buff area when tracking is enabled
 */
public class TrackingStatusBuff extends Buff {
    private static final Color trackingColor = new Color(255, 165, 0, 180); // Orange
    private static Tex trackingIcon = null;
    
    public TrackingStatusBuff() {
        super(createTrackingResource());
        if (trackingIcon == null) {
            trackingIcon = getTrackingIcon();
        }
    }
    
    /**
     * Creates a resource reference for the tracking buff
     */
    private static Indir<Resource> createTrackingResource() {
        try {
            // Try to use the actual tracking resource
            return Resource.local().loadwait("paginae/act/tracking").indir();
        } catch (Exception e) {
            // Fallback to a generic buff resource
            try {
                return Resource.local().loadwait("gfx/hud/buffs/frame").indir();
            } catch (Exception e2) {
                // Last resort - create a minimal resource
                return new Indir<Resource>() {
                    public Resource get() {
                        return Resource.local().loadwait("gfx/hud/buffs/frame");
                    }
                };
            }
        }
    }
    
    /**
     * Gets the tracking icon from the actual tracking toggle button
     */
    private static Tex getTrackingIcon() {
        try {
            if (nurgling.NUtils.getGameUI() != null && nurgling.NUtils.getGameUI().menu != null) {
                // Find the tracking toggle button and extract its icon
                for (MenuGrid.Pagina pag : nurgling.NUtils.getGameUI().menu.paginae) {
                    try {
                        String ref = ((Session.CachedRes.Ref)pag.res).resnm();
                        if (ref.equals("paginae/act/tracking")) {
                            // Get the button and its resource
                            MenuGrid.PagButton button = pag.button();
                            Resource res = button.res;
                            
                            // Try to get the icon from the button's resource
                            Resource.Image img = res.layer(Resource.imgc);
                            if (img != null) {
                                return img.tex();
                            }
                        }
                    } catch (Exception e) {
                        // Skip this pagina if there's an error
                    }
                }
            }
        } catch (Exception e) {
            // Error getting tracking icon
        }
        
        // Fallback: create a simple orange circle with "T"
        Coord buffSize = Buff.cframe.sz();
        BufferedImage img = TexI.mkbuf(buffSize);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(trackingColor);
        g.fillOval(2, 2, buffSize.x - 4, buffSize.y - 4);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        String text = "T";
        int x = (buffSize.x - fm.stringWidth(text)) / 2;
        int y = (buffSize.y - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);
        g.dispose();
        return new TexI(img);
    }

    @Override
    public void draw(GOut g) {
        // Draw the standard buff frame first
        g.chcolor(255, 255, 255, a);
        g.image(frame, Coord.z);
        
        // Draw our custom tracking icon instead of the resource icon
        if (trackingIcon != null) {
            Coord iconPos = imgoff;
            // Scale the icon up by 15% to match other buff icon sizes
            Coord iconSize = trackingIcon.sz();
            Coord scaledSize = new Coord((int)(iconSize.x * 1.15), (int)(iconSize.y * 1.15));
            // Center the scaled icon
            Coord centeredPos = iconPos.sub(scaledSize.sub(iconSize).div(2));
            g.image(trackingIcon, centeredPos, scaledSize);
        }
        g.chcolor();
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return "Tracking mode is enabled";
    }
}