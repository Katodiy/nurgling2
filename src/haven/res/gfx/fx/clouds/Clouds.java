/* Preprocessed source code */
package haven.res.gfx.fx.clouds;

import haven.*;
import haven.render.*;
import nurgling.NConfig;

/* >wtr: Clouds */
@haven.FromResource(name = "gfx/fx/clouds", version = 11)
public class Clouds implements Glob.Weather {
    public static final TexRender clouds = Resource.classres(Clouds.class).layer(TexR.class).tex();
    float  scale,  cmin,  cmax,  rmin,  rmax;
    float nscale, ncmin, ncmax, nrmin, nrmax;
    float oscale, ocmin, ocmax, ormin, ormax;
    float xv, yv;
    float ia = -1;

    public Clouds(Object... args) {
	update(args);
	scale = nscale;
	cmin = ncmin;
	cmax = ncmax;
	rmin = nrmin;
	rmax = nrmax;
	ia = -1;
    }

    public Pipe.Op state() {
	return(p -> {
		// Check if cloud shadows are disabled - allow dynamic toggling
		if ((Boolean)NConfig.get(NConfig.Key.disableCloudShadows)) {
		    return;  // Skip effect if disabled
		}
		CloudShadow ret = new CloudShadow(clouds, Glob.amblight(p), new Coord3f(xv, yv, 0), scale);
		ret.cmin = cmin; ret.cmax = cmax; ret.rmin = rmin; ret.rmax = rmax;
		ret.apply(p);
	    });
    }

    public void update(Object... args) {
	int n = 0;
	oscale = scale;
	ocmin = cmin;
	ocmax = cmax;
	ormin = rmin;
	ormax = rmax;
	nscale = 1.0f / (Integer)args[n++];
	ncmin = ((Number)args[n++]).floatValue() / 100.0f;
	ncmax = ((Number)args[n++]).floatValue() / 100.0f;
	nrmin = ((Number)args[n++]).floatValue() / 100.0f;
	nrmax = ((Number)args[n++]).floatValue() / 100.0f;
	if(args.length > n) {
	    xv = ((Number)args[n++]).floatValue();
	    yv = ((Number)args[n++]).floatValue();
	} else {
	    xv = 0.001f;
	    yv = 0.002f;
	}
	ia = 0;
    }

    public boolean tick(double ddt) {
	if(ia != -1) {
	    float dt = (float)ddt;
	    ia += dt;
	    if(ia >= 2) {
		scale = nscale;
		cmin = ncmin;
		cmax = ncmax;
		rmin = nrmin;
		rmax = nrmax;
		ia = -1;
	    } else {
		float A = ia / 2.0f, B = 1.0f - A;
		scale = (A * nscale) + (B * oscale);
		cmin = (A * ncmin) + (B * ocmin);
		cmax = (A * ncmax) + (B * ocmax);
		rmin = (A * nrmin) + (B * ormin);
		rmax = (A * nrmax) + (B * ormax);
	    }
	}
	return(false);
    }
}
