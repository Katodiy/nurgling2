package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.*;

public class SmelterAction implements Action {



    static NAlias ores = new NAlias ( new ArrayList<> (
            Arrays.asList ( "cassiterite", "hematite", "peacockore", "chalcopyrite", "malachite", "leadglance",
                    "cinnabar", "galena", "ilmenite", "hornsilver", "argentite", "sylvanite" , "magnetite", "nagyagite", "petzite", "cuprite","limonite") ) );


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea smelters = NArea.findSpec(Specialisation.SpecName.smelter.toString());
        Finder.findGobs(smelters,new NAlias("gfx/terobjs/smelter"));

        ArrayList<Container> containers = new ArrayList<>();

        for(Gob sm : Finder.findGobs(smelters,new NAlias("gfx/terobjs/smelter")))
        {
            Container cand = new Container();
            cand.gob = sm;
            cand.cap = ((sm.ngob.getModelAttribute()&128)==128)?"Smith's Smelter":"Ore Smelter";
            containers.add(cand);
        }

        for(Gob sm : Finder.findGobs(smelters,new NAlias("gfx/terobjs/primsmelter")))
        {
            Container cand = new Container();
            cand.gob = sm;
            cand.cap = "Furnace";
            containers.add(cand);
        }
        new FreeContainers(containers).run(gui);


//        Context.Updater updater = new Context.Updater(){
//
//            @Override
//            public void update(String cap, NInventory inv, Context.Container cont) throws InterruptedException {
//                cont.cap = cap;
//                cont.freeSpace = inv.getFreeSpace();
//                cont.maxSpace = inv.getTotalSpace();
//                cont.isFree = cont.freeSpace == cont.maxSpace;
//                if(cont instanceof FuelToContainers.FueledContainer)
//                {
//                    FuelToContainers.FueledContainer smelter = (FuelToContainers.FueledContainer)cont;
//                    smelter.fuelTotal = 9;
//                    for(WItem item: inv.getItems(ores))
//                    {
//                        if(((NGItem)item.item).getInfo(WellMined.class)==null)
//                            smelter.fuelTotal = 12;
//                    }
//
//                    smelter.fuelNeed = Math.max(0,smelter.fuelTotal - (int)(30 * NUtils.getFuelLvl(cont.cap, new Color(255, 128, 0))));
//                    if(!cap.equals("Furnace"))
//                    {
//                        smelter.fuelType = "Coal";
//                    }
//                }
//            }
//        };



    //        FreeContainer.transferAll(context,gui, transferedItems);

//        new FillContainer(context.getContainersInWork(), context, NArea.findSpec(Specialisation.SpecName.ore.toString()), ores).run(gui);
//        new FuelToContainers(context, context.getContainersInWork()).run(gui);
//        ArrayList<Gob> lighted = new ArrayList<>();
//        for(Context.Container cont : context.getContainersInWork()) {
//            lighted.add(cont.gob);
//        }
//        new LightGob(lighted, 2).run(gui);
        return Results.SUCCESS();
    }
}
