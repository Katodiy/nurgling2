package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.routes.Route;

public class NEditRouteName extends Window
{
    private final TextEntry te;
    public NEditRouteName(Coord sz, String title)
    {
        super(sz, title);
        prev = add(te = new TextEntry(UI.scale(200), ""));
        add(new Button(UI.scale(60), "Save")
        {
            @Override
            public void click()
            {
                super.click();
                NEditRouteName.this.hide();
                if(!te.text().isEmpty())
                {
                    ((NMapView) NUtils.getGameUI().map).changeRouteName(route.id, te.text());
                    item.label.settext(te.text());
                    NConfig.needRoutesUpdate();
                }
            }
        }, prev.pos("ur").adds(5, -6));
    }

    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if(msg.equals("close"))
        {
            hide();
        }
        else
        {
            super.wdgmsg(msg, args);
        }
    }

    @Override
    public void show() {
        super.show();
        // Center the window on screen
        if (parent != null) {
            Coord sz = parent.sz;
            Coord c = sz.div(2).sub(this.sz.div(2));
            this.c = c;
        }
    }

    public static void openChangeName(Route route, RoutesWidget.RouteItem item)
    {
        NEditRouteName nern = new NEditRouteName(UI.scale(new Coord(260, 25)), "Edit name");
        NUtils.getGameUI().add(nern);
        nern.show();
        nern.raise();
        nern.route = route;
        nern.item = item;
        nern.te.settext(route.name);
    }

    public Route route;
    RoutesWidget.RouteItem item;
}
