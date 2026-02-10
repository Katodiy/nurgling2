package nurgling.actions.bots;

import haven.*;
import static haven.OCache.posres;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.db.dao.StorageItemDao;
import nurgling.db.dao.ContainerDao;
import nurgling.i18n.L10n;
import nurgling.navigation.ChunkNavManager;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.NStorageItemsWidget.GroupedItem;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bot that fetches items from storage containers based on database information.
 * Navigates to containers and collects specified items.
 */
public class FetchStorageItemBot implements Action {

    private final String itemName;
    private final double minQuality;
    private final double maxQuality;
    private final int targetCount;
    private final List<StorageItemDao.StorageItemData> itemsToFetch;

    public static final AtomicBoolean stop = new AtomicBoolean(false);

    /**
     * Create a bot to fetch items matching the given criteria
     * @param item The grouped item to fetch
     * @param count Number of items to fetch
     * @param allItems All items from database matching this group
     */
    public FetchStorageItemBot(GroupedItem item, int count, List<StorageItemDao.StorageItemData> allItems) {
        this.itemName = item.name;
        this.minQuality = item.minQuality;
        this.maxQuality = item.maxQuality;
        this.targetCount = count;

        // Filter items that match the group criteria
        this.itemsToFetch = new ArrayList<>();
        for (StorageItemDao.StorageItemData data : allItems) {
            if (data.getName().equals(itemName)) {
                double q = data.getQuality();
                if (q >= minQuality && q <= maxQuality) {
                    itemsToFetch.add(data);
                }
            }
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        stop.set(false);

        if (itemsToFetch.isEmpty()) {
            gui.msg(L10n.get("storage.no_containers"), java.awt.Color.RED);
            return Results.FAIL();
        }

        // Group items by container
        Map<String, List<StorageItemDao.StorageItemData>> itemsByContainer = new LinkedHashMap<>();
        for (StorageItemDao.StorageItemData item : itemsToFetch) {
            itemsByContainer.computeIfAbsent(item.getContainer(), k -> new ArrayList<>()).add(item);
        }

        int collected = 0;
        int remaining = targetCount;

        gui.msg(L10n.get("storage.fetching_items").replace("{0}", String.valueOf(targetCount)));

        // Count items in player inventory before fetching
        int beforeCount = countItemsInInventory(gui, itemName);

        for (Map.Entry<String, List<StorageItemDao.StorageItemData>> entry : itemsByContainer.entrySet()) {
            if (stop.get()) {
                gui.msg(L10n.get("storage.fetch_cancelled"), java.awt.Color.YELLOW);
                return Results.FAIL();
            }

            if (remaining <= 0) break;

            String containerHash = entry.getKey();
            List<StorageItemDao.StorageItemData> containerItems = entry.getValue();
            // Pass actual DB records (limited by remaining) so we can match exact quality
            List<StorageItemDao.StorageItemData> toFetch = containerItems.subList(0, Math.min(containerItems.size(), remaining));

            // Try to find and navigate to container
            int fetched = fetchFromContainer(gui, containerHash, toFetch);

            if (fetched > 0) {
                collected += fetched;
                remaining -= fetched;
            }
        }

        // Verify actual items collected
        int afterCount = countItemsInInventory(gui, itemName);
        int actualCollected = afterCount - beforeCount;

        if (actualCollected > 0) {
            gui.msg(L10n.get("storage.fetch_complete").replace("{0}", String.valueOf(actualCollected)), java.awt.Color.GREEN);
            return Results.SUCCESS();
        } else {
            gui.msg(L10n.get("storage.container_not_found"), java.awt.Color.RED);
            return Results.FAIL();
        }
    }

    /**
     * Count items with matching name in player inventory
     */
    private int countItemsInInventory(NGameUI gui, String name) {
        try {
            return gui.getInventory().getItems(name).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Navigate to container and fetch items matching exact quality from DB records.
     * @param requestedItems DB records with exact qualities to match
     * @return number of items actually fetched
     */
    private int fetchFromContainer(NGameUI gui, String containerHash, List<StorageItemDao.StorageItemData> requestedItems) throws InterruptedException {
        // Count items before
        int beforeCount = countItemsInInventory(gui, itemName);

        // First try to find the container Gob in visible area
        Gob containerGob = Finder.findGob(containerHash);

        if (containerGob == null) {
            // Container not visible - try to find its position from database and navigate
            ContainerDao.ContainerData containerData = loadContainerData(gui, containerHash);
            if (containerData == null) {
                System.out.println("[FetchStorageItemBot] Could not load container data from DB for hash: " + containerHash);
                return 0;
            }

            // Parse local coordinates and navigate using gridId
            Coord localCoord = parseLocalCoordinates(containerData.getCoord());
            if (localCoord == null) {
                System.out.println("[FetchStorageItemBot] Could not parse coordinates: " + containerData.getCoord());
                return 0;
            }

            // Navigate to container position using chunk navigation with gridId
            System.out.println("[FetchStorageItemBot] Navigating to container at grid=" + containerData.getGridId() + " coord=" + localCoord);
            if (!navigateToContainer(gui, containerData.getGridId(), localCoord)) {
                System.out.println("[FetchStorageItemBot] Navigation failed to grid=" + containerData.getGridId());
                return 0;
            }

            // Try to find container again after navigation (gobs may take time to load)
            containerGob = Finder.findGob(containerHash);
            if (containerGob == null) {
                WaitForGobWithHash waitGob = new WaitForGobWithHash(containerHash);
                NUtils.addTask(waitGob);
                if (!waitGob.criticalExit) {
                    containerGob = Finder.findGob(containerHash);
                }
            }
            if (containerGob == null) {
                System.out.println("[FetchStorageItemBot] Container not found after navigation, hash=" + containerHash);
                return 0;
            }
        }

        // Move to container if not close enough
        if (!PathFinder.isAvailable(containerGob)) {
            PathFinder pf = new PathFinder(containerGob);
            pf.run(gui);
        }

        // Open container
        String containerName = getContainerWindowName(containerGob);
        if (containerName == null) {
            System.out.println("[FetchStorageItemBot] Unknown container type: " +
                (containerGob.ngob != null ? containerGob.ngob.name : "null"));
            return 0;
        }
        NUtils.rclickGob(containerGob);
        NUtils.addTask(new WaitWindow(containerName));

        // Wait for inventory to load
        Thread.sleep(500);

        // Get container inventory
        Window containerWindow = gui.getWindow(containerName);
        if (containerWindow == null) {
            return 0;
        }

        NInventory containerInv = gui.getInventory(containerName);
        if (containerInv == null) {
            containerWindow.wdgmsg("close");
            return 0;
        }

        // Fetch items one by one, matching exact quality from DB records
        int transferred = 0;
        for (StorageItemDao.StorageItemData dbItem : requestedItems) {
            if (stop.get()) break;
            if (gui.getInventory().calcFreeSpace() == 0) break;

            // Find a WItem in container matching name AND exact quality
            WItem match = findItemByQuality(containerInv, dbItem.getName(), dbItem.getQuality());
            if (match == null) {
                // No item with this exact quality in the container — skip
                continue;
            }

            int oldCount = countItemsInInventory(gui, itemName);
            TransferToContainer.transfer(match, gui.getInventory(), 1);

            // Verify item was actually transferred
            int newCount = countItemsInInventory(gui, itemName);
            if (newCount > oldCount) {
                transferred++;
            }
        }

        // Close container window
        containerWindow.wdgmsg("close");

        // Return actual count based on inventory difference
        int afterCount = countItemsInInventory(gui, itemName);
        return afterCount - beforeCount;
    }

    /**
     * Find a WItem in the container inventory matching the given name and exact quality.
     * Quality is compared rounded to 2 decimal places (same precision as DB storage).
     * @return matching WItem or null if not found
     */
    private WItem findItemByQuality(NInventory containerInv, String name, double targetQuality) throws InterruptedException {
        ArrayList<WItem> items = containerInv.getItems(name);
        for (WItem item : items) {
            if (item.item instanceof NGItem) {
                Float q = ((NGItem) item.item).quality;
                if (q != null) {
                    double rounded = Double.parseDouble(Utils.odformat2(q, 2));
                    if (Double.compare(rounded, targetQuality) == 0) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Load container data from database
     */
    private ContainerDao.ContainerData loadContainerData(NGameUI gui, String containerHash) {
        if (NCore.databaseManager == null || !NCore.databaseManager.isReady()) {
            return null;
        }

        try {
            return NCore.databaseManager.executeOperation(adapter -> {
                ContainerDao dao = new ContainerDao();
                List<ContainerDao.ContainerData> containers = dao.loadAllContainers(adapter);
                for (ContainerDao.ContainerData c : containers) {
                    if (c.getHash().equals(containerHash)) {
                        return c;
                    }
                }
                return null;
            });
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse local coordinates string "(x, y)" to Coord (in posres units, as stored by ContainerWatcher)
     */
    private Coord parseLocalCoordinates(String coords) {
        if (coords == null) return null;
        try {
            // Format: "(x, y)" or "(x,y)"
            String clean = coords.replace("(", "").replace(")", "").replace(" ", "");
            String[] parts = clean.split(",");
            if (parts.length == 2) {
                int x = (int) Double.parseDouble(parts[0]);
                int y = (int) Double.parseDouble(parts[1]);
                return new Coord(x, y);
            }
        } catch (Exception e) {
            // Parse error
        }
        return null;
    }

    /**
     * Navigate to a container using gridId + local coordinates via chunk navigation.
     * Uses planToGridCoord for cross-layer navigation.
     */
    private boolean navigateToContainer(NGameUI gui, long gridId, Coord localCoord) throws InterruptedException {
        ChunkNavManager chunkNav = ((NMapView) gui.map).getChunkNavManager();
        if (chunkNav == null || !chunkNav.isInitialized()) {
            System.out.println("[FetchStorageItemBot] ChunkNavManager not available");
            return false;
        }

        // Check if the grid is currently loaded (container might be nearby)
        try {
            MCache mcache = gui.map.glob.map;
            MCache.Grid grid = null;
            synchronized (mcache.grids) {
                for (MCache.Grid g : mcache.grids.values()) {
                    if (g.id == gridId) {
                        grid = g;
                        break;
                    }
                }
            }
            if (grid != null) {
                // Grid is loaded - container should be visible, use local pathfinding
                // localCoord is in posres units, convert to world position:
                // worldPos = gridOrigin(world) + localCoord * posres
                Coord2d gridOrigin = new Coord2d(grid.ul.x * MCache.tilesz.x, grid.ul.y * MCache.tilesz.y);
                Coord2d worldPos = gridOrigin.add(new Coord2d(localCoord.x * posres.x, localCoord.y * posres.y));
                return new PathFinder(worldPos).run(gui).IsSuccess();
            }
        } catch (Exception e) {
            // Fall through to chunk navigation
        }

        // Grid not loaded - use chunk navigation
        // localCoord is in posres units, convert to tile coordinates for chunk nav planner
        Coord2d offsetWorld = new Coord2d(localCoord.x * posres.x, localCoord.y * posres.y);
        Coord tileCoord = offsetWorld.floor(MCache.tilesz);
        nurgling.navigation.ChunkPath path = chunkNav.planToGridCoord(gridId, tileCoord);
        if (path != null) {
            return chunkNav.navigateWithPath(path, null, gui).IsSuccess();
        } else {
            System.out.println("[FetchStorageItemBot] Cannot plan path to grid=" + gridId + " tile=" + tileCoord);
        }

        return false;
    }

    /**
     * Get the window name for a container Gob using the canonical NContext.contcaps mapping.
     * Returns null if the container type is unknown.
     */
    private String getContainerWindowName(Gob gob) {
        if (gob.ngob == null || gob.ngob.name == null) {
            return null;
        }
        return NContext.contcaps.get(gob.ngob.name);
    }
}
