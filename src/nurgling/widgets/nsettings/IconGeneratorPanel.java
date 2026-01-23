package nurgling.widgets.nsettings;

import haven.*;
import haven.res.lib.itemtex.ItemTex;
import nurgling.tools.VSpec;
import nurgling.widgets.CustomIcon;
import nurgling.widgets.CustomIconGenerator;
import nurgling.widgets.CustomIconGenerator.IconBackground;
import nurgling.widgets.CustomIconManager;
import nurgling.widgets.SavedIconsWindow;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Settings panel for generating and managing custom icons.
 * Allows users to select a background and an in-game item
 * to create custom bot-style icons that can be used for scenarios and equipment presets.
 */
public class IconGeneratorPanel extends Panel {

    private static final int MARGIN = UI.scale(15);
    private static final int SWATCH_SIZE = UI.scale(32);
    private static final int SWATCH_GAP = UI.scale(5);
    private static final int LINE_HEIGHT = UI.scale(24);

    // Currently editing icon
    private CustomIcon editingIcon = null;

    // Icon name
    private TextEntry iconNameEntry;

    // Background selection
    private IconBackground selectedBackground = CustomIconGenerator.PRESET_BACKGROUNDS.get(0);
    private final List<BackgroundSwatch> backgroundSwatches = new ArrayList<>();

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
    private int previewIconsY = 0;

    // Buttons
    private Button saveNewBtn;
    private Button saveBtn;
    private Button deleteBtn;

    public IconGeneratorPanel() {
        super();

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
        int y = UI.scale(10);

        // ==================== Saved Icons Section ====================
        add(new Label("Saved Icons"), MARGIN, y);
        y += LINE_HEIGHT;

        // Buttons on their own row, aligned
        int topBtnWidth = UI.scale(100);
        int topBtnGap = UI.scale(10);
        add(new Button(topBtnWidth, "Browse Icons") {
            @Override
            public void click() {
                openSavedIconsWindow();
            }
        }, MARGIN, y);

        add(new Button(topBtnWidth, "New Icon") {
            @Override
            public void click() {
                startNewIcon();
            }
        }, MARGIN + topBtnWidth + topBtnGap, y);

        y += UI.scale(28) + UI.scale(10);

        // ==================== Icon Name ====================
        add(new Label("Name:"), MARGIN, y + UI.scale(3));
        iconNameEntry = add(new TextEntry(UI.scale(200), ""), MARGIN + UI.scale(50), y);
        y += LINE_HEIGHT + UI.scale(15);

        // ==================== Background Selection ====================
        add(new Label("Background:"), MARGIN, y);
        y += LINE_HEIGHT;

        // Background swatches - 8 per row
        int swatchX = MARGIN;
        int swatchesPerRow = 8;
        List<IconBackground> backgrounds = CustomIconGenerator.PRESET_BACKGROUNDS;
        for (int i = 0; i < backgrounds.size(); i++) {
            IconBackground bg = backgrounds.get(i);
            BackgroundSwatch swatch = new BackgroundSwatch(bg);
            add(swatch, new Coord(swatchX, y));
            backgroundSwatches.add(swatch);

            if ((i + 1) % swatchesPerRow == 0) {
                swatchX = MARGIN;
                y += SWATCH_SIZE + SWATCH_GAP;
            } else {
                swatchX += SWATCH_SIZE + SWATCH_GAP;
            }
        }
        // If last row wasn't complete, move y forward
        if (backgrounds.size() % swatchesPerRow != 0) {
            y += SWATCH_SIZE + SWATCH_GAP;
        }
        y += UI.scale(10);

        // ==================== Item Selection Section ====================
        add(new Label("Item (optional):"), MARGIN, y);
        y += LINE_HEIGHT;

        // Category list (left side)
        int catListWidth = UI.scale(150);
        int catListHeight = UI.scale(140);
        categoryList = add(new CategoryList(new Coord(catListWidth, catListHeight)), new Coord(MARGIN, y));

        // Item list (right side)
        int itemListX = MARGIN + catListWidth + UI.scale(15);

        // Search box
        add(new Label("Search:"), itemListX, y + UI.scale(3));
        int searchWidth = UI.scale(200);
        searchBox = add(new TextEntry(searchWidth, "") {
            @Override
            public void changed() {
                filterItems();
            }
        }, new Coord(itemListX + UI.scale(55), y));

        int itemListWidth = UI.scale(255);
        int itemListHeight = UI.scale(115);
        itemList = add(new ItemList(new Coord(itemListWidth, itemListHeight)),
            new Coord(itemListX, y + searchBox.sz.y + UI.scale(5)));

        y += catListHeight + UI.scale(15);

        // ==================== Preview Section ====================
        add(new Label("Preview:"), MARGIN, y);
        y += LINE_HEIGHT;

        // Store where preview icons will be drawn
        previewIconsY = y;
        int iconSize = UI.scale(32);

        // State labels centered under each icon
        int previewX = MARGIN;
        int previewSpacing = UI.scale(50);
        String[] labels = {"Up", "Down", "Hover"};
        for (int i = 0; i < labels.length; i++) {
            Text.Line labelText = Text.render(labels[i]);
            int labelWidth = labelText.sz().x;
            int iconCenterX = previewX + (previewSpacing * i) + (iconSize / 2);
            int labelX = iconCenterX - (labelWidth / 2);
            add(new Label(labels[i]), new Coord(labelX, y + iconSize + UI.scale(4)));
        }

        // ==================== Action Buttons ====================
        // All three buttons on the same row, aligned with item list area
        int btnWidth = UI.scale(80);
        int btnGap = UI.scale(8);
        int btnX = itemListX;  // Align with item list / search area

        saveNewBtn = add(new Button(btnWidth, "Save New") {
            @Override
            public void click() {
                saveAsNew();
            }
        }, new Coord(btnX, y));

        saveBtn = add(new Button(btnWidth, "Save") {
            @Override
            public void click() {
                saveIcon();
            }
        }, new Coord(btnX + btnWidth + btnGap, y));

        deleteBtn = add(new Button(btnWidth, "Delete") {
            @Override
            public void click() {
                deleteIcon();
            }
        }, new Coord(btnX + (btnWidth + btnGap) * 2, y));

        // Add padding at bottom so buttons aren't cut off
        y += UI.scale(35);

        updateButtonStates();
        pack();
    }

    private void openSavedIconsWindow() {
        SavedIconsWindow window = new SavedIconsWindow(this::editIcon);
        ui.gui.add(window, ui.gui.sz.div(2).sub(window.sz.div(2)));
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);

        // Draw selection highlight on background swatches
        for (BackgroundSwatch swatch : backgroundSwatches) {
            if (swatch.background == selectedBackground) {
                Coord pos = swatch.c.sub(2, 2);
                Coord size = swatch.sz.add(4, 4);
                g.chcolor(255, 255, 255, 255);
                g.rect(pos, size);
                g.chcolor();
            }
        }

        // Draw preview icons
        if (previewTextures != null) {
            int previewX = MARGIN;
            int spacing = UI.scale(50);

            for (int i = 0; i < previewTextures.length; i++) {
                if (previewTextures[i] != null) {
                    g.image(previewTextures[i], new Coord(previewX + spacing * i, previewIconsY));
                }
            }
        }
    }

    private void startNewIcon() {
        editingIcon = null;
        iconNameEntry.settext("");
        selectedBackground = CustomIconGenerator.PRESET_BACKGROUNDS.get(0);
        selectedItem = null;
        if (searchBox != null) {
            searchBox.settext("");
        }
        updatePreview();
        updateButtonStates();
    }

    private void editIcon(CustomIcon icon) {
        editingIcon = icon;
        iconNameEntry.settext(icon.getName());

        // Find and select the background
        String bgId = icon.getBackgroundId();
        for (IconBackground bg : CustomIconGenerator.PRESET_BACKGROUNDS) {
            if (bg.getId().equals(bgId)) {
                selectedBackground = bg;
                break;
            }
        }

        // Find and select the item if available
        selectedItem = null;
        JSONObject itemRes = icon.getItemResource();
        if (itemRes != null && itemRes.has("name")) {
            String itemName = itemRes.getString("name");
            for (ItemEntry entry : allItems) {
                if (entry.getName().equals(itemName)) {
                    selectedItem = entry;
                    break;
                }
            }
        }

        if (searchBox != null) {
            searchBox.settext("");
        }
        updatePreview();
        updateButtonStates();
    }

    private void saveAsNew() {
        String name = iconNameEntry.text().trim();
        if (name.isEmpty()) {
            name = "Icon " + (CustomIconManager.getInstance().getIconList().size() + 1);
        }

        JSONObject itemRes = selectedItem != null ? selectedItem.getResource() : null;
        CustomIcon newIcon = new CustomIcon(name, selectedBackground.getId(), itemRes);
        CustomIconManager.getInstance().addOrUpdateIcon(newIcon);

        editingIcon = newIcon;
        updateButtonStates();
    }

    private void saveIcon() {
        if (editingIcon == null) {
            saveAsNew();
            return;
        }

        String name = iconNameEntry.text().trim();
        if (name.isEmpty()) {
            name = "Unnamed";
        }

        editingIcon.setName(name);
        editingIcon.setBackgroundId(selectedBackground.getId());
        editingIcon.setItemResource(selectedItem != null ? selectedItem.getResource() : null);
        editingIcon.invalidateCache();

        CustomIconManager.getInstance().addOrUpdateIcon(editingIcon);
        updateButtonStates();
    }

    private void deleteIcon() {
        if (editingIcon != null) {
            CustomIconManager.getInstance().deleteIcon(editingIcon.getId());
            editingIcon = null;
            startNewIcon();
        }
    }

    private void updateButtonStates() {
        saveBtn.show(editingIcon != null);
        deleteBtn.show(editingIcon != null);
    }

    private void selectBackground(IconBackground background) {
        selectedBackground = background;
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
        previewIcons = CustomIconGenerator.generateIconSet(selectedBackground, itemRes);

        previewTextures = new Tex[3];
        for (int i = 0; i < 3; i++) {
            if (previewIcons[i] != null) {
                previewTextures[i] = new TexI(previewIcons[i]);
            }
        }
    }

    @Override
    public void load() {
        startNewIcon();
    }

    @Override
    public void save() {
        // Icons are saved immediately when Save button is clicked
    }

    // ==================== Inner Classes ====================

    /**
     * A clickable background swatch widget.
     */
    private class BackgroundSwatch extends Widget {
        final IconBackground background;
        private Tex previewTex;

        BackgroundSwatch(IconBackground background) {
            super(new Coord(SWATCH_SIZE, SWATCH_SIZE));
            this.background = background;

            BufferedImage preview = background.getPreview();
            if (preview != null) {
                previewTex = new TexI(preview);
            }
        }

        @Override
        public void draw(GOut g) {
            if (previewTex != null) {
                g.image(previewTex, Coord.z, sz);
            } else {
                g.chcolor(128, 128, 128, 255);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 1) {
                selectBackground(background);
                return true;
            }
            return super.mousedown(ev);
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

            BufferedImage icon = item.getIcon();
            if (icon != null) {
                iconTex = new TexI(icon);
            }
        }

        @Override
        public void draw(GOut g) {
            if (((ItemList) list).getSelected() == item) {
                g.chcolor(70, 70, 120, 180);
                g.frect(Coord.z, sz);
                g.chcolor();
            }

            int iconSize = UI.scale(24);
            int textX = iconSize + UI.scale(6);

            if (iconTex != null) {
                Coord texSz = iconTex.sz();
                double scale = Math.min((double) iconSize / texSz.x, (double) iconSize / texSz.y);
                Coord scaledSz = new Coord((int)(texSz.x * scale), (int)(texSz.y * scale));
                Coord iconPos = new Coord(
                    (iconSize - scaledSz.x) / 2 + UI.scale(2),
                    (sz.y - scaledSz.y) / 2
                );
                g.image(iconTex, iconPos, scaledSz);
            }

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
