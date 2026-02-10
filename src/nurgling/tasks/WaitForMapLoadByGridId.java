package nurgling.tasks;

import haven.MCache;
import nurgling.NGameUI;

/**
 * Waits for a specific grid (by gridId) to be fully loaded with mesh and fog ready.
 * Replacement for the deleted WaitForMapLoadPF that worked with routes.
 * Used by ChunkNav after portal traversal to ensure the target grid is ready.
 */
public class WaitForMapLoadByGridId extends NTask {
    private final NGameUI gui;
    private final long gridId;
    private long startTime;
    private static final long TIMEOUT_MS = 30000; // 30 second timeout

    public WaitForMapLoadByGridId(NGameUI gui, long gridId) {
        this.gui = gui;
        this.gridId = gridId;
    }

    @Override
    public boolean check() {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }

        // Timeout protection
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            return true;
        }

        // Invalid gridId - nothing to wait for
        if (gridId == 0 || gridId == -1) {
            return true;
        }

        for (MCache.Grid grid : gui.map.glob.map.grids.values()) {
            if (grid.id == gridId) {
                for (MCache.Grid.Cut cut : grid.cuts) {
                    if (!cut.mesh.isReady() || !cut.fo.isReady()) {
                        return false; // Not all cuts ready yet
                    }
                }
                return true; // All cuts ready
            }
        }

        // Grid not loaded yet
        return false;
    }
}
