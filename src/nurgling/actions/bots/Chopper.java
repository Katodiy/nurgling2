package nurgling.actions.bots;

import haven.UI;
import haven.Widget;
import haven.Window;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.conf.NChopperProp;
import nurgling.tasks.WaitCheckable;
import nurgling.widgets.bots.Checkable;

public class Chopper implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.Chopper w = null;
        NChopperProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.Chopper()), UI.scale(200,200))));
            prop = w.prop;
        }
        catch (InterruptedException e)
        {
            throw e;
        }
        finally {
            if(w!=null)
                w.destroy();
        }
        if(prop == null)
        {
            return Results.ERROR("No config");
        }
        return Results.SUCCESS();
    }
}
