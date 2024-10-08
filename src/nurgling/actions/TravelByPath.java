package nurgling.actions;

import haven.Coord2d;
import nurgling.NGameUI;

import java.util.ArrayList;

public class TravelByPath implements Action {

    final ArrayList<Coord2d> points;
    final Action actionInMoving;

    public TravelByPath(ArrayList<Coord2d> points, Action actionInMoving) {
        this.points = points;
        this.actionInMoving = actionInMoving;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (!points.isEmpty())
        {
            Coord2d c = points.remove(0);
            new PathFinder(c).run(gui);
            actionInMoving.run(gui);
        }
        return Results.SUCCESS();
    }
}
