package nurgling.actions;

import haven.GAttrib;
import haven.Gob;
import haven.Utils;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class AddHearthFire implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        boolean needToCreateNewRoute = true;
        Route existingRoute = null;

        for(Route route : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().values()) {
            for(Route.RouteSpecialization specialisation : route.spec) {
                if(specialisation.name.equals("HearthFires")) {
                    needToCreateNewRoute = false;
                    existingRoute = route;
                    break;
                }
            }
        }

        if(needToCreateNewRoute) {
            ((NMapView) NUtils.getGameUI().map).addHearthFireRoute();

            for(Route route : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().values()) {
                for(Route.RouteSpecialization specialisation : route.spec) {
                    if(specialisation.name.equals("HearthFires")) {
                        existingRoute = route;
                        break;
                    }
                }
            }
        }


        if(existingRoute == null) {
            Results.FAIL();
        }

        for(RoutePoint routePoint : existingRoute.waypoints) {
            if(routePoint.hearthFirePlayerName.equals(NUtils.getGameUI().getCharInfo().chrid)) {
                existingRoute.deleteWaypoint(routePoint);
            }
        }

        ArrayList<Gob> fires = Finder.findGobs(new NAlias("gfx/terobjs/pow"));

        ArrayList<Gob> candidates = new ArrayList<>();

        for(Gob fire : fires) {
            if (fire.ngob.getModelAttribute() == 17) {
                candidates.add(fire);
            }
        }

        if(candidates.isEmpty()) {
            System.out.println("Hearth Fire not found.");
            return Results.FAIL();
        }

        Gob ourFire = null;
        double currentDistance = 99999999;

        Gob player = NUtils.player();
        if(player != null) {
            for(Gob fire : candidates) {
                double distanceToFire = player.rc.dist(fire.rc);
                if (distanceToFire < currentDistance) {
                    ourFire = fire;
                    currentDistance = distanceToFire;
                }
            }
        }


        new PathFinder(ourFire).run(gui);

        existingRoute.addHearthFireWaypoint(NUtils.getGameUI().getCharInfo().chrid);

        NConfig.needRoutesUpdate();

        return Results.SUCCESS();
    }
}
