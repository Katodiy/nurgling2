package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.conf.CropRegistry;
import nurgling.tasks.WaitGobModelAttrChange;
import nurgling.tasks.WaitMoreItems;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;

public class HarvestTrellis implements Action {

    final NArea cropArea;
    final NArea putArea;
    final NAlias plantAlias;
    final NAlias harvestResult;

    public HarvestTrellis(NArea cropArea, NArea putArea, NAlias plantAlias, NAlias harvestResult) {
        this.cropArea = cropArea;
        this.putArea = putArea;
        this.plantAlias = plantAlias;
        this.harvestResult = harvestResult;
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

            // Harvest first plant
            Gob plant = plants.get(0);
            long currentStage = plant.ngob.getModelAttribute();

            // Navigate to plant
            new PathFinder(plant).run(gui);

            // Harvest the plant
            new SelectFlowerAction("Harvest", plant).run(gui);

            // Wait for stage to reset (plant persists, stage changes)
            NUtils.getUI().core.addTask(new WaitGobModelAttrChange(plant, currentStage));

            // Wait for harvest item to appear in inventory
            NUtils.getUI().core.addTask(new WaitMoreItems(gui.getInventory(), harvestResult, 1));

            // Check if we need to drop off
            if (gui.getInventory().getFreeSpace() < 3) {
                dropOffHarvest(gui);
            }
        }

        // Final cleanup - drop off any remaining harvest
        dropOffHarvest(gui);

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

    private void dropOffHarvest(NGameUI gui) throws InterruptedException {
        if (!gui.getInventory().getItems(harvestResult).isEmpty()) {
            new TransferToPiles(putArea.getRCArea(), harvestResult).run(gui);
        }
    }
}
