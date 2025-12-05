/* Preprocessed source code */
package haven.res.gfx.fx.wet;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.awt.Color;
import static haven.render.sl.Cons.*;

@haven.FromResource(name = "gfx/fx/wet", version = 2)
public class Wet extends State {
    public static final Slot<Wet> slot = new Slot<>(Slot.Type.DRAW, Wet.class);
    public final FColor col;
    public final float shine;

    public Wet(FColor col, float shine) {
	this.col = col;
	this.shine = shine;
    }

    public static final Uniform param = new Uniform(Type.VEC4, p -> {
	    Wet w = p.get(slot);
	    return(new float[] {w.col.r, w.col.g, w.col.b, w.shine});
	}, slot);
    public static final ShaderMacro shader = prog -> {
	Phong ph = prog.getmod(Phong.class);
	if((ph == null) || !ph.pfrag)
	    return;
	Phong.DoLight l = ph.dolight;
	ph.dolight.mod(() -> {
		Expression wsl = pow(max(dot(l.edir, reflect(neg(l.dir.ref()), l.norm)), l(0.0)), pick(param.ref(), "a"));
		l.dcalc.add(aadd(l.spec, mul(pick(param.ref(), "rgb"),
					     pick(fref(l.ls, "spc"), "rgb"),
					     wsl, l.lvl.ref())));
	    }, 0);
    };

    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe p) {
	p.put(slot, this);
    }

    public boolean equals(Wet that) {
	return(Utils.eq(this.col, that.col) && this.shine == that.shine);
    }

    public boolean equals(Object o) {
	return((o instanceof Wet) && equals((Wet)o));
    }
}

/* >wtr: Damp */
