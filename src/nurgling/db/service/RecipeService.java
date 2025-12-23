package nurgling.db.service;

import nurgling.cookbook.Recipe;
import nurgling.db.DatabaseManager;
import nurgling.db.dao.RecipeDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for recipe operations
 */
public class RecipeService {
    private final DatabaseManager databaseManager;
    private final RecipeDao recipeDao;

    public RecipeService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.recipeDao = new RecipeDao();
    }

    /**
     * Load recipes asynchronously by their hashes
     */
    public CompletableFuture<List<Recipe>> loadRecipesAsync(List<String> recipeHashes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return databaseManager.executeOperation(adapter -> recipeDao.loadRecipes(adapter, recipeHashes));
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load recipes", e);
            }
        });
    }

    /**
     * Load recipes synchronously by their hashes
     */
    public List<Recipe> loadRecipes(List<String> recipeHashes) throws SQLException {
        return databaseManager.executeOperation(adapter -> recipeDao.loadRecipes(adapter, recipeHashes));
    }

    /**
     * Save recipe asynchronously
     */
    public CompletableFuture<Void> saveRecipeAsync(Recipe recipe) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveRecipe(recipe);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save recipe", e);
            }
        });
    }

    /**
     * Save recipe synchronously
     */
    public void saveRecipe(Recipe recipe) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            recipeDao.saveRecipe(adapter, recipe);
            return null;
        });
    }

    /**
     * Delete recipe asynchronously
     */
    public CompletableFuture<Void> deleteRecipeAsync(String recipeHash) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteRecipe(recipeHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete recipe", e);
            }
        });
    }

    /**
     * Delete recipe synchronously
     */
    public void deleteRecipe(String recipeHash) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            recipeDao.deleteRecipe(adapter, recipeHash);
            return null;
        });
    }

    /**
     * Check if recipe exists asynchronously
     */
    public CompletableFuture<Boolean> recipeExistsAsync(String recipeHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return recipeExists(recipeHash);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check recipe existence", e);
            }
        });
    }

    /**
     * Check if recipe exists synchronously
     */
    public boolean recipeExists(String recipeHash) throws SQLException {
        return databaseManager.executeOperation(adapter -> recipeDao.recipeExists(adapter, recipeHash));
    }
}
