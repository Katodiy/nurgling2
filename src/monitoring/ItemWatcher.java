package monitoring;

import haven.Coord;
import haven.Utils;
import nurgling.DBPoolManager;
import nurgling.NUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ItemWatcher implements Runnable {

    public static class ItemInfo {
        String name;
        double q = -1;
        Coord c;
        String container;

        public ItemInfo(String name, double q, Coord c, String container) {
            this.name = name;
            this.q = Double.parseDouble(Utils.odformat2(q, 2));
            this.c = c;
            this.container = container;
        }
    }

    private final DBPoolManager poolManager;
    private final ArrayList<ItemInfo> iis;

    public ItemWatcher(ArrayList<ItemInfo> iis, DBPoolManager poolManager) {
        this.iis = iis;
        this.poolManager = poolManager;
    }

    @Override
    public void run() {
        if (iis == null || iis.isEmpty()) {
            return;
        }

        Connection conn = null;
        try {
            conn = poolManager.getConnection();
            if (conn == null) {
                System.err.println("ItemWatcher: Unable to get database connection");
                return;
            }

            deleteItems(conn);
            insertItems(conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                poolManager.returnConnection(conn);
            }
        }
    }

    private void deleteItems(Connection conn) throws SQLException {
        // Build parameterized IN clause: DELETE ... WHERE ... NOT IN (?, ?, ?, ...)
        String placeholders = iis.stream().map(i -> "?").collect(Collectors.joining(","));
        String deleteSql = "DELETE FROM storageitems WHERE container = ? AND item_hash NOT IN (" + placeholders + ")";

        try (PreparedStatement deleteStatement = conn.prepareStatement(deleteSql)) {
            deleteStatement.setString(1, iis.get(0).container);

            // Set each item hash as a separate parameter
            int paramIndex = 2;
            for (ItemInfo item : iis) {
                deleteStatement.setString(paramIndex++, generateItemHash(item));
            }

            deleteStatement.executeUpdate();
        }
    }

    private void insertItems(Connection conn) throws SQLException {
        final String insertSql = "INSERT INTO storageitems (item_hash, name, quality, coordinates, container) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (item_hash) DO UPDATE SET " +
                "name = EXCLUDED.name, quality = EXCLUDED.quality, coordinates = EXCLUDED.coordinates";

        try (PreparedStatement insertStatement = conn.prepareStatement(insertSql)) {
            for (ItemInfo item : iis) {
                String itemHash = generateItemHash(item);

                insertStatement.setString(1, itemHash);
                insertStatement.setString(2, item.name);
                insertStatement.setDouble(3, item.q);
                insertStatement.setString(4, item.c.toString());
                insertStatement.setString(5, item.container);

                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private String generateItemHash(ItemInfo item) {
        String data = item.name + item.c.toString() + item.q;
        return NUtils.calculateSHA256(data);
    }
}
