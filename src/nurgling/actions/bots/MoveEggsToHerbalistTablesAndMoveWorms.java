package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;


public class MoveEggsToHerbalistTablesAndMoveWorms implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        String eggs = "Silkworm Egg";
        String worms = "Silkworm";
        String leaves = "Mulberry Leaf";


        new DepositItemsToSpecArea(context, "Mulberry Leaf", Specialisation.SpecName.silkwormFeeding, 32).run(gui);

//        // 1. Fetch Mulberry Leaves and deposit in cupboards
//        while (needsToRestockLeaves()) {
//            new TakeItems2(context, leaves, gui.getInventory().getFreeSpace()).run(gui);
//            HashSet<String> leavesSet = new HashSet<>(Collections.singleton(leaves));
//            new TransferItems2(context, leavesSet).run(gui); // deposits to cupboards
//        }
//
//        // 2. Move worms from tables to cupboards
//        while (needsToMoveWorms()) {
//            new TakeItems2(context, worms, gui.getInventory().getFreeSpace(), Specialisation.SpecName.htable).run(gui);
//            HashSet<String> wormsSet = new HashSet<>(Collections.singleton(worms));
//            new TransferItems2(context, wormsSet).run(gui); // deposits to cupboards
//        }
//
//        // 3. Move eggs from egg cupboards to empty tables
//        while (needsToMoveEggs()) {
//            new TakeItems2(context, eggs, gui.getInventory().getFreeSpace(), Specialisation.SpecName.eggStorage).run(gui);
//            HashSet<String> eggsSet = new HashSet<>(Collections.singleton(eggs));
//            new TransferItems2(context, eggsSet).run(gui); // deposits to tables
//        }
//
//        new FreeInventory2(context).run(gui);

        return Results.SUCCESS();
    }
}
