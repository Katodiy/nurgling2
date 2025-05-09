package nurgling.actions;

import haven.Utils;
import nurgling.NGameUI;
import nurgling.NUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.sql.*;
import java.util.HashSet;

public class ReadJsonAction implements Action {
    private final String path;

    public ReadJsonAction(String path) {
        this.path = path;
    }

    @Override
    public Results run(NGameUI gui) {
        try (FileReader fileReader = new FileReader(path)) {
            JSONArray foodItems = new JSONArray(new JSONTokener(fileReader));
            loadDataIntoDatabase(gui.ui.core.poolManager.connection, foodItems);
            System.out.println("Data imported successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return Results.ERROR(e.getMessage());
        }
        return Results.SUCCESS();
    }

    private static final String INSERT_RECIPE_SQL =
            "INSERT OR IGNORE INTO recipes (recipe_hash, item_name, resource_name, hunger, energy) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_INGREDIENT_SQL =
            "INSERT OR IGNORE INTO ingredients (recipe_hash, name, percentage) VALUES (?, ?, ?)";
    private static final String INSERT_FEPS_SQL =
            "INSERT OR IGNORE INTO feps (recipe_hash, name, value, weight) VALUES (?, ?, ?, ?)";

    private static void loadDataIntoDatabase(Connection connection, JSONArray foodItems) throws SQLException {
        HashSet<String> existingHashes = getExistingRecipeHashes(connection);

        connection.setAutoCommit(false);

        try (PreparedStatement recipeStmt = connection.prepareStatement(INSERT_RECIPE_SQL);
             PreparedStatement ingredientStmt = connection.prepareStatement(INSERT_INGREDIENT_SQL);
             PreparedStatement fepsStmt = connection.prepareStatement(INSERT_FEPS_SQL)) {

            final int BATCH_SIZE = 100;
            int batchCounter = 0;

            for (int i = 0; i < foodItems.length(); i++) {
                JSONObject foodItem = foodItems.getJSONObject(i);
                String resourceName = foodItem.getString("resourceName");
                String itemName = foodItem.getString("itemName");

                if (itemName.indexOf('\u0000') >= 0) continue;

                String recipeHash = generateRecipeHash(foodItem);
                if (existingHashes.contains(recipeHash)) continue;

                // Добавляем рецепт
                recipeStmt.setString(1, recipeHash);
                recipeStmt.setString(2, itemName);
                recipeStmt.setString(3, resourceName);
                recipeStmt.setDouble(4, foodItem.getDouble("hunger"));
                recipeStmt.setInt(5, foodItem.getInt("energy"));
                recipeStmt.addBatch();

                // Добавляем ингредиенты
                JSONArray ingredients = foodItem.getJSONArray("ingredients");
                for (int j = 0; j < ingredients.length(); j++) {
                    JSONObject ingredient = ingredients.getJSONObject(j);
                    ingredientStmt.setString(1, recipeHash);
                    ingredientStmt.setString(2, ingredient.getString("name"));
                    ingredientStmt.setDouble(3, ingredient.getDouble("percentage"));
                    ingredientStmt.addBatch();
                }

                // Добавляем FEPS
                JSONArray feps = foodItem.getJSONArray("feps");
                double sum = calculateFepsSum(feps);
                for (int j = 0; j < feps.length(); j++) {
                    JSONObject fep = feps.getJSONObject(j);
                    fepsStmt.setString(1, recipeHash);
                    fepsStmt.setString(2, fep.getString("name"));
                    fepsStmt.setDouble(3, fep.getDouble("value"));
                    fepsStmt.setDouble(4, sum > 0 ? fep.getDouble("value") / sum : 0);
                    fepsStmt.addBatch();
                }

                if (++batchCounter % BATCH_SIZE == 0) {
                    executeBatch(connection, recipeStmt, ingredientStmt, fepsStmt);
                }
            }

            executeBatch(connection, recipeStmt, ingredientStmt, fepsStmt);
            connection.commit();
        }
    }

    private static HashSet<String> getExistingRecipeHashes(Connection connection) throws SQLException {
        HashSet<String> hashes = new HashSet<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT recipe_hash FROM recipes")) {
            while (rs.next()) {
                hashes.add(rs.getString("recipe_hash"));
            }
        }
        return hashes;
    }

    private static String generateRecipeHash(JSONObject foodItem) {
        StringBuilder hashInput = new StringBuilder();
        hashInput.append(foodItem.getString("resourceName"))
                .append(foodItem.getInt("energy"));

        JSONArray ingredients = foodItem.getJSONArray("ingredients");
        for (int i = 0; i < ingredients.length(); i++) {
            JSONObject ingredient = ingredients.getJSONObject(i);
            hashInput.append(ingredient.getString("name"))
                    .append(ingredient.getDouble("percentage"));
        }

        return NUtils.calculateSHA256(hashInput.toString());
    }

    private static double calculateFepsSum(JSONArray feps) {
        double sum = 0;
        for (int i = 0; i < feps.length(); i++) {
            sum += feps.getJSONObject(i).getDouble("value");
        }
        return sum;
    }

    private static void executeBatch(Connection connection, PreparedStatement... statements) throws SQLException {
        try {
            for (PreparedStatement stmt : statements) {
                stmt.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
}