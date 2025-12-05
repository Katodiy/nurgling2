package nurgling.overlays;

import haven.Gob;
import haven.render.Render;
import nurgling.NConfig;

import java.awt.*;

public class NBeehiveRadius extends NAreaRad {
    private boolean oldState = false;
    
    public NBeehiveRadius(Gob owner) {
        super(owner, (Boolean) NConfig.get(NConfig.Key.showBeehiveRadius) ? 150f : 0f, 
              new Color(0, 163, 192, 128), 
              new Color(0, 192, 0, 255));
        oldState = (Boolean) NConfig.get(NConfig.Key.showBeehiveRadius);
    }
    
    @Override
    public void gtick(Render g) {
        boolean currentState = (Boolean) NConfig.get(NConfig.Key.showBeehiveRadius);
        if (oldState != currentState) {
            oldState = currentState;
            if (oldState)
                setR(g, 150f);
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
