package nurgling.tools;

import haven.*;

public class NParser
{
    public static Coord str2coord(String val)
    {
        String []prep = ((val.substring(val.lastIndexOf("(")+1,val.lastIndexOf(")"))).replaceAll(" ","")).split(",");
        return new Coord(Integer.parseInt(prep[0]),Integer.parseInt(prep[1]));
    }
}
