package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.conf.CropRegistry;
import nurgling.tasks.NoGob;
import nurgling.tasks.WaitGobModelAttrChange;
import nurgling.tasks.WaitMoreItems;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;

import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class HarvestTrellis implements Action {

    final NArea cropArea;
    final NAlias plantAlias;
    final List<HarvestResultConfig> harvestResults;

    public HarvestTrellis(NArea cropArea, NAlias plantAlias, List<HarvestResultConfig> harvestResults) {
        this.cropArea = cropArea;
        this.plantAlias = plantAlias;
        this.harvestResults = harvestResults;

        // Sort by priority (lower = first)
        this.harvestResults.sort(Comparator.comparingInt(r -> r.priority));
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Get harvest stage from registry
        List<CropRegistry.CropStage> cropStages = CropRegistry.HARVESTABLE.getOrDefault(plantAlias, Collections.emptyList());
        if (cropStages.isEmpty()) {
            return Results.ERROR("No harvest stage defined for " + plantAlias.getDefault());
        }

        int harvestStage = cropStages.get(0).stage;

        // Main harvest loop - continue while harvestable plants exist
        while (hasHarvestablePlants(cropArea, plantAlias, harvestStage)) {
            // Check stamina and drink if needed
            checkStamina(gui);

            // Find harvestable plants
            ArrayList<Gob> plants = Finder.findGobs(cropArea, plantAlias, harvestStage);

            if (plants.isEmpty()) {
                break;
            }

            // Group plants by tile for optimized pathfinding
            Map<Coord, ArrayList<Gob>> plantsByTile = groupGobsByTile(plants);

            // Check if hybrid trellis (plant disappears) or true trellis (plant persists)
            boolean isHybrid = cropStages.get(0).isHybridTrellis;

            // Process each tile
            for (Map.Entry<Coord, ArrayList<Gob>> entry : plantsByTile.entrySet()) {
                Coord tile = entry.getKey();
                ArrayList<Gob> plantsOnTile = entry.getValue();

                // Get first plant for pathfinding
                Gob firstPlant = plantsOnTile.get(0);

                // Calculate pathfinder endpoint at edge of tile
                // For trellis, only check front and back edges (perpendicular to trellis orientation)
                Coord2d tileCenter = tile.mul(MCache.tilesz).add(MCache.tilehsz);

                // Get trellis angle to determine front/back orientation
                double angle = firstPlant.a;

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

                // Navigate to reachable tile edge, or fallback to first plant
                if (pathfinderEndpoint != null) {
                    new PathFinder(pathfinderEndpoint).run(gui);
                } else {
                    new PathFinder(firstPlant).run(gui);
                }

                // Harvest ALL plants on this tile
                for (Gob plant : plantsOnTile) {
                    Gob plantGob = Finder.findGob(plant.id);
                    if (plantGob == null || plantGob.ngob.getModelAttribute() != harvestStage) {
                        continue;
                    }

                    long currentStage = plant.ngob.getModelAttribute();

                    // Harvest the plant
                    new SelectFlowerAction("Harvest", plant).run(gui);

                    if (isHybrid) {
                        // Hybrid trellis: wait for plant to disappear
                        NUtils.getUI().core.addTask(new NoGob(plant.id));
                    } else {
                        // True trellis: wait for stage to reset (plant persists, stage changes)
                        NUtils.getUI().core.addTask(new WaitGobModelAttrChange(plant, currentStage));
                    }

                    // Wait for any harvest item to appear in inventory
                    // For multi-result crops, we wait for the first result config item
                    NUtils.getUI().core.addTask(new WaitMoreItems(gui.getInventory(), harvestResults.get(harvestResults.size() - 1).itemAlias, 1));
                }

                // Drop off after processing each tile
                if (gui.getInventory().getFreeSpace() <= 8) {
                    dropOffAllResults(gui);
                }
            }
        }

        // Final cleanup - drop off any remaining harvest
        dropOffAllResults(gui);

        return Results.SUCCESS();
    }

    private boolean hasHarvestablePlants(NArea area, NAlias plant, int stage) throws InterruptedException {
        return !Finder.findGobs(area, plant, stage).isEmpty();
    }

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

    private void dropOffAllResults(NGameUI gui) throws InterruptedException {
        // Drop off each result in priority order (already sorted)
        for (HarvestResultConfig result : harvestResults) {
            if (gui.getInventory().getItems(result.itemAlias).isEmpty()) {
                continue;  // No items of this type to drop
            }

            if (result.storageBehavior == CropRegistry.StorageBehavior.STOCKPILE) {
                // Drop to stockpile
                new TransferToPiles(result.targetArea.getRCArea(), result.itemAlias).run(gui);
            } else if (result.storageBehavior == CropRegistry.StorageBehavior.BARREL) {
                // Drop to barrels
                dropToBarrels(gui, result.itemAlias, result.targetArea);
            } else if (result.storageBehavior == CropRegistry.StorageBehavior.CONTAINER) {
                // Drop to containers
                dropToContainers(gui, result.itemAlias, result.targetArea);
            }
        }
    }

    private void dropToBarrels(NGameUI gui, NAlias item, NArea barrelArea) throws InterruptedException {
        // Find barrels in storage area
        ArrayList<Gob> barrels = Finder.findGobs(barrelArea, new NAlias("barrel"));

        if (barrels.isEmpty()) {
            throw new RuntimeException("No barrels found in storage area for " + item.getDefault());
        }

        // Try each barrel until items transferred or all full
        for (Gob barrel : barrels) {
            new TransferToBarrel(barrel, item).run(gui);

            // If inventory empty, we're done
            if (gui.getInventory().getItems(item).isEmpty()) {
                return;
            }
        }

        // If still items remaining, all barrels full
        if (!gui.getInventory().getItems(item).isEmpty()) {
            throw new RuntimeException("All barrels full, cannot store " + item.getDefault());
        }
    }

    private void dropToContainers(NGameUI gui, NAlias item, NArea containerArea) throws InterruptedException {
        // Find all containers in the area (using Context.contcaps pattern from HarvestCrop)
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(containerArea.getRCArea(),
                                       new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }

        if (containers.isEmpty()) {
            throw new RuntimeException("No containers found in storage area for " + item.getDefault());
        }

        // Try each container sequentially until all items transferred
        for (Container container : containers) {
            // Skip if no items left to transfer
            if (gui.getInventory().getItems(item).isEmpty()) {
                return;
            }

            // Transfer items to this container (partial fill is OK)
            new TransferToContainer(container, item).run(gui);
            new CloseTargetContainer(container).run(gui);

            // Container might be full now, but we continue to next container if items remain
        }

        // After trying all containers, check if items still remain
        if (!gui.getInventory().getItems(item).isEmpty()) {
            throw new RuntimeException("All containers full, cannot store " + item.getDefault());
        }
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
