package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.tasks.FilledPile;
import nurgling.tasks.WaitItems;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class TransferToContainer implements Action{

    NAlias items;

    Context.Container container;
    Context context;
    public TransferToContainer(Context context, Context.Container container, NAlias items) {
        this.container = container;
        this.items = items;
        this.context = context;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> witems;
        if (!(witems = gui.getInventory().getItems(items)).isEmpty()) {
                new PathFinder(container.gob).run(gui);
                witems = gui.getInventory().getItems(items);
                if (container.cap != null) {
                    new OpenTargetContainer(container.cap, container.gob).run(gui);
                } else {
                    new OpenAbstractContainer(new ArrayList<>(Context.contcaps.getall(container.gob.ngob.name)), container, context).run(gui);
                }
                transfer_size = Math.min(gui.getInventory().getItems(items).size(),Math.min(witems.size(), gui.getInventory(container.cap).getNumberFreeCoord(witems.get(0))));
                int oldSpace = gui.getInventory(container.cap).getItems(items).size();
                for(int i = 0; i <transfer_size; i++)
                {
                    witems.get(i).item.wdgmsg("transfer", Coord.z);
                }
                NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(container.cap), items, oldSpace + transfer_size));
                context.updateContainer(container.cap, gui.getInventory(container.cap),container);
            }
        return Results.SUCCESS();
    }
    int transfer_size = 0;

    public int getResult() {
        return transfer_size;
    }
}
