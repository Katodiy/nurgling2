package nurgling.db.service;

import nurgling.db.DatabaseManager;
import nurgling.db.dao.StorageItemDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for storage item operations
 */
public class StorageItemService {
    private final DatabaseManager databaseManager;
    private final StorageItemDao storageItemDao;

    public StorageItemService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.storageItemDao = new StorageItemDao();
    }

    /**
     * Save storage item asynchronously
     */
    public CompletableFuture<Void> saveStorageItemAsync(String itemHash, String name, Double quality,
                                                       String coordinates, String container) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveStorageItem(itemHash, name, quality, coordinates, container);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save storage item", e);
            }
        });
    }

    /**
     * Save storage item synchronously
     */
    public void saveStorageItem(String itemHash, String name, Double quality,
                               String coordinates, String container) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            storageItemDao.saveStorageItem(adapter, itemHash, name, quality, coordinates, container);
            return null;
        });
    }

    /**
     * Load all storage items asynchronously
     */
    public CompletableFuture<List<StorageItemDao.StorageItemData>> loadAllStorageItemsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadAllStorageItems();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load storage items", e);
            }
        });
    }

    /**
     * Load all storage items synchronously
     */
    public List<StorageItemDao.StorageItemData> loadAllStorageItems() throws SQLException {
        return databaseManager.executeOperation(adapter -> storageItemDao.loadAllStorageItems(adapter));
    }

    /**
     * Load storage items by container asynchronously
     */
    public CompletableFuture<List<StorageItemDao.StorageItemData>> loadStorageItemsByContainerAsync(String containerHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadStorageItemsByContainer(containerHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load storage items by container", e);
            }
        });
    }

    /**
     * Load storage items by container synchronously
     */
    public List<StorageItemDao.StorageItemData> loadStorageItemsByContainer(String containerHash) throws SQLException {
        return databaseManager.executeOperation(adapter -> storageItemDao.loadStorageItemsByContainer(adapter, containerHash));
    }

    /**
     * Load storage item by hash asynchronously
     */
    public CompletableFuture<StorageItemDao.StorageItemData> loadStorageItemAsync(String itemHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadStorageItem(itemHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load storage item", e);
            }
        });
    }

    /**
     * Load storage item by hash synchronously
     */
    public StorageItemDao.StorageItemData loadStorageItem(String itemHash) throws SQLException {
        return databaseManager.executeOperation(adapter -> storageItemDao.loadStorageItem(adapter, itemHash));
    }

    /**
     * Delete storage item asynchronously
     */
    public CompletableFuture<Void> deleteStorageItemAsync(String itemHash) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteStorageItem(itemHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete storage item", e);
            }
        });
    }

    /**
     * Delete storage item synchronously
     */
    public void deleteStorageItem(String itemHash) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            storageItemDao.deleteStorageItem(adapter, itemHash);
            return null;
        });
    }

    /**
     * Delete all storage items for container asynchronously
     */
    public CompletableFuture<Void> deleteStorageItemsByContainerAsync(String containerHash) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteStorageItemsByContainer(containerHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete storage items by container", e);
            }
        });
    }

    /**
     * Delete all storage items for container synchronously
     */
    public void deleteStorageItemsByContainer(String containerHash) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            storageItemDao.deleteStorageItemsByContainer(adapter, containerHash);
            return null;
        });
    }

    /**
     * Check if storage item exists asynchronously
     */
    public CompletableFuture<Boolean> storageItemExistsAsync(String itemHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return storageItemExists(itemHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check storage item existence", e);
            }
        });
    }

    /**
     * Check if storage item exists synchronously
     */
    public boolean storageItemExists(String itemHash) throws SQLException {
        return databaseManager.executeOperation(adapter -> storageItemDao.storageItemExists(adapter, itemHash));
    }
}
