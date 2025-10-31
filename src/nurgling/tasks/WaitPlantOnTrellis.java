package nurgling.tasks;

import haven.Coord;
import haven.Gob;
import haven.OCache;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import static haven.MCache.tilesz;

/**
 * Waits for a plant to appear on a trellis tile.
 * Counts plants on the specified tile and waits until count reaches expectedCount.
 * Used after planting seeds on trellis to verify the plant appeared.
 */
public class WaitPlantOnTrellis extends NTask {
    private final Coord tile;
    private final NAlias plantAlias;
    private final int expectedCount;

    public WaitPlantOnTrellis(Coord tile, NAlias plantAlias, int expectedCount) {
        this.tile = tile;
        this.plantAlias = plantAlias;
        this.expectedCount = expectedCount;
    }

    @Override
    public boolean check() {
        int count = 0;
        synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                if (!(gob instanceof OCache.Virtual) && gob.ngob != null && gob.ngob.name != null) {
                    if (NParser.checkName(gob.ngob.name, plantAlias)) {
                        // Check if gob is on the same tile
                        if (gob.rc.floor(tilesz).equals(tile)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count >= expectedCount;
    }
}
