package nurgling.widgets;

import haven.*;
import nurgling.NConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Crime status indicator that appears in the buff area when crime is enabled
 */
public class CrimeStatusBuff extends Buff {
    private static final Color crimeColor = new Color(255, 0, 0, 180); // Red
    private static Tex crimeIcon = null;
    
    public CrimeStatusBuff() {
        super(createCrimeResource());
        if (crimeIcon == null) {
            crimeIcon = getCrimeIcon();
        }
    }
    
    /**
     * Creates a resource reference for the crime buff
     */
    private static Indir<Resource> createCrimeResource() {
        try {
            // Try to use the actual crime resource
            return Resource.local().loadwait("paginae/act/crime").indir();
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
     * Gets the crime icon from the actual crime toggle button
     */
    private static Tex getCrimeIcon() {
        try {
            if (nurgling.NUtils.getGameUI() != null && nurgling.NUtils.getGameUI().menu != null) {
                // Find the crime toggle button and extract its icon
                for (MenuGrid.Pagina pag : nurgling.NUtils.getGameUI().menu.paginae) {
                    try {
                        String ref = ((Session.CachedRes.Ref)pag.res).resnm();
                        if (ref.equals("paginae/act/crime")) {
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
            // Error getting crime icon
        }
        
        // Fallback: create a simple red circle with "C"
        Coord buffSize = Buff.cframe.sz();
        BufferedImage img = TexI.mkbuf(buffSize);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(crimeColor);
        g.fillOval(2, 2, buffSize.x - 4, buffSize.y - 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        String text = "C";
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
        
        // Draw our custom crime icon instead of the resource icon
        if (crimeIcon != null) {
            Coord iconPos = imgoff;
            // Scale the icon up by 15% to match other buff icon sizes
            Coord iconSize = crimeIcon.sz();
            Coord scaledSize = new Coord((int)(iconSize.x * 1.15), (int)(iconSize.y * 1.15));
            // Center the scaled icon
            Coord centeredPos = iconPos.sub(scaledSize.sub(iconSize).div(2));
            g.image(crimeIcon, centeredPos, scaledSize);
        }
        g.chcolor();
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return "Crime mode is enabled";
    }
}