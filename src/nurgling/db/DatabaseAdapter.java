package nurgling.db;

import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for database operations.
 * Provides unified interface for SQLite and PostgreSQL databases.
 */
public abstract class DatabaseAdapter {
    protected final Connection connection;

    protected DatabaseAdapter(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get database connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Execute query and return result set
     */
    public abstract ResultSet executeQuery(String sql, Object... params) throws SQLException;

    /**
     * Execute update query (INSERT, UPDATE, DELETE)
     */
    public abstract int executeUpdate(String sql, Object... params) throws SQLException;

    /**
     * Execute batch update
     */
    public abstract int[] executeBatch(String sql, List<Object[]> paramList) throws SQLException;

    /**
     * Check if table exists
     */
    public abstract boolean tableExists(String tableName) throws SQLException;

    /**
     * Get database-specific SQL for LIMIT/OFFSET
     */
    public abstract String getLimitOffsetSql(String sql, int limit, int offset);

    /**
     * Get database-specific SQL for array operations
     */
    public abstract String getArrayInSql(String column, List<String> values);

    /**
     * Set array parameter for prepared statement
     */
    public abstract void setArrayParameter(PreparedStatement stmt, int index, List<String> values) throws SQLException;

    /**
     * Get upsert SQL (INSERT ... ON CONFLICT)
     */
    public abstract String getUpsertSql(String table, Map<String, Object> insertData, List<String> conflictColumns);

    /**
     * Get batch upsert SQL (INSERT ... ON CONFLICT DO UPDATE)
     * @param table Table name
     * @param columns Column names for insert
     * @param conflictColumns Columns that form the unique constraint
     * @param updateColumns Columns to update on conflict
     * @return SQL string with placeholders
     */
    public abstract String getBatchUpsertSql(String table, List<String> columns, 
                                             List<String> conflictColumns, List<String> updateColumns);

    /**
     * Create prepared statement with parameters
     */
    protected PreparedStatement prepareStatement(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt;
    }

    /**
     * Set parameters for prepared statement
     */
    protected void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                stmt.setNull(i + 1, java.sql.Types.NULL);
            } else if (param instanceof Double) {
                stmt.setDouble(i + 1, (Double) param);
            } else if (param instanceof Float) {
                stmt.setFloat(i + 1, (Float) param);
            } else if (param instanceof Integer) {
                stmt.setInt(i + 1, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(i + 1, (Long) param);
            } else if (param instanceof Boolean) {
                stmt.setBoolean(i + 1, (Boolean) param);
            } else if (param instanceof String) {
                stmt.setString(i + 1, (String) param);
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }

    /**
     * Close result set quietly
     */
    protected void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
        }
    }

    /**
     * Close statement quietly
     */
    protected void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ignore) {
            }
        }
    }
}
