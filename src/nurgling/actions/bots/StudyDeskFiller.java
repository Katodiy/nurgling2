package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Finder;
import nurgling.widgets.Specialisation;
import org.json.JSONObject;

import java.awt.Color;
import java.util.*;

/**
 * Bot that validates the current study desk layout against the planned layout from config
 */
public class StudyDeskFiller implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Step 1: Validate config exists
        Map<String, Object> charData = loadAndValidateConfig(gui);
        if (charData == null) {
            return Results.ERROR("No planner layout");
        }

        String studyDeskHash = (String) charData.get("gobHash");
        Map<String, Object> plannedLayout = (Map<String, Object>) charData.get("layout");

        gui.msg("Found layout config with " + plannedLayout.size() + " planned items", Color.GREEN);

        // Step 2: Navigate to study desk area
        NArea studyDeskArea = getStudyDeskArea(gui);
        if (studyDeskArea == null) {
            gui.msg("ERROR: No study desk area found! Please create one first.", Color.RED);
            return Results.ERROR("No study desk area found! Please create one first");
        }

        // Step 3: Find the specific study desk by hash
        gui.msg("Looking for study desk with hash: " + studyDeskHash, Color.YELLOW);
        Gob studyDesk = Finder.findGob(studyDeskHash);
        if (studyDesk == null) {
            gui.msg("ERROR: Could not find study desk with hash: " + studyDeskHash, Color.RED);
            return Results.ERROR("Could not find study desk with hash: " + studyDeskHash);
        }

        gui.msg("Found study desk! Navigating to it...", Color.GREEN);

        // Step 4: Navigate to the study desk
        new PathFinder(studyDesk).run(gui);

        // Step 5: Open the study desk
        gui.msg("Opening study desk...", Color.YELLOW);
        new OpenTargetContainer("Study Desk", studyDesk).run(gui);

        // Step 6: Get the study desk inventory
        NInventory studyDeskInv = gui.getInventory("Study Desk");
        if (studyDeskInv == null) {
            gui.msg("ERROR: Could not access study desk inventory!", Color.RED);
            return Results.ERROR("ERROR: Could not access study desk inventory!");
        }

        gui.msg("Study desk opened successfully! Analyzing contents...", Color.GREEN);

        // Step 7: Build map of current item positions
        Map<Coord, WItem> currentItems = buildCurrentItemsMap(studyDeskInv);
        gui.msg("Current desk has " + currentItems.size() + " items", Color.CYAN);

        // Step 8: Find missing items
        List<MissingItem> missingItems = findMissingItems(plannedLayout, currentItems);

        // Step 9: Report results
        reportMissingItems(gui, missingItems);

        return Results.SUCCESS();
    }

    /**
     * Load and validate the study desk layout config for the current character
     */
    private Map<String, Object> loadAndValidateConfig(NGameUI gui) {
        String charName = gui.chrid;
        Object existingData = NConfig.get(NConfig.Key.studyDeskLayout);

        if (existingData == null) {
            gui.msg("ERROR: No study desk layout configuration found!", Color.RED);
            return null;
        }

        Map<String, Object> allLayouts;
        if (existingData instanceof Map) {
            allLayouts = (Map<String, Object>) existingData;
        } else if (existingData instanceof String && !((String) existingData).isEmpty()) {
            JSONObject jsonObj = new JSONObject((String) existingData);
            allLayouts = jsonObj.toMap();
        } else {
            gui.msg("ERROR: Invalid study desk layout configuration!", Color.RED);
            return null;
        }

        if (!allLayouts.containsKey(charName)) {
            gui.msg("ERROR: No study desk layout found for character: " + charName, Color.RED);
            return null;
        }

        Object charObj = allLayouts.get(charName);
        if (!(charObj instanceof Map)) {
            gui.msg("ERROR: Invalid character layout data!", Color.RED);
            return null;
        }

        Map<String, Object> charData = (Map<String, Object>) charObj;

        if (!charData.containsKey("gobHash")) {
            gui.msg("ERROR: No study desk hash found in layout config!", Color.RED);
            return null;
        }

        if (!charData.containsKey("layout")) {
            gui.msg("ERROR: No layout data found in config!", Color.RED);
            return null;
        }

        Object layoutObj = charData.get("layout");
        if (!(layoutObj instanceof Map)) {
            gui.msg("ERROR: Invalid layout data format!", Color.RED);
            return null;
        }

        return charData;
    }

    /**
     * Get the study desk area using NContext
     */
    private NArea getStudyDeskArea(NGameUI gui) throws InterruptedException {
        gui.msg("Navigating to study desks area...", Color.YELLOW);
        NContext context = new NContext(gui);
        return context.getSpecArea(Specialisation.SpecName.studyDesks);
    }

    /**
     * Build a map of current item positions in the study desk
     */
    private Map<Coord, WItem> buildCurrentItemsMap(NInventory inventory) throws InterruptedException {
        Map<Coord, WItem> currentItems = new HashMap<>();
        ArrayList<WItem> items = inventory.getItems();

        for (WItem witem : items) {
            if (witem != null && witem.c != null) {
                // Convert pixel coordinates to grid coordinates
                Coord gridPos = witem.c.div(inventory.sqsz);
                currentItems.put(gridPos, witem);
            }
        }

        return currentItems;
    }

    /**
     * Find missing items by comparing planned layout vs current items
     */
    private List<MissingItem> findMissingItems(Map<String, Object> plannedLayout, Map<Coord, WItem> currentItems) {
        List<MissingItem> missingItems = new ArrayList<>();

        for (Map.Entry<String, Object> plannedEntry : plannedLayout.entrySet()) {
            String posKey = plannedEntry.getKey();
            String[] coords = posKey.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            Coord plannedPos = new Coord(x, y);

            if (!(plannedEntry.getValue() instanceof Map)) {
                continue;
            }

            Map<String, Object> itemData = (Map<String, Object>) plannedEntry.getValue();
            String itemName = (String) itemData.get("name");

            // Check if this position is empty in the current desk
            if (!currentItems.containsKey(plannedPos)) {
                missingItems.add(new MissingItem(itemName, plannedPos));
            }
        }

        return missingItems;
    }

    /**
     * Report missing items to the user
     */
    private void reportMissingItems(NGameUI gui, List<MissingItem> missingItems) {
        if (missingItems.isEmpty()) {
            gui.msg("SUCCESS: All planned items are present in the study desk!", Color.GREEN);
            return;
        }

        gui.msg("=".repeat(50), Color.YELLOW);
        gui.msg("MISSING ITEMS REPORT:", Color.YELLOW);
        gui.msg("=".repeat(50), Color.YELLOW);

        // Sort by position for easier reading
        missingItems.sort(Comparator.comparing(a -> a.position.x * 100 + a.position.y));

        for (MissingItem missing : missingItems) {
            gui.msg(String.format("  Missing: %s at position (%d, %d)",
                    missing.itemName, missing.position.x, missing.position.y), Color.ORANGE);
        }

        gui.msg("=".repeat(50), Color.YELLOW);
        gui.msg("Total missing items: " + missingItems.size(), Color.YELLOW);
    }

    /**
     * Helper class to store information about missing items
     */
    private static class MissingItem {
        String itemName;
        Coord position;

        MissingItem(String itemName, Coord position) {
            this.itemName = itemName;
            this.position = position;
        }
    }
}
