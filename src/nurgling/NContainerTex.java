package nurgling;

import java.util.*;

import haven.*;
import haven.render.*;

public class NContainerTex extends GAttrib implements Gob.SetupMod {


    public final HashMap<NStyle.Container, Pipe.Op> data = new HashMap<>();
    final HashMap<NStyle.Container, Integer> flags;
    public NContainerTex(Gob g, HashMap<NStyle.Container, Texture2D.Sampler2D> data, HashMap<NStyle.Container, Integer> flags) {
        super(g);
        ResDrawable rs = g.getattr(ResDrawable.class);
//        this.data.put(NStyle.Container.FREE, Pipe.Op.compose(new Tex2DN(data.get(NStyle.Container.FREE),rs.res.get().layer(TexR.class).tex().img)));
//        this.data.put(NStyle.Container.FULL, Pipe.Op.compose(new Tex2DN(data.get(NStyle.Container.FULL),rs.res.get().layer(TexR.class).tex().img)));
//        this.data.put(NStyle.Container.NOTFREE, Pipe.Op.compose(new Tex2DN(data.get(NStyle.Container.NOTFREE),rs.res.get().layer(TexR.class).tex().img)));
        this.flags = flags;
    }

    public Pipe.Op gobstate() {
        if ((gob.ngob.getModelAttribute() & ~flags.get(NStyle.Container.FREE)) == 0) {
            return data.get(NStyle.Container.FREE);
        } else if ((gob.ngob.getModelAttribute() & flags.get(NStyle.Container.FULL)) == flags.get(NStyle.Container.FULL)) {
            return data.get(NStyle.Container.FULL);
        } else {
            return data.get(NStyle.Container.NOTFREE);
        }
    }
}

