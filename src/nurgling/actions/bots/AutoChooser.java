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
    String item;

    public AutoChooser(NInventory inv,String item, String value)
    {
        this.value = value;
        this.inv = inv;
        this.item = item;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(inv!=null && (Window)inv.parent!=null)
        {
            NUtils.getUI().core.enableBotMod();
            NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
            ArrayList<WItem> items = inv.getItems(item);
            if (!items.isEmpty())
                ((Window) inv.parent).disable();
            ArrayList<WItem> for_ignore = new ArrayList<>();
            while (!items.isEmpty())
            {
                if (!((Window) inv.parent).isDisabled())
                {
                    NUtils.getUI().core.disableBotMod();
                    return Results.SUCCESS();
                }
                WItem cand = items.get(0);
                new SelectFlowerAction(value, cand).run(gui);
                for_ignore.add(cand);
                items = inv.getItems(item);
                items.removeAll(for_ignore);
            }
            NUtils.getUI().core.disableBotMod();
            if(inv.parent!=null)
                ((Window) inv.parent).enable();
        }
        return Results.SUCCESS();
    }

    public static void enable(NInventory inventory, String item, String value){
        if(inventory.parent instanceof Window)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        new AutoChooser(inventory, item, value).run(NUtils.getGameUI());
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
