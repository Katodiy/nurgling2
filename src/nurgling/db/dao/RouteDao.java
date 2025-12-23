package nurgling.db.dao;

import nurgling.db.DatabaseAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Route entities
 */
public class RouteDao {

    /**
     * Data class representing a route from the database
     */
    public static class RouteData {
        private final int id;
        private final String name;
        private final String path;
        private final String data;
        private final String profile;
        private final int version;
        private final Timestamp updatedAt;

        public RouteData(int id, String name, String path, String data, 
                        String profile, int version, Timestamp updatedAt) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.data = data;
            this.profile = profile;
            this.version = version;
            this.updatedAt = updatedAt;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getPath() { return path; }
        public String getData() { return data; }
        public String getProfile() { return profile; }
        public int getVersion() { return version; }
        public Timestamp getUpdatedAt() { return updatedAt; }
    }

    /**
     * Save or update a route with version increment only if data changed
     * @return the new version number, or current version if nothing changed
     */
    public int saveRoute(DatabaseAdapter adapter, int id, String name, String path,
                        String data, String profile) throws SQLException {

        String checkSql = "SELECT version, data, name, path FROM routes WHERE id = ? AND profile = ?";
        int currentVersion = 0;
        boolean exists = false;
        String existingData = null;
        String existingName = null;
        String existingPath = null;
        
        try (ResultSet rs = adapter.executeQuery(checkSql, id, profile)) {
            if (rs.next()) {
                exists = true;
                currentVersion = rs.getInt("version");
                existingData = rs.getString("data");
                existingName = rs.getString("name");
                existingPath = rs.getString("path");
            }
        }

        if (exists) {
            // Check if anything actually changed
            boolean dataChanged = !data.equals(existingData) || 
                                  !name.equals(existingName) || 
                                  !path.equals(existingPath);
            
            if (dataChanged) {
                int newVersion = currentVersion + 1;
                String updateSql = "UPDATE routes SET name = ?, path = ?, " +
                                  "data = ?, version = ?, updated_at = CURRENT_TIMESTAMP " +
                                  "WHERE id = ? AND profile = ?";
                adapter.executeUpdate(updateSql, name, path, data, newVersion, id, profile);
                return newVersion;
            }
            return currentVersion;
        } else {
            String insertSql = "INSERT INTO routes (id, name, path, data, profile, version) " +
                              "VALUES (?, ?, ?, ?, ?, 1)";
            adapter.executeUpdate(insertSql, id, name, path, data, profile);
            return 1;
        }
    }

    /**
     * Load all routes for a profile
     */
    public List<RouteData> loadRoutesByProfile(DatabaseAdapter adapter, String profile) throws SQLException {
        List<RouteData> routes = new ArrayList<>();
        String sql = "SELECT id, name, path, data, profile, version, updated_at FROM routes WHERE profile = ?";
        
        try (ResultSet rs = adapter.executeQuery(sql, profile)) {
            while (rs.next()) {
                routes.add(new RouteData(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("path"),
                    rs.getString("data"),
                    rs.getString("profile"),
                    rs.getInt("version"),
                    rs.getTimestamp("updated_at")
                ));
            }
        }
        return routes;
    }

    /**
     * Load a single route
     */
    public RouteData loadRoute(DatabaseAdapter adapter, int id, String profile) throws SQLException {
        String sql = "SELECT id, name, path, data, profile, version, updated_at FROM routes WHERE id = ? AND profile = ?";
        
        try (ResultSet rs = adapter.executeQuery(sql, id, profile)) {
            if (rs.next()) {
                return new RouteData(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("path"),
                    rs.getString("data"),
                    rs.getString("profile"),
                    rs.getInt("version"),
                    rs.getTimestamp("updated_at")
                );
            }
        }
        return null;
    }

    /**
     * Delete a route
     */
    public void deleteRoute(DatabaseAdapter adapter, int id, String profile) throws SQLException {
        String sql = "DELETE FROM routes WHERE id = ? AND profile = ?";
        adapter.executeUpdate(sql, id, profile);
    }

    /**
     * Get all route versions for a profile (for sync comparison)
     */
    public Map<Integer, Integer> getAllRouteVersions(DatabaseAdapter adapter, String profile) throws SQLException {
        Map<Integer, Integer> versions = new HashMap<>();
        String sql = "SELECT id, version FROM routes WHERE profile = ?";
        
        try (ResultSet rs = adapter.executeQuery(sql, profile)) {
            while (rs.next()) {
                versions.put(rs.getInt("id"), rs.getInt("version"));
            }
        }
        return versions;
    }

    /**
     * Get count of routes for a profile
     */
    public int getRoutesCount(DatabaseAdapter adapter, String profile) throws SQLException {
        String sql = "SELECT COUNT(*) as cnt FROM routes WHERE profile = ?";
        try (ResultSet rs = adapter.executeQuery(sql, profile)) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        return 0;
    }
}




