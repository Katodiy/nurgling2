package nurgling.actions;

import haven.*;
import haven.res.ui.tt.wear.Wear;
import nurgling.*;
import nurgling.tasks.*;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoSaveTableware implements Action
{
    public final static AtomicBoolean stop = new AtomicBoolean(false);
    NInventory tableInv = null;
    NInventory scInv = null;
    public AutoSaveTableware()
    {
        stop.set(false);
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        while (!stop.get())
        {
            tableInv = null;
            // Wait for condition: bot not paused and we can check table
            NUtils.addTask(new NTask()
            {
                @Override
                public boolean check()
                {
                    return stop.get() || (NUtils.getGameUI()!= null && findTableInventory());
                }
            });

            if (stop.get())
            {
                NUtils.getUI().core.autoSaveTableware = null;
                return Results.SUCCESS();
            }


            if (tableInv != null && scInv!=null)
            {
                ArrayList<WItem> items = tableInv.getItems();
                items.addAll(scInv.getItems());
                for (WItem witem : items)
                {

                    Wear w = ((NGItem) witem.item).getInfo(Wear.class);
                    if (w != null)
                    {

                        if (w.m - w.d <= 1)
                        {
                            witem.item.wdgmsg("transfer", haven.Coord.z);
                            NUtils.addTask(new nurgling.tasks.ISRemoved(witem.item.wdgid()));
                        }
                    }
                }
            }
        }


        NUtils.getUI().core.autoSaveTableware = null;
        return Results.SUCCESS();
    }


    private boolean findTableInventory()
    {
        NInventory tableCand = null;
        NInventory scCand = null;
        boolean isFeast = false;

        for (Widget w = NUtils.getGameUI().lchild; w != null; w = w.prev)
        {
            if (w instanceof Window)
            {
                Window wnd = (Window) w;
                if (((Window) w).cap.contains("Table"))
                {
                    for (Widget child : wnd.children())
                    {
                        if (child instanceof NInventory)
                        {
                            NInventory cand = ((NInventory) child);
                            if (cand.isz.y * cand.isz.x == 9)
                                tableCand = (NInventory) child;
                            if (cand.isz.y * cand.isz.x == 2)
                                scCand = (NInventory) child;
                        } else if (child instanceof Button)
                        {
                            if (((Button) child).text.text.equals("Feast!"))
                            {
                                isFeast = true;
                            }
                        }
                    }
                }
            }
        }
        if (isFeast)
        {
            tableInv = tableCand;
            scInv = scCand;
            return true;
        }

        return false;
    }

}
