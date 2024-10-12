package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.pf.CellsArray;
import nurgling.pf.NPFMap;
import nurgling.pf.Utils;
import nurgling.tools.Finder;

import static nurgling.tools.Finder.findLiftedbyPlayer;

public class FindPlaceAndAction implements Action {
    public FindPlaceAndAction(Gob gob, Pair<Coord2d, Coord2d> rcArea) {
        this.placed = gob;
        this.area = rcArea;
    }

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        if(placed == null)
            placed = findLiftedbyPlayer();
        if ( placed != null ) {
            Coord2d pos = Finder.getFreePlace(area, placed);
            if(pos!=null) {
                new PlaceObject(placed, pos,0).run(gui);
                return Results.SUCCESS();
            }
            else
                return Results.ERROR("No free place");

        }
        return Results.ERROR("No gob for place");
    }



    public FindPlaceAndAction(
            Gob gob,
            NArea area)
    {
        this.placed = gob;
        this.area = area.getRCArea();
    }

    public Gob getPlaced() {
        return placed;
    }

    Gob placed = null;
    Pair<Coord2d, Coord2d> area = null;
}