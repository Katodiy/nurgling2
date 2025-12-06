package nurgling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DBPoolManager {
    private final ExecutorService executorService;
    private Connection connection = null;
    private boolean isPostgres;
    private String currentUrl;
    private String currentUser;
    private String currentPass;
    private volatile boolean isConnecting = false;
    private volatile long lastConnectionAttempt = 0;
    private static final long CONNECTION_RETRY_DELAY_MS = 30000; // 30 seconds between retries

    public DBPoolManager(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
        this.isPostgres = (Boolean) NConfig.get(NConfig.Key.postgres);
        // Initialize connection asynchronously to avoid blocking UI thread
        asyncUpdateConnection();
    }
    
    private void asyncUpdateConnection() {
        if (isConnecting) {
            return; // Already attempting to connect
        }
        long now = System.currentTimeMillis();
        if (now - lastConnectionAttempt < CONNECTION_RETRY_DELAY_MS) {
            return; // Too soon to retry
        }
        isConnecting = true;
        lastConnectionAttempt = now;
        executorService.submit(() -> {
            try {
                updateConnection();
            } finally {
                isConnecting = false;
            }
        });
    }

    private synchronized void updateConnection() {
        try {
            // Закрываем предыдущее соединение, если оно есть
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

            if ((Boolean) NConfig.get(NConfig.Key.ndbenable)) {
                if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                    // PostgreSQL соединение with connection timeout (5 seconds) and socket timeout (10 seconds)
                    currentUrl = "jdbc:postgresql://" + NConfig.get(NConfig.Key.serverNode) + "/nurgling_db?connectTimeout=5&socketTimeout=10";
                    currentUser = (String) NConfig.get(NConfig.Key.serverUser);
                    currentPass = (String) NConfig.get(NConfig.Key.serverPass);
                    connection = DriverManager.getConnection(currentUrl, currentUser, currentPass);
                } else {
                    // SQLite соединение
                    currentUrl = "jdbc:sqlite:" + NConfig.get(NConfig.Key.dbFilePath);
                    connection = DriverManager.getConnection(currentUrl);
                }
                connection.setAutoCommit(false);
                
                // Run migrations after establishing connection
                try {
                    DBMigrationManager migrationManager = new DBMigrationManager(connection);
                    migrationManager.runMigrations();
                } catch (SQLException migrationEx) {
                    System.err.println("Failed to run database migrations: " + migrationEx.getMessage());
                    migrationEx.printStackTrace();
                    // Don't close connection, migrations might have partially succeeded
                }
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            connection = null;
        }
    }

    public void reconnect() {
        asyncUpdateConnection();
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // Trigger async reconnection instead of blocking
            asyncUpdateConnection();
            return null; // Return null to indicate connection not ready
        }
        return connection;
    }
    
    public boolean isConnectionReady() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public Future<?> submitTask(Runnable task) {
        synchronized (executorService) {
            return executorService.submit(() -> {
                try {
                    task.run();
                } finally {
                    // Освобождаем ресурсы, если необходимо
                }
            });
        }
    }

    public synchronized void shutdown() {
        executorService.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}