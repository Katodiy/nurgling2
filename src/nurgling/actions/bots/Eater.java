package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.conf.NWorldExplorerProp;
import nurgling.routes.RoutePoint;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.WaitCheckable;
import nurgling.tasks.WaitItemContent;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.FoodContainer;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.List;

import static haven.Coord.of;

public class Eater implements Action {

    boolean oz = false;
    List<RoutePoint> routePoints = null;

    public Eater(boolean oz) {
        this.oz = oz;
    }

    public Eater() {
        this.oz = false;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<String> items = FoodContainer.getFoodNames();

        Pair<Coord2d,Coord2d> area = null;
        NArea nArea = NArea.findSpec(Specialisation.SpecName.eat.toString());
        if(nArea==null)
        {
            nArea = NArea.globalFindSpec(Specialisation.SpecName.eat.toString());
            if(nArea!=null) {
                routePoints = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(nArea));
            }
        }
        else
        {
            area = nArea.getRCArea();
        }
        if(routePoints == null) {
            if (area == null && !oz) {
                SelectArea insa;
                NUtils.getGameUI().msg("Please select a food area");
                (insa = new SelectArea(Resource.loadsimg("baubles/waterRefiller"))).run(gui);
                area = insa.getRCArea();
            }
        }
        if(routePoints!=null)
        {
            new RoutePointNavigator(routePoints.getLast()).run(NUtils.getGameUI());
            area = nArea.getRCArea();
        }
        if(area!=null) {
            Context cnt = new Context();
            new FindAndEatItems(cnt, items, 8000, area).run(gui);
            return NUtils.getEnergy()*10000>8000?Results.SUCCESS():Results.FAIL();
        }
        else
            return Results.FAIL();
    }
}
