package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.conf.NFishingSettings;
import nurgling.tasks.NTask;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;

import java.util.ArrayList;

/**
 * Repairs fishing rod using items from inventory and equipped items with storage (belt, creel, etc.)
 */
public class RepairFishingRotFromInventory implements Action {

    NFishingSettings prop;

    public RepairFishingRotFromInventory(NFishingSettings prop) {
        this.prop = prop;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem rod = NUtils.getEquipment().findItem(prop.tool);
        if(rod == null) {
            return Results.ERROR("No fishing rod");
        }

        if(!((NGItem)rod.item).findContent(prop.fishline)) {
            Results repRes = repItemFromInventory(gui, rod, prop.fishline, 1);
            if (!repRes.IsSuccess()) return repRes;
        }

        if(!((NGItem)rod.item).findContent(prop.hook)) {
            Results repRes = repItemFromInventory(gui, rod, prop.hook, 1);
            if (!repRes.IsSuccess()) return repRes;
        }

        if(!((NGItem)rod.item).findContent(prop.bait)) {
            Results repRes = repItemFromInventory(gui, rod, prop.bait, prop.tool.endsWith("Primitive Casting-Rod")?1:5);
            if (!repRes.IsSuccess()) return repRes;
        }
        return Results.SUCCESS();
    }

    private Results repItemFromInventory(NGameUI gui, WItem rod, String item, int count) throws InterruptedException {
        WItem fl = findItemInAllInventories(item);
        if(fl == null) {
            return Results.ERROR("No " + item + " found in inventory or equipment");
        }
        NUtils.takeItemToHand(fl);
        NWItem itemHand = (NWItem) NUtils.getGameUI().vhand;
        NUtils.itemact(rod);
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return ((NWItem) NUtils.getGameUI().vhand !=itemHand || ((NGItem)rod.item).findContent(item)) ;
            }
        });
        if((NWItem) NUtils.getGameUI().vhand!=null) {
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return NUtils.getGameUI().vhand.item.spr!=null;
                }
            });
            NUtils.dropToInv();
        }
        return Results.SUCCESS();
    }

    /**
     * Search for item in player inventory and all equipped items with storage (belt, creel, etc.)
     */
    private WItem findItemInAllInventories(String itemName) throws InterruptedException {
        NAlias alias = new NAlias(itemName);
        
        // First check player's main inventory
        WItem item = NUtils.getGameUI().getInventory().getItem(alias);
        if(item != null) {
            return item;
        }
        
        // Then check all equipped items that have inventory (belt, creel, etc.)
        NEquipory equip = (NEquipory) NUtils.getEquipment();
        if(equip != null) {
            for(NEquipory.Slots slot : NEquipory.Slots.values()) {
                WItem equipped = equip.quickslots[slot.idx];
                if(equipped != null && equipped.item.contents instanceof NInventory) {
                    NInventory equipInv = (NInventory) equipped.item.contents;
                    item = equipInv.getItem(alias);
                    if(item != null) {
                        return item;
                    }
                }
            }
        }
        
        return null;
    }
}

