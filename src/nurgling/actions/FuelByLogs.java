package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tasks.*;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

public class FuelByLogs implements Action
{

    ArrayList<Container> conts;
    String name;
    Coord targetCoord = new Coord(1, 2);

    public FuelByLogs(ArrayList<Container> conts, String name) {
        this.conts = conts;
        this.name = name;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        int needed_size = 0;
        for (Container cont : conts) {
            Container.FuelLvl fuelLvl = cont.getattr(Container.FuelLvl.class);
            needed_size += fuelLvl.neededFuel();
        }
        for (Container cont : conts) {
            Container.FuelLvl fuelLvl = cont.getattr(Container.FuelLvl.class);
            while (fuelLvl.neededFuel() != 0) {
                String ftype = (String) fuelLvl.getRes().get(Container.FuelLvl.FUELTYPE);
                if (gui.getInventory().getItems(ftype).isEmpty()) {

                    int target_size = needed_size;
                    while (target_size != 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord) != 0 && NUtils.getGameUI().getInventory().getItems(new NAlias("block", "Block")).size()<needed_size) {
                        NArea fuel = NContext.findSpec(Specialisation.SpecName.fuel.toString(), ftype);
                        if(fuel == null)
                            return Results.ERROR("No specialisation \"FUEL\" set.");
                        ArrayList<Gob> logs = Finder.findGobs(fuel, new NAlias(name));
                        if (logs.isEmpty()) {
                            if (gui.getInventory().getItems(ftype).isEmpty())
                                return Results.ERROR("no items");
                            else
                                break;
                        }
                        logs.sort(NUtils.d_comp);
                        Gob log = logs.get(0);
                        while (Finder.findGob(log.id) != null) {
                            new PathFinder(log).run(gui);
//                            new Equip(new NAlias(prop.tool)).run(gui);
                            new SelectFlowerAction("Chop into blocks", log).run(gui);
                            NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/choppan"));
                            WaitPrepSmokingState wcs = new WaitPrepSmokingState(log, target_size);
                            NUtils.getUI().core.addTask(wcs);
                            switch (wcs.getState()) {
                                case LOGNOTFOUND:
                                    break;
                                case TIMEFORDRINK: {
                                    if(!(new Drink(0.9, true).run(gui)).IsSuccess())
                                        return Results.ERROR("Drink is not found");
                                    break;
                                }
                                case NOFREESPACE: {
                                    break;
                                }
                                case DANGER:
                                    return Results.ERROR("SOMETHING WRONG, STOP WORKING");
                            }
                            if(wcs.getState() == WaitPrepSmokingState.State.NOFREESPACE)
                                break;

                        }
                    }
                    needed_size -= NUtils.getGameUI().getInventory().getItems(new NAlias("block", "Block")).size();
                }
                new PathFinder(Finder.findGob(cont.gobid)).run(gui);
                new OpenTargetContainer(cont).run(gui);
                fuelLvl = cont.getattr(Container.FuelLvl.class);
                ArrayList<WItem> items = NUtils.getGameUI().getInventory().getItems(new NAlias("block", "Block"));
                int fueled = Math.min(fuelLvl.neededFuel(), items.size());
                int aftersize = gui.getInventory().getItems().size() - fueled;
                for (int i = 0; i < fueled; i++) {
                    NUtils.takeItemToHand(items.get(i));
                    NUtils.activateItem(Finder.findGob(cont.gobid));
                    NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
                }
                NUtils.getUI().core.addTask(new WaitTargetSize(NUtils.getGameUI().getInventory(), aftersize));
                new CloseTargetContainer(cont).run(gui);
            }
        }
        return Results.SUCCESS();
    }
}
