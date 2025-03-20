package monitoring;

import nurgling.NInventory;


import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ContainerWatcher  implements Runnable {
    NInventory.ParentGob parentGob;
    public java.sql.Connection connection;
    final String sql = "INSERT INTO containers (hash, grid_id, coord) VALUES (?, ?, ?)";
    public ContainerWatcher(NInventory.ParentGob parentGob) {
        this.parentGob = parentGob;
    }

    @Override
    public void run() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, parentGob.hash);
            preparedStatement.setLong(2, parentGob.grid_id);
            preparedStatement.setString(3, parentGob.coord.toString());

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            if (!e.getSQLState().equals("23505")) {  // Код ошибки для нарушения уникальности
                e.printStackTrace();
            }
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
        }

    }
}