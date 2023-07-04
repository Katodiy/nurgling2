package nurgling.widgets;

import haven.*;

public class NPortpraitWdg extends NDraggableWidget
{
    public NPortpraitWdg(Frame wdg, Coord sz)
    {
        super("portrait", sz);
        add(wdg);
        pack();
    }
}
