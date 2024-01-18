package nurgling.actions.bots;

import haven.*;
import haven.res.ui.tt.cn.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.iteminfo.*;
import nurgling.tasks.*;

import java.util.*;

public class AutoSplitter implements Action
{
    Double value;
    NInventory inv;

    public AutoSplitter(NInventory inv, Double value)
    {
        this.value = value;
        this.inv = inv;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        WItem item = NUtils.getUI().core.getLastActions().item;
        if (item != null)
        {
            String name = ((NGItem) item.item).name();
            NUtils.getUI().core.enableBotMod();
            NUtils.getUI().core.addTask(new WaitItemInHand(item));
            NInventory inv = (NInventory) item.parent;
            ((Window) inv.parent).disable();
            ArrayList<WItem> items = inv.getItems(name);
            if (items.size() > 0)
                ((Window) inv.parent).disable();
            inv.dropOn(inv.getFreeCoord(item), name);

            for (WItem witem : items)
            {
                if (!((Window) inv.parent).isDisabled())
                {
                    NUtils.getUI().core.disableBotMod();
                    return Results.SUCCESS();
                }
                GetItemCount gic = new GetItemCount(witem);
                NUtils.getUI().core.addTask(gic);
                if (gic.getResult() > value)
                {
                    int need = (int) (gic.getResult() / value + 0.01) - 1;
                    need = Math.min(need, inv.getNumberFreeCoord(item));
                    for (int i = 0; i < need; i++)
                    {
                        new SelectFlowerAction("Split", (NWItem) witem).run(gui);
                        FindSplitWnd fsw = new FindSplitWnd();
                        NUtils.getUI().core.addTask(fsw);
                        Window wnd = fsw.getResult();
                        for (Widget w2 = wnd.lchild; w2 != null; w2 = w2.prev)
                        {
                            if (w2 instanceof TextEntry)
                            {
                                ((TextEntry) w2).activate(String.valueOf(value));
                            }
                        }
                        NUtils.getUI().core.addTask(new WaitItemInHand(item));
                        inv.dropOn(inv.getFreeCoord(item), name);
                    }
                }
            }
            NUtils.getUI().core.disableBotMod();
            ((Window) inv.parent).enable();
        }

        return Results.SUCCESS();
    }

    public static void enable(NInventory inventory, Double value){
        if(inventory.parent instanceof Window)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        new AutoSplitter(inventory, value).run(NUtils.getGameUI());
                    }
                    catch (InterruptedException e)
                    {
                        NUtils.getGameUI().tickmsg(AutoSplitter.class.getName() + "stopped");
                    }
                }
            }, "Auto splitter(BOT)").start();
        }
    }
}
