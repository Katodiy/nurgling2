package nurgling.db.dao;

import nurgling.db.DatabaseAdapter;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Area entities
 */
public class AreaDao {

    /**
     * Area data class for database operations
     */
    public static class AreaData {
        private final int id;
        private final String name;
        private final String path;
        private final boolean hide;
        private final int colorR;
        private final int colorG;
        private final int colorB;
        private final int colorA;
        private final String data; // JSON string containing space, in, out, spec
        private final String profile;
        private final Timestamp updatedAt;
        private final int version;

        public AreaData(int id, String name, String path, boolean hide,
                       int colorR, int colorG, int colorB, int colorA,
                       String data, String profile, Timestamp updatedAt, int version) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.hide = hide;
            this.colorR = colorR;
            this.colorG = colorG;
            this.colorB = colorB;
            this.colorA = colorA;
            this.data = data;
            this.profile = profile;
            this.updatedAt = updatedAt;
            this.version = version;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getPath() { return path; }
        public boolean isHide() { return hide; }
        public int getColorR() { return colorR; }
        public int getColorG() { return colorG; }
        public int getColorB() { return colorB; }
        public int getColorA() { return colorA; }
        public String getData() { return data; }
        public String getProfile() { return profile; }
        public Timestamp getUpdatedAt() { return updatedAt; }
        public int getVersion() { return version; }

        /**
         * Convert to JSON for NArea compatibility
         */
        public JSONObject toJson() {
            JSONObject json = new JSONObject(data);
            json.put("id", id);
            json.put("name", name);
            json.put("path", path);
            json.put("hide", hide);

            JSONObject color = new JSONObject();
            color.put("r", colorR);
            color.put("g", colorG);
            color.put("b", colorB);
            color.put("a", colorA);
            json.put("color", color);

            return json;
        }
    }

    /**
     * Save or update an area with version increment only if data changed
     * @return the new version number, or current version if nothing changed
     */
    public int saveArea(DatabaseAdapter adapter, int id, String name, String path, boolean hide,
                        int colorR, int colorG, int colorB, int colorA,
                        String data, String profile) throws SQLException {

        // Determine hide value based on adapter type
        Object hideValue = (adapter instanceof nurgling.db.PostgresAdapter) ? hide : (hide ? 1 : 0);
        
        // Use UPSERT to handle race conditions
        if (adapter instanceof nurgling.db.PostgresAdapter) {
            // PostgreSQL: INSERT ON CONFLICT with version increment
            // Note: primary key is just 'id', not (id, profile)
            String upsertSql = "INSERT INTO areas (id, name, path, hide, color_r, color_g, color_b, color_a, data, profile, version, updated_at) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP) " +
                              "ON CONFLICT (id) DO UPDATE SET " +
                              "name = EXCLUDED.name, path = EXCLUDED.path, hide = EXCLUDED.hide, " +
                              "color_r = EXCLUDED.color_r, color_g = EXCLUDED.color_g, " +
                              "color_b = EXCLUDED.color_b, color_a = EXCLUDED.color_a, " +
                              "data = EXCLUDED.data, profile = EXCLUDED.profile, " +
                              "version = areas.version + 1, " +
                              "updated_at = CURRENT_TIMESTAMP " +
                              "WHERE areas.data != EXCLUDED.data OR areas.name != EXCLUDED.name OR areas.path != EXCLUDED.path " +
                              "RETURNING version";
            
            try (ResultSet rs = adapter.executeQuery(upsertSql,
                    id, name, path, hideValue,
                    colorR, colorG, colorB, colorA,
                    data, profile)) {
                if (rs.next()) {
                    return rs.getInt("version");
                }
            }
            // If no rows returned, nothing changed - get current version
            try (ResultSet rs = adapter.executeQuery("SELECT version FROM areas WHERE id = ? AND profile = ?", id, profile)) {
                if (rs.next()) {
                    return rs.getInt("version");
                }
            }
            return 1;
        } else {
            // SQLite: INSERT OR REPLACE
            String upsertSql = "INSERT OR REPLACE INTO areas (id, name, path, hide, color_r, color_g, color_b, color_a, data, profile, version, updated_at) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                              "COALESCE((SELECT version + 1 FROM areas WHERE id = ? AND profile = ?), 1), " +
                              "CURRENT_TIMESTAMP)";
            adapter.executeUpdate(upsertSql,
                id, name, path, hideValue,
                colorR, colorG, colorB, colorA,
                data, profile, id, profile);
            
            // Get the resulting version
            try (ResultSet rs = adapter.executeQuery("SELECT version FROM areas WHERE id = ? AND profile = ?", id, profile)) {
                if (rs.next()) {
                    return rs.getInt("version");
                }
            }
            return 1;
        }
    }

    /**
     * Get version of an area
     */
    public int getAreaVersion(DatabaseAdapter adapter, int id, String profile) throws SQLException {
        String sql = "SELECT version FROM areas WHERE id = ? AND profile = ?";
        try (ResultSet rs = adapter.executeQuery(sql, id, profile)) {
            if (rs.next()) {
                return rs.getInt("version");
            }
        }
        return 0; // Area doesn't exist
    }

    /**
     * Load all areas for a specific profile
     */
    public List<AreaData> loadAreasByProfile(DatabaseAdapter adapter, String profile) throws SQLException {
        List<AreaData> areas = new ArrayList<>();

        String sql = "SELECT id, name, path, hide, color_r, color_g, color_b, color_a, data, profile, updated_at, version " +
                    "FROM areas WHERE profile = ? ORDER BY id";

        try (ResultSet rs = adapter.executeQuery(sql, profile)) {
            while (rs.next()) {
                areas.add(new AreaData(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("path"),
                    rs.getBoolean("hide"),
                    rs.getInt("color_r"),
                    rs.getInt("color_g"),
                    rs.getInt("color_b"),
                    rs.getInt("color_a"),
                    rs.getString("data"),
                    rs.getString("profile"),
                    rs.getTimestamp("updated_at"),
                    rs.getInt("version")
                ));
            }
        }

        return areas;
    }

    /**
     * Load area by id and profile
     */
    public AreaData loadArea(DatabaseAdapter adapter, int id, String profile) throws SQLException {
        String sql = "SELECT id, name, path, hide, color_r, color_g, color_b, color_a, data, profile, updated_at, version " +
                    "FROM areas WHERE id = ? AND profile = ?";

        try (ResultSet rs = adapter.executeQuery(sql, id, profile)) {
            if (rs.next()) {
                return new AreaData(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("path"),
                    rs.getBoolean("hide"),
                    rs.getInt("color_r"),
                    rs.getInt("color_g"),
                    rs.getInt("color_b"),
                    rs.getInt("color_a"),
                    rs.getString("data"),
                    rs.getString("profile"),
                    rs.getTimestamp("updated_at"),
                    rs.getInt("version")
                );
            }
        }
        return null;
    }

    /**
     * Delete an area
     */
    public void deleteArea(DatabaseAdapter adapter, int id, String profile) throws SQLException {
        adapter.executeUpdate("DELETE FROM areas WHERE id = ? AND profile = ?", id, profile);
    }

    /**
     * Delete all areas for a profile
     */
    public void deleteAllAreas(DatabaseAdapter adapter, String profile) throws SQLException {
        adapter.executeUpdate("DELETE FROM areas WHERE profile = ?", profile);
    }

    /**
     * Check if area exists
     */
    public boolean areaExists(DatabaseAdapter adapter, int id, String profile) throws SQLException {
        try (ResultSet rs = adapter.executeQuery("SELECT 1 FROM areas WHERE id = ? AND profile = ?", id, profile)) {
            return rs.next();
        }
    }

    /**
     * Get the maximum updated_at timestamp for a profile
     */
    public Timestamp getLastUpdateTime(DatabaseAdapter adapter, String profile) throws SQLException {
        String sql = "SELECT MAX(updated_at) as last_update FROM areas WHERE profile = ?";
        try (ResultSet rs = adapter.executeQuery(sql, profile)) {
            if (rs.next()) {
                return rs.getTimestamp("last_update");
            }
        }
        return null;
    }

    /**
     * Get areas updated after a specific timestamp
     */
    public List<AreaData> getAreasUpdatedAfter(DatabaseAdapter adapter, String profile, Timestamp after) throws SQLException {
        List<AreaData> areas = new ArrayList<>();

        String sql = "SELECT id, name, path, hide, color_r, color_g, color_b, color_a, data, profile, updated_at, version " +
                    "FROM areas WHERE profile = ? AND updated_at > ? ORDER BY id";

        try (ResultSet rs = adapter.executeQuery(sql, profile, after)) {
            while (rs.next()) {
                areas.add(new AreaData(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("path"),
                    rs.getBoolean("hide"),
                    rs.getInt("color_r"),
                    rs.getInt("color_g"),
                    rs.getInt("color_b"),
                    rs.getInt("color_a"),
                    rs.getString("data"),
                    rs.getString("profile"),
                    rs.getTimestamp("updated_at"),
                    rs.getInt("version")
                ));
            }
        }

        return areas;
    }

    /**
     * Get count of areas for a profile
     */
    public int getAreasCount(DatabaseAdapter adapter, String profile) throws SQLException {
        String sql = "SELECT COUNT(*) as cnt FROM areas WHERE profile = ?";
        try (ResultSet rs = adapter.executeQuery(sql, profile)) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        return 0;
    }

    /**
     * Get all area versions for a profile (for efficient version comparison)
     */
    public java.util.Map<Integer, Integer> getAllAreaVersions(DatabaseAdapter adapter, String profile) throws SQLException {
        java.util.Map<Integer, Integer> versions = new java.util.HashMap<>();
        String sql = "SELECT id, version FROM areas WHERE profile = ?";
        try (ResultSet rs = adapter.executeQuery(sql, profile)) {
            while (rs.next()) {
                versions.put(rs.getInt("id"), rs.getInt("version"));
            }
        }
        return versions;
    }
}
