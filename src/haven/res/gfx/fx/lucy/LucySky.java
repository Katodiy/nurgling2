/* Preprocessed source code */
package haven.res.gfx.fx.lucy;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;
import static haven.render.sl.Function.PDir.*;

@haven.FromResource(name = "gfx/fx/lucy", version = 31)
public class LucySky implements RenderTree.Node, Glob.Weather {
    float fstr, sp, ph = 0, str = 0;

    public Pipe.Op state() {
	return(null);
    }

    public LucySky(Object... args) {
	update(args);
    }

    public void update(Object... args) {
	fstr = ((Number)args[0]).floatValue();
	sp = ((Number)args[1]).floatValue();
    }

    public boolean tick(double ddt) {
	float dt = (float)ddt;
	if(str < fstr)
	    str = Math.min(str + (dt * 0.01f), fstr);
	if(str > fstr)
	    str = Math.max(str - (dt * 0.01f), fstr);
	ph += dt * sp;
	return(false);
    }

    public final RenderContext.PostProcessor proc = new RenderContext.PostProcessor() {
	    public void run(GOut g, Texture2D.Sampler2D in) {
		g.usestate(new Lucy(str, ph));
		g.image(new TexRaw(in, true), Coord.z);
	    }
	};

    public void added(RenderTree.Slot slot) {
	RenderContext ctx = slot.state().get(RenderContext.slot);
	ctx.add(proc);
    }

    public void removed(RenderTree.Slot slot) {
	RenderContext ctx = slot.state().get(RenderContext.slot);
	ctx.remove(proc);
    }
}
