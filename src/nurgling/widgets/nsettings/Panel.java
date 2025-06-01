
package nurgling.widgets.nsettings;

import haven.Coord;
import haven.Label;
import haven.UI;
import haven.Widget;

public class Panel extends Widget {
    public Panel(String title) {
        super(UI.scale(580,580));
        add(new Label(title), UI.scale(10, 10));
    }

    // Метод для загрузки настроек (должен быть переопределен в потомках)
    public void load() {
        // Базовая реализация пустая
    }

    // Метод для сохранения настроек (должен быть переопределен в потомках)
    public void save() {
        // Базовая реализация пустая
    }
}