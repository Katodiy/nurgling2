package nurgling.actions.bots;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class CollectAndMoveSilkwormEggs implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        Specialisation.SpecName specName = Specialisation.SpecName.silkmothBreeding;
        String item = "Silkworm Egg";

        while (true) {
            int invSpace = gui.getInventory().getFreeSpace();
            int before = gui.getInventory().getItems(new NAlias(item)).size();

            new TakeItems2(context, item, invSpace, specName).run(gui);

            int after = gui.getInventory().getItems(new NAlias(item)).size();

            boolean hasEggsInInventory = after > 0;

            if (hasEggsInInventory) {
                new FreeInventory2(context).run(gui);
            }

            if (after <= before && !hasEggsInInventory) {
                return Results.SUCCESS();
            }
        }
    }
}
