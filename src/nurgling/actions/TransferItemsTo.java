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
                    if (NParser.checkName(gob.getres().name, "frame"))
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
        gui.tickmsg("start");
//        if(new OpenTargetContainer("Frame", findChest()).run(gui).equals(Results.SUCCESS()));
//        {
        gui.tickmsg("total free coord 2x2 " + gui.getInventory().getNumberFreeCoord(new Coord(2,2)));
//        }
        return Results.SUCCESS();
    }
}
