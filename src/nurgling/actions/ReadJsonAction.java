package nurgling.actions;

import haven.ItemInfo;
import haven.Utils;
import haven.res.ui.tt.ingred.Ingredient;
import haven.resutil.FoodInfo;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.iteminfo.NFoodInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.sql.*;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class ReadJsonAction implements Action {


    @Override
    public Results run(NGameUI gui)
    {
        // Путь к JSON-файлу
        String jsonFilePath = "C://work//food-info2.json";

        try {
            // Подключение к базе данных
            Connection connection = DriverManager.getConnection("jdbc:postgresql://" + NConfig.get(NConfig.Key.serverNode) +"/nurgling_db", (String) NConfig.get(NConfig.Key.serverUser), (String) NConfig.get(NConfig.Key.serverPass));
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

    final private static String insertRecipeSQL =  "INSERT INTO recipes (recipe_hash, item_name, resource_name, hunger, energy) VALUES (?, ?, ?, ?, ?)";
    final private static String insertIngredientSQL = "INSERT INTO ingredients (recipe_hash, name, percentage) VALUES (?, ?, ?)";
    final private static String insertFepsSQL = "INSERT INTO feps (recipe_hash, name, value) VALUES (?, ?, ?)";


    private static void loadDataIntoDatabase(Connection connection, JSONArray foodItems) throws SQLException {

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
            hashInput.append(resourceName).append(energy);
            for (int j = 0; j < ingredients.length(); j++) {
                JSONObject ingredient = ingredients.getJSONObject(j);
                hashInput.append(ingredient.getString("name")).append(ingredient.getDouble("percentage"));
            }

            // Вычисляем хэш
            String recipeHash = calculateSHA256(hashInput.toString());

            // Вставляем данные в таблицу recipes
            recipeStatement.setString(1, recipeHash);
            recipeStatement.setString(2, foodItem.getString("itemName"));
            recipeStatement.setString(3, resourceName);
            recipeStatement.setDouble(4, hunger);
            recipeStatement.setInt(5, energy);

            try {
                recipeStatement.executeUpdate();
            }
            catch (org.postgresql.util.PSQLException e) {
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

