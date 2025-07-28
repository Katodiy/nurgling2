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
        try {
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return parentGob.ngob.hash!=null;
                }
            });
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, parentGob.ngob.hash);
            preparedStatement.setLong(2, parentGob.ngob.grid_id);
            preparedStatement.setString(3, parentGob.ngob.gcoord.toString());

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            if (e.getSQLState()!=null && !e.getSQLState().equals("23505")) {  // Код ошибки для нарушения уникальности
                e.printStackTrace();
            }
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}