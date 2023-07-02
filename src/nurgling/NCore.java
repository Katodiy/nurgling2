package nurgling;

import haven.*;
import nurgling.tools.*;

import java.awt.*;

public class NCore extends Widget {
    NConfig config;

    public NCore() {
        config = new NConfig();
        config.read();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if(config.isUpdated())
        {
            config.write();
        }
    }
}
