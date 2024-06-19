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
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

public class FuelToContainers implements Action
{

    ArrayList<Container> conts;
    Context context;


    public FuelToContainers(Context context, ArrayList<Container> conts) {
        this.conts = conts;
        this.context = context;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
//        HashMap<String, Integer> fuelNeed = new HashMap<>();
//        for (Context.Container cont : conts) {
//            if (cont instanceof FueledContainer) {
//                FueledContainer fc = (FueledContainer) cont;
//                Integer val = fuelNeed.get(fc.fuelType);
//                if (val == null) {
//                    fuelNeed.put(fc.fuelType, fc.fuelNeed);
//                } else {
//                    fuelNeed.put(fc.fuelType, fc.fuelNeed + val);
//                }
//            }
//        }
//        for (String type : fuelNeed.keySet()) {
//            Coord targetCoord = new Coord(1, 1);
//            if (type.equals("Block")) {
//                targetCoord = new Coord(1, 2);
//            }
//            NArea area = NArea.findSpec(Specialisation.SpecName.fuel.toString(), type);
//            for (Context.Container cont : conts) {
//                if (cont instanceof FueledContainer) {
//                    FueledContainer fc = (FueledContainer) cont;
//                    if (fc.fuelType.equals(type) && area != null) {
//                        if (fc.fuelNeed > 0) {
//                            if (gui.getInventory().getItems(type).isEmpty()) {
//                                int target_size = fuelNeed.get(type);
//                                while (target_size != 0 && gui.getInventory().getNumberFreeCoord(targetCoord) > 0) {
//                                    ArrayList<Gob> piles = Finder.findGobs(area, new NAlias("stockpile"));
//                                    if (piles.isEmpty())
//                                        break;
//                                    piles.sort(NUtils.d_comp);
//                                    Gob pile = piles.get(0);
//                                    new PathFinder(pile).run(gui);
//                                    new OpenTargetContainer("Stockpile", pile).run(gui);
//                                    TakeItemsFromPile tifp;
//                                    (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), Math.min(target_size, gui.getInventory().getFreeSpace()))).run(gui);
//                                    new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
//                                    target_size = target_size - tifp.getResult();
//                                    if (gui.getInventory().getNumberFreeCoord(targetCoord) == 0 || target_size == 0)
//                                    {
//                                        for (Context.Container cont2 : conts) {
//                                            if (cont2 instanceof FueledContainer) {
//                                                FueledContainer fc2 = (FueledContainer) cont2;
//                                                if (fc2.fuelType.equals(type) && fc2.fuelNeed>0) {
//                                                    new PathFinder(fc2.gob).run(gui);
//                                                    ArrayList<WItem> items = NUtils.getGameUI().getInventory().getItems(type);
//                                                    int fueled = Math.min(fc2.fuelNeed, items.size());
//                                                    int aftersize = items.size() - fueled;
//                                                    for (int i = 0; i < fueled; i++) {
//                                                        NUtils.takeItemToHand(items.get(i));
//                                                        NUtils.activateItem(cont2.gob);
//                                                        NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
//                                                    }
//                                                    NUtils.getUI().core.addTask(new WaitTargetSize(NUtils.getGameUI().getInventory(), aftersize));
//                                                    fc2.fuelNeed-=fueled;
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
        return Results.SUCCESS();
    }
}
