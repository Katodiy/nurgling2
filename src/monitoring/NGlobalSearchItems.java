package monitoring;

import nurgling.NConfig;
import nurgling.tools.NSearchItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class NGlobalSearchItems implements Runnable {
    private final NSearchItem item;
    public java.sql.Connection connection;

    public static final ArrayList<String> containerHashes = new ArrayList<>();

    public NGlobalSearchItems(NSearchItem itemn) {
        this.item = itemn;
    }

    @Override
    public void run() {
        if (item.name.isEmpty() && item.q.isEmpty())
            return;
        try {
            boolean isSQLite = ((Boolean) NConfig.get(NConfig.Key.sqlite));

            String nameOp = isSQLite ? "LIKE"  : "ILIKE";
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

            try (PreparedStatement preparedStatement = connection.prepareStatement(dynamicSql.toString())) {
                preparedStatement.setString(1, "%" + item.name + "%");

                for (int i = 0; i < item.q.size(); i++) {
                    preparedStatement.setDouble(i + 2, item.q.get(i).val);
                }

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    synchronized (containerHashes) {
                        containerHashes.clear();
                        while (resultSet.next()) {
                            containerHashes.add(resultSet.getString("hash"));
                        }
                    }
                }

                connection.commit();
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
