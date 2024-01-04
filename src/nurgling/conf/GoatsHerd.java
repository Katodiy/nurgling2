package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class GoatsHerd implements JConf {

    public boolean ignoreChildren = false;
    public boolean ignoreBD = false;
    public boolean disable_killing = false;
    public int adultGoats = 4;
    public int breedingGap = 10;
    public double milkq = 1.5;
    public double woolq = 0.33;
    public double meatq = 0.;
    public double hideq = 0.;
    public double meatquanth =0.;
    public double milkquanth =0.;
    public double woolquanth =0.;
    public double meatquan1 = 0.;
    public double meatquan2 = 0.;
    public double milkquan1 = 0.;
    public double milkquan2 = 0.;
    public double woolquan1 = 0.;
    public double woolquan2 = 0.;
    public double coverbreed = 0.;
    public String name;

    public GoatsHerd(String name)
    {
        this.name = name;
    }

    public GoatsHerd(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("adult_count") != null)
            adultGoats = (Integer) values.get("adult_count");
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
        if (values.get("coverbreed") != null)
            coverbreed = ((Number) values.get("coverbreed")).doubleValue();
        if (values.get("woolquan1") != null)
            woolquan1 = ((Number) values.get("woolquan1")).doubleValue();
        if (values.get("woolquan2") != null)
            woolquan2 = ((Number) values.get("woolquan2")).doubleValue();
        if (values.get("woolquanth") != null)
            woolquanth = ((Number) values.get("woolquanth")).doubleValue();
        if (values.get("wq") != null)
            woolq = ((Number) values.get("wq")).doubleValue();
        if(current==null)
            current = name;
    }

    public static void set(GoatsHerd prop)
    {
        ArrayList<GoatsHerd> goatsHerds = ((ArrayList<GoatsHerd>) NConfig.get(NConfig.Key.goatsprop));
        if (goatsHerds != null)
        {
            for (Iterator<GoatsHerd> i = goatsHerds.iterator(); i.hasNext(); )
            {
                GoatsHerd oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            goatsHerds = new ArrayList<>();
        }
        goatsHerds.add(prop);
        current = prop.name;
        NConfig.set(NConfig.Key.goatsprop, goatsHerds);
    }

    public static void remove(String val) {
        ArrayList<GoatsHerd> goatsHerd = ((ArrayList<GoatsHerd>) NConfig.get(NConfig.Key.goatsprop));
        if (goatsHerd == null)
            goatsHerd = new ArrayList<>();
        for (GoatsHerd prop : goatsHerd)
        {
            if (prop.name.equals(val))
            {
                goatsHerd.remove(prop);
                break;
            }
        }
        if(val.equals(current)) {
            if(goatsHerd.isEmpty())
            current = null;
            else
                current = goatsHerd.get(0).name;
        }
    }

    public static void setCurrent(String text) {
        current = text;
    }

    @Override
    public String toString()
    {
        return "goatsHerd[" + name + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jGoatsHerd = new JSONObject();
        jGoatsHerd.put("type", "GoatsHerd");
        jGoatsHerd.put("name", name);
        jGoatsHerd.put("adult_count", adultGoats);
        jGoatsHerd.put("breading_gap", breedingGap);
        jGoatsHerd.put("mq", milkq);
        jGoatsHerd.put("meatq", meatq);
        jGoatsHerd.put("hideq", hideq);
        jGoatsHerd.put("milkquan1", milkquan1);
        jGoatsHerd.put("milkquan2", milkquan2);
        jGoatsHerd.put("milkquanth", milkquanth);
        jGoatsHerd.put("meatquan1", meatquan1);
        jGoatsHerd.put("meatquan2", meatquan2);
        jGoatsHerd.put("meatquanth", meatquanth);
        jGoatsHerd.put("ic", ignoreChildren);
        jGoatsHerd.put("bd", ignoreBD);
        jGoatsHerd.put("dk", disable_killing);
        jGoatsHerd.put("woolquan1", woolquan1);
        jGoatsHerd.put("woolquan2", woolquan2);
        jGoatsHerd.put("woolquanth", woolquanth);
        jGoatsHerd.put("wq", woolq);
        jGoatsHerd.put("coverbreed", coverbreed);
        return jGoatsHerd;
    }

    public static GoatsHerd get(String val)
    {
        if(val == null)
            return null;
        ArrayList<GoatsHerd> goatsHerd = ((ArrayList<GoatsHerd>) NConfig.get(NConfig.Key.goatsprop));
        if (goatsHerd == null)
            goatsHerd = new ArrayList<>();
        for (GoatsHerd prop : goatsHerd)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        current = val;
        return new GoatsHerd(val);
    }

    static String current = null;

    public static GoatsHerd getCurrent()
    {
        return get(current);
    }

    public static HashSet<String> getKeySet()
    {
        HashSet<String> res = new HashSet<>();
        ArrayList<GoatsHerd> goatsHerd = ((ArrayList<GoatsHerd>) NConfig.get(NConfig.Key.goatsprop));
        if (goatsHerd == null)
            goatsHerd = new ArrayList<>();
        for (GoatsHerd prop : goatsHerd)
        {
            res.add(prop.name);
        }
        return res;
    }



}
