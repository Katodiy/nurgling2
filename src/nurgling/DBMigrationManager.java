package nurgling;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DBMigrationManager {
    private final Connection connection;
    private final boolean isPostgres;

    public DBMigrationManager(Connection connection) {
        this.connection = connection;
        this.isPostgres = (Boolean) NConfig.get(NConfig.Key.postgres);
    }

    public void runMigrations() throws SQLException {
        boolean versionTableExists = checkVersionTableExists();
        int currentVersion = 0;
        
        if (versionTableExists) {
            currentVersion = getCurrentVersion();
        }
        
        List<Migration> migrations = getMigrations();
        for (Migration migration : migrations) {
            if (migration.version > currentVersion) {
                System.out.println("Running migration version " + migration.version + ": " + migration.description);
                try {
                    migration.run(connection, isPostgres);
                    
                    // Create version table if it doesn't exist yet (after first migration)
                    if (!versionTableExists) {
                        ensureVersionTableExists();
                        versionTableExists = true;
                    }
                    
                    updateVersion(migration.version);
                    connection.commit();
                    System.out.println("Migration " + migration.version + " completed successfully");
                } catch (SQLException e) {
                    connection.rollback();
                    System.err.println("Migration " + migration.version + " failed: " + e.getMessage());
                    throw e;
                }
            }
        }
    }

    private boolean checkVersionTableExists() {
        try {
            Statement stmt = connection.createStatement();
            stmt.executeQuery("SELECT version FROM schema_version LIMIT 1").close();
            stmt.close();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                // Ignore rollback errors
            }
            return false;
        }
    }
    
    private void ensureVersionTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE schema_version (" +
                                 "version INTEGER PRIMARY KEY, " +
                                 "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                 ")";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(createTableQuery);
        stmt.close();
        System.out.println("Created schema_version table");
    }

    private int getCurrentVersion() throws SQLException {
        String query = "SELECT MAX(version) as max_version FROM schema_version";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                int version = rs.getInt("max_version");
                return rs.wasNull() ? 0 : version;
            }
        }
        return 0;
    }

    private void updateVersion(int version) throws SQLException {
        String query = isPostgres
            ? "INSERT INTO schema_version (version) VALUES (" + version + ")"
            : "INSERT INTO schema_version (version) VALUES (" + version + ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    private List<Migration> getMigrations() {
        List<Migration> migrations = new ArrayList<>();
        
        migrations.add(new Migration(1, "Initial migration: create favorite_recipes table and add UNIQUE constraints") {
            @Override
            public void run(Connection conn, boolean isPostgres) throws SQLException {
                Statement stmt = conn.createStatement();
                
                // Create favorite_recipes table if it doesn't exist
                boolean favoriteTableExists = false;
                try {
                    stmt.executeQuery("SELECT 1 FROM favorite_recipes LIMIT 1").close();
                    favoriteTableExists = true;
                    System.out.println("favorite_recipes table already exists");
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        // Ignore
                    }
                }
                
                if (!favoriteTableExists) {
                    String createFavoriteRecipes = "CREATE TABLE favorite_recipes (" +
                                                  "recipe_hash VARCHAR(64) PRIMARY KEY REFERENCES recipes (recipe_hash) ON DELETE CASCADE" +
                                                  ")";
                    stmt.executeUpdate(createFavoriteRecipes);
                    System.out.println("Created favorite_recipes table");
                }
                
                // Add UNIQUE constraints for ingredients and feps
                if (isPostgres) {
                    // For PostgreSQL, add unique constraints
                    try {
                        stmt.executeUpdate("ALTER TABLE ingredients ADD CONSTRAINT ingredients_unique UNIQUE (recipe_hash, name)");
                        System.out.println("Added UNIQUE constraint to ingredients table");
                    } catch (SQLException e) {
                        // Constraint might already exist
                        if (e.getSQLState().equals("42P07") || e.getMessage().contains("already exists")) {
                            System.out.println("UNIQUE constraint on ingredients already exists");
                        } else {
                            throw e;
                        }
                    }
                    
                    try {
                        stmt.executeUpdate("ALTER TABLE feps ADD CONSTRAINT feps_unique UNIQUE (recipe_hash, name)");
                        System.out.println("Added UNIQUE constraint to feps table");
                    } catch (SQLException e) {
                        // Constraint might already exist
                        if (e.getSQLState().equals("42P07") || e.getMessage().contains("already exists")) {
                            System.out.println("UNIQUE constraint on feps already exists");
                        } else {
                            throw e;
                        }
                    }
                } else {
                    // For SQLite, check if constraints already exist
                    boolean needsIngredientsMigration = false;
                    boolean needsFepsMigration = false;
                    
                    try {
                        stmt.executeUpdate("INSERT INTO ingredients (recipe_hash, name, percentage) VALUES ('__test__', '__test__', 0)");
                        stmt.executeUpdate("INSERT INTO ingredients (recipe_hash, name, percentage) VALUES ('__test__', '__test__', 0)");
                        // If we got here, constraint doesn't exist
                        needsIngredientsMigration = true;
                        stmt.executeUpdate("DELETE FROM ingredients WHERE recipe_hash = '__test__'");
                    } catch (SQLException e) {
                        // Constraint exists
                        conn.rollback();
                        System.out.println("UNIQUE constraint on ingredients already exists");
                    }
                    
                    try {
                        stmt.executeUpdate("INSERT INTO feps (recipe_hash, name, value, weight) VALUES ('__test__', '__test__', 0, 0)");
                        stmt.executeUpdate("INSERT INTO feps (recipe_hash, name, value, weight) VALUES ('__test__', '__test__', 0, 0)");
                        // If we got here, constraint doesn't exist
                        needsFepsMigration = true;
                        stmt.executeUpdate("DELETE FROM feps WHERE recipe_hash = '__test__'");
                    } catch (SQLException e) {
                        // Constraint exists
                        conn.rollback();
                        System.out.println("UNIQUE constraint on feps already exists");
                    }
                    
                    if (needsIngredientsMigration) {
                        // Recreate ingredients table with unique constraint
                        stmt.executeUpdate("CREATE TABLE ingredients_new (" +
                                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                         "recipe_hash VARCHAR(64) REFERENCES recipes (recipe_hash) ON DELETE CASCADE, " +
                                         "name VARCHAR(255) NOT NULL, " +
                                         "percentage FLOAT NOT NULL, " +
                                         "UNIQUE (recipe_hash, name))");
                        
                        stmt.executeUpdate("INSERT INTO ingredients_new (recipe_hash, name, percentage) " +
                                         "SELECT recipe_hash, name, MIN(percentage) FROM ingredients " +
                                         "GROUP BY recipe_hash, name");
                        
                        stmt.executeUpdate("DROP TABLE ingredients");
                        stmt.executeUpdate("ALTER TABLE ingredients_new RENAME TO ingredients");
                        System.out.println("Added UNIQUE constraint to ingredients table");
                    }
                    
                    if (needsFepsMigration) {
                        // Recreate feps table with unique constraint
                        stmt.executeUpdate("CREATE TABLE feps_new (" +
                                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                         "recipe_hash VARCHAR(64) REFERENCES recipes (recipe_hash) ON DELETE CASCADE, " +
                                         "name VARCHAR(255) NOT NULL, " +
                                         "value FLOAT NOT NULL, " +
                                         "weight FLOAT NOT NULL, " +
                                         "UNIQUE (recipe_hash, name))");
                        
                        stmt.executeUpdate("INSERT INTO feps_new (recipe_hash, name, value, weight) " +
                                         "SELECT recipe_hash, name, MAX(value), MAX(weight) FROM feps " +
                                         "GROUP BY recipe_hash, name");
                        
                        stmt.executeUpdate("DROP TABLE feps");
                        stmt.executeUpdate("ALTER TABLE feps_new RENAME TO feps");
                        System.out.println("Added UNIQUE constraint to feps table");
                    }
                }
                
                stmt.close();
            }
        });
        
        migrations.add(new Migration(2, "Add resource_name column to ingredients table for layered sprites") {
            @Override
            public void run(Connection conn, boolean isPostgres) throws SQLException {
                Statement stmt = conn.createStatement();
                
                // Check if column already exists
                boolean columnExists = false;
                try {
                    stmt.executeQuery("SELECT resource_name FROM ingredients LIMIT 1").close();
                    columnExists = true;
                    System.out.println("resource_name column already exists in ingredients table");
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        // Ignore
                    }
                }
                
                if (!columnExists) {
                    stmt.executeUpdate("ALTER TABLE ingredients ADD COLUMN resource_name VARCHAR(512)");
                    System.out.println("Added resource_name column to ingredients table");
                }
                
                stmt.close();
            }
        });
        
        return migrations;
    }

    abstract static class Migration {
        final int version;
        final String description;

        Migration(int version, String description) {
            this.version = version;
            this.description = description;
        }

        abstract void run(Connection conn, boolean isPostgres) throws SQLException;
    }
}
