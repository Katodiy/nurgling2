package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.navigation.NavigationService;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeContainers implements Action
{
    ArrayList<Container> containers;
    NAlias pattern = null;
    NArea workingArea = null;
    NContext externalContext = null;
    String workingAreaId = null;

    public FreeContainers(ArrayList<Container> containers) {
        this.containers = containers;
    }

    public FreeContainers(ArrayList<Container> containers, NAlias pattern) {
        this.containers = containers;
        this.pattern = pattern;
    }

    public FreeContainers(ArrayList<Container> containers, NArea area) {
        this.containers = containers;
        this.workingArea = area;
    }

    public FreeContainers(ArrayList<Container> containers, NAlias pattern, NArea area) {
        this.containers = containers;
        this.pattern = pattern;
        this.workingArea = area;
    }

    public FreeContainers(ArrayList<Container> containers, NContext context, String areaId) {
        this.containers = containers;
        this.externalContext = context;
        this.workingAreaId = areaId;
    }

    HashSet<String> targets = new HashSet<>();

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        NContext context = externalContext != null ? externalContext : new NContext(gui);

        for(Container container: containers)
        {
            Container.Space space;
            if((space = container.getattr(Container.Space.class)).isReady())
            {
                if(space.getRes().get(Container.Space.FREESPACE) == space.getRes().get(Container.Space.MAXSPACE))
                    continue;
            }

            navigateToTargetContainer(gui, container, context);

            Gob gob = Finder.findGob(container.gobHash);
            if (gob == null) {
                // Container no longer exists, skip it
                continue;
            }
            new OpenTargetContainer(container).run(gui);
            for(WItem item : (pattern==null)?gui.getInventory(container.cap).getItems():gui.getInventory(container.cap).getItems(pattern))
            {
                if(context.addOutItem(((NGItem)item.item).name(),null, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1))
                    targets.add(((NGItem)item.item).name());
            }
            while (!new TakeItemsFromContainer(container, targets, pattern).run(gui).isSuccess)
            {
                new TransferItems2(context, targets).run(gui);
                navigateToTargetContainer(gui, container, context);
                Gob gobCheck = Finder.findGob(container.gobHash);
                if (gobCheck == null) {
                    // Container no longer exists after navigation, break out
                    break;
                }
                new OpenTargetContainer(container).run(gui);
            }
            new CloseTargetContainer(container).run(gui);
        }
        new TransferItems2(context, targets).run(gui);
        return Results.SUCCESS();
    }

    private void navigateToTargetContainer(NGameUI gui, Container container, NContext context) throws InterruptedException {
        PathFinder pf;

        Gob gob = Finder.findGob(container.gobHash);
        if(gob != null && PathFinder.isAvailable(gob)) {
            pf = new PathFinder(gob);
            pf.isHardMode = true;
            pf.run(gui);
        } else if (workingAreaId != null && context != null) {
            // Use context navigation for user-selected areas
            context.navigateToAreaIfNeeded(workingAreaId);
            if((gob = Finder.findGob(container.gobHash)) != null) {
                pf = new PathFinder(gob);
                pf.isHardMode = true;
                pf.run(gui);
            }
        } else if (workingArea != null) {
            NavigationService.getInstance().navigateToArea(workingArea, gui);
            if((gob = Finder.findGob(container.gobHash)) != null) {
                pf = new PathFinder(gob);
                pf.isHardMode = true;
                pf.run(gui);
            }
        }
    }
}
