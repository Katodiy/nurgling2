package nurgling.actions;

import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.conf.CropRegistry;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.Collections;
import java.util.List;

public class ValidateAllCropsReady implements Action {

    private final NArea field;
    private final NAlias crop;

    public ValidateAllCropsReady(NArea field, NAlias crop) {
        this.field = field;
        this.crop = crop;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        List<CropRegistry.CropStage> cropStages = CropRegistry.HARVESTABLE.getOrDefault(crop, Collections.emptyList());

        if (cropStages.isEmpty()) {
            return Results.FAIL();
        }

        int totalCropCount = Finder.findGobs(field, crop).size();
        if (totalCropCount == 0) {
            return Results.SUCCESS();
        }

        int readyCropCount = 0;
        for (CropRegistry.CropStage stage : cropStages) {
            readyCropCount += Finder.findGobs(field, crop, stage.stage).size();
        }

        if (readyCropCount < totalCropCount) {
            return Results.FAIL();
        }

        return Results.SUCCESS();
    }
}