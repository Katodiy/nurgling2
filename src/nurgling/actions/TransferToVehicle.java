package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.NTask;
import nurgling.tools.Finder;

import static nurgling.tools.Finder.findLiftedbyPlayer;

public class TransferToVehicle implements Action {
    private final Gob vehicle;

    public TransferToVehicle(Gob gob, Gob vehicle) {
        this.placed = gob;
        this.vehicle = vehicle;
    }

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        if(placed == null)
            placed = findLiftedbyPlayer();
        if ( placed != null ) {
            new PathFinder(vehicle).run(gui);
            NUtils.rclickGob(vehicle);
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return Finder.findGob(placed.id)==null;
                }
            });
            return Results.SUCCESS();
        }
        return Results.ERROR("No gob for place");
    }

    Gob placed = null;
}