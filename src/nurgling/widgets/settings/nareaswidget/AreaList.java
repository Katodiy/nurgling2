package nurgling.widgets.settings.nareaswidget;

import haven.*;
import nurgling.*;
import nurgling.actions.bots.Scaner;
import nurgling.areas.NArea;
import nurgling.overlays.map.NOverlay;
import nurgling.widgets.NAreasWidget;
import nurgling.widgets.NChangeAreaFolder;
import nurgling.widgets.NEditAreaName;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AreaList extends SListBox<AreaList.AreaItem, Widget> {
    private String currentFolder = null; // Текущая папка, если null — корень
    final Tex folderIcon = new TexI(Resource.loadsimg("nurgling/data/folder/u"));
    private ConcurrentHashMap<Integer, AreaItem> areas = new ConcurrentHashMap<>();
    private NAreasWidget parentWidget;

    // Конструктор принимает размер и ссылку на родительский виджет
    public AreaList(Coord sz, NAreasWidget parentWidget) {
        super(sz, UI.scale(15));
        this.parentWidget = parentWidget;
    }

    // Метод для обновления текущего расположения (папки)
    public void setCurrentFolder(String folder) {
        // Проверяем, изменилось ли состояние папки
        if (Objects.equals(currentFolder, folder)) {
            return;
        }
        currentFolder = folder;
        updateList(); // Обновляем список при изменении папки
    }

    // Возвращает список элементов для отображения
    protected List<AreaItem> items() {
        List<AreaItem> list = new ArrayList<>();

        // Если мы в подкаталоге, добавляем ".. (Back)"
        if (currentFolder != null) {
            list.add(new AreaItem(".. (Back)", null) {
                @Override
                public boolean mousedown(Coord c, int button) {
                    if (button == 1) {  // Левый клик
                        setCurrentFolder(null);
                        return true;
                    }
                    return super.mousedown(c, button);
                }

                @Override
                public void draw(GOut g) {
                    g.chcolor(Color.WHITE);
                    g.text(text.text(), Coord.z);
                    g.chcolor();
                }
            });
        }

        if (currentFolder == null) {
            for (AreaItem areaItem : areas.values()) {
                if (areaItem.area != null && (areaItem.area.dir == null || areaItem.area.dir.isEmpty())) {
                    list.add(areaItem);
                }
            }

            Set<String> dirs = new HashSet<>();
            for (AreaItem areaItem : areas.values()) {
                if (areaItem.area != null && areaItem.area.dir != null && !areaItem.area.dir.isEmpty()) {
                    dirs.add(areaItem.area.dir);
                }
            }

            for (String dir : dirs) {
                list.add(new AreaItem(dir, null) {
                    @Override
                    public boolean mousedown(Coord c, int button) {
                        if (button == 1) {
                            setCurrentFolder(dir);
                            return true;
                        }
                        return super.mousedown(c, button);
                    }

                    @Override
                    public void draw(GOut g) {
                        g.image(folderIcon, Coord.z);
                        g.text(text.text(), new Coord(folderIcon.sz().x + 5, 0));
                    }
                });
            }
        } else {
            for (AreaItem areaItem : areas.values()) {
                if (areaItem.area != null && currentFolder.equals(areaItem.area.dir)) {
                    list.add(areaItem);
                }
            }
        }

        return list;
    }

    // Метод для обновления списка зон
    public void updateList() {
        super.reset(); // Сбрасываем текущий список и пересоздаем его
    }

    // Метод для добавления зоны
    public void addArea(int id, String val, NArea area) {
        areas.put(id, new AreaItem(val, area));
        updateList(); // Обновляем список после добавления новой зоны
    }

    // Метод для создания элемента списка
    @Override
    protected Widget makeitem(AreaItem item, int idx, Coord sz) {
        return new ItemWidget<AreaItem>(this, sz, item) {
            {
                add(item);
            }

            @Override
            public void draw(GOut g) {
                if (item.area == null) {
                    g.image(folderIcon, Coord.z);
                    g.text(item.text.text(), new Coord(folderIcon.sz().x + 5, 0));
                } else {
                    g.chcolor(Color.WHITE);
                    g.text(item.text.text(), Coord.z);
                    g.chcolor();
                    super.draw(g);
                }
            }

            @Override
            public boolean mousedown(Coord c, int button) {
                if (button == 3) { // Правый клик мыши
                    showContextMenu(c, item);
                    return true;
                } else if (button == 1) { // Левый клик мыши
                    if (item.area != null) {
                        parentWidget.select(item.area.id); // Выбор зоны
                    } else {
                        item.mousedown(c, button);
                    }
                }
                return super.mousedown(c, button);
            }

            // Метод для отображения контекстного меню
            private void showContextMenu(Coord c, AreaItem item) {
                List<String> options = new ArrayList<>();
                options.add("Edit Name");
                options.add("Delete Area");
                options.add("Move to Folder");

                NFlowerMenu menu = new NFlowerMenu(options.toArray(new String[0])) {
                    @Override
                    public void nchoose(NPetal option) {
                        if (option != null) {
                            switch (option.name) {
                                case "Edit Name":
                                    editName(item);
                                    break;
                                case "Delete Area":
                                    deleteArea(item);
                                    break;
                                case "Move to Folder":
                                    moveToFolder(item);
                                    break;
                            }
                        }
                    }

                    @Override
                    public boolean mousedown(Coord c, int button) {
                        if (super.mousedown(c, button)) {
                            nchoose(null);
                        }
                        return true;
                    }
                };

                // Вычисление абсолютных координат относительно корневого виджета
                Coord rootPos = this.rootpos(c);
                ui.root.add(menu, rootPos);
            }

            private void editName(AreaItem item) {
                if (item.area != null) {
//                    NEditAreaName.changeName(item.area);  // Изменение имени
                    System.out.println("Edit name for: " + item.text.text());
                }
            }

            private void deleteArea(AreaItem item) {
                areas.remove(item.area.id); // Удаление зоны
                updateList(); // Обновляем список после удаления
            }

            private void moveToFolder(AreaItem item) {
                System.out.println("Move area to folder for: " + item.text.text());
            }
        };
    }

    // Внутренний класс для элементов зоны
    public static class AreaItem extends Widget {
        public Label text;
        public NArea area;

        public AreaItem(String text, NArea area) {
            this.text = add(new Label(text));
            this.area = area;
        }

        @Override
        public void resize(Coord sz) {
            super.resize(sz);
        }
    }

    @Override
    public void resize(Coord sz) {
        super.resize(new Coord(UI.scale(170) - UI.scale(6), sz.y));
    }

    Color bg = new Color(30, 40, 40, 160);

    @Override
    public void draw(GOut g) {
        g.chcolor(bg);
        g.frect(Coord.z, g.sz());
        super.draw(g);
    }
}
