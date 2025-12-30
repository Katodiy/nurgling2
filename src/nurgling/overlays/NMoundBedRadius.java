package nurgling.overlays;

import haven.Gob;
import haven.render.Render;
import nurgling.NConfig;

import java.awt.*;

public class NMoundBedRadius extends NAreaRad {
    private boolean oldState = false;

    // Mound bed has ~20 tile radius. 1 tile = 11 game units, so 20 tiles = 220 units
    private static final float RADIUS = 220f;

    public NMoundBedRadius(Gob owner) {
        super(owner, (Boolean) NConfig.get(NConfig.Key.showMoundBedRadius) ? RADIUS : 0f,
              new Color(64, 192, 64, 128),    // Fill color (green, for growth/nature)
              new Color(32, 255, 32, 255));   // Edge color (bright green)
        oldState = (Boolean) NConfig.get(NConfig.Key.showMoundBedRadius);
    }

    @Override
    public void gtick(Render g) {
        boolean currentState = (Boolean) NConfig.get(NConfig.Key.showMoundBedRadius);
        if (oldState != currentState) {
            oldState = currentState;
            if (oldState)
                setR(g, RADIUS);
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
