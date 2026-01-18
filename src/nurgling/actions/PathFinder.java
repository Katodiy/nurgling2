package nurgling.actions;

import haven.*;
import jdk.jshell.execution.Util;
import nurgling.*;
import nurgling.pf.*;
import nurgling.pf.Utils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.*;
import java.util.concurrent.atomic.*;

import static nurgling.pf.Graph.getPath;

public class PathFinder implements Action {
    private final int VISIBLE_AREA = 41;
    public static double pfmdelta = 1.5;
    NPFMap pfmap = null;
    Coord start_pos = null;
    Coord end_pos = null;
    ArrayList<Coord> end_poses = null;
    public boolean isHardMode = false;
    public boolean waterMode = false;
    public boolean gatesAlwaysClosed = false;
    Coord2d begin;
    Coord2d end;
    long target_id = -2;
    public boolean isDynamic = false;
    Gob dummy;
    boolean dn = false;
    Mode mode = Mode.NEAREST;
    Gob gobInStartPos = null;
    double badDir = Double.MAX_VALUE;



    public enum Mode
    {
        NEAREST,
        Y_MAX,
        Y_MIN,
        X_MAX,
        X_MIN,
    }

    public PathFinder(Coord2d begin, Coord2d end) {
        this.begin = begin;
        this.end = end;
    }

    public PathFinder(Coord2d begin, Gob target) {
        this(target);
        this.begin = begin;
    }

    public PathFinder(Coord2d end) {
        this(NUtils.getGameUI().map.player().rc, end);
    }

    public PathFinder(Gob target) {
        this(target.rc);
        target_id = target.id;
        Gob targetg;
        if((targetg = Finder.findGob(target_id))!=null)
        {
            if(NParser.checkName(targetg.ngob.name,new NAlias("pow")))
            {
                badDir = targetg.a;
            }
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (true) {
            LinkedList<Graph.Vertex> path = construct();

            if (path != null) {
                boolean needRestart = false;
//                NUtils.getGameUI().msg(Utils.pfGridToWorld(path.getLast().pos).toString());
                //TODO syntetic points
                for (Graph.Vertex vert : path) {
                    Coord2d targetCoord = Utils.pfGridToWorld(vert.pos);

                    if(vert == path.getLast() && isHardMode || dummy!=null) {
                        Coord2d tcord = dummy != null ? dummy.rc : Finder.findGob(target_id).rc;
                        if (Math.abs(targetCoord.x - tcord.x) < 4) {
                            targetCoord.x = tcord.x;
                            targetCoord.y += tcord.y - targetCoord.y > 0 ? -2 : 2;
                        }
                        if (Math.abs(targetCoord.y - tcord.y) < 4) {
                            targetCoord.y = tcord.y;
                            targetCoord.x += tcord.x - targetCoord.x > 0 ? -2 : 2;
                        }
                    }

                    if (!(new GoTo(targetCoord).run(gui)).IsSuccess()) {
                        this.begin = gui.map.player().rc;
                        needRestart = true;
                        break;
                    }
                }
                if (!needRestart)
                    return Results.SUCCESS();
            } else {
                if (dn) {
//                    if(start_pos == end_poses.get(0) && NUtils.player().rc.dist(Utils.pfGridToWorld(pfmap.cells[start_pos]))
                    return Results.SUCCESS();
                }
                return
                        Results.ERROR("Can't find path");

            }
        }
    }

    public LinkedList<Graph.Vertex> construct() throws InterruptedException {
        return construct(false);
    }

    public LinkedList<Graph.Vertex> construct(boolean test) throws InterruptedException {
        LinkedList<Graph.Vertex> path = new LinkedList<>();
        int mul = 1;
        while (path.isEmpty() && mul < 200) {
            if(pfmap!=null && pfmap.lastMul)
                return null;
            pfmap = new NPFMap(begin, end, mul);
            pfmap.getBegin();
            pfmap.getEnd();
            if(pfmap.bad) {
                if (test) {
                    return null;
                } else {
                    NUtils.getGameUI().error("Unable to build grid of required size");
                    throw new InterruptedException();
                }
            }
            pfmap.waterMode = waterMode;
            pfmap.gatesAlwaysClosed = gatesAlwaysClosed;
            pfmap.build();
            CellsArray dca = null;
            if (dummy != null)
                dca = pfmap.addGob(dummy);

            start_pos = Utils.toPfGrid(begin).sub(pfmap.getBegin());
            end_pos = Utils.toPfGrid(end).sub(pfmap.getBegin());
            // Находим свободные начальные и конечные точки

            if (!fixStartEnd(test)) {
//                dn = true; //start == end
                return null;
            }

            if (dca != null)
                pfmap.setCellArray(dca);
            if(!test)
                NPFMap.print(pfmap.getSize(), pfmap.getCells());


            Graph res = null;
            if (pfmap.getCells()[end_pos.x][end_pos.y].val == 7) {
                Thread th = new Thread(res = new Graph(pfmap, start_pos, end_pos));
                th.start();
                th.join();
            } else {
                switch (mode) {
                    case NEAREST:
                    {
                        LinkedList<Graph> graphs = new LinkedList<>();
                        for (Coord ep : end_poses) {
                            graphs.add(new Graph(pfmap, start_pos, ep));
                        }
                        LinkedList<Thread> threads = new LinkedList<>();
                        for (Graph graph : graphs) {
                            Thread th;
                            threads.add(th = new Thread(graph));
                            th.start();
                        }
                        for (Thread t : threads) {
                            t.join();
                        }

                        graphs.sort(new Comparator<Graph>() {
                            @Override
                            public int compare(Graph o1, Graph o2) {
                                return (Integer.compare(o1.getPathLen(), o2.getPathLen()));
                            }
                        });
                        if (!graphs.isEmpty())
                            res = graphs.get(0);
                        break;
                    }
                    case Y_MAX:
                    case Y_MIN:
                    case X_MAX:
                    case X_MIN:
                    {

                        Comparator comp = new Comparator<Coord>() {
                            @Override
                            public int compare(Coord o1, Coord o2) {
                                Coord2d t01 = Utils.pfGridToWorld(pfmap.cells[o1.x][o1.y].pos);
                                Coord2d t02 = Utils.pfGridToWorld(pfmap.cells[o2.x][o2.y].pos);
                                switch (mode)
                                {
                                    case Y_MAX:
                                        return Double.compare(t02.y, t01.y);
                                    case Y_MIN:
                                        return Double.compare(t01.y, t02.y);
                                    case X_MAX:
                                        return Double.compare(t02.x, t01.x);
                                    case X_MIN:
                                        return Double.compare(t01.x, t02.x);
                                }
                                return 0;
                            }
                        };

                        end_poses.sort(comp);
                        Thread th = new Thread(res = new Graph(pfmap, start_pos, end_poses.get(0)));
                        th.start();
                        th.join();
                    }
                }
            }
//                for (Graph g: graphs)
//                {
//                    NPFMap.print(pfmap.getSize(), g.getVert());
//                }


            if (res != null) {
                if (!isDynamic)
                    path = getPath(pfmap, res.path);
                else
                    path = res.path;
//                NPFMap.print(pfmap.getSize(), res.getVert());
                if (!path.isEmpty()) {
                    return path;
                }
            }
            mul++;
        }
        return null;
    }

    private boolean fixStartEnd(boolean test) {
        NPFMap.Cell[][] cells = pfmap.getCells();
        if(start_pos.x < pfmap.size && start_pos.y<pfmap.size && start_pos.x>=0 && start_pos.y>=0) {
            if (cells[start_pos.x][start_pos.y].val != 0) {
                if (target_id >= 0 && cells[start_pos.x][start_pos.y].content.contains(target_id) && !test) {
                    dn = true;
                    return false;
                }
                ArrayList<Coord> st_poses = findFreeNear(start_pos, true);
                if (st_poses.isEmpty())
                    return false;
                if (cells[start_pos.x][start_pos.y].content.size() == 1 || (cells[start_pos.x][start_pos.y].content.size() == 2 && cells[start_pos.x][start_pos.y].content.contains((long) -1)))
                    for (Long id : cells[start_pos.x][start_pos.y].content) {
                        if (id != -1) {
                            gobInStartPos = Finder.findGob(id);
                        }
                    }
                start_pos = st_poses.get(0);
            }
            if (start_pos.equals(end_pos) && dummy == null) {
                dn = true;
                return false;
            }
            if (end_pos.x < pfmap.size && end_pos.y < pfmap.size && end_pos.x >= 0 && end_pos.y >= 0) {
                if (cells[end_pos.x][end_pos.y].val != 0) {
                    end_poses = findFreeNear(end_pos, false);
                    if (dummy != null || (isHardMode && target_id != -2 && Finder.findGob(target_id) != null)) {
                        Coord2d tcoord = (dummy != null) ? dummy.rc : Finder.findGob(target_id).rc;
                        ArrayList<Coord> best_poses = new ArrayList<>();
                        for (Coord coord : end_poses) {
                            Coord2d coord2d = Utils.pfGridToWorld(cells[coord.x][coord.y].pos);
                            if (coord2d.x + MCache.tileqsz.x > tcoord.x && coord2d.x - MCache.tileqsz.x < tcoord.x ||
                                    coord2d.y + MCache.tileqsz.y > tcoord.y && coord2d.y - MCache.tileqsz.y < tcoord.y)
                                best_poses.add(coord);
                        }
                        if (!best_poses.isEmpty())
                            end_poses = best_poses;
                    }

                    if (badDir != Double.MAX_VALUE && target_id > 0) {
                        Coord2d orientation = new Coord2d(1, 0).rot(badDir);
                        Coord2d tcoord = Finder.findGob(target_id).rc;
                        ArrayList<Coord> best_poses = new ArrayList<>();
                        for (Coord coord : end_poses) {
                            Coord2d coord2d = Utils.pfGridToWorld(cells[coord.x][coord.y].pos).sub(tcoord).norm();
                            if (coord2d.dot(orientation) >= -0.2)
                                best_poses.add(coord);
                            else
                                cells[coord.x][coord.y].val = 0;
                        }
                        end_poses = best_poses;
                    }
                    for (Coord coord : end_poses) {
                        if (start_pos.equals(coord) && dummy == null) {
                            dn = true;
                            return false;
                        }
                        cells[coord.x][coord.y].val = 7;
                    }

                } else {
                    cells[end_pos.x][end_pos.y].val = 7;
                }
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    private ArrayList<Coord> findFreeNear(Coord pos, boolean start) {
        if (!start) {
            if (target_id != -2) {
                Gob target = dummy;
                if (target == null) {
                    target = Finder.findGob(target_id);
                }
                if(target == null)
                    return null;
                CellsArray ca = target.ngob.getCA();
                return findFreeNearByHB(ca, target_id, dummy, start);
            }
        } else {
            if (pfmap.cells[pos.x][pos.y].val!=0 && pfmap.cells[pos.x][pos.y].val!=7) {
                ArrayList<Coord> targets = null;
                if(pfmap.cells[pos.x][pos.y].content.contains((long)-1)) {
                    CellsArray ca = dummy.ngob.getCA();
                    return findFreeNearByHB(ca, target_id, dummy, start);
                }
                else {
                    for (Long cand : pfmap.cells[pos.x][pos.y].content) {
                        CellsArray ca;
                        if (cand <= 0) {
                            ca = dummy.ngob.getCA();
                        } else {
                            Gob gcand = Finder.findGob(cand);
                            ca = gcand.ngob.getCA();
                        }
                        ArrayList<Coord> cords = findFreeNearByHB(ca, cand, dummy, start);
                        if (targets == null) {
                            targets = cords;
                        } else {
                            ArrayList<Coord> forRemove = new ArrayList<>();
                            for (Coord cord1 : targets) {
                                boolean found = false;
                                for (Coord coord2 : cords) {
                                    if (cord1.equals(coord2.x, coord2.y)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found)
                                    forRemove.add(cord1);
                            }
                            targets.removeAll(forRemove);
                        }
                    }
                }
                return targets == null ? new ArrayList<>():targets;
            }
        }
        return new ArrayList<>(Arrays.asList(pos));
    }

    private void checkAndAdd(Coord pos, ArrayList<Coord> coords, AtomicBoolean check) {
        //debug method
        if (pfmap.getCells()[pos.x][pos.y].val == 0) {
            pfmap.getCells()[pos.x][pos.y].val = 7;
            coords.add(pos);
        } else if (target_id != -2 && check != null) {
            if (!pfmap.getCells()[pos.x][pos.y].content.contains(target_id))
                check.set(false);
        }
    }

    public static ArrayList<Coord2d> getNearestHardPoints(Gob target) throws InterruptedException  {
        PathFinder pf = new PathFinder(target);
        pf.isHardMode = true;
        pf.construct(true);
        ArrayList<Coord2d> res = new ArrayList<>();
        for(Coord ep : pf.end_poses)
        {
            Coord2d coord2d = Utils.pfGridToWorld(pf.pfmap.cells[ep.x][ep.y].pos);
            if(Math.abs(coord2d.x-target.rc.x)>Math.abs(coord2d.y-target.rc.y))
            {
                coord2d.y = target.rc.y;
            }
            else
            {
                coord2d.x = target.rc.x;
            }
            res.add(coord2d);
        }
        return res;
    }

    public static boolean isAvailable(Gob target) throws InterruptedException {
        if(NUtils.player() == null)
            return false;
        PathFinder pf = new PathFinder(target);
        LinkedList<Graph.Vertex> res = pf.construct(true);
        return res != null || pf.dn;
    }

    public static boolean isAvailable(Coord2d target) throws InterruptedException {
        if(NUtils.player() == null)
            return false;
        PathFinder pf = new PathFinder(target);
        LinkedList<Graph.Vertex> res = pf.construct(true);
        return res != null || pf.dn;
    }

    public static boolean isAvailable(Gob target, boolean hardMode) throws InterruptedException {
        if(NUtils.player() == null)
            return false;
        PathFinder pf = new PathFinder(target);
        pf.isHardMode = true;
        return pf.construct(true) != null;
    }

    public static boolean isAvailable(Coord2d begin, Coord2d target, boolean gatesAlwaysClosed) throws InterruptedException {
        if(NUtils.player() == null)
            return false;
        PathFinder pf = new PathFinder(begin, target);
        pf.gatesAlwaysClosed = gatesAlwaysClosed;
        LinkedList<Graph.Vertex> res = pf.construct(true);
        return res != null || pf.dn;
    }

    public static boolean isAvailable(Coord2d begin, Gob target, boolean gatesAlwaysClosed) throws InterruptedException {
        if(NUtils.player() == null)
            return false;
        PathFinder pf = new PathFinder(begin, target);
        pf.gatesAlwaysClosed = gatesAlwaysClosed;
        LinkedList<Graph.Vertex> res = pf.construct(true);
        return res != null || pf.dn;
    }

    public PathFinder(Gob dummy, boolean virtual) {
        this(dummy);
        this.dummy = dummy;
        assert virtual;
    }


    private ArrayList<Coord> findFreeNearByHB(CellsArray ca, long target_id, Gob dummy, boolean isStart) {
        ArrayList<Coord> res = new ArrayList<>();
        if (ca != null) {
            for (int i = 0; i < ca.x_len; i++)
                for (int j = 0; j < ca.y_len; j++) {
                    int ii = i + ca.begin.x - pfmap.begin.x;
                    int jj = j + ca.begin.y - pfmap.begin.y;
                    Coord npfpos = new Coord(ii, jj);
                    if (ii > 0 && ii < pfmap.size && jj > 0 && jj < pfmap.size) {
                        if (ca.cells[i][j] != 0) {
                            for (int d = 0; d < 4; d++) {
                                Coord test_coord = npfpos.add(Coord.uecw[d]);
                                if (test_coord.x < pfmap.size && test_coord.x >= 0 && test_coord.y < pfmap.size && test_coord.y >= 0)
                                    if (pfmap.cells[test_coord.x][test_coord.y].val == 0 || pfmap.cells[test_coord.x][test_coord.y].val == 7) {
                                        if (isStart || pfmap.cells[npfpos.x][npfpos.y].content.size() == 1) {
                                            pfmap.getCells()[test_coord.x][test_coord.y].val = 7;
                                            res.add(test_coord);
                                        } else if (pfmap.cells[npfpos.x][npfpos.y].content.size() > 1) {
                                            Coord2d test2d_coord = Utils.pfGridToWorld(pfmap.cells[test_coord.x][test_coord.y].pos);
                                            double dst = 9000, testdst;
                                            long res_id = -2;
                                            for (long id : pfmap.cells[npfpos.x][npfpos.y].content) {
                                                if (id >= 0) {
                                                    if ((testdst = Finder.findGob(id).rc.dist(test2d_coord)) < dst) {
                                                        res_id = id;
                                                        dst = testdst;
                                                    }
                                                } else {
                                                    if ((testdst = dummy.rc.dist(test2d_coord)) < dst) {
                                                        res_id = id;
                                                        dst = testdst;
                                                    }
                                                }
                                                if (res_id == target_id) {
                                                    pfmap.getCells()[test_coord.x][test_coord.y].val = 7;
                                                    res.add(test_coord);
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
        }

        if(isStart) {
            Coord2d player = NUtils.player().rc;
            if(Finder.findGob(target_id)!=null) {
                Coord2d targerc = Finder.findGob(target_id).rc;
                Coord2d playerdir = player.sub(targerc).norm();


                Comparator comp = new Comparator<Coord>() {
                    @Override
                    public int compare(Coord o1, Coord o2) {
                        Coord2d t01 = Utils.pfGridToWorld(pfmap.cells[o1.x][o1.y].pos).sub(targerc).norm();
                        Coord2d t02 = Utils.pfGridToWorld(pfmap.cells[o2.x][o2.y].pos).sub(targerc).norm();

                        return Double.compare(t02.dot(playerdir), t01.dot(playerdir));
                    }
                };

                res.sort(comp);
            }
//            Gob target = (dummy==null|| target_id!=-1)?Finder.findGob(target_id):dummy;
//            System.out.println("+++++++++++++++++++++++");
//            System.out.println("Target" + ((target!=dummy)?target.ngob.name:"") + "rc" + target.rc.toString() + " id " + target.id);
//            System.out.println("targetrc " + targerc);
//            System.out.println("Player" + " rc " + player.toString());
//            for(Coord coord: res)
//            {
//                Coord2d pos = Utils.pfGridToWorld(pfmap.cells[coord.x][coord.y].pos);
//                System.out.println(pos.toString() + "|" + " cos " + pos.sub(targerc).norm().dot(playerdir));
//            }
        }

        return res;
    }

    boolean getDNStatus() {
        return dn;
    }

    /**
     * Finds common approach points for two objects (e.g., workstation and barrel).
     * Returns approach points that are adjacent to BOTH objects.
     * 
     * @param gob1 First object (e.g., workstation)
     * @param gob2 Second object (e.g., barrel)
     * @return ArrayList of common approach points, or empty list if none found
     */
    public static ArrayList<Coord2d> findCommonApproachPoints(Gob gob1, Gob gob2) throws InterruptedException {
        ArrayList<Coord2d> result = new ArrayList<>();
        
        if (gob1 == null || gob2 == null) {
            NUtils.getGameUI().msg("findCommonApproachPoints: gob1 or gob2 is null");
            return result;
        }
        
        // Get approach points for both objects
        ArrayList<Coord2d> points1 = getNearestHardPoints(gob1);
        ArrayList<Coord2d> points2 = getNearestHardPoints(gob2);
        
        NUtils.getGameUI().msg("findCommonApproachPoints: gob1 has " + 
                (points1 != null ? points1.size() : 0) + " approach points, gob2 has " + 
                (points2 != null ? points2.size() : 0) + " approach points");
        
        if (points1 == null || points1.isEmpty() || points2 == null || points2.isEmpty()) {
            NUtils.getGameUI().msg("findCommonApproachPoints: One of the objects has no approach points");
            return result;
        }
        
        // Find common points (within tolerance)
        // Tolerance of 14 units to cover typical diagonal placement distances (~12-13 units)
        double tolerance = 14.0;
        for (Coord2d p1 : points1) {
            for (Coord2d p2 : points2) {
                double dist = p1.dist(p2);
                if (dist < tolerance) {
                    // Use the midpoint as the common point
                    Coord2d midpoint = new Coord2d((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
                    result.add(midpoint);
                    NUtils.getGameUI().msg("findCommonApproachPoints: Found common point at " + midpoint + 
                            " (p1=" + p1 + ", p2=" + p2 + ", dist=" + String.format("%.2f", dist) + ")");
                }
            }
        }
        
        if (result.isEmpty()) {
            NUtils.getGameUI().msg("findCommonApproachPoints: No common points found within tolerance " + tolerance);
            // Log some distances for debugging
            if (!points1.isEmpty() && !points2.isEmpty()) {
                double minDist = Double.MAX_VALUE;
                for (Coord2d p1 : points1) {
                    for (Coord2d p2 : points2) {
                        minDist = Math.min(minDist, p1.dist(p2));
                    }
                }
                NUtils.getGameUI().msg("findCommonApproachPoints: Minimum distance between approach points: " + 
                        String.format("%.2f", minDist));
            }
        }
        
        // Sort by distance to player
        Coord2d playerPos = NUtils.player().rc;
        result.sort((a, b) -> Double.compare(a.dist(playerPos), b.dist(playerPos)));
        
        return result;
    }
    
    /**
     * Finds the nearest common approach point for two objects.
     * This is useful when character needs to interact with both objects (e.g., crafting with barrel at workstation).
     * Applies hardMode alignment rule: aligns one coordinate (X or Y) with the workstation.
     * 
     * @param gob1 First object (workstation)
     * @param gob2 Second object (barrel)
     * @return Nearest common approach point with hardMode alignment applied, or null if none found
     */
    public static Coord2d findNearestCommonApproachPoint(Gob gob1, Gob gob2) throws InterruptedException {
        ArrayList<Coord2d> commonPoints = findCommonApproachPoints(gob1, gob2);
        if (commonPoints.isEmpty()) {
            return null;
        }
        
        // Use gob1 (workstation) as the reference for hardMode alignment
        Coord2d wsCoord = gob1.rc;
        
        for (Coord2d point : commonPoints) {
            // Apply hardMode rule: align one coordinate with workstation
            Coord2d alignedPoint = applyHardModeAlignment(point, wsCoord);
            
            NUtils.getGameUI().msg("findNearestCommonApproachPoint: Point " + point + 
                    " aligned to " + alignedPoint + " (hardMode rule applied)");
            
            return alignedPoint;
        }
        
        NUtils.getGameUI().msg("findNearestCommonApproachPoint: No common points found");
        return null;
    }
    
    /**
     * Apply hardMode alignment rule: replace one coordinate (X or Y) with the target's coordinate.
     * Replaces whichever coordinate is closer to the target.
     */
    private static Coord2d applyHardModeAlignment(Coord2d point, Coord2d targetCoord) {
        Coord2d result = new Coord2d(point.x, point.y);
        
        // Check which coordinate is closer to target - replace that one
        if (Math.abs(result.x - targetCoord.x) < Math.abs(result.y - targetCoord.y)) {
            // X is closer - replace X with target's X
            result.x = targetCoord.x;
        } else {
            // Y is closer - replace Y with target's Y
            result.y = targetCoord.y;
        }
        
        return result;
    }

//    boolean gridIsBiggerThanVisibleArea(NPFMap map) {
//            Utils.pfGridToWorld(map.getBegin())
//
//
//            int size = VISIBLE_AREA;
//            Coord2d a = new Coord2d(Math.min(begin.x, end.x), Math.min(begin.y, end.y));
//            Coord2d b = new Coord2d(Math.max(begin.x, end.x), Math.max(begin.y, end.y));
//            int dsize = Math.min(size, Math.max(8,((int) Math.ceil(b.dist(a) / MCache.tilehsz.x)) * mul));
//            return dsize == VISIBLE_AREA;
//    }
}
