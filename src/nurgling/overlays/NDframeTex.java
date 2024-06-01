package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.*;

public class NDframeTex extends GAttrib implements Gob.SetupMod {


    public final HashMap<NStyle.Container, Pipe.Op> data = new HashMap<>();
    public NDframeTex(Gob g, HashMap<NStyle.Container, Texture2D.Sampler2D> data) {
        super(g);
        ResDrawable rs = g.getattr(ResDrawable.class);
//        this.data.put(NStyle.Container.FREE, Pipe.Op.compose(new Tex2DN(data.get(NStyle.Container.FREE),rs.res.get().layer(TexR.class).tex().img)));
//        this.data.put(NStyle.Container.FULL, Pipe.Op.compose(new Tex2DN(data.get(NStyle.Container.FULL),rs.res.get().layer(TexR.class).tex().img)));
//        this.data.put(NStyle.Container.NOTFREE, Pipe.Op.compose(new Tex2DN(data.get(NStyle.Container.NOTFREE),rs.res.get().layer(TexR.class).tex().img)));
    }

    public Pipe.Op gobstate() {
        boolean isFound = false;
        Gob.Overlay targetOverlay = null;
        for (Gob.Overlay ol : gob.ols) {
            if(ol.spr instanceof StaticSprite)
            {
                isFound = true;
                targetOverlay = ol;
                break;
            }
        }
        if (!isFound) {
            return data.get(NStyle.Container.FREE);
        }
        else
        {
            if (!NParser.isIt(targetOverlay, new NAlias("-blood", "-fishraw", "-windweed")) || NParser.isIt(targetOverlay, new NAlias("-windweed-dry"))) {
                return data.get(NStyle.Container.FULL);
            }
            else {
                return data.get(NStyle.Container.NOTFREE);
            }
        }
    }
}

