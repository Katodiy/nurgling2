package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

public class FillFuelTarkilns implements Action
{

    ArrayList<Gob> gobs;
    Pair<Coord2d,Coord2d> fuel;

    public FillFuelTarkilns(ArrayList<Gob> gobs, Pair<Coord2d,Coord2d> fuel) {
        this.gobs = gobs;
        this.fuel = fuel;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        ArrayList<Gob> piles = Finder.findGobs(fuel, new NAlias("stockpile"));
        if (piles.isEmpty()) {
            return Results.ERROR("NO FUEL IN AREA");
        }
        NAlias fuelname = null;
        Coord targetCoord = null;
        int num = 0;
        if (piles.get(0).ngob.name.contains("block")) {
            fuelname = new NAlias("block", "Block");
            targetCoord = new Coord(2, 1);
            num = 80;
        } else if (piles.get(0).ngob.name.contains("board")) {
            fuelname = new NAlias("board", "Board");
            targetCoord = new Coord(1, 4);
            num = 40;
        }
        if (fuelname == null) {
            return Results.ERROR("NO CORRECT FUEL IN AREA");
        }
        HashMap<Gob, Integer> needFuel = new HashMap<>();
        for (Gob gob : gobs) {
            needFuel.put(gob, num);
        }
        while (true) {
            int count = 0;
            int maxSize = NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord);
            for (Integer val : needFuel.values()) {
                count += val;
                if (count >= maxSize) {
                    break;
                }
            }

            if (count == 0) {
                return Results.SUCCESS();
            }
            ArrayList<Gob> targetGobs = new ArrayList<>(needFuel.keySet());
            targetGobs.sort(NUtils.grid_comp);
            for (Gob gob : targetGobs) {
                while (needFuel.get(gob) != 0) {
                    if (NUtils.getGameUI().getInventory().getItems(fuelname).isEmpty()) {
                        int target_size = Math.min(maxSize, count);
                        while (target_size != 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord) != 0) {
                            piles = Finder.findGobs(fuel, new NAlias("stockpile"));
                            if (piles.isEmpty()) {
                                if (gui.getInventory().getItems().isEmpty())
                                    return Results.ERROR("no fuel items");
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
                    }
                    ArrayList<WItem> fueltitem = NUtils.getGameUI().getInventory().getItems(fuelname);
                    if (fueltitem.isEmpty())
                        return Results.ERROR("no fuel items");
                    int val = Math.min(needFuel.get(gob), fueltitem.size());
                    if (needFuel.get(gob) != 0) {
                        new PathFinder(gob).run(gui);

                        for (int i = 0; i < val; i++) {
                            NUtils.takeItemToHand(fueltitem.get(i));
                            NUtils.activateItem(gob);
                            NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
                        }
                        needFuel.put(gob, needFuel.get(gob) - val);
                    }
                }
            }
        }
    }
}
