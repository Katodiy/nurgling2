package nurgling.widgets;

import haven.*;

public class NMenuGridWdg extends NDraggableWidget
{
    MenuGrid menuGrid;

    public NMenuGridWdg()
    {
        super("menugrid", Coord.z);
    }

    public MenuGrid setMenuGrid(MenuGrid menuGrid)
    {
        this.menuGrid = menuGrid;
        add(menuGrid);
        pack();
        return menuGrid;
    }
}
