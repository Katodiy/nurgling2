package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NMakewindow;
import nurgling.widgets.Specialisation;

/**
 * Action for lighting fire using either a candelabrum or branches (alternative method).
 * 
 * Alternative method algorithm:
 * 1. Find fuel area with branches (stockpile)
 * 2. Take 2 branches
 * 3. Go to the object to light
 * 4. Drink to restore stamina
 * 5. Craft fire (menu: 'a' -> 'f' -> 'g' -> "Light fire")
 * 6. Wait for firebrand (splinter) to appear in hand
 * 7. Use firebrand on the object to light
 * 8. If firebrand extinguishes before lighting - repeat from step 2
 * 9. After lighting, drop item from hand
 */
public class LightFire implements Action {
    
    private final Gob firedGob;

    // Maximum attempts to light fire with branches before failing
    private static final int MAX_ATTEMPTS = 5;

    NMakewindow oldcraft = null;
    
    /**
     * Constructor for fire lighting with branches (alternative method)
     * @param firedGob the object to light fire on
     */
    public LightFire(Gob firedGob) {
        this.firedGob = firedGob;
    }
    

    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if(NUtils.getGameUI().craftwnd.makeWidget!=null)
        {
            oldcraft = NUtils.getGameUI().craftwnd.makeWidget;
        }
        Results res = lightWithBranches(gui);
        if(oldcraft!=null)
        {
            NUtils.getGameUI().craftwnd.makeWidget = oldcraft;
            NUtils.getGameUI().craftwnd.tabStrip.select(NUtils.getGameUI().craftwnd.tabs.get(NUtils.getGameUI().craftwnd.makeWidget.rcpnm));
        }
        return res;

    }
    
    /**
     * Light fire using branches (alternative method)
     */
    private Results lightWithBranches(NGameUI gui) throws InterruptedException {
        // Find fuel area with branches
        NArea branchArea = NContext.findSpec(Specialisation.SpecName.fuel.toString(),"Branch");

        if (branchArea == null) {
            gui.error("Cannot find area with branches for fire lighting");
            return Results.ERROR("No branch area found");
        }
        
        // Store initial state of the object to light
        long initialState = firedGob.ngob.getModelAttribute();
        
        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            attempts++;

            if(NUtils.getGameUI().getInventory().getItems("Branch").size()<2)
            {
                // Find stockpile with branches in the area
                Gob branchPile = Finder.findGob(branchArea, new NAlias("stockpile-branch", "stockpile"));
                if (branchPile == null)
                {
                    // Try to find any stockpile in the fuel area
                    for (Gob pile : Finder.findGobs(branchArea, new NAlias("stockpile")))
                    {
                        // Check if stockpile contains branches (by overlay or content)
                        branchPile = pile;
                        break;
                    }
                }

                if (branchPile == null)
                {
                    gui.error("Cannot find stockpile with branches");
                    return Results.ERROR("No branch stockpile found");
                }

                // Go to the branch pile
                PathFinder pf = new PathFinder(branchPile);
                pf.isHardMode = true;
                pf.run(gui);

                // Open stockpile and take 2 branches
                new OpenTargetContainer("Stockpile", branchPile).run(gui);

                NISBox stockpile = gui.getStockpile();
                if (stockpile == null)
                {
                    gui.error("Failed to open stockpile");
                    return Results.ERROR("Stockpile open failed");
                }

                // Take 2 branches
                int branchesToTake = Math.min(2, stockpile.calcCount());
                if (branchesToTake < 2)
                {
                    gui.error("Not enough branches in stockpile");
                    return Results.ERROR("Not enough branches");
                }

                TakeItemsFromPile takeAction = new TakeItemsFromPile(branchPile, stockpile, 2);
                takeAction.run(gui);
            }

            // Go to the object to light
            PathFinder pfToTarget = new PathFinder(firedGob);
            pfToTarget.run(gui);
            
            // Drink to restore stamina
            Results drinkResult = new Drink(0.9, false).run(gui);
            if (!drinkResult.IsSuccess()) {
                gui.error("Failed to drink");
                return Results.ERROR("Drink failed");
            }
            
            craftLightFire(gui);

            // Check if firebrand is still burning (has the right name)
            if (gui.vhand == null) {
                continue; // Firebrand extinguished, try again
            }
            
            // Use firebrand on the object to light
            NUtils.activateItem(firedGob);

            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {

                    return (gui.prog != null) && (gui.prog.prog > 0) ;
                }
            });

            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {

                    return (gui.prog == null) || (gui.prog.prog <= 0) ;
                }
            });
            
            // Drop item from hand if present
            if (gui.vhand != null) {
                NUtils.drop(gui.vhand);
                NUtils.addTask(new WaitFreeHand());
            }
            

            // Check if fire was lit (state changed)
            Gob updatedGob = Finder.findGob(firedGob.id);
            if (updatedGob != null && updatedGob.ngob.getModelAttribute() != initialState) {
                return Results.SUCCESS();
            }
        }
        
        // Failed after max attempts
        return Results.ERROR("Failed to light fire after " + MAX_ATTEMPTS + " attempts");
    }

    private void craftLightFire(NGameUI gui) throws InterruptedException {
        NUtils.getGameUI().ui.rcvr.rcvmsg(NUtils.getUI().getMenuGridId(), "act", "lightfire");

        if(NUtils.getGameUI().craftwnd.makeWidget!=null && !NUtils.getGameUI().craftwnd.makeWidget.rcpnm.equals("Light fire"))
        {
            for (MenuGrid.Pagina pb : NUtils.getGameUI().menu.paginae)
            {
                if (pb.button().name().equals("Light fire"))
                {
                    pb.button().use(new MenuGrid.Interaction());
                    break;
                }
            }
            NUtils.addTask(new NTask()
            {
                @Override
                public boolean check()
                {
                    return NUtils.getGameUI().craftwnd != null && NUtils.getGameUI().craftwnd.makeWidget!=null &&  NUtils.getGameUI().craftwnd.makeWidget.rcpnm.equals("Light fire");
                }
            });
        }
        NUtils.getGameUI().craftwnd.makeWidget.wdgmsg("make", 1);

        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {

                return (gui.prog != null) && (gui.prog.prog > 0) ;
            }
        });

        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {

                return (gui.prog == null) || (gui.prog.prog <= 0) ;
            }
        });
    }

}

