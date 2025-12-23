package nurgling.db.dao;

import nurgling.db.DatabaseAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for StorageItem entities
 */
public class StorageItemDao {

    /**
     * Storage item data class
     */
    public static class StorageItemData {
        private final String itemHash;
        private final String name;
        private final double quality;
        private final String coordinates;
        private final String container;

        public StorageItemData(String itemHash, String name, double quality, String coordinates, String container) {
            this.itemHash = itemHash;
            this.name = name;
            this.quality = quality;
            this.coordinates = coordinates;
            this.container = container;
        }

        public String getItemHash() { return itemHash; }
        public String getName() { return name; }
        public double getQuality() { return quality; }
        public String getCoordinates() { return coordinates; }
        public String getContainer() { return container; }
    }

    /**
     * Save or update storage item
     */
    public void saveStorageItem(DatabaseAdapter adapter, String itemHash, String name, Double quality,
                               String coordinates, String container) throws SQLException {
        // Use LinkedHashMap to preserve column order
        java.util.LinkedHashMap<String, Object> columns = new java.util.LinkedHashMap<>();
        columns.put("item_hash", itemHash);
        columns.put("name", name);
        columns.put("quality", quality);
        columns.put("coordinates", coordinates);
        columns.put("container", container);
        
        String sql = adapter.getUpsertSql("storageitems", columns, java.util.List.of("item_hash"));
        adapter.executeUpdate(sql, itemHash, name, quality, coordinates, container);
    }

    /**
     * Load all storage items
     */
    public List<StorageItemData> loadAllStorageItems(DatabaseAdapter adapter) throws SQLException {
        List<StorageItemData> items = new ArrayList<>();

        try (ResultSet rs = adapter.executeQuery("SELECT item_hash, name, quality, coordinates, container FROM storageitems")) {
            while (rs.next()) {
                items.add(new StorageItemData(
                    rs.getString("item_hash"),
                    rs.getString("name"),
                    rs.getDouble("quality"),
                    rs.getString("coordinates"),
                    rs.getString("container")
                ));
            }
        }

        return items;
    }

    /**
     * Load storage items by container
     */
    public List<StorageItemData> loadStorageItemsByContainer(DatabaseAdapter adapter, String containerHash) throws SQLException {
        List<StorageItemData> items = new ArrayList<>();

        try (ResultSet rs = adapter.executeQuery("SELECT item_hash, name, quality, coordinates, container " +
                                                "FROM storageitems WHERE container = ?", containerHash)) {
            while (rs.next()) {
                items.add(new StorageItemData(
                    rs.getString("item_hash"),
                    rs.getString("name"),
                    rs.getDouble("quality"),
                    rs.getString("coordinates"),
                    rs.getString("container")
                ));
            }
        }

        return items;
    }

    /**
     * Load storage item by hash
     */
    public StorageItemData loadStorageItem(DatabaseAdapter adapter, String itemHash) throws SQLException {
        try (ResultSet rs = adapter.executeQuery("SELECT item_hash, name, quality, coordinates, container " +
                                                "FROM storageitems WHERE item_hash = ?", itemHash)) {
            if (rs.next()) {
                return new StorageItemData(
                    rs.getString("item_hash"),
                    rs.getString("name"),
                    rs.getDouble("quality"),
                    rs.getString("coordinates"),
                    rs.getString("container")
                );
            }
        }
        return null;
    }

    /**
     * Delete storage item
     */
    public void deleteStorageItem(DatabaseAdapter adapter, String itemHash) throws SQLException {
        adapter.executeUpdate("DELETE FROM storageitems WHERE item_hash = ?", itemHash);
    }

    /**
     * Delete all storage items for a container
     */
    public void deleteStorageItemsByContainer(DatabaseAdapter adapter, String containerHash) throws SQLException {
        adapter.executeUpdate("DELETE FROM storageitems WHERE container = ?", containerHash);
    }

    /**
     * Check if storage item exists
     */
    public boolean storageItemExists(DatabaseAdapter adapter, String itemHash) throws SQLException {
        try (ResultSet rs = adapter.executeQuery("SELECT 1 FROM storageitems WHERE item_hash = ? LIMIT 1", itemHash)) {
            return rs.next();
        }
    }
}
