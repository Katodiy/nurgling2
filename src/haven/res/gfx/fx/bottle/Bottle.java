/* Preprocessed source code */
package haven.res.gfx.fx.bottle;

import java.awt.image.*;
import java.nio.*;
import haven.*;
import haven.render.*;
import haven.render.sl.*;
import nurgling.NConfig;
import haven.render.DataBuffer;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;
import static haven.render.sl.Function.PDir.*;

/* >wtr: Bottle */
@haven.FromResource(name = "gfx/fx/bottle", version = 2)
public class Bottle implements RenderTree.Node, Glob.Weather {
    public static final Texture2D.Sampler2D cloud;
    public float[] mdeg = new float[4], tmdeg = new float[4];

    static {
	Raster src = Resource.classres(Bottle.class).layer(Resource.imgc).img.getRaster();
	Coord sz = PUtils.imgsz(src);
	WritableRaster buf = PUtils.byteraster(sz, 2);
	SNoise3 rnd = new SNoise3();
	for(int y = 0; y < sz.y; y++) {
	    for(int x = 0; x < sz.x; x++) {
		for(int b = 0; b < 2; b++)
		    buf.setSample(x, y, b, (src.getSample(x, y, b) - 128) & 0xff);
	    }
	}
	final byte[] pixels = ((java.awt.image.DataBufferByte)buf.getDataBuffer()).getData();
	Texture2D tex = new Texture2D(sz, DataBuffer.Usage.STATIC, new VectorFormat(2, NumberFormat.SNORM8),
				      (img, env) -> {
					  if(img.level != 0)
					      return(null);
					  FillBuffer fbuf = env.fillbuf(img);
					  fbuf.pull(ByteBuffer.wrap(pixels));
					  return(fbuf);
				      });
	cloud = new Texture2D.Sampler2D(tex);
    }

    public Bottle(Object... args) {
	update(args);
    }

    class Draw extends RUtils.AdHoc {
	final float[] mdeg;
	Draw(float[] mdeg) {super(code); this.mdeg = mdeg;}

	Float[] mdeg() {
	    Float[] ret = new Float[mdeg.length];
	    for(int i = 0; i < ret.length; i++)
		ret[i] = mdeg[i];
	    return(ret);
	}
    }
    public static final Uniform umdeg = new Uniform(new Array(FLOAT, 4), p -> ((Draw)p.get(RUtils.adhoc)).mdeg(), RUtils.adhoc);
    public static final Uniform ctex = new Uniform(SAMPLER2D, p-> cloud);
    public static final ShaderMacro code = new ShaderMacro() {
	    final double marg = 0.1;

	    Function fly = new Function.Def(VEC2) {{
		Expression tc = param(IN, VEC2).ref();
		Expression a = code.local(FLOAT, mul(FrameInfo.time(), l(0.075))).ref();
		Expression v1 = vec2(-0.345, 0.836);
		Expression v2 = vec2(0.5851, -0.419);
		Expression[] terms = new Expression[4];
		for(int i = 0; i < terms.length; i++) {
		    terms[i] = mul(mix(pick(texture2D(ctex.ref(), mul(add(tc, mul(a, v1)), l(1 << i))), "rg"),
				       pick(texture2D(ctex.ref(), mul(add(tc, mul(a, v2)), l(1 << i))), "rg"),
				       l(0.5)),
				   idx(umdeg.ref(), l(i)));
		}
		Expression off = code.local(VEC2, add(terms)).ref();
		/*
		Expression margvec = code.local(VEC2, mul(abs(sub(tc, l(0.5))), l(2.0))).ref();
		Expression margv = code.local(VEC2, vec2(pow(max(sub(l(1.0), mul(pick(margvec, "x"), l(1.0 / marg))), l(0.0)), l(2.0)),
							 pow(max(sub(l(1.0), mul(pick(margvec, "y"), l(1.0 / marg))), l(0.0)), l(2.0)))).ref();
		Expression moff = code.local(VEC2, mul(off,
						       mix(mul(abs(add(sign(sub(tc, l(0.5))), neg(sign(off)))), l(0.5)),
							   vec2(1, 1),
							   margv))).ref();
		*/
		code.add(new Return(add(tc, off)));
	    }};

	    public void modify(ProgramContext prog) {
		ValBlock.Value tc = Tex2D.get(prog).texcoord();
		tc.mod(fly::call, 10);
	    }
	};

    public final RenderContext.PostProcessor proc = new RenderContext.PostProcessor() {
	    public void run(GOut g, Texture2D.Sampler2D in) {
		if ((Boolean)NConfig.get(NConfig.Key.disableDrugEffects)) {
		    g.image(new TexRaw(in, true), Coord.z);  // Just pass through without effect
		    return;
		}
		g.usestate(new Draw(mdeg));
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

    public Pipe.Op state() {
	return(null);
    }

    public void update(Object... args) {
	for(int i = 0; i < Math.min(args.length, tmdeg.length); i++)
	    tmdeg[i] = ((Number)args[i]).floatValue();
    }

    public boolean tick(double ddt) {
	float dt = (float)ddt;
	for(int i = 0; i < mdeg.length; i++) {
	    if(mdeg[i] < tmdeg[i])
		mdeg[i] = Math.min(mdeg[i] + (dt * 0.001f), tmdeg[i]);
	    if(mdeg[i] > tmdeg[i])
		mdeg[i] = Math.max(mdeg[i] - (dt * 0.001f), tmdeg[i]);
	}
	return(false);
    }
}
