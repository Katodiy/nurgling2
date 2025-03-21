package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashSet;

public class TransferTargetItemsFromContainers implements Action
{
    ArrayList<Container> containers;

    public TransferTargetItemsFromContainers(Context cnt, ArrayList<Container> containers, HashSet<String> targets, NAlias pattern) {
        this.context = cnt;
        this.containers = containers;
        this.targets = targets;
        this.patter = pattern;
    }

    HashSet<String> targets = new HashSet<>();

    NAlias patter;

    Context context;
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {

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

            while (!new TakeItemsFromContainer(container, targets, patter).run(gui).isSuccess)
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
