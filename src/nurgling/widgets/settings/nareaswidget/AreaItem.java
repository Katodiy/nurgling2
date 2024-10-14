package nurgling.widgets.settings.nareaswidget;

import haven.*;
import haven.Label;
import nurgling.*;
import nurgling.actions.bots.Scaner;
import nurgling.areas.NArea;
import nurgling.overlays.map.NOverlay;
import nurgling.widgets.NAreasWidget;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import haven.*;
import nurgling.areas.NArea;
import nurgling.widgets.NChangeAreaFolder;
import nurgling.widgets.NEditAreaName;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;

public class AreaItem extends Widget {
    public Label text;
    public NArea area;
    private NAreasWidget parentWidget; // Добавляем ссылку на родительский виджет

    // Обновленный конструктор принимает parentWidget как параметр
    public AreaItem(String text, NArea area, NAreasWidget parentWidget) {
        this.text = add(new Label(text));
        this.area = area;
        this.parentWidget = parentWidget; // Сохраняем parentWidget для дальнейшего использования
    }

    @Override
    public void resize(Coord sz) {
        super.resize(sz);
    }
}
