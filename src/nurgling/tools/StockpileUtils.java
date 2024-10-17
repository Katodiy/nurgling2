package nurgling.tools;

import haven.Coord;

import java.util.HashMap;

public class StockpileUtils {
    public static HashMap<String, Coord> itemMaxSize = new HashMap<>();
    static {
        itemMaxSize.put("gfx/terobjs/stockpile-hide", new Coord(2,2));
        itemMaxSize.put("gfx/terobjs/stockpile-fish", new Coord(2,3));
        itemMaxSize.put("gfx/terobjs/stockpile-bone", new Coord(3,2));
    }
}
