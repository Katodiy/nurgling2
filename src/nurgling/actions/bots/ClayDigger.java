package nurgling.actions.bots;

import haven.Gob;
import haven.UI;
import haven.res.lib.tree.TreeScale;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.conf.NClayDiggerProp;
import nurgling.tasks.WaitCheckable;
import nurgling.tasks.WaitChopperState;
import nurgling.tasks.WaitPose;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClayDigger implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.ClayDigger w = null;
        NClayDiggerProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.ClayDigger()), UI.scale(200,200))));
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
        if(prop.shovel==null)
        {
            return Results.ERROR("Not set required tools");
        }
        NUtils.getGameUI().msg("Please select area for dig clay");
        SelectArea insa;
        (insa = new SelectArea()).run(gui);


        NArea area = NArea.findOut(new NAlias("clay"),1);
        if(area==null || area.getRCArea() == null)
        {
            NUtils.getGameUI().msg("Please select area for output clay");
            SelectArea onsa;
            (onsa = new SelectArea()).run(gui);
            new DiggingResources(insa.getRCArea(),onsa.getRCArea(),new NAlias("clay"), prop.shovel).run(gui);
        }
        else {
            new DiggingResources(insa.getRCArea(), area.getRCArea(), new NAlias("clay"), prop.shovel).run(gui);
        }

        return Results.SUCCESS();
    }
}
