package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.equipment.EquipmentPreset;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitFreeHand;
import nurgling.widgets.NEquipory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EquipmentBot implements Action {

    private final EquipmentPreset preset;

    public EquipmentBot() {
        this.preset = null;
    }

    public EquipmentBot(EquipmentPreset preset) {
        this.preset = preset;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Map<Integer, String> slotConfig;

        if (preset != null) {
            slotConfig = preset.getSlotConfig();
        } else {
            return Results.ERROR("No equipment preset provided");
        }

        if (slotConfig == null || slotConfig.isEmpty()) {
            return Results.ERROR("Equipment configuration is empty");
        }

        // Process each configured slot
        for (Map.Entry<Integer, String> entry : slotConfig.entrySet()) {
            int targetSlot = entry.getKey();
            String targetResName = entry.getValue();

            // Check if item is already equipped in this slot
            WItem currentItem = NUtils.getEquipment().findItem(targetSlot);
            if (currentItem != null) {
                Resource currentRes = currentItem.item.getres();
                if (currentRes != null && matchesResource(currentRes.name, targetResName)) {
                    continue;
                }
            }

            // Find the item - check belt first, then inventory
            WItem itemToEquip = findItemInBelt(targetResName);
            if (itemToEquip == null) {
                itemToEquip = findItemInInventory(gui, targetResName);
            }

            if (itemToEquip == null) {
                gui.msg("Item not found: " + getItemName(targetResName));
                continue;
            }

            // Equip the item
            equipItem(gui, itemToEquip, targetSlot);
        }

        return Results.SUCCESS();
    }

    private WItem findItemInBelt(String resName) throws InterruptedException {
        WItem beltSlot = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);
        if (beltSlot != null && beltSlot.item.contents instanceof NInventory) {
            NInventory beltInv = (NInventory) beltSlot.item.contents;
            ArrayList<WItem> items = beltInv.getItems();
            for (WItem item : items) {
                Resource res = item.item.getres();
                if (res != null && matchesResource(res.name, resName)) {
                    return item;
                }
            }
        }
        return null;
    }

    private WItem findItemInInventory(NGameUI gui, String resName) throws InterruptedException {
        NInventory inv = gui.getInventory();
        if (inv != null) {
            ArrayList<WItem> items = inv.getItems();
            for (WItem item : items) {
                Resource res = item.item.getres();
                if (res != null && matchesResource(res.name, resName)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void equipItem(NGameUI gui, WItem item, int targetSlot) throws InterruptedException {
        // Check if target slot is occupied and free it
        WItem currentItem = NUtils.getEquipment().findItem(targetSlot);
        if (currentItem != null) {
            freeSlot(gui, currentItem);
        }

        // Send take message to pick up item
        item.item.wdgmsg("take", Coord.z);

        // Wait for item in hand
        NUtils.getUI().core.addTask(new NTask() {
            @Override
            public boolean check() {
                return NUtils.getGameUI().vhand != null;
            }
        });

        WItem handItem = NUtils.getGameUI().vhand;
        if (handItem == null) {
            gui.msg("Failed to pick up item");
            return;
        }

        // Drop on equipment slot
        NUtils.getEquipment().wdgmsg("drop", targetSlot);

        // Wait for item to appear in target slot or leave hand
        final int slot = targetSlot;
        NUtils.getUI().core.addTask(new NTask() {
            @Override
            public boolean check() {
                // Success if item appeared in slot (direct array access, no exception)
                if (NUtils.getEquipment().quickslots[slot] != null) {
                    return true;
                }
                // Also done if hand is empty (item went somewhere)
                return NUtils.getGameUI().vhand == null;
            }
        });

        // If item still in hand, slot rejected it - put back in inventory
        if (NUtils.getGameUI().vhand != null) {
            gui.msg("Cannot equip " + getItemName(handItem.item.getres().name) + " to that slot");
            NInventory inv = gui.getInventory();
            if (inv != null) {
                Coord pos = inv.getFreeCoord(NUtils.getGameUI().vhand);
                if (pos != null) {
                    inv.dropOn(pos);
                    NUtils.getUI().core.addTask(new WaitFreeHand());
                }
            }
        }
    }

    private void freeSlot(NGameUI gui, WItem slotItem) throws InterruptedException {
        // Pick up item from slot
        slotItem.item.wdgmsg("take", Coord.z);

        // Wait for item in hand
        NUtils.getUI().core.addTask(new NTask() {
            @Override
            public boolean check() {
                return NUtils.getGameUI().vhand != null;
            }
        });

        // Put in inventory
        NInventory inv = gui.getInventory();
        if (inv != null) {
            Coord pos = inv.getFreeCoord(NUtils.getGameUI().vhand);
            if (pos != null) {
                inv.dropOn(pos);
                NUtils.getUI().core.addTask(new WaitFreeHand());
            }
        }
    }

    private String getItemName(String resName) {
        if (resName.contains("/")) {
            return resName.substring(resName.lastIndexOf("/") + 1);
        }
        return resName;
    }

    /**
     * Matches resource names accounting for the /small/ variant used in belt/equipment.
     */
    private boolean matchesResource(String actualRes, String configuredRes) {
        if (actualRes == null || configuredRes == null) {
            return false;
        }
        if (actualRes.equals(configuredRes)) {
            return true;
        }
        // Check /small/ variant
        String smallVariant = configuredRes.replace("gfx/invobjs/", "gfx/invobjs/small/");
        if (actualRes.equals(smallVariant)) {
            return true;
        }
        // Check reverse
        String normalVariant = configuredRes.replace("gfx/invobjs/small/", "gfx/invobjs/");
        return actualRes.equals(normalVariant);
    }
}
