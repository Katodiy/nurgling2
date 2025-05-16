package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;
import nurgling.NCore;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

public class WaitDistance extends NTask {
    Coord2d last;
    double dist;
    Route route;
    Gob oldPlayer;

    public WaitDistance(Coord2d last, double dist, Route route) {
        this.last = last;
        this.dist = dist;
        this.route = route;
        this.oldPlayer = NUtils.player();
    }

    @Override
    public boolean check() {
        NCore.LastActions lastAction = null;
        if(NUtils.getUI() != null && NUtils.getUI().core.getLastActions() != null) {
            lastAction = NUtils.getUI().core.getLastActions();
        }

        Gob player = NUtils.player();

        if (player != this.oldPlayer)
            return true;

        this.oldPlayer = player;

        if (lastAction != null) {
            route.lastAction = lastAction;
        }

        if(last == null) {
            return false;
        }

        try {
            if(veryCloseToAGate()) {
                return true;
            }

        } catch (InterruptedException e) {
            System.out.println("WaitDistance interrupted");
        }

        return player.rc.dist(last) >= dist;
    }

    private boolean veryCloseToAGate() throws InterruptedException {
        String[] gateNames = {"gfx/terobjs/arch/polebiggate", "gfx/terobjs/arch/drystonewallbiggate", "gfx/terobjs/arch/polegate", "gfx/terobjs/arch/drystonewallgate"};
        Gob gate = Finder.findGob(this.oldPlayer.rc, new NAlias(gateNames), null, 10);

        if(gate != null) {
            return true;
        }
        return false;
    }
}
