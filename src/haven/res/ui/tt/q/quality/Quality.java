/* Preprocessed source code */
package haven.res.ui.tt.q.quality;

/* $use: ui/tt/q/qbuff */
import haven.*;
import haven.res.ui.tt.q.qbuff.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import haven.MenuGrid.Pagina;
import nurgling.NGItem;

/* >tt: Quality */
@haven.FromResource(name = "ui/tt/q/quality", version = 26)
public class Quality extends QBuff implements GItem.OverlayInfo<Tex> {
    public static boolean show = Utils.getprefb("qtoggle", false);
    NGItem ownitem = null;
    boolean withContent = false;
    private static final BufferedImage icon = Resource.remote().loadwait("ui/tt/q/quality").layer(Resource.imgc, 0).scaled();
    public Quality(Owner owner, double q) {
	super(owner, icon, "Quality", q);
    if (owner instanceof NGItem) {
        ownitem = (NGItem) owner;
        ownitem.quality = (float) q;
    }
    }

    @Override
    public int order()
    {
        return 101;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new Quality(owner, ((Number)args[1]).doubleValue()));
    }

    public Tex overlay() {
        BufferedImage text = null;
        if (ownitem != null && !ownitem.content().isEmpty()) {
            withContent = true;
            text = GItem.NumberInfo.numrender((int) Math.round(ownitem.content().get(0).quality()), new Color(97, 121, 227, 255));
        } else {
            withContent = false;
            text = GItem.NumberInfo.numrender((int) Math.round(q), new Color(35, 245, 245, 255));
        }
        BufferedImage bi = new BufferedImage(text.getWidth(), text.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bi.createGraphics();
        Color rgb = new Color(0, 0, 0, 115);
        graphics.setColor(rgb);
        graphics.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        graphics.drawImage(text, 0, 0, null);
        return (new TexI(bi));
    }

    public void drawoverlay(GOut g, Tex ol) {
        g.aimage(ol, new Coord(g.sz().x - ol.sz().x, ol.sz().y), 0, 1);
    }
}
