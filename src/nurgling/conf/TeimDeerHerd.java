package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class TeimDeerHerd implements JConf {

    public boolean ignoreChildren = false;
    public boolean ignoreBD = false;
    public boolean disable_killing = false;
    public boolean disable_q_percentage = false;
    public int adultDeers = 4;
    public int breedingGap = 10;
    public double meatq = 0.;
    public double hideq = 0.;
    public double meatquanth =0.;
    public double meatquan1 = 0.;
    public double meatquan2 = 0.;
    public double coverbreed = 0.;
    public String name;

    public TeimDeerHerd(String name)
    {
        this.name = name;
    }

    public TeimDeerHerd(HashMap<String, Object> values)
    {
        name = (String) values.get("name");
        if (values.get("adult_count") != null)
            adultDeers = (Integer) values.get("adult_count");
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
        if (values.get("qp") != null)
            disable_q_percentage = (Boolean) values.get("qp");
        if (values.get("coverbreed") != null)
            coverbreed = ((Number) values.get("coverbreed")).doubleValue();
        if(current==null)
            current = name;
    }

    public static void set(TeimDeerHerd prop)
    {
        ArrayList<TeimDeerHerd> teimDeerHers = ((ArrayList<TeimDeerHerd>) NConfig.get(NConfig.Key.deersprop));
        if (teimDeerHers != null)
        {
            for (Iterator<TeimDeerHerd> i = teimDeerHers.iterator(); i.hasNext(); )
            {
                TeimDeerHerd oldprop = i.next();
                if (oldprop.name.equals(prop.name))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            teimDeerHers = new ArrayList<>();
        }
        teimDeerHers.add(prop);
        current = prop.name;
        NConfig.set(NConfig.Key.deersprop, teimDeerHers);
    }

    public static void remove(String val) {
        ArrayList<TeimDeerHerd> deersHerd = ((ArrayList<TeimDeerHerd>) NConfig.get(NConfig.Key.deersprop));
        if (deersHerd == null)
            deersHerd = new ArrayList<>();
        for (TeimDeerHerd prop : deersHerd)
        {
            if (prop.name.equals(val))
            {
                deersHerd.remove(prop);
                break;
            }
        }
        if(val.equals(current)) {
            if(deersHerd.isEmpty())
            current = null;
            else
                current = deersHerd.get(0).name;
        }
    }

    public static void setCurrent(String text) {
        current = text;
    }

    @Override
    public String toString()
    {
        return "DeersHerd[" + name + "]";
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject jdeersHerd = new JSONObject();
        jdeersHerd.put("type", "DeersHerd");
        jdeersHerd.put("name", name);
        jdeersHerd.put("adult_count", adultDeers);
        jdeersHerd.put("breading_gap", breedingGap);
        jdeersHerd.put("meatq", meatq);
        jdeersHerd.put("hideq", hideq);
        jdeersHerd.put("meatquan1", meatquan1);
        jdeersHerd.put("meatquan2", meatquan2);
        jdeersHerd.put("meatquanth", meatquanth);
        jdeersHerd.put("ic", ignoreChildren);
        jdeersHerd.put("bd", ignoreBD);
        jdeersHerd.put("dk", disable_killing);
        jdeersHerd.put("qp", disable_q_percentage);
        jdeersHerd.put("coverbreed", coverbreed);
        return jdeersHerd;
    }

    public static TeimDeerHerd get(String val)
    {
        if(val == null)
            return null;
        ArrayList<TeimDeerHerd> deersHerd = ((ArrayList<TeimDeerHerd>) NConfig.get(NConfig.Key.deersprop));
        if (deersHerd == null)
            deersHerd = new ArrayList<>();
        for (TeimDeerHerd prop : deersHerd)
        {
            if (prop.name.equals(val))
            {
                return prop;
            }
        }
        current = val;
        return new TeimDeerHerd(val);
    }

    static String current = null;

    public static TeimDeerHerd getCurrent()
    {
        return get(current);
    }

    public static HashSet<String> getKeySet()
    {
        HashSet<String> res = new HashSet<>();
        ArrayList<TeimDeerHerd> deersHerd = ((ArrayList<TeimDeerHerd>) NConfig.get(NConfig.Key.deersprop));
        if (deersHerd == null)
            deersHerd = new ArrayList<>();
        for (TeimDeerHerd prop : deersHerd)
        {
            res.add(prop.name);
        }
        return res;
    }



}
