/* Preprocessed source code */
package haven.res.gfx.fx.lucy;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import nurgling.NConfig;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;
import static haven.render.sl.Function.PDir.*;

@haven.FromResource(name = "gfx/fx/lucy", version = 31)
public class Lucy extends RUtils.AdHoc {
    public static final Uniform cstr = new Uniform(FLOAT, p -> {
	if ((Boolean)NConfig.get(NConfig.Key.disableDrugEffects)) {
	    return 0.0f;  // Disable effect by setting strength to 0
	}
	return ((Lucy)p.get(RUtils.adhoc)).str;
    }, RUtils.adhoc);
    public static final Uniform cph = new Uniform(FLOAT, p -> ((Lucy)p.get(RUtils.adhoc)).ph, RUtils.adhoc);
    public final float str, ph;

    public Lucy(float str, float ph) {
	super(shader);
	this.str = str;
	this.ph = ph;
    }

    public static final Function rotate = new Function.Def(VEC4) {{
	Expression col = param(IN, VEC4).ref();
	Expression n = code.local(FLOAT, mod(cph.ref(), l(3.0))).ref();
	LValue rot = code.local(VEC4, null).ref();
	code.add(new If(lt(n, l(1.0)),
			stmt(ass(rot, mix(pick(col, "grba"), pick(col, "gbra"), smoothstep(l(0.0), l(1.0), n)))),
			new If(lt(n, l(2.0)),
			       stmt(ass(rot, mix(pick(col, "gbra"), pick(col, "brga"), smoothstep(l(1.0), l(2.0), n)))),
			       stmt(ass(rot, mix(pick(col, "brga"), pick(col, "grba"), smoothstep(l(2.0), l(3.0), n)))))));
	code.add(new Return(mix(col, rot, cstr.ref())));
    }};
    private static final ShaderMacro shader = prog -> {
	FragColor.fragcol(prog.fctx).mod(rotate::call, 5000);
    };
}

/* >wtr: LucySky */
