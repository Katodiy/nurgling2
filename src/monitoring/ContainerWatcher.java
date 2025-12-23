package monitoring;

import haven.Gob;
import nurgling.NUtils;
import nurgling.db.DatabaseManager;
import nurgling.tasks.NTask;

import java.sql.SQLException;

public class ContainerWatcher implements Runnable {
    private final Gob parentGob;
    private final DatabaseManager databaseManager;

    public ContainerWatcher(Gob parentGob, DatabaseManager databaseManager) {
        this.parentGob = parentGob;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
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

            // Save container using service
            databaseManager.getContainerService().saveContainer(
                parentGob.ngob.hash,
                parentGob.ngob.grid_id,
                parentGob.ngob.gcoord.toString()
            );

        } catch (SQLException e) {
            // SQLState 23505 = unique constraint violation (container already exists) - this is expected
            if (e.getSQLState() == null || !e.getSQLState().equals("23505")) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            // Bot was stopped, don't print stack trace
        }
    }
}
