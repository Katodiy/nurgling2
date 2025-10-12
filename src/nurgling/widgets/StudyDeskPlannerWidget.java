package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import nurgling.*;
import nurgling.tools.NAlias;
import org.json.*;

import java.awt.*;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

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

    public StudyDeskPlannerWidget() {
        super(sqsz.mul(DESK_SIZE).add(0, UI.scale(40)), "Study Desk Planner");

        loadLayout();
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

                        // Make sure graphics state is clean for bright rendering
                        g.defstate();
                        g.image(scaledImg, centerPos);
                    } else {
                        // Fallback: draw gray rectangle with text
                        drawFallbackItem(g, pos, itemSizePx, item.name);
                    }
                } catch(Exception e) {
                    // Fallback: draw gray rectangle with text
                    drawFallbackItem(g, pos, itemSizePx, item.name);
                }
            } else {
                // Fallback: draw gray rectangle with text
                drawFallbackItem(g, pos, itemSizePx, item.name);
            }
        }
    }

    private void drawFallbackItem(GOut g, Coord pos, Coord size, String name) {
        // Draw gray rectangle
        g.chcolor(128, 128, 128, 200);
        g.frect(pos.add(2, 2), size.sub(4, 4));
        g.chcolor();

        // Draw border
        g.chcolor(64, 64, 64, 255);
        g.rect(pos.add(2, 2), size.sub(4, 4));
        g.chcolor();

        // Center text within the item's area
        Coord textPos = pos.add(size.div(2));
        g.atext(name, textPos, 0.5, 0.5);
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
                    String itemName = getItemName(handItem);
                    Coord itemSize = getItemSize(handItem);
                    String resourceName = getItemResourceName(handItem);
                    Resource itemResource = getItemResource(handItem);

                    // Check if item fits
                    if(canPlaceItem(gridPos, itemSize)) {
                        // Remove any overlapping items
                        removeOverlappingItems(gridPos, itemSize);

                        // Create and place the item
                        PlannedCuriosity curiosity = new PlannedCuriosity(itemName, itemSize, resourceName, itemResource);
                        plannedItems.put(new Coord(gridPos.x, gridPos.y), curiosity);

                        return true;
                    }
                } else {
                    // Fallback for when we can't detect the item
                    String itemName = "Unknown Item";
                    Coord itemSize = new Coord(1, 1);
                    String resourceName = null;
                    Resource itemResource = null;

                    if(canPlaceItem(gridPos, itemSize)) {
                        removeOverlappingItems(gridPos, itemSize);
                        PlannedCuriosity curiosity = new PlannedCuriosity(itemName, itemSize, resourceName, itemResource);
                        plannedItems.put(new Coord(gridPos.x, gridPos.y), curiosity);
                        return true;
                    }
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

                    // Load the actual Resource object from the resource name
                    Resource itemResource = null;
                    if (resourceName != null && !resourceName.isEmpty()) {
                        try {
                            itemResource = Resource.remote().loadwait(resourceName);
                        } catch (Exception e) {
                            // If resource loading fails, continue without it
                        }
                    }

                    PlannedCuriosity curiosity = new PlannedCuriosity(name, size, resourceName, itemResource);
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

    public static class PlannedCuriosity {
        public final String name;
        public final Coord size;
        public final String resourceName;
        public final Resource resource;

        public PlannedCuriosity(String name, Coord size, String resourceName, Resource resource) {
            this.name = name;
            this.size = size != null ? size : new Coord(1, 1);
            this.resourceName = resourceName;
            this.resource = resource;
        }

        public PlannedCuriosity(String name, Coord size, String resourceName) {
            this(name, size, resourceName, null);
        }
    }
}