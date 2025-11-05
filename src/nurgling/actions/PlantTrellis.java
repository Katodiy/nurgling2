package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.WaitPlantOnTrellis;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;

import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class PlantTrellis implements Action {
    private static final NAlias TRELLIS_ALIAS = new NAlias("gfx/terobjs/plants/trellis");

    private enum StorageType {
        BARREL,
        STOCKPILE
    }

    private final NArea cropArea;
    private final NAlias plantAlias;
    private final NAlias seedAlias;
    private final NArea seedPutArea;

    public PlantTrellis(NArea cropArea, NAlias plantAlias, NAlias seedAlias, NArea seedPutArea) {
        this.cropArea = cropArea;
        this.plantAlias = plantAlias;
        this.seedAlias = seedAlias;
        this.seedPutArea = seedPutArea;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        replantEmptyTrellis(gui);

        // Drop any seeds remaining in hand back to inventory
        if (gui.vhand != null) {
            NUtils.dropToInv();
        }

        // Drop off any remaining seeds back to PUT area
        dropOffRemainingSeeds(gui);

        return Results.SUCCESS();
    }

    /**
     * Replants all empty trellis in the crop area.
     * Optimized: Fetch full inventory of seeds, plant until empty, refetch if more empty trellis.
     */
    private void replantEmptyTrellis(NGameUI gui) throws InterruptedException {
        while (true) {
            // Find all empty trellis (no plant on them)
            ArrayList<Gob> emptyTrellis = findEmptyTrellis();

            if (emptyTrellis.isEmpty()) {
                return;  // All trellis planted
            }

            // Check stamina before starting planting batch
            checkStamina(gui);

            // Fetch full inventory of seeds if needed
            if (gui.getInventory().getItems(seedAlias).isEmpty()) {
                fetchSeeds(gui);
            }

            // Group empty trellis by tile for optimized pathfinding
            Map<Coord, ArrayList<Gob>> trellisByTile = groupGobsByTile(emptyTrellis);

            // Process each tile
            for (Map.Entry<Coord, ArrayList<Gob>> entry : trellisByTile.entrySet()) {
                Coord tile = entry.getKey();
                ArrayList<Gob> trellisOnTile = entry.getValue();

                // Get first trellis for pathfinding
                Gob firstTrellis = trellisOnTile.get(0);

                // Calculate pathfinder endpoint at edge of tile
                // For trellis, only check front and back edges (perpendicular to trellis orientation)
                Coord2d tileCenter = tile.mul(MCache.tilesz).add(MCache.tilehsz);

                // Get trellis angle to determine front/back orientation
                double angle = firstTrellis.a;

                // Calculate front and back positions (perpendicular to trellis face)
                // Front is in direction of angle, back is opposite (angle + PI)
                double frontX = tileCenter.x + Math.cos(angle) * MCache.tilehsz.x;
                double frontY = tileCenter.y + Math.sin(angle) * MCache.tilehsz.y;
                double backX = tileCenter.x + Math.cos(angle + Math.PI) * MCache.tilehsz.x;
                double backY = tileCenter.y + Math.sin(angle + Math.PI) * MCache.tilehsz.y;

                Coord2d[] trellisFrontBack = new Coord2d[] {
                    new Coord2d(frontX, frontY),  // Front edge
                    new Coord2d(backX, backY)     // Back edge
                };

                Coord2d pathfinderEndpoint = null;
                for (Coord2d edge : trellisFrontBack) {
                    if (PathFinder.isAvailable(edge)) {
                        pathfinderEndpoint = edge;
                        break;
                    }
                }

                // Navigate to reachable tile edge, or fallback to first trellis
                if (pathfinderEndpoint != null) {
                    new PathFinder(pathfinderEndpoint).run(gui);
                } else {
                    new PathFinder(firstTrellis).run(gui);
                }

                // Check stamina after pathfinding to tile
                checkStamina(gui);

                // Detect seed type and use appropriate planting method
                ArrayList<WItem> seeds = gui.getInventory().getItems(seedAlias);
                if (seeds.isEmpty()) {
                    break; // Out of seeds, will refetch in next outer loop iteration
                }

                boolean isStacked = isSeedStacked(seeds);

                if (isStacked) {
                    plantTrellisWithStackedSeeds(gui, trellisOnTile, tile);
                } else {
                    plantTrellisWithIndividualSeeds(gui, trellisOnTile, tile);
                }
            }

            // If inventory still has seeds, all trellis planted
            if (!gui.getInventory().getItems(seedAlias).isEmpty()) {
                return;
            }
        }
    }

    /**
     * Finds all empty trellis (trellis without plants on them).
     */
    private ArrayList<Gob> findEmptyTrellis() throws InterruptedException {
        ArrayList<Gob> allTrellis = Finder.findGobs(cropArea, TRELLIS_ALIAS);
        ArrayList<Gob> plants = Finder.findGobs(cropArea, plantAlias);
        ArrayList<Gob> emptyTrellis = new ArrayList<>();

        for (Gob trellis : allTrellis) {
            if (!hasPlantOnTrellis(trellis, plants)) {
                emptyTrellis.add(trellis);
            }
        }

        return emptyTrellis;
    }

    /**
     * Checks if a trellis has a plant on it.
     * Plant is on trellis if within 1 tile distance (11 units).
     */
    private boolean hasPlantOnTrellis(Gob trellis, ArrayList<Gob> plants) {
        for (Gob plant : plants) {
            if (plant.rc.dist(trellis.rc) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the number of plants on a specific tile.
     * Used to track plant appearance after planting.
     */
    private int countPlantsOnTile(Coord tile) throws InterruptedException {
        ArrayList<Gob> allPlants = Finder.findGobs(cropArea, plantAlias);
        int count = 0;
        for (Gob plant : allPlants) {
            if (plant.rc.floor(tilesz).equals(tile)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Fetches seeds from storage area.
     * Automatically detects storage type (barrels or stockpiles) and uses appropriate method.
     */
    private void fetchSeeds(NGameUI gui) throws InterruptedException {
        int freeSpace = gui.getInventory().getFreeSpace();
        if (freeSpace == 0) {
            throw new RuntimeException("No inventory space for seeds!");
        }

        StorageType storageType = detectStorageType();

        switch (storageType) {
            case BARREL:
                fetchSeedsFromBarrels(gui);
                break;
            case STOCKPILE:
                fetchSeedsFromStockpiles(gui);
                break;
        }
    }

    /**
     * Fetches seeds from barrels in seed specialization area.
     * Pattern from SeedCrop.java - uses TakeFromBarrel action.
     */
    private void fetchSeedsFromBarrels(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> barrels = Finder.findGobs(seedPutArea, new NAlias("barrel"));

        // Try to fetch seeds from each barrel until inventory has seeds
        for (Gob barrel : barrels) {
            // Stop if we have seeds in inventory
            if (!gui.getInventory().getItems(seedAlias).isEmpty()) {
                return;
            }

            // Check if barrel has content before attempting to take
            if (NUtils.barrelHasContent(barrel)) {
                // Use TakeFromBarrel action without count (takes all available)
                new TakeFromBarrel(barrel, seedAlias).run(gui);
            }
        }

        // If still no seeds after trying all barrels, throw error
        if (gui.getInventory().getItems(seedAlias).isEmpty()) {
            throw new RuntimeException("No " + seedAlias.getDefault() + " available in barrels for replanting!");
        }
    }

    /**
     * Fetches seeds from stockpiles in PUT area.
     * Original PlantTrellis implementation preserved for backward compatibility.
     */
    private void fetchSeedsFromStockpiles(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> seedPiles = Finder.findGobs(seedPutArea, seedAlias);

        if (seedPiles.isEmpty()) {
            throw new RuntimeException("No " + seedAlias.getDefault() + " available in stockpiles for replanting!");
        }

        // Calculate how many seeds we can take based on free inventory space
        int freeSpace = gui.getInventory().getFreeSpace();
        int seedsToTake = freeSpace;

        // Navigate to first seed pile, open stockpile, and take seeds
        Gob pile = seedPiles.get(0);
        new PathFinder(pile).run(gui);
        new OpenTargetContainer("Stockpile", pile).run(gui);
        new TakeItemsFromPile(pile, gui.getStockpile(), seedsToTake).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
    }

    /**
     * Checks stamina and drinks if needed.
     * Same pattern as HarvestTrellis.
     */
    private void checkStamina(NGameUI gui) throws InterruptedException {
        if (NUtils.getStamina() < 0.35) {
            if (!new Drink(0.9, false).run(gui).isSuccess) {
                if ((Boolean) NConfig.get(NConfig.Key.harvestautorefill)) {
                    if (FillWaterskins.checkIfNeed()) {
                        if (!(new FillWaterskins(true).run(gui).IsSuccess()))
                            throw new InterruptedException();
                        else if (!new Drink(0.9, false).run(gui).isSuccess)
                            throw new InterruptedException();
                    }
                } else {
                    throw new InterruptedException();
                }
            }
        }
    }

    /**
     * Drops off any remaining seeds back to storage area.
     * Automatically detects storage type (barrels or stockpiles) and uses appropriate method.
     * Called after all planting is complete to return unused seeds.
     */
    private void dropOffRemainingSeeds(NGameUI gui) throws InterruptedException {
        // Check if there are any seeds in inventory
        if (gui.getInventory().getItems(seedAlias).isEmpty()) {
            return;  // No seeds to return
        }

        StorageType storageType = detectStorageType();

        switch (storageType) {
            case BARREL:
                dropOffSeedsToBarrels(gui);
                break;
            case STOCKPILE:
                dropOffSeedsToStockpiles(gui);
                break;
        }
    }

    /**
     * Drops off seeds to barrels in seed specialization area.
     * Pattern from SeedCrop.java - uses TransferToBarrel action.
     */
    private void dropOffSeedsToBarrels(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> barrels = Finder.findGobs(seedPutArea, new NAlias("barrel"));

        if (barrels.isEmpty()) {
            throw new RuntimeException("No barrels found in seed area for drop-off!");
        }

        // Transfer seeds to barrels until inventory empty or all barrels full
        for (Gob barrel : barrels) {
            if (gui.getInventory().getItems(seedAlias).isEmpty()) {
                return;  // All seeds transferred
            }

            TransferToBarrel tb = new TransferToBarrel(barrel, seedAlias);
            tb.run(gui);

            // If this barrel is now full, try next barrel
            if (!tb.isFull()) {
                return;  // Barrel has space, all seeds should be transferred
            }
        }
    }

    /**
     * Drops off seeds to stockpiles in PUT area.
     * Original PlantTrellis implementation preserved for backward compatibility.
     */
    private void dropOffSeedsToStockpiles(NGameUI gui) throws InterruptedException {
        // Transfer remaining seeds to stockpile
        new TransferToPiles(seedPutArea.getRCArea(), seedAlias).run(gui);
    }

    /**
     * Groups gobs by their tile coordinate for optimized pathfinding.
     * Instead of pathfinding to each gob individually, we pathfind once per tile
     * and process all gobs on that tile.
     *
     * @param gobs List of gobs to group
     * @return Map of tile coordinates to lists of gobs on that tile
     */
    private Map<Coord, ArrayList<Gob>> groupGobsByTile(ArrayList<Gob> gobs) {
        Map<Coord, ArrayList<Gob>> gobsByTile = new LinkedHashMap<>();

        for (Gob gob : gobs) {
            Coord tile = gob.rc.floor(tilesz);
            gobsByTile.computeIfAbsent(tile, k -> new ArrayList<>()).add(gob);
        }

        return gobsByTile;
    }

    /**
     * Detects the storage type in the seed area (barrels or stockpiles).
     * Barrels are used in seed specialization areas (field crop pattern).
     * Stockpiles are used in PUT areas (trellis output pattern).
     *
     * @return StorageType.BARREL if barrels found, StorageType.STOCKPILE if stockpiles found
     * @throws RuntimeException if no storage containers found
     */
    private StorageType detectStorageType() throws InterruptedException {
        ArrayList<Gob> barrels = Finder.findGobs(seedPutArea, new NAlias("barrel"));
        ArrayList<Gob> stockpiles = Finder.findGobs(seedPutArea, new NAlias("stockpile"));

        if (!barrels.isEmpty()) {
            return StorageType.BARREL;
        } else if (!stockpiles.isEmpty()) {
            return StorageType.STOCKPILE;
        } else {
            throw new RuntimeException("No storage containers (barrels or stockpiles) found in seed area!");
        }
    }

    /**
     * Checks if seeds come in stacks (have Amount info).
     * Stacked seeds (from barrels) have GItem.Amount.
     * Individual seeds (from stockpiles) do not.
     *
     * @param seeds List of seed items in inventory
     * @return true if seeds are stacked (from barrels), false if individual (from stockpiles)
     */
    private boolean isSeedStacked(ArrayList<WItem> seeds) {
        if (seeds.isEmpty()) {
            return false;
        }

        WItem firstSeed = seeds.get(0);
        GItem.Amount amount = ((NGItem) firstSeed.item).getInfo(GItem.Amount.class);
        return amount != null;
    }

    /**
     * Plants trellis using stacked seeds (from barrels).
     * Takes stack to hand once, then plants multiple trellis until stack depletes.
     * Handles insufficient stack amounts (< 5 seeds) by dropping back to inventory.
     */
    private void plantTrellisWithStackedSeeds(NGameUI gui, ArrayList<Gob> trellisOnTile, Coord tile)
            throws InterruptedException {

        // Take first seed stack to hand (if hand empty)
        if (gui.vhand == null) {
            ArrayList<WItem> seeds = gui.getInventory().getItems(seedAlias);
            if (!seeds.isEmpty()) {
                NUtils.takeItemToHand(seeds.get(0));
            } else {
                return; // No seeds available
            }
        }

        // Plant all trellis on this tile
        for (Gob trellis : trellisOnTile) {
            // Check if hand has enough seeds (need 5 to plant on trellis)
            if (gui.vhand != null) {
                GItem.Amount amount = ((NGItem) gui.vhand.item).getInfo(GItem.Amount.class);
                if (amount != null && amount.itemnum() < 5) {
                    // Not enough seeds in hand, drop back to inventory
                    NUtils.dropToInv();
                    // Hand is now empty, will pick up next stack below
                }
            }

            // Check if hand is empty (stack depleted or dropped due to insufficient amount)
            if (gui.vhand == null) {
                // Try to get more seeds
                ArrayList<WItem> seeds = gui.getInventory().getItems(seedAlias);
                if (seeds.isEmpty()) {
                    return; // No more seeds, will refetch in outer loop
                }
                // Take next stack to hand
                NUtils.takeItemToHand(seeds.get(0));
            }

            // Count plants before planting
            int plantCountBefore = countPlantsOnTile(tile);

            // Activate on trellis (consumes 5 seeds from stack in hand)
            NUtils.activateItem(trellis, false);

            // Wait for plant to appear
            NUtils.getUI().core.addTask(new WaitPlantOnTrellis(tile, plantAlias, plantCountBefore + 1));

            // NOTE: Stack stays in hand, will be used for next trellis
            // When stack depletes to 0, hand becomes free automatically
        }
    }

    /**
     * Plants trellis using individual seeds (from stockpiles).
     * Takes one seed to hand per trellis, waits for hand free after each plant.
     * This preserves the original PlantTrellis behavior for backward compatibility.
     */
    private void plantTrellisWithIndividualSeeds(NGameUI gui, ArrayList<Gob> trellisOnTile, Coord tile)
            throws InterruptedException {

        for (Gob trellis : trellisOnTile) {
            ArrayList<WItem> seeds = gui.getInventory().getItems(seedAlias);
            if (seeds.isEmpty()) {
                return; // Out of seeds, will refetch in outer loop
            }

            // Count plants before planting
            int plantCountBefore = countPlantsOnTile(tile);

            // Take seed to hand and plant ON trellis gob
            NUtils.takeItemToHand(seeds.get(0));
            NUtils.activateItem(trellis, false);
            NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));

            // Wait for plant to appear
            NUtils.getUI().core.addTask(new WaitPlantOnTrellis(tile, plantAlias, plantCountBefore + 1));
        }
    }
}
