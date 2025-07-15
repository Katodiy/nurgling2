package nurgling.actions.bots;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Equip;
import nurgling.actions.Results;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;
import nurgling.NInventory;

import java.util.ArrayList;

public class EquipTravellersSacksFromBelt implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NAlias sackAlias = new NAlias("Traveller's Sack", "Traveler's Sack");

        int equipped = 0;
        WItem lhand = NUtils.getEquipment().findItem(NEquipory.Slots.HAND_LEFT.idx);
        WItem rhand = NUtils.getEquipment().findItem(NEquipory.Slots.HAND_RIGHT.idx);

        if (lhand != null && sackAlias.keys.stream().anyMatch(k -> ((nurgling.NGItem) lhand.item).name().contains(k))) equipped++;
        if (rhand != null && sackAlias.keys.stream().anyMatch(k -> ((nurgling.NGItem) rhand.item).name().contains(k))) equipped++;

        if (equipped == 2)
            return Results.SUCCESS();

        WItem wbelt = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);
        if (wbelt == null || !(wbelt.item.contents instanceof NInventory))
            return Results.ERROR("No belt or can't access belt inventory");

        NInventory beltInv = (NInventory) wbelt.item.contents;
        ArrayList<WItem> sacksOnBelt = beltInv.getItems(sackAlias);

        int need = 2 - equipped;
        if (sacksOnBelt.isEmpty())
            return Results.ERROR("No Traveller's Sacks on belt");

        for (int i = 0; i < need && i < sacksOnBelt.size(); i++) {
            new Equip(sackAlias, true).run(gui);
        }

        return Results.SUCCESS();
    }
}
