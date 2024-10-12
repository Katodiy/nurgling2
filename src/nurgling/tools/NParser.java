package nurgling.tools;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

public class NParser
{
    public static boolean checkName(
            final String name,
            final NAlias regEx
    ) { if (regEx!=null) {
        /// Проверяем имя на соответствие
        for (String key : regEx.keys) {
            if (key!=null && name.toLowerCase().contains(key.toLowerCase())) {
                for (String ex : regEx.exceptions) {
                    if (name.toLowerCase().contains(ex.toLowerCase())) {
                        return false;
                    }
                }
                return true;
            }
        }
        if(regEx.keys.isEmpty())
        {
            for (String ex : regEx.exceptions) {
                if (name.toLowerCase().contains(ex.toLowerCase())) {
                    return false;
                }
            }
            return true;
        }
    }
        return false;
    }

    public static boolean checkName(
            final NAlias name,
            final NAlias regEx
    ) {
        if (regEx != null && name != null) {
            return (regEx.keys.containsAll(name.keys)) && (regEx.exceptions.containsAll(name.exceptions));
        }
        return false;
    }

    public static boolean checkName(
            final String name,
            final String... args
    ) {
        if(name==null)
            return false;
        return checkName(name, new NAlias(args));
    }


    public static Coord str2coord(String val)
    {
        String []prep = ((val.substring(val.lastIndexOf("(")+1,val.lastIndexOf(")"))).replaceAll(" ","")).split(",");
        return new Coord(Integer.parseInt(prep[0]),Integer.parseInt(prep[1]));
    }

    public static Coord2d str2coord2(String val)
    {
        String []prep = ((val.substring(val.lastIndexOf("(")+1,val.lastIndexOf(")"))).replaceAll(" ","")).split(",");
        return new Coord2d(Double.parseDouble(prep[0]),Double.parseDouble(prep[1]));
    }

    public static boolean isIt(Gob gob, NAlias name) throws InterruptedException
    {
        NUtils.getUI().core.addTask(new GetGobName(gob));
        if (gob.ngob.name != null)
        {
            return NParser.checkName(gob.ngob.name, name);
        }
        else
            return false;
    }

    public static boolean isIt(Gob.Overlay ol, NAlias name)
    {
        return (ol.spr instanceof StaticSprite && ((StaticSprite)ol.spr).res!=null && NParser.checkName(((StaticSprite)ol.spr).res.name,name));
    }

    public static boolean isIt(Coord pltc, NAlias name) {
            Resource res_beg = NUtils.getGameUI().ui.sess.glob.map.tilesetr(NUtils.getGameUI().ui.sess.glob.map.gettile(pltc));
            if (res_beg != null) {
                return checkName(res_beg.name, name);
            }
            return false;
    }
}
