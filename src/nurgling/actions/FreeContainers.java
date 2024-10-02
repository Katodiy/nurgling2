package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Context;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeContainers implements Action
{
    ArrayList<Container> containers;

    public FreeContainers(ArrayList<Container> containers) {
        this.containers = containers;
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
            new PathFinder(container.gob).run(gui);
            new OpenTargetContainer(container).run(gui);
            for(WItem item : gui.getInventory(container.cap).getItems())
            {
                String name = ((NGItem)item.item).name();
                NArea area = NArea.findOut(name, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1);
                if(area != null) {
                    targets.add(name);
                }
            }
            while (!new TakeItemsFromContainer(container, targets).run(gui).isSuccess)
            {
                for(String name: targets)
                {
                    new TransferItems(context, name).run(gui);
                }
                new PathFinder(container.gob).run(gui);
                new OpenTargetContainer(container).run(gui);
            }
            new CloseTargetContainer(container).run(gui);
        }
        for(String name: targets)
        {
            new TransferItems(context, name).run(gui);
        }
        return Results.SUCCESS();
    }
}
