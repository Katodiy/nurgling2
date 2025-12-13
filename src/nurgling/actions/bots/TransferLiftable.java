package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import haven.Resource;
import haven.UI;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.conf.NCarrierProp;
import nurgling.conf.NChopperProp;
import nurgling.routes.RoutePoint;
import nurgling.tasks.WaitCheckable;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class TransferLiftable implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.Carrier w = null;
        NCarrierProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.Carrier()), UI.scale(200,200))));
            prop = w.prop;
        }
        catch (InterruptedException e)
        {
            throw e;
        }
        finally {
            if(w!=null)
                w.destroy();
        }
        if(prop == null)
        {
            return Results.ERROR("No config");
        }



        SelectArea insa;
        NUtils.getGameUI().msg("Please, select input area");
        (insa = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);

        SelectArea outsa;
        NUtils.getGameUI().msg("Please, select output area");
        (outsa = new SelectArea(Resource.loadsimg("baubles/outputArea"))).run(gui);
        ArrayList<Gob> logs;
        RoutePoint inRoutePoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPoint(gui, insa.getRCArea().b.sub(insa.getRCArea().a).div(2).add(insa.getRCArea().a));
        RoutePoint outRoutePoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPoint(gui,outsa.getRCArea().b.sub(outsa.getRCArea().a).div(2).add(outsa.getRCArea().a));
        if(outRoutePoint!=null && inRoutePoint!=null)
        {
            if(inRoutePoint.toCoord2d(gui.map.glob.map).dist(outRoutePoint.toCoord2d(gui.map.glob.map))<450)
            {
                inRoutePoint = outRoutePoint = null;
            }

        }
        while (!(logs = Finder.findGobs(insa.getRCArea(), new NAlias(prop.object))).isEmpty()) {
            ArrayList<Gob> availableLogs = new ArrayList<>();
            for (Gob currGob: logs)
            {
                if(PathFinder.isAvailable(currGob))
                    availableLogs.add(currGob);
            }
            if(availableLogs.isEmpty())
                return Results.ERROR("Cant reach any object");

            availableLogs.sort(NUtils.d_comp);
            Gob log = availableLogs.get(0);
            new LiftObject(log).run(gui);
            if(outRoutePoint!=null)
                new RoutePointNavigator((outRoutePoint)).run(gui);
            new FindPlaceAndAction(log, outsa.getRCArea()).run(gui);
            Coord2d shift = log.rc.sub(NUtils.player().rc).norm().mul(2);
            new GoTo(NUtils.player().rc.sub(shift)).run(gui);
            if(inRoutePoint!=null)
                new RoutePointNavigator(inRoutePoint).run(gui);
        }

        return Results.SUCCESS();
    }
}
