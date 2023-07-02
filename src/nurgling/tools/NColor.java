package nurgling.tools;

import nurgling.conf.*;
import org.json.*;

import java.awt.*;
import java.awt.color.*;

public class NColor extends Color implements JConf
{
    public NColor(int r, int g, int b)
    {
        super(r, g, b);
    }

    public NColor(int r, int g, int b, int a)
    {
        super(r, g, b, a);
    }

    public NColor(int rgb)
    {
        super(rgb);
    }

    public NColor(int rgba, boolean hasalpha)
    {
        super(rgba, hasalpha);
    }

    public NColor(float r, float g, float b)
    {
        super(r, g, b);
    }

    public NColor(float r, float g, float b, float a)
    {
        super(r, g, b, a);
    }

    public NColor(ColorSpace cspace, float[] components, float alpha)
    {
        super(cspace, components, alpha);
    }

    public static NColor build(String value)
    {
        String[] arr = value.substring(value.lastIndexOf("[") + 1, value.lastIndexOf("]")).split(",");
        if (arr.length == 4)
        {
            return new NColor(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
        }
        else
        {
            return new NColor(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]), Integer.parseInt(arr[2]));
        }
    }

    @Override
    public String toString()
    {
        return "NColor[" + getRed() + "," + getGreen() + "," + getBlue() + "," + getAlpha() + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jcolor = new JSONObject();
        jcolor.put("red", getRed());
        jcolor.put("blue", getBlue());
        jcolor.put("green", getGreen());
        jcolor.put("alpha", getAlpha());
        return jcolor;
    }
}
