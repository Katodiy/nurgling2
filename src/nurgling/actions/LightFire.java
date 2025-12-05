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

import java.util.HashMap;
import java.util.Map;

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

    // Saved craft window state
    private CraftState savedState = null;
    
    /**
     * Class to hold the full state of a craft window
     */
    private static class CraftState {
        MenuGrid.Pagina pagina;
        String recipeName;
        boolean autoMode;
        boolean noTransfer;
        // Map of spec index to ingredient info (name, isIgnored)
        Map<Integer, IngredientInfo> inputIngredients = new HashMap<>();
        Map<Integer, IngredientInfo> outputIngredients = new HashMap<>();
        
        static class IngredientInfo {
            String name;
            boolean isIgnored;
            java.awt.image.BufferedImage img;
            
            IngredientInfo(String name, boolean isIgnored, java.awt.image.BufferedImage img) {
                this.name = name;
                this.isIgnored = isIgnored;
                this.img = img;
            }
        }
    }
    
    /**
     * Constructor for fire lighting with branches (alternative method)
     * @param firedGob the object to light fire on
     */
    public LightFire(Gob firedGob) {
        this.firedGob = firedGob;
    }
    

    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Save current craft state before light fire
        saveCraftState(gui);
        
        Results res = lightWithBranches(gui);
        
        // Restore previous craft state after light fire
        restoreCraftState(gui);
        
        return res;
    }
    
    /**
     * Save the current craft window state (pagina, autoMode, ingredients, etc)
     */
    private void saveCraftState(NGameUI gui) {
        savedState = null;
        
        if (gui.craftwnd == null || gui.craftwnd.makeWidget == null) {
            return;
        }
        
        NMakewindow mw = gui.craftwnd.makeWidget;
        savedState = new CraftState();
        savedState.recipeName = mw.rcpnm;
        savedState.autoMode = mw.autoMode;
        savedState.noTransfer = mw.noTransfer != null && mw.noTransfer.a;
        
        // Save selected ingredients for inputs
        for (int i = 0; i < mw.inputs.size(); i++) {
            NMakewindow.Spec spec = mw.inputs.get(i);
            if (spec.ing != null) {
                savedState.inputIngredients.put(i, new CraftState.IngredientInfo(
                    spec.ing.name, spec.ing.isIgnored, spec.ing.img
                ));
            }
        }
        
        // Save selected ingredients for outputs
        for (int i = 0; i < mw.outputs.size(); i++) {
            NMakewindow.Spec spec = mw.outputs.get(i);
            if (spec.ing != null) {
                savedState.outputIngredients.put(i, new CraftState.IngredientInfo(
                    spec.ing.name, spec.ing.isIgnored, spec.ing.img
                ));
            }
        }
        
        // Find the Pagina for current recipe in paginae
        for (MenuGrid.Pagina pag : gui.menu.paginae) {
            try {
                if (pag.button() != null && pag.button().name().equals(savedState.recipeName)) {
                    savedState.pagina = pag;
                    break;
                }
            } catch (Loading l) {
                // Skip if not loaded
            }
        }
    }
    
    /**
     * Restore the previously saved craft window state
     */
    private void restoreCraftState(NGameUI gui) throws InterruptedException {
        if (savedState == null || savedState.pagina == null) {
            return;
        }
        
        // Re-open the saved recipe by using its pagina
        savedState.pagina.button().use(new MenuGrid.Interaction(1, 0));
        
        // Wait for the craft window to show the correct recipe
        final String targetRecipe = savedState.recipeName;
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return gui.craftwnd != null && 
                       gui.craftwnd.makeWidget != null && 
                       gui.craftwnd.makeWidget.rcpnm.equals(targetRecipe);
            }
        });
        
        // Now restore the state to the new widget
        NMakewindow newMw = gui.craftwnd.makeWidget;
        if (newMw == null) {
            return;
        }
        
        // Restore autoMode
        newMw.autoMode = savedState.autoMode;
        
        // Restore noTransfer
        if (newMw.noTransfer != null) {
            newMw.noTransfer.a = savedState.noTransfer;
            newMw.noTransfer.visible = savedState.autoMode;
        }
        
        // Restore input ingredients
        for (Map.Entry<Integer, CraftState.IngredientInfo> entry : savedState.inputIngredients.entrySet()) {
            int idx = entry.getKey();
            CraftState.IngredientInfo info = entry.getValue();
            if (idx < newMw.inputs.size()) {
                NMakewindow.Spec spec = newMw.inputs.get(idx);
                spec.ing = newMw.new Ingredient(info.img, info.name, info.isIgnored);
            }
        }
        
        // Restore output ingredients
        for (Map.Entry<Integer, CraftState.IngredientInfo> entry : savedState.outputIngredients.entrySet()) {
            int idx = entry.getKey();
            CraftState.IngredientInfo info = entry.getValue();
            if (idx < newMw.outputs.size()) {
                NMakewindow.Spec spec = newMw.outputs.get(idx);
                spec.ing = newMw.new Ingredient(info.img, info.name, info.isIgnored);
            }
        }
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

