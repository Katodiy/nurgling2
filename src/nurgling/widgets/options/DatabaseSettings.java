package nurgling.widgets.options;

import haven.*;
import haven.Button;
import haven.Label;
import nurgling.DBPoolManager;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.tools.NParser;
import nurgling.widgets.nsettings.Panel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedList;

public class DatabaseSettings extends Panel {
    private Widget prev;
    private TextEntry hostEntry, usernameEntry, passwordEntry;
    private TextEntry filePathEntry;
    private Label hostLabel, userLabel, passLabel, fileLabel;
    private Button initDbButton;
    private CheckBox enableCheckbox;
    private final int labelWidth = UI.scale(80); // Ширина лейблов
    private final int entryX = UI.scale(110);    // X-координата для TextEntry (was 90, increased for better space)
    private final int margin = UI.scale(10);

    public DatabaseSettings() {
        super("");
        int y = margin;

        // Чекбокс включения/выключения базы данных
        prev = enableCheckbox = add(new CheckBox("Enable using Database") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.ndbenable);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.ndbenable, val);
                a = val;
                updateWidgetsVisibility();
            }
        }, new Coord(margin, y));
        y += enableCheckbox.sz.y + UI.scale(8);

        // Заголовок раздела
        prev = add(new Label("Database Settings:"), new Coord(margin, y));
        y += prev.sz.y + UI.scale(5);

        // Выпадающий список для выбора типа базы данных
        prev = add(new Label("Database Type:"), new Coord(margin, y));
        Dropbox<String> dbType = add(new Dropbox<String>(UI.scale(150), 5, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return new LinkedList<>(getDbTypes()).get(i);
            }

            @Override
            protected int listitems() {
                return getDbTypes().size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                // Устанавливаем соответствующий флаг в конфиге
                NConfig.set(NConfig.Key.postgres, "PostgreSQL".equals(item));
                NConfig.set(NConfig.Key.sqlite, "SQLite".equals(item));
                // Обновляем отображение виджетов
                updateWidgetsVisibility();
            }
        }, new Coord(entryX, y));
        dbType.change((Boolean) NConfig.get(NConfig.Key.postgres) ? "PostgreSQL" : "SQLite");
        y += dbType.sz.y + UI.scale(10);

        int firstSettingY = y;

        // Создаем виджеты для PostgreSQL
        hostLabel = add(new Label("Host:"), new Coord(margin, firstSettingY));
        hostEntry = add(new TextEntry(UI.scale(150), ""), new Coord(entryX, firstSettingY));
        hostEntry.settext((String) NConfig.get(NConfig.Key.serverNode));
        y += hostEntry.sz.y + UI.scale(5);

        userLabel = add(new Label("Username:"), new Coord(margin, y));
        usernameEntry = add(new TextEntry(UI.scale(150), ""), new Coord(entryX, y));
        usernameEntry.settext((String) NConfig.get(NConfig.Key.serverUser));
        y += usernameEntry.sz.y + UI.scale(5);

        passLabel = add(new Label("Password:"), new Coord(margin, y));
        passwordEntry = add(new TextEntry(UI.scale(150), ""), new Coord(entryX, y));
        passwordEntry.settext((String) NConfig.get(NConfig.Key.serverPass));
        passwordEntry.pw = true;
        y += passwordEntry.sz.y + UI.scale(10);

        // Создаем виджеты для SQLite
        fileLabel = add(new Label("File Path:"), new Coord(margin, firstSettingY));
        filePathEntry = add(new TextEntry(UI.scale(150), ""), new Coord(entryX, firstSettingY));
        filePathEntry.settext((String) NConfig.get(NConfig.Key.dbFilePath));
        y += filePathEntry.sz.y + UI.scale(5);

        // Кнопка инициализации новой базы данных
        initDbButton = add(new Button(UI.scale(200), "Initialize New Database") {
            @Override
            public void click() {
                super.click();
                java.awt.EventQueue.invokeLater(() -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileNameExtensionFilter("SQLite Database", "db"));
                    if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                        return;

                    String dbPath = fc.getSelectedFile().getAbsolutePath();
                    if (!dbPath.endsWith(".db")) {
                        dbPath += ".db";
                    }

                    try {
                        // Создаем новую базу данных
                        Files.deleteIfExists(Paths.get(dbPath));
                        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

                        // Инициализируем таблицы
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("CREATE TABLE recipes (" +
                                    "recipe_hash VARCHAR(64) PRIMARY KEY, " +
                                    "item_name VARCHAR(255) NOT NULL, " +
                                    "resource_name VARCHAR(255) NOT NULL, " +
                                    "hunger FLOAT NOT NULL, " +
                                    "energy INT NOT NULL)");

                            stmt.executeUpdate("CREATE TABLE ingredients (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                    "recipe_hash VARCHAR(64) REFERENCES recipes (recipe_hash) ON DELETE CASCADE, " +
                                    "name VARCHAR(255) NOT NULL, " +
                                    "percentage FLOAT NOT NULL)");

                            stmt.executeUpdate("CREATE TABLE feps (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                    "recipe_hash VARCHAR(64) REFERENCES recipes (recipe_hash) ON DELETE CASCADE, " +
                                    "name VARCHAR(255) NOT NULL, " +
                                    "value FLOAT NOT NULL, " +
                                    "weight FLOAT NOT NULL)");

                            stmt.executeUpdate("CREATE TABLE containers (" +
                                    "hash VARCHAR(64) PRIMARY KEY, " +
                                    "grid_id BIGINT, " +
                                    "coord VARCHAR(255))");

                            stmt.executeUpdate("CREATE TABLE storageitems (" +
                                    "item_hash VARCHAR(64) PRIMARY KEY, " +
                                    "name VARCHAR(255) NOT NULL, " +
                                    "quality DOUBLE PRECISION, " +
                                    "coordinates VARCHAR(255), " +
                                    "container VARCHAR(64) NOT NULL)");
                        }

                        conn.close();

                        // Устанавливаем путь в текстовое поле
                        filePathEntry.settext(dbPath);
                        NUtils.getGameUI().msg("Database successfully created and initialized", Color.YELLOW);
                    } catch (Exception e) {
                        NUtils.getGameUI().msg("Failed to create database: " + e.getMessage(), Color.RED);
                        e.printStackTrace();
                    }
                });
            }
        }, new Coord(entryX, y));
        // y += initDbButton.sz.y + UI.scale(5);

        // Обновляем видимость виджетов в соответствии с текущим выбором
        updateWidgetsVisibility();
    }

    public void save() {
        // Сохраняем настройки в зависимости от выбранного типа базы данных
        if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
            NConfig.set(NConfig.Key.serverNode, hostEntry.text());
            NConfig.set(NConfig.Key.serverUser, usernameEntry.text());
            NConfig.set(NConfig.Key.serverPass, passwordEntry.text());
        } else {
            NConfig.set(NConfig.Key.dbFilePath, filePathEntry.text());
        }
    }

    private void updateWidgetsVisibility() {
        boolean isEnabled = (Boolean) NConfig.get(NConfig.Key.ndbenable);
        boolean isPostgres = isEnabled && (Boolean) NConfig.get(NConfig.Key.postgres);
        boolean isSQLite = isEnabled && !isPostgres;

        if (hostLabel != null) {
            // Управляем видимостью всех элементов в зависимости от включения базы данных
            hostLabel.visible = isPostgres;
            hostEntry.visible = isPostgres;
            userLabel.visible = isPostgres;
            usernameEntry.visible = isPostgres;
            passLabel.visible = isPostgres;
            passwordEntry.visible = isPostgres;

            fileLabel.visible = isSQLite;
            filePathEntry.visible = isSQLite;
            initDbButton.visible = isSQLite;

            if (ui != null) {
                if (ui.core.poolManager == null)
                    ui.core.poolManager = new DBPoolManager(1);
                ui.core.poolManager.reconnect();
            }
        }

        // Переупаковываем виджет
        pack();
        sz.y = UI.scale(130);
    }

    private LinkedList<String> getDbTypes() {
        LinkedList<String> types = new LinkedList<>();
        types.add("PostgreSQL");
        types.add("SQLite");
        return types;
    }
}
