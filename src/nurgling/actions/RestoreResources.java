package nurgling.actions;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NGameUI;
import nurgling.NGob;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.actions.bots.Eater;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.routes.RoutePoint;
import nurgling.tools.Finder;

import static haven.MCache.cmaps;
import static haven.OCache.posres;

public class RestoreResources implements Action{

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
            if ( NUtils.getStamina() < 0.5 ) {
                if (!new Drink(0.9, false).run(gui).IsSuccess()) {
                    new FillWaterskins(true).run(gui);
                    if (!new Drink(0.9, false).run(gui).IsSuccess()) {
                        return Results.ERROR("Failed to restore stamina - no water available");
                    }
                }
            }
            if(NUtils.getEnergy()<0.35)
            {
                Eater eater = new Eater(true);
                Results eatResult = eater.run(gui);
                if (!eatResult.IsSuccess()) {
                    return Results.ERROR("Failed to restore energy - no food available");
                }
            }
            return Results.SUCCESS();
    }
}
