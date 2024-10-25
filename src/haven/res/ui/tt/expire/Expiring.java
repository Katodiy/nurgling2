package haven.res.ui.tt.expire;/* Preprocessed source code */
import haven.*;

import java.awt.*;
import java.awt.image.BufferedImage;

/* >tt: haven.res.ui.tt.expire.Expiring */
@haven.FromResource(name = "ui/tt/expire", version = 6)
public class Expiring extends ItemInfo implements GItem.MeterInfo, GItem.OverlayInfo<Tex> {
    public final double stime, etime;
    public final Glob glob;

    public Expiring(Owner owner, double stime, double etime) {
        super(owner);
        this.stime = stime;
        this.etime = etime;
        this.glob = owner.context(Glob.class);
    }

    public static Expiring mkinfo(Owner owner, Object... args) {
        double stime = ((Number) args[1]).doubleValue();
        double etime = ((Number) args[2]).doubleValue();
        return (new Expiring(owner, stime, etime));
    }

    public double meter() {
        return (Utils.clip((glob.globtime() - stime) / (etime - stime), 0.0, 1.0));
    }


    public Tex overlay() {
        String textval = String.format("%d%%", (int)(meter()*100));
        BufferedImage text = Text.render(textval,new Color(234, 164, 101, 255)).img;
        BufferedImage bi = new BufferedImage(text.getWidth(), text.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bi.createGraphics();
        Color rgb = new Color(0, 0, 0, 115);
        graphics.setColor(rgb);
        graphics.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        graphics.drawImage(text, 0, 0, null);
        return (new TexI(bi));
    }

    public void drawoverlay(GOut g, Tex ol) {
        g.aimage(ol, new Coord(0, g.sz().y - ol.sz().y), 0, 0);
    }
}
