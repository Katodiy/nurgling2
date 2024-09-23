package nurgling.overlays;

import haven.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NBarrelOverlay extends NObjectTexLabel
{
    String text = null;


    public static final Font bsans  = new Font("Sans", Font.BOLD, 10);
    private static final Text.Furnace active_title = new PUtils.BlurFurn(new Text.Foundry(bsans, 15, Color.YELLOW).aa(true), 2, 1, new Color(36, 25, 25));

    Gob gob;
    public NBarrelOverlay(Owner owner)
    {
        super(owner);
        gob = (Gob) owner;
        pos = new Coord3f(0,0,9);
    }

    @Override
    public boolean tick(double dt)
    {
        boolean found = false;
        for (Gob.Overlay ol : gob.ols) {
            if (ol.spr instanceof StaticSprite) {
                found = true;
                String buf = ((StaticSprite)ol.spr).toString();
                String ntext = buf.substring(buf.indexOf("barrel-")+7,buf.indexOf(">"));
                if(!ntext.equals(text)) {
                    text = ntext;
                    img = null;
                    label = new TexI(active_title.render(text).img);
                }
            }
        }
        if(!found)
        {
            text = null;
            img = null;
            label = null;
        }
        return super.tick(dt);
    }
}
