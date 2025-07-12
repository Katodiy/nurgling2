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

    public DBPoolManager(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
        this.isPostgres = (Boolean) NConfig.get(NConfig.Key.postgres);
        updateConnection();
    }

    private synchronized void updateConnection() {
        try {
            // Закрываем предыдущее соединение, если оно есть
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

            if ((Boolean) NConfig.get(NConfig.Key.ndbenable)) {
                if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                    // PostgreSQL соединение
                    currentUrl = "jdbc:postgresql://" + NConfig.get(NConfig.Key.serverNode) + "/nurgling_db?sql_mode=ANSI";
                    currentUser = (String) NConfig.get(NConfig.Key.serverUser);
                    currentPass = (String) NConfig.get(NConfig.Key.serverPass);
                    connection = DriverManager.getConnection(currentUrl, currentUser, currentPass);
                } else {
                    // SQLite соединение
                    currentUrl = "jdbc:sqlite:" + NConfig.get(NConfig.Key.dbFilePath);
                    connection = DriverManager.getConnection(currentUrl);
                }
                connection.setAutoCommit(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            connection = null;
        }
    }

    public synchronized void reconnect() {
        updateConnection();
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            updateConnection();
        }
        return connection;
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