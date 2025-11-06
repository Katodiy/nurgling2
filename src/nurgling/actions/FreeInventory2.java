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
        System.out.println("[FreeInventory2] Starting inventory transfer");
        HashMap<String,Integer> props = DropContainer.getDropProps();
        System.out.println("[FreeInventory2] Drop props loaded: " + props.size() + " entries");
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
                System.out.println("[FreeInventory2] Dropping item: " + ((NGItem) fordrop.item).name() + " (quality: " + ((NGItem) fordrop.item).quality + ")");
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
        
        System.out.println("[FreeInventory2] Finished dropping low-quality items");
        ArrayList<WItem> items = gui.getInventory().getItems();
        System.out.println("[FreeInventory2] Items in inventory to transfer: " + items.size());
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
            double quality = ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1;
            System.out.println("[FreeInventory2] Processing item: " + name + ", quality: " + quality);
            if(context.addOutItem(name, null, quality)) {
                System.out.println("[FreeInventory2]   -> Added to targets");
                targets.add(name);
            } else {
                System.out.println("[FreeInventory2]   -> Not added (already exists or no output area)");
            }
        }

        System.out.println("[FreeInventory2] Transferring " + targets.size() + " different item types: " + targets);
        new TransferItems2(context, targets).run(gui);

        return Results.SUCCESS();
    }
}
