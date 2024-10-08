package mapv4;

import haven.*;
import nurgling.NStyle;
import nurgling.NUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class StatusWdg extends Widget {

    public static AtomicBoolean status = new AtomicBoolean(false);

    final TexI online;
    final TexI offline;
    public StatusWdg(Coord sz) {
        super(sz);
        online = new TexI(Resource.loadsimg("nurgling/hud/automap/online"));
        offline = new TexI(Resource.loadsimg("nurgling/hud/automap/offline"));
    }

    @Override
    public void draw(GOut g) {
        if(status.get()) {
            g.image(online, Coord.z);
        }
        else
        {
            g.image(offline, Coord.z);
        }
        super.draw(g);
    }
}
