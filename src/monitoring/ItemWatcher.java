package monitoring;

import haven.Coord;
import haven.Utils;
import nurgling.NUtils;
import nurgling.db.DatabaseManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ItemWatcher implements Runnable {

    // Cache of container hashes with their item signature (hash of all item hashes)
    // Key = containerHash, Value = combined hash of all items in that container
    private static final ConcurrentHashMap<String, String> containerItemCache = new ConcurrentHashMap<>();
    private static final int MAX_CONTAINER_CACHE_SIZE = 1000;
    
    /**
     * Get current container cache size for debug display
     */
    public static int getContainerCacheSize() {
        return containerItemCache.size();
    }
    
    /**
     * Clear cached signature for a container to force re-sync on next close
     */
    public static void invalidateContainerCache(String containerHash) {
        if (containerHash != null) {
            containerItemCache.remove(containerHash);
        }
    }

    public static class ItemInfo {
        public String name;
        public double q = -1;
        public Coord c;           // Coordinates in inventory (or stack parent coords if in stack)
        public String container;
        public int stackIndex;    // Index in stack (-1 if not in stack)

        public ItemInfo(String name, double q, Coord c, String container, int stackIndex) {
            this.name = name;
            this.q = Double.parseDouble(Utils.odformat2(q, 2));
            this.c = c;
            this.container = container;
            this.stackIndex = stackIndex;
        }
        
        // Backward compatible constructor
        public ItemInfo(String name, double q, Coord c, String container) {
            this(name, q, c, container, -1);
        }
    }

    private final DatabaseManager databaseManager;
    private final ArrayList<ItemInfo> iis;
    private final String containerHash; // Store container hash separately for empty cache case

    public ItemWatcher(ArrayList<ItemInfo> iis, DatabaseManager databaseManager, String containerHash) {
        this.iis = iis;
        this.databaseManager = databaseManager;
        this.containerHash = containerHash;
    }
    
    // Backward compatible constructor (deprecated - use new one with containerHash)
    public ItemWatcher(ArrayList<ItemInfo> iis, DatabaseManager databaseManager) {
        this.iis = iis;
        this.databaseManager = databaseManager;
        this.containerHash = (iis != null && !iis.isEmpty()) ? iis.get(0).container : null;
    }

    @Override
    public void run() {
        if (containerHash == null) {
            return;
        }
        
        // Filter out items with negative or zero quality (stacks and unqualified items)
        if (iis != null) {
            iis.removeIf(item -> item.q <= 0);
        }
        
        boolean isEmpty = (iis == null || iis.isEmpty());
        
        // Build a signature of all items in this container (empty string if no items)
        String itemsSignature = isEmpty ? "" : buildItemsSignature();
        
        // Check if this container already has the same items (skip duplicate write)
        String cachedSignature = containerItemCache.get(containerHash);
        if (itemsSignature.equals(cachedSignature)) {
            nurgling.db.DatabaseManager.incrementSkippedContainer();
            return; // Same items, no need to write to DB
        }

        try {
            databaseManager.executeOperation(adapter -> {
                if (isEmpty) {
                    // Cache is empty - delete ALL items for this container from DB
                    deleteAllContainerItems(adapter);
                } else {
                    // Delete items that are NOT in the cache
                    deleteItems(adapter);
                    // Insert/update items from cache
                    insertItems(adapter);
                }
                return null;
            });
            
            // Update cache after successful write
            if (containerItemCache.size() >= MAX_CONTAINER_CACHE_SIZE) {
                // Simple eviction - remove random entries
                int toRemove = MAX_CONTAINER_CACHE_SIZE / 4;
                java.util.Iterator<String> it = containerItemCache.keySet().iterator();
                while (it.hasNext() && toRemove > 0) {
                    it.next();
                    it.remove();
                    toRemove--;
                }
            }
            containerItemCache.put(containerHash, itemsSignature);
            
            // Clear search query cache so next search will query fresh data
            NGlobalSearchItems.clearQueryCache();
            
            // Notify that container data has changed - increment version for debounced refresh
            nurgling.tools.NSearchItem.notifyContainerDataChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Build a hash signature representing all items in this container
     */
    private String buildItemsSignature() {
        StringBuilder sb = new StringBuilder();
        // Sort by hash to ensure consistent signature regardless of item order
        iis.stream()
            .map(this::generateItemHash)
            .sorted()
            .forEach(sb::append);
        return NUtils.calculateSHA256(sb.toString());
    }

    /**
     * Delete ALL items for this container (used when cache is empty)
     */
    private void deleteAllContainerItems(nurgling.db.DatabaseAdapter adapter) throws SQLException {
        String deleteSql = "DELETE FROM storageitems WHERE container = ?";
        adapter.executeUpdate(deleteSql, containerHash);
        System.out.println("ItemWatcher: Deleted ALL items for container " + containerHash);
    }
    
    private void deleteItems(nurgling.db.DatabaseAdapter adapter) throws SQLException {
        if (iis == null || iis.isEmpty()) return;
        
        // First, count how many items will be deleted for logging
        String countSql = "SELECT COUNT(*) FROM storageitems WHERE container = ?";
        int beforeCount = 0;
        try (java.sql.ResultSet rs = adapter.executeQuery(countSql, containerHash)) {
            if (rs.next()) {
                beforeCount = rs.getInt(1);
            }
        }
        
        // Build parameterized IN clause: DELETE ... WHERE ... NOT IN (?, ?, ?, ...)
        String placeholders = iis.stream().map(i -> "?").collect(java.util.stream.Collectors.joining(","));
        String deleteSql = "DELETE FROM storageitems WHERE container = ? AND item_hash NOT IN (" + placeholders + ")";

        Object[] params = new Object[iis.size() + 1];
        params[0] = containerHash;

        // Set each item hash as a separate parameter
        for (int i = 0; i < iis.size(); i++) {
            params[i + 1] = generateItemHash(iis.get(i));
        }

        int deleted = adapter.executeUpdate(deleteSql, params);
        
        if (deleted > 0 || beforeCount != iis.size()) {
            System.out.println("ItemWatcher.deleteItems: container=" + containerHash + 
                " beforeDB=" + beforeCount + " cache=" + iis.size() + " deleted=" + deleted);
        }
    }

    private void insertItems(nurgling.db.DatabaseAdapter adapter) throws SQLException {
        if (iis.isEmpty()) return;
        
        // Use batch upsert for efficient bulk insert
        java.util.List<String> columns = java.util.List.of("item_hash", "name", "quality", "coordinates", "container");
        java.util.List<String> conflictColumns = java.util.List.of("item_hash");
        java.util.List<String> updateColumns = java.util.List.of("name", "quality", "coordinates", "container");
        
        String batchSql = adapter.getBatchUpsertSql("storageitems", columns, conflictColumns, updateColumns);
        
        // Prepare batch parameters
        java.util.List<Object[]> paramList = new java.util.ArrayList<>(iis.size());
        for (ItemInfo item : iis) {
            String itemHash = generateItemHash(item);
            paramList.add(new Object[]{itemHash, item.name, item.q, item.c.toString(), item.container});
        }
        
        // Execute batch insert - much more efficient than individual inserts
        adapter.executeBatch(batchSql, paramList);
    }

    private String generateItemHash(ItemInfo item) {
        // Hash includes: name + coords + quality + stackIndex
        // stackIndex ensures items in same stack with same quality have different hashes
        String data = item.name + item.c.toString() + item.q + "_" + item.stackIndex;
        return NUtils.calculateSHA256(data);
    }
}
