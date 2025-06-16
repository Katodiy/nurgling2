package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.overlays.NCustomResult;
import nurgling.tasks.WaitPose;
import nurgling.tasks.WaitPoseOrMsg;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;

import static nurgling.NUtils.getGameUI;

public class FeedClover implements Action {
    static ArrayList<Long> feeded = new ArrayList<>();
    NAlias krtters =new NAlias(new ArrayList<String>(Arrays.asList("horse", "cattle", "boar", "goat", "sheep")), new ArrayList<String>(Arrays.asList("stallion", "mare")));
    NAlias clover = new NAlias("Clover");
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem item = gui.getInventory().getItem(clover);
        if(item==null)
        {
            return Results.ERROR("No clover");
        }
        NUtils.takeItemToHand(item);
        Gob gob = Finder.findGob(krtters, feeded);
        if(gob!=null) {
            NUtils.activateItem(gob, false);
            WaitPoseOrMsg wpom1 = new WaitPoseOrMsg(NUtils.player(),"gfx/borka/animaltease", new NAlias("The animal eye"));
            NUtils.addTask(wpom1);
            if(wpom1.isError())
            {
                gui.getInventory().dropOn(gui.getInventory().findFreeCoord(getGameUI().vhand));
            }
            else {
                WaitPoseOrMsg wpom2 = new WaitPoseOrMsg(NUtils.player(), "gfx/borka/idle", new NAlias("The animal loses"));
                NUtils.addTask(wpom2);
                if (wpom2.isError()) {
                    gui.getInventory().dropOn(gui.getInventory().findFreeCoord(getGameUI().vhand));
                    NUtils.player().addcustomol(new NCustomResult(NUtils.player(), "fail"));
                } else {
                    gob.addcustomol(new NCustomResult(gob, "success"));
                }
            }
            feeded.add(gob.id);
        }

        return Results.SUCCESS();
    }
}
