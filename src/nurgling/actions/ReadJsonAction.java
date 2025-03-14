package nurgling.actions;

import nurgling.NGameUI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.sql.*;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class ReadJsonAction implements Action {

    // JDBC URL, username и password для подключения к PostgreSQL
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5436/nurgling_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "postgres";

    @Override
    public Results run(NGameUI gui)
    {
        // Путь к JSON-файлу
        String jsonFilePath = "C://work//food-info2.json";

        try {
            // Подключение к базе данных
            Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);

            // Чтение JSON-файла
            FileReader fileReader = new FileReader(jsonFilePath);
            JSONTokener tokener = new JSONTokener(fileReader);
            JSONArray foodItems = new JSONArray(tokener);

            // Загрузка данных в базу
            loadDataIntoDatabase(connection, foodItems);

            // Закрытие соединения
            connection.close();
            System.out.println("Данные успешно загружены в базу данных.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Results.SUCCESS();
    }

    private static void loadDataIntoDatabase(Connection connection, JSONArray foodItems) throws SQLException {
        String insertRecipeSQL = "INSERT INTO recipes (item_name, resource_name, hunger, energy, recipe_hash) VALUES (?, ?, ?, ?, ?) RETURNING id";
        String insertIngredientSQL = "INSERT INTO ingredients (recipe_id, name, percentage) VALUES (?, ?, ?)";
        String insertFepsSQL = "INSERT INTO feps (recipe_id, name, value) VALUES (?, ?, ?)";

        PreparedStatement recipeStatement = connection.prepareStatement(insertRecipeSQL);
        PreparedStatement ingredientStatement = connection.prepareStatement(insertIngredientSQL);
        PreparedStatement fepsStatement = connection.prepareStatement(insertFepsSQL);

        for (int i = 0; i < foodItems.length(); i++) {
            JSONObject foodItem = foodItems.getJSONObject(i);

            // Получаем данные для хэширования
            String resourceName = foodItem.getString("resourceName");
            double hunger = foodItem.getDouble("hunger");
            int energy = foodItem.getInt("energy");
            JSONArray ingredients = foodItem.getJSONArray("ingredients");

            // Создаем строку для хэширования
            StringBuilder hashInput = new StringBuilder();
            hashInput.append(resourceName).append(hunger).append(energy);
            for (int j = 0; j < ingredients.length(); j++) {
                JSONObject ingredient = ingredients.getJSONObject(j);
                hashInput.append(ingredient.getString("name")).append(ingredient.getDouble("percentage"));
            }

            // Вычисляем хэш
            String recipeHash = calculateSHA256(hashInput.toString());

            // Вставляем данные в таблицу recipes
            recipeStatement.setString(1, foodItem.getString("itemName"));
            recipeStatement.setString(2, resourceName);
            recipeStatement.setDouble(3, hunger);
            recipeStatement.setInt(4, energy);
            recipeStatement.setString(5, recipeHash);

            try {
                recipeStatement.execute();
            }
            catch (org.postgresql.util.PSQLException e)
            {
                System.out.println(foodItem.toString());
                continue;
            }

            ResultSet resultSet = recipeStatement.getResultSet();
            int recipeId = -1;
            if (resultSet.next()) {
                recipeId = resultSet.getInt(1);
            }

            // Вставляем ингредиенты
            for (int j = 0; j < ingredients.length(); j++) {
                JSONObject ingredient = ingredients.getJSONObject(j);
                ingredientStatement.setInt(1, recipeId);
                ingredientStatement.setString(2, ingredient.getString("name"));
                ingredientStatement.setDouble(3, ingredient.getDouble("percentage"));
                ingredientStatement.executeUpdate();
            }

            // Вставляем эффекты (FEPS)
            JSONArray feps = foodItem.getJSONArray("feps");
            for (int j = 0; j < feps.length(); j++) {
                JSONObject fep = feps.getJSONObject(j);
                fepsStatement.setInt(1, recipeId);
                fepsStatement.setString(2, fep.getString("name"));
                fepsStatement.setDouble(3, fep.getDouble("value"));
                fepsStatement.executeUpdate();
            }
        }

        recipeStatement.close();
        ingredientStatement.close();
        fepsStatement.close();
    }

    private static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при вычислении хэша SHA-256", e);
        }
    }
}

