package nurgling.actions;

import haven.*;
import haven.res.lib.itemtex.*;
import haven.res.ui.barterbox.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;
import org.json.*;

import java.util.*;
import java.util.concurrent.atomic.*;

public class TakeItems implements Action
{
    final Context cnt;
    String item;
    int count;


    public TakeItems(Context context, String item, int count)
    {
        this.cnt = context;
        this.item = item;
        this.count = count;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        AtomicInteger left = new AtomicInteger(count);
        for(Context.Input input: cnt.getInputs(item))
        {
            if(input instanceof Context.Barter)
                takeFromBarter(left,gui, (Context.Barter)input);
            if(left.get() == 0)
                return Results.SUCCESS();
        }
        return Results.SUCCESS();
    }

    public Results takeFromBarter(AtomicInteger left, NGameUI gui, Context.Barter barter) throws InterruptedException
    {
        new PathFinder(barter.chest).run(gui);
        new OpenTargetContainer("Chest", barter.chest).run(gui);
        ArrayList<WItem> items = gui.getInventory("Chest").getItems("Branch");
        int size = items.size();
        int to_take = Math.min(left.get(),size);
        new TransferItems(gui.getInventory(), gui.getInventory("Chest").getItems("Branch"), to_take).run(gui);
        left.set(left.get() - to_take);
        new PathFinder(barter.barter).run(gui);
        new OpenTargetContainer("Barter Stand", barter.barter).run(gui);

        Window barter_wnd = gui.getWindow("Barter Stand");
        if(barter_wnd==null)
        {
            return Results.ERROR("No Barter window");
        }
        for(Widget ch = barter_wnd.child; ch != null; ch = ch.next)
        {
            if (ch instanceof Shopbox)
            {
                Shopbox sb = (Shopbox) ch;
                Shopbox.ShopItem offer = sb.getOffer();
                if (offer != null)
                {
                    if (offer.name.equals(item))
                    {
                        for (int i = 0; i < to_take; i++)
                        {
                            sb.wdgmsg("buy", new Object[0]);
                        }

                        NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(item), to_take));
                        break;
                    }
                }
            }
        }
        return Results.SUCCESS();
    }
}
