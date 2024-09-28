package nurgling.actions;

import haven.*;
import haven.render.sl.InstancedUniform;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.FilledPile;
import nurgling.tasks.WaitItems;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.TreeSet;

public class TransferToContainer implements Action{

    NAlias items;

    Container container;
    Context context;

    Integer th = -1;
    public TransferToContainer(Context context, Container container, NAlias items) {
        this.container = container;
        this.items = items;
        this.context = context;
    }

    public TransferToContainer(Context context, Container container, NAlias items, Integer th) {
        this.container = container;
        this.items = items;
        this.context = context;
        this.th = th;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> witems;
        if (!(witems = gui.getInventory().getItems(items)).isEmpty()) {
                new PathFinder(container.gob).run(gui);
                if(th==-1)
                    witems = gui.getInventory().getItems(items);
                else
                    witems = gui.getInventory().getItems(items, th);
                if (container.cap != null) {
                    new OpenTargetContainer(container.cap, container.gob).run(gui);
                }
                transfer_size = Math.min(gui.getInventory().getItems(items).size(), Math.min(witems.size(), gui.getInventory(container.cap).getNumberFreeCoord(witems.get(0))));
                if(container.getattr(Container.TargetItems.class)!=null && container.getattr(Container.TargetItems.class).getRes().containsKey(Container.TargetItems.MAXNUM)) {
                    int need = (Integer) container.getattr(Container.TargetItems.class).getRes().get(Container.TargetItems.MAXNUM) - (Integer) container.getattr(Container.TargetItems.class).getTargets(items);
                    transfer_size = Math.min(transfer_size, need);
                }

                int oldSpace = gui.getInventory(container.cap).getItems(items).size();
                for(int i = 0; i <transfer_size; i++)
                {
                    witems.get(i).item.wdgmsg("transfer", Coord.z);
                }
                NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(container.cap), items, oldSpace + transfer_size));
                container.update();
            }
        return Results.SUCCESS();
    }
    int transfer_size = 0;

    public int getResult() {
        return transfer_size;
    }
}
