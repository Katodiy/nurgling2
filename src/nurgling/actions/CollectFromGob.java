package nurgling.actions;

import haven.*;
import nurgling.NFlowerMenu;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.NFlowerMenuIsClosed;
import nurgling.tasks.WaitCollectState;
import nurgling.tasks.WaitPose;
import nurgling.tasks.WaitPrepBlocksState;
import nurgling.tools.NAlias;

import static haven.OCache.posres;


public class CollectFromGob implements Action{

    Gob target;
    String action;
    String pose;
    boolean withPiles = false;
    Coord targetSize = null;
    int marker = - 1;
    public CollectFromGob(Gob target, String action, String pose, Coord targetSize, NAlias targetItems, Pair<Coord2d, Coord2d> pileArea) {
        this.target = target;
        this.action = action;
        this.pose = pose;
        this.targetSize = targetSize;
        this.withPiles = true;
        this.targetItems = targetItems;
        this.pileArea = pileArea;
    }

    public CollectFromGob(Gob target, String action, String pose, boolean withPiles, Coord targetSize, int marker, NAlias targetItems, Pair<Coord2d, Coord2d> pileArea) {
        this.target = target;
        this.action = action;
        this.pose = pose;
        this.withPiles = withPiles;
        this.targetSize = targetSize;
        this.marker = marker;
        this.targetItems = targetItems;
        this.pileArea = pileArea;
    }

    NAlias targetItems;
    Pair<Coord2d,Coord2d> pileArea = null;
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WaitCollectState wcs = null;
        do {
            if (NUtils.getGameUI().getInventory().getNumberFreeCoord(targetSize) == 0) {
                if (withPiles)
                    new TransferToPiles(pileArea, targetItems).run(gui);
            }
            if(marker!=-1)
            {
                if((target.ngob.getModelAttribute()&marker)!=marker)
                {
                    return Results.SUCCESS();
                }
            }
            gui.map.wdgmsg("click", Coord.z, target.rc.floor(posres), 3, 0, 1, (int) target.id, target.rc.floor(posres),
                    0, -1);
            NFlowerMenu fm = NUtils.findFlowerMenu();
            if (fm != null) {
                if (fm.hasOpt(action)) {
                    new PathFinder(target).run(gui);
                    if (fm.chooseOpt(action)) {
                        NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                        NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), pose));
                        wcs = new WaitCollectState(target, targetSize);
                        NUtils.getUI().core.addTask(wcs);
                    } else {
                        NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                    }
                } else {
                    fm.wdgmsg("cl", -1);
                    NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                    return Results.FAIL();
                }
            } else
            {
                return Results.FAIL();
            }

        }
        while (wcs!=null && wcs.getState()!= WaitCollectState.State.NOITEMSFORCOLLECT);
        return Results.SUCCESS();
    }
}
