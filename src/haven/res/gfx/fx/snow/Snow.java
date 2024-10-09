/* Preprocessed source code */
/*
  $use: lib/globfx
  $use: lib/env
*/

package haven.res.gfx.fx.snow;

import haven.*;
import haven.render.*;
import java.util.*;
import java.nio.*;
import haven.res.lib.globfx.*;
import haven.res.lib.env.*;

/* >wtr: Snow */
@haven.FromResource(name = "gfx/fx/snow", version = 1)
public class Snow implements Glob.Weather, RenderTree.Node, TickList.Ticking, TickList.TickNode {
    public static final int maxflakes = 50000;
    public static final VertexArray.Layout fmt =
	new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0,  0, 20),
			       new VertexArray.Layout.Input(Homo3D.normal, new VectorFormat(3, NumberFormat.SNORM8),  0, 12, 20),
			       new VertexArray.Layout.Input(Tex2D.texc,    new VectorFormat(2, NumberFormat.UNORM8),  0, 16, 20));

    public final Material[] flakemats;
    public final Random rnd = new Random();
    public final Flake flakes[] = new Flake[maxflakes];
    public final Map<Material, MSlot> matmap = new HashMap<Material, MSlot>();
    public MapView mv;
    public Coord2d cc;
    public int nf;
    public float rate, acc;
    private final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    private VertexArray va = null;

    private class MSlot implements Rendered, RenderTree.Node {
	final Material m;
	final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
	Flake flakes[] = new Flake[128];
	Model model = null;
	Model.Indices ind = null;
	boolean added = false, update = true;
	int nf;

	MSlot(Material m) {
	    this.m = m;
	}

	public void draw(Pipe state, Render out) {
	    if(model != null)
		out.draw(state, model);
	}

	FillBuffer fillind(Model.Indices dst, Environment env) {
	    FillBuffer ret = env.fillbuf(dst);
	    ByteBuffer buf = ret.push();
	    for(int i = 0; i < nf; i++) {
		Flake f = flakes[i];
		int vi = f.vidx * 4;
		buf.putInt(vi + 0);
		buf.putInt(vi + 1);
		buf.putInt(vi + 3);
		buf.putInt(vi + 1);
		buf.putInt(vi + 2);
		buf.putInt(vi + 3);
	    }
	    return(ret);
	}

	void update(Render d) {
	    if((model == null) || (model.n != nf * 6)) {
		if(model != null)
		    model.dispose();
		int indsz = (ind == null) ? 0 : ind.n;
		if((indsz < nf * 6) || (indsz > nf * 24))
		    indsz = Math.max(Integer.highestOneBit(nf * 6), 64) << 1;
		if((ind == null) || (indsz != ind.n)) {
		    if(ind != null)
			ind.dispose();
		    ind = new Model.Indices(indsz, NumberFormat.UINT32, DataBuffer.Usage.STREAM, null).shared();
		}
		model = new Model(Model.Mode.TRIANGLES, va, ind, 0, nf * 6);
		for(RenderTree.Slot slot : this.slots)
		    slot.update();
	    }
	    d.update(model.ind, this::fillind);
	}

	void add(Flake f) {
	    if(nf >= flakes.length)
		flakes = Arrays.copyOf(flakes, flakes.length * 2);
	    (flakes[nf] = f).midx = nf;
	    nf++;
	    update = true;
	}

	void remove(Flake f) {
	    (flakes[f.midx] = flakes[--nf]).midx = f.midx;
	    flakes[nf] = null;
	    update = true;
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(m);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    slots.remove(slot);
	}
    }

    public class Flake {
	float x, y, z;
	float xv, yv, zv;
	float nx, ny, nz;
	float nxv, nyv, nzv;
	float wr = 1.0f;
	float ar = (0.5f + rnd.nextFloat()) / 50;
	float ckt;
	MSlot m;
	Material mat;
	int vidx, midx;

	public Flake(Material mat, float x, float y, float z) {
	    this.mat = mat;
	    this.x = x; this.y = y; this.z = z;
	    nx = rnd.nextFloat();
	    ny = rnd.nextFloat();
	    nz = rnd.nextFloat();
	    if(nx < 0.5f) nx -= 1.0f;
	    if(ny < 0.5f) ny -= 1.0f;
	    if(nz < 0.5f) nz -= 1.0f;
	    float nf = 1.0f / (float)Math.sqrt((nx * nx) + (ny * ny) + (nz * nz));
	    nx *= nf;
	    ny *= nf;
	    nz *= nf;
	}

	public Flake(Material mat) {
	    this(mat, 0, 0, 0);
	}

	public Flake(Material mat, Coord3f c) {
	    this(mat, c.x, c.y, c.z);
	}

	public Material mat() {return(mat);};
	public float size() {return(1.5f);}
    }

    public void added(RenderTree.Slot slot) {
	RenderContext ctx = slot.state().get(RenderContext.slot);
	if(ctx instanceof PView.WidgetContext) {
	    PView wdg = ((PView.WidgetContext)ctx).widget();
	    if(wdg instanceof MapView)
		this.mv = (MapView)wdg;
	}
	for(MSlot mat : matmap.values()) {
	    if(mat.added)
		slot.add(mat);
	}
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }

    FillBuffer fillvert(VertexArray.Buffer dst, Environment env) {
	FillBuffer ret = env.fillbuf(dst);
	ByteBuffer buf = ret.push();
	for(int i = 0; i < nf; i++) {
	    Flake f = flakes[i];
	    float x = f.x, y = -f.y, z = f.z;
	    /*
	    byte nx = (byte)(Utils.clip(f.nx, -1, 1) * 127);
	    byte ny = (byte)(Utils.clip(f.ny, -1, 1) * 127);
	    byte nz = (byte)(Utils.clip(f.nz, -1, 1) * 127);
	    */
	    byte nx = 0, ny = 0, nz = 1;
	    float sz = f.size();
	    buf.putFloat(x + sz * f.nz);
	    buf.putFloat(y - sz * f.nz);
	    buf.putFloat(z + sz * (f.ny - f.nx));
	    buf.put(nx).put(ny).put(nz).put((byte)0);
	    buf.put((byte)0).put((byte)0).put((byte)0).put((byte)0);
	    buf.putFloat(x + sz * f.nz);
	    buf.putFloat(y + sz * f.nz);
	    buf.putFloat(z - sz * (f.nx - f.ny));
	    buf.put(nx).put(ny).put(nz).put((byte)0);
	    buf.put((byte)0).put((byte)255).put((byte)0).put((byte)0);
	    buf.putFloat(x - sz * f.nz);
	    buf.putFloat(y + sz * f.nz);
	    buf.putFloat(z + sz * (f.nx - f.ny));
	    buf.put(nx).put(ny).put(nz).put((byte)0);
	    buf.put((byte)255).put((byte)255).put((byte)0).put((byte)0);
	    buf.putFloat(x - sz * f.nz);
	    buf.putFloat(y - sz * f.ny);
	    buf.putFloat(z + sz * (f.nx + f.ny));
	    buf.put(nx).put(ny).put(nz).put((byte)0);
	    buf.put((byte)255).put((byte)0).put((byte)0).put((byte)0);
	}
	return(ret);
    }

    void move(float dt) {
	// Coord3f av = Environ.get(mv.ui.sess.glob).wind();
	Coord3f av = Coord3f.o;
	for(int i = 0; i < nf; i++) {
	    Flake f = flakes[i];
	    f.ckt += dt;
	    float xvd = f.xv - av.x, yvd = f.yv - av.y, zvd = f.zv - av.z;
	    xvd *= f.wr; yvd *= f.wr; zvd *= f.wr;
	    float vel = (float)Math.sqrt((xvd * xvd) + (yvd * yvd) + (zvd * zvd));

	    /* Rotate the normal around the normal velocity vector. */
	    float nvl = (float)Math.sqrt((f.nxv * f.nxv) + (f.nyv * f.nyv) + (f.nzv * f.nzv));
	    if(nvl > 0) {
		float s = (float)Math.sin(nvl * dt);
		float c = (float)Math.cos(nvl * dt);
		nvl = 1.0f / nvl;
		float nxvn = f.nxv * nvl, nyvn = f.nyv * nvl, nzvn = f.nzv * nvl;
		float nx = f.nx, ny = f.ny, nz = f.nz;
		f.nx = (nx * (nxvn * nxvn * (1 - c) + c)) + (ny * (nxvn * nyvn * (1 - c) - nzvn * s)) + (nz * (nxvn * nzvn * (1 - c) + nyvn * s));
		f.ny = (nx * (nyvn * nxvn * (1 - c) + nzvn * s)) + (ny * (nyvn * nyvn * (1 - c) + c)) + (nz * (nyvn * nzvn * (1 - c) - nxvn * s));
		f.nz = (nx * (nzvn * nxvn * (1 - c) - nyvn * s)) + (ny * (nzvn * nyvn * (1 - c) + nxvn * s)) + (nz * (nzvn * nzvn * (1 - c) + c));

		float df = (float)Math.pow(0.7, dt);
		f.nxv *= df;
		f.nyv *= df;
		f.nzv *= df;
	    }

	    /* Add the cross-product of the airspeed and the normal to the normal velocity. */
	    float vr = (vel * vel) / 5.0f, ar = 0.5f;
	    float rxvd = xvd + ((rnd.nextFloat() - 0.5f) * vr), ryvd = yvd + ((rnd.nextFloat() - 0.5f) * vr), rzvd = zvd + ((rnd.nextFloat() - 0.5f) * vr);
	    float nxv = f.nxv, nyv = f.nyv, nzv = f.nzv;
	    f.nxv += (f.ny * rzvd - f.nz * ryvd) * dt * ar;
	    f.nyv += (f.nz * rxvd - f.nx * rzvd) * dt * ar;
	    f.nzv += (f.nx * ryvd - f.ny * rxvd) * dt * ar;

	    float ae = Math.abs((f.nx * xvd) + (f.ny * yvd) + (f.nz * zvd));
	    float xa = (f.nx * ae - xvd), ya = (f.ny * ae - yvd), za = (f.nz * ae - zvd);
	    f.xv += xa * Math.abs(xa) * f.ar * dt;
	    f.yv += ya * Math.abs(ya) * f.ar * dt;
	    f.zv += za * Math.abs(za) * f.ar * dt;
	    f.x += f.xv * dt;
	    f.y += f.yv * dt;
	    f.z += f.zv * dt;
	    f.zv -= 9.82f * dt;
	}
    }

    private void remove(Flake flake) {
	int i = flake.vidx;
	flake.m.remove(flake);
	(flakes[i] = flakes[--nf]).vidx = i;
	flakes[nf] = null;
    }

    void ckstop(MCache map) {
	for(int i = 0; i < nf; i++) {
	    if(flakes[i].ckt < 1)
		continue;
	    flakes[i].ckt = 0;
	    boolean drop = false;
	    try {
		drop = flakes[i].z < map.getcz(flakes[i].x, flakes[i].y) - 1;
	    } catch(Loading e) {
		drop = true;
	    }
	    if(drop) {
		remove(flakes[i]);
		i--;
	    }
	}
    }

    public Snow(Object... args) {
	Collection<Material> mats = new ArrayList<>();
	for(Material.Res mr : Resource.classres(Snow.class).pool.load("gfx/fx/snow-1", 1).get().layers(Material.Res.class))
	    mats.add(mr.get());
	flakemats = mats.toArray(new Material[0]);
	update(args);
    }

    public Pipe.Op state() {
	return(null);
    }

    public void update(Object... args) {
	rate = ((Number)args[0]).floatValue();
    }

    @Override public void autogtick(Render d) {
	if(va == null)
	    va = new VertexArray(fmt, new VertexArray.Buffer(maxflakes * 4 * fmt.inputs[0].stride, DataBuffer.Usage.STREAM, null)).shared();
	for(MSlot m : matmap.values()) {
	    if(m.update)
		m.update(d);
	}
	d.update(va.bufs[0], this::fillvert);
    }
    public TickList.Ticking ticker() {return(this);}

    private void ckmslots() {
	for(MSlot m : matmap.values()) {
	    if(!m.added) {
		try {
		    RUtils.multiadd(this.slots, m);
		    m.added = true;
		} catch(Loading l) {
		}
	    }
	}
    }

    public boolean tick(double ddt) {
	float dt = (float)ddt;
	if(mv == null)
	    return(false);
	try {
	    this.cc = Coord2d.of(mv.getcc());
	} catch(Loading l) {
	    return(false);
	}
	ckmslots();
	ckstop(mv.ui.sess.glob.map);
	acc += rate * dt;
	int n = (int)Math.floor(acc);
	acc -= n;
	for(int i = 0; i < n; i++)
	    addflake();
	move(dt);
	return(nf == 0);
    }

    public void addflake(Flake flake) {
	if(nf >= maxflakes)
	    return;
	(flakes[nf] = flake).vidx = nf;
	Material m = flake.mat();
	if((flake.m = matmap.get(m)) == null)
	    matmap.put(m, flake.m = new MSlot(m));
	flake.m.add(flake);
	nf++;
    }

    public Coord3f maprandoom() {
	Coord2d base = Coord2d.of(((rnd.nextFloat() * 2) - 1) * 75 * 11,
				  ((rnd.nextFloat() * 2) - 1) * 75 * 11)
	    .add(cc);
	return(mv.ui.sess.glob.map.getzp(base));
    }

    public void addflake() {
	Material mat = flakemats[rnd.nextInt(flakemats.length)];
	try {
	    addflake(new Flake(mat, maprandoom().add(0, 0, 350)));
	} catch(Loading l) {
	}
    }
}
