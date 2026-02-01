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
        // Count items before
        int beforeCount = countItemsInInventory(gui, itemName);
        
        // First try to find the container Gob in visible area
        Gob containerGob = Finder.findGob(containerHash);
        
        if (containerGob == null) {
            // Container not visible - try to find its position from database and navigate
            ContainerDao.ContainerData containerData = loadContainerData(gui, containerHash);
            if (containerData == null) {
                return 0;
            }
            
            // Parse coordinates and navigate
            Coord2d targetPos = parseCoordinates(containerData.getCoord());
            if (targetPos == null) {
                return 0;
            }
            
            // Navigate to container position using chunk navigation
            if (!navigateToPosition(gui, targetPos)) {
                return 0;
            }
            
            // Try to find container again after navigation
            containerGob = Finder.findGob(containerHash);
            if (containerGob == null) {
                // Container still not found - it might have been destroyed
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
     * Parse coordinates string "(x, y)" to Coord2d
     */
    private Coord2d parseCoordinates(String coords) {
        if (coords == null) return null;
        try {
            // Format: "(x, y)" or "(x,y)"
            String clean = coords.replace("(", "").replace(")", "").replace(" ", "");
            String[] parts = clean.split(",");
            if (parts.length == 2) {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                return new Coord2d(x, y);
            }
        } catch (Exception e) {
            // Parse error
        }
        return null;
    }
    
    /**
     * Navigate to a world position using chunk navigation
     */
    private boolean navigateToPosition(NGameUI gui, Coord2d targetPos) throws InterruptedException {
        ChunkNavManager chunkNav = ((NMapView) gui.map).getChunkNavManager();
        if (chunkNav == null || !chunkNav.isInitialized()) {
            // Fall back to local pathfinding
            return new PathFinder(targetPos).run(gui).IsSuccess();
        }
        
        // Use chunk navigation to get to target area
        Coord2d playerPos = gui.map.player().rc;
        double distance = playerPos.dist(targetPos);
        
        if (distance < 100) { // Close enough for local pathfinding
            return new PathFinder(targetPos).run(gui).IsSuccess();
        }
        
        // Far away - use global navigation
        // Create a temporary target for navigation
        nurgling.navigation.ChunkPath path = chunkNav.planToCoord(targetPos);
        if (path != null) {
            return chunkNav.navigateWithPath(path, null, gui).IsSuccess();
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

