package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class HorseHerd implements JConf {

    public boolean ignoreChildren = false;
    public boolean ignoreBD = false;
    public boolean disable_killing = false;
    public int adultHorse = 4;
    public int breedingGap = 10;
    public double stam1 = 1.5;
    public double stam2 = 0.;
    public double stamth = 32.;
    public double meatq =0.;
    public double hideq =0.;
    public double enduran = 0.;
    public double meta = 0.;
    public double meatquan1 = 0.;
    public double meatquan2 = 0.;
    public double meatquanth = 0.;
    public double coverbreed = 0.;
    public String name;

    public HorseHerd(String name)
    {
        this.name = name;
    }

    public HorseHerd(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("adult_count") != null)
            adultHorse = (Integer) values.get("adult_count");
        if (values.get("breading_gap") != null)
            breedingGap = (Integer) values.get("breading_gap");
        if (values.get("stam1") != null)
            stam1 = ((Number) values.get("stam1")).doubleValue();
        if (values.get("stam2") != null)
            stam2 = ((Number) values.get("stam2")).doubleValue();
        if (values.get("stamth") != null)
            stamth = ((Number) values.get("stamth")).doubleValue();
        if (values.get("meatq") != null)
            meatq = ((Number) values.get("meatq")).doubleValue();
        if (values.get("hideq") != null)
            hideq = ((Number) values.get("hideq")).doubleValue();
        if (values.get("enduran") != null)
            enduran = ((Number) values.get("enduran")).doubleValue();
        if (values.get("meta") != null)
            meta = ((Number) values.get("meta")).doubleValue();
        if (values.get("meatquan1") != null)
            meatquan1 = ((Number) values.get("meatquan1")).doubleValue();
        if (values.get("meatquan2") != null)
            meatquan2 = ((Number) values.get("meatquan2")).doubleValue();
        if (values.get("meatquanth") != null)
            meatquanth = ((Number) values.get("meatquanth")).doubleValue();
        if (values.get("coverbreed") != null)
            coverbreed = ((Number) values.get("coverbreed")).doubleValue();
        if (values.get("ic") != null)
            ignoreChildren = (Boolean) values.get("ic");
        if (values.get("bd") != null)
            ignoreBD = (Boolean) values.get("bd");
        if (values.get("dk") != null)
            disable_killing = (Boolean) values.get("dk");
        if(current==null)
            current = name;
    }

    public static void set(HorseHerd prop)
    {
        ArrayList<HorseHerd> horseHerds = ((ArrayList<HorseHerd>) NConfig.get(NConfig.Key.horseprop));
        if (horseHerds != null)
        {
            for (Iterator<HorseHerd> i = horseHerds.iterator(); i.hasNext(); )
            {
                HorseHerd oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            horseHerds = new ArrayList<>();
        }
        horseHerds.add(prop);
        current = prop.name;
        NConfig.set(NConfig.Key.horseprop, horseHerds);
    }

    public static void remove(String val) {
        ArrayList<HorseHerd> horseHerds = ((ArrayList<HorseHerd>) NConfig.get(NConfig.Key.horseprop));
        if (horseHerds == null)
            horseHerds = new ArrayList<>();
        for (HorseHerd prop : horseHerds)
        {
            if (prop.name.equals(val))
            {
                horseHerds.remove(prop);
                break;
            }
        }
        if(val.equals(current)) {
            if(horseHerds.isEmpty())
            current = null;
            else
                current = horseHerds.get(0).name;
        }
    }

    public static void setCurrent(String text) {
        current = text;
    }

    @Override
    public String toString()
    {
        return "HorseHerd[" + name + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jHorseHerd = new JSONObject();
        jHorseHerd.put("type", "HorseHerd");
        jHorseHerd.put("name", name);
        jHorseHerd.put("adult_count", adultHorse);
        jHorseHerd.put("breading_gap", breedingGap);
        jHorseHerd.put("stam1", stam1);
        jHorseHerd.put("stam2", stam2);
        jHorseHerd.put("stamth", stamth);
        jHorseHerd.put("meatq", meatq);
        jHorseHerd.put("hideq", hideq);
        jHorseHerd.put("enduran", enduran);
        jHorseHerd.put("meta", meta);
        jHorseHerd.put("meatquan1", meatquan1);
        jHorseHerd.put("meatquan2", meatquan2);
        jHorseHerd.put("meatquanth", meatquanth);
        jHorseHerd.put("coverbreed", coverbreed);
        jHorseHerd.put("ic", ignoreChildren);
        jHorseHerd.put("bd", ignoreBD);
        jHorseHerd.put("dk", disable_killing);
        return jHorseHerd;
    }

    public static HorseHerd get(String val)
    {
        if(val == null)
            return null;
        ArrayList<HorseHerd> horseHerds = ((ArrayList<HorseHerd>) NConfig.get(NConfig.Key.horseprop));
        if (horseHerds == null)
            horseHerds = new ArrayList<>();
        for (HorseHerd prop : horseHerds)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        current = val;
        return new HorseHerd(val);
    }

    static String current = null;

    public static HorseHerd getCurrent()
    {
        return get(current);
    }

    public static HashSet<String> getKeySet()
    {
        HashSet<String> res = new HashSet<>();
        ArrayList<HorseHerd> horseHerds = ((ArrayList<HorseHerd>) NConfig.get(NConfig.Key.horseprop));
        if (horseHerds == null)
            horseHerds = new ArrayList<>();
        for (HorseHerd prop : horseHerds)
        {
            res.add(prop.name);
        }
        return res;
    }



}
