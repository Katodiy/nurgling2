package nurgling.actions;

import haven.Coord;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.WaitLifted;
import nurgling.tasks.WaitPose;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeKIlnGP implements Action
{
    ArrayList<Container> containers;

    public FreeKIlnGP(ArrayList<Container> containers) {
        this.containers = containers;
    }


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
            int total = gui.getInventory(container.cap).getItems("Garden Pot").size();
            for (int i = 0; i < total; i++)
            {
                new PathFinder(container.gob).run(gui);
                new OpenTargetContainer(container).run(gui);
                gui.getInventory(container.cap).getItem("Garden Pot").item.wdgmsg("take", Coord.z);
                NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/banzai"));
                new FindPlaceAndAction(null,NArea.findSpec(new NArea.Specialisation(Specialisation.SpecName.gardenpot.toString()))).run(gui);
            }
        }
        return Results.SUCCESS();
    }
}
