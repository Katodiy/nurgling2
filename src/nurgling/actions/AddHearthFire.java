package nurgling.actions;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;

public class AddHearthFire implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        new TravelToHearthFire().run(NUtils.getGameUI());

        Gob player = NUtils.player();
        Coord2d rc = player.rc;

        Coord tilec = rc.div(MCache.tilesz).floor();
        MCache.Grid grid = NUtils.getGameUI().ui.sess.glob.map.getgridt(tilec);

        ((NMapView) NUtils.getGameUI().map).routeGraphManager.addHearthFire(tilec.sub(grid.ul), grid.id, NUtils.getGameUI().getCharInfo().chrid);

        NConfig.needRoutesUpdate();

        return Results.SUCCESS();
    }
}
