package nurgling.cookbook.connection;

import nurgling.NConfig;
import nurgling.cookbook.Recipe;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecipeLoader implements Runnable {
    private final Connection connection;
    private final ArrayList<String> recipeHashes;
    private final ArrayList<Recipe> recipes = new ArrayList<>();
    public AtomicBoolean ready = new AtomicBoolean(false);
    public RecipeLoader(Connection connection, ArrayList<String> recipeHashes) {
        this.connection = connection;
        this.recipeHashes = recipeHashes;
    }

    @Override
    public void run() {
        if (recipeHashes.isEmpty()) return;
        HashMap<String,Recipe> res = new HashMap<>();
        try {
            String sql;
            if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                sql = "SELECT r.recipe_hash, r.item_name, r.resource_name, r.hunger, r.energy, " +
                        "i.name AS ingredient_name, i.percentage, " +
                        "f.name AS fep_name, f.value AS fep_value, f.weight as fep_weight " +
                        "FROM recipes r " +
                        "LEFT JOIN ingredients i ON r.recipe_hash = i.recipe_hash " +
                        "LEFT JOIN feps f ON r.recipe_hash = f.recipe_hash " +
                        "WHERE r.recipe_hash = ANY(?)";
            } else { // SQLite
                sql = "SELECT r.recipe_hash, r.item_name, r.resource_name, r.hunger, r.energy, " +
                        "i.name AS ingredient_name, i.percentage, " +
                        "f.name AS fep_name, f.value AS fep_value, f.weight as fep_weight " +
                        "FROM recipes r " +
                        "LEFT JOIN ingredients i ON r.recipe_hash = i.recipe_hash " +
                        "LEFT JOIN feps f ON r.recipe_hash = f.recipe_hash " +
                        "WHERE r.recipe_hash IN (" + String.join(",", Collections.nCopies(recipeHashes.size(), "?")) + ")";
            }

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                    Array sqlArray = connection.createArrayOf("varchar", recipeHashes.toArray(new String[0]));
                    stmt.setArray(1, sqlArray);
                } else { // SQLite
                    for (int i = 0; i < recipeHashes.size(); i++) {
                        stmt.setString(i + 1, recipeHashes.get(i));
                    }
                }

                ResultSet rs = stmt.executeQuery();

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
                        recipe.getIngredients().put(
                                ingredientName,
                                rs.getDouble("percentage")
                        );
                    }

                    String fepName = rs.getString("fep_name");
                    if (!rs.wasNull() && fepName != null) {
                        recipe.getFeps().put(
                                fepName,
                                new Recipe.Fep(rs.getDouble("fep_value"),rs.getDouble("fep_weight"))
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading recipes:");
            e.printStackTrace();
        }
        finally
        {
            for(String hash: recipeHashes)
            {
                recipes.add(res.get(hash));
            }
            ready.set(true);
        }
    }

    public ArrayList<Recipe> getRecipes() {
        return recipes;
    }
}