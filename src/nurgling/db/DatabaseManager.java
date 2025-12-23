package nurgling.db;

import nurgling.NConfig;
import nurgling.db.service.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main database manager that provides unified access to all database operations.
 * Manages connection pool, database adapters, and service layer.
 */
public class DatabaseManager {
    private ExecutorService executorService;
    private final int threadPoolSize;
    private ConnectionPoolManager connectionPoolManager;
    private DatabaseAdapter adapter;
    private volatile boolean initialized = false;
    private volatile boolean shutdown = false;

    // Service layer
    private RecipeService recipeService;
    private FavoriteRecipeService favoriteRecipeService;
    private ContainerService containerService;
    private StorageItemService storageItemService;
    private AreaService areaService;
    private RouteService routeService;
    
    // Task queue for retry logic
    private final BlockingQueue<QueuedTask<?>> taskQueue = new LinkedBlockingQueue<>(1000);
    private ScheduledExecutorService queueProcessor;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds between retries
    private final AtomicInteger queuedTaskCount = new AtomicInteger(0);
    
    /**
     * Wrapper for queued database tasks with retry support
     */
    private static class QueuedTask<T> {
        final DatabaseOperation<T> operation;
        final CompletableFuture<T> future;
        final String description;
        int retryCount = 0;
        long nextRetryTime = 0;
        
        QueuedTask(DatabaseOperation<T> operation, CompletableFuture<T> future, String description) {
            this.operation = operation;
            this.future = future;
            this.description = description;
        }
    }

    public DatabaseManager(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        startQueueProcessor();
        initialize();
    }
    
    /**
     * Start the background queue processor
     */
    private void startQueueProcessor() {
        queueProcessor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DB-Queue-Processor");
            t.setDaemon(true);
            return t;
        });
        
        queueProcessor.scheduleWithFixedDelay(() -> {
            if (shutdown || !initialized) return;
            
            try {
                processQueuedTasks();
            } catch (Exception e) {
                System.err.println("[DatabaseManager] Queue processor error: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Process queued tasks that are ready for retry
     */
    private void processQueuedTasks() {
        if (taskQueue.isEmpty()) return;
        
        long now = System.currentTimeMillis();
        int processed = 0;
        int maxToProcess = 10; // Process up to 10 tasks per cycle
        
        while (processed < maxToProcess && !taskQueue.isEmpty()) {
            QueuedTask<?> task = taskQueue.peek();
            if (task == null) break;
            
            // Check if it's time to retry
            if (task.nextRetryTime > now) {
                break; // Tasks are ordered by time, so no point checking others
            }
            
            // Remove from queue and process
            task = taskQueue.poll();
            if (task == null) break;
            
            processTask(task);
            processed++;
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> void processTask(QueuedTask<T> task) {
        try {
            T result = executeOperation(task.operation);
            task.future.complete(result);
            queuedTaskCount.decrementAndGet();
        } catch (SQLException e) {
            task.retryCount++;
            if (task.retryCount < MAX_RETRIES) {
                // Schedule for retry
                task.nextRetryTime = System.currentTimeMillis() + RETRY_DELAY_MS * task.retryCount;
                taskQueue.offer(task);
                System.out.println("[DatabaseManager] Task '" + task.description + "' failed, retry " + 
                    task.retryCount + "/" + MAX_RETRIES + " scheduled");
            } else {
                // Max retries exceeded
                task.future.completeExceptionally(e);
                queuedTaskCount.decrementAndGet();
                System.err.println("[DatabaseManager] Task '" + task.description + "' failed after " + 
                    MAX_RETRIES + " retries: " + e.getMessage());
            }
        }
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

            // Get a connection to create adapter and run migrations
            Connection conn = connectionPoolManager.getConnection();
            if (conn != null) {
                this.adapter = DatabaseAdapterFactory.createAdapter(conn);

                // Run migrations FIRST using this connection
                runMigrations(conn);

                // Initialize services after migrations
                initializeServices();

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
        this.areaService = new AreaService(this);
        this.routeService = new RouteService(this);
    }

    /**
     * Run database migrations using the provided connection
     */
    private void runMigrations(Connection conn) {
        System.out.println("DatabaseManager: Starting migration check...");
        try {
            // Create adapter for this specific connection
            DatabaseAdapter migrationAdapter = DatabaseAdapterFactory.createAdapter(conn);
            System.out.println("DatabaseManager: Running migrations...");
            nurgling.db.migration.MigrationManager migrationManager = new nurgling.db.migration.MigrationManager(conn, migrationAdapter);
            migrationManager.runMigrations();
            conn.commit();
            System.out.println("DatabaseManager: Migrations completed");
        } catch (SQLException e) {
            System.err.println("Failed to run database migrations: " + e.getMessage());
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException ignore) {
            }
        }
    }

    /**
     * Execute task asynchronously
     */
    public Future<?> submitTask(Runnable task) {
        if (shutdown || executorService == null || executorService.isShutdown()) {
            return null;
        }
        try {
            return executorService.submit(task);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Executor was shut down between check and submit
            return null;
        }
    }
    
    /**
     * Execute operation with automatic retry on failure.
     * If connection is not available, the task is queued for later execution.
     * 
     * @param operation The database operation to execute
     * @param description Description for logging
     * @return CompletableFuture that completes when operation succeeds or max retries exceeded
     */
    public <T> CompletableFuture<T> executeWithRetry(DatabaseOperation<T> operation, String description) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        if (shutdown || !initialized) {
            future.completeExceptionally(new SQLException("Database not available"));
            return future;
        }
        
        // Try to execute immediately
        executorService.submit(() -> {
            try {
                T result = executeOperation(operation);
                future.complete(result);
            } catch (SQLException e) {
                // Failed - queue for retry
                QueuedTask<T> task = new QueuedTask<>(operation, future, description);
                task.retryCount = 1;
                task.nextRetryTime = System.currentTimeMillis() + RETRY_DELAY_MS;
                
                if (taskQueue.offer(task)) {
                    queuedTaskCount.incrementAndGet();
                    System.out.println("[DatabaseManager] Task '" + description + "' queued for retry (queue size: " + 
                        queuedTaskCount.get() + ")");
                } else {
                    // Queue is full
                    future.completeExceptionally(new SQLException("Task queue full, operation rejected: " + description));
                    System.err.println("[DatabaseManager] Task queue full, rejected: " + description);
                }
            }
        });
        
        return future;
    }
    
    /**
     * Get current queued task count
     */
    public int getQueuedTaskCount() {
        return queuedTaskCount.get();
    }

    /**
     * Execute database operation with automatic connection management
     */
    public <T> T executeOperation(DatabaseOperation<T> operation) throws SQLException {
        Connection conn = null;
        boolean connectionBroken = false;
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
            // Check if this is an I/O error (connection is broken)
            if (isConnectionBroken(e)) {
                connectionBroken = true;
                System.err.println("[DatabaseManager] Connection broken due to I/O error, will not return to pool");
            }
            if (conn != null && !connectionBroken) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Rollback failed, connection is likely broken
                    connectionBroken = true;
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                if (connectionBroken) {
                    // Close broken connection and notify pool
                    connectionPoolManager.closeBrokenConnection(conn);
                } else {
                    connectionPoolManager.returnConnection(conn);
                }
            }
        }
    }
    
    /**
     * Check if the SQLException indicates a broken connection
     */
    private boolean isConnectionBroken(SQLException e) {
        // Check for I/O errors, timeout errors, connection closed errors
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("i/o error") ||
                lowerMessage.contains("connection closed") ||
                lowerMessage.contains("connection reset") ||
                lowerMessage.contains("socket") ||
                lowerMessage.contains("timeout")) {
                return true;
            }
        }
        
        // Check cause chain
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.net.SocketException ||
                cause instanceof java.net.SocketTimeoutException ||
                cause instanceof java.io.IOException) {
                return true;
            }
            cause = cause.getCause();
        }
        
        return false;
    }

    /**
     * Check if database is ready
     */
    public boolean isReady() {
        return initialized && !shutdown && connectionPoolManager != null && connectionPoolManager.isReady();
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
     * Get area service
     */
    public AreaService getAreaService() {
        return areaService;
    }

    /**
     * Get route service
     */
    public RouteService getRouteService() {
        return routeService;
    }

    /**
     * Reconnect to database
     */
    public synchronized void reconnect() {
        // Shutdown existing resources but don't mark as permanently shut down
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (connectionPoolManager != null) {
            connectionPoolManager.shutdown();
            connectionPoolManager = null;
        }
        adapter = null;
        initialized = false;
        
        // Create new executor and reinitialize
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.shutdown = false;
        initialize();
    }

    /**
     * Shutdown database manager and release all resources
     */
    public synchronized void shutdown() {
        shutdown = true;
        
        // Stop queue processor first
        if (queueProcessor != null) {
            queueProcessor.shutdown();
            try {
                queueProcessor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                queueProcessor.shutdownNow();
            }
        }
        
        // Cancel all queued tasks
        int cancelled = 0;
        QueuedTask<?> task;
        while ((task = taskQueue.poll()) != null) {
            task.future.completeExceptionally(new SQLException("Database shutdown"));
            cancelled++;
        }
        if (cancelled > 0) {
            System.out.println("[DatabaseManager] Cancelled " + cancelled + " queued tasks on shutdown");
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
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
