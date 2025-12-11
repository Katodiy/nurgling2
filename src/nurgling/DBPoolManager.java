package nurgling;

import nurgling.db.SimpleConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Database pool manager that provides thread-safe connection pooling.
 * Each database task should borrow a connection, use it, and return it.
 */
public class DBPoolManager {
    private final ExecutorService executorService;
    private SimpleConnectionPool connectionPool;
    private volatile boolean initialized = false;

    // Pool sizes: PostgreSQL can handle multiple concurrent connections,
    // SQLite should use 1 (doesn't support concurrent writes)
    private static final int POSTGRES_POOL_SIZE = 5;
    private static final int SQLITE_POOL_SIZE = 1;

    public DBPoolManager(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        initializePool();
    }

    /**
     * Initialize the connection pool based on database type.
     */
    private synchronized void initializePool() {
        if (initialized) {
            return;
        }

        if (!(Boolean) NConfig.get(NConfig.Key.ndbenable)) {
            return;
        }

        boolean isPostgres = (Boolean) NConfig.get(NConfig.Key.postgres);
        int poolSize = isPostgres ? POSTGRES_POOL_SIZE : SQLITE_POOL_SIZE;

        connectionPool = new SimpleConnectionPool(poolSize);
        initialized = true;

        // Run migrations on first connection
        runMigrations();
    }

    /**
     * Run database migrations using a borrowed connection.
     */
    private void runMigrations() {
        Connection conn = null;
        try {
            conn = connectionPool.borrowConnection();
            if (conn != null) {
                DBMigrationManager migrationManager = new DBMigrationManager(conn);
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
                connectionPool.returnConnection(conn);
            }
        }
    }

    /**
     * Borrow a connection from the pool.
     * The caller MUST return the connection using returnConnection() when done.
     *
     * @return A database connection, or null if unavailable
     */
    public Connection getConnection() {
        if (!initialized || connectionPool == null) {
            return null;
        }
        return connectionPool.borrowConnection();
    }

    /**
     * Return a connection to the pool.
     * Always call this in a finally block after using a connection.
     *
     * @param conn The connection to return
     */
    public void returnConnection(Connection conn) {
        if (connectionPool != null && conn != null) {
            connectionPool.returnConnection(conn);
        }
    }

    /**
     * Check if the database is ready to accept connections.
     *
     * @return true if database is initialized and connections are available
     */
    public boolean isConnectionReady() {
        return initialized && connectionPool != null && connectionPool.isReady();
    }

    /**
     * Submit a task for execution.
     */
    public Future<?> submitTask(Runnable task) {
        return executorService.submit(task);
    }

    /**
     * Reconnect to the database (recreate the pool).
     */
    public synchronized void reconnect() {
        if (connectionPool != null) {
            connectionPool.shutdown();
        }
        initialized = false;
        connectionPool = null;
        initializePool();
    }

    /**
     * Shutdown the pool manager and release all resources.
     */
    public synchronized void shutdown() {
        executorService.shutdown();
        if (connectionPool != null) {
            connectionPool.shutdown();
            connectionPool = null;
        }
        initialized = false;
    }
}
