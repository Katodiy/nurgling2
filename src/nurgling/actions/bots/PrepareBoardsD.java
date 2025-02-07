package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NWItem;
import nurgling.actions.*;
import nurgling.conf.NPrepBoardsProp;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class PrepareBoardsD implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.PrepareBoards w = null;
        NPrepBoardsProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.PrepareBoards()), UI.scale(200,200))));
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
        (insa = new SelectArea(Resource.loadsimg("baubles/prepLogs"))).run(gui);

        ArrayList<Gob> logs;
        while (!(logs = Finder.findGobs(insa.getRCArea(),new NAlias("log"))).isEmpty())
        {
            logs.sort(NUtils.d_comp);
            Gob log = logs.get(0);
            while (Finder.findGob(log.id) != null) {
                new PathFinder(log).run(gui);
                new Equip(new NAlias(prop.tool)).run(gui);
                int space = NUtils.getGameUI().getInventory().calcNumberFreeCoord(new Coord(4, 1));
                if (space <= 1 && space>=0){
                    if(NUtils.getGameUI().getInventory().calcFreeSpace()<=4 || space==0){
                        return Results.ERROR("No space for board in the inv");
                    }
                }

                new SelectFlowerAction("Make boards", log).run(gui);
                NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/sawing"));
                NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(),new NAlias("Board"),1));
                if (NUtils.getEnergy() < 0.22) {
                    return Results.ERROR("No energy.");
                }
                if (NUtils.getStamina() <= 0.45) {
                    if(!(new Drink(0.9, true).run(gui)).IsSuccess())
                        return Results.ERROR("Drink is not found.");
                }

                try {
                    if(NUtils.getGameUI().getInventory().getItem(new NAlias("Board")) != null){
                        new DropTargetsFromInventory(new NAlias("Board")).run(gui);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return Results.SUCCESS();
    }
}
