package nurgling.actions;

import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.List;

public class RunToSafe implements Action{
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NUtils.setSpeed(4);
        NArea nArea = NContext.findSpecGlobal(Specialisation.SpecName.safe.toString());
        if(nArea!=null) {

            return Results.SUCCESS();
        }
        else {
            return Results.FAIL();
        }
    }
}
