package nurgling.db.migration;

import nurgling.db.DatabaseAdapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Database migration manager that handles schema updates
 */
public class MigrationManager {
    private final Connection connection;
    private final DatabaseAdapter adapter;

    public MigrationManager(Connection connection, DatabaseAdapter adapter) {
        this.connection = connection;
        this.adapter = adapter;
    }

    public void runMigrations() throws SQLException {
        boolean versionTableExists = checkVersionTableExists();
        int currentVersion = 0;

        if (versionTableExists) {
            currentVersion = getCurrentVersion();
        }

        List<Migration> migrations = getMigrations();
        System.out.println("Current schema version: " + currentVersion + ", available migrations: " + migrations.size());
        for (Migration migration : migrations) {
            if (migration.version > currentVersion) {
                System.out.println("Running migration version " + migration.version + ": " + migration.description);
                try {
                    migration.run(adapter);

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
            ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version LIMIT 1");
            rs.close();
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
        String query = adapter instanceof nurgling.db.PostgresAdapter
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
            public void run(DatabaseAdapter adapter) throws SQLException {
                // Create favorite_recipes table if it doesn't exist
                if (!adapter.tableExists("favorite_recipes")) {
                    String createFavoriteRecipes = "CREATE TABLE favorite_recipes (" +
                                                  "recipe_hash VARCHAR(64) PRIMARY KEY REFERENCES recipes (recipe_hash) ON DELETE CASCADE" +
                                                  ")";
                    adapter.executeUpdate(createFavoriteRecipes);
                    System.out.println("Created favorite_recipes table");
                }

                // Add UNIQUE constraints for ingredients and feps
                if (adapter instanceof nurgling.db.PostgresAdapter) {
                    // For PostgreSQL, add unique constraints
                    try {
                        adapter.executeUpdate("ALTER TABLE ingredients ADD CONSTRAINT ingredients_unique UNIQUE (recipe_hash, name)");
                        System.out.println("Added UNIQUE constraint to ingredients table");
                    } catch (SQLException e) {
                        if (e.getSQLState().equals("42P07") || e.getMessage().contains("already exists")) {
                            System.out.println("UNIQUE constraint on ingredients already exists");
                        } else {
                            throw e;
                        }
                    }

                    try {
                        adapter.executeUpdate("ALTER TABLE feps ADD CONSTRAINT feps_unique UNIQUE (recipe_hash, name)");
                        System.out.println("Added UNIQUE constraint to feps table");
                    } catch (SQLException e) {
                        if (e.getSQLState().equals("42P07") || e.getMessage().contains("already exists")) {
                            System.out.println("UNIQUE constraint on feps already exists");
                        } else {
                            throw e;
                        }
                    }
                } else {
                    // For SQLite, check if constraints already exist
                    ensureSqliteUniqueConstraints(adapter);
                }
            }
        });

        migrations.add(new Migration(2, "Add resource_name column to ingredients table for layered sprites") {
            @Override
            public void run(DatabaseAdapter adapter) throws SQLException {
                // Check if column already exists
                try {
                    adapter.executeQuery("SELECT resource_name FROM ingredients LIMIT 1").close();
                    System.out.println("resource_name column already exists in ingredients table");
                } catch (SQLException e) {
                    adapter.executeUpdate("ALTER TABLE ingredients ADD COLUMN resource_name VARCHAR(512)");
                    System.out.println("Added resource_name column to ingredients table");
                }
            }
        });

        migrations.add(new Migration(3, "Create areas table for shared area storage") {
            @Override
            public void run(DatabaseAdapter adapter) throws SQLException {
                // Create areas table if it doesn't exist
                if (!adapter.tableExists("areas")) {
                    String createAreasSql = "CREATE TABLE areas (" +
                            "id INTEGER PRIMARY KEY, " +
                            "name VARCHAR(255) NOT NULL, " +
                            "path VARCHAR(512) DEFAULT '', " +
                            "hide " + (adapter instanceof nurgling.db.PostgresAdapter ? "BOOLEAN" : "INTEGER") + " DEFAULT " + 
                                (adapter instanceof nurgling.db.PostgresAdapter ? "FALSE" : "0") + ", " +
                            "color_r INTEGER DEFAULT 194, " +
                            "color_g INTEGER DEFAULT 194, " +
                            "color_b INTEGER DEFAULT 65, " +
                            "color_a INTEGER DEFAULT 56, " +
                            "data TEXT NOT NULL, " +  // JSON data for space, in, out, spec
                            "profile VARCHAR(255) DEFAULT 'global', " +  // profile/genus for filtering
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")";
                    adapter.executeUpdate(createAreasSql);
                    System.out.println("Created areas table");

                    // Create index for faster profile-based queries
                    String createIndexSql = "CREATE INDEX idx_areas_profile ON areas (profile)";
                    adapter.executeUpdate(createIndexSql);
                    System.out.println("Created index on areas.profile");
                }
            }
        });

        migrations.add(new Migration(4, "Add version column to areas table") {
            @Override
            public void run(DatabaseAdapter adapter) throws SQLException {
                // Check if column already exists
                try {
                    adapter.executeQuery("SELECT version FROM areas LIMIT 1").close();
                    System.out.println("version column already exists in areas table");
                } catch (SQLException e) {
                    adapter.executeUpdate("ALTER TABLE areas ADD COLUMN version INTEGER DEFAULT 1");
                    System.out.println("Added version column to areas table");
                }
            }
        });

        return migrations;
    }

    private void ensureSqliteUniqueConstraints(DatabaseAdapter adapter) throws SQLException {
        boolean needsIngredientsMigration = checkNeedsIngredientsMigration(adapter);
        boolean needsFepsMigration = checkNeedsFepsMigration(adapter);

        if (needsIngredientsMigration) {
            recreateIngredientsTableWithConstraint(adapter);
        } else {
            System.out.println("UNIQUE constraint on ingredients already exists");
        }

        if (needsFepsMigration) {
            recreateFepsTableWithConstraint(adapter);
        } else {
            System.out.println("UNIQUE constraint on feps already exists");
        }
    }

    private boolean checkNeedsIngredientsMigration(DatabaseAdapter adapter) throws SQLException {
        try {
            adapter.executeUpdate("INSERT INTO ingredients (recipe_hash, name, percentage) VALUES ('__test__', '__test__', 0)");
            adapter.executeUpdate("INSERT INTO ingredients (recipe_hash, name, percentage) VALUES ('__test__', '__test__', 0)");
            adapter.executeUpdate("DELETE FROM ingredients WHERE recipe_hash = '__test__'");
            return true;
        } catch (SQLException e) {
            adapter.executeUpdate("DELETE FROM ingredients WHERE recipe_hash = '__test__'");
            return false;
        }
    }

    private boolean checkNeedsFepsMigration(DatabaseAdapter adapter) throws SQLException {
        try {
            adapter.executeUpdate("INSERT INTO feps (recipe_hash, name, value, weight) VALUES ('__test__', '__test__', 0, 0)");
            adapter.executeUpdate("INSERT INTO feps (recipe_hash, name, value, weight) VALUES ('__test__', '__test__', 0, 0)");
            adapter.executeUpdate("DELETE FROM feps WHERE recipe_hash = '__test__'");
            return true;
        } catch (SQLException e) {
            adapter.executeUpdate("DELETE FROM feps WHERE recipe_hash = '__test__'");
            return false;
        }
    }

    private void recreateIngredientsTableWithConstraint(DatabaseAdapter adapter) throws SQLException {
        adapter.executeUpdate("CREATE TABLE ingredients_new (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             "recipe_hash VARCHAR(64) REFERENCES recipes (recipe_hash) ON DELETE CASCADE, " +
                             "name VARCHAR(255) NOT NULL, " +
                             "percentage FLOAT NOT NULL, " +
                             "resource_name VARCHAR(512), " +
                             "UNIQUE (recipe_hash, name))");

        adapter.executeUpdate("INSERT INTO ingredients_new (recipe_hash, name, percentage, resource_name) " +
                             "SELECT recipe_hash, name, MIN(percentage), resource_name FROM ingredients " +
                             "GROUP BY recipe_hash, name");

        adapter.executeUpdate("DROP TABLE ingredients");
        adapter.executeUpdate("ALTER TABLE ingredients_new RENAME TO ingredients");
        System.out.println("Added UNIQUE constraint to ingredients table");
    }

    private void recreateFepsTableWithConstraint(DatabaseAdapter adapter) throws SQLException {
        adapter.executeUpdate("CREATE TABLE feps_new (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             "recipe_hash VARCHAR(64) REFERENCES recipes (recipe_hash) ON DELETE CASCADE, " +
                             "name VARCHAR(255) NOT NULL, " +
                             "value FLOAT NOT NULL, " +
                             "weight FLOAT NOT NULL, " +
                             "UNIQUE (recipe_hash, name))");

        adapter.executeUpdate("INSERT INTO feps_new (recipe_hash, name, value, weight) " +
                             "SELECT recipe_hash, name, MAX(value), MAX(weight) FROM feps " +
                             "GROUP BY recipe_hash, name");

        adapter.executeUpdate("DROP TABLE feps");
        adapter.executeUpdate("ALTER TABLE feps_new RENAME TO feps");
        System.out.println("Added UNIQUE constraint to feps table");
    }

    public abstract static class Migration {
        final int version;
        final String description;

        Migration(int version, String description) {
            this.version = version;
            this.description = description;
        }

        abstract void run(DatabaseAdapter adapter) throws SQLException;
    }
}
