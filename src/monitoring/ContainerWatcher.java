package monitoring;

import haven.Gob;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.tasks.NTask;


import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ContainerWatcher  implements Runnable {
    Gob parentGob;
    public java.sql.Connection connection;
    final String sql = "INSERT INTO containers (hash, grid_id, coord) VALUES (?, ?, ?)";
    public ContainerWatcher(Gob parentGob) {
        this.parentGob = parentGob;
    }

    @Override
    public void run() {
        // Проверяем соединение перед использованием
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.err.println("ContainerWatcher: Connection is not valid, skipping write");
                return;
            }
        } catch (SQLException e) {
            System.err.println("ContainerWatcher: Failed to validate connection: " + e.getMessage());
            return;
        }

        try {
            // Wait for hash and gcoord with limited timeout (200 ticks default)
            NTask waitTask = new NTask() {
                @Override
                public boolean check() {
                    return parentGob.ngob.hash!=null && parentGob.ngob.gcoord!=null;
                }
            };
            NUtils.addTask(waitTask);
            
            // Check if task timed out (critical exit)
            if (waitTask.criticalExit) {
                System.err.println("ContainerWatcher: Timeout waiting for hash and gcoord for gob " + parentGob.id);
                safeRollback();
                return;
            }
            
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, parentGob.ngob.hash);
            preparedStatement.setLong(2, parentGob.ngob.grid_id);
            preparedStatement.setString(3, parentGob.ngob.gcoord.toString());

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            // Проверяем, является ли это ошибкой соединения
            if (isConnectionError(e)) {
                System.err.println("ContainerWatcher: Database connection lost, data not saved");
                return;
            }
            
            // Игнорируем ошибки нарушения уникальности
            String sqlState = e.getSQLState();
            if (sqlState == null || !sqlState.equals("23505")) {
                e.printStackTrace();
            }
            safeRollback();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void safeRollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException rollbackException) {
            // Игнорируем ошибки отката при закрытом соединении
            if (!isConnectionError(rollbackException)) {
                rollbackException.printStackTrace();
            }
        }
    }
    
    private boolean isConnectionError(SQLException e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("connection has been closed") || 
                           msg.contains("I/O error") ||
                           msg.contains("Connection refused") ||
                           msg.contains("Connection reset"))) {
            return true;
        }
        // PostgreSQL connection error states
        String sqlState = e.getSQLState();
        if (sqlState != null && (sqlState.startsWith("08") || // Connection exception
                                 sqlState.equals("57P01"))) { // Admin shutdown
            return true;
        }
        return false;
    }
}