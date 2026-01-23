/* Preprocessed source code */
package haven.res.ui.locptr;

import haven.*;
import haven.render.*;
import nurgling.NUtils;
import nurgling.widgets.NMapWnd;

import java.awt.Color;
import java.util.Optional;

import static haven.MCache.tilesz;
import static java.lang.Math.*;

/* >wdg: Pointer */
@haven.FromResource(name = "ui/locptr", version = 23)
public class Pointer extends Widget {
    public static final BaseColor col = new BaseColor(new Color(241, 227, 157, 255));
    public Indir<Resource> icon;
    public Coord2d tc, mc;
    public Coord lc;
    public long gobid = -1;
    public boolean click;
    private Tex licon;
	private String tip = null;

    public Pointer(Indir<Resource> icon) {
	super(Coord.z);
	this.icon = icon;
    }

    public static Widget mkwidget(UI ui, Object... args) {
	Indir<Resource> icon = ui.sess.getresv(args[0]);
	return(new Pointer(icon));
    }
	
    public void presize() {
	resize(parent.sz);
    }

    protected void added() {
	presize();
	super.added();
    }

    private int signum(int a) {
	if(a < 0) return(-1);
	if(a > 0) return(1);
	return(0);
    }

    private void drawarrow(GOut g, Coord tc) {
	Coord hsz = sz.div(2);
	tc = tc.sub(hsz);
	if(tc.equals(Coord.z))
	    tc = new Coord(1, 1);
	double d = Coord.z.dist(tc);
	Coord sc = tc.mul((d - 25.0) / d);
	float ak = ((float)hsz.y) / ((float)hsz.x);
	if((abs(sc.x) > hsz.x) || (abs(sc.y) > hsz.y)) {
	    if(abs(sc.x) * ak < abs(sc.y)) {
		sc = new Coord((sc.x * hsz.y) / sc.y, hsz.y).mul(signum(sc.y));
	    } else {
		sc = new Coord(hsz.x, (sc.y * hsz.x) / sc.x).mul(signum(sc.x));
	    }
	}
	Coord ad = sc.sub(tc).norm(UI.scale(30.0));
	sc = sc.add(hsz);

	// gl.glEnable(GL2.GL_POLYGON_SMOOTH); XXXRENDER
	g.usestate(col);
	g.drawp(Model.Mode.TRIANGLES, new float[] {
		sc.x, sc.y,
		sc.x + ad.x - (ad.y / 3), sc.y + ad.y + (ad.x / 3),
		sc.x + ad.x + (ad.y / 3), sc.y + ad.y - (ad.x / 3),
	    });

	if(icon != null) {
	    try {
		if(licon == null)
		    licon = icon.get().layer(Resource.imgc).tex();
		g.aimage(licon, sc.add(ad), 0.5, 0.5);
	    } catch(Loading l) {
	    }
	}
	this.lc = sc.add(ad);
    }

    public void draw(GOut g) {
	this.lc = null;
	if(tc == null)
	    return;
	Gob gob = (gobid < 0) ? null : ui.sess.glob.oc.getgob(gobid);
	Coord3f sl;
	if(gob != null) {
	    try {
		sl = getparent(GameUI.class).map.screenxf(gob.getc());
	    } catch(Loading l) {
		return;
	    }
	} else {
	    sl = getparent(GameUI.class).map.screenxf(tc);
	}
	if(sl != null)
	    drawarrow(g, new Coord(sl));
    }

    public void update(Coord2d tc, long gobid) {
	this.tc = tc;
	this.gobid = gobid;
    }

    public boolean mousedown(MouseDownEvent ev) {
	// Handle right-click for marker line (nurgling feature)
	if(ev.b == 3 && (lc != null) && lc.dist(ev.c) < 20) {
	    try {
		Coord2d targetCoords = tc();
		if(targetCoords != null) {
		    nurgling.tools.NPointerClickHandler.handleRightClick(targetCoords, tip, gobid);
		    return(true);
		}
	    } catch(Exception e) {
		// Silently ignore if nurgling handler not available
	    }
	}

	if(click && (lc != null)) {
	    if(lc.dist(ev.c) < 20) {
		wdgmsg("click", ev.b, ui.modflags());
		return(true);
	    }
	}
	return(super.mousedown(ev));
    }

    public void uimsg(String name, Object... args) {
	if(name == "upd") {
	    if(args[0] == null)
		tc = null;
	    else
		tc = ((Coord)args[0]).mul(OCache.posres);
		triangulate(tc);
	    if(args[1] == null)
		gobid = -1;
	    else
		gobid = Utils.uiv(args[1]);
	} else if(name == "icon") {
	    Indir<Resource> icon = ui.sess.getresv(args[0]);
	    this.icon = icon;
	    licon = null;
	} else if(name == "cl") {
	    click = ((Integer)args[0]) != 0;
	} else if(name == "tip") {
		Object tt = args[0];
		if(tt instanceof String) {
			tip = (String) tt;
		} else {
			super.uimsg(name, args);
		}
	} else {
	    super.uimsg(name, args);
	}
    }

    public Object tooltip(Coord c, Widget prev) {
	if((lc != null) && (lc.dist(c) < 20))
	    return(tooltip());
	return(null);
    }

	public Object tooltip() {
		if(tip != null) {
			double d = getDistance();
			if(d > 0) {
				return String.format("%s (%.1fm%s)", tip, d, triangulating ? "[?]" : "");
			} else {
				return tip;
			}
		} else return (tooltip);
	}

	double getDistance() {
		MapView map = getparent(GameUI.class).map;
		Gob target = getGob();
		Gob player = map == null ? null : map.player();
		if(player != null) {
			if(target != null) {
				return player.rc.dist(target.rc) / 11.0;
			} else {
				Coord2d tc = tc();
				if(tc != null) {
					return player.rc.dist(tc) / 11.0;
				}
			}
		}
		return -1;
	}

	private Gob getGob() {
		return (gobid < 0) ? null : ui.sess.glob.oc.getgob(gobid);
	}

	Pair<Coord2d, Coord2d> firstLine = null;
	long firsSegment = -1;
	private boolean triangulating = false;
	long lastseg = -1;
	Coord lastsegtc = null;
	public MapFile.Marker marker;

	private void triangulate(Coord2d b) {
		if(b == null) {
			firstLine = null;
			return;
		}
		mc = null;
		tc();
		if(!triangulating) {return;}
		long curseg = NUtils.getGameUI().mapfile.playerSegmentId();
		Gob player = NUtils.getGameUI().map.player();
		if(player != null) {
			Pair<Coord2d, Coord2d> line = new Pair<>(player.rc, b);
			if(firstLine == null) {
				firsSegment = curseg;
				firstLine = line;
			} else if(curseg == firsSegment) {
				mc = intersect(firstLine, line).orElse(mc);
				triangulating = mc == null;
			} else {
				firstLine = null;
			}
		}
	}

	public Coord2d tc() {return tc(NUtils.getGameUI().mapfile.playerSegmentId());}

	public Coord2d tc(long id) {
		if(marker != null) {
			triangulating = false;
			MiniMap.Location loc = NUtils.getGameUI().mapfile.view.sessloc;
			if(id == marker.seg) {
				Coord2d tmp = mc = marker.tc.sub(loc.tc).mul(tilesz).add(6, 6);
				tc = tmp;
				return mc;
			} else {
				return null;
			}
		} else if(tc == null) {
			triangulating = false;
			return null;
		} else if(mc == null) {
			GameUI gui = getparent(GameUI.class);
			Gob player = gui.map.player();
			if(player != null) {
				double d = player.rc.dist(tc) / 11.0;
				if(d > 990) {
					mc = gui.mapfile.findMarkerPosition(tip);
					triangulating = mc == null;
					if(mc != null) {
						return mc;
					}
				}
			}
			mc = tc;
			return mc;
		} else {
			return mc;
		}
	}

	public Coord sc(Coord c, Coord sz) {
		Pair<Coord, Coord> p = screenp(c, sz);
		return p.a.add(p.b);
	}

	private Pair<Coord, Coord> screenp(Coord tc, Coord sz) {
		Coord hsz = sz.div(2);
		tc = tc.sub(hsz);
		if(tc.equals(Coord.z))
			tc = new Coord(1, 1);
		double d = Coord.z.dist(tc);
		Coord sc = tc.mul((d - 25.0) / d);
		float ak = ((float) hsz.y) / ((float) hsz.x);
		if((abs(sc.x) > hsz.x) || (abs(sc.y) > hsz.y)) {
			if(abs(sc.x) * ak < abs(sc.y)) {
				sc = new Coord((sc.x * hsz.y) / sc.y, hsz.y).mul(signum(sc.y));
			} else {
				sc = new Coord(hsz.x, (sc.y * hsz.x) / sc.x).mul(signum(sc.x));
			}
		}
		Coord ad = sc.sub(tc).norm(UI.scale(30.0));
		sc = sc.add(hsz);

		return new Pair<>(sc, ad);
	}

	public static Optional<Coord2d> intersect(Pair<Coord2d, Coord2d> lineA, Pair<Coord2d, Coord2d> lineB) {
		double a1 = lineA.b.y - lineA.a.y;
		double b1 = lineA.a.x - lineA.b.x;
		double c1 = a1 * lineA.a.x + b1 * lineA.a.y;

		double a2 = lineB.b.y - lineB.a.y;
		double b2 = lineB.a.x - lineB.b.x;
		double c2 = a2 * lineB.a.x + b2 * lineB.a.y;

		double delta = a1 * b2 - a2 * b1;
		if(delta == 0) {
			return Optional.empty();
		}
		return Optional.of(new Coord2d((float) ((b2 * c1 - b1 * c2) / delta), (float) ((a1 * c2 - a2 * c1) / delta)));
	}
}
