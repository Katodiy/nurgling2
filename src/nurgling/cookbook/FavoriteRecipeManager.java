package nurgling.cookbook;

import nurgling.db.DatabaseManager;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class FavoriteRecipeManager {
    private final DatabaseManager databaseManager;

    public FavoriteRecipeManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Set<String> loadFavorites() throws SQLException {
        return databaseManager.getFavoriteRecipeService().loadFavorites();
    }

    public void addFavorite(String recipeHash) throws SQLException {
        databaseManager.getFavoriteRecipeService().addFavorite(recipeHash);
    }

    public void removeFavorite(String recipeHash) throws SQLException {
        databaseManager.getFavoriteRecipeService().removeFavorite(recipeHash);
    }

    public boolean isFavorite(String recipeHash) throws SQLException {
        return databaseManager.getFavoriteRecipeService().isFavorite(recipeHash);
    }

    public void toggleFavorite(String recipeHash) throws SQLException {
        databaseManager.getFavoriteRecipeService().toggleFavorite(recipeHash);
    }
}
