package nurgling.widgets;

import haven.*;
import nurgling.*;

public class NMenuGridWdg extends Widget
{
    MenuGrid menuGrid;

    final Coord marg = UI.scale(new Coord(6,6));
    final Coord dmarg = UI.scale(new Coord(2,2));
    public static final IBox pbox = Window.wbox;
    public NMenuGridWdg()
    {
        super( Coord.z);
    }

    public MenuGrid setMenuGrid(MenuGrid menuGrid)
    {
        this.menuGrid = menuGrid;
        add(menuGrid,dmarg);
        pack();
        return menuGrid;
    }

    @Override
    public void resize(Coord sz)
    {
        super.resize(sz.add(dmarg.mul(2)));
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if(ui.core.mode!= NCore.Mode.DRAG)
        {
            menuGrid.mousedown(ev);
            return true;
        }
        else
        {
            return super.mousedown(ev);
        }
    }

    @Override
    public boolean mouseup(MouseUpEvent ev) {
        if(ui.core.mode!= NCore.Mode.DRAG)
        {
            return menuGrid.mouseup(ev);
        }
        else
        {
            return super.mouseup(ev);
        }
    }

    @Override
    public void mousemove(MouseMoveEvent ev) {
        super.mousemove(ev);
    }

    @Override
    public void draw(GOut g)
    {
        super.draw(g);
        pbox.draw(g, menuGrid.c.sub(marg), menuGrid.sz.add(marg.mul(2)));
    }
}
