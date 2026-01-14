package nurgling.db.dao;

import nurgling.db.DatabaseAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Data Access Object for Favorite Recipe entities
 */
public class FavoriteRecipeDao {

    /**
     * Load all favorite recipe hashes
     */
    public Set<String> loadFavorites(DatabaseAdapter adapter) throws SQLException {
        Set<String> favorites = new HashSet<>();

        try (ResultSet rs = adapter.executeQuery("SELECT recipe_hash FROM favorite_recipes")) {
            while (rs.next()) {
                favorites.add(rs.getString("recipe_hash"));
            }
        }

        return favorites;
    }

    /**
     * Add a recipe to favorites
     */
    public void addFavorite(DatabaseAdapter adapter, String recipeHash) throws SQLException {
        String sql = adapter.getUpsertSql("favorite_recipes",
                                         java.util.Map.of("recipe_hash", recipeHash),
                                         java.util.List.of("recipe_hash"));
        adapter.executeUpdate(sql, recipeHash);
    }

    /**
     * Remove a recipe from favorites
     */
    public void removeFavorite(DatabaseAdapter adapter, String recipeHash) throws SQLException {
        adapter.executeUpdate("DELETE FROM favorite_recipes WHERE recipe_hash = ?", recipeHash);
    }

    /**
     * Check if a recipe is in favorites
     */
    public boolean isFavorite(DatabaseAdapter adapter, String recipeHash) throws SQLException {
        try (ResultSet rs = adapter.executeQuery("SELECT COUNT(*) as cnt FROM favorite_recipes WHERE recipe_hash = ?",
                                                recipeHash)) {
            return rs.next() && rs.getInt("cnt") > 0;
        }
    }

    /**
     * Toggle favorite status for a recipe
     */
    public boolean toggleFavorite(DatabaseAdapter adapter, String recipeHash) throws SQLException {
        boolean isCurrentlyFavorite = isFavorite(adapter, recipeHash);
        if (isCurrentlyFavorite) {
            removeFavorite(adapter, recipeHash);
        } else {
            addFavorite(adapter, recipeHash);
        }
        return !isCurrentlyFavorite;
    }
}
