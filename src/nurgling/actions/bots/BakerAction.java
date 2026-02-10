package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.VSpec;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class BakerAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        NAlias doughs = VSpec.getNamesInCategory("Dough");
        NContext context = new NContext(gui);


        // Get ovens area through context - this will handle navigation automatically
        NArea ovens = context.getSpecArea(Specialisation.SpecName.ovens);

        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(ovens, new NAlias("gfx/terobjs/oven")))
        {
            Container cand = new Container(sm, "Oven", ovens);

            cand.initattr(Container.Space.class);
            cand.initattr(Container.FuelLvl.class);
            cand.getattr(Container.FuelLvl.class).setMaxlvl(4);
            cand.getattr(Container.FuelLvl.class).setFueltype("branch");

            containers.add(cand);
        }

        ArrayList<String> lighted = new ArrayList<>();
        for (Container cont : containers)
        {
            lighted.add(cont.gobHash);
        }

            Results res = null;
            while (res == null || res.IsSuccess()) {
                NUtils.getUI().core.addTask(new WaitForBurnout(lighted, 4));
                new FreeContainers(containers).run(gui);
                
                // Check if ovens are not empty after freeing
                for(Container oven : containers) {
                    oven.update();
                    Container.Space space = oven.getattr(Container.Space.class);
                    Integer freeSpace = (Integer)space.getRes().get(Container.Space.FREESPACE);
                    if(freeSpace != null && freeSpace != 8) {
                        return Results.ERROR("Cannot unload pies from ovens");
                    }
                }
                
                // Fill containers with ANY available dough type
                res = fillContainersWithAnyDough(gui, context, containers, doughs, ovens);
                
                if(!res.IsSuccess()) {
                    return res;
                }

            if (!res.IsSuccess())
            {
                return res;
            }

            ArrayList<Container> forFuel = new ArrayList<>();
            for (Container container : containers)
            {
                Container.Space space = container.getattr(Container.Space.class);
                if (!space.isEmpty())
                    forFuel.add(container);
            }
            new FuelToContainers(forFuel).run(gui);

            ArrayList<String> flighted = new ArrayList<>();
            for (Container cont : forFuel)
            {
                flighted.add(cont.gobHash);
            }
            new LightGob(flighted, 4).run(gui);
        }
        return Results.FAIL();
    }
    
    private Results fillContainersWithAnyDough(NGameUI gui, NContext context, ArrayList<Container> containers, NAlias doughs, NArea ovensArea) throws InterruptedException {
        // Find area with any dough type
        NArea doughArea = null;
        
        for(String doughName : doughs.keys) {
            NArea area = NContext.findInGlobal(doughName);
            if(area != null) {
                doughArea = area;
                context.addInItem(doughName, null);
                break;
            }
        }
        
        if(doughArea == null) {
            return Results.ERROR("NO DOUGH AVAILABLE IN ANY AREA");
        }
        
        // Get storages from the area  
        ArrayList<NContext.ObjectStorage> storages = context.getInStorages(doughs.keys.get(0));
        if(storages == null || storages.isEmpty()) {
            return Results.ERROR("NO STORAGE WITH DOUGH");
        }
        
        // Keep taking and filling until ovens are full or no more dough
        while(!allOvensFilled(containers)) {
            // Calculate how much we can take
            int freeSpaceInOvens = calculateFreeSpace(containers);
            int freeSpaceInInventory = gui.getInventory().getNumberFreeCoord(new haven.Coord(1, 1));
            int toTake = Math.min(freeSpaceInOvens, freeSpaceInInventory);
            
            if(toTake == 0) {
                return Results.ERROR("NO SPACE IN INVENTORY OR OVENS");
            }
            
            // Take items from containers with ANY dough type
            boolean tookSomething = false;
            for(NContext.ObjectStorage storage : storages) {
                if(storage instanceof Container && toTake > 0) {
                    Container cont = (Container) storage;
                    new PathFinder(Finder.findGob(cont.gobHash)).run(gui);
                    new OpenTargetContainer(cont).run(gui);
                    
                    int beforeCount = gui.getInventory().getItems(doughs).size();
                    
                    // Try to take ANY dough type from this container
                    java.util.HashSet<String> doughSet = new java.util.HashSet<>(doughs.keys);
                    TakeItemsFromContainer tifc = new TakeItemsFromContainer(cont, doughSet, null);
                    tifc.minSize = toTake;
                    tifc.run(gui);
                    
                    new CloseTargetContainer(cont).run(gui);
                    
                    int afterCount = gui.getInventory().getItems(doughs).size();
                    int taken = afterCount - beforeCount;
                    
                    if(taken > 0) {
                        tookSomething = true;
                        toTake -= taken;
                        
                        // Navigate to ovens area using context
                        context.navigateToAreaIfNeeded(Specialisation.SpecName.ovens.toString());
                        
                        // Transfer to ovens immediately after taking
                        for(Container oven : containers) {
                            if(!isReady(oven) && gui.getInventory().getItems(doughs).size() > 0) {
                                new PathFinder(Finder.findGob(oven.gobHash)).run(gui);
                                TransferToContainer ttc = new TransferToContainer(oven, doughs);
                                ttc.run(gui);
                                new CloseTargetContainer(oven).run(gui);
                            }
                        }
                        
                        // Check if ovens are now full
                        if(allOvensFilled(containers)) {
                            return Results.SUCCESS();
                        }
                    }
                    
                    // If this container is empty, move to next
                    if(!tifc.getResult()) {
                        continue;
                    }
                }
            }
            
            // If we couldn't take anything, we're done
            if(!tookSomething) {
                return Results.ERROR("NO MORE DOUGH AVAILABLE");
            }
        }
        
        return Results.SUCCESS();
    }
    
    private int calculateFreeSpace(ArrayList<Container> containers) {
        int total = 0;
        for(Container cont : containers) {
            Container.Space space = cont.getattr(Container.Space.class);
            if(space != null && space.getRes().get(Container.Space.FREESPACE) != null) {
                total += (Integer)space.getRes().get(Container.Space.FREESPACE);
            }
        }
        return total;
    }
    
    private boolean isReady(Container container) {
        Container.Space space = container.getattr(Container.Space.class);
        Integer freeSpace = (Integer)space.getRes().get(Container.Space.FREESPACE);
        return freeSpace != null && freeSpace == 0;
    }
    
    private boolean allOvensFilled(ArrayList<Container> containers) {
        for(Container oven : containers) {
            if(!isReady(oven)) {
                return false;
            }
        }
        return true;
    }
}

