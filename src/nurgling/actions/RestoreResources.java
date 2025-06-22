package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NGob;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.actions.bots.Eater;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.routes.RoutePoint;

public class RestoreResources implements Action{

    Gob gob = null;

    Coord2d target_pos = null;

    public RestoreResources(Gob gob) {
        this.gob = gob;
    }

    public RestoreResources(Coord2d target_pos) {
        this.target_pos = target_pos;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
            RoutePoint rp = null;
            boolean needPf = false;
            if ( NUtils.getStamina() < 0.5 ) {
                if (!new Drink(0.9, false).run(gui).IsSuccess()) {
                    FillWaterskins fv;
                    if (((fv = new FillWaterskins(true)).run(gui).IsSuccess())) {
                        if (fv.routePoints != null) {
                            rp = fv.routePoints.getFirst();
                        }
                        needPf = true;
                    }
                    if (!new Drink(0.9, false).run(gui).IsSuccess()) {
                       Results.FAIL();
                    }
                }
            }
            if(NUtils.getEnergy()<0.35)
            {
                Eater eater = new Eater(true);
                eater.run(gui);
                if(eater.routePoints!=null && rp == null)
                {
                    rp = eater.routePoints.getFirst();
                }
                needPf = true;
            }
            if(rp!=null)
            {
                new RoutePointNavigator(rp).run(NUtils.getGameUI());
            }
            if(needPf)
            {
                new PathFinder(gob==null?NGob.getDummy(target_pos, 0, new NHitBox(new Coord2d(-5.5,-5.5),new Coord2d(5.5,5.5))):gob, true).run(gui);
            }
            return Results.SUCCESS();

    }
}
