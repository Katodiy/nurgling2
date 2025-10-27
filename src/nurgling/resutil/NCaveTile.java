package nurgling.resutil;

import haven.*;
import haven.resutil.CaveTile;
import haven.MapMesh.Scan;
import haven.Surface.Vertex;
import nurgling.NConfig;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Custom cave tile implementation that allows configurable wall height.
 * When shortWalls config is enabled, walls are rendered at 25% of normal height (4 units instead of 16).
 *
 * This class replaces the default CaveTile factory using reflection to avoid modifying haven code.
 */
public class NCaveTile extends Tiler {
    public static final float h = 16;
    public static final float SHORT_H = 4; // 25% of normal height for short walls
    public final Material wtex;
    public final Tiler ground;
    private final String resname;

    static {
        // Replace the "cave" factory in Tiler's rnames map with our custom factory
        try {
            Field rnamesField = Tiler.class.getDeclaredField("rnames");
            rnamesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Tiler.Factory> rnames = (Map<String, Tiler.Factory>) rnamesField.get(null);
            rnames.put("cave", new NCaveTile.Factory());
            System.out.println("[NCaveTile] Successfully replaced 'cave' tiler factory");
        } catch (Exception e) {
            System.err.println("[NCaveTile] Failed to replace cave factory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class Walls {
        public final MapMesh m;
        public final Scan cs;
        public final Vertex[][] wv;
        private MapMesh.MapSurface ms;

        public Walls(MapMesh m) {
            this.m = m;
            this.ms = m.data(MapMesh.gnd);
            cs = new Scan(Coord.z, m.sz.add(1, 1));
            wv = new Vertex[cs.l][];
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

    /**
     * Factory for creating NCaveTile instances.
     * This replaces the default CaveTile factory at runtime via reflection.
     */
    public static class Factory implements Tiler.Factory {
        @Override
        public Tiler create(int id, Tileset set) {
            KeywordArgs desc = new KeywordArgs(set.ta, set.getres().pool);
            Material wtex = set.getres().flayer(Material.Res.class, Utils.iv(desc.get("wmat"))).get();
            Tiler ground = desc.oget("gnd").map(r -> Utils.irv(r).get().flayer(Tileset.class)).map(ts -> ts.tfac().create(id, ts)).orElse(null);
            String resname = set.getres().name;
            return new NCaveTile(id, set, wtex, ground, resname);
        }
    }

    public NCaveTile(int id, Tileset set, Material wtex, Tiler ground, String resname) {
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
            if(cid <= id || (m.map.tiler(cid) instanceof NCaveTile))
                continue;
            if(w == null) {
                w = m.data(walls);
            }
            modelwall(w, lc.add(tccs[(i + 1) % 4]), lc.add(tccs[i]));
        }

        // If short walls enabled, create cap geometry
        if(shortWalls) {
            if(w == null) {
                w = m.data(walls);
            }
            modelcap(w, lc);
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
            if(cid <= id || (m.map.tiler(cid) instanceof NCaveTile))
                continue;
            if(w == null) {
                w = m.data(walls);
            }
            mkwall(m, w, lc.add(tccs[(i + 1) % 4]), lc.add(tccs[i]));
        }

        // If short walls enabled, add horizontal cap on top
        if(shortWalls) {
            if(w == null) {
                w = m.data(walls);
            }
            mkcap(m, w, lc);
        }

        if(ground != null)
            ground.lay(m, rnd, lc, gc);
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {}
}
