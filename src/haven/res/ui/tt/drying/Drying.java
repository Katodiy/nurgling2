package haven.res.ui.tt.drying;/* Preprocessed source code */
import haven.*;

import java.awt.*;
import java.awt.image.BufferedImage;

/* >tt: haven.res.ui.tt.drying.Drying */
@haven.FromResource(name = "ui/tt/drying", version = 3)
public class Drying extends ItemInfo implements GItem.MeterInfo, GItem.OverlayInfo<Tex>  {
    public final double done;

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
