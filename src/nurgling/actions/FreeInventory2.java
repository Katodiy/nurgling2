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
import java.util.Comparator;
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

        ArrayList<WItem> items = gui.getInventory().getItems();
        items.sort(new Comparator<WItem>() {
            @Override
            public int compare(WItem o1, WItem o2) {
                Float q1 = ((NGItem)o1.item).quality;
                Float q2 = ((NGItem)o2.item).quality;
                if(q1 == null || q2 == null)
                    return 0;
                return Float.compare(q2,q1);
            }
        });
        for(WItem item : items)
        {
            String name = ((NGItem)item.item).name();
            if(context.addOutItem(name, null, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1))
                targets.add(name);
        }

            new TransferItems2(context, targets).run(gui);

        return Results.SUCCESS();
    }
}
