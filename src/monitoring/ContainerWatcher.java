package monitoring;

import haven.Gob;
import nurgling.DBPoolManager;
import nurgling.NUtils;
import nurgling.tasks.NTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ContainerWatcher implements Runnable {
    private final Gob parentGob;
    private final DBPoolManager poolManager;
    private static final String SQL = "INSERT INTO containers (hash, grid_id, coord) VALUES (?, ?, ?)";

    public ContainerWatcher(Gob parentGob, DBPoolManager poolManager) {
        this.parentGob = parentGob;
        this.poolManager = poolManager;
    }

    @Override
    public void run() {
        Connection conn = null;
        try {
            // Wait for hash and gcoord with limited timeout (200 ticks default)
            NTask waitTask = new NTask() {
                @Override
                public boolean check() {
                    return parentGob.ngob.hash != null && parentGob.ngob.gcoord != null;
                }
            };
            NUtils.addTask(waitTask);

            // Check if task timed out (critical exit)
            if (waitTask.criticalExit) {
                System.err.println("ContainerWatcher: Timeout waiting for hash and gcoord for gob " + parentGob.id);
                return;
            }

            // Borrow connection from pool
            conn = poolManager.getConnection();
            if (conn == null) {
                System.err.println("ContainerWatcher: Unable to get database connection");
                return;
            }

            try (PreparedStatement preparedStatement = conn.prepareStatement(SQL)) {
                preparedStatement.setString(1, parentGob.ngob.hash);
                preparedStatement.setLong(2, parentGob.ngob.grid_id);
                preparedStatement.setString(3, parentGob.ngob.gcoord.toString());
                preparedStatement.executeUpdate();
                conn.commit();
            }
        } catch (SQLException e) {
            // SQLState 23505 = unique constraint violation (container already exists) - this is expected
            if (e.getSQLState() == null || !e.getSQLState().equals("23505")) {
                e.printStackTrace();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            // Bot was stopped, don't print stack trace
        } finally {
            // Always return connection to pool
            if (conn != null) {
                poolManager.returnConnection(conn);
            }
        }
    }
}
