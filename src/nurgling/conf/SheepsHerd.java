package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class SheepsHerd implements JConf {

    public boolean ignoreChildren = false;
    public boolean ignoreBD = false;
    public boolean disable_killing = false;
    public boolean disable_q_percentage = false;
    public int adultSheeps = 4;
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

    public SheepsHerd(String name)
    {
        this.name = name;
    }

    public SheepsHerd(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("adult_count") != null)
            adultSheeps = (Integer) values.get("adult_count");
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
        if (values.get("qp") != null)
            disable_q_percentage = (Boolean) values.get("qp");
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

    public static void set(SheepsHerd prop)
    {
        ArrayList<SheepsHerd> sheepsHerds = ((ArrayList<SheepsHerd>) NConfig.get(NConfig.Key.sheepsprop));
        if (sheepsHerds != null)
        {
            for (Iterator<SheepsHerd> i = sheepsHerds.iterator(); i.hasNext(); )
            {
                SheepsHerd oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            sheepsHerds = new ArrayList<>();
        }
        sheepsHerds.add(prop);
        current = prop.name;
        NConfig.set(NConfig.Key.sheepsprop, sheepsHerds);
    }

    public static void remove(String val) {
        ArrayList<SheepsHerd> sheepsHerds = ((ArrayList<SheepsHerd>) NConfig.get(NConfig.Key.sheepsprop));
        if (sheepsHerds == null)
            sheepsHerds = new ArrayList<>();
        for (SheepsHerd prop : sheepsHerds)
        {
            if (prop.name.equals(val))
            {
                sheepsHerds.remove(prop);
                break;
            }
        }
        if(val.equals(current)) {
            if(sheepsHerds.isEmpty())
            current = null;
            else
                current = sheepsHerds.get(0).name;
        }
    }

    public static void setCurrent(String text) {
        current = text;
    }

    @Override
    public String toString()
    {
        return "sheepsHerd[" + name + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jSheepsHerd = new JSONObject();
        jSheepsHerd.put("type", "SheepsHerd");
        jSheepsHerd.put("name", name);
        jSheepsHerd.put("adult_count", adultSheeps);
        jSheepsHerd.put("breading_gap", breedingGap);
        jSheepsHerd.put("mq", milkq);
        jSheepsHerd.put("meatq", meatq);
        jSheepsHerd.put("hideq", hideq);
        jSheepsHerd.put("milkquan1", milkquan1);
        jSheepsHerd.put("milkquan2", milkquan2);
        jSheepsHerd.put("milkquanth", milkquanth);
        jSheepsHerd.put("meatquan1", meatquan1);
        jSheepsHerd.put("meatquan2", meatquan2);
        jSheepsHerd.put("meatquanth", meatquanth);
        jSheepsHerd.put("ic", ignoreChildren);
        jSheepsHerd.put("bd", ignoreBD);
        jSheepsHerd.put("dk", disable_killing);
        jSheepsHerd.put("qp", disable_q_percentage);
        jSheepsHerd.put("woolquan1", woolquan1);
        jSheepsHerd.put("woolquan2", woolquan2);
        jSheepsHerd.put("woolquanth", woolquanth);
        jSheepsHerd.put("wq", woolq);
        jSheepsHerd.put("coverbreed", coverbreed);
        return jSheepsHerd;
    }

    public static SheepsHerd get(String val)
    {
        if(val == null)
            return null;
        ArrayList<SheepsHerd> sheepsHerds = ((ArrayList<SheepsHerd>) NConfig.get(NConfig.Key.sheepsprop));
        if (sheepsHerds == null)
            sheepsHerds = new ArrayList<>();
        for (SheepsHerd prop : sheepsHerds)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        current = val;
        return new SheepsHerd(val);
    }

    static String current = null;

    public static SheepsHerd getCurrent()
    {
        return get(current);
    }

    public static HashSet<String> getKeySet()
    {
        HashSet<String> res = new HashSet<>();
        ArrayList<SheepsHerd> sheepsHerds = ((ArrayList<SheepsHerd>) NConfig.get(NConfig.Key.sheepsprop));
        if (sheepsHerds == null)
            sheepsHerds = new ArrayList<>();
        for (SheepsHerd prop : sheepsHerds)
        {
            res.add(prop.name);
        }
        return res;
    }



}
