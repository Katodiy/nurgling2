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

    String target_name;
    String exception = null;

    public Equip(String target_name) {
        this.target_name = target_name;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        WItem lhand = NUtils.getEquipment().findItem (NEquipory.Slots.HAND_LEFT.idx);

        WItem rhand = NUtils.getEquipment().findItem (NEquipory.Slots.HAND_RIGHT.idx);
        if((lhand!=null && NParser.checkName(target_name,((NGItem)lhand.item).name())) || (rhand!=null && NParser.checkName(target_name,((NGItem)rhand.item).name())))
        {
            return Results.SUCCESS();
        }
        WItem wbelt = NUtils.getEquipment().findItem (NEquipory.Slots.BELT.idx);
        if(wbelt!=null) {
            if (wbelt.item.contents instanceof NInventory) {
                WItem witem = ((NInventory) wbelt.item.contents).getItem(new NAlias(target_name));
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
                        NUtils.getUI().core.addTask(new WaitItemInEquip(witem,new NEquipory.Slots[]{NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT}));
                    } else {
                        if (rhand == null && lhand == null) {
                            NUtils.takeItemToHand(witem);
                            NUtils.getEquipment().wdgmsg("drop", -1);
                            NUtils.getUI().core.addTask(new WaitItemInEquip(witem,new NEquipory.Slots[]{NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT}));
                        } else {

                            if(lhand!=null && !NParser.checkName(((NGItem)lhand.item).name(), new NAlias(exception)))
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
        items.add("Boar Spear");
        items.add("Metal Shovel");
        items.add("Tinker's Shovel");
        items.add("Dowsing Rod");
        items.add("Battle Axe of the Twelfth Bay");
        items.add("Cutblade");
        return items.contains(((NGItem)item.item).name());
    }
}
