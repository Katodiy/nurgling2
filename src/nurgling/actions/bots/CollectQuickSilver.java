package nurgling.actions.bots;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NWItem;
import nurgling.actions.*;
import nurgling.actions.test.TESTtakehanddporop;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.HandWithoutContent;
import nurgling.tasks.NotThisInHand;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class CollectQuickSilver implements Action {

    private ArrayList<Container> containers;

    NAlias target;

    public CollectQuickSilver(ArrayList<Container> containers) {
        this.containers = containers;
        target = new NAlias("Quicksilver");
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if (containers.isEmpty())
            return Results.ERROR("No containers in area");

        WItem bucket = NUtils.getEquipment().findBucket("Quicksilver");
        NArea.Specialisation barrels = new NArea.Specialisation(Specialisation.SpecName.barrel.toString(), "Quicksilver");
        if(barrels == null)
            return Results.ERROR("Quicksilver spec not found");
        NArea area = NContext.findSpec(barrels);
        if (bucket != null && area != null) {
            Gob barrel = Finder.findGob(area, new NAlias("barrel"));
            if (barrel != null) {
                for (Container container : containers) {
                    Container.TargetItems targetItems = container.getattr(Container.TargetItems.class);
                    if (targetItems.getTargets(target) != 0) {
                        if(NUtils.getGameUI().vhand == null)
                            NUtils.takeItemToHand(bucket);
                        new PathFinder(Finder.findGob(container.gobid)).run(gui);
                        new OpenTargetContainer(container).run(gui);
                        for (WItem item : NUtils.getGameUI().getInventory(container.cap).getItems(target)) {
                            NUtils.itemact(item);
                        }
                        NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(container.cap), target, 0));
                        new CloseTargetContainer(container).run(gui);
                    }
                }

                if(NUtils.getGameUI().vhand != null) {
                    new PathFinder(barrel).run(gui);
                    NUtils.activateItem(barrel, true);
                    NUtils.getUI().core.addTask(new HandWithoutContent());
                    NUtils.getEquipment().wdgmsg("drop", -1);
                }
            }
        }

        return Results.SUCCESS();
    }
}
