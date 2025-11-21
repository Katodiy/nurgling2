/* Preprocessed source code */
package haven.res.gfx.fx.shroomed;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import java.util.*;
import java.nio.*;
import java.awt.image.*;
import haven.render.DataBuffer;
import nurgling.NConfig;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;
import static haven.render.sl.Function.PDir.*;

/* >wtr: Shroomed */
@haven.FromResource(name = "gfx/fx/shroomed", version = 3)
public class Shroomed implements Glob.Weather, RenderTree.Node {
    public static final Indir<Resource> flash = Resource.classres(Shroomed.class).pool.load("gfx/fx/shroomflash", 2);
    public static final Texture2D.Sampler2D cloud;
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    public final Collection<Overlay> flashes = new ArrayList<>();
    public float str = 0, tstr = 0, nflash = -1;
    private final Random rnd = new Random();

    static {
	final Coord sz = new Coord(256, 256);
	WritableRaster buf = PUtils.alpharaster(sz);
	SNoise3 rnd = new SNoise3();
	for(int y = 0; y < sz.y; y++) {
	    double Y = (double)y / sz.y;
	    for(int x = 0; x < sz.x; x++) {
		double X = (double)x / sz.x;
		double a =
		    (rnd.getr(0, 1, 1.0 /  4.0, X - 0, Y - 0, 381) * 0.5714286) +
		    (rnd.getr(0, 1, 1.0 / 16.0, X - 0, Y - 0, 381) * 0.2857143) +
		    (rnd.getr(0 ,1, 1.0 / 64.0, X - 0, Y - 0, 381) * 0.1428571);
		double b =
		    (rnd.getr(0, 1, 1.0 /  4.0, X - 1, Y - 0, 381) * 0.5714286) +
		    (rnd.getr(0, 1, 1.0 / 16.0, X - 1, Y - 0, 381) * 0.2857143) +
		    (rnd.getr(0 ,1, 1.0 / 64.0, X - 1, Y - 0, 381) * 0.1428571);
		double c =
		    (rnd.getr(0, 1, 1.0 /  4.0, X - 0, Y - 1, 381) * 0.5714286) +
		    (rnd.getr(0, 1, 1.0 / 16.0, X - 0, Y - 1, 381) * 0.2857143) +
		    (rnd.getr(0 ,1, 1.0 / 64.0, X - 0, Y - 1, 381) * 0.1428571);
		double d =
		    (rnd.getr(0, 1, 1.0 /  4.0, X - 1, Y - 1, 381) * 0.5714286) +
		    (rnd.getr(0, 1, 1.0 / 16.0, X - 1, Y - 1, 381) * 0.2857143) +
		    (rnd.getr(0 ,1, 1.0 / 64.0, X - 1, Y - 1, 381) * 0.1428571);
		double v = (((a * (1 - X)) + (b * X)) * (1 - Y)) + (((c * (1 - X)) + (d * X)) * Y);
		buf.setSample(x, y, 0, (int)(255 * Utils.clip(v, 0, 1)));
	    }
	}
	final byte[] pixels = ((java.awt.image.DataBufferByte)buf.getDataBuffer()).getData();
	Texture2D tex = new Texture2D(sz, DataBuffer.Usage.STATIC, new VectorFormat(1, NumberFormat.UNORM8),
				      (img, env) -> {
					  if(img.level != 0)
					      return(null);
					  FillBuffer fbuf = env.fillbuf(img);
					  fbuf.pull(ByteBuffer.wrap(pixels));
					  return(fbuf);
				      });
	cloud = new Texture2D.Sampler2D(tex);
    }

    public static class Overlay implements RenderTree.Node {
	public final Sprite sub;
	public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

	public Overlay(Sprite spr) {
	    sub = spr;
	}

	public void added(RenderTree.Slot slot) {
	    slot.add(sub);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    slots.remove(slot);
	}

	public void remove() {
	    while(!slots.isEmpty())
		Utils.el(slots).remove();
	}
    }

    public Shroomed(Object... args) {
	update(args);
	str = tstr;
    }

    public static final Uniform ustr = new Uniform(FLOAT, p -> ((Haze)p.get(RUtils.adhoc)).a, RUtils.adhoc);
    public static final Uniform ctex = new Uniform(SAMPLER2D, p -> cloud);
    public static final Function filament = new Function.Def(FLOAT) {{
	Expression in = param(IN, FLOAT).ref();
	code.add(new Return(pow(sub(l(1.0), abs(sub(mul(in, l(2.0)), l(1.0)))), l(4.0))));
    }};
    public static final Function haze = new Function.Def(VEC4) {{
	Expression tc = param(IN, VEC2).ref();
	Expression ltc = code.local(VEC2, mul(tc, FrameConfig.u_screensize.ref(), vec2(l(1.0 / 1024.0)))).ref();
	Expression v = code.local(FLOAT, add(mul(filament.call(pick(texture2D(ctex.ref(), add(ltc, mul(vec2( 0.0491, 0.0313), FrameInfo.u_time.ref()))), "r")),
						 filament.call(pick(texture2D(ctex.ref(), add(ltc, mul(vec2(-0.0364, 0.0198), FrameInfo.u_time.ref()))), "r"))),
					     mul(pick(texture2D(ctex.ref(), add(mul(ltc, l(4.0)), mul(vec2( 0.010, 0.015), FrameInfo.u_time.ref()))), "r"),
						 pick(texture2D(ctex.ref(), add(mul(ltc, l(4.0)), mul(vec2(-0.012, 0.012), FrameInfo.u_time.ref()))), "r")))).ref();
	Block.Local col = code.local(VEC3, null);
	code.add(new If(lt(v, l(0.5)),
			stmt(ass(col, mix(vec3(0.5, 0.0, 1.0), vec3(1.0, 0.0, 1.0), mul(v, l(2.0))))),
			stmt(ass(col, mix(vec3(1.0, 0.0, 1.0), vec3(1.0, 0.5, 1.0), mul(sub(v, l(0.5)), l(2.0)))))));
	code.add(new Return(vec4(col.ref(), ustr.ref())));
    }};
    public static final ShaderMacro code = prog -> {
	FragColor.fragcol(prog.fctx).mod(in -> MiscLib.colblend.call(in, haze.call(Tex2D.get(prog).texcoord().ref())), 10);
    };
    public class Haze extends RUtils.AdHoc {
	final float a;
	public Haze(float a) {super(code); this.a = a;}
    }

    public final RenderContext.PostProcessor proc = new RenderContext.PostProcessor() {
	    public void run(GOut g, Texture2D.Sampler2D in) {
		if ((Boolean)NConfig.get(NConfig.Key.disableDrugEffects)) {
		    g.image(new TexRaw(in, true), Coord.z);  // Just pass through without effect
		    return;
		}
		g.usestate(new Haze(str));
		g.usestate(new FrameConfig(g.sz()));
		g.image(new TexRaw(in, true), Coord.z);
	    }
	};

    public void play(Sprite flash) {
	Overlay ol = new Overlay(flash);
	RUtils.multiadd(slots, ol);
	flashes.add(ol);
    }

    public void added(RenderTree.Slot slot) {
	/* See comment in sfx/ambient/weather/wsound */
	slot.ostate(Pipe.Op.compose(Homo3D.cam.nil, Location.xlate(Coord3f.of(0, 0, -10))));
	RenderContext ctx = slot.state().get(RenderContext.slot);
	ctx.add(proc);
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	RenderContext ctx = slot.state().get(RenderContext.slot);
	ctx.remove(proc);
	slots.remove(slot);
    }

    public Pipe.Op state() {
	return(null);
    }

    public void update(Object... args) {
	tstr = ((Number)args[0]).floatValue();
    }

    public boolean tick(double ddt) {
	float dt = (float)ddt;
	if(str < tstr)
	    str = Math.min(str + (dt * 0.1f), tstr);
	if(str > tstr)
	    str = Math.max(str - (dt * 0.1f), tstr);
	if(nflash < 0)
	    nflash = rnd.nextFloat() * 2;
	if((nflash -= (dt * str)) < 0) {
	    try {
		play(Sprite.create(null, flash.get(), Message.nil));
	    } catch(Loading l) {
	    }
	}
	for(Iterator<Overlay> i = flashes.iterator(); i.hasNext();) {
	    Overlay ol = i.next();
	    if(ol.sub.tick(ddt)) {
		ol.remove();
		i.remove();
	    }
	}
	return(false);
    }
}
