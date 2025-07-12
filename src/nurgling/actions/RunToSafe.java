package nurgling.actions;

import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.routes.RoutePoint;
import nurgling.widgets.Specialisation;

import java.util.List;

public class RunToSafe implements Action{
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NUtils.setSpeed(4);
        NArea nArea = NContext.findSpecGlobal(Specialisation.SpecName.safe.toString());
        if(nArea!=null) {
            List<RoutePoint> routePoints = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(nArea));
            if(routePoints!=null && !routePoints.isEmpty())
                new RoutePointNavigator(routePoints.get(routePoints.size()-1)).run(NUtils.getGameUI());
            return Results.SUCCESS();
        }
        else {
            return Results.FAIL();
        }
    }
}
