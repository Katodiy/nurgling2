package nurgling.tools;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.pf.CellsArray;
import nurgling.pf.NPFMap;
import nurgling.tasks.*;

import java.util.*;

public class Finder
{
    static final Comparator<Gob> x_comp = new Comparator<Gob> () {
        @Override
        public int compare(
                Gob lhs,
                Gob rhs
        ) {
            // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
            return (lhs.rc.x > rhs.rc.x) ? -1 : ((lhs.rc.x < rhs.rc.x) ? 1 : (lhs.rc.y > rhs.rc.y) ? -1 : (
                    lhs.rc.y < rhs.rc.y) ? 1 : 0);
        }
    };

    static final Comparator<Gob> y_comp = new Comparator<Gob> () {
        @Override
        public int compare(
                Gob lhs,
                Gob rhs
        ) {
            // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
            return (lhs.rc.y > rhs.rc.y) ? -1 : ((lhs.rc.y < rhs.rc.y) ? 1 : (lhs.rc.x > rhs.rc.x) ? -1 : (
                    lhs.rc.x < rhs.rc.x) ? 1 : 0);
        }
    };

    static void sort(ArrayList<Gob> gobs)
    {
        if(!gobs.isEmpty())
        {
            Coord2d min = new Coord2d(gobs.get(0).rc.x,gobs.get(0).rc.y);
            Coord2d max = new Coord2d(gobs.get(0).rc.x,gobs.get(0).rc.y);
            for(Gob gob: gobs)
            {
                max.x = Math.max(gob.rc.x,max.x);
                max.y = Math.max(gob.rc.y,max.y);
                min.x = Math.min(gob.rc.x,min.x);
                min.y = Math.min(gob.rc.y,min.y);
            }
            if(Math.abs(max.y-min.y) > Math.abs(max.x - min.x))
                gobs.sort(x_comp);
            else
                gobs.sort(y_comp);
        }
    }

    public static ArrayList<Gob> findGobs(NArea area, NAlias name) throws InterruptedException
    {
        Pair<Coord2d,Coord2d> space = area.getRCArea();
        ArrayList<Gob> result = new ArrayList<> ();
        synchronized ( NUtils.getGameUI().ui.sess.glob.oc ) {
            for ( Gob gob : NUtils.getGameUI().ui.sess.glob.oc ) {
                if (!(gob instanceof OCache.Virtual))
                {
                    if (gob.rc.x >= space.a.x && gob.rc.y >= space.a.y && gob.rc.x <= space.b.x && gob.rc.y <= space.b.y)
                    {
                        if (NParser.isIt(gob, name))
                        {
                            result.add(gob);
                        }
                    }
                }
            }
        }
        sort(result);
        return result;
    }

    public static ArrayList<Gob> findGobs(Area area, NAlias name) throws InterruptedException
    {
        Coord2d b = area.ul.mul(MCache.tilesz);
        Coord2d e = area.br.mul(MCache.tilesz).add(MCache.tilesz);
        Pair<Coord2d,Coord2d> space = new Pair<>(b,e);
        ArrayList<Gob> result = new ArrayList<> ();
        synchronized ( NUtils.getGameUI().ui.sess.glob.oc ) {
            for ( Gob gob : NUtils.getGameUI().ui.sess.glob.oc ) {
                if (!(gob instanceof OCache.Virtual))
                {
                    if (gob.rc.x >= space.a.x && gob.rc.y >= space.a.y && gob.rc.x <= space.b.x && gob.rc.y <= space.b.y)
                    {
                        if (NParser.isIt(gob, name))
                        {
                            result.add(gob);
                        }
                    }
                }
            }
        }
        sort(result);
        return result;
    }


    public static ArrayList<Gob> findGobs(NArea area, NAlias name, int mattr) throws InterruptedException
    {
        Pair<Coord2d,Coord2d> space = area.getRCArea();
        ArrayList<Gob> result = new ArrayList<> ();
        synchronized ( NUtils.getGameUI().ui.sess.glob.oc ) {
            for ( Gob gob : NUtils.getGameUI().ui.sess.glob.oc ) {
                if (!(gob instanceof OCache.Virtual))
                {
                    if (gob.rc.x >= space.a.x && gob.rc.y >= space.a.y && gob.rc.x <= space.b.x && gob.rc.y <= space.b.y)
                    {
                        if (NParser.isIt(gob, name) && gob.ngob.getModelAttribute() == mattr)
                        {
                            result.add(gob);
                        }
                    }
                }
            }
        }
        sort(result);
        return result;
    }

    public static Gob findGob(NArea area, NAlias name) throws InterruptedException
    {
        NUtils.getUI().core.addTask(new FindPlayer());
        Pair<Coord2d,Coord2d> space = area.getRCArea();
        Gob result = null;
        double dist = 10000;
        synchronized ( NUtils.getGameUI().ui.sess.glob.oc ) {
            for ( Gob gob : NUtils.getGameUI().ui.sess.glob.oc ) {
                if (!(gob instanceof OCache.Virtual))
                {
                    if (gob.rc.x >= space.a.x && gob.rc.y >= space.a.y && gob.rc.x <= space.b.x && gob.rc.y <= space.b.y)
                    {
                        if (NParser.isIt(gob, name) && NUtils.player()!=null)
                        {
                            double new_dist;
                            if((new_dist = gob.rc.dist(NUtils.player().rc))<dist)
                            {
                                dist = new_dist;
                                result = gob;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }


    public static Gob findCrop(NArea area, NAlias name, int stage) throws InterruptedException
    {
        NUtils.getUI().core.addTask(new FindPlayer());
        Pair<Coord2d,Coord2d> space = area.getRCArea();
        Gob result = null;
        double dist = 10000;
        synchronized ( NUtils.getGameUI().ui.sess.glob.oc ) {
            for ( Gob gob : NUtils.getGameUI().ui.sess.glob.oc ) {
                if (!(gob instanceof OCache.Virtual))
                {
                    if (gob.rc.x >= space.a.x && gob.rc.y >= space.a.y && gob.rc.x <= space.b.x && gob.rc.y <= space.b.y)
                    {
                        if (NParser.isIt(gob, name) && gob.ngob.getModelAttribute() == stage && NUtils.player()!=null)
                        {
                            double new_dist;
                            if((new_dist = gob.rc.dist(NUtils.player().rc))<dist)
                            {
                                dist = new_dist;
                                result = gob;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static Gob findGob(NAlias name) throws InterruptedException
    {
        NUtils.getUI().core.addTask(new FindPlayer());
        Gob result = null;
        double dist = 10000;
        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
                if (!(gob instanceof OCache.Virtual || gob.attr.isEmpty() || gob.getClass().getName().contains("GlobEffector")))
                {
                    if (NParser.isIt(gob, name) && NUtils.player() != null)
                    {
                        double new_dist;
                        if ((new_dist = gob.rc.dist(NUtils.player().rc)) < dist)
                        {
                            dist = new_dist;
                            result = gob;
                        }
                    }
                }
            }
        }
        return result;
    }

    public static Gob findGob(long gobid)
    {
        if(gobid == -1)
        {
            if(NUtils.getGameUI().map.placing!=null)
                return NUtils.getGameUI().map.placing.get();
            return null;
        }
        else
        {
            return NUtils.getGameUI().ui.sess.glob.oc.getgob(gobid);
        }
    }

    public static Gob findGob(Coord pos) {
        Pair<Coord2d,Coord2d> space = new Pair<>(new Coord2d(pos.x*MCache.tilesz.x,pos.y*MCache.tilesz.y),new Coord2d((pos.x + 1) *MCache.tilesz.x,(pos.y+1)*MCache.tilesz.y));
//        NUtils.getGameUI().msg(space.a + " " +  space.b);
        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
                if (!(gob instanceof OCache.Virtual || gob.attr.isEmpty() || gob.getClass().getName().contains("GlobEffector")))
                {
                    // Только внутри тайла, без пересечений
                    if (gob.id!= NUtils.playerID() && gob.rc.x >=space.a.x && gob.rc.y >=space.a.y && gob.rc.x <=space.b.x && gob.rc.y <=space.b.y)
                    {
                        return gob;
                    }
                }
            }
        }
        return null;
    }



    public static Gob findGob(Coord pos, NAlias exc){
        Pair<Coord2d,Coord2d> space = new Pair<>(new Coord2d(pos.x*MCache.tilesz.x,pos.y*MCache.tilesz.y),new Coord2d((pos.x + 1) *MCache.tilesz.x,(pos.y+1)*MCache.tilesz.y));
//        NUtils.getGameUI().msg(space.a + " " +  space.b);
        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
                if(gob.ngob!=null && gob.ngob.name!=null && !NParser.checkName(gob.ngob.name,exc)) {
                    if (!(gob instanceof OCache.Virtual || gob.attr.isEmpty() || gob.getClass().getName().contains("GlobEffector"))) {
                        // Только внутри тайла, без пересечений
                        if (gob.id != NUtils.playerID() && gob.rc.x >= space.a.x && gob.rc.y >= space.a.y && gob.rc.x <= space.b.x && gob.rc.y <= space.b.y) {
                            return gob;
                        }
                    }
                }
            }
        }
        return null;
    }


    public static Gob findLiftedbyPlayer() {
        long plid;
        Following fl;
        if ((plid = NUtils.playerID()) != -1) {
            synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
                for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                    if ((fl = gob.getattr(Following.class)) != null) {

                        if (fl.tgt == plid) {
                            return gob;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static Gob findGob(Coord pos, NAlias crop, int stage) {
        Pair<Coord2d,Coord2d> space = new Pair<>(new Coord2d(pos.x*MCache.tilesz.x,pos.y*MCache.tilesz.y),new Coord2d((pos.x + 1) *MCache.tilesz.x,(pos.y+1)*MCache.tilesz.y));
//        NUtils.getGameUI().msg(space.a + " " +  space.b);
        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
                if (!(gob instanceof OCache.Virtual || gob.attr.isEmpty() || gob.getClass().getName().contains("GlobEffector")))
                {
                    // Только внутри тайла, без пересечений
                    if (gob.id!= NUtils.playerID() && gob.rc.x >=space.a.x && gob.rc.y >=space.a.y && gob.rc.x <=space.b.x && gob.rc.y <=space.b.y && NParser.checkName(gob.ngob.name,crop) && gob.ngob.getModelAttribute()==stage)
                    {
                        return gob;
                    }
                }
            }
        }
        return null;
    }

    public static ArrayList<Gob> findGobs(Area area, NAlias name, int stage) {
        Coord2d b = area.ul.mul(MCache.tilesz);
        Coord2d e = area.br.mul(MCache.tilesz).add(MCache.tilesz);
        Pair<Coord2d,Coord2d> space = new Pair<>(b,e);
        ArrayList<Gob> result = new ArrayList<> ();
        synchronized ( NUtils.getGameUI().ui.sess.glob.oc ) {
            for ( Gob gob : NUtils.getGameUI().ui.sess.glob.oc ) {
                if (!(gob instanceof OCache.Virtual))
                {
                    if (gob.rc.x >= space.a.x && gob.rc.y >= space.a.y && gob.rc.x <= space.b.x && gob.rc.y <= space.b.y)
                    {
                        if (gob.ngob.name!=null && NParser.checkName(gob.ngob.name, name) && gob.ngob.getModelAttribute() == stage )
                        {
                            result.add(gob);
                        }
                    }
                }
            }
        }
        sort(result);
        return result;
    }


    public static Coord2d getFreePlace(NArea area, Gob placed) {
        Coord2d pos = null;

        Pair<Coord2d, Coord2d> rcarea = area.getRCArea();
        Coord2d a = rcarea.a;
        Coord2d b = rcarea.b;
        // Последнее деление умножение нужно чтобы сопоставить сетку пф с сеткой лофтара по углу (ускорение запроса поверхности тайлов)

        Coord begin = nurgling.pf.Utils.toPfGrid(a,(byte) 1);
        Coord end =  nurgling.pf.Utils.toPfGrid(b,(byte) 1);


        NPFMap.Cell[][] cells = new NPFMap.Cell[end.x - begin.x][end.y - begin.y];

        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {

            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
                CellsArray ca;
                if (gob.ngob != null && gob.ngob.hitBox != null && (ca = gob.ngob.getCA()) != null && NUtils.player()!=null && gob.id!=NUtils.player().id && gob.getattr(Following.class) == null)
                {
                    if ((ca.begin.x >= begin.x && ca.begin.x <= end.x ||
                            ca.end.x >= begin.x && ca.end.x <= end.x) &&
                            (ca.begin.y >= begin.y && ca.begin.y <= end.y ||
                                    ca.end.y >= begin.y && ca.end.y <= end.y))
                    {
                        for (int i = 0; i < ca.x_len; i++)
                            for (int j = 0; j < ca.y_len; j++)
                            {
                                int ii = i + ca.begin.x - begin.x;
                                int jj = j + ca.begin.y - begin.y;
                                if (ii > 0 && ii < end.x - begin.x && jj > 0 && jj < end.y - begin.y)
                                {
                                    cells[ii][jj].val = ca.cells[i][j];
                                    if(ca.cells[i][j]!=0)
                                    {
                                        cells[ii][jj].content.add(gob.id);
                                    }
                                }
                            }
                    }
                }
            }
        }
        CellsArray gca = placed.ngob.getCA((byte) 1);
        Coord checkpos = new Coord(0,0);
        while (checkpos.x+gca.x_len<end.x) {
            while (checkpos.y + gca.y_len < end.y) {
                boolean free = true;

                for (int i = 0; i < gca.x_len; i++) {
                    for (int j = 0; j < gca.y_len; j++) {
                        if (gca.cells[i][j] != 0 && cells[checkpos.x][checkpos.y].val != 0) {
                            free = false;
                            break;
                        }
                        if (!free)
                            break;
                    }
                }
                if (free)
                {
                    return nurgling.pf.Utils.pfGridToWorld(checkpos,(byte)1);
                }
                else
                {
                    checkpos.y = checkpos.y + gca.y_len;
                }
            }
            checkpos.x=checkpos.x+gca.x_len;
            checkpos.y = begin.y;
        }
        return pos;
    }
}
