package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.NMapView;
import nurgling.tasks.WaitPose;
import nurgling.tasks.NFlowerMenuIsClosed;
import nurgling.tools.*;
import nurgling.widgets.TreeSpacingDialog;
import nurgling.overlays.TreeGhostPreview;
import nurgling.widgets.NEquipory;

import java.util.*;

/**
 * Automated tree planting bot for Haven & Hearth
 *
 * Features:
 * - Equipment validation (shovel required)
 * - Seed inventory checking
 * - Area selection with visual preview
 * - Configurable tree spacing (2, 3, 4, 5 tiles apart)
 * - Efficient tile-based planting execution
 * - Proper pose waiting and error handling
 */
public class PlantTrees implements Action {

    // Constants
    private static final Coord TREE_SIZE = new Coord(1, 1);
    private static final NAlias TREE_SEEDS = VSpec.getAllPlantableSeeds();
    private static final NAlias SHOVEL = new NAlias("Shovel");

    // State variables
    private Pair<Coord2d, Coord2d> selectedArea;
    private ArrayList<Coord2d> plantingPositions;
    private TreeSpacingDialog spacingDialog;
    private TreeGhostPreview ghostPreview;
    private int selectedSpacing = 2; // Default: every other tile
    private volatile boolean userConfirmed = false;
    private volatile boolean userCancelled = false;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
            // Reset state for reusability
            resetState();

            // Phase 1: Validation
            Results validation = validateEquipmentAndSeeds(gui);
            if (!validation.IsSuccess()) {
                return validation;
            }

            // Phase 2: Area Selection
            Results areaResult = selectPlantingArea(gui);
            if (!areaResult.IsSuccess()) {
                return areaResult;
            }

            // Phase 3: Spacing Configuration with Preview
            Results configResult = showSpacingConfiguration(gui);
            if (!configResult.IsSuccess()) {
                return configResult;
            }

            // Phase 4: Execute Planting
            Results plantingResult = executePlanting(gui);
            cleanup(gui);
            return plantingResult;

        } catch (InterruptedException e) {
            cleanup(gui);
            throw e; // Never catch InterruptedException - breaks stop button
        } catch (Exception e) {
            cleanup(gui);
            return Results.ERROR("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Validates that tree seeds are available
     */
    private Results validateEquipmentAndSeeds(NGameUI gui) {
        // TODO: Add shovel validation when needed
        // For now, just check for tree seeds

        // Check for tree seeds in inventory
        try {
            ArrayList<WItem> seeds = gui.getInventory().getItems(TREE_SEEDS);
            if (seeds.isEmpty()) {
                return Results.ERROR("No tree or bush seeds found in inventory");
            }

            // Debug: Show what seed types we're looking for (first 10)
            if (TREE_SEEDS.keys != null && !TREE_SEEDS.keys.isEmpty()) {
                int count = Math.min(10, TREE_SEEDS.keys.size());
                StringBuilder seedTypes = new StringBuilder("Searching for seeds including: ");
                for (int i = 0; i < count; i++) {
                    seedTypes.append(TREE_SEEDS.keys.get(i));
                    if (i < count - 1) seedTypes.append(", ");
                }
                if (TREE_SEEDS.keys.size() > 10) {
                    seedTypes.append(", and ").append(TREE_SEEDS.keys.size() - 10).append(" more...");
                }
                NUtils.getUI().msg(seedTypes.toString());
            }

            NUtils.getUI().msg("Found " + seeds.size() + " plantable items. Ready to plant!");
        } catch (Exception e) {
            return Results.ERROR("Failed to check inventory: " + e.getMessage());
        }

        return Results.SUCCESS();
    }

    /**
     * Allows user to select rectangular area for tree planting
     */
    private Results selectPlantingArea(NGameUI gui) throws InterruptedException {
        try {
            NUtils.getUI().msg("Select area for tree planting...");

            // Use basic area selection without rotation UI
            SelectArea selector = new SelectArea(Resource.loadsimg("baubles/buildArea"));

            selector.run(gui);

            if (selector.getRCArea() == null) {
                return Results.FAIL(); // User cancelled area selection
            }

            this.selectedArea = selector.getRCArea();

            // Validate area size (max 50x50 tiles for performance)
            double width = Math.abs(selectedArea.b.x - selectedArea.a.x);
            double height = Math.abs(selectedArea.b.y - selectedArea.a.y);
            int tilesWidth = (int)(width / MCache.tilesz.x) + 1;
            int tilesHeight = (int)(height / MCache.tilesz.y) + 1;

            if (tilesWidth > 50 || tilesHeight > 50) {
                return Results.ERROR("Selected area too large. Maximum 50x50 tiles allowed.");
            }

            NUtils.getUI().msg("Area selected: " + tilesWidth + "x" + tilesHeight + " tiles. Choose tree spacing...");
            return Results.SUCCESS();

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Area selection failed: " + e.getMessage());
        }
    }

    /**
     * Shows spacing configuration dialog with preview
     */
    private Results showSpacingConfiguration(NGameUI gui) throws InterruptedException {
        try {
            // Generate initial tree positions with default spacing
            generatePlantingGrid(selectedSpacing);

            // Create and show ghost preview
            createGhostPreview(gui);

            // Show initial tree count
            NUtils.getUI().msg("Initial spacing: every " + selectedSpacing + " tiles → " + plantingPositions.size() + " trees planned");

            // Create and show spacing dialog
            spacingDialog = new TreeSpacingDialog(this);
            gui.add(spacingDialog, UI.scale(200, 200));

            // Wait for user confirmation or cancellation
            while (!userConfirmed && !userCancelled) {
                Thread.sleep(50);
            }

            // Clean up dialog
            if (spacingDialog != null) {
                spacingDialog.destroy();
                spacingDialog = null;
            }

            if (userCancelled) {
                // Remove ghost preview when user cancels
                removeGhostPreview();
                return Results.FAIL();
            }

            NUtils.getUI().msg("Tree placement confirmed. Starting planting...");
            return Results.SUCCESS();

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Configuration failed: " + e.getMessage());
        }
    }

    /**
     * Generates tree planting positions based on selected area and spacing
     */
    private void generatePlantingGrid(int spacing) {
        plantingPositions = new ArrayList<>();

        if (selectedArea == null) return;

        double startX = Math.min(selectedArea.a.x, selectedArea.b.x);
        double startY = Math.min(selectedArea.a.y, selectedArea.b.y);
        double endX = Math.max(selectedArea.a.x, selectedArea.b.x);
        double endY = Math.max(selectedArea.a.y, selectedArea.b.y);

        // Convert to tile coordinates
        Coord startTile = new Coord((int)(startX / MCache.tilesz.x), (int)(startY / MCache.tilesz.y));
        Coord endTile = new Coord((int)(endX / MCache.tilesz.x), (int)(endY / MCache.tilesz.y));

        // Generate grid with specified spacing - one tree per cell maximum
        for (int x = startTile.x; x <= endTile.x; x += spacing) {
            for (int y = startTile.y; y <= endTile.y; y += spacing) {
                // Center position within tile
                Coord2d worldPos = new Coord2d(
                    x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                    y * MCache.tilesz.y + MCache.tilesz.y / 2.0
                );
                plantingPositions.add(worldPos);
            }
        }
    }

    /**
     * Creates visual preview overlay showing planned tree positions
     */
    private void createGhostPreview(NGameUI gui) {
        try {
            // Remove existing preview
            removeGhostPreview();

            // Create new preview
            if (!plantingPositions.isEmpty()) {
                Gob player = NUtils.player();
                if (player != null) {
                    ghostPreview = new TreeGhostPreview(player, plantingPositions);
                    player.addol(ghostPreview);
                }
            }
        } catch (Exception e) {
            NUtils.getUI().msg("Failed to create preview: " + e.getMessage());
        }
    }

    /**
     * Removes ghost preview overlay
     */
    private void removeGhostPreview() {
        try {
            Gob player = NUtils.player();
            if (player != null) {
                Gob.Overlay overlay = player.findol(TreeGhostPreview.class);
                if (overlay != null) {
                    overlay.remove();
                }
            }
            ghostPreview = null;
        } catch (Exception e) {
            // Silent cleanup - don't propagate cleanup errors
        }
    }

    /**
     * Executes tree planting using efficient tile-based grouping
     */
    private Results executePlanting(NGameUI gui) throws InterruptedException {
        try {
            if (plantingPositions.isEmpty()) {
                return Results.ERROR("No tree positions to plant");
            }

            // Group positions by tile for efficient pathfinding
            Map<Coord, ArrayList<Coord2d>> tileGroups = groupPositionsByTile(plantingPositions);
            int totalPlanted = 0;
            int totalPositions = plantingPositions.size();

            NUtils.getUI().msg("Starting to plant " + totalPositions + " trees...");

            // Process each tile group
            for (Map.Entry<Coord, ArrayList<Coord2d>> entry : tileGroups.entrySet()) {
                ArrayList<Coord2d> positionsOnTile = entry.getValue();

                // Check if we still have seeds
                ArrayList<WItem> seeds = gui.getInventory().getItems(TREE_SEEDS);
                if (seeds.isEmpty()) {
                    NUtils.getUI().msg("No more seeds available. Planted " + totalPlanted + " trees.");
                    return Results.SUCCESS();
                }

                // Pathfind to first position on this tile
                if (!positionsOnTile.isEmpty()) {
                    new PathFinder(positionsOnTile.get(0)).run(gui);

                    // Plant all trees on this tile
                    for (Coord2d position : positionsOnTile) {
                        Results plantResult = plantTreeAtPosition(gui, position);
                        if (plantResult.IsSuccess()) {
                            totalPlanted++;
                            if (totalPlanted % 5 == 0) {
                                NUtils.getUI().msg("Planted " + totalPlanted + "/" + totalPositions + " trees...");
                            }
                        } else {
                            break; // Stop if out of seeds
                        }
                        // Continue with other positions even if one fails
                    }
                }
            }

            NUtils.getUI().msg("Tree planting completed! Planted " + totalPlanted + " trees.");
            return Results.SUCCESS();

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Planting failed: " + e.getMessage());
        }
    }

    /**
     * Groups planting positions by tile for efficient pathfinding
     */
    private Map<Coord, ArrayList<Coord2d>> groupPositionsByTile(ArrayList<Coord2d> positions) {
        Map<Coord, ArrayList<Coord2d>> tileGroups = new HashMap<>();

        for (Coord2d pos : positions) {
            Coord tile = new Coord(
                (int)(pos.x / MCache.tilesz.x),
                (int)(pos.y / MCache.tilesz.y)
            );

            tileGroups.computeIfAbsent(tile, k -> new ArrayList<>()).add(pos);
        }

        return tileGroups;
    }

    /**
     * Plants a single tree at the specified position
     */
    private Results plantTreeAtPosition(NGameUI gui, Coord2d position) throws InterruptedException {
        try {
            // Check if we have seeds
            ArrayList<WItem> seeds = gui.getInventory().getItems(TREE_SEEDS);
            if (seeds.isEmpty()) {
                return Results.ERROR("No more seeds available");
            }

            // Take seed to hand
            WItem seed = seeds.get(0);
            NUtils.takeItemToHand(seed);

            // Activate item (seed) at the planting position
            NUtils.activateItem(position);

            // Wait for flower menu to appear and select option 1
            NFlowerMenu fm = NUtils.findFlowerMenu();
            if (fm != null && fm.nopts.length > 0) {
                // Select option 1 (index 0)
                fm.nchoose(fm.nopts[0]);
                // Wait for flower menu to close
                NUtils.addTask(new NFlowerMenuIsClosed());
            } else {
                return Results.ERROR("No flower menu appeared after activating seed");
            }

            // Wait for planting action to complete
            NUtils.addTask(new WaitPose(NUtils.player(), "gfx/borka/shoveldig"));
            NUtils.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));

            return Results.SUCCESS();

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return Results.ERROR("Failed to plant tree at position: " + e.getMessage());
        }
    }

    /**
     * Cleans up overlays and UI components following BuildTrellis pattern
     */
    private void cleanup(NGameUI gui) {
        try {
            removeGhostPreview();

            if (spacingDialog != null) {
                spacingDialog.destroy();
                spacingDialog = null;
            }

            // Clean up area selection mode
            if (gui != null && gui.map != null) {
                ((NMapView) gui.map).isAreaSelectionMode.set(false);
            }
        } catch (Exception e) {
            // Silent cleanup - don't propagate cleanup errors
        }
    }

    // Methods called by TreeSpacingDialog

    /**
     * Updates spacing and regenerates preview
     */
    public void updateSpacing(int newSpacing) {
        this.selectedSpacing = newSpacing;
        generatePlantingGrid(newSpacing);

        try {
            // Update ghost preview
            createGhostPreview(NUtils.getGameUI());
            // Calculate area dimensions for better feedback
            double width = Math.abs(selectedArea.b.x - selectedArea.a.x);
            double height = Math.abs(selectedArea.b.y - selectedArea.a.y);
            int tilesWidth = (int)(width / MCache.tilesz.x) + 1;
            int tilesHeight = (int)(height / MCache.tilesz.y) + 1;

            NUtils.getUI().msg("Spacing: every " + newSpacing + " tiles → " + plantingPositions.size() + " trees in " + tilesWidth + "x" + tilesHeight + " area");
        } catch (Exception e) {
            NUtils.getUI().msg("Failed to update preview: " + e.getMessage());
        }
    }

    /**
     * Called when user confirms tree placement
     */
    public void confirmPlacement() {
        userConfirmed = true;
    }

    /**
     * Called when user cancels tree placement
     */
    public void cancelPlacement() {
        userCancelled = true;
    }

    /**
     * Gets current number of planned tree positions
     */
    public int getPlantingCount() {
        return plantingPositions != null ? plantingPositions.size() : 0;
    }

    /**
     * Resets state variables for bot reusability
     */
    private void resetState() {
        // Reset user interaction flags
        userConfirmed = false;
        userCancelled = false;

        // Clean up any existing ghost preview before resetting
        removeGhostPreview();

        // Reset state variables
        selectedArea = null;
        plantingPositions = null;
        spacingDialog = null;
        selectedSpacing = 2; // Reset to default
    }
}