package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.Equip;
import nurgling.actions.Results;
import nurgling.tools.NAlias;

public class EquipTSacks implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        new Equip(new NAlias("Traveller's Sack")).run(gui);
        new Equip(new NAlias("Traveller's Sack"), true).run(gui);
        return Results.SUCCESS();
    }
}
