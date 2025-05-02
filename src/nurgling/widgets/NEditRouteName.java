package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.routes.Route;

public class NEditRouteName extends Window
{
    private final TextEntry te;
    public NEditRouteName()
    {
        super(UI.scale(new Coord(260, 25)), "Edit name");
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

    public static void openChangeName(Route route, RoutesWidget.RouteItem item)
    {
        NUtils.getGameUI().nern.show();
        NUtils.getGameUI().nern.raise();
        NUtils.getGameUI().nern.route = route;
        NUtils.getGameUI().nern.item = item;
        NUtils.getGameUI().nern.te.settext(route.name);

    }

    public Route route;
    RoutesWidget.RouteItem item;
}
