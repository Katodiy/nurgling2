package nurgling.cookbook.connection;

import nurgling.NConfig;
import nurgling.cookbook.Recipe;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeHashFetcher implements Runnable {
    private final Connection connection;
    private ArrayList<Recipe> recipes;  // Теперь храним сразу рецепты
    public AtomicBoolean ready = new AtomicBoolean(false);
    private String sql;

    public RecipeHashFetcher(Connection connection, String sql) {
        this.connection = connection;
        this.recipes = new ArrayList<>();
        this.sql = sql;
    }

    public void run() {
        try {
            String query;
            if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                query = "SELECT " +
                        "r.recipe_hash, r.item_name, r.resource_name, r.hunger, r.energy, " +
                        "f.name as fep_name, f.value as fep_value, f.weight as fep_weight, " +
                        "i.name as ing_name, i.percentage as ing_percentage " +
                        "FROM recipes r " +
                        "LEFT JOIN feps f ON r.recipe_hash = f.recipe_hash " +
                        "LEFT JOIN ingredients i ON r.recipe_hash = i.recipe_hash " +
                        "WHERE " + extractWhereClause(sql);
            } else { // SQLite
                query = "SELECT " +
                        "r.recipe_hash, r.item_name, r.resource_name, r.hunger, r.energy, " +
                        "f.name as fep_name, f.value as fep_value, f.weight as fep_weight, " +
                        "i.name as ing_name, i.percentage as ing_percentage " +
                        "FROM recipes r " +
                        "LEFT JOIN feps f ON r.recipe_hash = f.recipe_hash " +
                        "LEFT JOIN ingredients i ON r.recipe_hash = i.recipe_hash " +
                        "WHERE " + extractWhereClause(sql);
            }

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            Map<String, Recipe> recipeMap = new HashMap<>();

            while (rs.next()) {
                String hash = rs.getString("recipe_hash");

                Recipe recipe = recipeMap.computeIfAbsent(hash, k -> {
                    try {
                        return new Recipe(
                                hash,
                                rs.getString("item_name"),
                                rs.getString("resource_name"),
                                rs.getDouble("hunger"),
                                rs.getInt("energy"),
                                new HashMap<>(), // Ингредиенты
                                new HashMap<>()   // FEPS
                        );
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

                // Добавляем FEP если есть
                String fepName = rs.getString("fep_name");
                if (fepName != null && !recipe.getFeps().containsKey(fepName)) {
                    recipe.getFeps().put(fepName,
                            new Recipe.Fep(
                                    rs.getDouble("fep_value"),
                                    rs.getDouble("fep_weight")
                            ));
                }

                // Добавляем ингредиент если есть
                String ingName = rs.getString("ing_name");
                if (ingName != null && !recipe.getIngredients().containsKey(ingName)) {
                    recipe.getIngredients().put(
                            ingName,
                            rs.getDouble("ing_percentage")
                    );
                }
            }

            recipes = new ArrayList<>(recipeMap.values());
            System.out.println("Successfully fetched " + recipes.size() +
                    " recipes with FEPS and ingredients");
        } catch (SQLException e) {
            System.err.println("Error fetching recipes:");
            e.printStackTrace();
        } finally {
            ready.set(true);
        }
    }

    private String extractWhereClause(String inputSql) {
        if ((Boolean) NConfig.get(NConfig.Key.sqlite)) {
            inputSql = inputSql.replace("ILIKE", "LIKE");
        }

        // Если строка уже содержит SQL-ключевые слова (старый формат)
        if (inputSql.toLowerCase().contains("where") ||
                inputSql.toLowerCase().contains("join") ||
                inputSql.toLowerCase().contains("order by")) {
            return ((Boolean) NConfig.get(NConfig.Key.sqlite))?extractWhereFromSql(inputSql).replace("ILIKE", "LIKE"):extractWhereFromSql(inputSql);
        }
        // Если строка использует новый фильтрующий синтаксис
        else {
            return ((Boolean) NConfig.get(NConfig.Key.sqlite))?parseFilterSyntax(inputSql).replace("ILIKE", "LIKE"):parseFilterSyntax(inputSql);
        }
    }

    private String extractWhereFromSql(String sql) {
        String lowerSql = sql.toLowerCase();
        int wherePos = lowerSql.indexOf("where");
        if (wherePos >= 0) {
            int orderByPos = lowerSql.indexOf("order by");
            if (orderByPos > wherePos) {
                return sql.substring(wherePos + 5, orderByPos).trim();
            }
            return sql.substring(wherePos + 5).trim();
        }
        return "1=1";
    }

    private String parseFilterSyntax(String filterString) {
        if (filterString == null || filterString.trim().isEmpty()) {
            return "1=1";
        }

        List<String> conditions = new ArrayList<>();
        String[] parts = filterString.split(";");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            try {
                // Обработка фильтров по имени
                if (part.startsWith("name:")) {
                    String value = part.substring(5).trim();
                    boolean exact = value.startsWith("\"") && value.endsWith("\"");
                    boolean exclude = part.startsWith("-name:");

                    if (exact) {
                        value = value.substring(1, value.length() - 1);
                        conditions.add(exclude ?
                                "LOWER(r.item_name) != LOWER('" + escapeSql(value) + "')" :
                                "LOWER(r.item_name) = LOWER('" + escapeSql(value) + "')");
                    } else {
                        conditions.add(exclude ?
                                "r.item_name NOT ILIKE '%" + escapeSql(value) + "%'" :
                                "r.item_name ILIKE '%" + escapeSql(value) + "%'");
                    }
                }
                // Обработка фильтров по ингредиентам
                else if (part.startsWith("from:") || part.startsWith("-from:")) {
                    boolean exclude = part.startsWith("-from:");
                    String value = part.substring(exclude?6:5).trim();
                    boolean exact = value.startsWith("\"") && value.endsWith("\"");

                    if (exact) {
                        value = value.substring(1, value.length() - 1);
                        conditions.add(exclude ?
                                "NOT EXISTS (SELECT 1 FROM ingredients i WHERE i.recipe_hash = r.recipe_hash AND LOWER(i.name) = LOWER('" + escapeSql(value) + "'))" :
                                "EXISTS (SELECT 1 FROM ingredients i WHERE i.recipe_hash = r.recipe_hash AND LOWER(i.name) = LOWER('" + escapeSql(value) + "'))");
                    } else {
                        conditions.add(exclude ?
                                "NOT EXISTS (SELECT 1 FROM ingredients i WHERE i.recipe_hash = r.recipe_hash AND i.name ILIKE '%" + escapeSql(value) + "%')" :
                                "EXISTS (SELECT 1 FROM ingredients i WHERE i.recipe_hash = r.recipe_hash AND i.name ILIKE '%" + escapeSql(value) + "%')");
                    }
                }
                // Обработка фильтров по FEP
                else if (part.matches("(str|agi|con|int|dex|per|wil|psy|cha)(2?)\\s*([<>]=?|=)\\s*(\\d+)(%?)")) {
                    Matcher m = Pattern.compile("(str|agi|con|int|dex|per|wil|psy|cha)(2?)\\s*([<>]=?|=)\\s*(\\d+)(%?)").matcher(part);
                    if (m.find()) {
                        String fepType = mapFepType(m.group(1));
                        String fepLevel = m.group(2).isEmpty() ? "1" : "2";
                        String operator = m.group(3);
                        String value = m.group(4);
                        boolean isPercentage = !m.group(5).isEmpty();

                        String fepName = fepType + " +" + fepLevel;

                        if (isPercentage) {
                            conditions.add(String.format(
                                    "r.recipe_hash IN (" +
                                            "SELECT r2.recipe_hash FROM recipes r2 " +
                                            "JOIN feps f2 ON r2.recipe_hash = f2.recipe_hash " +
                                            "GROUP BY r2.recipe_hash " +
                                            "HAVING COALESCE(SUM(CASE WHEN f2.name = '%s' THEN f2.value ELSE 0 END), 0) / " +
                                            "NULLIF(SUM(f2.value), 0) * 100 %s %s" +
                                            ")", fepName, operator, value));
                        } else {
                            conditions.add(String.format(
                                    "EXISTS (SELECT 1 FROM feps f WHERE f.recipe_hash = r.recipe_hash AND f.name = '%s' AND f.value %s %s)",
                                    fepName, operator, value));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing filter condition: " + part);
            }
        }

        return conditions.isEmpty() ? "1=1" : String.join(" AND ", conditions);
    }

    private String mapFepType(String shortType) {
        switch (shortType) {
            case "str": return "Strength";
            case "agi": return "Agility";
            case "con": return "Constitution";
            case "int": return "Intelligence";
            case "dex": return "Dexterity";
            case "per": return "Perception";
            case "wil": return "Will";
            case "psy": return "Psyche";
            case "cha": return "Charisma";
            default: return shortType;
        }
    }

    private String escapeSql(String input) {
        return input.replace("'", "''");
    }

    public static String genFep(String type, boolean desc) {
        if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
            return "FROM recipes r " +
                    "LEFT JOIN feps f ON r.recipe_hash = f.recipe_hash AND f.name = '" + type + "' " +
                    "GROUP BY r.recipe_hash, f.name, r.resource_name, r.hunger, r.energy, f.value " +
                    "ORDER BY COALESCE(f.value, 0) " + (desc ? "DESC" : "ASC");
        } else { // SQLite
            return "FROM recipes r " +
                    "LEFT JOIN feps f ON r.recipe_hash = f.recipe_hash AND f.name = '" + type + "' " +
                    "GROUP BY r.recipe_hash, r.resource_name, r.hunger, r.energy " +
                    "ORDER BY IFNULL(f.value, 0) " + (desc ? "DESC" : "ASC");
        }
    }

    public ArrayList<Recipe> getRecipes() {
        return recipes;
    }
}