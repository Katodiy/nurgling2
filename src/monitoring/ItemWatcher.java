package monitoring;

import haven.Coord;
import haven.Utils;
import nurgling.NUtils;
import nurgling.db.DatabaseManager;

import java.sql.SQLException;
import java.util.ArrayList;

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

    private final DatabaseManager databaseManager;
    private final ArrayList<ItemInfo> iis;

    public ItemWatcher(ArrayList<ItemInfo> iis, DatabaseManager databaseManager) {
        this.iis = iis;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        if (iis == null || iis.isEmpty()) {
            return;
        }

        try {
            databaseManager.executeOperation(adapter -> {
                // Delete old items from container
                deleteItems(adapter);

                // Insert new items
                insertItems(adapter);

                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteItems(nurgling.db.DatabaseAdapter adapter) throws SQLException {
        // Build parameterized IN clause: DELETE ... WHERE ... NOT IN (?, ?, ?, ...)
        String placeholders = iis.stream().map(i -> "?").collect(java.util.stream.Collectors.joining(","));
        String deleteSql = "DELETE FROM storageitems WHERE container = ? AND item_hash NOT IN (" + placeholders + ")";

        Object[] params = new Object[iis.size() + 1];
        params[0] = iis.get(0).container;

        // Set each item hash as a separate parameter
        for (int i = 0; i < iis.size(); i++) {
            params[i + 1] = generateItemHash(iis.get(i));
        }

        adapter.executeUpdate(deleteSql, params);
    }

    private void insertItems(nurgling.db.DatabaseAdapter adapter) throws SQLException {
        // Use LinkedHashMap to preserve column order
        java.util.LinkedHashMap<String, Object> columns = new java.util.LinkedHashMap<>();
        columns.put("item_hash", "?");
        columns.put("name", "?");
        columns.put("quality", "?");
        columns.put("coordinates", "?");
        columns.put("container", "?");
        
        String insertSql = adapter.getUpsertSql("storageitems", columns, java.util.List.of("item_hash"));

        // For simplicity, insert items one by one since batch operations are complex with different SQL syntax
        for (ItemInfo item : iis) {
            String itemHash = generateItemHash(item);
            adapter.executeUpdate(insertSql,
                itemHash, item.name, item.q, item.c.toString(), item.container);
        }
    }

    private String generateItemHash(ItemInfo item) {
        String data = item.name + item.c.toString() + item.q;
        return NUtils.calculateSHA256(data);
    }
}
