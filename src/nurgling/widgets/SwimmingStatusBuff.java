package nurgling.widgets;

import haven.*;
import nurgling.NConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Swimming status indicator that appears in the buff area when swimming is enabled
 */
public class SwimmingStatusBuff extends Buff {
    private static final Color swimmingColor = new Color(64, 128, 255, 180);
    private static Tex swimmingIcon = null;
    
    public SwimmingStatusBuff() {
        super(createSwimmingResource());
        if (swimmingIcon == null) {
            swimmingIcon = getSwimmingIcon();
        }
    }
    
    /**
     * Creates a resource reference for the swimming buff
     */
    private static Indir<Resource> createSwimmingResource() {
        try {
            // Try to use the actual swimming resource
            return Resource.local().loadwait("paginae/act/swim").indir();
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
     * Gets the swimming icon from the actual swimming toggle button
     */
    private static Tex getSwimmingIcon() {
        try {
            if (nurgling.NUtils.getGameUI() != null && nurgling.NUtils.getGameUI().menu != null) {
                // Find the swimming toggle button and extract its icon
                for (MenuGrid.Pagina pag : nurgling.NUtils.getGameUI().menu.paginae) {
                    try {
                        String ref = ((Session.CachedRes.Ref)pag.res).resnm();
                        if (ref.equals("paginae/act/swim")) {
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
                        System.out.println("DEBUG: Error processing pagina: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting swimming icon: " + e.getMessage());
        }
        
        // Fallback: create a simple blue circle with "S"
        Coord buffSize = Buff.cframe.sz();
        BufferedImage img = TexI.mkbuf(buffSize);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(swimmingColor);
        g.fillOval(2, 2, buffSize.x - 4, buffSize.y - 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        String text = "S";
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
        
        // Draw our custom swimming icon instead of the resource icon
        if (swimmingIcon != null) {
            Coord iconPos = imgoff;
            // Scale the icon up by 15% to match other buff icon sizes
            Coord iconSize = swimmingIcon.sz();
            Coord scaledSize = new Coord((int)(iconSize.x * 1.15), (int)(iconSize.y * 1.15));
            // Center the scaled icon
            Coord centeredPos = iconPos.sub(scaledSize.sub(iconSize).div(2));
            g.image(swimmingIcon, centeredPos, scaledSize);
        }
        g.chcolor();
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return "Swimming mode is enabled";
    }
    
    /**
     * Checks if swimming is currently enabled by checking the swimming toggle state
     */
    public static boolean isSwimmingEnabled() {
        try {
            if (nurgling.NUtils.getGameUI() == null || nurgling.NUtils.getGameUI().menu == null) {
                return false;
            }
            
            // Check if the swimming pagina exists and if it's a toggle that's active
            for (MenuGrid.Pagina pag : nurgling.NUtils.getGameUI().menu.paginae) {
                try {
                    String ref = ((Session.CachedRes.Ref)pag.res).resnm();
                    if (ref.equals("paginae/act/swim")) {
                        // Check if this toggle is active by looking at the button
                        MenuGrid.PagButton button = pag.button();
                        if (button instanceof haven.res.ui.pag.toggle.Toggle) {
                            haven.res.ui.pag.toggle.Toggle toggle = (haven.res.ui.pag.toggle.Toggle) button;
                            return toggle.a;
                        }
                    }
                } catch (Exception e) {
                    // Skip this pagina if there's an error
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}