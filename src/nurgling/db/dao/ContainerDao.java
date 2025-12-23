package nurgling.db.dao;

import nurgling.db.DatabaseAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Container entities
 */
public class ContainerDao {

    /**
     * Container data class
     */
    public static class ContainerData {
        private final String hash;
        private final long gridId;
        private final String coord;

        public ContainerData(String hash, long gridId, String coord) {
            this.hash = hash;
            this.gridId = gridId;
            this.coord = coord;
        }

        public String getHash() { return hash; }
        public long getGridId() { return gridId; }
        public String getCoord() { return coord; }
    }

    /**
     * Save or update container
     */
    public void saveContainer(DatabaseAdapter adapter, String hash, long gridId, String coord) throws SQLException {
        String sql = adapter.getUpsertSql("containers",
                                         java.util.Map.of("hash", hash,
                                                         "grid_id", gridId,
                                                         "coord", coord),
                                         java.util.List.of("hash"));
        adapter.executeUpdate(sql, hash, gridId, coord);
    }

    /**
     * Load all containers
     */
    public List<ContainerData> loadAllContainers(DatabaseAdapter adapter) throws SQLException {
        List<ContainerData> containers = new ArrayList<>();

        try (ResultSet rs = adapter.executeQuery("SELECT hash, grid_id, coord FROM containers")) {
            while (rs.next()) {
                containers.add(new ContainerData(
                    rs.getString("hash"),
                    rs.getLong("grid_id"),
                    rs.getString("coord")
                ));
            }
        }

        return containers;
    }

    /**
     * Load container by hash
     */
    public ContainerData loadContainer(DatabaseAdapter adapter, String hash) throws SQLException {
        try (ResultSet rs = adapter.executeQuery("SELECT hash, grid_id, coord FROM containers WHERE hash = ?",
                                                hash)) {
            if (rs.next()) {
                return new ContainerData(
                    rs.getString("hash"),
                    rs.getLong("grid_id"),
                    rs.getString("coord")
                );
            }
        }
        return null;
    }

    /**
     * Delete container
     */
    public void deleteContainer(DatabaseAdapter adapter, String hash) throws SQLException {
        adapter.executeUpdate("DELETE FROM containers WHERE hash = ?", hash);
        // CASCADE will handle storageitems deletion
    }

    /**
     * Check if container exists
     */
    public boolean containerExists(DatabaseAdapter adapter, String hash) throws SQLException {
        try (ResultSet rs = adapter.executeQuery("SELECT 1 FROM containers WHERE hash = ? LIMIT 1", hash)) {
            return rs.next();
        }
    }
}
