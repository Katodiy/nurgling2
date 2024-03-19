package nurgling.actions;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tools.Context;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class FreeContainer implements Action
{
    Context.Container cont;
    Context context;

    boolean freeInEnd;
    HashSet<TreeMap<Integer,String>> transferedItems;

    Context.Updater updater;
    public FreeContainer(Context.Container cont, Context context, boolean freeInEnd, HashSet<TreeMap<Integer,String>> transferedItems) {
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
//        data.addAll(cont.itemInfo.keySet());
//        for(String name: data)
//        {
//                if (cont.itemInfo.get(name) != null) {
//                    if(context.getOutputs(name)!=null) {
//                    new PathFinder(cont.gob).run(gui);
//                    new OpenTargetContainer(cont.cap, cont.gob).run(gui);
//                    TakeItemsFromContainer tifc = new TakeItemsFromContainer(cont, name);
//                    tifc.run(gui);
//                    if(cont.itemInfo.get(name)!=null)
//                    {
//                        cont.freeSpace = gui.getInventory(cont.cap).getFreeSpace();
//                        cont.isFree = cont.freeSpace == cont.maxSpace;
//                        transferedItems.addAll(data);
//                        transferAll(context, gui, transferedItems);
//                    }
//                }
//            }
//        }
//        cont.freeSpace = gui.getInventory(cont.cap).getFreeSpace();
//        cont.isFree = cont.freeSpace == cont.maxSpace;
//        transferedItems.addAll(data);
//        if(freeInEnd) {
//            transferAll(context, gui, transferedItems);
//        }
        return Results.SUCCESS();
    }



    public static void transferAll(Context context, NGameUI gui, Set<String> names) throws InterruptedException
    {
        for(String name: names)
        {
            new TransferItems(context, name, gui.getInventory().getItems(name).size()).run(gui);
        }
    }
}
