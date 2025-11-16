package nurgling.actions.bots.silk;

import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.DepositItemsToSpecArea;
import nurgling.actions.Results;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

public class RefillSilkwormFeedingCupboards implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        String leaves = "Mulberry Leaf";

        new DepositItemsToSpecArea(context, new NAlias(leaves), Specialisation.SpecName.silkwormFeeding, 32).run(gui);

        return Results.SUCCESS();
    }
}
