package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import java.util.*;
import java.awt.event.*;

public class NChangeAreaFolder extends Window {
    private final List<String> folderList;
    private final NArea area;
    private final NAreasWidget.AreaItem item;
    private final NAreasWidget areasWidget;
    private final Listbox<String> listbox;

    public NChangeAreaFolder(NAreasWidget areasWidget, NArea area, NAreasWidget.AreaItem item, List<String> folderList) {
        super(UI.scale(new Coord(260, 200)), "Change Folder");
        this.areasWidget = areasWidget;
        this.area = area;
        this.item = item;
        this.folderList = folderList;

        // Создаём список для выбора папки
        listbox = new Listbox<String>(UI.scale(200), UI.scale(10), UI.scale(20)) {
            @Override
            protected String listitem(int idx) {
                return folderList.get(idx);
            }

            @Override
            protected int listitems() {
                return folderList.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, Coord.z);
            }
        };

        listbox.sel = area.dir != null && !area.dir.isEmpty() ? area.dir : "DefaultFolder";
        add(listbox, UI.scale(new Coord(5, 5)));

        // Добавляем кнопки
        Button selectBtn = new Button(UI.scale(60), "Select") {
            @Override
            public void click() {
                String selectedDir = listbox.sel;
                if (selectedDir != null) {
                    if (selectedDir.equals("New folder...")) {
                        promptNewFolder();
                    } else {
                        if (selectedDir.equals("DefaultFolder")) {
                            area.dir = null;
                        } else {
                            area.dir = selectedDir;
                        }
                        updateAreas();
                        NChangeAreaFolder.this.hide();
                    }
                }
            }
        };
        add(selectBtn, listbox.c.add(UI.scale(0, listbox.sz.y + 5)));

        Button cancelBtn = new Button(UI.scale(60), "Cancel") {
            @Override
            public void click() {
                NChangeAreaFolder.this.hide();
            }
        };
        add(cancelBtn, selectBtn.c.add(UI.scale(selectBtn.sz.x + 5, 0)));

        pack();
    }

    private void promptNewFolder() {
        // Открываем окно для ввода нового имени папки
        NEditFolderName folderNameWindow = new NEditFolderName(areasWidget, area, item, this);
        ui.root.add(folderNameWindow, c);
        this.hide();
    }

    private void updateAreas() {
        // Обновляем интерфейс
        areasWidget.al.updateList();
        NConfig.needAreasUpdate(); // Сохраняем изменения
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(msg, args);
        }
    }
}
