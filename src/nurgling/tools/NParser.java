package nurgling.tools;

import haven.*;

public class NParser
{
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
}
