package nurgling.actions.test;

import haven.Coord2d;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.pf.NPFMap;
import nurgling.tasks.ChangeModelAtrib;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

/*
* Lift/Place test
* */

public class TESTLiftDrop extends Test
{

    public TESTLiftDrop()
    {
        this.num = 100;
    }

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        long start =System.currentTimeMillis();
        NPFMap pfmap = new NPFMap(NUtils.player().rc.sub(new Coord2d(100,100)), NUtils.player().rc.add(new Coord2d(100,100)), 1);
        pfmap.build();
        NUtils.getGameUI().msg("Build time in ms:" + String.valueOf(System.currentTimeMillis() - start));
        NPFMap.print(pfmap.getSize(), pfmap.getCells());
//        new AutoEater().run(gui);
//        NPFMap npf = new NPFMap(NUtils.player().rc, Finder.findGob(new NAlias("gfx/terobjs/trough")).rc, 2);
//        npf.build();
//        npf.print(npf.getSize(), npf.getCells());
//        new PathFinder(Finder.findGob(new NAlias("gfx/terobjs/trough"))).run(gui);
//        Gob trough = Finder.findGob(new NAlias("gfx/terobjs/trough"));
//        Coord2d pos = trough.rc;
//        double a = trough.a;
//        Gob cistern  = Finder.findGob(new NAlias("gfx/terobjs/cistern"));
//        new LiftObject(trough).run(gui);
//        new PathFinder ( cistern ).run(gui);
//        NUtils.activateGob ( cistern );
//        NUtils.getUI().core.addTask(new ChangeModelAtrib(trough, 7));
//        new FindPlaceAndAction(trough, NContext.findSpec("trough")).run(gui);
//        new LiftObject(trough).run(gui);
//        new PathFinder ( cistern ).run(gui);
//        NUtils.activateGob ( cistern );
//        NUtils.getUI().core.addTask(new ChangeModelAtrib(trough, 0));
//        new FindPlaceAndAction(trough, NContext.findSpec("trough")).run(gui);
    }
}
