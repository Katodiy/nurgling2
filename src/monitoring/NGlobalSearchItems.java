package monitoring;

import nurgling.NConfig;
import nurgling.db.DatabaseManager;
import nurgling.tools.NSearchItem;

import java.sql.SQLException;
import java.util.ArrayList;

public class NGlobalSearchItems implements Runnable {
    private final NSearchItem item;
    private final DatabaseManager databaseManager;

    public static final ArrayList<String> containerHashes = new ArrayList<>();
    public static volatile long updateVersion = 0; // Incremented when containerHashes changes
    
    // Cache for last search query to avoid duplicate DB queries
    private static volatile String lastSearchQuery = "";
    private static volatile long lastQueryTime = 0;
    private static final long QUERY_CACHE_DURATION_MS = 2000; // Cache results for 2 seconds

    public NGlobalSearchItems(NSearchItem item, DatabaseManager databaseManager) {
        this.item = item;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        if (item.name.isEmpty() && item.q.isEmpty()) {
            return;
        }
        
        // Build search signature for deduplication
        String searchSignature = buildSearchSignature();
        long now = System.currentTimeMillis();
        
        // Skip if same search was just performed (within cache duration)
        if (searchSignature.equals(lastSearchQuery) && (now - lastQueryTime) < QUERY_CACHE_DURATION_MS) {
            nurgling.db.DatabaseManager.incrementSkippedSearch();
            return;
        }

        try {
            databaseManager.executeOperation(adapter -> {
                boolean isSQLite = adapter instanceof nurgling.db.SqliteAdapter;

                String nameOp = isSQLite ? "LIKE" : "ILIKE";
                String collation = isSQLite ? " COLLATE NOCASE" : "";

                StringBuilder dynamicSql = new StringBuilder()
                        .append("SELECT DISTINCT c.hash ")
                        .append("FROM containers c ")
                        .append("JOIN storageitems si ON c.hash = si.container ")
                        .append("WHERE si.name ").append(nameOp).append(" ?").append(collation);

                if (!item.q.isEmpty()) {
                    dynamicSql.append(" AND (");
                    for (int i = 0; i < item.q.size(); i++) {
                        if (i > 0) {
                            dynamicSql.append(" OR ");
                        }
                        dynamicSql.append("(");
                        switch (item.q.get(i).type) {
                            case MORE:
                                dynamicSql.append("si.quality > ?");
                                break;
                            case LOW:
                                dynamicSql.append("si.quality < ?");
                                break;
                            case EQ:
                                dynamicSql.append("si.quality = ?");
                                break;
                        }
                        dynamicSql.append(")");
                    }
                    dynamicSql.append(")");
                }

                Object[] params = new Object[1 + item.q.size()];
                params[0] = "%" + item.name + "%";

                for (int i = 0; i < item.q.size(); i++) {
                    params[i + 1] = item.q.get(i).val;
                }

                try (java.sql.ResultSet resultSet = adapter.executeQuery(dynamicSql.toString(), params)) {
                    synchronized (containerHashes) {
                        containerHashes.clear();
                        while (resultSet.next()) {
                            containerHashes.add(resultSet.getString("hash"));
                        }
                        updateVersion++;
                    }
                }

                return null;
            });
            
            // Update cache after successful query
            lastSearchQuery = searchSignature;
            lastQueryTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Build a signature representing the current search parameters
     */
    private String buildSearchSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(item.name);
        for (NSearchItem.Quality quality : item.q) {
            sb.append("|").append(quality.type).append(":").append(quality.val);
        }
        return sb.toString();
    }
    
    /**
     * Clear the query cache - called when container data changes
     */
    public static void clearQueryCache() {
        lastSearchQuery = "";
        lastQueryTime = 0;
    }
}
