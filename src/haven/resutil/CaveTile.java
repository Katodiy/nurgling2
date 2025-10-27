/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven.resutil;

import java.util.*;
import haven.*;
import haven.MapMesh.Scan;
import haven.Surface.Vertex;
import haven.render.BaseColor;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.tools.RockResourceMapper;

public class CaveTile extends Tiler {
    public static final float h = 16;
    public static final float SHORT_H = 4; // 25% of normal height for short walls
    public final Material wtex;
    public final Tiler ground;
    private final String resname;

    public static class Walls {
	public final MapMesh m;
	public final Scan cs;
	public final Vertex[][] wv;
	public final Vertex[][] hv; // Highlight vertices (offset above wall vertices)
	private MapMesh.MapSurface ms;
	public String resname; // Resource name to determine if short walls should apply

	public Walls(MapMesh m) {
	    this.m = m;
	    this.ms = m.data(MapMesh.gnd);
	    cs = new Scan(Coord.z, m.sz.add(1, 1));
	    wv = new Vertex[cs.l][];
	    hv = new Vertex[cs.l][]; // Initialize highlight vertex cache
	    this.resname = null;
	}

	public Vertex[] forhighlight(Coord tc) {
	    if(hv[cs.o(tc)] == null) {
		// Get the base wall vertices
		Vertex[] base = fortile(tc);
		// Create highlight vertices offset above the top vertex
		Vertex[] buf = hv[cs.o(tc)] = new Vertex[1];
		buf[0] = ms.new Vertex(base[3].x, base[3].y, base[3].z + 0.1f);
	    }
	    return(hv[cs.o(tc)]);
	}

	public Vertex[] fortile(Coord tc) {
	    if(wv[cs.o(tc)] == null) {
		Random rnd = m.grnd(tc.add(m.ul));
		Vertex[] buf = wv[cs.o(tc)] = new Vertex[4];

		// Use configurable wall height based on shortWalls setting
		float wallHeight = h;
		boolean shortWalls = false;
		try {
		    Boolean sw = (Boolean) NConfig.get(NConfig.Key.shortWalls);
		    if(sw != null && sw) {
			wallHeight = SHORT_H;
			shortWalls = true;
		    }
		} catch (Exception e) {
		    // If config check fails, use default height
		}

		buf[0] = ms.new Vertex(ms.fortile(tc));
		for(int i = 1; i < buf.length; i++) {
		    buf[i] = ms.new Vertex(buf[0].x, buf[0].y, buf[0].z + (i * wallHeight / (buf.length - 1)));

		    // Only add random offsets for full-height walls (natural cave look)
		    // Skip randomization for short walls to get clean, straight boxes
		    if(!shortWalls) {
			buf[i].x += (rnd.nextFloat() - 0.5f) * 3.0f;
			buf[i].y += (rnd.nextFloat() - 0.5f) * 3.0f;
			buf[i].z += (rnd.nextFloat() - 0.5f) * 3.5f;
		    }
		}
	    }
	    return(wv[cs.o(tc)]);
	}
    }
    public static final MapMesh.DataID<Walls> walls = MapMesh.makeid(Walls.class);

    @ResName("cave")
    public static class Factory implements Tiler.Factory {
	public Tiler create(int id, Tileset set) {
	    KeywordArgs desc = new KeywordArgs(set.ta, set.getres().pool);
	    Material wtex = set.getres().flayer(Material.Res.class, Utils.iv(desc.get("wmat"))).get();
	    Tiler ground = desc.oget("gnd").map(r -> Utils.irv(r).get().flayer(Tileset.class)).map(ts -> ts.tfac().create(id, ts)).orElse(null);
	    String resname = set.getres().name;
	    return(new CaveTile(id, set, wtex, ground, resname));
	}
    }

    public CaveTile(int id, Tileset set, Material wtex, Tiler ground, String resname) {
	super(id);
	this.wtex = wtex;
	this.ground = ground;
	this.resname = resname;
    }

    private static final Coord[] tces = {new Coord(0, -1), new Coord(1, 0), new Coord(0, 1), new Coord(-1, 0)};
    private static final Coord[] tccs = {new Coord(0, 0), new Coord(1, 0), new Coord(1, 1), new Coord(0, 1)};

    private void modelwall(Walls w, Coord ltc, Coord rtc) {
	Vertex[] lw = w.fortile(ltc), rw = w.fortile(rtc);
	for(int i = 0; i < lw.length - 1; i++) {
	    w.ms.new Face(lw[i + 1], lw[i], rw[i + 1]);
	    w.ms.new Face(lw[i], rw[i], rw[i + 1]);
	}
    }

    private void modelcap(Walls w, Coord lc) {
	// Get the 4 corner vertices for this tile (creating them if needed)
	Vertex[] c0 = w.fortile(lc.add(0, 0));
	Vertex[] c1 = w.fortile(lc.add(1, 0));
	Vertex[] c2 = w.fortile(lc.add(1, 1));
	Vertex[] c3 = w.fortile(lc.add(0, 1));

	// Create horizontal cap face using top vertices (index 3)
	w.ms.new Face(c0[3], c3[3], c2[3]);
	w.ms.new Face(c0[3], c2[3], c1[3]);
    }

    private boolean shouldHighlight() {
	try {
	    // Check if highlighting is enabled
	    Boolean enabled = (Boolean) NConfig.get(NConfig.Key.highlightRockTiles);
	    if (enabled == null || !enabled) {
		return false;
	    }

	    // Check if we have a game UI
	    if (NUtils.getGameUI() == null || NUtils.getGameUI().iconconf == null) {
		return false;
	    }

	    // Get selected icons from settings
	    Set<String> selectedGobResources = new HashSet<>();
	    synchronized (NUtils.getGameUI().iconconf.settings) {
		for (GobIcon.Setting setting : NUtils.getGameUI().iconconf.settings.values()) {
		    if (setting.show && setting.icon != null && setting.icon.res != null) {
			selectedGobResources.add(setting.icon.res.name);
		    }
		}
	    }

	    if (selectedGobResources.isEmpty()) {
		return false;
	    }

	    // Convert selected gob resources to tile resources and check if this tile matches
	    Set<String> selectedTileResources = RockResourceMapper.getTileResourcesToHighlight(selectedGobResources);
	    return selectedTileResources.contains(resname);

	} catch (Exception e) {
	    // Silently fail
	    return false;
	}
    }

    private void modelhighlight(Walls w, Coord lc) {
	// Create highlight vertices for each corner (offset above wall vertices)
	Vertex[] h0 = w.forhighlight(lc.add(0, 0));
	Vertex[] h1 = w.forhighlight(lc.add(1, 0));
	Vertex[] h2 = w.forhighlight(lc.add(1, 1));
	Vertex[] h3 = w.forhighlight(lc.add(0, 1));

	// Create highlight face geometry (will be textured during lay phase)
	w.ms.new Face(h0[0], h3[0], h2[0]);
	w.ms.new Face(h0[0], h2[0], h1[0]);
    }

    public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
	super.model(m, rnd, lc, gc);

	boolean shortWalls = false;
	try {
	    Boolean sw = (Boolean) NConfig.get(NConfig.Key.shortWalls);
	    shortWalls = (sw != null && sw);
	} catch (Exception e) {
	    // Use default
	}

	Walls w = null;
	for(int i = 0; i < 4; i++) {
	    int cid = m.map.gettile(gc.add(tces[i]));
	    if(cid <= id || (m.map.tiler(cid) instanceof CaveTile))
		continue;
	    if(w == null) {
		w = m.data(walls);
		w.resname = resname; // Set resource name for wall height calculation
	    }
	    modelwall(w, lc.add(tccs[(i + 1) % 4]), lc.add(tccs[i]));
	}

	// If short walls enabled, create cap geometry
	if(shortWalls) {
	    if(w == null) {
		w = m.data(walls);
		w.resname = resname;
	    }
	    modelcap(w, lc);
	}

	// If this tile should be highlighted, create highlight geometry
	if(shouldHighlight()) {
	    if(w == null) {
		w = m.data(walls);
		w.resname = resname;
	    }
	    modelhighlight(w, lc);
	}
    }

    private void mkwall(MapMesh m, Walls w, Coord ltc, Coord rtc) {
	Vertex[] lw = w.fortile(ltc), rw = w.fortile(rtc);
	MapMesh.Model mod = MapMesh.Model.get(m, wtex);

	// Render vertical wall between two tile corners
	MeshBuf.Vertex[] lv = new MeshBuf.Vertex[lw.length], rv = new MeshBuf.Vertex[rw.length];
	MeshBuf.Tex tex = mod.layer(mod.tex);
	for(int i = 0; i < lv.length; i++) {
	    float ty = (float)i / (float)(lv.length - 1);
	    lv[i] = new Surface.MeshVertex(mod, lw[i]);
	    tex.set(lv[i], new Coord3f(0, ty, 0));
	    rv[i] = new Surface.MeshVertex(mod, rw[i]);
	    tex.set(rv[i], new Coord3f(1, ty, 0));
	}
	for(int i = 0; i < lv.length - 1; i++) {
	    mod.new Face(lv[i + 1], lv[i], rv[i + 1]);
	    mod.new Face(lv[i], rv[i], rv[i + 1]);
	}
    }

    private void mkcap(MapMesh m, Walls w, Coord lc) {
	// Get the 4 corner vertices (already created during model() phase)
	Vertex[] c0 = w.fortile(lc.add(0, 0));
	Vertex[] c1 = w.fortile(lc.add(1, 0));
	Vertex[] c2 = w.fortile(lc.add(1, 1));
	Vertex[] c3 = w.fortile(lc.add(0, 1));

	// Apply texture to the cap using the top vertex (index 3) from each corner
	MapMesh.Model mod = MapMesh.Model.get(m, wtex);
	MeshBuf.Tex tex = mod.layer(mod.tex);

	MeshBuf.Vertex[] cv = new MeshBuf.Vertex[4];
	cv[0] = new Surface.MeshVertex(mod, c0[3]);
	tex.set(cv[0], new Coord3f(0, 0, 0));
	cv[1] = new Surface.MeshVertex(mod, c1[3]);
	tex.set(cv[1], new Coord3f(1, 0, 0));
	cv[2] = new Surface.MeshVertex(mod, c2[3]);
	tex.set(cv[2], new Coord3f(1, 1, 0));
	cv[3] = new Surface.MeshVertex(mod, c3[3]);
	tex.set(cv[3], new Coord3f(0, 1, 0));

	// Add textured triangles for the cap
	mod.new Face(cv[0], cv[3], cv[2]);
	mod.new Face(cv[0], cv[2], cv[1]);
    }

    private static java.awt.Color getPulsatingColor() {
	long PULSE_PERIOD = 2000; // 2 seconds for full pulse cycle
	long elapsed = System.currentTimeMillis();
	double phase = (elapsed % PULSE_PERIOD) / (double) PULSE_PERIOD;

	// Use sine wave for smooth pulsation (0 to 1 to 0)
	double t = (Math.sin(phase * Math.PI * 2) + 1) / 2;

	// Blue color (30, 144, 255)
	int blueR = 30, blueG = 144, blueB = 255;
	// Orange color (255, 200, 100)
	int orangeR = 255, orangeG = 200, orangeB = 100;

	// Interpolate between blue and orange
	int r = (int)(blueR + t * (orangeR - blueR));
	int g = (int)(blueG + t * (orangeG - blueG));
	int b = (int)(blueB + t * (orangeB - blueB));

	return new java.awt.Color(r, g, b, 80); // Semi-transparent
    }

    private void mkhighlight(MapMesh m, Walls w, Coord lc) {
	// Get the highlight vertices (already created during model() phase)
	Vertex[] h0 = w.forhighlight(lc.add(0, 0));
	Vertex[] h1 = w.forhighlight(lc.add(1, 0));
	Vertex[] h2 = w.forhighlight(lc.add(1, 1));
	Vertex[] h3 = w.forhighlight(lc.add(0, 1));

	// Create a simple colored material for the highlight
	java.awt.Color color = getPulsatingColor();
	Material highlightMat = new Material(new BaseColor(color));

	// Get model with the highlight material
	MapMesh.Model mod = MapMesh.Model.get(m, highlightMat);

	// Create mesh vertices from the cached highlight vertices
	MeshBuf.Vertex[] cv = new MeshBuf.Vertex[4];
	cv[0] = new Surface.MeshVertex(mod, h0[0]);
	cv[1] = new Surface.MeshVertex(mod, h1[0]);
	cv[2] = new Surface.MeshVertex(mod, h2[0]);
	cv[3] = new Surface.MeshVertex(mod, h3[0]);

	// Add colored triangles for the highlight
	mod.new Face(cv[0], cv[3], cv[2]);
	mod.new Face(cv[0], cv[2], cv[1]);
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
	// Check if short walls are enabled
	boolean shortWalls = false;
	try {
	    Boolean sw = (Boolean) NConfig.get(NConfig.Key.shortWalls);
	    shortWalls = (sw != null && sw);
	} catch (Exception e) {
	    // If config check fails, render normally
	}

	// Always render walls (height is determined by fortile() vertices)
	Walls w = null;
	for(int i = 0; i < 4; i++) {
	    int cid = m.map.gettile(gc.add(tces[i]));
	    if(cid <= id || (m.map.tiler(cid) instanceof CaveTile))
		continue;
	    if(w == null) {
		w = m.data(walls);
		w.resname = resname; // Set resource name for wall height calculation
	    }
	    mkwall(m, w, lc.add(tccs[(i + 1) % 4]), lc.add(tccs[i]));
	}

	// If short walls enabled, add horizontal cap on top
	if(shortWalls) {
	    if(w == null) {
		w = m.data(walls);
		w.resname = resname;
	    }
	    mkcap(m, w, lc);
	}

	// If this tile should be highlighted, add pulsating highlight
	if(shouldHighlight()) {
	    if(w == null) {
		w = m.data(walls);
		w.resname = resname;
	    }
	    mkhighlight(m, w, lc);
	}

	if(ground != null)
	    ground.lay(m, rnd, lc, gc);
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {}
}
