package nurgling.tools;

import haven.Coord;
import haven.Gob;

import java.util.HashMap;

public class StockpileUtils {
    public static HashMap<String, Coord> itemMaxSize = new HashMap<>();
    static {
        itemMaxSize.put("gfx/terobjs/stockpile-hide", new Coord(2,2));
        itemMaxSize.put("gfx/terobjs/stockpile-fish", new Coord(2,3));
        itemMaxSize.put("gfx/terobjs/stockpile-bone", new Coord(3,2));
    }

    public static HashMap<String, String> defaultItems = new HashMap<>();
    static {
        defaultItems.put("gfx/terobjs/stockpile-hide", "Bear Hide");
        defaultItems.put("gfx/terobjs/stockpile-fish", "Pike");
        defaultItems.put("gfx/terobjs/stockpile-bone", "Bone Material");
        defaultItems.put("gfx/terobjs/stockpile-ore", "Cassiterite");
    }


//    public static String getDefaultItem(Gob pile) {
//        if(pile.ngob.name!=null)
//        {
//            return defaultItems.get(pile.ngob.name);
//        }
//        return null;
//    }
}
