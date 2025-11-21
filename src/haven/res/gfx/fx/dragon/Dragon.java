/* Preprocessed source code */
package haven.res.gfx.fx.dragon;

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

/* >wtr: Dragon */
@haven.FromResource(name = "gfx/fx/dragon", version = 76)
public class Dragon implements RenderTree.Node, Glob.Weather {
    public static final Texture2D.Sampler2D cloud;
    public float mdeg, cdeg, tcdeg;

    static {
	Raster src = Resource.classres(Dragon.class).layer(Resource.imgc).img.getRaster();
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

    public Dragon(Object... args) {
	update(args);
    }

    class Draw extends RUtils.AdHoc {
	final float mdeg, cdeg;
	Draw(float mdeg, float cdeg) {super(code); this.mdeg = mdeg; this.cdeg = cdeg;}
    }
    public static final Uniform umdeg = new Uniform(FLOAT, p -> ((Draw)p.get(RUtils.adhoc)).mdeg, RUtils.adhoc);
    public static final Uniform umdegi = new Uniform(FLOAT, p -> 1.0f / ((Draw)p.get(RUtils.adhoc)).mdeg, RUtils.adhoc);
    public static final Uniform ucdeg = new Uniform(FLOAT, p -> ((Draw)p.get(RUtils.adhoc)).cdeg, RUtils.adhoc);
    public static final Uniform ctex = new Uniform(SAMPLER2D, p-> cloud);
    public static final ShaderMacro code = new ShaderMacro() {
	    final double marg = 0.1;

	    Function fly = new Function.Def(VEC2) {{
		Expression a = code.local(FLOAT, mul(FrameInfo.time(), l(0.15))).ref();
		Expression v1 = vec2(-0.345, 0.836);
		Expression v2 = vec2(0.5851, -0.419);
		code.add(new Return(mul(mix(pick(texture2D(ctex.ref(), add(Tex2D.rtexcoord.ref(), mul(a, v1))), "rg"),
					    pick(texture2D(ctex.ref(), add(Tex2D.rtexcoord.ref(), mul(a, v2))), "rg"),
					    l(0.5)),
					umdeg.ref())));
	    }};

	    Function margoff = new Function.Def(VEC4) {{
		Expression in = param(IN, VEC4).ref();
		Expression tc = param(IN, VEC2).ref();
		Expression off = param(IN, VEC2).ref();
		Expression margvec = code.local(VEC2, sub(l(0.5), abs(sub(tc, l(0.5))))).ref();
		Expression margv = code.local(FLOAT, min(add(pow(max(sub(l(1.0), mul(pick(margvec, "x"), l(1.0 / marg))), l(0.0)), l(2.0)),
							     pow(max(sub(l(1.0), mul(pick(margvec, "y"), l(1.0 / marg))), l(0.0)), l(2.0))),
							   l(1.0))).ref();
		Expression mono = code.local(FLOAT, dot(pick(in, "rgb"), vec3(l(1.0 / 3.0)))).ref();
		Expression moffed = code.local(VEC4, vec4(mix(mix(pick(in, "rgb"),
								  mul(mono, vec3(1.0, 0.0, 0.0)),
								  mul(mix(l(0.40), l(0.8), mul(abs(pick(off, "x")), umdegi.ref())),
								      ucdeg.ref())),
							      mul(mono, vec3(0.0, 0.0, 1.0)),
							      mul(mix(l(0.20), l(0.4), mul(abs(pick(off, "y")), umdegi.ref())),
								  ucdeg.ref())),
							  pick(in, "a"))).ref();
		code.add(new Return(mix(moffed, vec4(0.3, 0, 0.15, 1), mul(margv, clamp(mul(ucdeg.ref(), l(5.0)), l(0.0), l(1.0))))));
	    }};

	    public void modify(ProgramContext prog) {
		ValBlock.Value off = prog.fctx.uniform.ext(fly, () -> prog.fctx.uniform.new Value(VEC2) {
			public Expression root() {
			    return(fly.call());
			}
		    });
		ValBlock.Value tc = Tex2D.get(prog).texcoord();
		tc.mod(in -> add(in, off.depref()), 10);
		FragColor.fragcol(prog.fctx).mod(in -> margoff.call(in, tc.ref(), off.ref()), 10);
	    }
	};

    public final RenderContext.PostProcessor proc = new RenderContext.PostProcessor() {
	    public void run(GOut g, Texture2D.Sampler2D in) {
		if ((Boolean)NConfig.get(NConfig.Key.disableDrugEffects)) {
		    g.image(new TexRaw(in, true), Coord.z);  // Just pass through without effect
		    return;
		}
		float cmdeg = mdeg * cdeg;
		g.usestate(new Draw(cmdeg, cdeg));
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
	mdeg = ((Number)args[0]).floatValue();
	tcdeg = ((Number)args[1]).floatValue();
    }

    public boolean tick(double ddt) {
	float dt = (float)ddt;
	if(cdeg < tcdeg)
	    cdeg = Math.min(cdeg + (dt * 0.01f), tcdeg);
	if(cdeg > tcdeg)
	    cdeg = Math.max(cdeg - (dt * 0.01f), tcdeg);
	return(false);
    }
}
