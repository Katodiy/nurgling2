package nurgling.conf;

import haven.*;

import java.util.*;

public class NCooldown
{
    public static HashMap<String, Double> data = new HashMap<>();
    static
    {
        data.put("paginae/atk/pow", 1.8);
        data.put("paginae/atk/haymaker", 3.0);
        data.put("paginae/atk/lefthook", 2.4);
        data.put("paginae/atk/barrage", 1.2);
        data.put("paginae/atk/yieldground", 2.0);
        data.put("paginae/atk/artevade", 2.66);
        data.put("paginae/atk/chop", 2.4);
        data.put("paginae/atk/cleave", 4.8);
        data.put("paginae/atk/flex", 1.8);
        data.put("paginae/atk/fdodge", 2.13);
        data.put("paginae/atk/gojug", 2.4);
        data.put("paginae/atk/fullcircle", 2.4);
        data.put("paginae/atk/punchboth", 2.4);
        data.put("paginae/atk/oppknock", 2.73);
        data.put("paginae/atk/lowblow", 3.00);
        data.put("paginae/atk/knockteeth", 2.13);
        data.put("paginae/atk/sideswipe", 1.53);
        data.put("paginae/atk/ravenbite", 2.4);
        data.put("paginae/atk/regain", 2.33);
        data.put("paginae/atk/ripapart", 3.6);
        data.put("paginae/atk/stealthunder", 2.4);
        data.put("paginae/atk/sting", 3.0);
        data.put("paginae/atk/sos", 3.0);
        data.put("paginae/atk/takedown", 3.0);
        data.put("paginae/atk/zigzag", 3.33);
        data.put("paginae/atk/uppercut", 1.8);
        data.put("paginae/atk/watchmoves", 2.73);
    }

    public static HashMap<String, Double> fixeddata = new HashMap<>();
    static
    {
        fixeddata.put("paginae/atk/qdodge", 1.5);
        fixeddata.put("paginae/atk/sidestep", 1.5);
        fixeddata.put("paginae/atk/jump", 1.5);
        fixeddata.put("paginae/atk/toarms", 0.66);
        fixeddata.put("paginae/atk/oakstance", 0.66);
        fixeddata.put("paginae/atk/parry", 0.66);
        fixeddata.put("paginae/atk/combmed", 0.66);
        fixeddata.put("paginae/atk/bloodlust", 0.66);
        fixeddata.put("paginae/atk/chinup", 0.66);
        fixeddata.put("paginae/atk/shield", 0.66);
    }

    public static HashMap<String, Pair<Double,Double>> vardata = new HashMap<>();
    static
    {
        vardata.put("paginae/atk/think", new Pair<>(3.6,4.8));
        vardata.put("paginae/atk/takeaim", new Pair<>(1.4, 1.8));
        vardata.put("paginae/atk/dash", new Pair<>(3.6 ,4.8));
    }
}
