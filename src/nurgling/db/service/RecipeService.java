package nurgling.db.service;

import nurgling.cookbook.Recipe;
import nurgling.db.DatabaseManager;
import nurgling.db.dao.RecipeDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;

/**
 * Service layer for recipe operations
 */
public class RecipeService {
    private final DatabaseManager databaseManager;
    private final RecipeDao recipeDao;
    
    // Executor with limited queue for recipe save operations
    private final ExecutorService saveExecutor;
    private final Semaphore saveSemaphore;
    private static final int MAX_CONCURRENT_SAVES = 2;

    public RecipeService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.recipeDao = new RecipeDao();
        
        // Create executor with queue for save operations
        this.saveExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_SAVES, r -> {
            Thread thread = new Thread(r);
            thread.setName("RecipeSaveWorker-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
        
        this.saveSemaphore = new Semaphore(MAX_CONCURRENT_SAVES);
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
     * Save recipe asynchronously with queuing to avoid connection pool exhaustion
     */
    public CompletableFuture<Void> saveRecipeAsync(Recipe recipe) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Acquire permit before attempting to save
                saveSemaphore.acquire();
                try {
                    saveRecipe(recipe);
                } finally {
                    saveSemaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Recipe save interrupted", e);
            } catch (SQLException e) {
                System.err.println("Failed to save recipe '" + recipe.getName() + "': " + e.getMessage());
                throw new RuntimeException("Failed to save recipe", e);
            }
        }, saveExecutor);
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
    
    /**
     * Shutdown the service gracefully
     */
    public void shutdown() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
