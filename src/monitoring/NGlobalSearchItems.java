package monitoring;

import nurgling.NInventory;
import nurgling.tools.NSearchItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class NGlobalSearchItems implements Runnable {
    NSearchItem item;
    public java.sql.Connection connection;


    public static final ArrayList<String> containerHashes = new ArrayList<>();

    public NGlobalSearchItems(NSearchItem itemn) {
        this.item = itemn;
    }
    final String sql = "SELECT DISTINCT c.hash " +
            "FROM containers c " +
            "JOIN storageitems si ON c.hash = si.container " +
            "WHERE si.name ILIKE ? ";

    @Override
    public void run() {
        if(item.name.isEmpty() && item.q.isEmpty())
            return;
        try {
            // Создаем динамический SQL-запрос с учетом всех условий качества
            StringBuilder dynamicSql = new StringBuilder(sql);

            // Добавляем условия для каждого элемента качества
            for (int i = 0; i < item.q.size(); i++) {
                if (i == 0) {
                    dynamicSql.append(" AND (");
                } else {
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
            if (!item.q.isEmpty()) {
                dynamicSql.append(")");
            }

            // Подготовка SQL-запроса
            try (PreparedStatement preparedStatement = connection.prepareStatement(dynamicSql.toString())) {
                // Установка параметра имени
                preparedStatement.setString(1, "%" + item.name + "%");

                // Установка параметров качества
                for (int i = 0; i < item.q.size(); i++) {
                    preparedStatement.setDouble(i + 2, item.q.get(i).val);
                }

                // Выполнение запроса
                ResultSet resultSet = preparedStatement.executeQuery();

                synchronized (containerHashes) {
                    containerHashes.clear();
                    while (resultSet.next()) {
                        containerHashes.add(resultSet.getString("hash"));
                    }
                }

                // Фиксация транзакции
                connection.commit();
            }
        } catch (SQLException e) {
            try {
                // Откат транзакции в случае ошибки
                connection.rollback();
                e.printStackTrace();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
        }
    }
}
