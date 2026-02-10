package nurgling.db;

import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL implementation of DatabaseAdapter
 */
public class PostgresAdapter extends DatabaseAdapter {

    public PostgresAdapter(Connection connection) {
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
        String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?)";
        try (PreparedStatement stmt = prepareStatement(sql, tableName);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() && rs.getBoolean(1);
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
        return column + " = ANY(?)";
    }

    @Override
    public void setArrayParameter(PreparedStatement stmt, int index, List<String> values) throws SQLException {
        Array sqlArray = connection.createArrayOf("varchar", values.toArray(new String[0]));
        stmt.setArray(index, sqlArray);
    }

    @Override
    public String getUpsertSql(String table, Map<String, Object> insertData, List<String> conflictColumns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");

        // Columns
        sql.append(String.join(", ", insertData.keySet()));

        sql.append(") VALUES (");

        // Placeholders
        sql.append(String.join(", ", insertData.keySet().stream()
                .map(k -> "?")
                .toArray(String[]::new)));

        sql.append(") ON CONFLICT (");
        sql.append(String.join(", ", conflictColumns));
        sql.append(") DO NOTHING");

        return sql.toString();
    }

    @Override
    public String getBatchUpsertSql(String table, List<String> columns, 
                                    List<String> conflictColumns, List<String> updateColumns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");

        // Columns
        sql.append(String.join(", ", columns));

        sql.append(") VALUES (");

        // Placeholders
        sql.append(String.join(", ", columns.stream()
                .map(k -> "?")
                .toArray(String[]::new)));

        sql.append(") ON CONFLICT (");
        sql.append(String.join(", ", conflictColumns));
        sql.append(") DO UPDATE SET ");

        // Update clause
        sql.append(String.join(", ", updateColumns.stream()
                .map(col -> col + " = EXCLUDED." + col)
                .toArray(String[]::new)));

        return sql.toString();
    }
}
