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
        String container = "Large Chest";
        gui.tickmsg("start");
        while(true)
        {
            new OpenTargetContainer(container, findChest()).run(gui);
            new TransferItems(gui.getInventory(), gui.getInventory(container).getItems("Autumn Steak"), 10).run(gui);
            new TransferItems(gui.getInventory(), gui.getInventory(container).getItems("Autumn Steak"), 10).run(gui);
            new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
            new OpenTargetContainer(container, findChest()).run(gui);
            new TransferItems(gui.getInventory(container), gui.getInventory().getItems("Autumn Steak"), 10).run(gui);
            new TransferItems(gui.getInventory(container), gui.getInventory().getItems("Autumn Steak"), 10).run(gui);
            new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
        }
    }
}
