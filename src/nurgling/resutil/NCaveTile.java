package nurgling.resutil;

import haven.*;
import haven.MapMesh.Scan;
import haven.Surface.Vertex;
import haven.resutil.CaveTile;
import nurgling.NConfig;

import java.util.Random;

/**
 * Custom cave tile implementation that allows configurable wall height.
 * When shortWalls config is enabled, walls are rendered at 25% of normal height (4 units instead of 16).
 * This class extends CaveTile without modifying haven code.
 */
public class NCaveTile extends CaveTile {
    // Short wall height (25% of normal, similar to shortCupboards scale)
    public static final float SHORT_HEIGHT = 4;


    public NCaveTile(int id, Tileset set, Material wtex, Tiler ground) {
        super(id, set, wtex, ground);
    }

    @Override
    public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
        // Check if short walls are enabled
        try {
            Boolean shortWalls = (Boolean) NConfig.get(NConfig.Key.shortWalls);
            if (shortWalls != null && shortWalls) {
                // Short walls enabled - skip all wall rendering
                return;
            }
        } catch (Exception e) {
            // If config check fails, fall through to normal rendering
        }

        // Normal walls - delegate to parent implementation
        super.model(m, rnd, lc, gc);
    }

    @Override
    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
        // Check if short walls are enabled
        try {
            Boolean shortWalls = (Boolean) NConfig.get(NConfig.Key.shortWalls);
            if (shortWalls != null && shortWalls) {
                // Short walls enabled - skip wall textures, but still render ground
                if(ground != null)
                    ground.lay(m, rnd, lc, gc);
                return;
            }
        } catch (Exception e) {
            // If config check fails, fall through to normal rendering
        }

        // Normal walls - delegate to parent implementation
        super.lay(m, rnd, lc, gc);
    }

    /**
     * Factory for creating NCaveTile instances.
     * This replaces the default CaveTile factory at runtime.
     */
    public static class Factory implements Tiler.Factory {
        @Override
        public Tiler create(int id, Tileset set) {
            KeywordArgs desc = new KeywordArgs(set.ta, set.getres().pool);
            Material wtex = set.getres().flayer(Material.Res.class, Utils.iv(desc.get("wmat"))).get();
            Tiler ground = desc.oget("gnd").map(r -> Utils.irv(r).get().flayer(Tileset.class)).map(ts -> ts.tfac().create(id, ts)).orElse(null);
            return new NCaveTile(id, set, wtex, ground);
        }
    }
}
