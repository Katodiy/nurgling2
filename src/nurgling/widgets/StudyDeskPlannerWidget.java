package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Label;
import haven.resutil.Curiosity;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import nurgling.iteminfo.NCuriosity;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.List;

public class StudyDeskPlannerWidget extends haven.Window implements DTarget {
    public static final Coord DESK_SIZE = new Coord(7, 7);
    public static final Coord DESK_SIZE_FINE = new Coord(9, 9);
    public static final Coord DESK_SIZE_GRAND = new Coord(11, 11);
    public static final Coord sqsz = UI.scale(new Coord(32, 32)).add(1, 1);
    public static final Tex invsq;

    private final Map<Coord, PlannedCuriosity> plannedItems = new HashMap<>();
    private final Map<Coord, PlannedCuriosity> originalLayout = new HashMap<>();

    static {
        Coord sz = sqsz.add(1, 1);
        WritableRaster buf = PUtils.imgraster(sz);
        for(int i = 1, y = sz.y - 1; i < sz.x - 1; i++) {
            buf.setSample(i, 0, 0, 20); buf.setSample(i, 0, 1, 28); buf.setSample(i, 0, 2, 21); buf.setSample(i, 0, 3, 167);
            buf.setSample(i, y, 0, 20); buf.setSample(i, y, 1, 28); buf.setSample(i, y, 2, 21); buf.setSample(i, y, 3, 167);
        }
        for(int i = 1, x = sz.x - 1; i < sz.y - 1; i++) {
            buf.setSample(0, i, 0, 20); buf.setSample(0, i, 1, 28); buf.setSample(0, i, 2, 21); buf.setSample(0, i, 3, 167);
            buf.setSample(x, i, 0, 20); buf.setSample(x, i, 1, 28); buf.setSample(x, i, 2, 21); buf.setSample(x, i, 3, 167);
        }
        for(int y = 1; y < sz.y - 1; y++) {
            for(int x = 1; x < sz.x - 1; x++) {
                buf.setSample(x, y, 0, 36); buf.setSample(x, y, 1, 52); buf.setSample(x, y, 2, 38); buf.setSample(x, y, 3, 125);
            }
        }
        invsq = new TexI(PUtils.rasterimg(buf));
    }

    private final Coord studyDeskSize;
    private StudyTimePanel timePanel;
    private Scrollport timeScrollport;
    private String studyDeskHash = null; // Hash of the study desk this planner is for

    public StudyDeskPlannerWidget(Coord studyDeskSize) {
        // Width = grid + gap + time panel (200px)
        super(sqsz.mul(studyDeskSize).add(UI.scale(160), UI.scale(40)), L10n.get("study.planner_title"));

        this.studyDeskSize = studyDeskSize;

        loadLayout();
        addTimePanel();
        addButtons();
    }

    public void setStudyDeskHash(String hash) {
        if (this.studyDeskHash == null || !this.studyDeskHash.equals(hash)) {
            this.studyDeskHash = hash;
            loadLayout(); // Reload layout for this specific desk
        }
        // Always update the gob hash in the config
        updateGobHashInConfig(hash);
    }

    private void updateGobHashInConfig(String hash) {
        String charName = NUtils.getGameUI().chrid;

        // Load existing data
        Object existingData = NConfig.get(NConfig.Key.studyDeskLayout);
        Map<String, Object> allLayouts;

        if (existingData instanceof Map) {
            allLayouts = (Map<String, Object>) existingData;
        } else if (existingData instanceof String && !((String) existingData).isEmpty()) {
            // Backward compatibility: convert old string format to Map
            JSONObject jsonObj = new JSONObject((String) existingData);
            allLayouts = jsonObj.toMap();
        } else {
            allLayouts = new HashMap<>();
        }

        // Get or create character data
        Map<String, Object> charData;
        if (allLayouts.containsKey(charName)) {
            Object charObj = allLayouts.get(charName);
            if (charObj instanceof Map) {
                charData = (Map<String, Object>) charObj;
            } else {
                charData = new HashMap<>();
                allLayouts.put(charName, charData);
            }
        } else {
            charData = new HashMap<>();
            allLayouts.put(charName, charData);
        }

        // Update only the gob hash
        charData.put("gobHash", hash);

        // Save back as Map (will be serialized as proper JSON by NConfig)
        NConfig.set(NConfig.Key.studyDeskLayout, allLayouts);
    }

    @Override
    public void cdraw(GOut g) {
        // Position the grid at the top-left corner
        Coord gridStart = new Coord(UI.scale(0), UI.scale(0));

        // Draw grid squares
        Coord c = new Coord();
        for(c.y = 0; c.y < studyDeskSize.y; c.y++) {
            for(c.x = 0; c.x < studyDeskSize.x; c.x++) {
                Coord pos = gridStart.add(c.mul(sqsz));
                g.image(invsq, pos);
            }
        }

        // Draw items
        for(Map.Entry<Coord, PlannedCuriosity> entry : plannedItems.entrySet()) {
            Coord itemPos = entry.getKey();
            PlannedCuriosity item = entry.getValue();

            Coord pos = gridStart.add(itemPos.mul(sqsz));
            Coord itemSizePx = sqsz.mul(item.size);

            // Draw item image if available
            if(item.resource != null) {
                // Get the original bright image using the same approach as UsingTools.java
                Resource.Image img = item.resource.layer(Resource.imgc);
                if(img != null) {
                    // Use scaled image like UsingTools.java does
                    TexI scaledImg = new TexI(img.scaled());

                    // Draw the image at its natural size, centered in the item area
                    Coord imgNaturalSize = scaledImg.sz();
                    Coord centerPos = pos.add(itemSizePx.div(2)).sub(imgNaturalSize.div(2));

                    // Apply blueprint-like styling: blue tint and transparency
                    g.defstate();
                    g.chcolor(100, 150, 255, 180); // Blue tint with transparency
                    g.image(scaledImg, centerPos);
                    g.chcolor(); // Reset color
                } else {
                    // Fallback: draw blueprint-style rectangle with text
                    drawBlueprintItem(g, pos, itemSizePx, item.name);
                }
            } else {
                // Fallback: draw blueprint-style rectangle with text
                drawBlueprintItem(g, pos, itemSizePx, item.name);
            }
        }
    }

    private void drawBlueprintItem(GOut g, Coord pos, Coord size, String name) {
        // Draw a semi-transparent blue rectangle for blueprint effect
        g.chcolor(80, 120, 200, 120);
        g.frect(pos.add(2, 2), size.sub(4, 4));
        g.chcolor();

        // Draw brighter blue border
        g.chcolor(100, 150, 255, 220);
        g.rect(pos.add(2, 2), size.sub(4, 4));
        g.chcolor();

        // Draw name in light blue
        if(name != null && !name.isEmpty()) {
            Text nameText = Text.render(name, new java.awt.Color(150, 200, 255));
            Coord textPos = pos.add(size.div(2)).sub(nameText.sz().div(2));
            g.image(nameText.tex(), textPos);
        }
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        // Convert window coordinates to content coordinates
        Coord contentCoord = ev.c.sub(deco.contarea().ul);

        // Use the same grid calculation as in cdraw()
        Coord gridStart = new Coord(UI.scale(0), UI.scale(0));

        if(contentCoord.isect(gridStart, studyDeskSize.mul(sqsz))) {
            Coord gridPos = contentCoord.sub(gridStart).div(sqsz);
            if(gridPos.x >= 0 && gridPos.x < studyDeskSize.x && gridPos.y >= 0 && gridPos.y < studyDeskSize.y) {
                if(ev.b == 3) {
                    // Right-click: delete item at this position
                    PlannedCuriosity clickedItem = findItemAt(gridPos);
                    if(clickedItem != null) {
                        // Find the top-left corner of this item and remove it
                        Coord itemPos = findItemPosition(clickedItem);
                        if(itemPos != null) {
                            plannedItems.remove(itemPos);
                            return true;
                        }
                    }
                }
            }
        }
        return super.mousedown(ev);
    }

    private PlannedCuriosity findItemAt(Coord gridPos) {
        // Check direct position first
        PlannedCuriosity direct = plannedItems.get(gridPos);
        if(direct != null) {
            return direct;
        }

        // Check if we clicked on a multi-cell item
        for(Map.Entry<Coord, PlannedCuriosity> entry : plannedItems.entrySet()) {
            Coord itemPos = entry.getKey();
            PlannedCuriosity item = entry.getValue();

            if(gridPos.x >= itemPos.x && gridPos.x < itemPos.x + item.size.x &&
               gridPos.y >= itemPos.y && gridPos.y < itemPos.y + item.size.y) {
                return item;
            }
        }
        return null;
    }

    private Coord findItemPosition(PlannedCuriosity item) {
        for(Map.Entry<Coord, PlannedCuriosity> entry : plannedItems.entrySet()) {
            if(entry.getValue() == item) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean mouseup(MouseUpEvent ev) {
        return super.mouseup(ev);
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
        return handleItemPlacement(cc);
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        return handleItemPlacement(cc);
    }

    private boolean handleItemPlacement(Coord cc) {
        // Convert window coordinates to content coordinates
        Coord contentCoord = cc.sub(deco.contarea().ul);

        // Use the same grid calculation as in cdraw()
        Coord gridStart = new Coord(UI.scale(0), UI.scale(0));

        if(contentCoord.isect(gridStart, studyDeskSize.mul(sqsz))) {
            Coord gridPos = contentCoord.sub(gridStart).div(sqsz);

            if(gridPos.x >= 0 && gridPos.x < studyDeskSize.x && gridPos.y >= 0 && gridPos.y < studyDeskSize.y) {

                // Get the item being dragged from vhand
                WItem handItem = NUtils.getGameUI().vhand;

                if(handItem != null && handItem.item != null) {
                    // First check if this is a curio item
                    int studyTime = getStudyTime(handItem);
                    if(studyTime <= 0) {
                        // Not a curio item, reject it
                        NUtils.getGameUI().msg("Only curio items can be placed in the study desk planner!", Color.RED);
                        return false;
                    }

                    String itemName = getItemName(handItem);
                    Coord itemSize = getItemSize(handItem);
                    String resourceName = getItemResourceName(handItem);
                    Resource itemResource = getItemResource(handItem);
                    int mentalWeight = getMentalWeight(handItem);

                    // Check if item fits
                    if(canPlaceItem(gridPos, itemSize)) {
                        // Remove any overlapping items
                        removeOverlappingItems(gridPos, itemSize);

                        // Create and place the item
                        PlannedCuriosity curiosity = new PlannedCuriosity(itemName, itemSize, resourceName, itemResource, studyTime, mentalWeight);
                        plannedItems.put(new Coord(gridPos.x, gridPos.y), curiosity);

                        return true;
                    }
                } else {
                    // Fallback for when we can't detect the item - still reject non-curios
                    NUtils.getGameUI().msg("Cannot place item - no item information available!", Color.RED);
                    return false;
                }
            }
        }
        return false;
    }

    private String getItemName(WItem item) {
        if(item.item != null) {
            // Try to get name from NGItem
            if(item.item instanceof NGItem) {
                NGItem ngItem = (NGItem) item.item;
                String name = ngItem.name();
                if(name != null && !name.isEmpty()) {
                    return name;
                }
            }
            // Fallback to resource name
            if(item.item.getres() != null) {
                Resource res = item.item.getres();
                if(res.layers(Resource.Tooltip.class) != null) {
                    for(Resource.Tooltip tt : res.layers(Resource.Tooltip.class)) {
                        return tt.text();
                    }
                }
                return res.name;
            }
        }

        return "Unknown Item";
    }

    private Coord getItemSize(WItem item) {
        if(item.item != null && item.item.spr != null) {
            // Use the same approach as GetNumberFreeCoord: divide by UI.scale(32) but don't swap coordinates
            Coord lc = item.item.spr.sz().div(UI.scale(32));
            // Don't swap x and y - keep them as they are for proper orientation
            return lc;
        }
        return new Coord(1, 1);
    }

    private String getItemResourceName(WItem item) {
        if(item.item != null && item.item.getres() != null) {
            return item.item.getres().name;
        }
        return null;
    }

    private Resource getItemResource(WItem item) {
        if(item.item != null && item.item.getres() != null) {
            // Get a fresh resource instance to avoid any visual state from the hand item
            Resource originalResource = item.item.getres();
            String resourceName = originalResource.name;
            if(resourceName != null) {
                // Load a clean version of the resource
                return Resource.remote().loadwait(resourceName);
            }
            return originalResource;
        }
        return null;
    }

    private int getStudyTime(WItem item) {
            if(item.item != null) {
                List<ItemInfo> info = item.item.info();
                if(info != null) {
                    Curiosity curiosity = ItemInfo.find(Curiosity.class, info);
                    if(curiosity != null) {
                        return curiosity.time;
                    }
                }
            }
        return 0;
    }

    private int getMentalWeight(WItem item) {
        if(item.item != null) {
            List<ItemInfo> info = item.item.info();
            if(info != null) {
                Curiosity curiosity = ItemInfo.find(Curiosity.class, info);
                if(curiosity != null) {
                    return curiosity.mw;
                }
            }
        }
        return 0;
    }

    private boolean canPlaceItem(Coord pos, Coord size) {
        // Check if item goes outside grid
        if(pos.x + size.x > studyDeskSize.x || pos.y + size.y > studyDeskSize.y) {
            return false;
        }
        return true;
    }

    private void removeOverlappingItems(Coord pos, Coord size) {
        // Remove any items that would overlap with the new item
        for(int y = pos.y; y < pos.y + size.y; y++) {
            for(int x = pos.x; x < pos.x + size.x; x++) {
                plannedItems.remove(new Coord(x, y));
            }
        }

        // Also remove items that would be partially covered
        plannedItems.entrySet().removeIf(entry -> {
            Coord itemPos = entry.getKey();
            Coord itemSize = entry.getValue().size;

            // Check if this item overlaps with the new position
            return itemPos.x < pos.x + size.x && itemPos.x + itemSize.x > pos.x &&
                   itemPos.y < pos.y + size.y && itemPos.y + itemSize.y > pos.y;
        });
    }


    private void saveLayout() {
        // Get character name
        String charName = NUtils.getGameUI().chrid;
        // Load existing data for all characters
        Object existingData = NConfig.get(NConfig.Key.studyDeskLayout);
        Map<String, Object> allLayouts;
        if (existingData instanceof Map) {
            allLayouts = (Map<String, Object>) existingData;
        } else if (existingData instanceof String && !((String) existingData).isEmpty()) {
            // Backward compatibility: convert old string format to Map
            JSONObject jsonObj = new JSONObject((String) existingData);
            allLayouts = jsonObj.toMap();
        } else {
            allLayouts = new HashMap<>();
        }
        // Create character data with gob hash and layout
        Map<String, Object> charData = new HashMap<>();

        // Store gob hash if available
        if (studyDeskHash != null) {
            charData.put("gobHash", studyDeskHash);
        }

        // Create layout
        Map<String, Object> layout = new HashMap<>();
        for(Map.Entry<Coord, PlannedCuriosity> entry : plannedItems.entrySet()) {
            Coord pos = entry.getKey();
            PlannedCuriosity item = entry.getValue();

            Map<String, Object> itemData = new HashMap<>();
            itemData.put("name", item.name);
            itemData.put("sizeX", item.size.x);
            itemData.put("sizeY", item.size.y);
            itemData.put("studyTime", item.studyTime);
            itemData.put("mentalWeight", item.mentalWeight);
            if(item.resourceName != null) {
                itemData.put("resourceName", item.resourceName);
            }
            layout.put(pos.x + "," + pos.y, itemData);
        }
        charData.put("layout", layout);

        // Save under character name
        allLayouts.put(charName, charData);

        // Save as Map (will be serialized as proper JSON by NConfig)
        NConfig.set(NConfig.Key.studyDeskLayout, allLayouts);
        NUtils.getGameUI().msg(L10n.get("study.layout_saved"), Color.GREEN);
    }

    private void loadLayout() {
        plannedItems.clear();
        originalLayout.clear();

        // Get character name
        String charName = NUtils.getGameUI().chrid;

        // Load all layouts
        Object existingData = NConfig.get(NConfig.Key.studyDeskLayout);
        if(existingData != null) {
            Map<String, Object> allLayouts;
            if (existingData instanceof Map) {
                allLayouts = (Map<String, Object>) existingData;
            } else if (existingData instanceof String && !((String) existingData).isEmpty()) {
                // Backward compatibility: convert old string format to Map
                JSONObject jsonObj = new JSONObject((String) existingData);
                allLayouts = jsonObj.toMap();
            } else {
                return; // No data to load
            }

            // Get data for this character
            if (allLayouts.containsKey(charName)) {
                Object charObj = allLayouts.get(charName);
                if (!(charObj instanceof Map)) {
                    return; // Invalid data format
                }
                Map<String, Object> charData = (Map<String, Object>) charObj;

                // Load gob hash if available
                if (charData.containsKey("gobHash")) {
                    String savedHash = (String) charData.get("gobHash");
                    if (studyDeskHash == null) {
                        studyDeskHash = savedHash;
                    }
                }

                // Get layout
                if (charData.containsKey("layout")) {
                    Object layoutObj = charData.get("layout");
                    if (!(layoutObj instanceof Map)) {
                        return; // Invalid layout format
                    }
                    Map<String, Object> layout = (Map<String, Object>) layoutObj;

                    for(Map.Entry<String, Object> entry : layout.entrySet()) {
                        String key = entry.getKey();
                        String[] coords = key.split(",");
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);

                        if (!(entry.getValue() instanceof Map)) {
                            continue; // Skip invalid item data
                        }
                        Map<String, Object> itemData = (Map<String, Object>) entry.getValue();

                        String name = (String) itemData.get("name");

                        // Load size (default to 1x1 for backward compatibility)
                        int sizeX = itemData.containsKey("sizeX") ? ((Number) itemData.get("sizeX")).intValue() : 1;
                        int sizeY = itemData.containsKey("sizeY") ? ((Number) itemData.get("sizeY")).intValue() : 1;
                        Coord size = new Coord(sizeX, sizeY);

                        String resourceName = (String) itemData.get("resourceName");
                        int studyTime = itemData.containsKey("studyTime") ? ((Number) itemData.get("studyTime")).intValue() : 0;
                        int mentalWeight = itemData.containsKey("mentalWeight") ? ((Number) itemData.get("mentalWeight")).intValue() : 0;

                        // Load the actual Resource object from the resource name
                        Resource itemResource = null;
                        if (resourceName != null && !resourceName.isEmpty()) {
                            itemResource = Resource.remote().loadwait(resourceName);
                        }

                        PlannedCuriosity curiosity = new PlannedCuriosity(name, size, resourceName, itemResource, studyTime, mentalWeight);
                        plannedItems.put(new Coord(x, y), curiosity);
                        originalLayout.put(new Coord(x, y), curiosity);
                    }
                }
            }
        }
    }

    private void clearLayout() {
        plannedItems.clear();
        NUtils.getGameUI().msg(L10n.get("study.layout_cleared"), Color.YELLOW);
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if(msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(msg, args);
        }
    }

    public Map<Coord, PlannedCuriosity> getPlannedLayout() {
        return new HashMap<>(plannedItems);
    }

    private Label mentalWeightLabel;

    private void addTimePanel() {
        // Position the time panel to the right of the grid with a gap
        Coord gridEnd = sqsz.mul(studyDeskSize);
        Coord panelPos = new Coord(gridEnd.x + UI.scale(10), 0);

        // Reserve space for mental weight label at the bottom
        int scrollHeight = gridEnd.y - UI.scale(20);
        Coord scrollSize = new Coord(UI.scale(200), scrollHeight);

        // Create the content panel with scrolling support
        // Start with a small initial size, it will auto-resize based on content
        timePanel = new StudyTimePanel(new Coord(scrollSize.x, UI.scale(50)), this);

        // Wrap in a Scrollport
        timeScrollport = new Scrollport(scrollSize);
        timeScrollport.cont.add(timePanel, Coord.z);
        add(timeScrollport, panelPos);

        // Add Mental Weight label below the scrollport
        mentalWeightLabel = new Label(L10n.get("study.mental_weight") + ": 0");
        mentalWeightLabel.setcolor(new Color(255, 192, 255)); // Light purple color
        add(mentalWeightLabel, new Coord(panelPos.x, panelPos.y + scrollHeight + UI.scale(5)));
    }

    private void addButtons() {
        // Save button
        Button saveButton = new Button(UI.scale(60), L10n.get("common.save")) {
            @Override
            public void click() {
                saveLayout();
                // Update the original layout to match current state
                originalLayout.clear();
                originalLayout.putAll(plannedItems);
            }
        };

        // Cancel button
        Button cancelButton = new Button(UI.scale(60), L10n.get("common.cancel")) {
            @Override
            public void click() {
                cancelChanges();
            }
        };

        // Position buttons below the grid
        Coord gridBottom = sqsz.mul(studyDeskSize);
        int buttonY = gridBottom.y + UI.scale(8);

        // Center the buttons horizontally
        int totalButtonWidth = UI.scale(60) * 2 + UI.scale(10); // two buttons + gap
        int startX = (gridBottom.x - totalButtonWidth) / 2;

        add(saveButton, new Coord(startX, buttonY));
        add(cancelButton, new Coord(startX + UI.scale(70), buttonY));
    }

    private void cancelChanges() {
        // Restore to original layout
        plannedItems.clear();
        plannedItems.putAll(originalLayout);
        NUtils.getGameUI().msg(L10n.get("study.changes_cancelled"), Color.YELLOW);
    }

    public static class StudyTimePanel extends Widget {
        private final StudyDeskPlannerWidget parent;
        private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 10);
        private static final int LINE_HEIGHT = UI.scale(20); // Single line per item

        public StudyTimePanel(Coord sz, StudyDeskPlannerWidget parent) {
            super(sz);
            this.parent = parent;
        }

        private int lastItemCount = -1;

        @Override
        public void draw(GOut g) {
            super.draw(g);

            // Calculate study times for each unique item
            Map<String, ItemTimeInfo> itemTimes = calculateItemTimes();

            // Check if we need to rebuild (item count changed)
            if(itemTimes.size() != lastItemCount) {
                lastItemCount = itemTimes.size();
                rebuildContent(itemTimes);
            }

            // Sort alphabetically by item name
            List<ItemTimeInfo> sortedItems = new ArrayList<>(itemTimes.values());
            sortedItems.sort(Comparator.comparing(a -> a.name, String.CASE_INSENSITIVE_ORDER));

            // Calculate total mental weight (each unique item type contributes its weight once)
            int totalMentalWeight = 0;
            for(ItemTimeInfo info : itemTimes.values()) {
                totalMentalWeight += info.mentalWeight;
            }
            updateMentalWeight(totalMentalWeight);

            int y = 0;
            for(ItemTimeInfo info : sortedItems) {
                // Draw icon if available
                if(info.resource != null) {
                    Resource.Image img = info.resource.layer(Resource.imgc);
                    if(img != null) {
                        TexI scaledImg = new TexI(img.scaled());
                        Coord iconSize = UI.scale(new Coord(16, 16));
                        g.image(scaledImg, new Coord(0, y), iconSize);
                    }
                }

                // Draw quantity and time text (convert to real time)
                int realTime = (int)(info.totalTime / NCuriosity.server_ratio);
                String timeText = String.format("x%d - %s", info.count, formatTime(realTime));
                Text t = fnd.render(timeText, Color.WHITE);
                g.image(t.tex(), new Coord(UI.scale(20), y + 2));

                y += LINE_HEIGHT;
            }
        }

        private void updateMentalWeight(int mentalWeight) {
            if(parent.mentalWeightLabel != null) {
                String text = L10n.get("study.mental_weight") + ": " + mentalWeight;
                parent.mentalWeightLabel.settext(text);
            }
        }

        private void rebuildContent(Map<String, ItemTimeInfo> itemTimes) {
            // Calculate required height - ensure it's enough to trigger scrollbar
            int contentHeight = itemTimes.size() * LINE_HEIGHT + UI.scale(10);
            // Ensure minimum height
            contentHeight = Math.max(contentHeight, UI.scale(50));

            // Force resize if different
            Coord newSize = new Coord(sz.x, contentHeight);
            if(!sz.equals(newSize)) {
                resize(newSize);

                // IMPORTANT: Update scrollport container to recalculate scrollbar
                if(parent.timeScrollport != null && parent.timeScrollport.cont != null) {
                    parent.timeScrollport.cont.update();
                }
            }
        }


        private Map<String, ItemTimeInfo> calculateItemTimes() {
            Map<String, ItemTimeInfo> itemTimes = new HashMap<>();

            for(PlannedCuriosity item : parent.plannedItems.values()) {
                if(item.studyTime > 0) {
                    String key = item.resourceName != null ? item.resourceName : item.name;

                    ItemTimeInfo info = itemTimes.get(key);
                    if(info == null) {
                        info = new ItemTimeInfo(item.name, item.resource, item.studyTime, item.mentalWeight);
                        itemTimes.put(key, info);
                    } else {
                        info.count++;
                        info.totalTime += item.studyTime;
                    }
                }
            }

            return itemTimes;
        }

        private String formatTime(int seconds) {
            if(seconds == 0) {
                return "0s";
            }

            int days = seconds / 86400;
            int hours = (seconds % 86400) / 3600;
            int minutes = (seconds % 3600) / 60;
            int secs = seconds % 60;

            StringBuilder sb = new StringBuilder();
            if(days > 0) {
                sb.append(days).append("d");
            }
            if(hours > 0) {
                if(sb.length() > 0) sb.append(" ");
                sb.append(hours).append("h");
            }
            if(minutes > 0) {
                if(sb.length() > 0) sb.append(" ");
                sb.append(minutes).append("m");
            }
            if(secs > 0) {
                if(sb.length() > 0) sb.append(" ");
                sb.append(secs).append("s");
            }

            return sb.toString();
        }

        private static class ItemTimeInfo {
            String name;
            Resource resource;
            int studyTime;
            int mentalWeight;
            int count = 1;
            int totalTime;

            ItemTimeInfo(String name, Resource resource, int studyTime, int mentalWeight) {
                this.name = name;
                this.resource = resource;
                this.studyTime = studyTime;
                this.mentalWeight = mentalWeight;
                this.totalTime = studyTime;
            }
        }
    }

    public static class PlannedCuriosity {
        public final String name;
        public final Coord size;
        public final String resourceName;
        public final Resource resource;
        public final int studyTime; // Study time in seconds
        public final int mentalWeight; // Mental weight of the item

        public PlannedCuriosity(String name, Coord size, String resourceName, Resource resource, int studyTime, int mentalWeight) {
            this.name = name;
            this.size = size != null ? size : new Coord(1, 1);
            this.resourceName = resourceName;
            this.resource = resource;
            this.studyTime = studyTime;
            this.mentalWeight = mentalWeight;
        }

        public PlannedCuriosity(String name, Coord size, String resourceName, Resource resource, int studyTime) {
            this(name, size, resourceName, resource, studyTime, 0);
        }

        public PlannedCuriosity(String name, Coord size, String resourceName) {
            this(name, size, resourceName, null, 0, 0);
        }
    }
}