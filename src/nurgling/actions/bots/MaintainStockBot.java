package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.CloseTargetContainer;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.scenarios.CraftPreset;
import nurgling.scenarios.CraftPresetManager;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * MaintainStockBot maintains a target stock level of crafted items in an area.
 * It counts existing items and available space in containers, then crafts only what's needed.
 */
public class MaintainStockBot implements Action {
    private String presetId;
    private int areaId;
    private int targetQuantity;

    public MaintainStockBot() {
    }

    public MaintainStockBot(Map<String, Object> settings) {
        if (settings != null) {
            if (settings.containsKey("presetId")) {
                this.presetId = (String) settings.get("presetId");
            }
            if (settings.containsKey("areaId")) {
                Object a = settings.get("areaId");
                if (a instanceof Integer) {
                    this.areaId = (Integer) a;
                } else if (a instanceof Long) {
                    this.areaId = ((Long) a).intValue();
                } else if (a instanceof Number) {
                    this.areaId = ((Number) a).intValue();
                }
            }
            if (settings.containsKey("targetQuantity")) {
                Object q = settings.get("targetQuantity");
                if (q instanceof Integer) {
                    this.targetQuantity = (Integer) q;
                } else if (q instanceof Long) {
                    this.targetQuantity = ((Long) q).intValue();
                } else if (q instanceof Number) {
                    this.targetQuantity = ((Number) q).intValue();
                }
            }
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Validate settings
        if (presetId == null || presetId.isEmpty()) {
            return Results.ERROR("No craft preset selected");
        }
        if (targetQuantity <= 0) {
            return Results.ERROR("Target quantity must be greater than 0");
        }

        // Load preset
        CraftPreset preset = CraftPresetManager.getInstance().getPreset(presetId);
        if (preset == null) {
            return Results.ERROR("Craft preset not found: " + presetId);
        }

        // Get output item info
        if (preset.getOutputs().isEmpty()) {
            return Results.ERROR("Preset has no output items");
        }
        CraftPreset.OutputSpec output = preset.getOutputs().get(0);
        String outputItemName = output.getName();
        Coord itemSize = new Coord(output.getWidth(), output.getHeight());

        gui.msg("MaintainStock: Checking " + outputItemName + " (target: " + targetQuantity + ")");

        // Get the area
        NArea area = NUtils.getArea(areaId);
        if (area == null) {
            return Results.ERROR("Area not found: " + areaId);
        }

        // Navigate to area
        Coord2d areaCenter = area.getCenter2d();
        if (areaCenter != null) {
            new PathFinder(areaCenter).run(gui);
        }

        // Find containers in area
        ArrayList<Gob> containerGobs = findContainersInArea(area);
        if (containerGobs.isEmpty()) {
            gui.msg("MaintainStock: No containers found in area, nothing to do");
            return Results.SUCCESS();
        }

        // Count items and available space
        int currentCount = 0;
        int availableSpace = 0;
        NAlias itemAlias = new NAlias(outputItemName);

        for (Gob gob : containerGobs) {
            String containerCap = getContainerCap(gob);
            if (containerCap == null) {
                continue;
            }

            Container container = new Container(gob, containerCap, area);

            // Navigate to container
            new PathFinder(gob).run(gui);

            // Open container
            new OpenTargetContainer(container).run(gui);

            // Get inventory and count
            NInventory inv = gui.getInventory(containerCap);
            if (inv != null) {
                // Count existing items
                ArrayList<WItem> items = inv.getItems(itemAlias);
                currentCount += items.size();

                // Count available space
                int freeSlots = inv.getNumberFreeCoord(itemSize);
                availableSpace += freeSlots;
            }

            // Close container
            new CloseTargetContainer(container).run(gui);
        }

        gui.msg("MaintainStock: Found " + currentCount + " items, space for " + availableSpace + " more");

        // Calculate how many to craft
        int needed = targetQuantity - currentCount;
        if (needed <= 0) {
            gui.msg("MaintainStock: Stock is sufficient (" + currentCount + "/" + targetQuantity + ")");
            return Results.SUCCESS();
        }

        int actualToCraft = Math.min(needed, availableSpace);
        if (actualToCraft <= 0) {
            gui.msg("MaintainStock: No space available in containers");
            return Results.SUCCESS();
        }

        gui.msg("MaintainStock: Crafting " + actualToCraft + " " + outputItemName);

        // Run AutocraftBot with calculated quantity
        Map<String, Object> autocraftSettings = new HashMap<>();
        autocraftSettings.put("presetId", presetId);
        autocraftSettings.put("quantity", actualToCraft);
        AutocraftBot autocraft = new AutocraftBot(autocraftSettings);
        return autocraft.run(gui);
    }

    /**
     * Find all container gobs in the given area.
     */
    private ArrayList<Gob> findContainersInArea(NArea area) throws InterruptedException {
        ArrayList<Gob> containers = new ArrayList<>();

        // Find standard containers
        NAlias containerAlias = new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>());
        containers.addAll(Finder.findGobs(area, containerAlias));

        // Also find stockpiles
        containers.addAll(Finder.findGobs(area, new NAlias("stockpile")));

        return containers;
    }

    /**
     * Get the window caption for a container gob.
     */
    private String getContainerCap(Gob gob) {
        if (gob == null || gob.ngob == null || gob.ngob.name == null) {
            return null;
        }

        // Check standard containers
        String cap = NContext.contcaps.get(gob.ngob.name);
        if (cap != null) {
            return cap;
        }

        // Check for stockpile
        if (gob.ngob.name.contains("stockpile")) {
            return "Stockpile";
        }

        return null;
    }
}
