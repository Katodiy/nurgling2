package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.resutil.Curiosity;
import nurgling.*;
import nurgling.iteminfo.NCuriosity;
import nurgling.tools.NAlias;
import org.json.*;

import java.awt.*;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.List;

public class StudyDeskPlannerWidget extends haven.Window implements DTarget {
    public static final Coord DESK_SIZE = new Coord(7, 7);
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

    private StudyTimePanel timePanel;

    public StudyDeskPlannerWidget() {
        // Width = grid + gap + time panel (200px)
        super(sqsz.mul(DESK_SIZE).add(UI.scale(160), UI.scale(40)), "Study Desk Planner");

        loadLayout();
        addTimePanel();
        addButtons();
    }

    @Override
    public void cdraw(GOut g) {
        // Position the grid at the top-left corner
        Coord gridStart = new Coord(UI.scale(0), UI.scale(0));

        // Draw grid squares
        Coord c = new Coord();
        for(c.y = 0; c.y < DESK_SIZE.y; c.y++) {
            for(c.x = 0; c.x < DESK_SIZE.x; c.x++) {
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
                try {
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
                } catch(Exception e) {
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
        Coord gridEnd = gridStart.add(DESK_SIZE.mul(sqsz));

        if(contentCoord.isect(gridStart, DESK_SIZE.mul(sqsz))) {
            Coord gridPos = contentCoord.sub(gridStart).div(sqsz);
            if(gridPos.x >= 0 && gridPos.x < DESK_SIZE.x && gridPos.y >= 0 && gridPos.y < DESK_SIZE.y) {
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
        return handleItemPlacement(cc, ul);
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        return handleItemPlacement(cc, ul);
    }

    private boolean handleItemPlacement(Coord cc, Coord ul) {
        // Convert window coordinates to content coordinates
        Coord contentCoord = cc.sub(deco.contarea().ul);

        // Use the same grid calculation as in cdraw()
        Coord gridStart = new Coord(UI.scale(0), UI.scale(0));

        if(contentCoord.isect(gridStart, DESK_SIZE.mul(sqsz))) {
            Coord gridPos = contentCoord.sub(gridStart).div(sqsz);

            if(gridPos.x >= 0 && gridPos.x < DESK_SIZE.x && gridPos.y >= 0 && gridPos.y < DESK_SIZE.y) {

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

                    // Check if item fits
                    if(canPlaceItem(gridPos, itemSize)) {
                        // Remove any overlapping items
                        removeOverlappingItems(gridPos, itemSize);

                        // Create and place the item
                        PlannedCuriosity curiosity = new PlannedCuriosity(itemName, itemSize, resourceName, itemResource, studyTime);
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
        try {
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
                            return tt.t;
                        }
                    }
                    return res.name;
                }
            }
        } catch(Exception e) {
            // Fallback
        }
        return "Unknown Item";
    }

    private Coord getItemSize(WItem item) {
        try {
            if(item.item != null && item.item.spr != null) {
                // Use the same approach as GetNumberFreeCoord: divide by UI.scale(32) but don't swap coordinates
                Coord lc = item.item.spr.sz().div(UI.scale(32));
                // Don't swap x and y - keep them as they are for proper orientation
                return lc;
            }
        } catch(Exception e) {
            // Fallback to 1x1
        }
        return new Coord(1, 1);
    }

    private String getItemResourceName(WItem item) {
        try {
            if(item.item != null && item.item.getres() != null) {
                return item.item.getres().name;
            }
        } catch(Exception e) {
            // Fallback
        }
        return null;
    }

    private Resource getItemResource(WItem item) {
        try {
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
        } catch(Exception e) {
            // Fallback to original method
            try {
                if(item.item != null && item.item.getres() != null) {
                    return item.item.getres();
                }
            } catch(Exception e2) {
                // Fallback
            }
        }
        return null;
    }

    private int getStudyTime(WItem item) {
        try {
            if(item.item != null) {
                List<ItemInfo> info = item.item.info();
                if(info != null) {
                    Curiosity curiosity = ItemInfo.find(Curiosity.class, info);
                    if(curiosity != null) {
                        return curiosity.time;
                    }
                }
            }
        } catch(Exception e) {
            // Fallback
        }
        return 0;
    }


    private boolean canPlaceItem(Coord pos, Coord size) {
        // Check if item goes outside grid
        if(pos.x + size.x > DESK_SIZE.x || pos.y + size.y > DESK_SIZE.y) {
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
        try {
            JSONObject layout = new JSONObject();
            for(Map.Entry<Coord, PlannedCuriosity> entry : plannedItems.entrySet()) {
                Coord pos = entry.getKey();
                PlannedCuriosity item = entry.getValue();

                JSONObject itemData = new JSONObject();
                itemData.put("name", item.name);
                itemData.put("sizeX", item.size.x);
                itemData.put("sizeY", item.size.y);
                itemData.put("studyTime", item.studyTime);
                if(item.resourceName != null) {
                    itemData.put("resourceName", item.resourceName);
                }

                layout.put(pos.x + "," + pos.y, itemData);
            }

            NConfig.set(NConfig.Key.studyDeskLayout, layout.toString());
            NUtils.getGameUI().msg("Study desk layout saved!", Color.GREEN);
        } catch(Exception e) {
            NUtils.getGameUI().msg("Failed to save layout: " + e.getMessage(), Color.RED);
        }
    }

    private void loadLayout() {
        try {
            plannedItems.clear();
            originalLayout.clear();
            String layoutStr = (String) NConfig.get(NConfig.Key.studyDeskLayout);
            if(layoutStr != null && !layoutStr.isEmpty()) {
                JSONObject layout = new JSONObject(layoutStr);

                for(String key : layout.keySet()) {
                    String[] coords = key.split(",");
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);

                    JSONObject itemData = layout.getJSONObject(key);
                    String name = itemData.getString("name");

                    // Load size (default to 1x1 for backward compatibility)
                    int sizeX = itemData.optInt("sizeX", 1);
                    int sizeY = itemData.optInt("sizeY", 1);
                    Coord size = new Coord(sizeX, sizeY);

                    String resourceName = itemData.optString("resourceName", null);
                    int studyTime = itemData.optInt("studyTime", 0);

                    // Load the actual Resource object from the resource name
                    Resource itemResource = null;
                    if (resourceName != null && !resourceName.isEmpty()) {
                        try {
                            itemResource = Resource.remote().loadwait(resourceName);
                        } catch (Exception e) {
                            // If resource loading fails, continue without it
                        }
                    }

                    PlannedCuriosity curiosity = new PlannedCuriosity(name, size, resourceName, itemResource, studyTime);
                    plannedItems.put(new Coord(x, y), curiosity);
                    originalLayout.put(new Coord(x, y), curiosity);
                }
            }
        } catch(Exception e) {
            NUtils.getGameUI().msg("Failed to load layout: " + e.getMessage(), Color.RED);
        }
    }

    private void clearLayout() {
        plannedItems.clear();
        NUtils.getGameUI().msg("Layout cleared!", Color.YELLOW);
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

    private void addTimePanel() {
        // Position the time panel to the right of the grid with a gap
        Coord gridEnd = sqsz.mul(DESK_SIZE);
        Coord panelPos = new Coord(gridEnd.x + UI.scale(10), 0);
        Coord panelSize = new Coord(UI.scale(200), gridEnd.y);

        timePanel = new StudyTimePanel(panelSize, this);
        add(timePanel, panelPos);
    }

    private void addButtons() {
        // Save button
        Button saveButton = new Button(UI.scale(60), "Save") {
            @Override
            public void click() {
                saveLayout();
                // Update the original layout to match current state
                originalLayout.clear();
                originalLayout.putAll(plannedItems);
            }
        };

        // Cancel button
        Button cancelButton = new Button(UI.scale(60), "Cancel") {
            @Override
            public void click() {
                cancelChanges();
            }
        };

        // Position buttons below the grid
        Coord gridBottom = sqsz.mul(DESK_SIZE);
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
        NUtils.getGameUI().msg("Changes cancelled - layout restored", Color.YELLOW);
    }

    public static class StudyTimePanel extends Widget {
        private final StudyDeskPlannerWidget parent;
        private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 12);
        private static final int LINE_HEIGHT = UI.scale(20);

        public StudyTimePanel(Coord sz, StudyDeskPlannerWidget parent) {
            super(sz);
            this.parent = parent;
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);

            // Calculate study times for each unique item
            Map<String, ItemTimeInfo> itemTimes = calculateItemTimes();

            // Sort by resource name for consistent display
            List<ItemTimeInfo> sortedItems = new ArrayList<>(itemTimes.values());
            sortedItems.sort(Comparator.comparing(a -> a.name));

            int y = 0;
            for(ItemTimeInfo info : sortedItems) {
                if(y + LINE_HEIGHT > sz.y) {
                    break; // Don't draw beyond panel bounds
                }

                // Draw icon if available
                if(info.resource != null) {
                    try {
                        Resource.Image img = info.resource.layer(Resource.imgc);
                        if(img != null) {
                            TexI scaledImg = new TexI(img.scaled());
                            Coord iconSize = UI.scale(new Coord(16, 16));
                            g.image(scaledImg, new Coord(0, y), iconSize);
                        }
                    } catch(Exception e) {
                        // Skip icon if there's an issue
                    }
                }

                // Draw quantity and time text (convert to real time)
                int realTime = (int)(info.totalTime / NCuriosity.server_ratio);
                String displayText = String.format("x%d - %s", info.count, formatTime(realTime));
                Text t = fnd.render(displayText, Color.WHITE);
                g.image(t.tex(), new Coord(UI.scale(20), y + 2));

                y += LINE_HEIGHT;
            }
        }

        private Map<String, ItemTimeInfo> calculateItemTimes() {
            Map<String, ItemTimeInfo> itemTimes = new HashMap<>();

            for(PlannedCuriosity item : parent.plannedItems.values()) {
                if(item.studyTime > 0) {
                    String key = item.resourceName != null ? item.resourceName : item.name;

                    ItemTimeInfo info = itemTimes.get(key);
                    if(info == null) {
                        info = new ItemTimeInfo(item.name, item.resource, item.studyTime);
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
            int count = 1;
            int totalTime;

            ItemTimeInfo(String name, Resource resource, int studyTime) {
                this.name = name;
                this.resource = resource;
                this.studyTime = studyTime;
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

        public PlannedCuriosity(String name, Coord size, String resourceName, Resource resource, int studyTime) {
            this.name = name;
            this.size = size != null ? size : new Coord(1, 1);
            this.resourceName = resourceName;
            this.resource = resource;
            this.studyTime = studyTime;
        }

        public PlannedCuriosity(String name, Coord size, String resourceName) {
            this(name, size, resourceName, null, 0);
        }
    }
}