package nurgling;

import haven.*;

import java.util.*;

public class NHitBox
{
    public Coord2d begin;
    public Coord2d end;
    public NHitBox(Coord begin, Coord end)
    {
        this.begin = new Coord2d(begin);
        this.end = new Coord2d(end);
    }

    private final static HashMap<String, NHitBox> custom = new HashMap<String, NHitBox>()
    {
        {
            put("log", new NHitBox(new Coord(-10,-2),new Coord(10,2)));
            put("gfx/terobjs/vehicle/dugout", new NHitBox(new Coord(-10,-2),new Coord(10,2)));
            put("gfx/terobjs/trough", new NHitBox(new Coord(-4,-13),new Coord(4,13)));
            put("gfx/terobjs/minehole", new NHitBox(new Coord(-15,-15),new Coord(15,15)));
            put("bumlings", new NHitBox(new Coord(-3,-3),new Coord(3,3)));
            put("gfx/terobjs/arch/stonemansion", new NHitBox(new Coord(-50,-50),new Coord(50,50)));
            put("gfx/terobjs/arch/logcabin", new NHitBox(new Coord(-23,-23),new Coord(23,23)));
            put("gfx/terobjs/arch/greathall", new NHitBox(new Coord(-80,-55),new Coord(80,55)));
            put("gfx/terobjs/arch/timberhouse", new NHitBox(new Coord(-33,-33),new Coord(33,33)));
            put("gfx/terobjs/arch/stonetower", new NHitBox(new Coord(-39,-39),new Coord(39,39)));
            put("gfx/terobjs/arch/windmill", new NHitBox(new Coord(-28,-28),new Coord(28,28)));
            put("gfx/terobjs/arch/stonestead", new NHitBox(new Coord(-45,-28),new Coord(45,28)));
            put("gfx/terobjs/villageidol", new NHitBox(new Coord(-11,-17),new Coord(11,17)));
            put("gfx/terobjs/pclaim", new NHitBox(new Coord(-3,-3),new Coord(3,3)));
            put("gfx/terobjs/iconsign", new NHitBox(new Coord(-2,-2),new Coord(2,2)));
            put("gfx/terobjs/candelabrum", new NHitBox(new Coord(-2,-2),new Coord(2,2)));
            put("gfx/terobjs/lanternpost", new NHitBox(new Coord(-2,-2),new Coord(2,2)));
            put("gfx/terobjs/cistern", new NHitBox(new Coord(-9,-9),new Coord(9,9)));
            put("gfx/terobjs/oven", new NHitBox(new Coord(-9,-9),new Coord(9,9)));
            put("gfx/terobjs/kiln", new NHitBox(new Coord(-9,-9),new Coord(9,9)));
            put("gfx/terobjs/leanto", new NHitBox(new Coord(-9,-9),new Coord(9,9)));
            put("gfx/terobjs/stonepillar", new NHitBox(new Coord(-12,-12),new Coord(12,12)));
            put("gfx/terobjs/fineryforge", new NHitBox(new Coord(-9,-9),new Coord(9,9)));
            put("gfx/terobjs/smelter", new NHitBox(new Coord(-11,-11),new Coord(11,19)));
            put("gfx/terobjs/charterstone", new NHitBox(new Coord(-9,-9),new Coord(9,9)));
            put("gfx/terobjs/steelcrucible", new NHitBox(new Coord(-3,-4),new Coord(3,4)));
            put("gfx/terobjs/beehive", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/column", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/meatgrinder", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/brazier", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/granary", new NHitBox(new Coord(-16,-16),new Coord(16,16)));
            put("gfx/terobjs/pow", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/smokeshed", new NHitBox(new Coord(-6,-6),new Coord(6,6)));
            put("gfx/terobjs/vehicle/cart", new NHitBox(new Coord(-6,-6),new Coord(6,6)));
            put("gfx/terobjs/knarrdock", new NHitBox(new Coord(-60,-14),new Coord(62,14)));
            put("gfx/terobjs/furn/bed-sturdy", new NHitBox(new Coord(-9,-6),new Coord(9,6)));
            put("gfx/terobjs/vehicle/wreckingball-fold", new NHitBox(new Coord(-5,-11),new Coord(5,11)));
            put("gfx/terobjs/quern", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/arch/palisadeseg", new NHitBox(new Coord(-6,-6),new Coord(6,6)));
            put("gfx/terobjs/arch/palisadecp", new NHitBox(new Coord(-6,-6),new Coord(6,6)));
            put("gfx/terobjs/arch/polecp", new NHitBox(new Coord(-5,-5),new Coord(5,5)));
            put("gfx/terobjs/arch/poleseg", new NHitBox(new Coord(-5,-5),new Coord(5,5)));
            put("gfx/terobjs/arch/drystonewallseg", new NHitBox(new Coord(-5,-5),new Coord(5,5)));
            put("gfx/terobjs/arch/drystonewallcp", new NHitBox(new Coord(-5,-5),new Coord(5,5)));
            put("gfx/terobjs/arch/polebiggate", new NHitBox(new Coord(-5,-16),new Coord(5,16)));
            put("gfx/terobjs/arch/drystonewallbiggate", new NHitBox(new Coord(-5,-16),new Coord(5,16)));
            put("gfx/terobjs/arch/palisadebiggate", new NHitBox(new Coord(-5,-16),new Coord(5,16)));
            put("gfx/terobjs/arch/polegate", new NHitBox(new Coord(-5,-11),new Coord(5,11)));
            put("gfx/terobjs/arch/drystonewallgate", new NHitBox(new Coord(-5,-11),new Coord(5,11)));
            put("gfx/terobjs/arch/palisadegate", new NHitBox(new Coord(-5,-11),new Coord(5,11)));
            put("gfx/terobjs/potterswheel", new NHitBox(new Coord(-2,-6),new Coord(2,6)));
            put("gfx/terobjs/stockpile-oddtuber", new NHitBox(new Coord(-5,-5),new Coord(5,5)));
            put("gfx/terobjs/stockpile-lemon", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/stockpile-nut", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/stockpile-cavebulb", new NHitBox(new Coord(-4,-4),new Coord(4,4)));
            put("gfx/terobjs/primsmelter", new NHitBox(new Coord(-11,-7),new Coord(8,7)));
            put("gfx/kritter/cattle/calf", new NHitBox(new Coord(-9,-3),new Coord(9,3)));
            put("gfx/kritter/horse/stallion", new NHitBox(new Coord(-9,-3),new Coord(9,3)));
            put("gfx/kritter/horse/mare", new NHitBox(new Coord(-9,-3),new Coord(9,3)));
            put("gfx/kritter/horse/foal", new NHitBox(new Coord(-8,-3),new Coord(8,3)));
            put("gfx/kritter/pig/piglet", new NHitBox(new Coord(-5,-2),new Coord(5,2)));
            put("gfx/kritter/pig/sow", new NHitBox(new Coord(-5,-2),new Coord(5,2)));
            put("gfx/kritter/sheep/lamb", new NHitBox(new Coord(-5,-2),new Coord(5,2)));
            put("gfx/kritter/goat/billy", new NHitBox(new Coord(-5,-2),new Coord(5,2)));
            put("gfx/kritter/goat/nanny", new NHitBox(new Coord(-4,-2),new Coord(5,2)));
            put("gfx/kritter/goat/kid", new NHitBox(new Coord(-5,-2),new Coord(5,2)));
        }
    };

    public static NHitBox findCustom(String name)
    {
        NHitBox res = custom.get(name);
        if(res!=null)
            return res;
        if(name.endsWith("log") && name.startsWith("gfx/terobjs/trees"))
            return custom.get("log");
        if(name.startsWith("gfx/terobjs/bumlings"))
            return custom.get("bumlings");
        return null;
    }

    @Override
    public String toString()
    {
        return "([" + String.valueOf(begin.x) + "," + String.valueOf(begin.y) + "],[" + String.valueOf(end.x) + "," + String.valueOf(end.y) + "])";
    }
}
