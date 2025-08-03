package nurgling.tools;

import haven.*;
import haven.res.ui.barterbox.Shopbox;
import nurgling.*;
import nurgling.tasks.*;
import java.util.HashMap;
import java.util.Map;

public class NParser
{
    // Cache for frequently used NAlias objects
    private static final Map<String, NAlias> ALIAS_CACHE = new HashMap<>();
    private static final int MAX_ALIAS_CACHE_SIZE = 500;
    public static boolean checkName(
            final String name,
            final NAlias regEx
    ) { 
        if (regEx == null || name == null) return false;
        String lowerName = name.toLowerCase();

        // Check if cached match result exists
        if (regEx.matches(lowerName)) {
            return true;
        }

        return false;
    }

    public static boolean checkName(
            final NAlias name,
            final NAlias regEx
    ) {
        if (regEx != null && name != null) {
            for (String key : name.keys) {
                if (!regEx.matches(key)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean checkName(
            final String name,
            final String... args
    ) {
        if(name==null)
            return false;
        for (String arg : args) {
            NAlias alias = getCachedAlias(arg);
            if (alias.matches(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get cached NAlias object or create and cache a new one
     */
    private static NAlias getCachedAlias(String key) {
        NAlias cached = ALIAS_CACHE.get(key);
        if (cached == null) {
            cached = new NAlias(key);
            if (ALIAS_CACHE.size() < MAX_ALIAS_CACHE_SIZE) {
                ALIAS_CACHE.put(key, cached);
            }
        }
        return cached;
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


    public static boolean checkName(
            Shopbox.ShopItem price,
            final NAlias regEx
    ) { 
        if (regEx == null || price == null || price.name == null) 
            return false;
            
        // Handle special meat cases
        if(regEx.keys.size()==1)
        {
            String firstKey = regEx.keys.get(0);
            if ( firstKey != null && firstKey.contains("Raw Wild") ) {
                // Add non-wild variant
                String nonWild = firstKey.replace("Wild ", "");
                if (!regEx.keys.contains(nonWild)) {
                    regEx.keys.add(nonWild);
                    regEx.buildCaches(); // Rebuild caches after modification
                }
            }
        }

        // Use optimized matching
        return regEx.matches(price.name);
    }
}
