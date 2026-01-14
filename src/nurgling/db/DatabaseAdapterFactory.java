package nurgling.db;

import nurgling.NConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Factory for creating database adapters based on configuration
 */
public class DatabaseAdapterFactory {

    /**
     * Create appropriate database adapter based on configuration
     */
    public static DatabaseAdapter createAdapter(Connection connection) throws SQLException {
        boolean isPostgres = (Boolean) NConfig.get(NConfig.Key.postgres);

        if (isPostgres) {
            return new PostgresAdapter(connection);
        } else {
            return new SqliteAdapter(connection);
        }
    }

    /**
     * Get database type as string
     */
    public static String getDatabaseType() {
        return (Boolean) NConfig.get(NConfig.Key.postgres) ? "PostgreSQL" : "SQLite";
    }

    /**
     * Check if using PostgreSQL
     */
    public static boolean isPostgres() {
        return (Boolean) NConfig.get(NConfig.Key.postgres);
    }

    /**
     * Check if using SQLite
     */
    public static boolean isSqlite() {
        return !(Boolean) NConfig.get(NConfig.Key.postgres);
    }
}
