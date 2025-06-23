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

import static haven.MCache.cmaps;
import static haven.OCache.posres;

public class RestoreResources implements Action{

    Gob gob = null;

    Coord2d target_pos = null;
    long id;
    Coord oldCoord = null;

    public RestoreResources(Gob gob) {
        this.gob = gob;
    }

    public RestoreResources(Coord2d target_pos) {
        this.target_pos = target_pos;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
            Coord pltc = (new Coord2d(target_pos.x / MCache.tilesz.x, target_pos.y / MCache.tilesz.y)).floor();
            synchronized (NUtils.getGameUI().ui.sess.glob.map.grids) {
                if (NUtils.getGameUI().ui.sess.glob.map.grids.containsKey(pltc.div(cmaps))) {
                    MCache.Grid g = NUtils.getGameUI().ui.sess.glob.map.getgridt(pltc);
                    oldCoord = (target_pos.sub(g.ul.mul(Coord2d.of(11, 11)))).floor(posres);
                    id = g.id;
                }
            }
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
                synchronized (NUtils.getGameUI().ui.sess.glob.map.grids) {
                    if (NUtils.getGameUI().ui.sess.glob.map.grids.containsKey(pltc.div(cmaps))) {
                        for(MCache.Grid g : NUtils.getGameUI().ui.sess.glob.map.grids.values())
                        {
                            if(g.id == id)
                            {
                                target_pos = oldCoord.mul(posres).add(g.ul.mul(Coord2d.of(11, 11)));
                                break;
                            }
                        }
                    }
                }
                new PathFinder(gob==null?NGob.getDummy(target_pos, 0, new NHitBox(new Coord2d(-5.5,-5.5),new Coord2d(5.5,5.5))):gob, true).run(gui);
            }
            return Results.SUCCESS();

    }
}
