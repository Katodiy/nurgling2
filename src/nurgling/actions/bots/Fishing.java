package nurgling.actions.bots;

import haven.Resource;
import haven.UI;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.DiggingResources;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.conf.NClayDiggerProp;
import nurgling.conf.NFishingSettings;
import nurgling.tasks.WaitCheckable;
import nurgling.tools.NAlias;

public class Fishing implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.Fishing w = null;
        NFishingSettings prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.Fishing()), UI.scale(200,200))));
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
