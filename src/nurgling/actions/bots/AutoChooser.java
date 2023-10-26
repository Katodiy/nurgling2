package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tasks.*;

import java.util.*;

public class AutoChooser implements Action
{
    String value;
    NInventory inv;

    public AutoChooser(NInventory inv, String value)
    {
        this.value = value;
        this.inv = inv;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(inv!=null && (Window)inv.parent!=null)
        {
            NUtils.getUI().core.enableBotMod();
            NCore.LastActions actions = NUtils.getUI().core.getLastActions();
            if (actions.item != null && actions.petal != null)
            {
                NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                ArrayList<WItem> items = inv.getItems(actions.item.item);
                if (items.size() > 0)
                    ((Window) inv.parent).disable();
                for (WItem item : items)
                {
                    if (!((Window) inv.parent).isDisabled())
                    {
                        NUtils.getUI().core.disableBotMod();
                        return Results.SUCCESS();
                    }
                    new SelectFlowerAction(actions.petal, (NWItem) item).run(gui);
                }
            }
            NUtils.getUI().core.disableBotMod();
            ((Window) inv.parent).enable();
        }
        return Results.SUCCESS();
    }

    public static void enable(NInventory inventory, String value){
        if(inventory.parent instanceof Window)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        new AutoChooser(inventory, value).run(NUtils.getGameUI());
                    }
                    catch (InterruptedException e)
                    {
                        NUtils.getGameUI().tickmsg(AutoChooser.class.getName() + "stopped");
                    }
                }
            }, "Auto chooser(BOT)").start();
        }
    }
}
