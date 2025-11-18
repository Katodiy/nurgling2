package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.conf.NBlueprintPlanterProp;
import nurgling.overlays.TreeGhostPreview;
import nurgling.tasks.*;
import nurgling.tools.*;
import org.json.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BlueprintTreePlanter implements Action {

    private static final NAlias TREE_SEEDS = VSpec.getAllPlantableSeeds();
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
                        NUtils.getUI().msg("Blueprint cleared.");
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
            
            NUtils.getUI().msg("Move blueprint and LEFT CLICK to place (" + blueprintData.trees.size() + " trees, " + blueprintData.width + "x" + blueprintData.height + " area)");
            
            WaitBlueprintPlacement waitPlacement = new WaitBlueprintPlacement(blueprintPlob);
            NUtils.getUI().core.addTask(waitPlacement);
            
            if (!blueprintPlob.isPlaced()) {
                cleanupBlueprint();
                return Results.FAIL();
            }
            
            Coord2d blueprintOrigin = blueprintPlob.getPosition();
            
            plantPositions.clear();
            
            for (Map.Entry<Coord, String> entry : blueprintData.trees.entrySet()) {
                Coord gridPos = entry.getKey();
                String treeType = entry.getValue();
                
                Coord2d worldPos = blueprintOrigin.add(
                    gridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                    gridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
                );
                
                plantPositions.add(new PlantPosition(worldPos, treeType));
            }
            
            // Save placed position (grid ID + tile coords)
            Coord tilePos = blueprintOrigin.floor(MCache.tilesz);
            Coord gridCoord = tilePos.div(MCache.cmaps);
            Coord tileInGrid = tilePos.mod(MCache.cmaps);
            MCache.Grid grid = gui.map.glob.map.grids.get(gridCoord);
            
            if (grid != null) {
                prop.blueprintName = blueprintName;
                prop.gridId = grid.id;
                prop.tileCoord = tileInGrid;
                NBlueprintPlanterProp.set(prop);
            }
            
            NUtils.getUI().msg("Blueprint placed: " + plantPositions.size() + " trees. Click 'Start Planting' to begin.");
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
            plantPositions.clear();
            for (Map.Entry<Coord, String> entry : blueprintData.trees.entrySet()) {
                Coord gridPos = entry.getKey();
                String treeType = entry.getValue();
                
                Coord2d treeWorldPos = worldPos.add(
                    gridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                    gridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
                );
                
                plantPositions.add(new PlantPosition(treeWorldPos, treeType));
            }
            
            NUtils.getUI().msg("Blueprint placed from saved position.");
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
            
            ArrayList<WItem> seeds = gui.getInventory().getItems(TREE_SEEDS);
            if (seeds.isEmpty()) {
                return Results.ERROR("No tree seeds found in inventory");
            }
            
            NUtils.getUI().msg("Starting to plant " + plantPositions.size() + " trees from blueprint...");
            
            int totalPlanted = 0;
            Map<Coord, ArrayList<PlantPosition>> tileGroups = groupPositionsByTile(plantPositions);
            
            for (Map.Entry<Coord, ArrayList<PlantPosition>> entry : tileGroups.entrySet()) {
                ArrayList<PlantPosition> positionsOnTile = entry.getValue();
                
                seeds = gui.getInventory().getItems(TREE_SEEDS);
                if (seeds.isEmpty()) {
                    NUtils.getUI().msg("No more seeds. Planted " + totalPlanted + " trees.");
                    return Results.SUCCESS();
                }
                
                if (!positionsOnTile.isEmpty()) {
                    new PathFinder(positionsOnTile.get(0).worldPos).run(gui);
                    
                    for (PlantPosition position : positionsOnTile) {
                        Results plantResult = plantTreeAtPosition(gui, position.worldPos);
                        if (plantResult.IsSuccess()) {
                            totalPlanted++;
                            if (totalPlanted % 5 == 0) {
                                NUtils.getUI().msg("Planted " + totalPlanted + "/" + plantPositions.size() + " trees...");
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
            
            NUtils.getUI().msg("Blueprint planting completed! Planted " + totalPlanted + " trees.");
            return Results.SUCCESS();
            
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Planting failed: " + e.getMessage());
        }
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
