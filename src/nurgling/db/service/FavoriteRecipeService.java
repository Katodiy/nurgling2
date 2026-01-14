package nurgling.db.service;

import nurgling.db.DatabaseManager;
import nurgling.db.dao.FavoriteRecipeDao;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for favorite recipe operations
 */
public class FavoriteRecipeService {
    private final DatabaseManager databaseManager;
    private final FavoriteRecipeDao favoriteRecipeDao;

    public FavoriteRecipeService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.favoriteRecipeDao = new FavoriteRecipeDao();
    }

    /**
     * Load all favorites asynchronously
     */
    public CompletableFuture<Set<String>> loadFavoritesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadFavorites();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load favorites", e);
            }
        });
    }

    /**
     * Load all favorites synchronously
     */
    public Set<String> loadFavorites() throws SQLException {
        return databaseManager.executeOperation(adapter -> favoriteRecipeDao.loadFavorites(adapter));
    }

    /**
     * Add recipe to favorites asynchronously
     */
    public CompletableFuture<Void> addFavoriteAsync(String recipeHash) {
        return CompletableFuture.runAsync(() -> {
            try {
                addFavorite(recipeHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add favorite", e);
            }
        });
    }

    /**
     * Add recipe to favorites synchronously
     */
    public void addFavorite(String recipeHash) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            favoriteRecipeDao.addFavorite(adapter, recipeHash);
            return null;
        });
    }

    /**
     * Remove recipe from favorites asynchronously
     */
    public CompletableFuture<Void> removeFavoriteAsync(String recipeHash) {
        return CompletableFuture.runAsync(() -> {
            try {
                removeFavorite(recipeHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove favorite", e);
            }
        });
    }

    /**
     * Remove recipe from favorites synchronously
     */
    public void removeFavorite(String recipeHash) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            favoriteRecipeDao.removeFavorite(adapter, recipeHash);
            return null;
        });
    }

    /**
     * Check if recipe is favorite asynchronously
     */
    public CompletableFuture<Boolean> isFavoriteAsync(String recipeHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return isFavorite(recipeHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check favorite status", e);
            }
        });
    }

    /**
     * Check if recipe is favorite synchronously
     */
    public boolean isFavorite(String recipeHash) throws SQLException {
        return databaseManager.executeOperation(adapter -> favoriteRecipeDao.isFavorite(adapter, recipeHash));
    }

    /**
     * Toggle favorite status asynchronously
     */
    public CompletableFuture<Boolean> toggleFavoriteAsync(String recipeHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return toggleFavorite(recipeHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to toggle favorite", e);
            }
        });
    }

    /**
     * Toggle favorite status synchronously
     */
    public boolean toggleFavorite(String recipeHash) throws SQLException {
        return databaseManager.executeOperation(adapter -> favoriteRecipeDao.toggleFavorite(adapter, recipeHash));
    }
}
