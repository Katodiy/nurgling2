package nurgling;

import haven.*;

import java.util.*;

public class NPFMap
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

    public enum TyleType
    {
        Default,
        Nil
    }

    // rc - центр квадрата
    public void processTile(Coord2d rc, TyleType type)
    {
    }

    ///@param delta - Координата сдвига сетки( -1, 0)
    public void shiftGrid(Coord delta)
    {

    }

    public void clear()
    {
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
