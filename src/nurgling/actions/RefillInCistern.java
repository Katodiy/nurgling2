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
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

import static nurgling.tools.Finder.findLiftedbyPlayer;

public class RefillInCistern implements Action
{

    ArrayList<Container> conts;
    Pair<Coord2d,Coord2d> area;
    NAlias content;

    public RefillInCistern(Pair<Coord2d,Coord2d> area, NAlias content) {
        this.area = area;
        this.content = content;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob player = NUtils.player();
        if (player == null) {
            return Results.ERROR("Player not found.");
        }

        // Find the barrel that's currently lifted by the player
        // The Following attribute validates the player is carrying the barrel
        Gob barrel = findLiftedbyPlayer();

        Following fl;
        if (barrel == null || (fl = barrel.getattr(Following.class)) == null || fl.tgt != player.id) {
            return Results.ERROR("Barrel not found.");
        }

        Gob waterSource = Finder.findGob(area, new NAlias("cistern", "well"));
        if(waterSource == null)
        {
            return Results.ERROR("Water source (cistern or well) not found.");
        }
        boolean isOverlay = NUtils.isOverlay(barrel, content);
        new PathFinder(waterSource).run(gui);
        NUtils.activateGob(waterSource);
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
                NUtils.activateGob(waterSource);
                NUtils.addTask(new IsOverlay(barrel, content));
                return Results.ERROR("NO MORE FLUID");
            }
        }
        else
        {
            NUtils.addTask(new IsOverlay(barrel, content));
            if(!NUtils.isOverlay(barrel, content))
            {
                return Results.ERROR("NO FLUID");
            }
        }
        return Results.SUCCESS();
    }
}
