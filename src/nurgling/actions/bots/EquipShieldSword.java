package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.Equip;
import nurgling.actions.Results;
import nurgling.tools.NAlias;

public class EquipShieldSword implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        new Equip(new NAlias("Wooden RoundShield"), new NAlias("Bronze Sword", "Hirdsman's Sword")).run(gui);
        new Equip(new NAlias("Bronze Sword", "Hirdsman's Sword"), new NAlias("Wooden RoundShield")).run(gui);
        return Results.SUCCESS();
    }
}
