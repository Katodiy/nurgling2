package nurgling.actions.bots;

import haven.GameUI;
import haven.Gob;
import haven.WItem;
import haven.Window;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class SmelterAction implements Action {

    public static class Smelter extends Context.Container
    {
        public Smelter(Gob gob, ArrayList<String> names) {
            super(gob, names);
        }

        int FuelTotal;
        int FuelNeed;
    }

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
                if(cont instanceof Smelter)
                {
                    Smelter smelter = (Smelter)cont;
                    smelter.FuelTotal = 9;
                    for(WItem item: inv.getItems(ores))
                    {
                        if(((NGItem)item.item).getInfo(WellMined.class)==null)
                            smelter.FuelTotal = 12;
                    }

                    smelter.FuelNeed = Math.max(0,smelter.FuelTotal - (int)(30 * NUtils.getFuelLvl(cont.cap, new Color(255, 128, 0))));
                }
            }
        };


        NArea smelter = NArea.findSpec(Specialisation.SpecName.smelter.toString());
        Context context = new Context();
        context.setCurrentUpdater(updater);
        ArrayList<Context.Container> smelterCont = new ArrayList<>();

        for(Gob sm : Finder.findGobs(smelter,new NAlias("gfx/terobjs/smelter")))
        {
            smelterCont.add(new Smelter(sm, new ArrayList<>(Context.contcaps.getall(sm.ngob.name))));
        }
        context.addConstContainers(smelterCont);

        for(Gob sm : Finder.findGobs(smelter,new NAlias("gfx/terobjs/primsmelter")))
        {
            smelterCont.add(new Smelter(sm, new ArrayList<>(Context.contcaps.getall(sm.ngob.name))));
        }
        HashSet<String> transferedItems = new HashSet<>();
        for(Context.Container cont : context.getContainersInWork())
        {
            Context.Updater upd = new Context.Updater() {
                @Override
                public void update(String cap, NInventory inv, Context.Container cont) throws InterruptedException {
                    cont.cap = cap;
                    cont.freeSpace = inv.getFreeSpace();
                    cont.maxSpace = inv.getTotalSpace();
                    cont.isFree = cont.freeSpace == cont.maxSpace;
                }
            };

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
            FreeContainer fc = new FreeContainer(cont, context, false, transferedItems);
            fc.run(gui);
        }
        FreeContainer.transferAll(context,gui, transferedItems);

        new FillContainer(context.getContainersInWork(), context, NArea.findSpec(Specialisation.SpecName.ore.toString()), ores).run(gui);


        return Results.SUCCESS();
    }


}
