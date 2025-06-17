package nurgling.actions.bots;

import haven.Fightview;
import haven.Gob;
import haven.WItem;
import haven.res.ui.tt.leashed.Leashed;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.overlays.NCustomResult;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitAnyEscaper;
import nurgling.tasks.WaitPoseOrMsg;
import nurgling.tasks.WaitRelationState;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;

import static nurgling.NUtils.getGameUI;

public class TaimingAnimal implements Action {
    NAlias krtters =new NAlias(new ArrayList<String>(Arrays.asList("horse", "cattle", "boar", "goat", "sheep", "reindeer")), new ArrayList<String>(Arrays.asList("stallion", "mare")));
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob taiming_animal = Finder.findGob(NUtils.player().rc, krtters, new NAlias("fgtidle"), 10000);
        if (taiming_animal != null) {
            NUtils.attack(taiming_animal, false);
            ArrayList<Long> ids = new ArrayList<>();
            synchronized (NUtils.getGameUI().fv.lsrel) {
                for (Fightview.Relation rel : NUtils.getGameUI().fv.lsrel) {
                    if (rel.gobid >= 0) {
                        ids.add(rel.gobid);
                    }
                    if (rel.gst != 1) {
                        NUtils.getGameUI().fv.wdgmsg("give", (int) rel.gobid, 1);
                        NUtils.addTask(new WaitRelationState(rel.gobid, 1));
                    }
                }
            }
            NUtils.getDefaultCur();
            NUtils.addTask(new WaitAnyEscaper(ids));

            if ((Boolean) NConfig.get(NConfig.Key.ropeAfterTaiming) && !taiming_animal.pose().contains("dead")) {
                WItem rope = gui.getInventory().getItem(new NAlias("Rope"), Leashed.class);
                if (rope != null) {
                    NUtils.takeItemToHand(rope);
                    NUtils.activateItem(taiming_animal, false);
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            if (getGameUI().vhand != null) {
                                return (((NGItem) getGameUI().vhand.item).getInfo(Leashed.class) != null);
                            }
                            return false;
                        }
                    });
                    gui.getInventory().dropOn(gui.getInventory().findFreeCoord(getGameUI().vhand));
                }
            }
        }
        return Results.SUCCESS();
    }
}
