package nurgling.actions;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tools.Context;

import java.util.HashSet;
import java.util.Set;

public class FreeContainer implements Action
{
    Context.Container cont;
    Context context;

    boolean freeInEnd;
    HashSet<String> transferedItems;

    Context.Updater updater;
    public FreeContainer(Context.Container cont, Context context, boolean freeInEnd, HashSet<String> transferedItems) {
        this.cont = cont;
        this.context = context;
        this.freeInEnd = freeInEnd;
        this.transferedItems = transferedItems;
        if(this.transferedItems == null)
            this.transferedItems = new HashSet<>();
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        data.addAll(cont.itemInfo.keySet());
        for(String name: data)
        {
            if(context.getOutputs(name)!=null) {
                while (cont.itemInfo.get(name) != null) {
                    new PathFinder(cont.gob).run(gui);
                    new OpenTargetContainer(cont.cap, cont.gob).run(gui);
                    TakeItemsFromContainer tifc = new TakeItemsFromContainer(cont, name);
                    tifc.run(gui);
                    if(cont.itemInfo.get(name)!=null)
                    {
                        cont.freeSpace = gui.getInventory(cont.cap).getFreeSpace();
                        cont.isFree = cont.freeSpace == cont.maxSpace;
                        transferedItems.addAll(data);
                        transferAll(context, gui, transferedItems);
                    }
                }
            }
        }
        cont.freeSpace = gui.getInventory(cont.cap).getFreeSpace();
        cont.isFree = cont.freeSpace == cont.maxSpace;
        transferedItems.addAll(data);
        if(freeInEnd) {
            transferAll(context, gui, transferedItems);
        }
        return Results.SUCCESS();
    }

    Set<String> data = new HashSet<>();

    public Set<String> getData() {
        return data;
    }

    public static void transferAll(Context context, NGameUI gui, Set<String> names) throws InterruptedException
    {
        for(String name: names)
        {
            new TransferItems(context, name, gui.getInventory().getItems(name).size()).run(gui);
        }
    }
}
