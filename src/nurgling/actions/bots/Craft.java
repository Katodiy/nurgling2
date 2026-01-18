package nurgling.actions.bots;

import haven.*;
import haven.res.lib.itemtex.ItemTex;
import haven.res.ui.tt.cn.CustomName;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static haven.OCache.posres;
import nurgling.tools.StackSupporter;


public class Craft implements Action {


    public Craft(List<NMakewindow.Spec> in, List<NMakewindow.Spec> out, String station, int count) {

    }

    public Craft(List<NMakewindow.Spec> in, List<NMakewindow.Spec> out, String station) {
        this(in, out, station, 1);
    }

    public Craft(NMakewindow mwnd, int size) {
        this.mwnd = mwnd;
        this.count = size;
    }

    public Craft(NMakewindow mwnd) {
        this(mwnd, 1);
    }

    NMakewindow mwnd = null;
    String tools = null;
    int count = 0;

    boolean isGlobalMode = false;

    private int getActualItemCount(WItem item) {
        if (item.item.info != null) {
            for (ItemInfo inf : item.item.info) {
                if (inf instanceof CustomName) {
                    float count = ((CustomName) inf).count;
                    if (count > 0) {
                        return (int) (count * 100);
                    }
                }
            }
        }
        return 1;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (mwnd != null) {
            return mwnd_run(gui);
        }
        return Results.SUCCESS();
    }

    private Results mwnd_run(NGameUI gui) throws InterruptedException {
        NContext ncontext = new NContext(gui);
        int size = 0;
        for (NMakewindow.Spec s : mwnd.inputs) {
            // Skip ignored optional ingredients
            if (s.ing != null && s.ing.isIgnored) {
                continue;
            }

            if (!s.categories) {
                ncontext.addInItem(s.name, ItemTex.create(ItemTex.save(s.spr)));
                if (!ncontext.isInBarrel(s.name)) {
                    size += s.count;
                }
            } else if (s.ing != null) {
                ncontext.addInItem(s.ing.name, s.ing.img);
                if (!ncontext.isInBarrel(s.ing.name)) {
                    size += s.count;
                }
            } else {
                // Auto-select any available ingredient from category
                selectIngredientFromCategory(s);
                if (s.ing != null && !s.ing.isIgnored) {
                    ncontext.addInItem(s.ing.name, s.ing.img);
                    if (!ncontext.isInBarrel(s.ing.name)) {
                        size += s.count;
                    }
                }
            }
        }

        for (NMakewindow.Spec s : mwnd.outputs) {

            if (!mwnd.noTransfer.a) {
                if (!s.categories) {
                    if(!ncontext.isInBarrel(s.name))
                        size += s.count;
                    ncontext.addOutItem(s.name, ItemTex.create(ItemTex.save(s.spr)), 1);
                } else if (s.ing != null) {
                    if(!ncontext.isInBarrel(s.ing.name))
                        size += s.count;
                    ncontext.addOutItem(s.ing.name, s.ing.img, 1);
                }
            }
        }

        if (!mwnd.tools.isEmpty()) {
            ncontext.addTools(mwnd.tools);
        } else {
            if (mwnd.outputs.size() == 1) {
                String outName = mwnd.outputs.get(0).name;
                ncontext.addCustomTool(outName);
            }
        }

        if (ncontext.equip != null)
            new Equip(new NAlias(ncontext.equip)).run(gui);

        AtomicInteger left = new AtomicInteger(count);

        for (NMakewindow.Spec s : mwnd.inputs) {
            // Skip ignored optional ingredients
            if (s.ing != null && s.ing.isIgnored) {
                continue;
            }
            
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item)) {
                if (ncontext.workstation == null) {
                    NArea barrelwa = ncontext.getSpecArea(Specialisation.SpecName.barrelworkarea);
                    if (barrelwa == null)
                        return Results.ERROR("Not found area for work with barrels!");
                    else
                        ncontext.bwaused = true;
                }
                else
                {
                    ncontext.bwaused = true;
                }
            }
        }

        // Prepare workstation once before craft loop
        if (ncontext.workstation != null) {
            if (!new PrepareWorkStation(ncontext, ncontext.workstation.station).run(gui).IsSuccess()) {
                return Results.ERROR("Failed to prepare workstation");
            }
            if (ncontext.workstation.targetPoint != null) {
                new PathFinder(ncontext.workstation.targetPoint.getCurrentCoord()).run(gui);
            }
            // Refresh mwnd reference after PrepareWorkStation (may have changed due to LightFire)
            refreshMakeWidget(gui);
        }

        Results craftResult = null;
        while (left.get() > 0) {
            craftResult = crafting(ncontext, gui, size, left);
            if (!craftResult.IsSuccess()) {
                return craftResult;
            }
        }

        for (NMakewindow.Spec s : mwnd.inputs) {
            // Skip ignored optional ingredients
            if (s.ing != null && s.ing.isIgnored) {
                continue;
            }
            
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item)) {
                new ReturnBarrelFromWorkArea(ncontext, item).run(gui);
            }
        }

        if (!mwnd.noTransfer.a) {
            new FreeInventory2(ncontext).run(gui);
        }


        return Results.SUCCESS();
    }

    Results crafting(NContext ncontext, NGameUI gui, int size, AtomicInteger left) throws InterruptedException {

        double currentEnergy = NUtils.getEnergy();

        if (currentEnergy < 0.25) {
            if (!new RestoreResources().run(gui).IsSuccess()) {
                return Results.ERROR("Energy too low and failed to restore resources");
            }
        }
        
        int freeSpace = NUtils.getGameUI().getInventory().getFreeSpace();

        int for_craft;
        if (size == 0) {
            for_craft = left.get();
        } else {
            // Use stack-aware calculation for better inventory utilization
            for_craft = calculateMaxCraftsWithStacking(ncontext, freeSpace, left.get());
        }
        

        if (for_craft <= 0) {
            return Results.ERROR("Not enough inventory space");
        }
        
        for (NMakewindow.Spec s : mwnd.inputs) {
            // Auto-select ingredient from category if not already selected
            if (s.categories && s.ing == null) {
                selectIngredientFromCategory(s);
            }
            
            // Skip ignored optional ingredients
            if (s.ing != null && s.ing.isIgnored) {
                continue;
            }
            
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item)) {
                if(ncontext.workstation == null) {
                    new TransferBarrelInWorkArea(ncontext, item).run(gui);
                }
                else if(ncontext.workstation.targetPoint == null)
                {
                    new TransferBarrelToWorkstation(ncontext, item).run(gui);
                }
            } else {
                if (!new TakeItems2(ncontext, s.ing == null ? s.name : s.ing.name, s.count * for_craft).run(gui).IsSuccess()) {
                    return Results.ERROR("Failed to take items: " + item);
                }
            }
        }



        if (ncontext.workstation != null) {
            if (!new UseWorkStation(ncontext).run(gui).IsSuccess()) {
                return Results.ERROR("Failed to use workstation");
            }
        }
        else if (ncontext.bwaused) {
            NArea barrelwa = ncontext.getSpecArea(Specialisation.SpecName.barrelworkarea);
            Pair<Coord2d, Coord2d> rcArea = barrelwa.getRCArea();
            Coord2d center = rcArea.b.sub(rcArea.a).div(2).add(rcArea.a);
            new PathFinder(center).run(gui);
        }

        int count = 0;
        ArrayList<Long> barrelIds = GetBarrelsIds(ncontext);
        gui.msg("Craft: Will try to open " + barrelIds.size() + " barrel(s)");
        
        for (Long barrelid : barrelIds) {
            Gob barrel = Finder.findGob(barrelid);
            if (barrel == null) {
                gui.msg("Craft: Barrel with id " + barrelid + " not found!");
                continue;
            }
            
            // Check distance to barrel - need to be close enough to interact
            double distToBarrel = NUtils.player().rc.dist(barrel.rc);
            gui.msg("Craft: Distance to barrel " + barrelid + ": " + String.format("%.2f", distToBarrel));
            
            if (distToBarrel > 20) {
                // Too far from barrel, need to move closer
                gui.msg("Craft: Too far from barrel, moving closer...");
                new PathFinder(barrel).run(gui);
            }
            
            gui.map.wdgmsg("click", Coord.z, barrel.rc.floor(posres), 3, 0, 0, (int) barrel.id,
                    barrel.rc.floor(posres), 0, -1);
            count++;
            int finalCount = count;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return NUtils.getGameUI().getWindowsNum("Barrel") == finalCount;
                }
            });
        }
        ArrayList<Window> windows = NUtils.getGameUI().getWindows("Barrel");
        gui.msg("Craft: Found " + windows.size() + " barrel window(s) for resource check");
        
        boolean hasEnoughResources = true;
        String insufficientItem = null;
        double foundAmount = 0;
        double requiredAmount = 0;
        
        for (NMakewindow.Spec s : mwnd.inputs) {
            // Skip ignored optional ingredients
            if (s.ing != null && s.ing.isIgnored) {
                continue;
            }
            
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item)) {
                double val = gui.findBarrelContent(windows, new NAlias(item));
                gui.msg("Craft: Barrel content check for '" + item + "': raw value = " + val);
                
                // Handle case when barrel content not found (-1 means not found)
                if (val < 0) {
                    hasEnoughResources = false;
                    insufficientItem = item;
                    foundAmount = 0; // Not found = 0
                    requiredAmount = s.count;
                    gui.msg("Craft: Barrel content for '" + item + "' NOT FOUND (barrel may be closed or missing)");
                    break;
                }
                
                double valInMilligrams = val * 100;
                if(valInMilligrams < s.count)
                {
                    hasEnoughResources = false;
                    insufficientItem = item;
                    foundAmount = valInMilligrams;
                    requiredAmount = s.count;
                    break;
                }
            }
        }
        
        if (!hasEnoughResources) {
            for (NMakewindow.Spec s : mwnd.inputs) {
                // Skip ignored optional ingredients
                if (s.ing != null && s.ing.isIgnored) {
                    continue;
                }
                
                String item = s.ing == null ? s.name : s.ing.name;
                if (ncontext.isInBarrel(item)) {
                    new ReturnBarrelFromWorkArea(ncontext, item).run(gui);
                }
            }
            return Results.ERROR("Not enough resources in barrels: '" + insufficientItem + 
                    "' found " + String.format("%.2f", foundAmount) + 
                    ", required " + String.format("%.2f", requiredAmount));
        }

        new Drink(0.9, false).run(gui);
        int resfc = for_craft;
        String targetName = null;
        for (NMakewindow.Spec s : mwnd.outputs) {
            String itemName = s.ing != null ? s.ing.name : s.name;
            int outputMultiplier = NContext.getOutputMultiplier(itemName);
            resfc = s.count * for_craft * outputMultiplier;
            ArrayList<WItem> currentItems;
            if (s.ing != null) {
                targetName = s.ing.name;
                currentItems = NUtils.getGameUI().getInventory().getItems(new NAlias(s.ing.name));
            } else {
                targetName = s.name;
                currentItems = NUtils.getGameUI().getInventory().getItems(new NAlias(s.name));
            }
            
            int actualCurrentCount = 0;
            for (WItem item : currentItems) {
                actualCurrentCount += getActualItemCount(item);
            }
            
            resfc += actualCurrentCount;
            
        }

        craftProc(ncontext, gui, resfc, targetName);

        boolean isCauldron = ncontext.workstation != null &&
                ncontext.workstation.station != null &&
                ncontext.workstation.station.contains("gfx/terobjs/cauldron");

        if (isCauldron)
        {

            Gob cauldron = Finder.findGob(ncontext.workstation.selected);
            PrepareCauldron pc = new PrepareCauldron(cauldron, ncontext);
            pc.run(gui);
            // Refresh mwnd reference after PrepareCauldron (may have changed due to LightFire)
            refreshMakeWidget(gui);
            if(pc.wasUpdate)
            {
                if (!new UseWorkStation(ncontext).run(gui).IsSuccess()) {
                    return Results.ERROR("Failed to use workstation");
                }
                craftProc(ncontext, gui, resfc, targetName);
            }
        }
        for (NMakewindow.Spec s : mwnd.outputs) {
            if (s.ing != null) {
                NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(s.ing.name), resfc));
            } else {
                NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(s.name), resfc));
            }
        }
        HashSet<String> targets = new HashSet<>();
        for (NMakewindow.Spec s : mwnd.outputs) {
            GetItems gi;
            if (s.ing != null) {
                NUtils.getUI().core.addTask(gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(s.ing.name)));
                targets.add(s.ing.name);
            } else {
                NUtils.getUI().core.addTask(gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(s.name)));
                targets.add(s.name);
            }


        }
        if (!mwnd.noTransfer.a) {
            new FreeInventory2(ncontext).run(gui);
        }
        left.set(left.get() - for_craft);
        return Results.SUCCESS();
    }

    private void craftProc(NContext ncontext, NGameUI gui, int resfc, String targetName) throws InterruptedException
    {
        mwnd.wdgmsg("make", 1);
        int finalResfc = resfc;
        String finalTargetName = targetName;
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {

                return (((gui.prog != null) && (gui.prog.prog > 0) && ((ncontext.workstation == null) || (ncontext.workstation.selected == -1) || NUtils.isWorkStationReady(ncontext.workstation.station, Finder.findGob(ncontext.workstation.selected)))));
            }
        });



        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                GetItems gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(finalTargetName));
                gi.check();
                return gui.prog == null || !gui.prog.visible || gi.getResult().size() >= finalResfc;
            }
        });
        NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres),3, 0);
        NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres),1, 0);
    }

    ArrayList<Long> GetBarrelsIds(NContext ncontext) throws InterruptedException
    {
        ArrayList<Long> ids = new ArrayList<>();
        NUtils.getGameUI().msg("GetBarrelsIds: Checking " + mwnd.inputs.size() + " inputs for barrel items");
        
        for (NMakewindow.Spec s : mwnd.inputs)
        {
            // Skip ignored optional ingredients
            if (s.ing != null && s.ing.isIgnored) {
                continue;
            }
            
            String item = s.ing == null ? s.name : s.ing.name;
            String storedHash = ncontext.getPlacedBarrelHash(item);
            NUtils.getGameUI().msg("GetBarrelsIds: Checking item '" + item + "', isInBarrel=" + ncontext.isInBarrel(item) + 
                    ", storedHash=" + (storedHash != null ? storedHash.substring(0, Math.min(16, storedHash.length())) + "..." : "null"));
            
            if (ncontext.isInBarrel(item))
            {
                Gob barrel = ncontext.getBarrelInWorkArea(item);
                NUtils.getGameUI().msg("GetBarrelsIds: getBarrelInWorkArea('" + item + "') returned " + 
                        (barrel != null ? "barrel id=" + barrel.id + " hash=" + barrel.ngob.hash.substring(0, Math.min(16, barrel.ngob.hash.length())) + "..." : "NULL"));
                if (barrel != null)
                    ids.add(barrel.id);
            }
        }
        return ids;
    }

    private void selectIngredientFromCategory(NMakewindow.Spec spec) {
        if (!spec.categories || spec.ing != null) {
            return;
        }

        ArrayList<org.json.JSONObject> categoryItems = VSpec.categories.get(spec.name);
        if (categoryItems == null || categoryItems.isEmpty()) {
            NUtils.getGameUI().msg("Category '" + spec.name + "' not found in VSpec.categories");
            return;
        }

        NUtils.getGameUI().msg("Searching ingredient for category: " + spec.name + " (" + categoryItems.size() + " options)");

        // First try to find in nearby areas
        for (org.json.JSONObject obj : categoryItems) {
            String itemName = (String) obj.get("name");
            if (NContext.findIn(itemName) != null) {
                NUtils.getGameUI().msg("Found nearby: " + itemName + " for category " + spec.name);
                spec.ing = mwnd.new Ingredient(obj);
                return;
            }
        }

        // If not found nearby, try global search
        for (org.json.JSONObject obj : categoryItems) {
            String itemName = (String) obj.get("name");
            if (NContext.findInGlobal(itemName) != null) {
                NUtils.getGameUI().msg("Found globally: " + itemName + " for category " + spec.name);
                spec.ing = mwnd.new Ingredient(obj);
                return;
            }
        }

        NUtils.getGameUI().msg("No available ingredients found for category: " + spec.name);
    }

    /**
     * Refresh the mwnd reference from the current craft window.
     * This is needed after operations that may change the craft widget (like LightFire).
     */
    private void refreshMakeWidget(NGameUI gui) {
        if (gui.craftwnd != null && gui.craftwnd.makeWidget != null) {
            mwnd = gui.craftwnd.makeWidget;
        }
    }

    /**
     * Calculate the number of slots needed for a given number of crafts, considering stacking.
     * @param ncontext The crafting context
     * @param numCrafts Number of crafts to calculate for
     * @return Total number of inventory slots needed
     */
    private int calculateSlotsNeeded(NContext ncontext, int numCrafts) {
        int totalSlots = 0;
        
        // Calculate slots for inputs
        for (NMakewindow.Spec s : mwnd.inputs) {
            // Skip ignored optional ingredients
            if (s.ing != null && s.ing.isIgnored) {
                continue;
            }
            
            String itemName = s.ing != null ? s.ing.name : s.name;
            
            // Skip items stored in barrels
            if (ncontext.isInBarrel(itemName)) {
                continue;
            }
            
            int itemsNeeded = s.count * numCrafts;
            int stackSize = StackSupporter.getFullStackSize(itemName);
            
            // Calculate slots needed: ceil(itemsNeeded / stackSize)
            int slotsForItem = (itemsNeeded + stackSize - 1) / stackSize;
            totalSlots += slotsForItem;
        }
        
        // Calculate slots for outputs (if noTransfer is not enabled)
        if (!mwnd.noTransfer.a) {
            for (NMakewindow.Spec s : mwnd.outputs) {
                String itemName = s.ing != null ? s.ing.name : s.name;
                
                // Skip items stored in barrels
                if (ncontext.isInBarrel(itemName)) {
                    continue;
                }
                
                int outputMultiplier = NContext.getOutputMultiplier(itemName);
                int itemsProduced = s.count * numCrafts * outputMultiplier;
                int stackSize = StackSupporter.getFullStackSize(itemName);
                
                // Calculate slots needed: ceil(itemsProduced / stackSize)
                int slotsForItem = (itemsProduced + stackSize - 1) / stackSize;
                totalSlots += slotsForItem;
            }
        }
        
        return totalSlots;
    }

    /**
     * Calculate the maximum number of crafts that can fit in the inventory, considering stacking.
     * Uses binary search to find the optimal number.
     * @param ncontext The crafting context
     * @param freeSpace Available inventory slots
     * @param maxCrafts Maximum number of crafts desired
     * @return Maximum number of crafts that can fit
     */
    private int calculateMaxCraftsWithStacking(NContext ncontext, int freeSpace, int maxCrafts) {
        if (freeSpace <= 0) {
            return 0;
        }
        
        // Binary search for the maximum number of crafts
        int low = 0;
        int high = maxCrafts;
        int result = 0;
        
        while (low <= high) {
            int mid = (low + high) / 2;
            int slotsNeeded = calculateSlotsNeeded(ncontext, mid);
            
            if (slotsNeeded <= freeSpace) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        
        return result;
    }

}
