/* Preprocessed source code */
package haven.res.gfx.fx.floatimg;

import haven.*;
import haven.render.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import static haven.PUtils.*;

@haven.FromResource(name = "gfx/fx/floatimg", version = 6)
public class FloatSprite extends Sprite implements PView.Render2D {
    public static final double floaty = UI.scale(10.0);
    public final double tm;
    final Tex tex;
    final int sy;
    double a = 0;

    public int cury() {
	return(sy - (int)(floaty * a));
    }

    public FloatSprite(Owner owner, Resource res, Tex tex, int tm) {
	super(owner, res);
	this.tex = tex;
	this.tm = tm;
	this.sy = place(owner.context(Gob.class), tex.sz().y);
    }

    private static int place(Gob gob, int h) {
	int y = 0;
	trying: while(true) {
	    for(Gob.Overlay ol : gob.ols) {
		if(ol.spr instanceof FloatSprite) {
		    FloatSprite f = (FloatSprite)ol.spr;
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

    public boolean tick(double dt) {
	a += dt / tm;
	return(a >= 1);
    }
}
