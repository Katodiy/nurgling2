package nurgling.tasks;

import haven.Coord;
import haven.Coord2d;
import haven.MCache;
import nurgling.NGameUI;
import nurgling.NUtils;

/**
 * Headless-friendly version of WaitForMapLoadNoCoord.
 * Only waits for grid data from server, not mesh/fog rendering.
 */
public class WaitForMapGridLoad extends NTask {
    private final NGameUI gui;
    private int checkCount = 0;

    public WaitForMapGridLoad(NGameUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean check() {
        checkCount++;

        if (NUtils.player() == null) {
            if (checkCount % 20 == 1) {
                System.out.println("[WaitForMapGridLoad] check #" + checkCount + " - waiting for player");
            }
            return false;
        }

        if (NUtils.player().rc == null) {
            if (checkCount % 20 == 1) {
                System.out.println("[WaitForMapGridLoad] check #" + checkCount + " - waiting for player position");
            }
            return false;
        }

        Coord2d rc = NUtils.player().rc;
        Coord tc = rc.div(MCache.tilesz).floor();
        Coord gc = tc.div(NUtils.getGameUI().ui.sess.glob.map.cmaps);
        MCache map = NUtils.getGameUI().ui.sess.glob.map;

        // Try to get the grid - this will REQUEST it from the server if not present
        try {
            map.getgrid(gc);
            // If we get here, grid is loaded
            if (checkCount % 20 == 1) {
                System.out.println("[WaitForMapGridLoad] check #" + checkCount + " - grid loaded!");
            }
            return true;
        } catch (MCache.LoadingMap e) {
            // Grid not yet loaded, request was sent
            if (checkCount % 20 == 1) {
                System.out.println("[WaitForMapGridLoad] check #" + checkCount +
                    " - grid " + gc + " requested, waiting... (grids=" + map.grids.size() + ")");
            }
            return false;
        }
    }
}
