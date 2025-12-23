package nurgling.db.dao;

import nurgling.cookbook.Recipe;
import nurgling.db.DatabaseAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Data Access Object for Recipe entities
 */
public class RecipeDao {

    /**
     * Load recipes by their hashes
     */
    public List<Recipe> loadRecipes(DatabaseAdapter adapter, List<String> recipeHashes) throws SQLException {
        if (recipeHashes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Recipe> recipes = new ArrayList<>();
        HashMap<String, Recipe> res = new HashMap<>();

        // For SQLite, build IN clause manually with placeholders
        String placeholders = String.join(",", recipeHashes.stream()
                .map(h -> "?")
                .toArray(String[]::new));

        String sql = "SELECT r.recipe_hash, r.item_name, r.resource_name, r.hunger, r.energy, " +
                "i.name AS ingredient_name, i.percentage, i.resource_name AS ing_resource, " +
                "f.name AS fep_name, f.value AS fep_value, f.weight as fep_weight " +
                "FROM recipes r " +
                "LEFT JOIN ingredients i ON r.recipe_hash = i.recipe_hash " +
                "LEFT JOIN feps f ON r.recipe_hash = f.recipe_hash " +
                "WHERE r.recipe_hash IN (" + placeholders + ")";

        try (ResultSet rs = adapter.executeQuery(sql, recipeHashes.toArray())) {
            while (rs.next()) {
                String hash = rs.getString("recipe_hash");
                Recipe recipe = res.computeIfAbsent(hash, k -> {
                    try {
                        return new Recipe(
                                hash,
                                rs.getString("item_name"),
                                rs.getString("resource_name"),
                                rs.getDouble("hunger"),
                                rs.getInt("energy"),
                                new HashMap<>(),
                                new HashMap<>()
                        );
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

                String ingredientName = rs.getString("ingredient_name");
                if (!rs.wasNull() && ingredientName != null) {
                    String ingResource = rs.getString("ing_resource");
                    recipe.getIngredients().put(
                            ingredientName,
                            new Recipe.IngredientInfo(rs.getDouble("percentage"), ingResource)
                    );
                }

                String fepName = rs.getString("fep_name");
                if (!rs.wasNull() && fepName != null) {
                    recipe.getFeps().put(
                            fepName,
                            new Recipe.Fep(rs.getDouble("fep_value"), rs.getDouble("fep_weight"))
                    );
                }
            }
        }

        // Maintain order from input list
        for (String hash : recipeHashes) {
            Recipe recipe = res.get(hash);
            if (recipe != null) {
                recipes.add(recipe);
            }
        }

        return recipes;
    }

    /**
     * Save a recipe with its ingredients and feps
     */
    public void saveRecipe(DatabaseAdapter adapter, Recipe recipe) throws SQLException {
        // Save main recipe data - use adapter-specific upsert
        java.util.Map<String, Object> insertData = new java.util.HashMap<>();
        insertData.put("recipe_hash", recipe.getHash());
        insertData.put("item_name", recipe.getName());
        insertData.put("resource_name", recipe.getResourceName());
        insertData.put("hunger", recipe.getHunger());
        insertData.put("energy", recipe.getEnergy());

        java.util.List<String> conflictColumns = new java.util.ArrayList<>();
        conflictColumns.add("recipe_hash");

        String insertRecipeSql = adapter.getUpsertSql("recipes", insertData, conflictColumns);

        adapter.executeUpdate(insertRecipeSql,
                             recipe.getHash(),
                             recipe.getName(),
                             recipe.getResourceName(),
                             recipe.getHunger(),
                             recipe.getEnergy());

        // Save ingredients
        saveIngredients(adapter, recipe);

        // Save feps
        saveFeps(adapter, recipe);
    }

    private void saveIngredients(DatabaseAdapter adapter, Recipe recipe) throws SQLException {
        // Delete existing ingredients
        adapter.executeUpdate("DELETE FROM ingredients WHERE recipe_hash = ?", recipe.getHash());

        // Insert new ingredients
        if (!recipe.getIngredients().isEmpty()) {
            List<Object[]> ingredientParams = new ArrayList<>();
            for (java.util.Map.Entry<String, Recipe.IngredientInfo> entry : recipe.getIngredients().entrySet()) {
                ingredientParams.add(new Object[]{
                    recipe.getHash(),
                    entry.getKey(),
                    entry.getValue().percentage,
                    entry.getValue().resourceName
                });
            }

            String insertIngredientSql = "INSERT INTO ingredients (recipe_hash, name, percentage, resource_name) " +
                                       "VALUES (?, ?, ?, ?)";
            adapter.executeBatch(insertIngredientSql, ingredientParams);
        }
    }

    private void saveFeps(DatabaseAdapter adapter, Recipe recipe) throws SQLException {
        // Delete existing feps
        adapter.executeUpdate("DELETE FROM feps WHERE recipe_hash = ?", recipe.getHash());

        // Insert new feps
        if (!recipe.getFeps().isEmpty()) {
            List<Object[]> fepParams = new ArrayList<>();
            for (java.util.Map.Entry<String, Recipe.Fep> entry : recipe.getFeps().entrySet()) {
                fepParams.add(new Object[]{
                    recipe.getHash(),
                    entry.getKey(),
                    entry.getValue().val,
                    entry.getValue().weigth
                });
            }

            String insertFepSql = "INSERT INTO feps (recipe_hash, name, value, weight) VALUES (?, ?, ?, ?)";
            adapter.executeBatch(insertFepSql, fepParams);
        }
    }

    /**
     * Delete a recipe and all its related data
     */
    public void deleteRecipe(DatabaseAdapter adapter, String recipeHash) throws SQLException {
        adapter.executeUpdate("DELETE FROM recipes WHERE recipe_hash = ?", recipeHash);
        // CASCADE constraints will handle ingredients and feps deletion
    }

    /**
     * Check if recipe exists
     */
    public boolean recipeExists(DatabaseAdapter adapter, String recipeHash) throws SQLException {
        try (ResultSet rs = adapter.executeQuery("SELECT 1 FROM recipes WHERE recipe_hash = ? LIMIT 1", recipeHash)) {
            return rs.next();
        }
    }
}
