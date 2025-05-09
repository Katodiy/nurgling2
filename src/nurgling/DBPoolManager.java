package nurgling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DBPoolManager {

    private final ExecutorService executorService;
    public final Connection connection;

    public DBPoolManager(int poolSize) throws SQLException {
        this.executorService = Executors.newFixedThreadPool(poolSize);
        this.connection = DriverManager.getConnection("jdbc:postgresql://" + NConfig.get(NConfig.Key.serverNode) +"/nurgling_db" +"?sql_mode=ANSI", (String) NConfig.get(NConfig.Key.serverUser), (String) NConfig.get(NConfig.Key.serverPass));
        connection.setAutoCommit(false);
    }

    public Future<?> submitTask(Runnable task) {
        return executorService.submit(() -> {
            try {
                task.run();
            } finally {
                // Освобождаем ресурсы, если необходимо
            }
        });
    }

    public void shutdown() {
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