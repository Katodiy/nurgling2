package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

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
                    if (NParser.checkName(gob.getres().name, "chest"))
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

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
//        String container = "Stockpile";
//        gui.tickmsg("start");
//        while (true)
//        {
//            new OpenTargetContainer(container, findChest()).run(gui);
//            for (int i = 0; i < 13; i++)
//                new TransferItems(gui.getStockpile(), gui.getInventory().getItems("Block"), 1).run(gui);
//            new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
//
//            new OpenTargetContainer(container, findChest()).run(gui);
//            for (int i = 0; i < 13; i++)
//                new TakeItemsFromPile(gui.getStockpile(), new Coord(1, 2), 1).run(gui);
////            new TransferItems(gui.getInventory(container), gui.getInventory().getItems("Autumn Steak"), 10).run(gui);
////            new TransferItems(gui.getInventory(container), gui.getInventory().getItems("Autumn Steak"), 10).run(gui);
//            new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
//        }
//        return Results.SUCCESS();
        gui.tickmsg("start");
//        if(new OpenTargetContainer("Chest", findChest()).run(gui).equals(Results.SUCCESS()));
//        {
            gui.tickmsg("total free coord 2x2 " + gui.getInventory().getNumberFreeCoord(new Coord(1,2)));
//        }
        return Results.SUCCESS();
    }
}
