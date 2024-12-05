package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NISBox;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeContainers implements Action
{
    ArrayList<Container> containers;
    NAlias pattern = null;

    public FreeContainers(ArrayList<Container> containers) {
        this.containers = containers;
    }

    public FreeContainers(ArrayList<Container> containers, NAlias pattern) {
        this.containers = containers;
        this.pattern = pattern;
    }



    HashSet<String> targets = new HashSet<>();

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        Context context = new Context();
        for(Container container: containers)
        {
            Container.Space space;
            if((space = container.getattr(Container.Space.class)).isReady())
            {
                if(space.getRes().get(Container.Space.FREESPACE) == space.getRes().get(Container.Space.MAXSPACE))
                    continue;
            }
            PathFinder pf = new PathFinder(container.gob);
            pf.isHardMode = true;
            pf.run(gui);
            new OpenTargetContainer(container).run(gui);
            for(WItem item : (pattern==null)?gui.getInventory(container.cap).getItems():gui.getInventory(container.cap).getItems(pattern))
            {
                String name = ((NGItem)item.item).name();
                NArea area = NArea.findOut(name, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1);
                if(area != null) {
                    targets.add(name);
                }
            }
            while (!new TakeItemsFromContainer(container, targets, pattern).run(gui).isSuccess)
            {
                new TransferItems(context, targets).run(gui);
                pf = new PathFinder(container.gob);
                pf.isHardMode = true;
                pf.run(gui);
                new OpenTargetContainer(container).run(gui);
            }
            new CloseTargetContainer(container).run(gui);
        }
        new TransferItems(context, targets).run(gui);
        return Results.SUCCESS();
    }
}
