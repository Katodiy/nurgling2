package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class PigsHerd implements JConf {

    public boolean ignoreChildren = false;
    public boolean ignoreBD = false;
    public boolean disable_killing = false;
    public int adultPigs = 4;
    public int breedingGap = 10;
    public double meatq = 0.;
    public double hideq = 0.;
    public double meatquanth =0.;
    public double trufquanth =0.;
    public double meatquan1 = 0.;
    public double meatquan2 = 0.;
    public double trufquan1 = 0.;
    public double trufquan2 = 0.;
    public double coverbreed = 0.;
    public String name;

    public PigsHerd(String name)
    {
        this.name = name;
    }

    public PigsHerd(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("adult_count") != null)
            adultPigs = (Integer) values.get("adult_count");
        if (values.get("breading_gap") != null)
            breedingGap = (Integer) values.get("breading_gap");
        if (values.get("meatq") != null)
            meatq = ((Number) values.get("meatq")).doubleValue();
        if (values.get("hideq") != null)
            hideq = ((Number) values.get("hideq")).doubleValue();
        if (values.get("meatquan1") != null)
            meatquan1 = ((Number) values.get("meatquan1")).doubleValue();
        if (values.get("meatquan2") != null)
            meatquan2 = ((Number) values.get("meatquan2")).doubleValue();
        if (values.get("meatquanth") != null)
            meatquanth = ((Number) values.get("meatquanth")).doubleValue();
        if (values.get("ic") != null)
            ignoreChildren = (Boolean) values.get("ic");
        if (values.get("bd") != null)
            ignoreBD = (Boolean) values.get("bd");
        if (values.get("dk") != null)
            disable_killing = (Boolean) values.get("dk");
        if (values.get("coverbreed") != null)
            coverbreed = ((Number) values.get("coverbreed")).doubleValue();
        if (values.get("trufquan1") != null)
            trufquan1 = ((Number) values.get("trufquan1")).doubleValue();
        if (values.get("trufquan2") != null)
            trufquan2 = ((Number) values.get("trufquan2")).doubleValue();
        if (values.get("trufquanth") != null)
            trufquanth = ((Number) values.get("trufquanth")).doubleValue();
     
        if(current==null)
            current = name;
    }

    public static void set(PigsHerd prop)
    {
        ArrayList<PigsHerd> pigsHerds = ((ArrayList<PigsHerd>) NConfig.get(NConfig.Key.pigsprop));
        if (pigsHerds != null)
        {
            for (Iterator<PigsHerd> i = pigsHerds.iterator(); i.hasNext(); )
            {
                PigsHerd oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            pigsHerds = new ArrayList<>();
        }
        pigsHerds.add(prop);
        current = prop.name;
        NConfig.set(NConfig.Key.pigsprop, pigsHerds);
    }

    public static void remove(String val) {
        ArrayList<PigsHerd> pigsHerds = ((ArrayList<PigsHerd>) NConfig.get(NConfig.Key.pigsprop));
        if (pigsHerds == null)
            pigsHerds = new ArrayList<>();
        for (PigsHerd prop : pigsHerds)
        {
            if (prop.name.equals(val))
            {
                pigsHerds.remove(prop);
                break;
            }
        }
        if(val.equals(current)) {
            if(pigsHerds.isEmpty())
            current = null;
            else
                current = pigsHerds.get(0).name;
        }
    }

    public static void setCurrent(String text) {
        current = text;
    }

    @Override
    public String toString()
    {
        return "pigsHerd[" + name + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jPigsHerd = new JSONObject();
        jPigsHerd.put("type", "PigsHerd");
        jPigsHerd.put("name", name);
        jPigsHerd.put("adult_count", adultPigs);
        jPigsHerd.put("breading_gap", breedingGap);
        jPigsHerd.put("meatq", meatq);
        jPigsHerd.put("hideq", hideq);
        jPigsHerd.put("meatquan1", meatquan1);
        jPigsHerd.put("meatquan2", meatquan2);
        jPigsHerd.put("meatquanth", meatquanth);
        jPigsHerd.put("ic", ignoreChildren);
        jPigsHerd.put("bd", ignoreBD);
        jPigsHerd.put("dk", disable_killing);
        jPigsHerd.put("trufquan1", trufquan1);
        jPigsHerd.put("trufquan2", trufquan2);
        jPigsHerd.put("trufquanth", trufquanth);
        jPigsHerd.put("coverbreed", coverbreed);
        return jPigsHerd;
    }

    public static PigsHerd get(String val)
    {
        if(val == null)
            return null;
        ArrayList<PigsHerd> pigsHerds = ((ArrayList<PigsHerd>) NConfig.get(NConfig.Key.pigsprop));
        if (pigsHerds == null)
            pigsHerds = new ArrayList<>();
        for (PigsHerd prop : pigsHerds)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        current = val;
        return new PigsHerd(val);
    }

    static String current = null;

    public static PigsHerd getCurrent()
    {
        return get(current);
    }

    public static HashSet<String> getKeySet()
    {
        HashSet<String> res = new HashSet<>();
        ArrayList<PigsHerd> pigsHerds = ((ArrayList<PigsHerd>) NConfig.get(NConfig.Key.pigsprop));
        if (pigsHerds == null)
            pigsHerds = new ArrayList<>();
        for (PigsHerd prop : pigsHerds)
        {
            res.add(prop.name);
        }
        return res;
    }

}
