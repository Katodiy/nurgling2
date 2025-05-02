package nurgling.tools;

import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;

public class RouteCreator implements Runnable {
    @Override
    public void run() {
        NUtils.getGameUI().msg("Adding route");
        ((NMapView) NUtils.getGameUI().map).addRoute();
        NConfig.needRoutesUpdate();
        NUtils.getGameUI().routes.show();
    }
}
