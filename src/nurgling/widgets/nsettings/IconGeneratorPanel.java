package nurgling.widgets.nsettings;

import haven.*;
import haven.res.lib.itemtex.ItemTex;
import nurgling.tools.VSpec;
import nurgling.widgets.CustomIconGenerator;
import org.json.JSONObject;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Settings panel for generating custom icons.
 * Allows users to select a background color and an in-game item
 * to create custom bot-style icons.
 */
public class IconGeneratorPanel extends Panel {

    private static final int MARGIN = UI.scale(10);
    private static final int SWATCH_SIZE = UI.scale(24);
    private static final int SWATCH_GAP = UI.scale(4);

    // Color selection
    private Color selectedColor = CustomIconGenerator.PRESET_COLORS[0];
    private final List<ColorSwatch> colorSwatches = new ArrayList<>();

    // Item selection
    private final List<Category> categories = new ArrayList<>();
    private final Map<String, List<ItemEntry>> categoryItems = new HashMap<>();
    private final List<ItemEntry> allItems = new ArrayList<>();
    private CategoryList categoryList;
    private ItemList itemList;
    private TextEntry searchBox;
    private ItemEntry selectedItem = null;

    // Preview
    private BufferedImage[] previewIcons = null;
    private Tex[] previewTextures = null;

    public IconGeneratorPanel() {
        super("Icon Generator");

        initializeItemData();
        buildUI();
        updatePreview();
    }

    private void initializeItemData() {
        // Load categories and items from VSpec
        Set<String> categoryNames = VSpec.categories.keySet();
        for (String categoryName : categoryNames) {
            categories.add(new Category(categoryName));
            List<ItemEntry> items = new ArrayList<>();
            for (JSONObject obj : VSpec.categories.get(categoryName)) {
                String name = obj.getString("name");
                ItemEntry item = new ItemEntry(name, obj);
                items.add(item);
                allItems.add(item);
            }
            items.sort(Comparator.comparing(ItemEntry::getName));
            categoryItems.put(categoryName, items);
        }
        categories.sort(Comparator.comparing(Category::getName));
    }

    private void buildUI() {
        int y = MARGIN;

        // Title: Background Color
        add(new Label("Background Color:"), new Coord(MARGIN, y));
        y += UI.scale(20);

        // Color swatches
        int swatchX = MARGIN;
        int swatchesPerRow = 9;
        for (int i = 0; i < CustomIconGenerator.PRESET_COLORS.length; i++) {
            Color color = CustomIconGenerator.PRESET_COLORS[i];
            ColorSwatch swatch = new ColorSwatch(color);
            add(swatch, new Coord(swatchX, y));
            colorSwatches.add(swatch);

            swatchX += SWATCH_SIZE + SWATCH_GAP;
            if ((i + 1) % swatchesPerRow == 0) {
                swatchX = MARGIN;
                y += SWATCH_SIZE + SWATCH_GAP;
            }
        }

        if (CustomIconGenerator.PRESET_COLORS.length % swatchesPerRow != 0) {
            y += SWATCH_SIZE + SWATCH_GAP;
        }

        y += UI.scale(10);

        // Title: Item Selection
        add(new Label("Item Selection:"), new Coord(MARGIN, y));
        y += UI.scale(20);

        // Category list (left side)
        add(new Label("Categories"), new Coord(MARGIN, y));
        int catListWidth = UI.scale(150);
        int catListHeight = UI.scale(200);
        categoryList = add(new CategoryList(new Coord(catListWidth, catListHeight)), new Coord(MARGIN, y + UI.scale(16)));

        // Item list (right side)
        int itemListX = MARGIN + catListWidth + UI.scale(10);
        add(new Label("Items"), new Coord(itemListX, y));

        // Search box
        int searchWidth = UI.scale(280);
        searchBox = add(new TextEntry(searchWidth, "") {
            @Override
            public void changed() {
                filterItems();
            }
        }, new Coord(itemListX, y + UI.scale(16)));

        int itemListWidth = UI.scale(280);
        int itemListHeight = UI.scale(180);
        itemList = add(new ItemList(new Coord(itemListWidth, itemListHeight)),
            new Coord(itemListX, y + UI.scale(16) + searchBox.sz.y + UI.scale(4)));

        y += UI.scale(16) + catListHeight + UI.scale(20);

        // Preview section
        add(new Label("Preview:"), new Coord(MARGIN, y));
        y += UI.scale(20);

        // Preview will be drawn in draw() method
        // Reserve space for preview
        y += UI.scale(50);

        // State labels under preview area
        int previewX = MARGIN + UI.scale(10);
        int previewSpacing = UI.scale(50);
        add(new Label("Normal", Text.std), new Coord(previewX + UI.scale(4), y));
        add(new Label("Pressed", Text.std), new Coord(previewX + previewSpacing + UI.scale(1), y));
        add(new Label("Hover", Text.std), new Coord(previewX + previewSpacing * 2 + UI.scale(4), y));

        pack();
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);

        // Draw selection highlight on color swatches
        for (ColorSwatch swatch : colorSwatches) {
            if (swatch.color.equals(selectedColor)) {
                Coord pos = swatch.c.sub(2, 2);
                Coord size = swatch.sz.add(4, 4);
                g.chcolor(255, 255, 255, 255);
                g.rect(pos, size);
                g.chcolor();
            }
        }

        // Draw preview icons
        if (previewTextures != null) {
            int previewY = sz.y - UI.scale(70);
            int previewX = MARGIN + UI.scale(10);
            int spacing = UI.scale(50);

            for (int i = 0; i < previewTextures.length; i++) {
                if (previewTextures[i] != null) {
                    g.image(previewTextures[i], new Coord(previewX + spacing * i, previewY));
                }
            }
        }
    }

    private void selectColor(Color color) {
        selectedColor = color;
        updatePreview();
    }

    private void selectItem(ItemEntry item) {
        selectedItem = item;
        updatePreview();
    }

    private void filterItems() {
        String searchText = searchBox.text().toLowerCase().trim();

        if (searchText.isEmpty()) {
            // Show items from selected category
            Category selected = categoryList.getSelected();
            if (selected != null) {
                List<ItemEntry> items = categoryItems.get(selected.getName());
                itemList.setItems(items != null ? items : Collections.emptyList());
            } else {
                itemList.setItems(Collections.emptyList());
            }
        } else {
            // Filter all items by search text
            List<ItemEntry> filtered = new ArrayList<>();
            for (ItemEntry item : allItems) {
                if (item.getName().toLowerCase().contains(searchText)) {
                    filtered.add(item);
                }
            }
            // Remove duplicates and sort
            Set<String> seen = new HashSet<>();
            List<ItemEntry> unique = new ArrayList<>();
            for (ItemEntry item : filtered) {
                if (seen.add(item.getName())) {
                    unique.add(item);
                }
            }
            unique.sort(Comparator.comparing(ItemEntry::getName));
            itemList.setItems(unique);
        }
    }

    private void updatePreview() {
        JSONObject itemRes = selectedItem != null ? selectedItem.getResource() : null;
        previewIcons = CustomIconGenerator.generateIconSet(selectedColor, itemRes);

        previewTextures = new Tex[3];
        for (int i = 0; i < 3; i++) {
            if (previewIcons[i] != null) {
                previewTextures[i] = new TexI(previewIcons[i]);
            }
        }
    }

    @Override
    public void load() {
        // Reset to defaults when panel is shown
        selectedColor = CustomIconGenerator.PRESET_COLORS[0];
        selectedItem = null;
        if (searchBox != null) {
            searchBox.settext("");
        }
        updatePreview();
    }

    @Override
    public void save() {
        // Currently just for preview - no persistence yet
    }

    // ==================== Inner Classes ====================

    /**
     * A clickable color swatch widget.
     */
    private class ColorSwatch extends Widget {
        final Color color;

        ColorSwatch(Color color) {
            super(new Coord(SWATCH_SIZE, SWATCH_SIZE));
            this.color = color;
        }

        @Override
        public void draw(GOut g) {
            // Draw background
            g.chcolor(color);
            g.frect(Coord.z, sz);

            // Draw border
            g.chcolor(0, 0, 0, 200);
            g.rect(Coord.z, sz);
            g.chcolor();
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 1) {
                selectColor(color);
                return true;
            }
            return super.mousedown(ev);
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            return String.format("RGB(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    /**
     * Represents a category from VSpec.
     */
    private static class Category {
        private final String name;

        Category(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }
    }

    /**
     * Represents an item from VSpec.
     */
    private static class ItemEntry {
        private final String name;
        private final JSONObject resource;
        private BufferedImage cachedIcon;
        private boolean iconLoaded = false;

        ItemEntry(String name, JSONObject resource) {
            this.name = name;
            this.resource = resource;
        }

        String getName() {
            return name;
        }

        JSONObject getResource() {
            return resource;
        }

        BufferedImage getIcon() {
            if (!iconLoaded) {
                iconLoaded = true;
                try {
                    cachedIcon = ItemTex.create(resource);
                } catch (Exception e) {
                    cachedIcon = null;
                }
            }
            return cachedIcon;
        }
    }

    /**
     * List widget for categories.
     */
    private class CategoryList extends SListBox<Category, Widget> {
        private final List<Category> items = new ArrayList<>();
        private Category selected = null;

        CategoryList(Coord sz) {
            super(sz, UI.scale(20));
            items.addAll(categories);
        }

        @Override
        protected List<Category> items() {
            return items;
        }

        @Override
        protected Widget makeitem(Category item, int idx, Coord sz) {
            return new CategoryItemWidget(this, sz, item);
        }

        @Override
        public void change(Category item) {
            selected = item;
            super.change(item);
            // Update item list when category changes
            if (searchBox.text().isEmpty()) {
                List<ItemEntry> categoryItemList = categoryItems.get(item.getName());
                itemList.setItems(categoryItemList != null ? categoryItemList : Collections.emptyList());
            }
        }

        Category getSelected() {
            return selected;
        }
    }

    /**
     * Widget for displaying a category in the list.
     */
    private class CategoryItemWidget extends SListWidget.ItemWidget<Category> {
        private final Text.Line text;

        CategoryItemWidget(SListBox<Category, ?> list, Coord sz, Category item) {
            super(list, sz, item);
            this.text = Text.render(item.getName());
        }

        @Override
        public void draw(GOut g) {
            // Highlight if selected
            if (((CategoryList) list).getSelected() == item) {
                g.chcolor(70, 70, 120, 180);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
            g.image(text.tex(), new Coord(UI.scale(4), (sz.y - text.sz().y) / 2));
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 1) {
                ((CategoryList) list).change(item);
                return true;
            }
            return super.mousedown(ev);
        }
    }

    /**
     * List widget for items.
     */
    private class ItemList extends SListBox<ItemEntry, Widget> {
        private List<ItemEntry> items = new ArrayList<>();
        private ItemEntry selected = null;

        ItemList(Coord sz) {
            super(sz, UI.scale(28));
        }

        @Override
        protected List<ItemEntry> items() {
            return items;
        }

        @Override
        protected Widget makeitem(ItemEntry item, int idx, Coord sz) {
            return new ItemItemWidget(this, sz, item);
        }

        void setItems(List<ItemEntry> newItems) {
            items = new ArrayList<>(newItems);
            selected = null;
            update();
        }

        @Override
        public void change(ItemEntry item) {
            selected = item;
            super.change(item);
            selectItem(item);
        }

        ItemEntry getSelected() {
            return selected;
        }
    }

    /**
     * Widget for displaying an item in the list.
     */
    private class ItemItemWidget extends SListWidget.ItemWidget<ItemEntry> {
        private final Text.Line text;
        private Tex iconTex;

        ItemItemWidget(SListBox<ItemEntry, ?> list, Coord sz, ItemEntry item) {
            super(list, sz, item);
            this.text = Text.render(item.getName());

            // Load icon lazily
            BufferedImage icon = item.getIcon();
            if (icon != null) {
                iconTex = new TexI(icon);
            }
        }

        @Override
        public void draw(GOut g) {
            // Highlight if selected
            if (((ItemList) list).getSelected() == item) {
                g.chcolor(70, 70, 120, 180);
                g.frect(Coord.z, sz);
                g.chcolor();
            }

            int iconSize = UI.scale(24);
            int textX = iconSize + UI.scale(6);

            // Draw icon
            if (iconTex != null) {
                // Scale icon to fit
                Coord texSz = iconTex.sz();
                double scale = Math.min((double) iconSize / texSz.x, (double) iconSize / texSz.y);
                Coord scaledSz = new Coord((int)(texSz.x * scale), (int)(texSz.y * scale));
                Coord iconPos = new Coord(
                    (iconSize - scaledSz.x) / 2 + UI.scale(2),
                    (sz.y - scaledSz.y) / 2
                );
                g.image(iconTex, iconPos, scaledSz);
            }

            // Draw text
            g.image(text.tex(), new Coord(textX, (sz.y - text.sz().y) / 2));
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 1) {
                ((ItemList) list).change(item);
                return true;
            }
            return super.mousedown(ev);
        }
    }
}
