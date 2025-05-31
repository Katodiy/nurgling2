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
}
