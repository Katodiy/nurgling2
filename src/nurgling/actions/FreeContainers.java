package nurgling.actions;

import haven.WItem;
import nurgling.*;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.routes.RoutePoint;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeContainers implements Action
{
    ArrayList<Container> containers;
    NAlias pattern = null;
    RoutePoint closestRoutePoint = null;

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
        NContext context = new NContext(gui);

        this.closestRoutePoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(gui);

        for(Container container: containers)
        {
            Container.Space space;
            if((space = container.getattr(Container.Space.class)).isReady())
            {
                if(space.getRes().get(Container.Space.FREESPACE) == space.getRes().get(Container.Space.MAXSPACE))
                    continue;
            }

            navigateToTargetContainer(gui, container);

            new OpenTargetContainer(container).run(gui);
            for(WItem item : (pattern==null)?gui.getInventory(container.cap).getItems():gui.getInventory(container.cap).getItems(pattern))
            {
                if(context.addOutItem(((NGItem)item.item).name(),null,((NGItem) item.item).quality))
                    targets.add(((NGItem)item.item).name());
            }
            while (!new TakeItemsFromContainer(container, targets, pattern).run(gui).isSuccess)
            {
                new TransferItems2(context, targets).run(gui);
                navigateToTargetContainer(gui, container);
                new OpenTargetContainer(container).run(gui);
            }
            new CloseTargetContainer(container).run(gui);
        }
        new TransferItems2(context, targets).run(gui);
        return Results.SUCCESS();
    }

    private void navigateToTargetContainer(NGameUI gui, Container container) throws InterruptedException {
        PathFinder pf;

        if(PathFinder.isAvailable(Finder.findGob(container.gobid))) {
            pf = new PathFinder(Finder.findGob(container.gobid));
            pf.isHardMode = true;
            pf.run(gui);
        } else {
            new RoutePointNavigator(this.closestRoutePoint).run(NUtils.getGameUI());
            pf = new PathFinder(Finder.findGob(container.gobid));
            pf.isHardMode = true;
            pf.run(gui);
        }
    }
}
