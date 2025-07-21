package nurgling.actions;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NGameUI;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.pf.CellsArray;
import nurgling.pf.Graph;
import nurgling.pf.NPFMap;
import nurgling.pf.Utils;
import nurgling.tasks.DynMovingCompleted;
import nurgling.tasks.IsMoving;
import nurgling.tasks.MovingCompleted;
import nurgling.tasks.WaitPath;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import static haven.OCache.posres;

public class DynamicPf implements Action
{
    Gob target;
    boolean isVirtual = false;

    public DynamicPf(Gob gob)
    {
        this.target = gob;
    }

    public DynamicPf(Gob dummy, boolean b) {
        this.target = dummy;
        this.isVirtual = b;
    }

    boolean virtual = false;
    public boolean isHardMode = false;
    public class WorkerPf implements Runnable
    {
        public LinkedList<Graph.Vertex> path;
        public AtomicBoolean ready = new AtomicBoolean(false);
        public NPFMap pfMap;
        @Override
        public void run() {
            try {
                PathFinder pf = (isVirtual) ? new PathFinder(target, isVirtual) : new PathFinder(target);
                pf.isDynamic = true;
                pf.isHardMode = isHardMode;
                path = pf.construct();
                pfMap = pf.pfmap;
                ready.set(true);
            } catch (InterruptedException e) {

            }
        }

        public boolean checkDN() {

            PathFinder pf = (isVirtual) ? new PathFinder(target, isVirtual) : new PathFinder(target);
            pf.isDynamic = true;
            pf.isHardMode = isHardMode;
            try {
                path = pf.construct();

            } catch (InterruptedException e) {

            }
            return pf.getDNStatus();
        }
    }




    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WorkerPf wpf = new WorkerPf();

        do {
            NUtils.getUI().core.addTask((new WaitPath(wpf)));
            LinkedList<Graph.Vertex> path = wpf.path;
            updatePath(path, wpf,target);
            while (!path.isEmpty()) {
                Coord2d targetCoord = Utils.pfGridToWorld(path.pop().pos);
                gui.map.wdgmsg("click", Coord.z, targetCoord.floor(posres), 1, 0);
                IsMoving im;
                NUtils.getUI().core.addTask(im = new IsMoving(targetCoord, 20));
                if (im.getResult()) {
                    DynMovingCompleted dmc;
                    NUtils.getUI().core.addTask((dmc = new DynMovingCompleted(new WorkerPf(), target, targetCoord)));
                    if (dmc.needUpdate()) {
                        if (dmc.wpf.path != null) {
                            path = dmc.wpf.path;
                            updatePath(path, dmc.wpf,target);
                        }
                    }
                } else {
                    wpf = new WorkerPf();
                    NUtils.getUI().core.addTask((new WaitPath(wpf)));
                    path = wpf.path;
                    updatePath(path, wpf,target);
                }
            }
        }
        while (!wpf.checkDN());
        return Results.SUCCESS();
    }

    private static void updatePath(LinkedList<Graph.Vertex> path, WorkerPf wpf, Gob target) {
        if (!path.isEmpty()) {
            LinkedList<Graph.Vertex> for_remove = new LinkedList<>();
            int shift = 1;
            while (shift < path.size()) {
                Coord2d first = NUtils.player().rc;
                Coord2d second = Utils.pfGridToWorld(path.get(shift).pos);
                Coord2d fsdir = second.sub(first);
                Coord2d center = fsdir.div(2).add(first);
                int hlen = (int) Math.ceil(fsdir.len() / 2);
                NHitBox hb = new NHitBox(new Coord(-1, -hlen), new Coord(1, hlen));

                if (wpf.pfMap.checkCA(new CellsArray(hb, fsdir.curAngle(), center)).isEmpty()) {
                    for_remove.add(path.get(shift - 1));
                    shift++;
                } else {
                    /// TODO здесь тоже надо было бы добавить проверку обычного оптимизатора, но лень
                    break;
                }
            }
            path.removeAll(for_remove);
            Graph.getPath(wpf.pfMap, path);
        }
    }
}
