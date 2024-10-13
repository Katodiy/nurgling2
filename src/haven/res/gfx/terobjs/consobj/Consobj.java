/* Preprocessed source code */
/* $use: lib/obst */

package haven.res.gfx.terobjs.consobj;

import haven.*;
import haven.render.*;
import haven.res.lib.obst.*;
import nurgling.NConfig;

import static haven.MCache.tilesz;

/* >spr: Consobj */
@haven.FromResource(name = "gfx/terobjs/consobj", version = 35)
public class Consobj extends Sprite implements Sprite.CUpd {
    public final static Indir<Resource> signres = Resource.remote().load("gfx/terobjs/sign", 6);
    public final static Indir<Resource> poleres = Resource.remote().load("gfx/terobjs/arch/conspole", 2);
    public final static float bscale = 1f / 11;
    private static Material bmat = null;
    public final Gob gob = owner.context(Gob.class);
    public final ResData built;
    public float done;
    final Coord3f cc;
    final Sprite sign, pole;
    public final Location[] poles;
    final MCache map;
    final RenderTree.Node bound;

    Coord3f gnd(float rx, float ry) {
	double a = -gob.a;
	float s = (float)Math.sin(a), c = (float)Math.cos(a);
	float gx = rx * c + ry * s, gy = ry * c - rx * s;
	if(!(Boolean) NConfig.get(NConfig.Key.flatsurface))
		return(new Coord3f(rx, -ry, map.getcz(gx + cc.x, gy + cc.y) - cc.z));
	else
		return(new Coord3f(rx, -ry, 0));
    }

    public Consobj(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	this.map = owner.context(Glob.class).map;
	if(bmat == null)
	    bmat = Resource.classres(Consobj.class).layer(Material.Res.class).get();
	Obstacle obst = Obstacle.parse(sdt);
	done = sdt.uint8() / 255.0f;
	if(!sdt.eom()) {
	    int resid = sdt.uint16();
	    built = new ResData(owner.context(Resource.Resolver.class).getres(resid), new MessageBuf(sdt.bytes()));
	} else {
	    built = null;
	}
	sign = Sprite.create(owner, signres.get(), Message.nil);
	pole = Sprite.create(owner, poleres.get(), Message.nil);
	this.cc = gob.getrc();
	poles = new Location[obst.verts().size()];
	if(obst.p.length > 0) {
	    double bu = obst.p[0][0].x, bl = obst.p[0][0].y, bb = obst.p[0][0].x, br = obst.p[0][0].y;
	    int i = 0;
	    for(Coord2d v : obst.verts()) {
		poles[i++] = Location.xlate(gnd((float)v.x, (float)v.y));
		bu = Math.min(v.y, bu); bl = Math.min(v.x, bl);
		bb = Math.max(v.y, bb); br = Math.max(v.x, br);
	    }
	    if(((br - bl) > 22) || ((bb - bu) > 22))
		bound = mkbound(obst);
	    else
		bound = null;
	} else {
	    bound = null;
	}
    }

    void trace(MeshBuf buf, float x1, float y1, float x2, float y2) {
	float dx = x2 - x1, dy = y2 - y1, ed = (float)Math.sqrt(dx * dx + dy * dy);
	float lx = x1, ly = y1;
	Coord3f nrm = new Coord3f(dy / ed, dx / ed, 0);
	MeshBuf.Tex tex = buf.layer(MeshBuf.tex);
	MeshBuf.Vertex ll = buf.new Vertex(gnd(lx, ly), nrm);
	MeshBuf.Vertex lh = buf.new Vertex(gnd(lx, ly).add(0, 0, 3), nrm);
	tex.set(ll, new Coord3f(0, 1, 0));
	tex.set(lh, new Coord3f(0, 0, 0));
	int lim = 0;
	while(true) {
	    boolean end = true;
	    float ma = 1.0f, a;
	    float nx = x2, ny = y2;
	    if(dx != 0) {
		float ex;
		if(dx > 0) {
		    a = ((ex = (float)((Math.floor(lx / tilesz.x) + 1) * tilesz.x)) - x1) / dx;
		} else {
		    a = ((ex = (float)((Math.ceil (lx / tilesz.x) - 1) * tilesz.x)) - x1) / dx;
		}
		if(a < ma) {
		    nx = ex; ny = y1 + dy * a;
		    ma = a;
		    end = false;
		}
	    }
	    if(dy != 0) {
		float ey;
		if(dy > 0)
		    a = ((ey = (float)((Math.floor(ly / tilesz.y) + 1) * tilesz.y)) - y1) / dy;
		else
		    a = ((ey = (float)((Math.ceil (ly / tilesz.y) - 1) * tilesz.y)) - y1) / dy;
		if(a < ma) {
		    nx = x1 + dx * a; ny = ey;
		    ma = a;
		    end = false;
		}
	    }
	    MeshBuf.Vertex nl = buf.new Vertex(gnd(nx, ny), nrm);
	    MeshBuf.Vertex nh = buf.new Vertex(gnd(nx, ny).add(0, 0, 3), nrm);
	    tex.set(nl, new Coord3f(ma * ed * bscale, 1, 0));
	    tex.set(nh, new Coord3f(ma * ed * bscale, 0, 0));
	    buf.new Face(lh, ll, nh); buf.new Face(ll, nl, nh);
	    ll = nl; lh = nh;
	    lx = nx; ly = ny;
	    if(end)
		return;
	    if(lim++ > 100)
		throw(new RuntimeException("stuck in trace"));
	}
    }

    RenderTree.Node mkbound(Obstacle obst) {
	MeshBuf buf = new MeshBuf();
	for(Coord2d[] f : obst.p) {
	    for(int v = 0, n = f.length; v < n; v++) {
		int w = (v + 1) % n;
		trace(buf, (float)f[v].x, (float)f[v].y, (float)f[w].x, (float)f[w].y);
	    }
	}
	FastMesh mesh = buf.mkmesh();
	return(bmat.apply(mesh));
    }

    public void added(RenderTree.Slot slot) {
	slot.add(sign);
	if(bound != null) {
	    slot.add(bound);
	    for(Location loc : poles)
		slot.add(pole, loc);
	}
    }

    public void update(Message sdt) {
	for(int i = 0; i < 4; i++)
	    sdt.int8();
	done = sdt.uint8() / 255.0f;
    }
}
