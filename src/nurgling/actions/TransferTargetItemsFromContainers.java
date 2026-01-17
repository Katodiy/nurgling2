package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
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
            // Skip empty containers using visual flag (except dframes)
            Gob gob = Finder.findGob(container.gobid);
            if(gob != null && !"Frame".equals(container.cap) && gob.ngob.isContainerEmpty())
            {
                continue;
            }
            PathFinder pf = new PathFinder(Finder.findGob(container.gobid));
            pf.isHardMode = true;
            pf.run(gui);
            new OpenTargetContainer(container).run(gui);

            while (!new TakeItemsFromContainer(container, targets, patter).run(gui).isSuccess)
            {
                new TransferItems(context, targets).run(gui);
                pf = new PathFinder(Finder.findGob(container.gobid));
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
