package nurgling.actions.bots;

import haven.Gob;
import haven.WItem;
import haven.res.ui.tt.wellmined.WellMined;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.awt.*;
import java.util.*;

public class SmelterAction implements Action {



    static NAlias ores = new NAlias ( new ArrayList<> (
            Arrays.asList ( "cassiterite", "hematite", "peacockore", "chalcopyrite", "malachite", "leadglance",
                    "cinnabar", "galena", "ilmenite", "hornsilver", "argentite", "sylvanite" , "magnetite", "nagyagite", "petzite", "cuprite","limonite") ) );


    @Override
    public Results run(NGameUI gui) throws InterruptedException {


        Context.Updater updater = new Context.Updater(){

            @Override
            public void update(String cap, NInventory inv, Context.Container cont) throws InterruptedException {
                cont.cap = cap;
                cont.freeSpace = inv.getFreeSpace();
                cont.maxSpace = inv.getTotalSpace();
                cont.isFree = cont.freeSpace == cont.maxSpace;
                if(cont instanceof FuelToContainers.FueledContainer)
                {
                    FuelToContainers.FueledContainer smelter = (FuelToContainers.FueledContainer)cont;
                    smelter.fuelTotal = 9;
                    for(WItem item: inv.getItems(ores))
                    {
                        if(((NGItem)item.item).getInfo(WellMined.class)==null)
                            smelter.fuelTotal = 12;
                    }

                    smelter.fuelNeed = Math.max(0,smelter.fuelTotal - (int)(30 * NUtils.getFuelLvl(cont.cap, new Color(255, 128, 0))));
                    if(!cap.equals("Furnace"))
                    {
                        smelter.fuelType = "Coal";
                    }
                }
            }
        };


        NArea smelter = NArea.findSpec(Specialisation.SpecName.smelter.toString());
        Context context = new Context();
        context.setCurrentUpdater(updater);
        ArrayList<Context.Container> smelterCont = new ArrayList<>();

        for(Gob sm : Finder.findGobs(smelter,new NAlias("gfx/terobjs/smelter")))
        {
            smelterCont.add(new FuelToContainers.FueledContainer(sm, new ArrayList<>(Context.contcaps.getall(sm.ngob.name))));
        }
        context.addConstContainers(smelterCont);

        for(Gob sm : Finder.findGobs(smelter,new NAlias("gfx/terobjs/primsmelter")))
        {
            smelterCont.add(new FuelToContainers.FueledContainer(sm, new ArrayList<>(Context.contcaps.getall(sm.ngob.name))));
        }
        HashSet<TreeMap<Integer,String>> transferedItems = new HashSet<TreeMap<Integer,String>>();
        for(Context.Container cont : context.getContainersInWork())
        {
            new PathFinder(cont.gob).run(gui);
            if(cont.cap == null)
            {
                OpenAbstractContainer oac;
                (oac = new OpenAbstractContainer(cont.names, cont, context)).run(gui);
                if(cont.cap == null)
                    return Results.ERROR("INCORRECT CONTAINER");
            }
            else
            {
                new OpenTargetContainer(cont.cap, cont.gob).run(gui);
            }
            context.fillForInventory(gui.getInventory(cont.cap), cont.itemInfo);
//            if(context.output.get("Bar of Copper")== null)
//                context.addOutput("Bar of Copper" , Context.GetOutput("Bar of Tin", NArea.findOut("Bar of Copper")));
//            transferedItems.add("Bar of Copper");
            FreeContainer fc = new FreeContainer(cont, context, false, transferedItems);
            fc.run(gui);
            //new DropFromContainer(cont,new NAlias("slag")).run(gui);
        }
//        FreeContainer.transferAll(context,gui, transferedItems);

        new FillContainer(context.getContainersInWork(), context, NArea.findSpec(Specialisation.SpecName.ore.toString()), ores).run(gui);
        new FuelToContainers(context, context.getContainersInWork()).run(gui);
        ArrayList<Gob> lighted = new ArrayList<>();
        for(Context.Container cont : context.getContainersInWork()) {
            lighted.add(cont.gob);
        }
        new LightGob(lighted, 2).run(gui);
        return Results.SUCCESS();
    }
}
