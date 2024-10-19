package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.WaitTargetSize;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

public class FuelToContainers implements Action
{

    ArrayList<Container> conts;
    Coord targetCoord = new Coord(1, 1);

    public FuelToContainers(ArrayList<Container> conts) {
        this.conts = conts;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        HashMap<String, Integer> neededFuel = new HashMap<>();
        for (Container cont : conts) {
            Container.FuelLvl fuelLvl = cont.getattr(Container.FuelLvl.class);
            String ftype = (String) fuelLvl.getRes().get(Container.FuelLvl.FUELTYPE);
            if (!neededFuel.containsKey(ftype)) {
                neededFuel.put(ftype, 0);
            }
            neededFuel.put(ftype, neededFuel.get(ftype) + fuelLvl.neededFuel());
        }
        for (Container cont : conts) {
            Container.FuelLvl fuelLvl = cont.getattr(Container.FuelLvl.class);
            while (fuelLvl.neededFuel() != 0) {
                String ftype = (String) fuelLvl.getRes().get(Container.FuelLvl.FUELTYPE);
                if (gui.getInventory().getItems(ftype).isEmpty()) {

                    int target_size = neededFuel.get(ftype);
                    while (target_size != 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord) != 0) {
                        NArea fuel = NArea.findSpec(Specialisation.SpecName.fuel.toString(), ftype);
                        if(fuel == null)
                            return Results.ERROR("No specialisation \"FUEL\" set.");
                        ArrayList<Gob> piles = Finder.findGobs(fuel, new NAlias("stockpile"));
                        if (piles.isEmpty()) {
                            if (gui.getInventory().getItems(ftype).isEmpty())
                                return Results.ERROR("no items");
                            else
                                break;
                        }
                        piles.sort(NUtils.d_comp);

                        Gob pile = piles.get(0);
                        new PathFinder(pile).run(gui);
                        new OpenTargetContainer("Stockpile", pile).run(gui);
                        TakeItemsFromPile tifp;
                        (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), Math.min(target_size, gui.getInventory().getFreeSpace()))).run(gui);
                        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                        target_size = target_size - tifp.getResult();
                    }
                    neededFuel.put(ftype, target_size);
                }
                new PathFinder(cont.gob).run(gui);
                new OpenTargetContainer(cont).run(gui);
                fuelLvl = cont.getattr(Container.FuelLvl.class);
                ArrayList<WItem> items = NUtils.getGameUI().getInventory().getItems(ftype);
                int fueled = Math.min(fuelLvl.neededFuel(), items.size());
                int aftersize = gui.getInventory().getItems().size() - fueled;
                for (int i = 0; i < fueled; i++) {
                    NUtils.takeItemToHand(items.get(i));
                    NUtils.activateItem(cont.gob);
                    NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
                }
                NUtils.getUI().core.addTask(new WaitTargetSize(NUtils.getGameUI().getInventory(), aftersize));
                new CloseTargetContainer(cont).run(gui);
            }
        }
        return Results.SUCCESS();
    }
}
