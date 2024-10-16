package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FillFuelPow implements Action
{

    ArrayList<Gob> pows;
    int marker;
    Coord targetCoord = new Coord(2, 1);

    public FillFuelPow(ArrayList<Gob> pows, int marker) {
        this.pows = pows;
        this.marker = marker;
    }
    NAlias fuelname = new NAlias("block", "Block");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        while (true) {
            ArrayList<Gob> target = new ArrayList<>();
            int count = 0;
            int maxSize = NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord);
            for (Gob gob : pows) {
                if ((gob.ngob.getModelAttribute() & marker) == 0) {
                    target.add(gob);
                    count+=2;
                    if (count >= maxSize) {
                        break;
                    }
                }
            }
            if(target.isEmpty()) {
                return Results.SUCCESS();
            }
            for ( Gob gob : target ) {
                if(NUtils.getGameUI().getInventory().getItems(fuelname).isEmpty()) {
                    int target_size = count;
                    while (target_size != 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord) != 0) {
                        ArrayList<Gob> piles = Finder.findGobs(NArea.findSpec(Specialisation.SpecName.fuel.toString(), "Block"), new NAlias("stockpile"));
                        if (piles.isEmpty()) {
                            if (gui.getInventory().getItems().isEmpty())
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
                }
                new PathFinder(gob).run(gui);
                ArrayList<WItem> fueltitem = NUtils.getGameUI().getInventory().getItems(fuelname);
                if (fueltitem.size()<2) {
                    return Results.ERROR("no fuel");
                }
                for(int i=0; i<2;i++) {
                    NUtils.takeItemToHand(fueltitem.get(i));
                    NUtils.activateItem(gob);
                    NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
                }
            }
        }
    }
}
