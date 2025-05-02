package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.routes.Route;

import java.awt.image.BufferedImage;

public class RouteLabel extends Sprite implements RenderTree.Node, PView.Render2D {
    private static final Coord3f Z_OFFSET = new Coord3f(0, 0, 0); // Slightly above the ground
    private TexI label;
    public static final double floaty = UI.scale(5.0);
    Route route;
    public final Tex tex = Resource.loadtex("gfx/hud/chkmarks");
    double a = 0;
    final int sy;

    public RouteLabel(Owner owner, Route route) {
        super(owner, null);
        this.route = route;
        this.sy = place(owner.context(Gob.class), tex.sz().y);
        update();
    }

    private static int place(Gob gob, int h) {
        int y = 0;
        trying: while(true) {
            for(Gob.Overlay ol : gob.ols) {
                if(ol.spr instanceof RouteLabel) {
                    RouteLabel f = (RouteLabel)ol.spr;
                    int y2 = f.cury();
                    int h2 = f.tex.sz().y;
                    if(((y2 >= y) && (y2 < y + h)) ||
                            ((y >= y2) && (y < y2 + h2))) {
                        y = y2 - h;
                        continue trying;
                    }
                }
            }
            return(y);
        }
    }

    public int cury() {
        return(sy - (int)(floaty * a));
    }

    private void update() {
        // Create a simple icon or label (e.g. a dot or waypoint marker)
        BufferedImage img = NStyle.openings.render(route.name).img;;
        label = new TexI(img);
    }

    @Override
    public boolean tick(double dt) {
        // Remove sprite if the gob was removed
        return haven.Utils.eq(NUtils.findGob(((Gob) owner).id), null);
    }

    @Override
    public void draw(GOut g, Pipe state) {
        Coord sc = Homo3D.obj2view(Coord3f.o, state, Area.sized(Coord.z, g.sz())).round2();
        if(sc == null)
            return;
        int α;
        if(a < 0.75)
            α = 255;
        else
            α = (int)Utils.clip(255 * ((1 - a) / 0.25), 0, 255);
        g.chcolor(255, 255, 255, α);
        Coord c = tex.sz().inv();
        c.x = c.x / 2;
        c.y += cury();
        c.y -= 15;
        g.image(tex, sc.add(c));
        g.chcolor();
    }
}
