package nurgling.actions.bots;

import haven.*;
import haven.res.lib.itemtex.*;
import haven.res.ui.barterbox.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.*;
import org.json.*;

import java.util.*;

public class Scaner implements Action
{
    NArea area;

    public Scaner(NArea area)
    {
        this.area = area;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        Gob stand = Finder.findGob(area, new NAlias("gfx/terobjs/barterstand"));
        if(stand==null)
        {
            return Results.ERROR("No BARTER STAND in area");
        }
        new PathFinder(stand).run(gui);
        new OpenTargetContainer("Barter Stand", stand).run(gui);
        Window barter_wnd = gui.getWindow("Barter Stand");
        if(barter_wnd==null)
        {
            return Results.ERROR("No Barter window");
        }
        for(Widget ch = barter_wnd.child; ch != null; ch = ch.next) {
            if(ch instanceof Shopbox)
            {
                Shopbox sb = (Shopbox) ch;
                Shopbox.ShopItem price = sb.getPrice();
                if(price!=null)
                {
                    String name = price.name;
                    JSONObject res = ItemTex.save(price.spr);
                    if(res!=null && !name.equals("Branch"))
                    {
                        JSONArray data = area.jout;
                        boolean find = false;
                        for (int i = 0; i < data.length(); i++)
                        {
                            if (((JSONObject) data.get(i)).get("name").equals(name))
                            {
                                find = true;
                                break;
                            }
                        }
                        if (!find)
                        {
                            res.put("name", name);
                            res.put("marked", true);
                            data.put(res);

                        }
                    }
                }
                Shopbox.ShopItem offer = sb.getOffer();
                if(offer!=null)
                {
                    String name = offer.name;
                    JSONObject res = ItemTex.save(offer.spr);
                    if(res!=null)
                    {
                        JSONArray data = area.jin;
                        boolean find = false;
                        for (int i = 0; i < data.length(); i++)
                        {
                            if (((JSONObject) data.get(i)).get("name").equals(name))
                            {
                                find = true;
                                break;
                            }
                        }
                        if (!find)
                        {
                            res.put("name", name);
                            res.put("marked", true);
                            data.put(res);
                        }
                    }
                }
            }
        }
        area.update();
        NConfig.needAreasUpdate();
        NUtils.getGameUI().areas.set(area.id);

        return Results.SUCCESS();
    }

    public static void startScan(NArea area)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    new Scaner(area).run(NUtils.getGameUI());
                }
                catch (InterruptedException e)
                {
                    NUtils.getGameUI().tickmsg(Scaner.class.getName() + "stopped");
                }
            }
        }, "Scaner(BOT)").start();

    }
}
