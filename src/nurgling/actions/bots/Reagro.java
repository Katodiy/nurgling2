package nurgling.actions.bots;

import haven.Coord;
import haven.Fightview;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.GetCurs;
import nurgling.tasks.WaitAnyEscaper;
import nurgling.tasks.WaitBattleWindow;
import nurgling.tasks.WaitRelationState;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

import static haven.OCache.posres;

public class Reagro implements Action {
    boolean isEnabled = true;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (isEnabled) {
            NUtils.addTask(new WaitBattleWindow());
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
            WaitAnyEscaper wae = new WaitAnyEscaper(ids);
            NUtils.addTask(wae);
            if(wae.getEscaper()!=-1)
            {
                Gob escaper = Finder.findGob(wae.getEscaper());
                if (escaper != null && escaper.pose()!= null && !NParser.checkName(escaper.pose(), new NAlias("dead", "knock")) &&  escaper.rc.dist(NUtils.player().rc) < 200) {
                    NUtils.attack(escaper, false);
                }
            }

            NUtils.getDefaultCur();
        }

        return Results.SUCCESS();
    }
}
