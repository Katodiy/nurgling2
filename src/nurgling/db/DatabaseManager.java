package nurgling.db;

import nurgling.NConfig;
import nurgling.db.service.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Main database manager that provides unified access to all database operations.
 * Manages connection pool, database adapters, and service layer.
 */
public class DatabaseManager {
    private final ExecutorService executorService;
    private ConnectionPoolManager connectionPoolManager;
    private DatabaseAdapter adapter;
    private volatile boolean initialized = false;

    // Service layer
    private RecipeService recipeService;
    private FavoriteRecipeService favoriteRecipeService;
    private ContainerService containerService;
    private StorageItemService storageItemService;

    public DatabaseManager(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        initialize();
    }

    /**
     * Initialize database manager with connection pool and services
     */
    private synchronized void initialize() {
        if (initialized) {
            return;
        }

        if (!(Boolean) NConfig.get(NConfig.Key.ndbenable)) {
            return;
        }

        try {
            // Initialize connection pool manager
            this.connectionPoolManager = new ConnectionPoolManager();

            // Get a connection to create adapter
            Connection conn = connectionPoolManager.getConnection();
            if (conn != null) {
                this.adapter = DatabaseAdapterFactory.createAdapter(conn);

                // Initialize services
                initializeServices();

                // Run migrations
                runMigrations();

                connectionPoolManager.returnConnection(conn);
                initialized = true;

                System.out.println("DatabaseManager initialized successfully with " +
                                 DatabaseAdapterFactory.getDatabaseType());
            } else {
                System.err.println("Failed to initialize DatabaseManager: cannot get database connection");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize DatabaseManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize service layer
     */
    private void initializeServices() {
        this.recipeService = new RecipeService(this);
        this.favoriteRecipeService = new FavoriteRecipeService(this);
        this.containerService = new ContainerService(this);
        this.storageItemService = new StorageItemService(this);
    }

    /**
     * Run database migrations
     */
    private void runMigrations() {
        Connection conn = null;
        try {
            conn = connectionPoolManager.getConnection();
            if (conn != null) {
                nurgling.db.migration.MigrationManager migrationManager = new nurgling.db.migration.MigrationManager(conn, adapter);
                migrationManager.runMigrations();
                conn.commit();
            }
        } catch (SQLException e) {
            System.err.println("Failed to run database migrations: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }
            }
        } finally {
            if (conn != null) {
                connectionPoolManager.returnConnection(conn);
            }
        }
    }

    /**
     * Execute task asynchronously
     */
    public Future<?> submitTask(Runnable task) {
        return executorService.submit(task);
    }

    /**
     * Execute database operation with automatic connection management
     */
    public <T> T executeOperation(DatabaseOperation<T> operation) throws SQLException {
        Connection conn = null;
        try {
            conn = connectionPoolManager.getConnection();
            if (conn == null) {
                throw new SQLException("Unable to get database connection");
            }

            DatabaseAdapter operationAdapter = DatabaseAdapterFactory.createAdapter(conn);
            T result = operation.execute(operationAdapter);
            conn.commit();
            return result;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                connectionPoolManager.returnConnection(conn);
            }
        }
    }

    /**
     * Check if database is ready
     */
    public boolean isReady() {
        return initialized && connectionPoolManager != null && connectionPoolManager.isReady();
    }

    /**
     * Get recipe service
     */
    public RecipeService getRecipeService() {
        return recipeService;
    }

    /**
     * Get favorite recipe service
     */
    public FavoriteRecipeService getFavoriteRecipeService() {
        return favoriteRecipeService;
    }

    /**
     * Get container service
     */
    public ContainerService getContainerService() {
        return containerService;
    }

    /**
     * Get storage item service
     */
    public StorageItemService getStorageItemService() {
        return storageItemService;
    }

    /**
     * Reconnect to database
     */
    public synchronized void reconnect() {
        shutdown();
        initialized = false;
        initialize();
    }

    /**
     * Shutdown database manager and release all resources
     */
    public synchronized void shutdown() {
        executorService.shutdown();
        if (connectionPoolManager != null) {
            connectionPoolManager.shutdown();
            connectionPoolManager = null;
        }
        adapter = null;
        initialized = false;
    }

    /**
     * Functional interface for database operations
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(DatabaseAdapter adapter) throws SQLException;
    }
}
