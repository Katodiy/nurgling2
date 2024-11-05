package nurgling.actions;

import haven.Gob;

import haven.Coord;
import haven.Gob;
import haven.Skeleton;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitPose;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import static haven.OCache.posres;

public class TakeVehicle implements Action {
    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        if(vehicle==null)
            vehicle = Finder.findGob ( name );

        if(!NUtils.player().pose().contains("carry")) {
            NUtils.addTask(new WaitPose(NUtils.player(), "idle"));
        }
        if(!NUtils.player().pose().contains("carry")) {
            NUtils.rclickGob(vehicle);
            NUtils.addTask(new WaitPose(NUtils.player(), "carry"));
        }

        return Results.SUCCESS();
    }

    public TakeVehicle(NAlias name ) {
        this.name = name;
    }

    public TakeVehicle(Gob vehicle ) {
        this.vehicle = vehicle;
    }

    NAlias name;
    Gob vehicle = null;
}
