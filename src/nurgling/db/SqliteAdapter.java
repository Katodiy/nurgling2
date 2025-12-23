package nurgling.db;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQLite implementation of DatabaseAdapter
 */
public class SqliteAdapter extends DatabaseAdapter {

    public SqliteAdapter(Connection connection) {
        super(connection);
    }

    @Override
    public ResultSet executeQuery(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = prepareStatement(sql, params);
        ResultSet rs = stmt.executeQuery();
        // Wrap the ResultSet to close the statement when the ResultSet is closed
        return new StatementClosingResultSet(rs, stmt);
    }

    @Override
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(sql, params)) {
            return stmt.executeUpdate();
        }
    }

    @Override
    public int[] executeBatch(String sql, List<Object[]> paramList) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Object[] params : paramList) {
                setParameters(stmt, params);
                stmt.addBatch();
            }
            return stmt.executeBatch();
        }
    }

    @Override
    public boolean tableExists(String tableName) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement stmt = prepareStatement(sql, tableName);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        }
    }

    @Override
    public String getLimitOffsetSql(String sql, int limit, int offset) {
        StringBuilder sb = new StringBuilder(sql);
        if (limit > 0) {
            sb.append(" LIMIT ").append(limit);
        }
        if (offset > 0) {
            sb.append(" OFFSET ").append(offset);
        }
        return sb.toString();
    }

    @Override
    public String getArrayInSql(String column, List<String> values) {
        return column + " IN (" + String.join(",", values.stream()
                .map(v -> "?")
                .collect(Collectors.toList())) + ")";
    }

    @Override
    public void setArrayParameter(PreparedStatement stmt, int index, List<String> values) throws SQLException {
        // For SQLite, we expand the IN clause with multiple parameters
        // This method is not used for IN clauses in SQLite - parameters are set individually
        throw new UnsupportedOperationException("Use individual parameters for SQLite IN clauses");
    }

    @Override
    public String getUpsertSql(String table, Map<String, Object> insertData, List<String> conflictColumns) {
        StringBuilder sql = new StringBuilder("INSERT OR IGNORE INTO ").append(table).append(" (");

        // Columns
        sql.append(String.join(", ", insertData.keySet()));

        sql.append(") VALUES (");

        // Placeholders
        sql.append(String.join(", ", insertData.keySet().stream()
                .map(k -> "?")
                .toArray(String[]::new)));

        sql.append(")");

        return sql.toString();
    }

    @Override
    public String getBatchUpsertSql(String table, List<String> columns, 
                                    List<String> conflictColumns, List<String> updateColumns) {
        // SQLite uses INSERT OR REPLACE for upsert behavior
        StringBuilder sql = new StringBuilder("INSERT OR REPLACE INTO ").append(table).append(" (");

        // Columns
        sql.append(String.join(", ", columns));

        sql.append(") VALUES (");

        // Placeholders
        sql.append(String.join(", ", columns.stream()
                .map(k -> "?")
                .toArray(String[]::new)));

        sql.append(")");

        return sql.toString();
    }
}
