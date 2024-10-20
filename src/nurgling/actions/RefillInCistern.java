package nurgling.actions;

import haven.Coord2d;
import haven.Following;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.IsOverlay;
import nurgling.tasks.WaitSound;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class RefillInCistern implements Action
{

    ArrayList<Container> conts;
    Pair<Coord2d,Coord2d> area;
    NAlias content;

    public RefillInCistern( Pair<Coord2d,Coord2d> area, NAlias content) {
        this.area = area;
        this.content = content;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob player = NUtils.player();
        if(player == null || !NParser.checkName(player.pose(), "gfx/borka/banzai"))
        {

            return Results.ERROR("Barrel not found.");
        }
        Gob barrel = Finder.findGob(new NAlias("barrel"));
        Following fl ;
        if(barrel == null || (fl = barrel.getattr(Following.class))==null || fl.tgt!=player.id)
            return Results.ERROR("Barrel not found.");

        Gob cistern = Finder.findGob(area, new NAlias("cistern"));
        if(cistern == null)
        {
            return Results.ERROR("Cistern not found.");
        }
        boolean isOverlay = NUtils.isOverlay(barrel, content);
        new PathFinder(cistern).run(gui);
        NUtils.activateGob(cistern);
        if(isOverlay) {
            WaitSound ws = new WaitSound("sfx/fx/water");
            NUtils.addTask(ws);
            if(!ws.getResult())
            {
                if(NUtils.isOverlay(barrel, content))
                    return Results.SUCCESS();
            }
            if(!NUtils.isOverlay(barrel, content))
            {
                NUtils.activateGob(cistern);
                NUtils.addTask(new IsOverlay(barrel, content));
                return Results.ERROR("NO MORE FLUID");
            }
        }
        else
        {
            NUtils.addTask(new IsOverlay(barrel, content));
            if(!NUtils.isOverlay(barrel, content))
            {
                NUtils.getGameUI().error("NO FLUID");
                throw new InterruptedException();
            }
        }
        return Results.SUCCESS();
    }
}
