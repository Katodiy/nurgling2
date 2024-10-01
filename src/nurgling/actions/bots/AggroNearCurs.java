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
import nurgling.tasks.WaitAnyEscaper;
import nurgling.tasks.WaitBattleWindow;
import nurgling.tasks.WaitRelationState;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Arrays;

import static haven.OCache.posres;

public class AggroNearCurs implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Coord2d lc = ((NMapView)NUtils.getGameUI().map).getLCoord();
        Gob target = Finder.findGob(lc, new NAlias("kritter"),new NAlias(new ArrayList<String>(),new ArrayList<>(Arrays.asList("dead", "knock"))),200);
        if(target!=null) {
            NUtils.attack(target, false);
            if(!NParser.checkName(NUtils.getCursorName(), "arw")) {
                NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres),3, 0);
                NUtils.getUI().core.addTask(new GetCurs("arw"));
            }
        }
        return Results.SUCCESS();
    }
}
