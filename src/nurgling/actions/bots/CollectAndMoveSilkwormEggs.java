package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

/**
 * Collects silkworm eggs from breeding cabinets and stores them in egg storage
 * Uses FreeInventory2 to automatically deposit eggs in the correct storage area
 */
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
