package nurgling.cookbook;

import nurgling.DBPoolManager;
import nurgling.NConfig;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class FavoriteRecipeManager {
    private final DBPoolManager poolManager;

    public FavoriteRecipeManager(DBPoolManager poolManager) {
        this.poolManager = poolManager;
    }

    public Set<String> loadFavorites() throws SQLException {
        Set<String> favorites = new HashSet<>();
        Connection conn = null;

        try {
            conn = poolManager.getConnection();
            if (conn == null) {
                return favorites;
            }

            String query = "SELECT recipe_hash FROM favorite_recipes";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    favorites.add(rs.getString("recipe_hash"));
                }
            }

            conn.commit();
        } finally {
            if (conn != null) {
                poolManager.returnConnection(conn);
            }
        }

        return favorites;
    }

    public void addFavorite(String recipeHash) throws SQLException {
        Connection conn = null;

        try {
            conn = poolManager.getConnection();
            if (conn == null) {
                throw new SQLException("Unable to get database connection");
            }

            String query;
            if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                query = "INSERT INTO favorite_recipes (recipe_hash) VALUES (?) ON CONFLICT DO NOTHING";
            } else {
                // SQLite uses INSERT OR IGNORE
                query = "INSERT OR IGNORE INTO favorite_recipes (recipe_hash) VALUES (?)";
            }

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, recipeHash);
                stmt.executeUpdate();
                conn.commit();
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                poolManager.returnConnection(conn);
            }
        }
    }

    public void removeFavorite(String recipeHash) throws SQLException {
        Connection conn = null;

        try {
            conn = poolManager.getConnection();
            if (conn == null) {
                throw new SQLException("Unable to get database connection");
            }

            String query = "DELETE FROM favorite_recipes WHERE recipe_hash = ?";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, recipeHash);
                stmt.executeUpdate();
                conn.commit();
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                poolManager.returnConnection(conn);
            }
        }
    }

    public boolean isFavorite(String recipeHash) throws SQLException {
        Connection conn = null;

        try {
            conn = poolManager.getConnection();
            if (conn == null) {
                return false;
            }

            String query = "SELECT COUNT(*) as cnt FROM favorite_recipes WHERE recipe_hash = ?";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, recipeHash);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }

            conn.commit();
        } finally {
            if (conn != null) {
                poolManager.returnConnection(conn);
            }
        }

        return false;
    }

    public void toggleFavorite(String recipeHash) throws SQLException {
        if (isFavorite(recipeHash)) {
            removeFavorite(recipeHash);
        } else {
            addFavorite(recipeHash);
        }
    }
}
