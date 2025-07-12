package nurgling.tools;

import haven.Coord;
import haven.Gob;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.areas.NGlobalCoord;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitForMapLoad;

public class CheckGridsState implements Runnable{
    @Override
    public void run() {
        try {
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return (NUtils.getGameUI()==null || NUtils.getGameUI().map==null) || NUtils.getGameUI().map.plgob!=-1;
                }
            });
            if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
                Gob player = NUtils.player();
                if (player != null) {

                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return (new NGlobalCoord(player.rc)).getGridId() != 0;
                        }
                    });
                    NGlobalCoord newCoord = new NGlobalCoord(player.rc);
                    if (((NMapView) NUtils.getGameUI().map).lastGC ==null || newCoord.getGridId() != ((NMapView) NUtils.getGameUI().map).lastGC.getGridId()) {
                        ((NMapView) NUtils.getGameUI().map).lastGC = newCoord;
                        NUtils.addTask(new WaitForMapLoad(NUtils.getGameUI(), newCoord));
                        if (NUtils.getGameUI().areas.visible) {
                            ((NMapView) NUtils.getGameUI().map).destroyDummys();
                            ((NMapView) NUtils.getGameUI().map).initDummys();
                        }

                        if (NUtils.getGameUI().routesWidget.visible) {
                            ((NMapView) NUtils.getGameUI().map).initRouteDummys(NUtils.getGameUI().routesWidget.getSelectedRouteId());
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
