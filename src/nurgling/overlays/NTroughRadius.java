package nurgling.overlays;

import haven.Gob;
import haven.render.Render;
import nurgling.NConfig;

import java.awt.*;

public class NTroughRadius extends NAreaRad {
    private boolean oldState = false;
    
    public NTroughRadius(Gob owner) {
        super(owner, (Boolean) NConfig.get(NConfig.Key.showTroughRadius) ? 200f : 0f, 
              new Color(192, 192, 0, 128), 
              new Color(0, 164, 192, 255));
        oldState = (Boolean) NConfig.get(NConfig.Key.showTroughRadius);
    }
    
    @Override
    public void gtick(Render g) {
        boolean currentState = (Boolean) NConfig.get(NConfig.Key.showTroughRadius);
        if (oldState != currentState) {
            oldState = currentState;
            if (oldState)
                setR(g, 200f);
            else
                setR(g, 0);
        }
        if (oldState)
            super.gtick(g);
    }
    
    @Override
    public boolean tick(double dt) {
        return super.tick(dt);
    }
}
