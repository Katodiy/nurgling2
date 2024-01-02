package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class CowsHerd implements JConf {

    public boolean ignoreChildren = false;
    public boolean ignoreBD = false;
    public boolean disable_killing = false;
    public int adultCows = 4;
    public int breedingGap = 10;
    public double milkq = 1.5;
    public double meatq = 0.;
    public double hideq = 0.;
    public double meatquanth =0.;
    public double milkquanth =0.;
    public double meatquan1 = 0.;
    public double meatquan2 = 0.;
    public double milkquan1 = 0.;
    public double milkquan2 = 0.;
    public double coverbreed = 0.;
    public String name;

    public CowsHerd( String name)
    {
        this.name = name;
    }

    public CowsHerd(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("adult_count") != null)
            adultCows = (Integer) values.get("adult_count");
        if (values.get("breading_gap") != null)
            breedingGap = (Integer) values.get("breading_gap");
        if (values.get("mq") != null)
            milkq = ((Number) values.get("mq")).doubleValue();
        if (values.get("meatq") != null)
            meatq = ((Number) values.get("meatq")).doubleValue();
        if (values.get("hideq") != null)
            hideq = ((Number) values.get("hideq")).doubleValue();
        if (values.get("milkquan1") != null)
            milkquan1 = ((Number) values.get("milkquan1")).doubleValue();
        if (values.get("milkquan2") != null)
            milkquan2 = ((Number) values.get("milkquan2")).doubleValue();
        if (values.get("milkquanth") != null)
            milkquanth = ((Number) values.get("milkquanth")).doubleValue();
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
        if(current==null)
            current = name;
    }

    public static void set(CowsHerd prop)
    {
        ArrayList<CowsHerd> cowsHerds = ((ArrayList<CowsHerd>) NConfig.get(NConfig.Key.cowsprop));
        if (cowsHerds != null)
        {
            for (Iterator<CowsHerd> i = cowsHerds.iterator(); i.hasNext(); )
            {
                CowsHerd oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            cowsHerds = new ArrayList<>();
        }
        cowsHerds.add(prop);
        current = prop.name;
        NConfig.set(NConfig.Key.cowsprop, cowsHerds);
    }

    public static void remove(String val) {
        ArrayList<CowsHerd> cowsHerd = ((ArrayList<CowsHerd>) NConfig.get(NConfig.Key.cowsprop));
        if (cowsHerd == null)
            cowsHerd = new ArrayList<>();
        for (CowsHerd prop : cowsHerd)
        {
            if (prop.name.equals(val))
            {
                cowsHerd.remove(prop);
                break;
            }
        }
        if(val.equals(current)) {
            if(cowsHerd.isEmpty())
            current = null;
            else
                current = cowsHerd.get(0).name;
        }
    }

    public static void setCurrent(String text) {
        current = text;
    }

    @Override
    public String toString()
    {
        return "CowsHerd[" + name + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jCowsHerd = new JSONObject();
        jCowsHerd.put("type", "CowsHerd");
        jCowsHerd.put("name", name);
        jCowsHerd.put("adult_count", adultCows);
        jCowsHerd.put("breading_gap", breedingGap);
        jCowsHerd.put("mq", milkq);
        jCowsHerd.put("meatq", meatq);
        jCowsHerd.put("hideq", hideq);
        jCowsHerd.put("milkquan1", milkquan1);
        jCowsHerd.put("milkquan2", milkquan2);
        jCowsHerd.put("milkquanth", milkquanth);
        jCowsHerd.put("meatquan1", meatquan1);
        jCowsHerd.put("meatquan2", meatquan2);
        jCowsHerd.put("meatquanth", meatquanth);
        jCowsHerd.put("ic", ignoreChildren);
        jCowsHerd.put("bd", ignoreBD);
        jCowsHerd.put("dk", disable_killing);
        return jCowsHerd;
    }

    public static CowsHerd get(String val)
    {
        if(val == null)
            return null;
        ArrayList<CowsHerd> cowsHerd = ((ArrayList<CowsHerd>) NConfig.get(NConfig.Key.cowsprop));
        if (cowsHerd == null)
            cowsHerd = new ArrayList<>();
        for (CowsHerd prop : cowsHerd)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        current = val;
        return new CowsHerd(val);
    }

    static String current = null;

    public static CowsHerd getCurrent()
    {
        return get(current);
    }

    public static HashSet<String> getKeySet()
    {
        HashSet<String> res = new HashSet<>();
        ArrayList<CowsHerd> cowsHerd = ((ArrayList<CowsHerd>) NConfig.get(NConfig.Key.cowsprop));
        if (cowsHerd == null)
            cowsHerd = new ArrayList<>();
        for (CowsHerd prop : cowsHerd)
        {
            res.add(prop.name);
        }
        return res;
    }



}
