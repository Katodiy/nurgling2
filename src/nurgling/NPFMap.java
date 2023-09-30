package nurgling;

import haven.*;

import java.util.*;

public class NPFMap extends Widget
{
    // Получить координату игрока
    // NUtils.getGameUI().map.player().rc;

    // Получить Gob
    // NUtils.getGameUI().ui.sess.glob.oc.getgob( айди )
    // Получить координату (в абсолютных)
    // NUtils.getGameUI().ui.sess.glob.oc.getgob( айди ).rc
    // Получить угол
    // NUtils.getGameUI().ui.sess.glob.oc.getgob( айди ).a

    // Получить хитбокс
    // NUtils.getGameUI().ui.sess.glob.oc.getgob( айди ).ngob.hitBox
    // Получить углы (относительные)
    // NUtils.getGameUI().ui.sess.glob.oc.getgob( айди ).ngob.hitBox.begin
    // NUtils.getGameUI().ui.sess.glob.oc.getgob( айди ).ngob.hitBox.end

    // Мат методы для работы с координатами принадлежат классу координаты
    // Coord2d

    // MCache.tilesz2 - размер тайла, константа

    // HashMap<V,K> - хэш таблица
    double tilesz = MCache.tilesz2.x/4.;

    NPFMap(double tilesz)
    {
        this.tilesz = tilesz;
    }


    public void processGob(long id)
    {

    }

    public void processKritter(long id)
    {

    }

    public void deleteGob(long id)
    {

    }

    private void clear()
    {
    }

    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        if(NUtils.getGameUI().map!=null)
        {
            if(NUtils.getGameUI().map.lastload!=null)
            {
                if (NUtils.getGameUI().map.lastload instanceof MCache.LoadingMap)
                {
                    NUtils.getGameUI().msg("MCache.LoadingMap");
                    clear();
                }
            }
            else
            {
                if (NUtils.getGameUI().map.player() != null)
                {
                    Coord plcrd = NUtils.getGameUI().map.player().rc.div(MCache.tilesz).floor();
                    if (NUtils.getGameUI().ui.sess.glob.map.grids.get((plcrd.div(MCache.cmaps))) != null)
                    {
                        Resource res = NUtils.getGameUI().ui.sess.glob.map.tilesetr(NUtils.getGameUI().ui.sess.glob.map.gettile(plcrd));
                        String name = res.name;
                    }
                    //
                    //if ( res_beg.name.contains ( "tiles/cave" ) || res_beg.name.contains ( "tiles/nil" ) || res_beg.name.contains ( "tiles/deep" ) ||
                    //        res_beg.name.contains ( "tiles/rocks" ) ) {
                    //    if ( !res_beg.name.contains ( "tangle" ) ) {
                    //        array[i][j].isFree = false;
                    //        array[i][j].tileCenter = beg;
                    //    }
                    //}
                }
            }
        }
    }

    public PFGraph getGraph(Coord2d begin, Coord2d end)
    {
        return null;
    }

    class PFGraph
    {
        class Vertex{
            // Нужен только список соседей в которые можно пойти
            ArrayList<Long> sosedi = new ArrayList<>();
            boolean isVisited;
            double distance;
        }

        HashMap<Long,Vertex> data = new HashMap();
    }
}
