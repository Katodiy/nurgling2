package nurgling.actions;

import haven.Coord;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.tasks.WaitFreeHand;
import nurgling.tasks.WaitItemInEquip;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;

public class EquipFromInventory implements Action {
    NAlias targetItem;
    NInventory inventory;

    public EquipFromInventory(NAlias targetItem) {
        this.targetItem = targetItem;
    }

    public EquipFromInventory(NAlias targetItem, NInventory inventory) {
        this.targetItem = targetItem;
        this.inventory = inventory;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem lhand = NUtils.getEquipment().findItem(NEquipory.Slots.HAND_LEFT.idx);
        WItem rhand = NUtils.getEquipment().findItem(NEquipory.Slots.HAND_RIGHT.idx);
        WItem wbelt = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);

        boolean needToFreeHand = (lhand != null && rhand != null);

        if (needToFreeHand) {
            // both hands are occupied, free one
            WItem handToFree = lhand;

            NUtils.takeItemToHand(handToFree);

            if (wbelt != null && wbelt.item.contents instanceof NInventory) {
                NInventory beltInv = (NInventory) wbelt.item.contents;
                if (beltInv.getFreeSpace() > 0) {
                    NUtils.transferToBelt();
                } else {
                    Coord pos = gui.getInventory().getFreeCoord(NUtils.getGameUI().vhand);
                    gui.getInventory().dropOn(pos, ((NGItem) NUtils.getGameUI().vhand.item).name());
                }
            } else {
                Coord pos = gui.getInventory().getFreeCoord(NUtils.getGameUI().vhand);
                gui.getInventory().dropOn(pos, ((NGItem) NUtils.getGameUI().vhand.item).name());
            }

            NUtils.getEquipment().wdgmsg("drop", -1);
            NUtils.getUI().core.addTask(new WaitFreeHand());
        }

        WItem item;
        // Now equip the desired item
        if (this.inventory == null) {
            item = gui.getInventory().getItem(targetItem);
        } else {
            item = this.inventory.getItem(targetItem);
        }

        if (item != null) {
            NUtils.takeItemToHand(item);
            NUtils.getEquipment().wdgmsg("drop", -1);
            NUtils.getUI().core.addTask(new WaitItemInEquip(item, new NEquipory.Slots[]{
                    NEquipory.Slots.HAND_LEFT,
                    NEquipory.Slots.HAND_RIGHT
            }));
        } else {
            return Results.ERROR("No target item found in inventory: " + targetItem.keys);
        }

        return Results.SUCCESS();
    }
}
