package nurgling.actions.bots;

import haven.Coord;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.WaitItemInEquip;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.NEquipory;
import nurgling.NInventory;

import java.util.ArrayList;

public class EquipTravellersSacksFromBelt implements Action {
    private static final NAlias sackAlias = new NAlias("Traveller's Sack", "Traveler's Sack");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem wbelt = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);
        if (wbelt == null || !(wbelt.item.contents instanceof NInventory))
            return Results.ERROR("No belt or can't access belt inventory");

        NInventory beltInv = (NInventory) wbelt.item.contents;
        ArrayList<WItem> sacksOnBelt = beltInv.getItems(sackAlias);
        if (sacksOnBelt.isEmpty())
            return Results.ERROR("No Traveller's Sacks on belt");

        WItem lhand = NUtils.getEquipment().findItem(NEquipory.Slots.HAND_LEFT.idx);
        if (lhand == null || !NParser.checkName(((NGItem) lhand.item).name(), sackAlias)) {
            if (!equipSackToHand(gui, beltInv, NEquipory.Slots.HAND_LEFT.idx))
                return Results.ERROR("Could not equip sack to left hand");
        }

        WItem rhand = NUtils.getEquipment().findItem(NEquipory.Slots.HAND_RIGHT.idx);
        if (rhand == null || !NParser.checkName(((NGItem) rhand.item).name(), sackAlias)) {
            if (!equipSackToHand(gui, beltInv, NEquipory.Slots.HAND_RIGHT.idx))
                return Results.ERROR("Could not equip sack to right hand");
        }

        return Results.SUCCESS();
    }

    private boolean equipSackToHand(NGameUI gui, NInventory beltInv, int handSlot) throws InterruptedException {
        WItem sack = beltInv.getItem(sackAlias);
        if (sack == null)
            return false;

        NUtils.takeItemToHand(sack);

        NUtils.getEquipment().wdgmsg("drop", handSlot);

        NEquipory.Slots slot = (handSlot == NEquipory.Slots.HAND_LEFT.idx)
                ? NEquipory.Slots.HAND_LEFT
                : NEquipory.Slots.HAND_RIGHT;

        NUtils.getUI().core.addTask(new WaitItemInEquip(sack, new NEquipory.Slots[]{slot}));

        if (NUtils.getGameUI().vhand != null) {
            if (beltInv.getFreeSpace() > 0) {
                NUtils.transferToBelt();
            } else {
                WItem vhandItem = NUtils.getGameUI().vhand;
                if (vhandItem != null) {
                    Coord pos = NUtils.getGameUI().getInventory().getFreeCoord(vhandItem);
                    gui.getInventory().dropOn(pos, ((NGItem) vhandItem.item).name());
                }
            }
        }
        return true;
    }
}
