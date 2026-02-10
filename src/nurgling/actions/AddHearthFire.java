package nurgling.actions;

import haven.GAttrib;
import haven.Gob;
import haven.Utils;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.List;

public class AddHearthFire implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
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


        return Results.SUCCESS();
    }
}
