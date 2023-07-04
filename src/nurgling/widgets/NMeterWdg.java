package nurgling.widgets;

import haven.*;

public class NMeterWdg extends NDraggableWidget
{
    public NMeterWdg(String name, Coord sz, Widget meter)
    {
        super(name, sz);
        add(meter);
        pack();
    }
}
