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
            if (prop.gridId != null && prop.tileCoord != null && prop.blueprintName != null) {
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
            
            // Prepare seedlings on tables
            int seedlingsPrepared = 0;
            for (PlantPosition pos : unplantedPositions) {
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
                        NUtils.getUI().msg("Prepared " + seedlingsPrepared + "/" + unplantedPositions.size() + " seedlings...");
                    }
                } else {
                    NUtils.getUI().msg("Failed to prepare seedling: ");
                    break;
                }
            }
            
            NUtils.getUI().msg("Seedling preparation completed! Prepared " + seedlingsPrepared + " seedlings.");
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
        
        // Check if seed already exists in inventory
        ArrayList<WItem> existingSeeds = gui.getInventory().getItems(new NAlias(seedName));
        if (!existingSeeds.isEmpty()) {
            NUtils.getUI().msg("Seed already in inventory: " + seedName);
            return Results.SUCCESS();
        }
        
        // Add seed to context so TakeItems2 knows where to look for it
        context.addInItem(seedName, null);
        
        // Take 1 seed from logistics
        Results takeResult = new TakeItems2(context, seedName, 1).run(gui);
        if (!takeResult.IsSuccess()) {
            return Results.ERROR("Failed to get seed: " + seedName);
        }
        
        // Check that we actually have the seed
        ArrayList<WItem> seeds = gui.getInventory().getItems(new NAlias(seedName));
        if (seeds.isEmpty()) {
            return Results.ERROR("Seed not found in inventory after taking: " + seedName);
        }
        
        NUtils.getUI().msg("Successfully got seed: " + seedName);
        return Results.SUCCESS();
    }
    
    private Results prepareSeedling(NGameUI gui, NContext context, String treeType, Container targetTable) throws InterruptedException {
        // 1. Get seed from logistics
        Results seedResult = getSeedFromLogistics(gui, context, treeType);
        if (!seedResult.IsSuccess()) {
            return seedResult;
        }
        
        // TODO: Implement full seedling preparation:
        // 2. Get pot from gardenpot zone
        // 3. Get 4 soil from soil zone  
        // 4. Get water from barrel
        // 5. Fill pot with soil
        // 6. Fill pot with water
        // 7. Put seed in pot
        // 8. Place pot on herbalist table
        
        return Results.SUCCESS();
    }

    private Map<Coord, ArrayList<PlantPosition>> groupPositionsByTile(List<PlantPosition> positions) {
        Map<Coord, ArrayList<PlantPosition>> tileGroups = new HashMap<>();
        
        for (PlantPosition pos : positions) {
            Coord tile = new Coord(
                (int)(pos.worldPos.x / MCache.tilesz.x),
                (int)(pos.worldPos.y / MCache.tilesz.y)
            );
            
            tileGroups.computeIfAbsent(tile, k -> new ArrayList<>()).add(pos);
        }
        
        return tileGroups;
    }

    private Results plantTreeAtPosition(NGameUI gui, Coord2d position) throws InterruptedException {
        try {
            ArrayList<WItem> seeds = gui.getInventory().getItems(TREE_SEEDS);
            if (seeds.isEmpty()) {
                return Results.ERROR("No more seeds available");
            }
            
            WItem seed = seeds.get(0);
            NUtils.takeItemToHand(seed);
            
            NUtils.activateItem(position);
            
            NFlowerMenu fm = NUtils.findFlowerMenu();
            if (fm != null && fm.nopts.length > 0) {
                if (fm.chooseOpt("Plant tree")) {
                    NUtils.addTask(new NFlowerMenuIsClosed());
                } else {
                    return Results.ERROR("No 'Plant tree' option found");
                }
            } else {
                return Results.ERROR("No flower menu appeared");
            }
            
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
