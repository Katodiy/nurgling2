package nurgling.actions.bots;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.tasks.ISRemoved;
import nurgling.tasks.StackSizeChanged;
import nurgling.tasks.WaitFreeHand;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;

/**
 * Collects highest quality Silkworm Eggs from containers in a selected area.
 *
 * This bot considers individual egg quality within stacks, not the average.
 * It collects the GLOBALLY highest quality eggs across ALL containers.
 *
 * For example, if Container A has eggs Q20, Q15, Q10 and Container B has Q20, Q18, Q16,
 * the collection order will be: Q20(A), Q20(B), Q18(B), Q16(B), Q15(A), Q10(A)
 *
 * Stops when player inventory is full.
 */
public class CollectHighestQualitySilkwormEggs implements Action {

    private static final NAlias EGG_ALIAS = new NAlias("Silkworm Egg");

    /**
     * Represents a single egg found during scanning.
     */
    private static class EggInfo implements Comparable<EggInfo> {
        final float quality;
        final long containerGobId;
        final String containerCap;

        EggInfo(float quality, long containerGobId, String containerCap) {
            this.quality = quality;
            this.containerGobId = containerGobId;
            this.containerCap = containerCap;
        }

        @Override
        public int compareTo(EggInfo other) {
            // Descending order - highest quality first
            return Float.compare(other.quality, this.quality);
        }
    }

    /**
     * Comparator for sorting items by quality (descending).
     */
    private static final Comparator<WItem> QUALITY_DESC = (a, b) -> {
        Float qA = ((NGItem) a.item).quality;
        Float qB = ((NGItem) b.item).quality;
        if (qA == null && qB == null) return 0;
        if (qA == null) return 1;  // null quality goes to end
        if (qB == null) return -1;
        return Float.compare(qB, qA);  // Descending
    };

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Phase 0: Area selection
        gui.msg("Please select area containing containers with Silkworm Eggs");
        SelectArea selectArea = new SelectArea(Resource.loadsimg("baubles/inputArea"));
        selectArea.run(gui);
        Pair<Coord2d, Coord2d> area = selectArea.getRCArea();

        if (area == null) {
            return Results.ERROR("No area selected");
        }

        // Phase 1: Scan all containers and build global quality list
        gui.msg("Scanning containers for Silkworm Eggs...");
        List<EggInfo> allEggs = scanAllContainers(gui, area);

        if (allEggs.isEmpty()) {
            gui.msg("No Silkworm Eggs found in any containers");
            return Results.SUCCESS();
        }

        // Sort globally by quality (descending)
        Collections.sort(allEggs);

        gui.msg("Found " + allEggs.size() + " eggs. Collecting highest quality first...");

        // Phase 2: Collect eggs in global quality order
        int totalCollected = collectInQualityOrder(gui, allEggs);

        gui.msg("Collection complete. Collected " + totalCollected + " eggs.");
        return Results.SUCCESS();
    }

    /**
     * Scans all containers in the area and builds a global list of all eggs with quality info.
     */
    private List<EggInfo> scanAllContainers(NGameUI gui, Pair<Coord2d, Coord2d> area)
            throws InterruptedException {
        List<EggInfo> allEggs = new ArrayList<>();

        // Find all containers in area
        ArrayList<Gob> containerGobs = Finder.findGobs(
                area,
                new NAlias(new ArrayList<>(NContext.contcaps.keySet()))
        );

        for (Gob gob : containerGobs) {
            String cap = NContext.contcaps.get(gob.ngob.name);
            if (cap == null) continue;

            Container container = new Container(gob, cap, null);

            // Navigate and open
            new PathFinder(gob).run(gui);
            new OpenTargetContainer(container).run(gui);

            NInventory inv = gui.getInventory(cap);
            if (inv != null) {
                ArrayList<WItem> eggs = inv.getItems(EGG_ALIAS);

                // Record each individual egg
                for (WItem egg : eggs) {
                    Float quality = ((NGItem) egg.item).quality;
                    if (quality != null && quality > 0) {
                        allEggs.add(new EggInfo(quality, gob.id, cap));
                    }
                }
            }

            new CloseTargetContainer(container).run(gui);
        }

        return allEggs;
    }

    /**
     * Collects eggs in global quality order, optimizing container access.
     * Returns the number of eggs collected.
     */
    private int collectInQualityOrder(NGameUI gui, List<EggInfo> sortedEggs)
            throws InterruptedException {
        int collected = 0;
        long currentContainerGobId = -1;
        Container currentContainer = null;

        for (EggInfo eggInfo : sortedEggs) {
            // Check if player inventory has space
            if (gui.getInventory().getFreeSpace() == 0) {
                // Close current container if open
                if (currentContainer != null) {
                    new CloseTargetContainer(currentContainer).run(gui);
                }
                gui.msg("Inventory full.");
                return collected;
            }

            // Switch containers if needed
            if (eggInfo.containerGobId != currentContainerGobId) {
                // Close previous container
                if (currentContainer != null) {
                    new CloseTargetContainer(currentContainer).run(gui);
                }

                // Open new container
                Gob gob = Finder.findGob(eggInfo.containerGobId);
                if (gob == null) {
                    continue; // Container no longer exists
                }

                currentContainer = new Container(gob, eggInfo.containerCap, null);
                currentContainerGobId = eggInfo.containerGobId;

                new PathFinder(gob).run(gui);
                new OpenTargetContainer(currentContainer).run(gui);
            }

            // Get the highest quality egg currently in this container and take it
            // (We can't use the original WItem reference as it may be invalid)
            NInventory containerInv = gui.getInventory(eggInfo.containerCap);
            if (containerInv == null) {
                continue;
            }

            ArrayList<WItem> currentEggs = containerInv.getItems(EGG_ALIAS);
            if (currentEggs.isEmpty()) {
                continue;
            }

            // Sort by quality and take the best one
            currentEggs.sort(QUALITY_DESC);
            WItem bestEgg = currentEggs.get(0);

            // Verify the quality matches what we expect (within tolerance)
            Float actualQuality = ((NGItem) bestEgg.item).quality;
            if (actualQuality == null) {
                continue;
            }

            // Take the egg
            if (takeEggToPlayerInventory(bestEgg, gui)) {
                collected++;
            }
        }

        // Close last container
        if (currentContainer != null) {
            new CloseTargetContainer(currentContainer).run(gui);
        }

        return collected;
    }

    /**
     * Takes a single egg from a container to the player inventory.
     * Handles both stacked and non-stacked items.
     * Returns true if successful.
     */
    private boolean takeEggToPlayerInventory(WItem item, NGameUI gui)
            throws InterruptedException {
        if (item.parent instanceof ItemStack) {
            // Item is inside a stack - use hand-based transfer
            ItemStack sourceStack = (ItemStack) item.parent;
            int originalSize = sourceStack.wmap.size();

            NUtils.takeItemToHand(item);
            NUtils.dropToInv(gui.getInventory());
            NUtils.addTask(new WaitFreeHand());

            // Wait for stack to update
            if (originalSize > 1) {
                NUtils.addTask(new StackSizeChanged(sourceStack, originalSize));
            }
        } else {
            // Single item (not in a stack) - use simple transfer
            int itemId = item.item.wdgid();
            item.item.wdgmsg("transfer", Coord.z);
            NUtils.addTask(new ISRemoved(itemId));
        }

        return true;
    }
}
