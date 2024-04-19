package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;

import static haven.OCache.posres;

public class SnekkjaSail implements Action {


    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        Gob snek = Finder.findGob(new NAlias("snekkja"));
        if(gui.hand.isEmpty()){
//            PathFinder pathFinder = new PathFinder(boat);
//            pathFinder.isHardMode = true;
//            pathFinder.run(gui);
            NUtils.rclickGob(snek);
            new SelectFlowerAction("Man the helm", snek).run(gui);
            NUtils.getUI().core.addTask(new FollowAndPose(NUtils.player(),"gfx/borka/snekkjaman0"));
            NUtils.getUI().core.addTask(new IsVesselMoving(snek));
            NUtils.getUI().core.addTask(new IsVesselNotMoving(snek));
            NUtils.getUI().msg("snek is moving");
        }


        return Results.SUCCESS();
    }
}