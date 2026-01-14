package nurgling.db.service;

import nurgling.db.DatabaseManager;
import nurgling.db.dao.ContainerDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for container operations
 */
public class ContainerService {
    private final DatabaseManager databaseManager;
    private final ContainerDao containerDao;

    public ContainerService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.containerDao = new ContainerDao();
    }

    /**
     * Save container asynchronously
     */
    public CompletableFuture<Void> saveContainerAsync(String hash, long gridId, String coord) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveContainer(hash, gridId, coord);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save container", e);
            }
        });
    }

    /**
     * Save container synchronously
     */
    public void saveContainer(String hash, long gridId, String coord) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            containerDao.saveContainer(adapter, hash, gridId, coord);
            return null;
        });
    }

    /**
     * Load all containers asynchronously
     */
    public CompletableFuture<List<ContainerDao.ContainerData>> loadAllContainersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadAllContainers();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load containers", e);
            }
        });
    }

    /**
     * Load all containers synchronously
     */
    public List<ContainerDao.ContainerData> loadAllContainers() throws SQLException {
        return databaseManager.executeOperation(adapter -> containerDao.loadAllContainers(adapter));
    }

    /**
     * Load container by hash asynchronously
     */
    public CompletableFuture<ContainerDao.ContainerData> loadContainerAsync(String hash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadContainer(hash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load container", e);
            }
        });
    }

    /**
     * Load container by hash synchronously
     */
    public ContainerDao.ContainerData loadContainer(String hash) throws SQLException {
        return databaseManager.executeOperation(adapter -> containerDao.loadContainer(adapter, hash));
    }

    /**
     * Delete container asynchronously
     */
    public CompletableFuture<Void> deleteContainerAsync(String hash) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteContainer(hash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete container", e);
            }
        });
    }

    /**
     * Delete container synchronously
     */
    public void deleteContainer(String hash) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            containerDao.deleteContainer(adapter, hash);
            return null;
        });
    }

    /**
     * Check if container exists asynchronously
     */
    public CompletableFuture<Boolean> containerExistsAsync(String hash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return containerExists(hash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check container existence", e);
            }
        });
    }

    /**
     * Check if container exists synchronously
     */
    public boolean containerExists(String hash) throws SQLException {
        return databaseManager.executeOperation(adapter -> containerDao.containerExists(adapter, hash));
    }
}
