package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.NBlueprintPlanterProp;
import nurgling.overlays.TreeGhostPreview;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.Specialisation;
import org.json.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BlueprintTreePlanter implements Action {

    private static final NAlias TREE_SEEDS = VSpec.getAllPlantableSeeds();
    private static final NAlias TREE_OBJECTS = new NAlias("tree");
    private BlueprintPlob blueprintPlob = null;
    private List<PlantPosition> plantPositions = new ArrayList<>();
    private NBlueprintPlanterProp prop = null;
    
    private static class PlantPosition {
        Coord2d worldPos;
        String treeType;
        
        PlantPosition(Coord2d worldPos, String treeType) {
            this.worldPos = worldPos;
            this.treeType = treeType;
        }
    }
    
    private static class BlueprintData {
        Map<Coord, String> trees;
        int width;
        int height;
        
        BlueprintData(Map<Coord, String> trees, int width, int height) {
            this.trees = trees;
            this.width = width;
            this.height = height;
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
            // Load saved prop
            prop = NBlueprintPlanterProp.get(NUtils.getUI().sessInfo);
            
            // Auto-place last blueprint if it was saved
            if (prop != null && prop.gridId != null && prop.tileCoord != null && prop.blueprintName != null) {
                autoPlaceLastBlueprint(gui);
            }
            
            nurgling.widgets.bots.BlueprintTreePlanter window = null;
            
            while (true) {
                try {
                    window = new nurgling.widgets.bots.BlueprintTreePlanter();
                    NUtils.getUI().core.addTask(new WaitCheckable(
                        NUtils.getGameUI().add(window, UI.scale(200, 200))
                    ));
                    
                    nurgling.widgets.bots.BlueprintTreePlanter.State state = window.getState();
                    
                    if (state == nurgling.widgets.bots.BlueprintTreePlanter.State.CANCELLED) {
                        cleanupBlueprint();
                        return Results.SUCCESS();
                    }
                    
                    if (state == nurgling.widgets.bots.BlueprintTreePlanter.State.PLACE_BLUEPRINT) {
                        String selectedBlueprint = window.getSelectedBlueprint();
                        if (selectedBlueprint == null || selectedBlueprint.isEmpty()) {
                            NUtils.getUI().msg("Please select a blueprint first.");
                            continue;
                        }
                        
                        // Try to load and place saved blueprint position if available
                        if (prop.gridId != null && prop.tileCoord != null && 
                            selectedBlueprint.equals(prop.blueprintName)) {
                            Results autoResult = autoPlaceLastBlueprint(gui);
                            if (autoResult.IsSuccess()) {
                                continue;
                            }
                        }
                        
                        // Otherwise place manually
                        Results placeResult = placeBlueprintInWorld(gui, selectedBlueprint);
                        if (!placeResult.IsSuccess()) {
                            return placeResult;
                        }
                        continue;
                    }
                    
                    if (state == nurgling.widgets.bots.BlueprintTreePlanter.State.CLEAR_BLUEPRINT) {
                        clearBlueprint();
                        continue;
                    }
                    
                    if (state == nurgling.widgets.bots.BlueprintTreePlanter.State.START_BOT) {
                        if (plantPositions.isEmpty()) {
                            NUtils.getUI().msg("No blueprint placed. Place blueprint first.");
                            continue;
                        }
                        
                        Results plantResult = executePlanting(gui);
                        cleanupBlueprint();
                        return plantResult;
                    }
                    
                } finally {
                    if (window != null) {
                        window.destroy();
                    }
                }
            }
            
        } catch (InterruptedException e) {
            cleanupBlueprint();
            throw e;
        } catch (Exception e) {
            cleanupBlueprint();
            return Results.ERROR("Unexpected error: " + e.getMessage());
        }
    }

    private Results placeBlueprintInWorld(NGameUI gui, String blueprintName) throws InterruptedException {
        try {
            BlueprintData blueprintData = loadBlueprint(blueprintName);
            
            if (blueprintData.trees.isEmpty()) {
                return Results.ERROR("No trees in blueprint. Please create a blueprint first.");
            }
            
            cleanupBlueprint();
            
            blueprintPlob = new BlueprintPlob(gui.map, blueprintData.trees, blueprintData.width, blueprintData.height);
            
            
            WaitBlueprintPlacement waitPlacement = new WaitBlueprintPlacement(blueprintPlob);
            NUtils.getUI().core.addTask(waitPlacement);
            
            if (!blueprintPlob.isPlaced()) {
                cleanupBlueprint();
                return Results.FAIL();
            }
            
            Coord2d blueprintOrigin = blueprintPlob.getPosition();
            Coord tilePos = blueprintOrigin.floor(MCache.tilesz);
            
            plantPositions.clear();
            
            for (Map.Entry<Coord, String> entry : blueprintData.trees.entrySet()) {
                Coord gridPos = entry.getKey();
                String treeType = entry.getValue();
                
                // Calculate world position from tile corner, not center
                Coord2d worldPos = tilePos.mul(MCache.tilesz).add(
                    gridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                    gridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
                );
                
                plantPositions.add(new PlantPosition(worldPos, treeType));
            }
            
            // Save placed position (grid ID + tile coords)
            Coord tilePos1 = blueprintOrigin.floor(MCache.tilesz);
            Coord gridCoord = tilePos1.div(MCache.cmaps);
            Coord tileInGrid = tilePos1.mod(MCache.cmaps);
            MCache.Grid grid = gui.map.glob.map.grids.get(gridCoord);
            
            if (grid != null) {
                prop.blueprintName = blueprintName;
                prop.gridId = grid.id;
                prop.tileCoord = tileInGrid;
                NBlueprintPlanterProp.set(prop);
            }
            
            return Results.SUCCESS();
            
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Failed to place blueprint: " + e.getMessage());
        }
    }

    private BlueprintData loadBlueprint(String blueprintName) throws Exception {
        Map<Coord, String> trees = new HashMap<>();
        int width = 10;
        int height = 10;
        
        File configFile = new File("tree_garden_blueprints.json");
        if (!configFile.exists()) {
            NUtils.getUI().msg("No blueprint file found. Please create a blueprint using Blueprint window.");
            return new BlueprintData(trees, width, height);
        }
        
        String content = new String(Files.readAllBytes(configFile.toPath()));
        JSONObject root = new JSONObject(content);
        
        JSONArray blueprintArray = root.getJSONArray("blueprints");
        for (int i = 0; i < blueprintArray.length(); i++) {
            JSONObject bp = blueprintArray.getJSONObject(i);
            String name = bp.getString("name");
            
            if (name.equals(blueprintName)) {
                width = bp.optInt("width", 10);
                height = bp.optInt("height", 10);
                
                if (bp.has("trees")) {
                    JSONArray treesArray = bp.getJSONArray("trees");
                    for (int j = 0; j < treesArray.length(); j++) {
                        JSONObject tree = treesArray.getJSONObject(j);
                        int x = tree.getInt("x");
                        int y = tree.getInt("y");
                        String type = tree.getString("type");
                        trees.put(new Coord(x, y), type);
                    }
                }
                break;
            }
        }
        return new BlueprintData(trees, width, height);
    }

    private void cleanupBlueprint() {
        try {
            if (blueprintPlob != null) {
                blueprintPlob.remove();
                blueprintPlob = null;
            }
        } catch (Exception e) {
            // Silent cleanup
        }
    }
    
    private void clearBlueprint() {
        cleanupBlueprint();
        plantPositions.clear();
        
        // Clear saved position
        prop.blueprintName = null;
        prop.gridId = null;
        prop.tileCoord = null;
        NBlueprintPlanterProp.set(prop);
    }
    
    private Results autoPlaceLastBlueprint(NGameUI gui) {
        try {
            BlueprintData blueprintData = loadBlueprint(prop.blueprintName);
            
            if (blueprintData.trees.isEmpty()) {
                return Results.FAIL();
            }
            
            // Find grid by ID
            MCache.Grid grid = null;
            for (MCache.Grid g : gui.map.glob.map.grids.values()) {
                if (g.id == prop.gridId) {
                    grid = g;
                    break;
                }
            }
            
            if (grid == null) {
                return Results.FAIL(); // Grid not loaded
            }
            
            cleanupBlueprint();
            
            // Calculate world position from grid ID + tile coords
            Coord gridCoord = grid.gc;
            Coord absoluteTilePos = gridCoord.mul(MCache.cmaps).add(prop.tileCoord);
            Coord2d worldPos = absoluteTilePos.mul(MCache.tilesz).add(MCache.tilesz.div(2));
            
            blueprintPlob = new BlueprintPlob(gui.map, blueprintData.trees, blueprintData.width, blueprintData.height);
            
            // Set position manually
            blueprintPlob.adjustPosition(Coord.z, worldPos);
            blueprintPlob.place();
            
            // Generate plant positions
            Coord tilePos = worldPos.floor(MCache.tilesz);
            plantPositions.clear();
            for (Map.Entry<Coord, String> entry : blueprintData.trees.entrySet()) {
                Coord gridPos = entry.getKey();
                String treeType = entry.getValue();
                
                // Calculate world position from tile corner, not center
                Coord2d treeWorldPos = tilePos.mul(MCache.tilesz).add(
                    gridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                    gridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
                );
                
                plantPositions.add(new PlantPosition(treeWorldPos, treeType));
            }
            
            return Results.SUCCESS();
            
        } catch (Exception e) {
            return Results.FAIL();
        }
    }

    private Results executePlanting(NGameUI gui) throws InterruptedException {
        try {
            if (plantPositions.isEmpty()) {
                return Results.ERROR("No planting positions");
            }
            
            NUtils.getUI().msg("Checking planted trees and preparing seedlings...");
            
            // Filter out positions where trees already exist
            List<PlantPosition> unplantedPositions = new ArrayList<>();
            for (PlantPosition pos : plantPositions) {
                Gob existingTree = Finder.findGob(pos.worldPos);
                if (existingTree == null || !NParser.isIt(existingTree, TREE_OBJECTS)) {
                    unplantedPositions.add(pos);
                }
            }
            
            NUtils.getUI().msg("Found " + unplantedPositions.size() + " trees to plant (" + 
                (plantPositions.size() - unplantedPositions.size()) + " already planted)");
            
            if (unplantedPositions.isEmpty()) {
                return Results.SUCCESS();
            }
            
            // Initialize context and find herbalist tables
            NContext context = new NContext(gui);
            NArea htableArea = context.getSpecArea(Specialisation.SpecName.htable,"Trees");
            
            if (htableArea == null) {
                return Results.ERROR("No herbalist table area found. Please configure htable specialization.");
            }
            
            ArrayList<Gob> htableGobs = Finder.findGobs(htableArea, new NAlias("gfx/terobjs/htable"));
            if (htableGobs.isEmpty()) {
                return Results.ERROR("No herbalist tables found in area.");
            }
            
            ArrayList<Container> herbalistTables = new ArrayList<>();
            for (Gob htable : htableGobs) {
                Container container = new Container(htable, "Herbalist Table");
                container.initattr(Container.Space.class);
                herbalistTables.add(container);
            }
            
            NUtils.getUI().msg("Found " + herbalistTables.size() + " herbalist tables");
            
            // Phase 1: Collect and plant ready pots
            NUtils.getUI().msg("Collecting and planting ready seedlings...");
            
            int treesPlanted = 0;
            List<PlantPosition> remainingPositions = new ArrayList<>(unplantedPositions);
            
            // Try to collect and plant pots for each position
            for (PlantPosition pos : new ArrayList<>(remainingPositions)) {
                // Check if pot for this tree type is ready on any table
                boolean potCollected = false;
                for (Container table : herbalistTables)
                {
                    Container.Space spaceAttr = table.getattr(Container.Space.class);
                    if (spaceAttr != null && spaceAttr.isReady())
                    {
                        // If table is completely empty, skip detailed check
                        if (spaceAttr.isEmpty())
                        {
                            continue;
                        }
                    }

                    Results collectResult = collectReadyPotFromTable(gui, pos.treeType, table);
                    if (collectResult.IsSuccess())
                    {
                        potCollected = true;
                        break;
                    }
                }
                
                if (potCollected) {
                    // Plant the tree (pot is now in inventory)
                    Results plantResult = plantTreeFromPot(gui, pos.worldPos, pos.treeType);
                    if (plantResult.IsSuccess()) {
                        treesPlanted++;
                        remainingPositions.remove(pos);
                        
                        // Drop empty pot after planting
                        new FreeInventory2(context).run(gui);
                        
                        if (treesPlanted % 5 == 0) {
                            NUtils.getUI().msg("Planted " + treesPlanted + " trees...");
                        }
                    }
                }
            }
            
            if (treesPlanted > 0) {
                NUtils.getUI().msg("Planted " + treesPlanted + " trees from ready pots.");
            }
            
            // Phase 2: Prepare seedlings for remaining positions
            if (!remainingPositions.isEmpty()) {
                NUtils.getUI().msg("Preparing seedlings for remaining " + remainingPositions.size() + " trees...");
                
                int seedlingsPrepared = 0;
                for (PlantPosition pos : remainingPositions) {
                    // Check if there's space on any table
                    Container availableTable = findTableWithSpace(gui, herbalistTables);
                    if (availableTable == null) {
                        NUtils.getUI().msg("No more space on herbalist tables. Prepared " + seedlingsPrepared + " seedlings.");
                        break;
                    }
                    
                    Results seedlingResult = prepareSeedling(gui, context, pos.treeType, availableTable);
                    if (seedlingResult.IsSuccess()) {
                        seedlingsPrepared++;
                        if (seedlingsPrepared % 5 == 0) {
                            NUtils.getUI().msg("Prepared " + seedlingsPrepared + "/" + remainingPositions.size() + " seedlings...");
                        }
                    } else {
                        NUtils.getUI().msg("Failed to prepare seedling");
                        break;
                    }
                }
                
                NUtils.getUI().msg("Seedling preparation completed! Prepared " + seedlingsPrepared + " seedlings.");
                NUtils.getUI().msg("These seedlings will mature over time. Re-run the bot later to plant them.");
            }
            
            NUtils.getUI().msg("Job completed! Planted " + treesPlanted + " trees total.");
            return Results.SUCCESS();
            
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Planting failed: " + e.getMessage());
        }
    }
    
    private Container findTableWithSpace(NGameUI gui, ArrayList<Container> tables) throws InterruptedException {
        for (Container table : tables) {
            Gob tableGob = Finder.findGob(table.gobid);
            if (tableGob == null) continue;
            Container.Space spaceAttr = table.getattr(Container.Space.class);
            if (spaceAttr != null && spaceAttr.isReady()) {
                // If table is completely empty, skip detailed check
                if (spaceAttr.isEmpty()) {
                    return table;
                }
            }
            
            new PathFinder(tableGob).run(gui);
            new OpenTargetContainer(table).run(gui);
            
            int freeSpace = gui.getInventory(table.cap).getFreeSpace();
            
            new CloseTargetContainer(table).run(gui);
            
            if (freeSpace > 0) {
                return table;
            }
        }
        return null;
    }
    
    private Results getSeedFromLogistics(NGameUI gui, NContext context, String treeType) throws InterruptedException {
        // Convert tree type from minimap path to regular path
        String treePath = treeType.replace("/mm/", "/");
        
        // Get seed name for this tree
        String seedName = VSpec.getSeedForTree(treePath);
        if (seedName == null) {
            return Results.ERROR("No seed found for tree: " + treePath);
        }
        
        NUtils.getUI().msg("Getting seed: " + seedName + " for tree: " + treePath);
        
        // Create NAlias with exact match using lowercase comparison
        NAlias exactSeedAlias = new NAlias(seedName);
        
        // Check if high quality seed already exists in inventory (exact match)
        ArrayList<WItem> existingSeeds = gui.getInventory().getItems(exactSeedAlias, NInventory.QualityType.High);
        if (!existingSeeds.isEmpty()) {
            // Verify exact name match (not substring)
            for (WItem item : existingSeeds) {
                if (item.item instanceof NGItem) {
                    String itemName = ((NGItem) item.item).name();
                    if (itemName.equalsIgnoreCase(seedName)) {
                        NUtils.getUI().msg("High quality seed already in inventory: " + seedName);
                        return Results.SUCCESS();
                    }
                }
            }
        }
        
        // Add seed to context so TakeItems2 knows where to look for it
        context.addInItem(seedName, null);
        
        // Take 1 high quality seed from logistics with exact name match
        TakeItems2 takeSeed = new TakeItems2(context, seedName, 1, NInventory.QualityType.High);
        takeSeed.exactMatch = true;
        Results takeResult = takeSeed.run(gui);
        if (!takeResult.IsSuccess()) {
            return Results.ERROR("Failed to get seed: " + seedName);
        }
        
        // Check that we actually have the seed with exact name match
        ArrayList<WItem> seeds = gui.getInventory().getItems(exactSeedAlias, NInventory.QualityType.High);
        boolean foundExactMatch = false;
        for (WItem item : seeds) {
            if (item.item instanceof NGItem) {
                String itemName = ((NGItem) item.item).name();
                if (itemName.equalsIgnoreCase(seedName)) {
                    foundExactMatch = true;
                    break;
                }
            }
        }
        
        if (!foundExactMatch) {
            return Results.ERROR("Exact seed not found in inventory after taking: " + seedName);
        }
        
        NUtils.getUI().msg("Successfully got high quality seed: " + seedName);
        return Results.SUCCESS();
    }
    
    private Results getGardenPot(NGameUI gui, NContext context) throws InterruptedException {
        String potName = "Treeplanter's Pot";
        
        NUtils.getUI().msg("Getting garden pot...");
        
        // Check if high quality pot already exists in inventory
        ArrayList<WItem> existingPots = gui.getInventory().getItems(new NAlias(potName), NInventory.QualityType.High);
        if (!existingPots.isEmpty()) {
            NUtils.getUI().msg("High quality treeplanter's pot already in inventory");
            return Results.SUCCESS();
        }
        
        // Add pot to context so TakeItems2 knows where to look for it
        context.addInItem(potName, null);
        
        // Take 1 high quality pot from logistics
        Results takeResult = new TakeItems2(context, potName, 1, NInventory.QualityType.High).run(gui);
        if (!takeResult.IsSuccess()) {
            return Results.ERROR("Failed to get treeplanter's pot");
        }
        
        // Check that we actually have the pot
        ArrayList<WItem> pots = gui.getInventory().getItems(new NAlias(potName), NInventory.QualityType.High);
        if (pots.isEmpty()) {
            return Results.ERROR("High quality treeplanter's pot not found in inventory after taking");
        }
        
        NUtils.getUI().msg("Successfully got high quality treeplanter's pot");
        return Results.SUCCESS();
    }
    
    private Results getSoilFromZone(NGameUI gui, NContext context, int amount) throws InterruptedException {
        // Check soil area
        NArea soilArea = context.getSpecArea(Specialisation.SpecName.soilForTrees);
        if (soilArea == null) {
            return Results.ERROR("No soil zone found. Please configure 'Soil for Trees' specialization.");
        }
        
        // Find soil piles in the area
        ArrayList<Gob> soilPiles = Finder.findGobs(soilArea, new NAlias("gfx/terobjs/stockpile-soil"));
        if (soilPiles.isEmpty()) {
            return Results.ERROR("No soil piles found in soil zone.");
        }
        
        // Navigate to the nearest pile
        Gob nearestPile = soilPiles.get(0);
        new PathFinder(nearestPile).run(gui);
        
        // Open stockpile and take soil
        new OpenTargetContainer("Stockpile", nearestPile).run(gui);
        
        TakeItemsFromPile takeFromPile = new TakeItemsFromPile(nearestPile, gui.getStockpile(), amount);
        takeFromPile.run(gui);
        
        new CloseTargetWindow(gui.getWindow("Stockpile")).run(gui);
        
        // Verify we got the soil
        ArrayList<WItem> soil = gui.getInventory().getItems(new NAlias("Soil", "Mulch"));
        if (soil.isEmpty()) {
            return Results.ERROR("Failed to get soil from pile");
        }
        
        NUtils.getUI().msg("Successfully got " + amount + " soil");
        return Results.SUCCESS();
    }
    
    private Results getWaterFromBarrel(NGameUI gui, NContext context) throws InterruptedException {
        // Check water area
        NArea waterArea = context.getSpecArea(Specialisation.SpecName.waterForTrees);
        if (waterArea == null) {
            return Results.ERROR("No water zone found. Please configure 'Water for Trees' specialization.");
        }
        
        // Find barrels in the area
        ArrayList<Gob> barrels = Finder.findGobs(waterArea, new NAlias("barrel"));
        if (barrels.isEmpty()) {
            return Results.ERROR("No barrels found in water zone.");
        }
        
        // Find barrel with water
        Gob waterBarrel = null;
        for (Gob barrel : barrels) {
            if (NUtils.barrelHasContent(barrel) && NParser.checkName(NUtils.getContentsOfBarrel(barrel), "water")) {
                waterBarrel = barrel;
                break;
            }
        }
        
        if (waterBarrel == null) {
            return Results.ERROR("No barrels with water found in water zone.");
        }
        
        // Navigate to barrel
        new PathFinder(waterBarrel).run(gui);
        
        NUtils.getUI().msg("Found barrel with water");
        return Results.SUCCESS();
    }
    
    private Results fillPotWithSoilAndWater(NGameUI gui, NContext context, Gob waterBarrel) throws InterruptedException {
        // Get the high quality pot
        ArrayList<WItem> pots = gui.getInventory().getItems(new NAlias("Treeplanter's Pot"), NInventory.QualityType.High);
        if (pots.isEmpty()) {
            return Results.ERROR("High quality garden pot not in inventory");
        }
        WItem pot = pots.get(0);
        
        // Get soil
        NAlias soil = new NAlias("Soil", "Mulch");
        ArrayList<WItem> soilItems = gui.getInventory().getItems(soil);
        if (soilItems.size() < 4) {
            return Results.ERROR("Not enough soil (need 4, have " + soilItems.size() + ")");
        }
        
        // Fill pot with 4 soil
        NUtils.getUI().msg("Filling pot with soil...");
        for (int i = 0; i < 4; i++) {
            WItem item = gui.getInventory().getItem(soil);
            if(item!=null)
            {
                NUtils.takeItemToHand(item);
                pot.item.wdgmsg("itemact", 0);
                NUtils.getUI().core.addTask(new HandIsFree(gui.getInventory()));
            }
            else
            {
                return Results.ERROR("No soil (need 4, have " + soilItems.size() + ")");
            }
        }
        
        // Fill pot with water from barrel
        NUtils.getUI().msg("Filling pot with water from barrel...");
        NUtils.takeItemToHand(pot);
        NUtils.activateItem(waterBarrel);
        // Wait for pot to be filled with 1 liter of water
        NUtils.getUI().core.addTask(new WaitPotFilled(NUtils.getGameUI().vhand, 1.0));
        // Put pot back to inventory
        NUtils.dropToInv();
        NUtils.getUI().core.addTask(new HandIsFree(gui.getInventory()));
        
        // Verify pot has water by checking text
        ArrayList<WItem> filledPots = gui.getInventory().getItems(new NAlias("Treeplanter's Pot"), NInventory.QualityType.High);
        if (filledPots.isEmpty()) {
            return Results.ERROR("High quality pot disappeared after filling");
        }
        
        NUtils.getUI().msg("Pot filled with soil and water");
        
        NUtils.getUI().msg("Successfully filled pot with soil and water");
        return Results.SUCCESS();
    }
    
    private Results putSeedInPot(NGameUI gui, NContext context, String treeType) throws InterruptedException {
        // Convert tree type from minimap path to regular path
        String treePath = treeType.replace("/mm/", "/");
        
        // Get seed name for this tree
        String seedName = VSpec.getSeedForTree(treePath);
        if (seedName == null) {
            return Results.ERROR("No seed found for tree: " + treePath);
        }
        
        // Get the high quality seed with exact name match
        ArrayList<WItem> seeds = gui.getInventory().getItems(new NAlias(seedName), NInventory.QualityType.High);
        WItem seed = null;
        for (WItem item : seeds) {
            if (item.item instanceof NGItem) {
                String itemName = ((NGItem) item.item).name();
                if (itemName.equals(seedName)) {
                    seed = item;
                    break;
                }
            }
        }
        
        if (seed == null) {
            return Results.ERROR("Exact seed not found in inventory: " + seedName);
        }
        
        // Get the high quality pot
        ArrayList<WItem> pots = gui.getInventory().getItems(new NAlias("Treeplanter's Pot"), NInventory.QualityType.High);
        if (pots.isEmpty()) {
            return Results.ERROR("High quality pot not found in inventory");
        }
        
        WItem pot = pots.get(0);
        
        NUtils.getUI().msg("Putting seed in pot...");
        
        // Take seed in hand and click on pot (same as soil)
        NUtils.takeItemToHand(seed);
        pot.item.wdgmsg("itemact", 0);
        NUtils.getUI().core.addTask(new HandIsFree(gui.getInventory()));
        
        NUtils.getUI().msg("Seed placed in pot");
        return Results.SUCCESS();
    }
    
    /**
     * Converts tree resource path to short display name.
     * E.g., "gfx/terobjs/mm/trees/pine" -> "Pine"
     */
    private String getShortTreeName(String treePath) {
        Map<String, String> treeNames = new HashMap<>();
        treeNames.put("gfx/terobjs/mm/trees/acacia", "Acacia");
        treeNames.put("gfx/terobjs/mm/trees/alder", "Alder");
        treeNames.put("gfx/terobjs/mm/trees/almondtree", "Almond Tree");
        treeNames.put("gfx/terobjs/mm/trees/appletree", "Apple Tree");
        treeNames.put("gfx/terobjs/mm/trees/appletreegreen", "Green Apple Tree");
        treeNames.put("gfx/terobjs/mm/trees/ash", "Ash");
        treeNames.put("gfx/terobjs/mm/trees/aspen", "Aspen");
        treeNames.put("gfx/terobjs/mm/trees/baywillow", "Bay Willow");
        treeNames.put("gfx/terobjs/mm/trees/beech", "Beech");
        treeNames.put("gfx/terobjs/mm/trees/birch", "Birch");
        treeNames.put("gfx/terobjs/mm/trees/birdcherrytree", "Bird Cherry Tree");
        treeNames.put("gfx/terobjs/mm/trees/blackpine", "Black Pine");
        treeNames.put("gfx/terobjs/mm/trees/blackpoplar", "Black Poplar");
        treeNames.put("gfx/terobjs/mm/trees/buckthorn", "Buckthorn");
        treeNames.put("gfx/terobjs/mm/trees/carobtree", "Carob Tree");
        treeNames.put("gfx/terobjs/mm/trees/cedar", "Cedar");
        treeNames.put("gfx/terobjs/mm/trees/charredtree", "Charred Tree");
        treeNames.put("gfx/terobjs/mm/trees/chastetree", "Chaste Tree");
        treeNames.put("gfx/terobjs/mm/trees/checkertree", "Checker Tree");
        treeNames.put("gfx/terobjs/mm/trees/cherry", "Cherry Tree");
        treeNames.put("gfx/terobjs/mm/trees/chestnuttree", "Chestnut Tree");
        treeNames.put("gfx/terobjs/mm/trees/conkertree", "Conker Tree");
        treeNames.put("gfx/terobjs/mm/trees/corkoak", "Corkoak");
        treeNames.put("gfx/terobjs/mm/trees/crabappletree", "Crabapple Tree");
        treeNames.put("gfx/terobjs/mm/trees/cypress", "Cypress");
        treeNames.put("gfx/terobjs/mm/trees/dogwood", "Dogwood");
        treeNames.put("gfx/terobjs/mm/trees/dwarfpine", "Dwarf Pine");
        treeNames.put("gfx/terobjs/mm/trees/elm", "Elm");
        treeNames.put("gfx/terobjs/mm/trees/figtree", "Fig Tree");
        treeNames.put("gfx/terobjs/mm/trees/fir", "Fir");
        treeNames.put("gfx/terobjs/mm/trees/gloomcap", "Gloomcap");
        treeNames.put("gfx/terobjs/mm/trees/gnomeshat", "Gnome's Hat");
        treeNames.put("gfx/terobjs/mm/trees/goldenchain", "Goldenchain");
        treeNames.put("gfx/terobjs/mm/trees/grayalder", "Gray Alder");
        treeNames.put("gfx/terobjs/mm/trees/hazel", "Hazel");
        treeNames.put("gfx/terobjs/mm/trees/hornbeam", "Hornbeam");
        treeNames.put("gfx/terobjs/mm/trees/juniper", "Juniper");
        treeNames.put("gfx/terobjs/mm/trees/kingsoak", "King's Oak");
        treeNames.put("gfx/terobjs/mm/trees/larch", "Larch");
        treeNames.put("gfx/terobjs/mm/trees/laurel", "Laurel");
        treeNames.put("gfx/terobjs/mm/trees/lemontree", "Lemon Tree");
        treeNames.put("gfx/terobjs/mm/trees/linden", "Linden");
        treeNames.put("gfx/terobjs/mm/trees/lotetree", "Lote Tree");
        treeNames.put("gfx/terobjs/mm/trees/maple", "Maple");
        treeNames.put("gfx/terobjs/mm/trees/mayflower", "Mayflower");
        treeNames.put("gfx/terobjs/mm/trees/medlartree", "Medlar Tree");
        treeNames.put("gfx/terobjs/mm/trees/moundtree", "Mound Tree");
        treeNames.put("gfx/terobjs/mm/trees/mulberry", "Mulberry Tree");
        treeNames.put("gfx/terobjs/mm/trees/oak", "Oak");
        treeNames.put("gfx/terobjs/mm/trees/olivetree", "Olive Tree");
        treeNames.put("gfx/terobjs/mm/trees/orangetree", "Orange Tree");
        treeNames.put("gfx/terobjs/mm/trees/osier", "Osier");
        treeNames.put("gfx/terobjs/mm/trees/peartree", "Pear Tree");
        treeNames.put("gfx/terobjs/mm/trees/persimmontree", "Persimmon Tree");
        treeNames.put("gfx/terobjs/mm/trees/pine", "Pine");
        treeNames.put("gfx/terobjs/mm/trees/planetree", "Plane Tree");
        treeNames.put("gfx/terobjs/mm/trees/plumtree", "Plum Tree");
        treeNames.put("gfx/terobjs/mm/trees/poplar", "Poplar");
        treeNames.put("gfx/terobjs/mm/trees/quincetree", "Quince Tree");
        treeNames.put("gfx/terobjs/mm/trees/rowan", "Rowan");
        treeNames.put("gfx/terobjs/mm/trees/sallow", "Sallow");
        treeNames.put("gfx/terobjs/mm/trees/silverfir", "Silverfir");
        treeNames.put("gfx/terobjs/mm/trees/sorbtree", "Sorb Tree");
        treeNames.put("gfx/terobjs/mm/trees/spruce", "Spruce");
        treeNames.put("gfx/terobjs/mm/trees/stonepine", "Stone Pine");
        treeNames.put("gfx/terobjs/mm/trees/strawberrytree", "Strawberry Tree");
        treeNames.put("gfx/terobjs/mm/trees/sweetgum", "Sweetgum");
        treeNames.put("gfx/terobjs/mm/trees/sycamore", "Sycamore");
        treeNames.put("gfx/terobjs/mm/trees/tamarisk", "Tamarisk");
        treeNames.put("gfx/terobjs/mm/trees/terebinth", "Terebinth");
        treeNames.put("gfx/terobjs/mm/trees/towercap", "Towercap");
        treeNames.put("gfx/terobjs/mm/trees/treeheath", "Tree Heath");
        treeNames.put("gfx/terobjs/mm/trees/trombonechantrelle", "Trombone Chantrelle");
        treeNames.put("gfx/terobjs/mm/trees/walnuttree", "Walnut Tree");
        treeNames.put("gfx/terobjs/mm/trees/wartybirch", "Warty Birch");
        treeNames.put("gfx/terobjs/mm/trees/whitebeam", "Whitebeam");
        treeNames.put("gfx/terobjs/mm/trees/willow", "Willow");
        treeNames.put("gfx/terobjs/mm/trees/wychelm", "Wych Elm");
        treeNames.put("gfx/terobjs/mm/trees/yew", "Yew");
        treeNames.put("gfx/terobjs/mm/trees/zelkova", "Zelkova");
        
        String shortName = treeNames.get(treePath);
        if (shortName != null) {
            return shortName;
        }
        
        // Fallback: extract last part from path and capitalize
        String[] parts = treePath.split("/");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            return Character.toUpperCase(last.charAt(0)) + last.substring(1);
        }
        
        return treePath;
    }
    
    /**
     * Checks if pot is ready (no meter overlay and has tree name in rawinfo)
     */
    private boolean isPotReady(WItem pot) {
        if (!(pot.item instanceof NGItem)) {
            return false;
        }
        
        NGItem ngPot = (NGItem) pot.item;
        
        // Check if pot has meter (still growing)
        if (ngPot.meter > 0) {
            return false;
        }
        
        // Check if pot has tree name in rawinfo
        String treeName = getTreeNameFromPot(pot);
        return treeName != null;
    }
    
    /**
     * Extracts tree name from pot's rawinfo
     */
    private String getTreeNameFromPot(WItem pot) {
        if (!(pot.item instanceof NGItem)) {
            return null;
        }
        
        NGItem ngPot = (NGItem) pot.item;
        
        // Access rawinfo via reflection or directly
        try {
            java.lang.reflect.Field rawinfoField = GItem.class.getDeclaredField("rawinfo");
            rawinfoField.setAccessible(true);
            ItemInfo.Raw rawinfo = (ItemInfo.Raw) rawinfoField.get(ngPot);
            
            if (rawinfo == null || rawinfo.data == null) {
                return null;
            }
            
            // Parse rawinfo.data structure: data[1] is Object[], data[1][1] is tree name
            for (Object o : rawinfo.data) {
                if (o instanceof Object[]) {
                    Object[] arr = (Object[]) o;
                    if (arr.length >= 2 && arr[1] instanceof String) {
                        String treeName = (String) arr[1];
                        // Check if this is a sprouted tree
                        if (treeName.startsWith("Sprouted")) {
                            // Remove "Sprouted " prefix and return just the tree name
                            return treeName.substring("Sprouted ".length());
                        }
                    }
                }
            }
        } catch (Exception e) {
            NUtils.getUI().msg("Error accessing pot rawinfo: " + e.getMessage());
        }
        
        return null;
    }
    
    private Results placePotOnTable(NGameUI gui, Container targetTable) throws InterruptedException {
        // Get the high quality pot
        ArrayList<WItem> pots = gui.getInventory().getItems(new NAlias("Treeplanter's Pot"), NInventory.QualityType.High);
        if (pots.isEmpty()) {
            return Results.ERROR("High quality pot not found in inventory");
        }
        
        NUtils.getUI().msg("Placing pot on herbalist table...");
        
        // Navigate to the table
        Gob tableGob = Finder.findGob(targetTable.gobid);
        if (tableGob == null) {
            return Results.ERROR("Herbalist table not found");
        }
        
        new PathFinder(tableGob).run(gui);
        
        // Open the table
        new OpenTargetContainer(targetTable).run(gui);
        
        // Transfer pot to table
        WItem pot = pots.get(0);
        NUtils.takeItemToHand(pot);
        
        // Drop pot into table inventory
        NInventory tableInv = gui.getInventory(targetTable.cap);
        if (tableInv == null) {
            new CloseTargetContainer(targetTable).run(gui);
            return Results.ERROR("Could not access table inventory");
        }
        
        NUtils.dropToInv(tableInv);
        NUtils.getUI().core.addTask(new HandIsFree(gui.getInventory()));
        
        // Close the table
        new CloseTargetContainer(targetTable).run(gui);
        
        NUtils.getUI().msg("Pot placed on herbalist table");
        return Results.SUCCESS();
    }
    
    private Results prepareSeedling(NGameUI gui, NContext context, String treeType, Container targetTable) throws InterruptedException {
        // 1. Get seed from logistics
        Results seedResult = getSeedFromLogistics(gui, context, treeType);
        if (!seedResult.IsSuccess()) {
            return seedResult;
        }
        
        // 2. Get pot from gardenpot zone
        Results potResult = getGardenPot(gui, context);
        if (!potResult.IsSuccess()) {
            return potResult;
        }
        
        // 3. Get 4 soil from soil zone
        Results soilResult = getSoilFromZone(gui, context, 4);
        if (!soilResult.IsSuccess()) {
            return soilResult;
        }
        
        // 4. Find barrel with water
        NArea waterArea = context.getSpecArea(Specialisation.SpecName.waterForTrees);
        if (waterArea == null) {
            return Results.ERROR("No water zone found. Please configure 'Water for Trees' specialization.");
        }
        
        ArrayList<Gob> barrels = Finder.findGobs(waterArea, new NAlias("barrel"));
        Gob waterBarrel = null;
        for (Gob barrel : barrels) {
            if (NUtils.barrelHasContent(barrel) && NParser.checkName(NUtils.getContentsOfBarrel(barrel), "water")) {
                waterBarrel = barrel;
                break;
            }
        }
        
        if (waterBarrel == null) {
            return Results.ERROR("No barrels with water found in water zone.");
        }
        
        // 5-6. Fill pot with soil and water
        Results fillResult = fillPotWithSoilAndWater(gui, context, waterBarrel);
        if (!fillResult.IsSuccess()) {
            return fillResult;
        }
        
        // 7. Put seed in pot
        Results seedInPotResult = putSeedInPot(gui, context, treeType);
        if (!seedInPotResult.IsSuccess()) {
            return seedInPotResult;
        }
        
        // 8. Place pot on herbalist table
        Results tablePlaceResult = placePotOnTable(gui, targetTable);
        if (!tablePlaceResult.IsSuccess()) {
            return tablePlaceResult;
        }
        
        return Results.SUCCESS();
    }

    /**
     * Collects a ready pot of specified tree type from herbalist table
     */
    private Results collectReadyPotFromTable(NGameUI gui, String treeType, Container table) throws InterruptedException {
        // Navigate to table
        Gob tableGob = Finder.findGob(table.gobid);
        if (tableGob == null) {
            return Results.ERROR("Herbalist table not found");
        }
        
        new PathFinder(tableGob).run(gui);
        new OpenTargetContainer(table).run(gui);
        
        NInventory tableInv = gui.getInventory(table.cap);
        if (tableInv == null) {
            new CloseTargetContainer(table).run(gui);
            return Results.ERROR("Could not access table inventory");
        }
        
        // Find ready pots with specified tree type
        ArrayList<WItem> pots = tableInv.getItems(new NAlias("Treeplanter's Pot"));
        WItem readyPot = null;
        
        // Convert blueprint tree path to short name for comparison
        String shortTreeName = getShortTreeName(treeType);
        
        for (WItem pot : pots) {
            if (isPotReady(pot)) {
                String potTreeName = getTreeNameFromPot(pot);
                if (potTreeName != null && potTreeName.equals(shortTreeName)) {
                    readyPot = pot;
                    break;
                }
            }
        }
        
        if (readyPot == null) {
            new CloseTargetContainer(table).run(gui);
            return Results.ERROR("No ready pot for tree type: " + treeType);
        }
        
        // Take pot to inventory
        NUtils.takeItemToHand(readyPot);
        NUtils.dropToInv(gui.getInventory());
        NUtils.getUI().core.addTask(new HandIsFree(gui.getInventory()));
        
        new CloseTargetContainer(table).run(gui);
        
        NUtils.getUI().msg("Collected ready pot for: " + treeType);
        return Results.SUCCESS();
    }
    
    /**
     * Plants a tree from pot at specified position
     */
    private Results plantTreeFromPot(NGameUI gui, Coord2d position, String treeType) throws InterruptedException {
        try {
            // Check if we have the pot in inventory
            ArrayList<WItem> pots = gui.getInventory().getItems(new NAlias("Treeplanter's Pot"));
            WItem pot = null;
            
            // Convert blueprint tree path to short name for comparison
            String shortTreeName = getShortTreeName(treeType);
            
            for (WItem p : pots) {
                String potTreeName = getTreeNameFromPot(p);
                if (potTreeName != null && potTreeName.equals(shortTreeName)) {
                    pot = p;
                    break;
                }
            }
            
            if (pot == null) {
                return Results.ERROR("No pot for tree type: " + treeType);
            }
            
            // Right-click on pot to open context menu and select "Plant tree"
            new SelectFlowerAction("Plant tree", pot).run(gui);
            
            // Wait for plob to appear
            NUtils.addTask(new WaitPlob());
            
            // Get the plob and place it at the ghost position
            MapView.Plob plob = gui.map.placing.get();
            if (plob == null) {
                return Results.ERROR("Failed to get plob for tree planting");
            }
            
            // Place the tree at the exact ghost position
            gui.map.wdgmsg("place", position.floor(OCache.posres), 0, 1, 0);
            
            // Wait for planting to complete
            NUtils.addTask(new WaitPose(NUtils.player(), "gfx/borka/shoveldig"));
            NUtils.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));
            
            return Results.SUCCESS();
            
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Failed to plant tree: " + e.getMessage());
        }
    }
    
}
