package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Fightview;
import haven.Gob;
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

public class AttackAll implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> cands = Finder.findGobs(NUtils.player().rc, new NAlias(new ArrayList<>(Arrays.asList("kritter")), new ArrayList<>(Arrays.asList("horse"))),new NAlias(new ArrayList<String>(),new ArrayList<>(Arrays.asList("dead", "knock"))),140);
        gui.msg(String.valueOf(cands.size()));
        for(Gob target : cands) {
            boolean isFound = false;
            if(gui.fv!=null && gui.fv.lsrel!=null) {
                for (Fightview.Relation rel : gui.fv.lsrel)
                {
                    if(target.id == rel.gobid) {
                        isFound = true;
                        break;
                    }
                }
            }
            if(!isFound)
                if (target != null) {
                    NUtils.attack(target, false);
                }
        }
        if (!NParser.checkName(NUtils.getCursorName(), "arw")) {
            NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres), 3, 0);
            NUtils.getUI().core.addTask(new GetCurs("arw"));
        }
        return Results.SUCCESS();
    }
}
