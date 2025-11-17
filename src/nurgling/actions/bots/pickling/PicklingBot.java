package nurgling.actions.bots.pickling;

import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.Results;

public class PicklingBot implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (true) {
            // Phase 1: Fill pickling jars with fresh inputs (We have to do this to make sure we don't overfill jars
            // with brine)
            Results fillResult = new GlobalFreshFillingPhase().run(gui);
            if (!fillResult.isSuccess) {
                return Results.FAIL();
            }

            // Phase 2: Re-fill brine
            Results brineResult = new GlobalBrinePhase().run(gui);
            if (!brineResult.isSuccess) {
                return Results.FAIL();
            }

            // Phase 3: Extract all ready items
            Results extractResult = new GlobalExtractionPhase().run(gui);
            if (!extractResult.isSuccess) {
                return Results.FAIL();
            }

            // Phase 4: Fill pickling jars with fresh inputs again
            Results refillResult = new GlobalFreshFillingPhase().run(gui);
            if (!refillResult.isSuccess) {
                return Results.FAIL();
            }
        }
    }
}