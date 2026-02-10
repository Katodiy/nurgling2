package nurgling.cookbook.connection;

import nurgling.NConfig;
import nurgling.cookbook.Recipe;
import nurgling.db.DatabaseManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecipeLoader implements Runnable {
    private final DatabaseManager databaseManager;
    private final ArrayList<String> recipeHashes;
    private final ArrayList<Recipe> recipes = new ArrayList<>();
    public AtomicBoolean ready = new AtomicBoolean(false);

    public RecipeLoader(DatabaseManager databaseManager, ArrayList<String> recipeHashes) {
        this.databaseManager = databaseManager;
        this.recipeHashes = recipeHashes;
    }

    @Override
    public void run() {
        if (recipeHashes.isEmpty()) {
            ready.set(true);
            return;
        }

        try {
            List<Recipe> loadedRecipes = databaseManager.getRecipeService().loadRecipes(recipeHashes);
            recipes.addAll(loadedRecipes);
        } catch (Exception e) {
            System.err.println("Error loading recipes:");
            e.printStackTrace();
        } finally {
            ready.set(true);
        }
    }

    public ArrayList<Recipe> getRecipes() {
        return recipes;
    }
}
