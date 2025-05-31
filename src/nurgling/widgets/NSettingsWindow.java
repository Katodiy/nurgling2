package nurgling.widgets;


import haven.*;
import nurgling.widgets.nsettings.Fonts;
import nurgling.widgets.nsettings.Panel;

import java.util.*;
import java.awt.Color;

public class NSettingsWindow extends Window {
    private final SettingsList list;
    Widget container;
    private Widget currentPanel = null;
    public NSettingsWindow() {
        super(UI.scale(800, 600), "Settings", true);

        // Создаем контейнер для разделения на две части
        container = add(new Widget(Coord.z));

        // Левая часть - список настроек
        list = add(new SettingsList(UI.scale(200, 580)), UI.scale(10, 10));


        // Наполняем список демо-настройками
        populateDemoSettings();
        // Ресайз контейнера
        container.resize(UI.scale(800, 600));

       // pack();
    }


    private void populateDemoSettings() {
        // Пример структуры настроек
        SettingsCategory general = new SettingsCategory("General",new Panel("General"),container);
        general.addChild(new SettingsItem("Fonts",new Fonts(),container));
        list.addCategory(general);
    }

    // Класс для списка настроек
    private class SettingsList extends SListBox<SettingsItem, SettingsListItem> {
        public SettingsList(Coord sz) {
            super(sz, UI.scale(24));
        }

        @Override
        protected List<? extends SettingsItem> items() {
            // Возвращаем плоский список всех элементов с учетом иерархии
            List<SettingsItem> allItems = new ArrayList<>();
            for (SettingsItem item : categories) {
                allItems.add(item);
                if(item.expanded)
                    allItems.addAll(item.getChildren());
            }
            return allItems;
        }

        @Override
        protected SettingsListItem makeitem(SettingsItem item, int idx, Coord sz) {
            return new SettingsListItem(this, sz, item);
        }

        private final List<SettingsCategory> categories = new ArrayList<>();

        public void addCategory(SettingsCategory category) {
            categories.add(category);
            update();
        }

        public void update() {
            super.update();
        }
    }

    // Класс для элемента списка настроек
    private class SettingsListItem extends SListWidget.ItemWidget<SettingsItem> {
        private final Text text;


        public SettingsListItem(SListWidget<SettingsItem, ?> list, Coord sz, SettingsItem item) {
            super(list, sz, item);

            // Определяем отступ в зависимости от уровня вложенности
            int indent = item.getLevel() * UI.scale(15);

            // Создаем текст с учетом отступа
            this.text = Text.render(item.getName());

            // Если есть дети, добавляем кнопку раскрытия
            if (!item.getChildren().isEmpty()) {
                add(new Button(UI.scale(20), "+"), indent, 0).action(() -> {
                    item.expanded = !item.expanded;
                    ((SettingsList)list).update();
                });
            }
        }

        @Override
        public void draw(GOut g) {
            // Рисуем текст с отступом
            int indent = item.getLevel() * UI.scale(15);
            g.image(text.tex(), Coord.of(indent + UI.scale(25), (sz.y - text.sz().y) / 2));
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (super.mousedown(ev)) {
                NSettingsWindow.this.showSettings(item);
                return true;
            }
            list.change(item);
            return true;
        }
    }

    // Базовый класс для элемента настроек
    private static class SettingsItem {
        public Widget panel;
        private boolean expanded = false;
        private final String name;
        private final List<SettingsItem> children = new ArrayList<>();
        private SettingsItem parent;

        public SettingsItem(String name, Widget panel, Widget container) {
            this.name = name;
            this.panel = panel;
            container.add(panel, UI.scale(210,0));
            panel.hide();
        }

        public String getName() { return name; }
        public List<SettingsItem> getChildren() { return children; }

        public void addChild(SettingsItem child) {
            child.parent = this;
            children.add(child);
        }

        public int getLevel() {
            return parent == null ? 0 : parent.getLevel() + 1;
        }
    }

    // Категория настроек (может содержать подкатегории)
    private static class SettingsCategory extends SettingsItem {
        public SettingsCategory(String name, Widget panel, Widget container) {
            super(name, panel, container);
        }
    }

    // Метод для отображения выбранных настроек
    private void showSettings(SettingsItem item) {
        if(currentPanel!=null)
            currentPanel.hide();
        currentPanel = item.panel;
        currentPanel.show();

    }


}