package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.res.ui.obj.buddy.Buddy;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.GetCurs;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Arrays;

import static haven.OCache.posres;

public class AggroNearestBorka implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> cands = Finder.findGobs(NUtils.player().rc, new NAlias("borka"),new NAlias(new ArrayList<String>(),new ArrayList<>(Arrays.asList("dead", "knock"))),2000);
        ArrayList<Gob> targets = new ArrayList<>();
        for(Gob gob: cands) {
            Buddy buddy = gob.getattr(Buddy.class);
            if(buddy==null)
            {
                if(!NUtils.getUI().sess.glob.party.memb.containsKey(gob.id))
                {
                    targets.add(gob);
                }
            }
            else
            {
                if(buddy.b.group!=1)
                {
                    targets.add(gob);
                }
            }
        }
        if(!targets.isEmpty()) {
            targets.sort(NUtils.d_comp);
            Gob target = targets.get(0);
            if (target != null) {
                NUtils.attack(target, false);
                if (!NParser.checkName(NUtils.getCursorName(), "arw")) {
                    NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres), 3, 0);
                    NUtils.getUI().core.addTask(new GetCurs("arw"));
                }
            }
        }
        return Results.SUCCESS();
    }
}
