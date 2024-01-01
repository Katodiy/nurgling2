package nurgling.actions;

import haven.*;
import nurgling.*;

public class LiftObject implements Action {
    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        if ( gob != null ) {
            new PathFinder ( gob ).run(gui);
            NUtils.lift (gob);
            return Results.SUCCESS();
        }
        return Results.ERROR("No gob for Lift");
    }

    public LiftObject (
            Gob gob

    ) {
        this.gob = gob;
    }

    Gob gob = null;
}