package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

public class TestUtils
{
    public static Gob findGob(String name){
        double distance = 10000;
        /// Расстояние до объекта с "запасом"
        Gob result = null;
        synchronized ( NUtils.getGameUI().ui.sess.glob.oc ) {
            for ( Gob gob : NUtils.getGameUI().ui.sess.glob.oc )
            {
                if (gob.getres() != null && gob.getres().name != null)
                {
                    if (NParser.checkName(gob.getres().name, name))
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
}
