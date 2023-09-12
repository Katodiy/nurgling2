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
    public boolean mousedown(Coord c, int button)
    {
        if(ui.core.mode!= NCore.Mode.DRAG)
        {
            menuGrid.mousedown(c,button);
            return true;
        }
        else
        {
            return super.mousedown(c, button);
        }
    }

    @Override
    public boolean mouseup(Coord c, int button)
    {
        if(ui.core.mode!= NCore.Mode.DRAG)
        {
            return menuGrid.mouseup(c,button);
        }
        else
        {
            return super.mouseup(c, button);
        }
    }

    @Override
    public void mousemove(Coord c)
    {
        super.mousemove(c);
    }

    @Override
    public void draw(GOut g)
    {
        super.draw(g);
        pbox.draw(g, menuGrid.c.sub(marg), menuGrid.sz.add(marg.mul(2)));
    }
}
