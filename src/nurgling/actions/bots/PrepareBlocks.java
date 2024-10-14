package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.UI;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tasks.WaitCheckable;
import nurgling.tasks.WaitPose;
import nurgling.tasks.WaitPrepBlocksState;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class PrepareBlocks implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.PrepareBlocks w = null;
        NPrepBlocksProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.PrepareBlocks()), UI.scale(200,200))));
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
        SelectArea insa;
        NUtils.getGameUI().msg("Please select area with logs");
        (insa = new SelectArea()).run(gui);

        SelectArea outsa;
        NUtils.getGameUI().msg("Please select area for piles");
        (outsa = new SelectArea()).run(gui);

        ArrayList<Gob> logs;
        while (!(logs = Finder.findGobs(insa.getRCArea(),new NAlias("log"))).isEmpty())
        {
            logs.sort(NUtils.d_comp);
            Gob log = logs.get(0);
            while (Finder.findGob(log.id) != null) {
                if (NUtils.getGameUI().getInventory().calcNumberFreeCoord(new Coord(1, 2)) == 0)
                {
                    new TransferToPiles(outsa.getRCArea(),new NAlias("block")).run(gui);
                }
                new PathFinder(log).run(gui);
                new Equip(new NAlias(prop.tool)).run(gui);
                new SelectFlowerAction("Chop into blocks", log).run(gui);
                NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/choppan"));
                WaitPrepBlocksState wcs = new WaitPrepBlocksState(log, prop);
                NUtils.getUI().core.addTask(wcs);
                switch (wcs.getState()) {
                    case LOGNOTFOUND:
                        break;
                    case TIMEFORDRINK: {
                        if(!(new Drink(0.9).run(gui)).IsSuccess())
                            return Results.ERROR("Drink is not found");
                        break;
                    }
                    case NOFREESPACE: {
                        new TransferToPiles(outsa.getRCArea(),new NAlias("block")).run(gui);
                        break;
                    }
                    case DANGER:
                        return Results.ERROR("SOMETHING WRONG, STOP WORKING");

                }
            }
        }
        new TransferToPiles(outsa.getRCArea(),new NAlias("block")).run(gui);
        return Results.SUCCESS();
    }
}
