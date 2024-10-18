package nurgling.overlays;

import haven.Gob;
import haven.render.Render;

import java.awt.*;

public class NAreaRange extends NAreaRad {
    boolean oldState = false;
    nurgling.conf.NAreaRad prop;
    public NAreaRange(Owner owner, nurgling.conf.NAreaRad prop) {
        super((Gob) owner, prop.radius);
        this.prop = prop;
    }

    @Override
    public void gtick(Render g) {
        if(oldState!=prop.vis){
            oldState = prop.vis;
            if(oldState )
                setR(g,(float)prop.radius);
            else
                setR(g,0);
        }
        if(oldState)
            super.gtick(g);
    }
}
