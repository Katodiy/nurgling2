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
        // Save main recipe data - use LinkedHashMap to preserve column order
        java.util.LinkedHashMap<String, Object> insertData = new java.util.LinkedHashMap<>();
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
        if (!recipe.getIngredients().isEmpty()) {
            // Upsert ingredients (handles concurrent updates)
            List<Object[]> ingredientParams = new ArrayList<>();
            List<String> ingredientNames = new ArrayList<>();
            for (java.util.Map.Entry<String, Recipe.IngredientInfo> entry : recipe.getIngredients().entrySet()) {
                ingredientParams.add(new Object[]{
                    recipe.getHash(),
                    entry.getKey(),
                    entry.getValue().percentage,
                    entry.getValue().resourceName
                });
                ingredientNames.add(entry.getKey());
            }

            List<String> columns = java.util.Arrays.asList("recipe_hash", "name", "percentage", "resource_name");
            List<String> conflictColumns = java.util.Arrays.asList("recipe_hash", "name");
            List<String> updateColumns = java.util.Arrays.asList("percentage", "resource_name");
            
            String upsertSql = adapter.getBatchUpsertSql("ingredients", columns, conflictColumns, updateColumns);
            adapter.executeBatch(upsertSql, ingredientParams);

            // Delete ingredients no longer in the recipe
            deleteRemovedItems(adapter, "ingredients", recipe.getHash(), ingredientNames);
        } else {
            // No ingredients, delete all
            adapter.executeUpdate("DELETE FROM ingredients WHERE recipe_hash = ?", recipe.getHash());
        }
    }

    private void saveFeps(DatabaseAdapter adapter, Recipe recipe) throws SQLException {
        if (!recipe.getFeps().isEmpty()) {
            // Upsert feps (handles concurrent updates)
            List<Object[]> fepParams = new ArrayList<>();
            List<String> fepNames = new ArrayList<>();
            for (java.util.Map.Entry<String, Recipe.Fep> entry : recipe.getFeps().entrySet()) {
                fepParams.add(new Object[]{
                    recipe.getHash(),
                    entry.getKey(),
                    entry.getValue().val,
                    entry.getValue().weigth
                });
                fepNames.add(entry.getKey());
            }

            List<String> columns = java.util.Arrays.asList("recipe_hash", "name", "value", "weight");
            List<String> conflictColumns = java.util.Arrays.asList("recipe_hash", "name");
            List<String> updateColumns = java.util.Arrays.asList("value", "weight");
            
            String upsertSql = adapter.getBatchUpsertSql("feps", columns, conflictColumns, updateColumns);
            adapter.executeBatch(upsertSql, fepParams);

            // Delete feps no longer in the recipe
            deleteRemovedItems(adapter, "feps", recipe.getHash(), fepNames);
        } else {
            // No feps, delete all
            adapter.executeUpdate("DELETE FROM feps WHERE recipe_hash = ?", recipe.getHash());
        }
    }

    private void deleteRemovedItems(DatabaseAdapter adapter, String table, String recipeHash, 
                                    List<String> currentNames) throws SQLException {
        if (currentNames.isEmpty()) {
            adapter.executeUpdate("DELETE FROM " + table + " WHERE recipe_hash = ?", recipeHash);
            return;
        }

        // Build placeholders for NOT IN clause
        String placeholders = String.join(",", currentNames.stream()
                .map(n -> "?")
                .toArray(String[]::new));
        
        String deleteSql = "DELETE FROM " + table + " WHERE recipe_hash = ? AND name NOT IN (" + placeholders + ")";
        
        // Build params array: recipe_hash followed by all names
        Object[] params = new Object[1 + currentNames.size()];
        params[0] = recipeHash;
        for (int i = 0; i < currentNames.size(); i++) {
            params[i + 1] = currentNames.get(i);
        }
        
        adapter.executeUpdate(deleteSql, params);
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
