package nurgling.actions;

import haven.Gob;
import haven.WItem;
import haven.Widget;
import haven.Window;
import haven.res.ui.barterbox.Shopbox;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NContext;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class TakeItems2 implements Action
{
    final NContext cnt;
    String item;
    int count;


    public TakeItems2(NContext context, String item, int count)
    {
        this.cnt = context;
        this.item = item;
        this.count = count;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        AtomicInteger left = new AtomicInteger(count);
        ArrayList<NContext.ObjectStorage> inputs = cnt.getInStorages(item);
        if(inputs == null || inputs.isEmpty())
            return Results.FAIL();
        for(NContext.ObjectStorage input: inputs)
        {
            if(input instanceof NContext.Barter)
                takeFromBarter(left,gui, (NContext.Barter)input);
            else if (input instanceof NContext.Pile)
            {
                takeFromPile(left, gui,(NContext.Pile) input);
            }
            else if (input instanceof Container)
            {
                takeFromContainer(left, gui, (Container) input);
            }
            if(NUtils.getGameUI().getInventory().getItems(new NAlias(item)).size() >= count) {
                return Results.SUCCESS();
            }
            else
            {
                left.set(count - NUtils.getGameUI().getInventory().getItems(new NAlias(item)).size());
            }
        }
        return Results.SUCCESS();
    }

    public Results takeFromBarter(AtomicInteger left, NGameUI gui, NContext.Barter barter) throws InterruptedException
    {
        Gob gchest = Finder.findGob(barter.chest);
        Gob gbarter = Finder.findGob(barter.barter);
        if(gbarter==null || gchest==null)
            return Results.FAIL();
        new PathFinder(gchest).run(gui);
        new OpenTargetContainer("Chest", gchest).run(gui);
        ArrayList<WItem> items = gui.getInventory("Chest").getItems("Branch");
        int size = items.size();
        int to_take = Math.min(left.get(),size);
        new SimpleTransferToContainer(gui.getInventory(), gui.getInventory("Chest").getItems("Branch"), to_take).run(gui);
        left.set(left.get() - to_take);
        new PathFinder(gbarter).run(gui);
        new OpenTargetContainer("Barter Stand", gbarter).run(gui);

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

    public Results takeFromPile(AtomicInteger left, NGameUI gui, NContext.Pile pile) throws InterruptedException
    {
        new PathFinder(pile.pile).run(gui);
        new OpenTargetContainer("Stockpile",  pile.pile).run(gui);
        TakeItemsFromPile tifp;
        (tifp = new TakeItemsFromPile(pile.pile, gui.getStockpile(), left.get())).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);

        return Results.SUCCESS();
    }

    public Results takeFromContainer(AtomicInteger left, NGameUI gui, Container cont) throws InterruptedException
    {
        Gob contgob = Finder.findGob(cont.gobid);
        if(contgob == null)
            return Results.FAIL();
        new PathFinder(contgob).run(gui);
        new OpenTargetContainer(cont).run(gui);
        TakeItemsFromContainer tifc = new TakeItemsFromContainer(cont,new HashSet<>(Arrays.asList(item)), null);
        tifc.minSize = left.get();
        tifc.run(gui);
            new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
        return Results.SUCCESS();
    }
}
