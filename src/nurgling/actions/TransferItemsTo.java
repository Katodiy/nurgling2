package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.*;

public class TransferItemsTo implements Action
{
    // TODO FOR DELETE
    Gob findChest(){
        double distance = 10000;
        /// Расстояние до объекта с "запасом"
        Gob result = null;
        synchronized ( NUtils.getGameUI().ui.sess.glob.oc ) {
            for ( Gob gob : NUtils.getGameUI().ui.sess.glob.oc )
            {
                if (gob.getres() != null && gob.getres().name != null)
                {
                    if (NParser.checkName(gob.getres().name, "stockpile"))
                    {
                        /// Сравнивается расстояние между игроком и объектом
                        double dist = NUtils.getGameUI().map.player().rc.dist(gob.rc);
                        /// Если расстояние минимально то оно и объект запоминаются
                        if (dist < distance)
                        {
                            distance = dist;
                            result = gob;
                        }
                    }
                }
            }
        }
        return result;
    }

    public static final NAlias hides = new NAlias(new ArrayList<>(Arrays.asList("Fur", "skin", "hide")),
            new ArrayList<>(Arrays.asList("blood", "raw", "Fresh", "Jacket", "cape")));

    public static final NAlias ores = new NAlias(new ArrayList<>(Arrays.asList("rit", "Ore", "ore")));

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        String container = "Stockpile";
        while (true)
        {
            Gob pile = findChest();
            new OpenTargetContainer(container, pile).run(gui);
            ArrayList<WItem> items = gui.getInventory().getItems(ores);
            new TransferItemsOLD(gui.getStockpile(), items, items.size()).run(gui);
            new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);

            new OpenTargetContainer(container, findChest()).run(gui);
            new TakeItemsFromPile(pile, gui.getStockpile()).run(gui);
//            new TransferItems(gui.getInventory(container), gui.getInventory().getItems("Autumn Steak"), 10).run(gui);
            new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
        }
//        return Results.SUCCESS();
//        gui.tickmsg("start");
////        if(new OpenTargetContainer("Chest", findChest()).run(gui).equals(Results.SUCCESS()));
////        {
//            gui.tickmsg("total free coord 2x2 " + gui.getInventory().getNumberFreeCoord(new Coord(1,2)));
////        }
//        return Results.SUCCESS();
    }
}
