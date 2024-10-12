package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.*;

public class NEditFolderName extends Window {
    private final TextEntry te;
    private final NArea area;
    private final NAreasWidget.AreaItem item;
    private final NChangeAreaFolder parentWindow;
    private final NAreasWidget areasWidget;

    public NEditFolderName(NAreasWidget areasWidget, NArea area, NAreasWidget.AreaItem item, NChangeAreaFolder parentWindow) {
        super(UI.scale(new Coord(260, 100)), "New Folder");
        this.areasWidget = areasWidget;
        this.area = area;
        this.item = item;
        this.parentWindow = parentWindow;

        prev = add(te = new TextEntry(UI.scale(200), ""), UI.scale(5, 5));
        add(new Button(UI.scale(60), "Save") {
            @Override
            public void click() {
                if (!te.text().isEmpty()) {
                    area.dir = te.text().trim();
                    updateAreas();
                    NEditFolderName.this.hide();
                }
            }
        }, prev.pos("ur").adds(5, -6));

        add(new Button(UI.scale(60), "Cancel") {
            @Override
            public void click() {
                NEditFolderName.this.hide();
                // Показываем предыдущее окно
                parentWindow.show();
            }
        }, prev.pos("ur").adds(70, -6));

        pack();
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
            // Показываем предыдущее окно
            parentWindow.show();
        } else {
            super.wdgmsg(msg, args);
        }
    }
}
