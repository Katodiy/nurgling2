package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.tasks.WaitPlantOnTrellis;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;

import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class PlantTrellis implements Action {
    private static final NAlias TRELLIS_ALIAS = new NAlias("gfx/terobjs/plants/trellis");

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

                // Plant ALL empty trellis on this tile
                for (Gob trellis : trellisOnTile) {
                    ArrayList<WItem> seeds = gui.getInventory().getItems(seedAlias);
                    if (seeds.isEmpty()) {
                        break;  // Out of seeds, will refetch in next iteration
                    }

                    // Count plants before planting
                    int plantCountBefore = countPlantsOnTile(tile);

                    // Take seed to hand and plant ON trellis gob
                    NUtils.takeItemToHand(seeds.get(0));
                    NUtils.activateItem(trellis, false);

                    // Wait for plant to appear (count increased by 1)
                    NUtils.getUI().core.addTask(new WaitPlantOnTrellis(tile, plantAlias, plantCountBefore + 1));
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
     * Fetches as many seeds as possible from PUT area stockpiles.
     * Fills inventory to maximize seeds per trip.
     */
    private void fetchSeeds(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> seedPiles = Finder.findGobs(seedPutArea, seedAlias);

        if (seedPiles.isEmpty()) {
            throw new RuntimeException("No " + seedAlias.getDefault() + " available in PUT area for replanting!");
        }

        // Calculate how many seeds we can take based on free inventory space
        int freeSpace = gui.getInventory().getFreeSpace();
        int seedsToTake = freeSpace;

        if (seedsToTake == 0) {
            throw new RuntimeException("No inventory space for seeds!");
        }

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
     * Drops off any remaining seeds back to the PUT area stockpile.
     * Called after all planting is complete to return unused seeds.
     */
    private void dropOffRemainingSeeds(NGameUI gui) throws InterruptedException {
        // Check if there are any seeds in inventory
        if (gui.getInventory().getItems(seedAlias).isEmpty()) {
            return;  // No seeds to return
        }

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
}
