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

    public NGlobalSearchItems(NSearchItem item, DatabaseManager databaseManager) {
        this.item = item;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        if (item.name.isEmpty() && item.q.isEmpty()) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
