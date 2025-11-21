/* Preprocessed source code */
package haven.res.gfx.fx.wet;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.awt.Color;
import static haven.render.sl.Cons.*;

@haven.FromResource(name = "gfx/fx/wet", version = 2)
public class Damp implements Glob.Weather {
    FColor ccol, ncol, ocol;
    float cshine, nshine, oshine, ia = -1;

    public Damp(Object... args) {
	update(args);
	ccol = ncol;
	cshine = nshine;
	ia = -1;
    }

    public Pipe.Op state() {
	return(new Wet(ccol, cshine));
    }

    public void update(Object... args) {
	ocol = ccol;
	oshine = cshine;
	if(args[0] instanceof Color)
	    ncol = new FColor((Color)args[0]);
	else
	    ncol = (FColor)args[0];
	nshine = ((Number)args[1]).floatValue();
	ia = 0;
    }

    public boolean tick(double ddt) {
	if(ia >= 0) {
	    float dt = (float)ddt;
	    ia += dt;
	    if(ia >= 2) {
		ccol = ncol;
		cshine = nshine;
		ia = -1;
	    } else {
		float A = ia / 2.0f, B = 1.0f - A;
		ccol = ocol.blend(ncol, A);
		cshine = (A * nshine) + (B * oshine);
	    }
	}
	return(false);
    }
}
