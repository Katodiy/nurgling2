
package nurgling.widgets.nsettings;

import haven.Coord;
import haven.Label;
import haven.UI;
import haven.Widget;

import java.util.Collections;
import java.util.Map;

public class Panel extends Widget {
    public Panel()
    {
        super(UI.scale(580,580));
    }
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

    /**
     * Sub-trees of this panel that are hidden until something reveals them, mapped to the action
     * that reveals them.
     *
     * <p>The settings search leaves hidden widgets out of its index, because a result the user
     * cannot be shown is worse than no result. A panel that hides part of itself behind tabs or
     * sub-pages can register those containers here to have their contents indexed anyway; picking
     * one of their settings out of the search runs the matching action first.
     *
     * <p>Keyed by identity, so the map should be an IdentityHashMap.
     *
     * @return Container widget to reveal action; empty by default
     */
    public Map<Widget, Runnable> searchReveal() {
        return Collections.emptyMap();
    }
}
