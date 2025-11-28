package nurgling.cookbook;

import nurgling.NConfig;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class FavoriteRecipeManager {
    private final Connection connection;

    public FavoriteRecipeManager(Connection connection) throws SQLException {
        this.connection = connection;
        ensureTableExists();
    }

    private void ensureTableExists() throws SQLException {
        try {
            // Try to query the table - if it exists, this will succeed
            Statement stmt = connection.createStatement();
            stmt.executeQuery("SELECT 1 FROM favorite_recipes LIMIT 1").close();
            stmt.close();
            // Table exists, nothing to do
        } catch (SQLException e) {
            // Rollback the failed transaction
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                // Ignore rollback errors
            }
            
            // Table doesn't exist, create it
            String createTableQuery = "CREATE TABLE favorite_recipes (" +
                                     "recipe_hash VARCHAR(64) PRIMARY KEY REFERENCES recipes (recipe_hash) ON DELETE CASCADE" +
                                     ")";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(createTableQuery);
            stmt.close();
            connection.commit();
            System.out.println("Created favorite_recipes table");
        }
    }

    public Set<String> loadFavorites() throws SQLException {
        Set<String> favorites = new HashSet<>();
        String query = "SELECT recipe_hash FROM favorite_recipes";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                favorites.add(rs.getString("recipe_hash"));
            }
        }
        return favorites;
    }

    public void addFavorite(String recipeHash) throws SQLException {
        String query;
        if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
            query = "INSERT INTO favorite_recipes (recipe_hash) VALUES (?) ON CONFLICT DO NOTHING";
        } else {
            // SQLite uses INSERT OR IGNORE
            query = "INSERT OR IGNORE INTO favorite_recipes (recipe_hash) VALUES (?)";
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, recipeHash);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public void removeFavorite(String recipeHash) throws SQLException {
        String query = "DELETE FROM favorite_recipes WHERE recipe_hash = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, recipeHash);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public boolean isFavorite(String recipeHash) throws SQLException {
        String query = "SELECT COUNT(*) as cnt FROM favorite_recipes WHERE recipe_hash = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, recipeHash);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("cnt") > 0;
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
