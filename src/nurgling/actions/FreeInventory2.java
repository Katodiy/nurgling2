package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.areas.NContext;
import nurgling.tasks.GetItems;
import nurgling.tasks.NTask;
import nurgling.widgets.DropContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FreeInventory2 implements Action
{
    NContext context;

    public FreeInventory2(NContext context) {
        this.context = context;
    }

    HashSet<String> targets = new HashSet<>();

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        HashMap<String,Integer> props = DropContainer.getDropProps();
        WItem fordrop = null;

        do {
            fordrop = null;
            for (WItem item : gui.getInventory().getItems()) {
                if (props.containsKey(((NGItem) item.item).name())) {
                    if (((NGItem) item.item).quality == null || ((NGItem) item.item).quality < props.get(((NGItem) item.item).name())) {
                        fordrop = item;
                        break;
                    }

                }
            }
            if (fordrop != null) {
                NUtils.drop(fordrop);
                WItem finalFordrop = fordrop;
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        GetItems gi = new GetItems(gui.getInventory());
                        gi.check();
                        if(gi.check())
                            return !gi.getResult().contains(finalFordrop);
                        return false;
                    }
                });
            }
        }while (fordrop !=null);

        for(WItem item : gui.getInventory().getItems())
        {
            String name = ((NGItem)item.item).name();
            if(context.addOutItem(name, null, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1))
                targets.add(name);
        }

        new TransferItems2(context, targets).run(gui);

        return Results.SUCCESS();
    }
}
