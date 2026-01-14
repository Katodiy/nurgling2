package nurgling.actions;

import haven.*;
import nurgling.GhostAlpha;
import nurgling.NGameUI;
import nurgling.NGob;
import nurgling.NISBox;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.ConstructionMaterialsRegistry;
import nurgling.overlays.BuildGhostPreview;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

import static haven.OCache.posres;

public class Build implements Action
{
    Command cmd;
    NArea buildArea;  // NArea for global navigation support
    int rotationCount = 0;  // Rotation count: 0, 1, 2, 3 for 0°, 90°, 180°, 270°
    ArrayList<Coord2d> ghostPositions = null;  // Optional ghost positions from preview
    BuildGhostPreview ghostPreview = null;  // Optional reference to ghost preview for removal

    NContext context;
    public static class Command
    {
        public String name;
        public String windowName = null; // Window name if different from menu name
        public nurgling.NHitBox customHitBox = null;

        public ArrayList<Ingredient> ingredients = new ArrayList<Ingredient>();
    }

    public static class Ingredient
    {

        public Coord coord;
        public NArea nArea;  // NArea for global navigation support
        public NAlias name;
        public int count;
        public int left = 0;
        public Action specialWay = null;
        public ArrayList<Container> containers = new ArrayList<>();

        public Ingredient(Coord coord, NArea area, NAlias name, int count)
        {
            this.coord = coord;
            this.nArea = area;
            this.name = name;
            this.count = count;
        }

        public Ingredient(Coord coord, NArea area, NAlias name, int count, Action specialWay)
        {
            this.coord = coord;
            this.nArea = area;
            this.name = name;
            this.count = count;
            this.specialWay = specialWay;
        }

        /**
         * Resolve the area from construction materials zones if useAutoZone is true.
         * This should be called before trying to use the ingredient.
         * @param context The NContext for area lookup and navigation
         * @return true if area was resolved (or was already set), false if no zone found
         */
        public boolean resolveAutoZone(NContext context) throws InterruptedException {
            if (nArea != null) {
                return true; // Already has an area or doesn't need auto-lookup
            }
            
            NArea materialArea = context.getBuildMaterialArea(name);
            if (materialArea != null) {
                this.nArea = materialArea;
                return true;
            }
            return false;
        }

        /**
         * Navigate to this ingredient's area and get the RC area.
         * Uses global pathfinding if the area is not visible.
         * @param context NContext for navigation
         * @return The RC area pair, or null if navigation failed
         */
        public Pair<Coord2d, Coord2d> navigateAndGetArea(NContext context) throws InterruptedException {
            if (nArea != null) {
                // Navigate to the area if needed
                NUtils.navigateToArea(nArea);
                // Now get the RC area
                return nArea.getRCArea();
            }
            return null;
        }
    }

    public Build(NContext context,Command cmd, NArea area)
    {
        this(context, cmd, area, 0);
    }

    public Build(NContext context,Command cmd, NArea area, int rotationCount)
    {
        this.context = context;
        this.cmd = cmd;
        this.buildArea = area;
        this.rotationCount = rotationCount;
    }

    public Build(NContext context, Command cmd, NArea area, int rotationCount, ArrayList<Coord2d> ghostPositions, BuildGhostPreview ghostPreview)
    {
        this.context = context;
        this.cmd = cmd;
        this.buildArea = area;
        this.rotationCount = rotationCount;
        this.ghostPositions = ghostPositions;
        this.ghostPreview = ghostPreview;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        try {
            return runBuild(gui);
        } finally {
            // ALWAYS clean up ghost preview on exit (normal or exception)
            cleanupGhosts();
        }
    }
    
    /**
     * Clean up all ghost previews - called on any exit from Build
     */
    private void cleanupGhosts() {
        if (ghostPreview != null) {
            try {
                ghostPreview.dispose();
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
        }
        // Also try to remove from player just in case
        try {
            Gob player = NUtils.player();
            if (player != null) {
                player.delattr(nurgling.overlays.BuildGhostPreview.class);
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private Results runBuild(NGameUI gui) throws InterruptedException
    {
        // Create context for navigation and zone resolution
        Pair<Coord2d,Coord2d> area = null;
        // Navigate to build area if using NArea and resolve the RC area
        if (buildArea != null) {
            NUtils.navigateToArea(buildArea);
            area = buildArea.getRCArea();
            if (area == null) {
                return Results.ERROR("Cannot get build area coordinates");
            }
        }

        // Resolve auto-zones for ingredients that need it
        for (Ingredient ingredient : cmd.ingredients)
        {
            if (ingredient.nArea == null)
            {
                if (!ingredient.resolveAutoZone(context))
                {
                    // Zone not found, log warning but continue (bot may ask user later)
                    NUtils.getGameUI().msg("Warning: No construction materials area found for " + ingredient.name.getKeys().get(0));
                }
            }
        }

        // Containers will be populated when we navigate to ingredient areas during refill

        // First, check for unfinished constructions (consobj) in the area and add them to ghost positions
        if (ghostPositions == null)
        {
            ghostPositions = new ArrayList<>();
        }

        ArrayList<Gob> consobjs = Finder.findGobs(area, new NAlias("gfx/terobjs/consobj"));
        if (!consobjs.isEmpty())
        {
            // Add consobj positions to the beginning of ghost positions so they get finished first
            ArrayList<Coord2d> consobjPositions = new ArrayList<>();
            for (Gob consobj : consobjs)
            {
                consobjPositions.add(consobj.rc);
            }
            consobjPositions.addAll(ghostPositions);
            ghostPositions = consobjPositions;
        }

        int ghostIndex = 0;
        Coord2d pos = Coord2d.z;
        do
        {
            // First, determine position (may need plob for finding free place)
            if (ghostPositions != null && !ghostPositions.isEmpty() && ghostIndex < ghostPositions.size())
            {
                pos = ghostPositions.get(ghostIndex);
            } else
            {
                // Need to activate menu to get hitbox for finding free place
                for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
                {
                    if (pag.button() != null && pag.button().name().equals(cmd.name))
                    {
                        pag.button().use(new MenuGrid.Interaction(1, 0));
                        break;
                    }
                }

                if (NUtils.getGameUI().map.placing == null)
                {
                    for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
                    {
                        if (pag.button() != null && pag.button().name().equals(cmd.name))
                        {
                            pag.button().use(new MenuGrid.Interaction(1, 0));
                            break;
                        }
                    }
                }
                NUtils.addTask(new WaitPlob());
                MapView.Plob plob = NUtils.getGameUI().map.placing.get();
                double rotationAngle = (rotationCount * Math.PI / 2.0);
                plob.a = rotationAngle;

                nurgling.NHitBox hitBox = plob.ngob.hitBox;
                if (hitBox == null && cmd.customHitBox != null)
                {
                    hitBox = cmd.customHitBox;
                }

                pos = Finder.getFreePlace(area, hitBox, rotationAngle);
            }

            if (pos == null)
            {
                break;
            }

            // Check if there's already an object at this position (skip ghost gobs)
            Gob existingGob = Finder.findGob(pos);
            if (existingGob != null && existingGob.getattr(GhostAlpha.class) == null)
            {
                if (NParser.checkName(existingGob.ngob.name, "gfx/terobjs/consobj"))
                {
                    // Found unfinished construction, finish it (finishConstruction handles its own refilling)
                    // Cancel any active placement cursor first
                    if (NUtils.getGameUI().map.placing != null)
                    {
                        NUtils.getGameUI().map.placing.cancel();
                        NUtils.getGameUI().map.placing = null;
                    }
                    Results result = finishConstruction(gui, pos, existingGob);
                    if (!result.IsSuccess())
                    {
                        return result;
                    }

                    // Remove ghost after successful construction
                    if (ghostPreview != null)
                    {
                        ghostPreview.removeGhost(pos);
                    }

                    ghostIndex++;

                    // Move player away and continue to next position
                    Coord2d finalPos = pos;
                    final Gob[] targetGob = {null};
                    NUtils.addTask(new NTask()
                    {
                        @Override
                        public boolean check()
                        {
                            return (targetGob[0] = Finder.findGob(finalPos)) != null;
                        }
                    });
                    if (targetGob[0] != null)
                    {
                        Coord2d shift = targetGob[0].rc.sub(NUtils.player().rc).norm().mul(4);
                        new GoTo(NUtils.player().rc.sub(shift)).run(gui);
                    }

                    continue;
                } else
                {
                    // Object is not consobj, skip this position
                    if (ghostPreview != null)
                    {
                        ghostPreview.removeGhost(pos);
                    }
                    ghostIndex++;
                    continue;
                }
            }

            // Activate build menu for new construction
            for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
            {
                if (pag.button() != null && pag.button().name().equals(cmd.name))
                {
                    pag.button().use(new MenuGrid.Interaction(1, 0));
                    break;
                }
            }

            if (NUtils.getGameUI().map.placing == null)
            {
                for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
                {
                    if (pag.button() != null && pag.button().name().equals(cmd.name))
                    {
                        pag.button().use(new MenuGrid.Interaction(1, 0));
                        break;
                    }
                }
            }
            NUtils.addTask(new WaitPlob());
            MapView.Plob plob = NUtils.getGameUI().map.placing.get();
            double rotationAngle = (rotationCount * Math.PI / 2.0);
            plob.a = rotationAngle;

            nurgling.NHitBox hitBox = plob.ngob.hitBox;
            if (hitBox == null && cmd.customHitBox != null)
            {
                hitBox = cmd.customHitBox;
            }

            // Now check and refill resources for new construction
            boolean isExist = false;
            ArrayList<Ingredient> curings = new ArrayList<Ingredient>();
            for (Ingredient ingredient : cmd.ingredients)
            {
                int size = NUtils.getGameUI().getInventory().getItems(ingredient.name).size();
                if (size > 0)
                {
                    isExist = true;
                }
                Ingredient copy = new Ingredient(ingredient.coord, ingredient.nArea, ingredient.name, ingredient.count - size, ingredient.specialWay);
                copy.containers = ingredient.containers;
                copy.left = Math.max(0, size - copy.count);
                curings.add(copy);
            }
            if (!isExist)
            {
                if (!refillIng(gui, curings, context))
                    return Results.ERROR("NO ITEMS");
            }

            PathFinder pf = new PathFinder(NGob.getDummy(pos, rotationAngle, hitBox), true);
            pf.isHardMode = true;
            pf.run(gui);

            gui.map.wdgmsg("place", pos.floor(posres), (int) Math.round(rotationAngle * 32768 / Math.PI), 1, 0);
            NUtils.addTask(new WaitConstructionObject(pos));

            String windowName = cmd.windowName != null ? cmd.windowName : cmd.name;
            NUtils.addTask(new WaitWindow(windowName));
            Gob gob;
            int buildAttempt = 0;
            boolean windowStillOpen = true;

            while (windowStillOpen)
            {
                buildAttempt++;

                if (needRefill(curings))
                {
                    if (!refillIng(gui, curings, context))
                        return Results.ERROR("NO ITEMS");

                    // Return to construction site
                    gob = Finder.findGob(pos);
                    if (gob == null)
                        return Results.ERROR("Something went wrong, no gob");

                    // Use PathFinder without HardMode - HardMode causes NPE when end cell is free
                    // because end_poses is not initialized in that case
                    PathFinder pf2 = new PathFinder(gob);
                    pf2.isHardMode = true;
                    pf2.run(gui);

                    NUtils.rclickGob(gob);
                    NUtils.addTask(new WaitWindow(windowName));
                }

                NUtils.startBuild(NUtils.getGameUI().getWindow(windowName));

                NUtils.addTask(new NTask()
                {
                    int count = 0;

                    @Override
                    public boolean check()
                    {
                        return NUtils.getGameUI().prog != null || count++ > 100;
                    }
                });
                WaitBuildState wbs = new WaitBuildState();
                NUtils.addTask(wbs);
                if (wbs.getState() == WaitBuildState.State.TIMEFORDRINK)
                {
                    if (!(new Drink(0.9, false).run(gui)).IsSuccess())
                        return Results.ERROR("Drink is not found");
                } else if (wbs.getState() == WaitBuildState.State.DANGER)
                {
                    return Results.ERROR("Low energy");
                }

                // Small delay to let window state update
                Thread.sleep(300);

                // Check if window is still open - if closed, construction is complete
                Window window = NUtils.getGameUI().getWindow(windowName);
                if (window == null)
                {
                    windowStillOpen = false;
                } else
                {
                    windowStillOpen = true;
                }
            }

            // Check if construction was actually completed
            Gob finalGob = Finder.findGob(pos);
            boolean constructionCompleted = (finalGob != null && !NParser.checkName(finalGob.ngob.name, "gfx/terobjs/consobj"));

            if (constructionCompleted)
            {

                // Remove ghost after successful construction
                if (ghostPreview != null)
                {
                    ghostPreview.removeGhost(pos);
                }

                ghostIndex++;
            }
            Coord2d finalPos = pos;
            final Gob[] targetGob = {null};
            NUtils.addTask(new NTask()
            {
                @Override
                public boolean check()
                {
                    return (targetGob[0] = Finder.findGob(finalPos)) != null;
                }
            });
            if (targetGob[0] != null)
            {
                Coord2d shift = targetGob[0].rc.sub(NUtils.player().rc).norm().mul(4);
                new GoTo(NUtils.player().rc.sub(shift)).run(gui);
            }

            // Don't wait for items to return to inventory if using ghosts
            // because we'll start a new iteration with fresh curings
            if (ghostPositions == null || ghostPositions.isEmpty())
            {
                for (Ingredient ingredient : curings)
                {
                    NUtils.addTask(new WaitItems(NUtils.getGameUI().getInventory(), ingredient.name, ingredient.left));
                }
            }

            // Get next position
            if (ghostPositions != null && !ghostPositions.isEmpty())
            {
                // When using ghosts, check if we've processed all ghosts
                if (ghostIndex >= ghostPositions.size())
                {
                    pos = null;
                } else
                {
                    // Set pos to next ghost position for loop condition check
                    pos = ghostPositions.get(ghostIndex);
                }
            } else
            {
                // When not using ghosts, find next free place
                pos = Finder.getFreePlace(area, hitBox, rotationAngle);
            }
        }
        while (pos != null);
        return Results.SUCCESS();
    }

    private boolean refillIng(NGameUI gui, ArrayList<Ingredient> curings, NContext context) throws InterruptedException
    {
        for (Ingredient ingredient : curings)
        {
            if (ingredient.specialWay == null)
            {
                // Skip if we don't need any more of this ingredient
                if (ingredient.count <= 0) {
                    continue;
                }
                
                if (ingredient.nArea == null) {
                    NUtils.getGameUI().msg("No area defined for " + ingredient.name.getKeys().get(0));
                    continue;
                }
                
                // Navigate to ingredient area
                NUtils.navigateToArea(ingredient.nArea);
                Pair<Coord2d, Coord2d> ingredientArea = ingredient.nArea.getRCArea();
                if (ingredientArea == null) {
                    NUtils.getGameUI().msg("Cannot access ingredient area for " + ingredient.name.getKeys().get(0));
                    continue;
                }
                
                // Populate containers after navigation if not done yet
                if (ingredient.containers.isEmpty()) {
                    for (Gob sm : Finder.findGobs(ingredientArea, new NAlias(new ArrayList<>(NContext.contcaps.keySet()))))
                    {
                        Container cand = new Container(sm, NContext.contcaps.get(sm.ngob.name), null);
                        cand.initattr(Container.Space.class);
                        ingredient.containers.add(cand);
                    }
                }

                if (!ingredient.containers.isEmpty())
                {
                    for (Container container : ingredient.containers)
                    {
                        Container.Space space = container.getattr(Container.Space.class);
                        if (space.isReady())
                        {
                            int aval = (int) space.getRes().get(Container.Space.MAXSPACE) - (int) space.getRes().get(Container.Space.FREESPACE);
                            if (aval != 0)
                            {
                                new PathFinder(Finder.findGob(container.gobid)).run(gui);
                                new OpenTargetContainer(container).run(gui);
                                TakeAvailableItemsFromContainer tifc = new TakeAvailableItemsFromContainer(container, ingredient.name, ingredient.count);
                                tifc.run(gui);
                                ingredient.count = ingredient.count - tifc.getCount();
                            }
                        } else
                        {
                            new PathFinder(Finder.findGob(container.gobid)).run(gui);
                            new OpenTargetContainer(container).run(gui);
                            TakeAvailableItemsFromContainer tifc = new TakeAvailableItemsFromContainer(container, ingredient.name, ingredient.count);
                            tifc.run(gui);
                            ingredient.count = ingredient.count - tifc.getCount();
                        }

                        if (ingredient.count == 0)
                            break;
                    }
                } else
                {
                    // No containers, try stockpiles
                    while (ingredient.count != 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(ingredient.coord) != 0)
                    {
                        ArrayList<Gob> piles = Finder.findGobs(ingredientArea, new NAlias("stockpile"));
                        if (piles.isEmpty())
                        {
                            if (NUtils.getGameUI().getInventory().getItems(ingredient.name).size() != ingredient.count)
                                return false;
                        }
                        piles.sort(NUtils.d_comp);
                        if (piles.isEmpty())
                            return false;
                        Gob pile = piles.get(0);
                        new PathFinder(pile).run(NUtils.getGameUI());
                        new OpenTargetContainer("Stockpile", pile).run(NUtils.getGameUI());
                        TakeItemsFromPile tifp;
                        (tifp = new TakeItemsFromPile(pile, NUtils.getGameUI().getStockpile(), Math.min(ingredient.count, NUtils.getGameUI().getInventory().getNumberFreeCoord(ingredient.coord)))).run(gui);
                        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                        ingredient.count = ingredient.count - tifp.getResult();
                    }
                }
            }
        }
        
        // Navigate back to build area
        if (buildArea != null) {
            NUtils.navigateToArea(buildArea);
        }
        
        return !needRefill(curings);
    }

    boolean needRefill(ArrayList<Ingredient> curings) throws InterruptedException
    {
        boolean needRefill = false;
        for (Ingredient ingredient : curings)
        {
            if (ingredient.count > 0)
            {
                int size = NUtils.getGameUI().getInventory().getItems(ingredient.name).size();
                if (size > 0)
                {
                    return false;
                } else
                {
                    needRefill = true;
                }
            }
        }
        return needRefill;
    }

    private static class ConstructionProgress
    {
        public int current;
        public int required;

        public ConstructionProgress(int current, int required)
        {
            this.current = current;
            this.required = required;
        }
    }

    private ArrayList<ConstructionProgress> parseConstructionProgress(Window window)
    {
        ArrayList<ConstructionProgress> progress = new ArrayList<>();

        try
        {
            int widgetCount = 0;
            int nisboxCount = 0;

            for (Widget wdg = window.lchild; wdg != null; wdg = wdg.prev)
            {
                widgetCount++;

                if (wdg instanceof NISBox)
                {
                    nisboxCount++;
                    NISBox nisbox = (NISBox) wdg;

                    // NISBox has calcCount() and calcFreeSpace() methods
                    int current = nisbox.calcCount();
                    int freeSpace = nisbox.calcFreeSpace();

                    if (current >= 0 && freeSpace >= 0)
                    {
                        int required = current + freeSpace;
                        progress.add(new ConstructionProgress(current, required));
                    }
                }
            }

        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return progress;
    }

    private Results finishConstruction(NGameUI gui, Coord2d pos, Gob consobj) throws InterruptedException
    {

        PathFinder pf = new PathFinder(consobj);
        pf.isHardMode = true;
        pf.run(gui);

        NUtils.rclickGob(consobj);

        String windowName = cmd.windowName != null ? cmd.windowName : cmd.name;
        NUtils.addTask(new WaitWindow(windowName));

        Gob gob = consobj;
        int attemptCount = 0;
        final int MAX_ATTEMPTS = 10;

        // Loop until construction is complete or we exceed max attempts
        while ((gob = Finder.findGob(pos)) != null && NParser.checkName(gob.ngob.name, "gfx/terobjs/consobj"))
        {
            attemptCount++;

            if (attemptCount > MAX_ATTEMPTS)
            {
                return Results.ERROR("Too many attempts to finish construction");
            }

            // Get current progress
            Window window = NUtils.getGameUI().getWindow(windowName);
            if (window == null)
            {
                return Results.ERROR("Cannot open construction window");
            }

            ArrayList<ConstructionProgress> progressList = parseConstructionProgress(window);
            if (progressList.isEmpty() || progressList.size() != cmd.ingredients.size())
            {
                return Results.ERROR("Cannot parse construction progress");
            }

            // Calculate remaining ingredients for THIS attempt
            ArrayList<Ingredient> remainingIngredients = new ArrayList<>();
            for (int i = 0; i < cmd.ingredients.size(); i++)
            {
                Ingredient original = cmd.ingredients.get(i);
                ConstructionProgress prog = progressList.get(i);

                int remaining = prog.required - prog.current;

                if (remaining > 0)
                {
                    Ingredient copy = new Ingredient(original.coord, original.nArea, original.name, remaining, original.specialWay);
                    copy.containers = original.containers;
                    remainingIngredients.add(copy);
                }
            }

            // If nothing left to add, construction should be complete
            if (!remainingIngredients.isEmpty())
            {
                if (!refillIng(gui, remainingIngredients, context))
                {
                    return Results.ERROR("Cannot refill ingredients for construction");
                }

                // Return to construction sign with HardMode
                gob = Finder.findGob(pos);
                if (gob == null)
                {
                    return Results.ERROR("Something went wrong, no gob");
                }

                PathFinder pf2 = new PathFinder(gob);
                pf2.isHardMode = true;
                pf2.run(gui);

                NUtils.rclickGob(gob);
                NUtils.addTask(new WaitWindow(windowName));
            }

            // Attempt to build
            NUtils.startBuild(NUtils.getGameUI().getWindow(windowName));

            NUtils.addTask(new NTask()
            {
                int count = 0;

                @Override
                public boolean check()
                {
                    return NUtils.getGameUI().prog != null || count++ > 100;
                }
            });

            WaitBuildState wbs = new WaitBuildState();
            NUtils.addTask(wbs);
            if (wbs.getState() == WaitBuildState.State.TIMEFORDRINK)
            {
                if (!(new Drink(0.9, false).run(gui)).IsSuccess())
                {
                    return Results.ERROR("Drink is not found");
                }
            } else if (wbs.getState() == WaitBuildState.State.DANGER)
            {
                return Results.ERROR("Low energy");
            }
        }

        return Results.SUCCESS();
    }
}
