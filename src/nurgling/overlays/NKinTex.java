package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.*;

public class NKinTex extends GAttrib implements Gob.SetupMod {

    public final HashMap<Integer, Pipe.Op> data = new HashMap<>();

    public NKinTex(Gob g) {
        super(g);

        ResDrawable rs = g.getattr(ResDrawable.class);
        for(Integer key : NStyle.dkinAlt.keySet())
        {
            this.data.put(key, Pipe.Op.compose(new Tex2DN(NStyle.dkinAlt.get(key),NStyle.dkinAlt.get(0))));
        }
    }

    public Pipe.Op gobstate() {
//        KinInfo ki = (KinInfo)gob.getattr(KinInfo.class);
//        if(ki!=null)
//        {
//            return data.get(ki.group);
//        }
        return null;
    }
}

