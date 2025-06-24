package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NGob;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.*;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

public class WaterToContainer implements Action
{
    ArrayList<Container> conts;

    public WaterToContainer(ArrayList<Container> conts) {
        this.conts = conts;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        HashMap<String, Integer> neededWater = new HashMap<>();
        for (Container cont : conts) {
            Container.WaterLvl watLvl = cont.getattr(Container.WaterLvl.class);

            neededWater.put(Container.WaterLvl.WATERLVL, watLvl.neededWater());
        }

        Gob cistern = Finder.findGob(NContext.findSpec(Specialisation.SpecName.water.toString()), new NAlias("cistern"));
        Gob barrel = Finder.findGob(NContext.findSpec(Specialisation.SpecName.water.toString()), new NAlias("barrel"));
        if(cistern == null || barrel == null)
            return Results.ERROR("No barrel or cistern.");
        Coord2d pos = new Coord2d(barrel.rc.x, barrel.rc.y);
        new LiftObject(barrel).run(gui);
        NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/banzai"));

        if(!NUtils.isOverlay(barrel, new NAlias("water"))){
            new PathFinder(cistern).run(gui);
            NUtils.activateGob(cistern);
            IsOverlay task = new IsOverlay(barrel, new NAlias("water"));
            NUtils.getUI().core.addTask(task);
            if(!task.getResult())
                return Results.ERROR("No water in cistern!");
        }
        for (Container cont : conts) {
            Container.WaterLvl waterLvl = cont.getattr(Container.WaterLvl.class);
            if(waterLvl.neededWater() >=2 ){// <29.1
                new PathFinder(cont.gob).run(gui);
                NUtils.activateGob(cont.gob);
                NUtils.getUI().core.addTask(new WaitGobModelAttr(cont.gob,4));
                //TODO: change task from modelAttr to sound
            }

            if(!NUtils.isOverlay(barrel, new NAlias("water"))){
                new PathFinder(cistern).run(gui);
                NUtils.activateGob(cistern);
                IsOverlay task = new IsOverlay(barrel, new NAlias("water"));
                NUtils.getUI().core.addTask(task);
                if(!task.getResult())
                    return Results.ERROR("No water in cistern!");

                new PathFinder(cont.gob).run(gui);
                NUtils.activateGob(cistern);

                if((cont.gob.ngob.getModelAttribute() & 4) == 0){
                    gui.msg("You clicked with empty barreld");
                    NUtils.activateGob(cont.gob);
                }
            }
        }
        if ( barrel != null ) {
            PathFinder pf = new PathFinder ( NGob.getDummy(pos, 0, barrel.ngob.hitBox) , true);
            pf.isHardMode = true;
            pf.run(gui);
            NUtils.place(barrel,pos,0);
            return Results.SUCCESS();
        }
        return Results.ERROR("No place for barrel!");
    }
}