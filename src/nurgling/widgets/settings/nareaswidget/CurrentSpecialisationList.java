package nurgling.widgets.settings.nareaswidget;

import haven.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CurrentSpecialisationList extends SListBox<SpecialisationItem, Widget> {
    public List<SpecialisationItem> specItems;

    public CurrentSpecialisationList(Coord sz) {
        super(sz, UI.scale(15));
        this.specItems = new ArrayList<>();
    }

    @Override
    public void change(SpecialisationItem item) {
        super.change(item);
    }

    protected List<SpecialisationItem> items() {
        return specItems;
    }

    @Override
    public void resize(Coord sz) {
        super.resize(new Coord(sz.x, sz.y));
    }

    protected Widget makeitem(SpecialisationItem item, int idx, Coord sz) {
        return (new ItemWidget<SpecialisationItem>(this, sz, item) {
            {
                add(item);
            }

            public boolean mousedown(Coord c, int button) {
                super.mousedown(c, button);
                return (true);
            }
        });
    }

    Color bg = new Color(30, 40, 40, 160);

    @Override
    public void draw(GOut g) {
        g.chcolor(bg);
        g.frect(Coord.z, g.sz());
        super.draw(g);
    }
    public void updateList() {
        super.reset(); // Сбрасываем старый список
    }
}
