package monitoring;

import haven.Coord;
import haven.Utils;
import nurgling.NUtils;

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
            this.q = Double.parseDouble(Utils.odformat2(q,2));
            this.c = c;
            this.container = container;
        }
    }

    public java.sql.Connection connection;
    private final ArrayList<ItemInfo> iis;

    public ItemWatcher(ArrayList<ItemInfo> iis)
    {
        this.iis = iis;
    }

    @Override
    public void run() {
        if (iis == null || iis.isEmpty()) {
            return;
        }

        // Проверяем соединение перед использованием
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.err.println("ItemWatcher: Connection is not valid, skipping write");
                return;
            }
        } catch (SQLException e) {
            System.err.println("ItemWatcher: Failed to validate connection: " + e.getMessage());
            return;
        }

        try {
            deleteItems();
            insertItems();
            connection.commit();
        } catch (SQLException e) {
            // Проверяем, является ли это ошибкой соединения
            if (isConnectionError(e)) {
                System.err.println("ItemWatcher: Database connection lost, data not saved");
                return;
            }
            rollback();
            e.printStackTrace();
        }
    }

    private void deleteItems() throws SQLException {
        String deleteSql = "DELETE FROM storageitems WHERE container = ? AND item_hash NOT IN (?)";
        String inClause = iis.stream().map(i -> generateItemHash(i)).collect(Collectors.joining(","));

        try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
            deleteStatement.setString(1, iis.get(0).container);
            deleteStatement.setString(2, inClause);
            deleteStatement.executeUpdate();
        }
    }

    private void insertItems() throws SQLException {
        final String insertSql = "INSERT INTO storageitems (item_hash, name, quality, coordinates, container) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (item_hash) DO UPDATE SET " +
                "name = EXCLUDED.name, quality = EXCLUDED.quality, coordinates = EXCLUDED.coordinates";

        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
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

    private void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException ignore) {
        }
    }
    
    private boolean isConnectionError(SQLException e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("connection has been closed") || 
                           msg.contains("I/O error") ||
                           msg.contains("Connection refused") ||
                           msg.contains("Connection reset"))) {
            return true;
        }
        // PostgreSQL connection error states
        String sqlState = e.getSQLState();
        if (sqlState != null && (sqlState.startsWith("08") || // Connection exception
                                 sqlState.equals("57P01"))) { // Admin shutdown
            return true;
        }
        return false;
    }

    // Метод для генерации хэша предмета
    private String generateItemHash(ItemInfo item) {
        // Пример: хэш на основе имени, координат и контейнера
        String data = item.name + item.c.toString() + item.q;
        return NUtils.calculateSHA256(data);  // Используем SHA-256 для генерации хэша
    }
}