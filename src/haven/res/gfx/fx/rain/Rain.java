/* Preprocessed source code */
/* $use: lib/vertspr */

package haven.res.gfx.fx.rain;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import haven.res.lib.vertspr.*;
import java.util.*;
import java.nio.*;
import haven.render.VertexArray.Layout;
import static haven.render.sl.Cons.*;
import static haven.Utils.sb;

/* XXX: Remove me as soon as custom clients can be expected to have
 * merged the fixes from mainline. */
@haven.FromResource(name = "gfx/fx/rain", version = 2)
public class Rain implements Glob.Weather, RenderTree.Node {
    public static final float sz = 75 * 11;
    public static final float ft = 0.03f;
    public static final float droplife = 1.5f, splashlife = 0.15f;
    public static final float svf = 15;
    public static final Layout vfmt = new Layout(new Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 20),
						 new Layout.Input(Homo3D.normal, new VectorFormat(3, NumberFormat.SNORM8), 0, 12, 20),
						 new Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.UNORM8), 0, 16, 20));
    public static final Rendered.Order draworder = new Rendered.Order.Default(20000);
    public static final Pipe.Op mat = new Light.PhongLight(true,
							   new FColor(1.00f, 1.50f, 2.00f),
							   new FColor(1.00f, 1.50f, 2.00f),
							   new FColor(1.00f, 1.50f, 2.00f),
							   new FColor(0.25f, 0.37f, 0.50f), 10);
    public MapView mv;
    public Coord2d cc;
    public Coord3f wv = Coord3f.of(50, 20, -300), wvr = Coord3f.of(15f, 15f, 150f);
    public float rate = 0, acc = 0;
    public final Collection<Drop> drops = new FastArrayList2<>();
    public final Collection<Splash> splashes = new FastArrayList2<>();
    public final DropSprite dropspr = new DropSprite();
    public final SplashSprite splashspr = new SplashSprite();
    private final Random rnd = new Random();

    public static int snorm8(float v) {
	return(((int)(v * 0x7f)) & 0xff);
    }

    public class Drop {
	float tx, ty, tz;
	float xv, yv, zv;
	float tm, life;
	int norm;

	Drop(Coord3f tc, float itm) {
	    xv = wv.x + (((rnd.nextFloat() * 2) - 1) * wvr.x);
	    yv = wv.y + (((rnd.nextFloat() * 2) - 1) * wvr.y);
	    zv = wv.z + (((rnd.nextFloat() * 2) - 1) * wvr.z);
	    tx = tc.x; ty = -tc.y; tz = tc.z;
	    tm = rnd.nextFloat() * itm;
	    life = droplife;
	    float nx = (rnd.nextFloat() * 2) - 1, ny = (rnd.nextFloat() * 2) - 1;
	    float nz = (float)Math.sqrt(1 - (nx * nx) - (ny * ny));
	    norm = (snorm8(nx) << 0) | (snorm8(ny) << 8) | (snorm8(nz) << 8);
	}
    }

    public class DropSprite extends DynSprite implements TickList.Ticking, TickList.TickNode {
	public DropSprite() {
	    super(null, null);
	    ostate(VertexColor.instance, new States.LineWidth(1),
		   mat, draworder, States.maskdepth);
	}

	public void draw(Pipe state, Render out) {
	    super.draw(state, out);
	}

	public VertexArray.Layout fmt() {return(vfmt);}
	public Model.Mode mode() {return(Model.Mode.LINES);}

	private FillBuffer fill(VertexArray.Buffer dst, Environment env) {
	    FillBuffer ret = env.fillbuf(dst);
	    ByteBuffer buf = ret.push();
	    
	    for(Drop d : drops) {
		float t = d.life - d.tm;
		buf.putFloat(d.tx - (t * d.xv)).putFloat(d.ty - (t * d.yv)).putFloat(d.tz - (t * d.zv));
		buf.putInt(d.norm);
		buf.putInt(0x80ffffff);
		t += ft;
		buf.putFloat(d.tx - (t * d.xv)).putFloat(d.ty - (t * d.yv)).putFloat(d.tz - (t * d.zv));
		buf.putInt(d.norm);
		buf.putInt(0x00ffffff);
	    }
	    return(ret);
	}

	public void autogtick(Render g) {
	    update(g, this::fill, drops.size() * 2 * vfmt.inputs[0].stride);
	}

	public TickList.Ticking ticker() {return(this);}
    }

    public class Splash {
	float ox, oy, oz;
	float xv, yv;
	float tm, life, ilife;
	int norm;

	Splash(Drop d) {
	    ox = d.tx; oy = d.ty; oz = d.tz;
	    xv = ((rnd.nextFloat() * 2) - 1) * svf; yv = ((rnd.nextFloat() * 2) - 1) * svf;
	    life = splashlife + (rnd.nextFloat() * splashlife);
	    ilife = 1 / life;
	    tm = d.tm - d.life;
	    norm = d.norm;
	}
    }

    public class SplashSprite extends DynSprite implements TickList.Ticking, TickList.TickNode {
	public SplashSprite() {
	    super(null, null);
	    ostate(VertexColor.instance, new States.LineWidth(1),
		   mat, draworder, States.maskdepth);
	}

	public void draw(Pipe state, Render out) {
	    super.draw(state, out);
	}

	public VertexArray.Layout fmt() {return(vfmt);}
	public Model.Mode mode() {return(Model.Mode.LINES);}

	private FillBuffer fill(VertexArray.Buffer dst, Environment env) {
	    final float G = -98.2f;
	    FillBuffer ret = env.fillbuf(dst);
	    ByteBuffer buf = ret.push();
	    for(Splash d : splashes) {
		float izv = -G * d.life;
		float t = d.tm;
		buf.putFloat(d.ox + (d.xv * t)).putFloat(d.oy + (d.yv * t));
		buf.putFloat((G * t * t) + (izv * t) + d.oz);
		buf.putInt(d.norm);
		buf.putInt(0x80ffffff);
		t = Math.max(t - (ft * 2), 0);
		buf.putFloat(d.ox + (d.xv * t)).putFloat(d.oy + (d.yv * t));
		buf.putFloat((G * t * t) + (izv * t) + d.oz);
		buf.putInt(d.norm);
		buf.putInt(0x00ffffff);
	    }
	    return(ret);
	}

	public void autogtick(Render g) {
	    update(g, this::fill, splashes.size() * 2 * vfmt.inputs[0].stride);
	}

	public TickList.Ticking ticker() {return(this);}
    }

    public Rain(Object... args) {
	update(args);
    }

    public Pipe.Op state() {return(null);}

    public void update(Object... args) {
	rate = ((Number)args[0]).floatValue();
	if(args.length > 1) {
	    wv = Coord3f.of(((Number)args[1]).floatValue(),
			    ((Number)args[2]).floatValue(),
			    ((Number)args[3]).floatValue());
	}
	if(args.length > 4) {
	    wvr = Coord3f.of(((Number)args[4]).floatValue(),
			     ((Number)args[5]).floatValue(),
			     ((Number)args[6]).floatValue());
	}
    }

    public Coord3f maprandoom() {
	Coord2d base = Coord2d.of(((rnd.nextFloat() * 2) - 1) * sz,
				  ((rnd.nextFloat() * 2) - 1) * sz)
	    .add(cc);
	return(mv.ui.sess.glob.map.getzp(base));
    }

    public void mkdrop(float dt) {
	Coord3f tc;
	try {
	    tc = maprandoom();
	} catch(Loading l) {
	    return;
	}
	drops.add(new Drop(tc, dt));
    }

    /*
    public void mklandfx(Coord3f c) {
	mv.ui.sess.glob.loader.defer(() -> {
		Gob n = mv.ui.sess.glob.oc.new Virtual(Coord2d.of(c), rnd.nextFloat() * (float)Math.PI * 2) {
			public Coord3f getc() {
			    return(c);
			}

			protected Pipe.Op getmapstate(Coord3f pc) {
			    return(null);
			}
		    };
		n.addol(new Gob.Overlay(n, -1, Resource.classres(Rain.class).pool.load("gfx/fx/waterdroplet", 1), Message.nil), false);
		mv.ui.sess.glob.oc.add(n);
	    }, null);
    }
    */

    private boolean first = true;
    public boolean tick(double ddt) {
	if(mv == null)
	    return(false);
	float dt = (float)ddt;
	try {
		Coord3f coord3f = mv.getcc();
		if(coord3f!=null)
	    	this.cc = Coord2d.of(coord3f);
	} catch(Loading l) {
	    return(false);
	}
	float itm = first ? droplife : dt;
	acc += rate * itm;
	int n = (int)Math.floor(acc);
	acc -= n;
	first = false;
	for(int i = 0; i < n; i++)
	    mkdrop(itm);
	for(Iterator<Drop> i = drops.iterator(); i.hasNext();) {
	    Drop d = i.next();
	    if((d.tm += dt) >= d.life) {
		i.remove();
		for(int o = 0; o < 4; o++)
		    splashes.add(new Splash(d));
	    }
	}
	for(Iterator<Splash> i = splashes.iterator(); i.hasNext();) {
	    Splash d = i.next();
	    if((d.tm += dt) >= d.life) {
		i.remove();
	    }
	}
	return(false);
    }

    public void added(RenderTree.Slot slot) {
	RenderContext ctx = slot.state().get(RenderContext.slot);
	if(ctx instanceof PView.WidgetContext) {
	    PView wdg = ((PView.WidgetContext)ctx).widget();
	    if(wdg instanceof MapView)
		this.mv = (MapView)wdg;
	}
	slot.add(dropspr);
	slot.add(splashspr);
    }
}
