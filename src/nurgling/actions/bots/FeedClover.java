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

public class FeedClover implements Action {
    NAlias krtters =new NAlias(new ArrayList<String>(Arrays.asList("horse", "cattle", "boar", "goat", "sheep")), new ArrayList<String>(Arrays.asList("stallion", "mare")));
    NAlias clover = new NAlias("Clover");
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem item = gui.getInventory().getItem(clover);
        if(item==null)
        {
            return Results.ERROR("No clover");
        }
        Coord pos = item.c.div(Inventory.sqsz);
        NUtils.takeItemToHand(item);
        Gob gob = Finder.findGob(krtters);
        if(gob!=null) {
            NUtils.activateItem(gob, false);
            WaitPoseOrMsg wpom1 = new WaitPoseOrMsg(NUtils.player(),"gfx/borka/animaltease", "The animal eye");
            NUtils.addTask(wpom1);
            if(wpom1.isError())
            {
                gui.getInventory().dropOn(pos, clover);
            }
            else {
                WaitPoseOrMsg wpom2 = new WaitPoseOrMsg(NUtils.player(), "gfx/borka/idle", "The animal loses");
                NUtils.addTask(wpom2);
                if (wpom2.isError()) {
                    gui.getInventory().dropOn(pos, clover);
                    NUtils.player().addcustomol(new NCustomResult(NUtils.player(), "fail"));
                } else {
                    gob.addcustomol(new NCustomResult(gob, "success"));
                }
            }
        }

        return Results.SUCCESS();
    }
}
