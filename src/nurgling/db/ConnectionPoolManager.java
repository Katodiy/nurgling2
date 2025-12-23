package nurgling.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool manager that provides thread-safe connection pooling.
 * Each database task should borrow a connection, use it, and return it.
 */
public class ConnectionPoolManager {
    private SimpleConnectionPool connectionPool;
    private volatile boolean initialized = false;

    // Pool sizes: PostgreSQL can handle multiple concurrent connections,
    // SQLite should use 1 (doesn't support concurrent writes)
    private static final int POSTGRES_POOL_SIZE = 10;
    private static final int SQLITE_POOL_SIZE = 1;

    public ConnectionPoolManager() {
        initializePool();
    }

    /**
     * Initialize the connection pool based on database type.
     */
    private synchronized void initializePool() {
        if (initialized) {
            return;
        }

        boolean isPostgres = (Boolean) nurgling.NConfig.get(nurgling.NConfig.Key.postgres);
        int poolSize = isPostgres ? POSTGRES_POOL_SIZE : SQLITE_POOL_SIZE;

        connectionPool = new SimpleConnectionPool(poolSize);
        initialized = true;
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
     * Close a broken connection and update pool counter.
     * Use this when a connection has experienced an I/O error.
     *
     * @param conn The broken connection to close
     */
    public void closeBrokenConnection(Connection conn) {
        if (connectionPool != null && conn != null) {
            connectionPool.closeBrokenConnection(conn);
        }
    }

    /**
     * Check if the database is ready to accept connections.
     *
     * @return true if database is initialized and connections are available
     */
    public boolean isReady() {
        return initialized && connectionPool != null && connectionPool.isReady();
    }

    /**
     * Shutdown the pool manager and release all resources.
     */
    public synchronized void shutdown() {
        if (connectionPool != null) {
            connectionPool.shutdown();
        }
        initialized = false;
    }
}
