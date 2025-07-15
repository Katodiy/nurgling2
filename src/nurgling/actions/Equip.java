package nurgling.actions;

import haven.Coord;
import haven.Inventory;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.WaitItemInHand;
import nurgling.tasks.WaitItemInEquip;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.NEquipory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Equip implements Action {

    NAlias target_name;
    NAlias exception = null;
    boolean isBothHands = false;

    public Equip(NAlias target_name) {
        this.target_name = target_name;
    }

    public Equip(NAlias target_name, boolean isBothHands) {
        this.target_name = target_name;
        this.isBothHands = isBothHands;
    }

    public Equip(NAlias target_name, NAlias exception) {
        this.target_name = target_name;
        this.exception = exception;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if(target_name.keys.contains("Traveller's Sack")) {
            target_name.keys.add("Traveler's Sack");
        } else if (target_name.keys.contains("Traveler's Sack")) {
            target_name.keys.add("Traveller's Sack");
        }
        WItem lhand = NUtils.getEquipment().findItem (NEquipory.Slots.HAND_LEFT.idx);

        WItem rhand = NUtils.getEquipment().findItem (NEquipory.Slots.HAND_RIGHT.idx);
        boolean l = lhand != null && NParser.checkName(((NGItem) lhand.item).name(), target_name);
        boolean r = rhand != null && NParser.checkName(((NGItem) rhand.item).name(), target_name);

        if ((!isBothHands && (l || r)) || (isBothHands && l && r)) {
            return Results.SUCCESS();
        }

        WItem wbelt = NUtils.getEquipment().findItem (NEquipory.Slots.BELT.idx);
        if(wbelt!=null) {
            if (wbelt.item.contents instanceof NInventory) {
                WItem witem = ((NInventory) wbelt.item.contents).getItem(target_name);
                if(witem != null) {
                    if (isTwoHanded(witem) && ((lhand != null && rhand != null && lhand != rhand && !isTwoHanded(lhand)))) {
                        NUtils.takeItemToHand(rhand);
                        if (((NInventory) wbelt.item.contents).getFreeSpace() == 0) {
                            WItem item = NUtils.getGameUI().vhand;
                            Coord pos = NUtils.getGameUI().getInventory().getFreeCoord(item);
                            gui.getInventory().dropOn(pos, ((NGItem) item.item).name());
                        } else {
                            NUtils.transferToBelt();
                        }

                        NUtils.takeItemToHand(lhand);
                        ((NInventory) wbelt.item.contents).dropOn(witem.c.div(Inventory.sqsz));
                        NUtils.getUI().core.addTask(new WaitItemInHand(witem));
                        NUtils.getEquipment().wdgmsg("drop", -1);
                    } else {
                        if ((rhand == null && lhand == null) || (!isTwoHanded(witem) && (rhand == null || lhand == null))) {
                            NUtils.takeItemToHand(witem);
                            NUtils.getEquipment().wdgmsg("drop", -1);
                        } else {

                            if(lhand!=null && !NParser.checkName(((NGItem)lhand.item).name(), exception))
                            {
                                NUtils.takeItemToHand(lhand);
                                ((NInventory) wbelt.item.contents).dropOn(witem.c.div(Inventory.sqsz));
                                NUtils.getUI().core.addTask(new WaitItemInHand(witem));
                                NUtils.getEquipment().wdgmsg("drop", -1);

                            }
                            else
                            {
                                NUtils.takeItemToHand(rhand);
                                ((NInventory) wbelt.item.contents).dropOn(witem.c.div(Inventory.sqsz));
                                NUtils.getUI().core.addTask(new WaitItemInHand(witem));
                                NUtils.getEquipment().wdgmsg("drop", -1);
                            }
                        }
                    }
                    NUtils.getUI().core.addTask(new WaitItemInEquip(witem,new NEquipory.Slots[]{NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT}));
                }
                else {
                        return Results.ERROR("No target item");
                }

            }

        }

        return Results.SUCCESS();
    }

    boolean isTwoHanded(WItem item)
    {
        HashSet<String> items = new HashSet<>();
        items.add("Scythe");
        items.add("Pickaxe");
        items.add("Glass Blowing Rod");
        items.add("Boar Spear");
        items.add("Metal Shovel");
        items.add("Tinker's Shovel");
        items.add("Wooden Shovel");
        items.add("Dowsing Rod");
        items.add("Battle Axe of the Twelfth Bay");
        items.add("Cutblade");
        return items.contains(((NGItem)item.item).name());
    }
}
