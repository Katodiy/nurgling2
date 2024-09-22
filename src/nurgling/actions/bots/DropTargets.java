package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.GetItems;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class DropTargets implements Action {

    private ArrayList<Container> containers;

    NAlias target;

    public DropTargets(ArrayList<Container> containers, NAlias target) {
        this.containers = containers;
        this.target = target;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if (containers.isEmpty())
            return Results.ERROR("No containers in area");

        for (Container container : containers) {
            Container.TargetItems targetItems = container.getattr(Container.TargetItems.class);
            if(targetItems.getTargets(target)!=0)
            {
                new PathFinder(container.gob).run(gui);
                new OpenTargetContainer(container).run(gui);
                for(WItem item : NUtils.getGameUI().getInventory(container.cap).getItems(target))
                {
                    NUtils.drop(item);
                }
                NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(container.cap),target,0));
                container.update();
            }
        }

        return Results.SUCCESS();
    }
}
