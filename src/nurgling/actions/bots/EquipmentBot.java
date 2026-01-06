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
import java.util.HashSet;
import java.util.Map;

public class EquipmentBot implements Action {

    private final EquipmentPreset preset;

    private static class ItemLocation {
        final WItem item;
        final boolean fromBelt;
        final Coord beltCoord;

        ItemLocation(WItem item, boolean fromBelt, Coord beltCoord) {
            this.item = item;
            this.fromBelt = fromBelt;
            this.beltCoord = beltCoord;
        }
    }

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

        for (Map.Entry<Integer, String> entry : slotConfig.entrySet()) {
            int targetSlot = entry.getKey();
            String targetResName = entry.getValue();

            WItem currentItem = NUtils.getEquipment().findItem(targetSlot);
            if (currentItem != null) {
                Resource currentRes = currentItem.item.getres();
                if (currentRes != null && matchesResource(currentRes.name, targetResName)) {
                    continue;
                }
            }

            ItemLocation itemLoc = findItemInBelt(targetResName);
            if (itemLoc == null) {
                itemLoc = findItemInInventory(gui, targetResName);
            }

            if (itemLoc == null) {
                gui.msg("Item not found: " + getItemName(targetResName));
                continue;
            }

            equipItem(gui, itemLoc, targetSlot);
        }

        return Results.SUCCESS();
    }

    private ItemLocation findItemInBelt(String resName) throws InterruptedException {
        WItem beltSlot = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);
        if (beltSlot != null && beltSlot.item.contents instanceof NInventory) {
            NInventory beltInv = (NInventory) beltSlot.item.contents;
            ArrayList<WItem> items = beltInv.getItems();
            for (WItem item : items) {
                Resource res = item.item.getres();
                if (res != null && matchesResource(res.name, resName)) {
                    Coord beltCoord = item.c.div(Inventory.sqsz);
                    return new ItemLocation(item, true, beltCoord);
                }
            }
        }
        return null;
    }

    private ItemLocation findItemInInventory(NGameUI gui, String resName) throws InterruptedException {
        NInventory inv = gui.getInventory();
        if (inv != null) {
            ArrayList<WItem> items = inv.getItems();
            for (WItem item : items) {
                Resource res = item.item.getres();
                if (res != null && matchesResource(res.name, resName)) {
                    return new ItemLocation(item, false, null);
                }
            }
        }
        return null;
    }

    private void equipItem(NGameUI gui, ItemLocation itemLoc, int targetSlot) throws InterruptedException {
        boolean isTwoHanded = isTwoHanded(itemLoc.item);

        // If two-handed, need to free the OTHER hand slot first
        if (isTwoHanded && (targetSlot == NEquipory.Slots.HAND_LEFT.idx || targetSlot == NEquipory.Slots.HAND_RIGHT.idx)) {
            int otherHandSlot = (targetSlot == NEquipory.Slots.HAND_LEFT.idx)
                ? NEquipory.Slots.HAND_RIGHT.idx
                : NEquipory.Slots.HAND_LEFT.idx;

            WItem otherHandItem = NUtils.getEquipment().findItem(otherHandSlot);
            if (otherHandItem != null) {
                freeSlot(gui, otherHandItem);
            }
        }

        // Check if target slot is occupied
        WItem currentItem = NUtils.getEquipment().findItem(targetSlot);

        if (currentItem != null && itemLoc.fromBelt) {
            // SWAP: Drop equipped item directly onto belt item to swap them
            // This puts the belt item in hand and equipped item goes to belt
            currentItem.item.wdgmsg("take", Coord.z);

            NUtils.getUI().core.addTask(new NTask() {
                @Override
                public boolean check() {
                    return NUtils.getGameUI().vhand != null;
                }
            });

            if (NUtils.getGameUI().vhand == null) {
                gui.msg("Failed to pick up equipped item");
                return;
            }

            // Drop on the belt item - this swaps them
            NInventory beltInv = getBeltInventory();
            if (beltInv != null) {
                beltInv.wdgmsg("drop", itemLoc.beltCoord);

                // Wait for swap to complete (hand should now have the belt item)
                NUtils.getUI().core.addTask(new NTask() {
                    private int ticks = 0;
                    @Override
                    public boolean check() {
                        ticks++;
                        // The swap changes what's in hand
                        WItem hand = NUtils.getGameUI().vhand;
                        if (hand != null) {
                            Resource res = hand.item.getres();
                            if (res != null && matchesResource(res.name, itemLoc.item.item.getres().name)) {
                                return true; // Now holding the item we want to equip
                            }
                        }
                        return ticks > 30;
                    }
                });
            }
        } else if (currentItem != null) {
            // Not from belt - free the slot normally
            freeSlot(gui, currentItem);

            // Pick up item from inventory
            itemLoc.item.item.wdgmsg("take", Coord.z);

            NUtils.getUI().core.addTask(new NTask() {
                @Override
                public boolean check() {
                    return NUtils.getGameUI().vhand != null;
                }
            });
        } else {
            // Slot is empty - just pick up item
            itemLoc.item.item.wdgmsg("take", Coord.z);

            NUtils.getUI().core.addTask(new NTask() {
                @Override
                public boolean check() {
                    return NUtils.getGameUI().vhand != null;
                }
            });
        }

        WItem handItem = NUtils.getGameUI().vhand;
        if (handItem == null) {
            gui.msg("Failed to pick up item");
            return;
        }

        // Drop on equipment slot
        NUtils.getEquipment().wdgmsg("drop", targetSlot);

        final int slot = targetSlot;
        NUtils.getUI().core.addTask(new NTask() {
            @Override
            public boolean check() {
                if (NUtils.getEquipment().quickslots[slot] != null) {
                    return true;
                }
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

    private NInventory getBeltInventory() throws InterruptedException {
        WItem beltSlot = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);
        if (beltSlot != null && beltSlot.item.contents instanceof NInventory) {
            return (NInventory) beltSlot.item.contents;
        }
        return null;
    }

    private void freeSlot(NGameUI gui, WItem slotItem) throws InterruptedException {
        slotItem.item.wdgmsg("take", Coord.z);

        NUtils.getUI().core.addTask(new NTask() {
            @Override
            public boolean check() {
                return NUtils.getGameUI().vhand != null;
            }
        });

        if (NUtils.getGameUI().vhand == null) {
            return;
        }

        // Try to put on belt first
        NInventory beltInv = getBeltInventory();
        if (beltInv != null) {
            int freeSpace = beltInv.getFreeSpace();

            if (freeSpace > 0) {
                if (tryTransferToBelt()) {
                    return;
                }
            }
        }

        // Fall back to inventory
        NInventory inv = gui.getInventory();
        if (inv != null) {
            Coord pos = inv.getFreeCoord(NUtils.getGameUI().vhand);
            if (pos != null) {
                inv.dropOn(pos);
                NUtils.getUI().core.addTask(new WaitFreeHand());
            }
        }
    }

    private boolean tryTransferToBelt() throws InterruptedException {
        if (NUtils.getGameUI().vhand == null) {
            return false;
        }

        NUtils.transferToBelt();

        NUtils.getUI().core.addTask(new NTask() {
            private int ticks = 0;
            @Override
            public boolean check() {
                ticks++;
                if (NUtils.getGameUI().vhand == null) {
                    return true;
                }
                return ticks > 30;
            }
        });

        return NUtils.getGameUI().vhand == null;
    }

    private String getItemName(String resName) {
        if (resName.contains("/")) {
            return resName.substring(resName.lastIndexOf("/") + 1);
        }
        return resName;
    }

    private boolean matchesResource(String actualRes, String configuredRes) {
        if (actualRes == null || configuredRes == null) {
            return false;
        }
        if (actualRes.equals(configuredRes)) {
            return true;
        }
        String smallVariant = configuredRes.replace("gfx/invobjs/", "gfx/invobjs/small/");
        if (actualRes.equals(smallVariant)) {
            return true;
        }
        String normalVariant = configuredRes.replace("gfx/invobjs/small/", "gfx/invobjs/");
        return actualRes.equals(normalVariant);
    }

    private static final HashSet<String> TWO_HANDED_ITEMS = new HashSet<>();
    static {
        TWO_HANDED_ITEMS.add("Scythe");
        TWO_HANDED_ITEMS.add("Pickaxe");
        TWO_HANDED_ITEMS.add("Glass Blowing Rod");
        TWO_HANDED_ITEMS.add("Boar Spear");
        TWO_HANDED_ITEMS.add("Metal Shovel");
        TWO_HANDED_ITEMS.add("Tinker's Shovel");
        TWO_HANDED_ITEMS.add("Wooden Shovel");
        TWO_HANDED_ITEMS.add("Dowsing Rod");
        TWO_HANDED_ITEMS.add("Battle Axe of the Twelfth Bay");
        TWO_HANDED_ITEMS.add("Cutblade");
    }

    private boolean isTwoHanded(WItem item) {
        if (item == null || item.item == null) {
            return false;
        }
        String name = ((NGItem) item.item).name();
        return TWO_HANDED_ITEMS.contains(name);
    }
}
