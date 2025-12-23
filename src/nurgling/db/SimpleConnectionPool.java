package nurgling.db;

import nurgling.NConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple thread-safe connection pool for database connections.
 * Supports both PostgreSQL and SQLite backends.
 */
public class SimpleConnectionPool {
    private final BlockingQueue<Connection> pool;
    private final int maxSize;
    private final AtomicInteger currentSize;
    private final AtomicBoolean isShutdown;

    private final boolean isPostgres;
    private final String jdbcUrl;
    private final String user;
    private final String password;

    private static final long BORROW_TIMEOUT_MS = 5000; // 5 seconds timeout for borrowing
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;
    private static final int MAX_TOTAL_CONNECTIONS = 20; // Safety limit to prevent connection leaks

    /**
     * Creates a new connection pool.
     *
     * @param maxSize Maximum number of connections in the pool
     */
    public SimpleConnectionPool(int maxSize) {
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingQueue<>(maxSize);
        this.currentSize = new AtomicInteger(0);
        this.isShutdown = new AtomicBoolean(false);

        this.isPostgres = (Boolean) NConfig.get(NConfig.Key.postgres);

        if (isPostgres) {
            // Increased timeouts: connectTimeout=10s, socketTimeout=60s for slow operations
            this.jdbcUrl = "jdbc:postgresql://" + NConfig.get(NConfig.Key.serverNode)
                         + "/nurgling_db?connectTimeout=10&socketTimeout=60";
            this.user = (String) NConfig.get(NConfig.Key.serverUser);
            this.password = (String) NConfig.get(NConfig.Key.serverPass);
        } else {
            this.jdbcUrl = "jdbc:sqlite:" + NConfig.get(NConfig.Key.dbFilePath);
            this.user = null;
            this.password = null;
        }
    }

    /**
     * Borrow a connection from the pool.
     * Creates a new connection if pool is empty and below max size.
     * Blocks up to BORROW_TIMEOUT_MS if pool is exhausted.
     *
     * @return A valid database connection, or null if unable to obtain one
     */
    public Connection borrowConnection() {
        if (isShutdown.get()) {
            return null;
        }

        Connection conn = pool.poll();

        if (conn != null) {
            // Validate existing connection
            if (isValid(conn)) {
                logPoolStatus("borrow (reused)");
                return conn;
            } else {
                // Connection is stale, close it and decrement counter
                closeQuietly(conn);
                currentSize.decrementAndGet();
            }
        }

        // Try to create a new connection if below max size
        if (currentSize.get() < maxSize) {
            conn = createConnection();
            if (conn != null) {
                currentSize.incrementAndGet();
                logPoolStatus("borrow (new)");
                return conn;
            }
        }

        // Pool is at max capacity, wait for a connection to be returned
        try {
            System.out.println("[ConnectionPool] Pool exhausted, waiting for connection (pool=" + pool.size() + ", total=" + currentSize.get() + "/" + maxSize + ")");
            conn = pool.poll(BORROW_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (conn != null && isValid(conn)) {
                logPoolStatus("borrow (waited)");
                return conn;
            } else if (conn != null) {
                closeQuietly(conn);
                currentSize.decrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.err.println("[ConnectionPool] Failed to borrow connection!");
        return null;
    }
    
    private void logPoolStatus(String action) {
        // Only log occasionally to avoid spam
        if (currentSize.get() > maxSize / 2) {
            System.out.println("[ConnectionPool] " + action + ": pool=" + pool.size() + ", total=" + currentSize.get() + "/" + maxSize);
        }
    }

    /**
     * Return a connection to the pool.
     * If the connection is invalid or pool is shut down, it will be closed instead.
     *
     * @param conn The connection to return
     */
    public void returnConnection(Connection conn) {
        if (conn == null) {
            return;
        }

        if (isShutdown.get()) {
            closeQuietly(conn);
            currentSize.decrementAndGet();
            return;
        }

        // Reset connection state
        try {
            if (!conn.getAutoCommit()) {
                conn.rollback(); // Rollback any uncommitted transaction
            }
        } catch (SQLException e) {
            // Connection is broken, close it
            closeQuietly(conn);
            currentSize.decrementAndGet();
            return;
        }

        if (!isValid(conn)) {
            closeQuietly(conn);
            currentSize.decrementAndGet();
            return;
        }

        // Try to return to pool
        if (!pool.offer(conn)) {
            // Pool is full (shouldn't happen with proper usage), close the connection
            closeQuietly(conn);
            currentSize.decrementAndGet();
        }
    }

    /**
     * Create a new database connection.
     *
     * @return A new connection with autoCommit disabled, or null on failure
     */
    private Connection createConnection() {
        try {
            Connection conn;
            if (isPostgres) {
                conn = DriverManager.getConnection(jdbcUrl, user, password);
            } else {
                conn = DriverManager.getConnection(jdbcUrl);
            }
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            System.err.println("Failed to create database connection: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a connection is still valid.
     *
     * @param conn The connection to validate
     * @return true if connection is valid
     */
    private boolean isValid(Connection conn) {
        try {
            if (conn == null || conn.isClosed()) {
                return false;
            }
            return conn.isValid(VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Close a connection without throwing exceptions.
     */
    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignore) {
            }
        }
    }

    /**
     * Check if the pool has available connections or can create new ones.
     *
     * @return true if connections are available
     */
    public boolean isReady() {
        if (isShutdown.get()) {
            return false;
        }
        return !pool.isEmpty() || currentSize.get() < maxSize;
    }

    /**
     * Get the current number of connections managed by this pool.
     */
    public int getCurrentSize() {
        return currentSize.get();
    }

    /**
     * Shutdown the pool and close all connections.
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            Connection conn;
            while ((conn = pool.poll()) != null) {
                closeQuietly(conn);
            }
            currentSize.set(0);
        }
    }
}
