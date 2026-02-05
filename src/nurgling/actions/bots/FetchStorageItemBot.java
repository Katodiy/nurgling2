package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
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
            int itemsInContainer = Math.min(containerItems.size(), remaining);
            
            // Try to find and navigate to container
            int fetched = fetchFromContainer(gui, containerHash, itemsInContainer);
            
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
     * Navigate to container and fetch items from it
     * @return number of items actually fetched
     */
    private int fetchFromContainer(NGameUI gui, String containerHash, int count) throws InterruptedException {
        System.out.println("[FetchStorageItemBot] fetchFromContainer called:");
        System.out.println("  - containerHash: " + containerHash);
        System.out.println("  - count: " + count);

        // Count items before
        int beforeCount = countItemsInInventory(gui, itemName);
        System.out.println("[FetchStorageItemBot] Items in inventory before: " + beforeCount);

        // First try to find the container Gob in visible area
        Gob containerGob = Finder.findGob(containerHash);
        System.out.println("[FetchStorageItemBot] Container visible? " + (containerGob != null));

        if (containerGob == null) {
            System.out.println("[FetchStorageItemBot] Container not visible - loading from database...");
            // Container not visible - try to find its position from database and navigate
            ContainerDao.ContainerData containerData = loadContainerData(gui, containerHash);
            if (containerData == null) {
                System.out.println("[FetchStorageItemBot] ERROR: Could not load container data from DB");
                return 0;
            }

            System.out.println("[FetchStorageItemBot] Container data from DB:");
            System.out.println("  - Hash: " + containerData.getHash());
            System.out.println("  - GridId: " + containerData.getGridId());
            System.out.println("  - Coord: " + containerData.getCoord());

            // Parse local coordinates and navigate using gridId
            Coord localCoord = parseLocalCoordinates(containerData.getCoord());
            if (localCoord == null) {
                System.out.println("[FetchStorageItemBot] ERROR: Could not parse local coordinates from: " + containerData.getCoord());
                return 0;
            }
            System.out.println("[FetchStorageItemBot] Parsed localCoord: " + localCoord);

            // Navigate to container position using chunk navigation with gridId
            System.out.println("[FetchStorageItemBot] Navigating to container...");
            if (!navigateToContainer(gui, containerData.getGridId(), localCoord)) {
                System.out.println("[FetchStorageItemBot] ERROR: Navigation failed!");
                return 0;
            }
            System.out.println("[FetchStorageItemBot] Navigation succeeded, looking for container gob again...");

            // Try to find container again after navigation
            containerGob = Finder.findGob(containerHash);
            if (containerGob == null) {
                System.out.println("[FetchStorageItemBot] ERROR: Container still not found after navigation!");
                // Container still not found - it might have been destroyed
                return 0;
            }
            System.out.println("[FetchStorageItemBot] Container found after navigation!");
        }
        
        // Move to container if not close enough
        if (!PathFinder.isAvailable(containerGob)) {
            PathFinder pf = new PathFinder(containerGob);
            pf.run(gui);
        }
        
        // Open container
        String containerName = getContainerWindowName(containerGob);
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
        
        // Check if item actually exists in container
        ArrayList<WItem> availableItems;
        try {
            availableItems = containerInv.getItems(itemName);
        } catch (InterruptedException e) {
            containerWindow.wdgmsg("close");
            return 0;
        }
        
        if (availableItems.isEmpty()) {
            // Item not found in container - close and return 0
            containerWindow.wdgmsg("close");
            return 0;
        }
        
        // Take items from container
        NAlias itemPattern = new NAlias(itemName);
        HashSet<String> names = new HashSet<>();
        names.add(itemName);
        
        // Create container wrapper for TakeItemsFromContainer
        Container container = new Container(containerGob, containerName, null);
        
        // Create quality filter based on min/max quality
        TakeItemsFromContainer takeAction = new TakeItemsFromContainer(
            container,
            names,
            itemPattern,
            maxQuality // Use maxQuality as the quality cap
        );
        takeAction.minSize = Math.min(count, availableItems.size());
        
        takeAction.run(gui);
        
        // Wait for transfer to complete
        Thread.sleep(200);
        
        // Close container window
        containerWindow.wdgmsg("close");
        
        // Count items after and return difference
        int afterCount = countItemsInInventory(gui, itemName);
        return afterCount - beforeCount;
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
     * Parse local coordinates string "(x, y)" to Coord (integer tile coordinates)
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
        System.out.println("[FetchStorageItemBot] navigateToContainer called:");
        System.out.println("  - Target gridId: " + gridId);
        System.out.println("  - Target localCoord: " + localCoord);

        ChunkNavManager chunkNav = ((NMapView) gui.map).getChunkNavManager();
        if (chunkNav == null) {
            System.out.println("[FetchStorageItemBot] ERROR: ChunkNavManager is null");
            return false;
        }
        if (!chunkNav.isInitialized()) {
            System.out.println("[FetchStorageItemBot] ERROR: ChunkNavManager is not initialized");
            return false;
        }
        System.out.println("[FetchStorageItemBot] ChunkNavManager: enabled=" + chunkNav.isEnabled() + ", initialized=" + chunkNav.isInitialized());

        // Check if the grid is currently loaded (container might be nearby)
        try {
            MCache mcache = gui.map.glob.map;
            MCache.Grid grid = null;
            synchronized (mcache.grids) {
                System.out.println("[FetchStorageItemBot] Searching for grid in MCache, loaded grids count: " + mcache.grids.size());
                for (MCache.Grid g : mcache.grids.values()) {
                    if (g.id == gridId) {
                        grid = g;
                        System.out.println("[FetchStorageItemBot] Found target grid in MCache! ul=" + g.ul);
                        break;
                    }
                }
            }
            if (grid != null) {
                // Grid is loaded - container should be visible, use local pathfinding
                Coord worldTile = grid.ul.add(localCoord);
                Coord2d worldPos = worldTile.mul(MCache.tilesz).add(MCache.tilehsz);
                System.out.println("[FetchStorageItemBot] Grid is loaded, using local PathFinder to " + worldPos);
                boolean result = new PathFinder(worldPos).run(gui).IsSuccess();
                System.out.println("[FetchStorageItemBot] Local PathFinder result: " + result);
                return result;
            } else {
                System.out.println("[FetchStorageItemBot] Target grid NOT in MCache - need chunk navigation");
            }
        } catch (Exception e) {
            System.out.println("[FetchStorageItemBot] Exception checking MCache: " + e.getMessage());
            e.printStackTrace();
            // Fall through to chunk navigation
        }

        // Grid not loaded - use chunk navigation
        System.out.println("[FetchStorageItemBot] Calling planToGridCoord...");
        nurgling.navigation.ChunkPath path = chunkNav.planToGridCoord(gridId, localCoord);
        if (path != null) {
            System.out.println("[FetchStorageItemBot] Path planned successfully!");
            System.out.println("  - Path waypoints: " + (path.waypoints != null ? path.waypoints.size() : 0));
            System.out.println("  - Path segments: " + (path.segments != null ? path.segments.size() : 0));
            System.out.println("  - Path requiresPortals: " + path.requiresPortals);
            System.out.println("  - Path confidence: " + path.confidence);
            System.out.println("[FetchStorageItemBot] Calling navigateWithPath...");
            boolean result = chunkNav.navigateWithPath(path, null, gui).IsSuccess();
            System.out.println("[FetchStorageItemBot] navigateWithPath result: " + result);

            // Log player position after navigation
            Gob playerAfter = NUtils.player();
            if (playerAfter != null) {
                System.out.println("[FetchStorageItemBot] Player position after navigation: " + playerAfter.rc);
            }

            // Check if target grid is now loaded
            try {
                MCache mcache = gui.map.glob.map;
                synchronized (mcache.grids) {
                    for (MCache.Grid g : mcache.grids.values()) {
                        if (g.id == gridId) {
                            System.out.println("[FetchStorageItemBot] Target grid NOW LOADED! ul=" + g.ul);
                            Coord expectedWorldTile = g.ul.add(localCoord);
                            Coord2d expectedPos = expectedWorldTile.mul(MCache.tilesz).add(MCache.tilehsz);
                            System.out.println("[FetchStorageItemBot] Expected container position: " + expectedPos);
                            if (playerAfter != null) {
                                System.out.println("[FetchStorageItemBot] Distance to expected position: " + playerAfter.rc.dist(expectedPos));
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[FetchStorageItemBot] Error checking grid after nav: " + e.getMessage());
            }

            return result;
        } else {
            System.out.println("[FetchStorageItemBot] ERROR: planToGridCoord returned null!");
            // Debug: check if target chunk exists in graph
            nurgling.navigation.ChunkNavGraph graph = chunkNav.getGraph();
            if (graph != null) {
                boolean hasChunk = graph.hasChunk(gridId);
                System.out.println("[FetchStorageItemBot] Graph hasChunk(" + gridId + "): " + hasChunk);
                System.out.println("[FetchStorageItemBot] Graph total chunks: " + graph.getAllChunks().size());

                // Get player's current chunk for comparison
                long playerChunkId = graph.getPlayerChunkId();
                System.out.println("[FetchStorageItemBot] Player current chunk: " + playerChunkId);
                if (playerChunkId != -1) {
                    System.out.println("[FetchStorageItemBot] Player chunk in graph: " + graph.hasChunk(playerChunkId));
                }
            }
        }

        return false;
    }
    
    /**
     * Get the window name for a container Gob
     */
    private String getContainerWindowName(Gob gob) {
        if (gob.ngob == null || gob.ngob.name == null) {
            return "Container";
        }
        
        String name = gob.ngob.name;
        
        // Extract container type from resource name
        if (name.contains("cupboard")) return "Cupboard";
        if (name.contains("chest")) {
            if (name.contains("largechest") || name.contains("large")) return "Large Chest";
            return "Chest";
        }
        if (name.contains("crate")) return "Crate";
        if (name.contains("barrel")) return "Barrel";
        if (name.contains("basket")) return "Basket";
        if (name.contains("coffer")) return "Coffer";
        if (name.contains("cabinet")) return "Metal Cabinet";
        if (name.contains("stonecasket")) return "Stonecasket";
        
        // Default fallback
        return "Container";
    }
}

