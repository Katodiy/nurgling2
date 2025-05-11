package nurgling.tasks;

import haven.Loading;
import haven.MCache;
import haven.MapView;
import nurgling.NMapView;
import nurgling.NUtils;

public class WaitPlayerNotNull extends NTask {

    public WaitPlayerNotNull()
    {

    }

    @Override
    public boolean check()
    {
        long map = NUtils.getGameUI().map.plgob;
        return NUtils.player() != null;
//        return MCache.tilesz.floor() != null;
    }

}
