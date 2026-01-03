package nurgling.actions;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.navigation.NavigationService;
import nurgling.widgets.Specialisation;

public class RunToSafe implements Action{
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NUtils.setSpeed(4);
        NArea nArea = NContext.findSpecGlobal(Specialisation.SpecName.safe.toString());
        if(nArea!=null) {
            NavigationService.getInstance().navigateToArea(nArea, gui);
            return Results.SUCCESS();
        }
        else {
            return Results.FAIL();
        }
    }
}
