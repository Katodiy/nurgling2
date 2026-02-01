package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.db.dao.StorageItemDao;
import nurgling.db.service.StorageItemService;
import nurgling.i18n.L10n;

import java.awt.Color;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Widget for displaying all storage items from the database.
 * Features:
 * - Sorting by clicking on column headers (Name, Quality, Count)
 * - Grouping modes: None, Quality, Q1, Q5, Q10 (like NInventory)
 * - Min quality filter
 * - Pagination for large item sets
 */
public class NStorageItemsWidget extends Window {

    private static final int PAGE_SIZE = 25;
    private static final int WINDOW_WIDTH = 550;
    private static final int WINDOW_HEIGHT = 500;

    // Column positions
    private static final int COL_NAME = UI.scale(10);
    private static final int COL_QUALITY = UI.scale(320);
    private static final int COL_COUNT = UI.scale(420);

    private int currentPage = 0;
    private List<GroupedItem> allItems = new ArrayList<>();
    private List<GroupedItem> displayedItems = new ArrayList<>();
    
    // Sorting
    private SortColumn currentSortColumn = SortColumn.QUALITY;
    private boolean sortDescending = true;
    
    // Grouping modes (like NInventory)
    public enum Grouping {
        NONE("Type"),
        Q("Quality"),
        Q5("Q 5"),
        Q10("Q 10");
        
        public final String displayName;
        
        Grouping(String displayName) {
            this.displayName = displayName;
        }
    }
    
    public enum SortColumn {
        NAME, QUALITY, COUNT
    }

    private StorageItemsList itemsList;
    private Label pageLabel;
    private Label totalLabel;
    private Dropbox<Grouping> groupingDropbox;
    private TextEntry searchField;
    private TextEntry qualityFilterEntry;
    private String searchText = "";
    private Double minQualityFilter = null;
    private Grouping currentGrouping = Grouping.Q;
    private boolean isLoading = false;
    
    // Clickable column headers
    private Label nameHeaderLabel;
    private Label qualityHeaderLabel;
    private Label countHeaderLabel;

    /**
     * Grouped item representation for display
     */
    public static class GroupedItem {
        public final String name;
        public final double quality; // -1 for type-only grouping (will show range)
        public final double minQuality;
        public final double maxQuality;
        public final int count;
        public final List<StorageItemDao.StorageItemData> items;

        public GroupedItem(String name, double quality, int count, List<StorageItemDao.StorageItemData> items) {
            this.name = name;
            this.quality = quality;
            this.minQuality = items.stream().mapToDouble(StorageItemDao.StorageItemData::getQuality).min().orElse(0);
            this.maxQuality = items.stream().mapToDouble(StorageItemDao.StorageItemData::getQuality).max().orElse(0);
            this.count = count;
            this.items = items;
        }

        public String getQualityDisplay() {
            if (quality >= 0) {
                return Utils.odformat2(quality, 2);
            } else {
                // Range display for type-only grouping
                if (minQuality == maxQuality) {
                    return Utils.odformat2(minQuality, 2);
                }
                return Utils.odformat2(minQuality, 2) + "-" + Utils.odformat2(maxQuality, 2);
            }
        }
    }

    public NStorageItemsWidget() {
        super(UI.scale(new Coord(WINDOW_WIDTH, WINDOW_HEIGHT)), L10n.get("storage.window_title"));

        int y = UI.scale(5);
        int margin = UI.scale(10);

        // Row 1: Search field + Grouping dropdown
        prev = add(new Label(L10n.get("storage.search")), new Coord(margin, y + UI.scale(3)));
        searchField = add(new TextEntry(UI.scale(120), "") {
            @Override
            public boolean keydown(KeyDownEvent e) {
                boolean res = super.keydown(e);
                searchText = text().toLowerCase();
                applyFiltersAndSort();
                return res;
            }
        }, new Coord(UI.scale(60), y));

        // Grouping dropdown
        int groupingX = UI.scale(195);
        groupingDropbox = add(new Dropbox<Grouping>(UI.scale(80), Grouping.values().length, UI.scale(16)) {
            @Override
            protected Grouping listitem(int i) {
                return Grouping.values()[i];
            }

            @Override
            protected int listitems() {
                return Grouping.values().length;
            }

            @Override
            protected void drawitem(GOut g, Grouping item, int i) {
                g.text(item.displayName, Coord.z);
            }

            @Override
            public void change(Grouping item) {
                super.change(item);
                currentGrouping = item;
                processItems();
            }
        }, new Coord(groupingX, y));
        // Don't call change() here - it triggers processItems before all widgets are created
        // Set selection directly instead
        groupingDropbox.sel = currentGrouping;
        groupingDropbox.settip(L10n.get("storage.grouping_tip"));

        // Quality filter
        int qualityX = UI.scale(290);
        add(new Label("Q>="), new Coord(qualityX, y + UI.scale(3)));
        qualityFilterEntry = add(new TextEntry(UI.scale(40), "") {
            @Override
            public void changed() {
                super.changed();
                parseQualityFilter();
                applyFiltersAndSort();
            }
        }, new Coord(qualityX + UI.scale(28), y));
        qualityFilterEntry.settip(L10n.get("storage.quality_filter_tip"));

        // Refresh button
        add(new Button(UI.scale(70), L10n.get("storage.refresh")) {
            @Override
            public void click() {
                loadItems();
            }
        }, new Coord(UI.scale(WINDOW_WIDTH - 90), y));

        y += UI.scale(30);

        // Column headers (clickable for sorting)
        int headerY = y;
        nameHeaderLabel = add(new Label(L10n.get("storage.col_name") + " ▼") {
            @Override
            public boolean mousedown(MouseDownEvent ev) {
                if (ev.b == 1) {
                    toggleSort(SortColumn.NAME);
                    return true;
                }
                return super.mousedown(ev);
            }
        }, new Coord(COL_NAME, headerY));
        
        qualityHeaderLabel = add(new Label(L10n.get("storage.col_quality") + " ▼") {
            @Override
            public boolean mousedown(MouseDownEvent ev) {
                if (ev.b == 1) {
                    toggleSort(SortColumn.QUALITY);
                    return true;
                }
                return super.mousedown(ev);
            }
        }, new Coord(COL_QUALITY, headerY));
        
        countHeaderLabel = add(new Label(L10n.get("storage.col_count") + " ▼") {
            @Override
            public boolean mousedown(MouseDownEvent ev) {
                if (ev.b == 1) {
                    toggleSort(SortColumn.COUNT);
                    return true;
                }
                return super.mousedown(ev);
            }
        }, new Coord(COL_COUNT, headerY));
        
        updateHeaderLabels();

        y += UI.scale(20);

        // Items list
        itemsList = add(new StorageItemsList(UI.scale(new Coord(WINDOW_WIDTH - 20, WINDOW_HEIGHT - 120))),
                new Coord(UI.scale(5), y));

        // Pagination controls at bottom
        int bottomY = UI.scale(WINDOW_HEIGHT - 45);

        prev = add(new Button(UI.scale(50), "<<") {
            @Override
            public void click() {
                if (currentPage > 0) {
                    currentPage--;
                    updateDisplayedItems();
                }
            }
        }, new Coord(UI.scale(WINDOW_WIDTH / 2 - 80), bottomY));

        pageLabel = add(new Label(""), new Coord(UI.scale(WINDOW_WIDTH / 2 - 20), bottomY + UI.scale(5)));

        add(new Button(UI.scale(50), ">>") {
            @Override
            public void click() {
                int maxPage = getMaxPage();
                if (currentPage < maxPage) {
                    currentPage++;
                    updateDisplayedItems();
                }
            }
        }, new Coord(UI.scale(WINDOW_WIDTH / 2 + 30), bottomY));

        totalLabel = add(new Label(""), new Coord(UI.scale(10), bottomY + UI.scale(5)));

        pack();
    }
    
    private void toggleSort(SortColumn column) {
        if (currentSortColumn == column) {
            sortDescending = !sortDescending;
        } else {
            currentSortColumn = column;
            sortDescending = true;
        }
        updateHeaderLabels();
        applyFiltersAndSort();
    }
    
    private void updateHeaderLabels() {
        if (nameHeaderLabel == null || qualityHeaderLabel == null || countHeaderLabel == null) {
            return; // Not yet initialized
        }
        
        String nameText = L10n.get("storage.col_name");
        String qualityText = L10n.get("storage.col_quality");
        String countText = L10n.get("storage.col_count");
        
        String arrow = sortDescending ? " ▼" : " ▲";
        
        nameHeaderLabel.settext(nameText + (currentSortColumn == SortColumn.NAME ? arrow : ""));
        qualityHeaderLabel.settext(qualityText + (currentSortColumn == SortColumn.QUALITY ? arrow : ""));
        countHeaderLabel.settext(countText + (currentSortColumn == SortColumn.COUNT ? arrow : ""));
    }
    
    private void parseQualityFilter() {
        if (qualityFilterEntry == null) {
            minQualityFilter = null;
            return;
        }
        String text = qualityFilterEntry.text().trim();
        if (text.isEmpty()) {
            minQualityFilter = null;
            return;
        }
        try {
            minQualityFilter = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            minQualityFilter = null;
        }
    }

    private int getMaxPage() {
        return Math.max(0, (displayedItems.size() + PAGE_SIZE - 1) / PAGE_SIZE - 1);
    }

    private void updatePageLabel() {
        if (pageLabel == null || totalLabel == null) {
            return; // Not yet initialized
        }
        int maxPage = getMaxPage();
        pageLabel.settext((currentPage + 1) + " / " + (maxPage + 1));
        totalLabel.settext(L10n.get("storage.total_items", displayedItems.size()));
    }

    @Override
    public boolean show(boolean show) {
        if (show && (Boolean) NConfig.get(NConfig.Key.ndbenable) &&
                ui != null && ui.core != null && ui.core.databaseManager != null &&
                ui.core.databaseManager.isReady()) {
            loadItems();
        }
        return super.show(show);
    }

    private void loadItems() {
        if (isLoading) return;
        if (ui == null || ui.core == null || ui.core.databaseManager == null ||
                !ui.core.databaseManager.isReady()) {
            NUtils.getGameUI().msg(L10n.get("storage.db_not_ready"), Color.RED);
            return;
        }

        isLoading = true;
        StorageItemService storageService = new StorageItemService(ui.core.databaseManager);

        storageService.loadAllStorageItemsAsync()
            .thenAccept(items -> {
                // Filter out items with negative quality (shouldn't be in DB, but just in case)
                List<StorageItemDao.StorageItemData> validItems = items.stream()
                    .filter(item -> item.getQuality() >= 0)
                    .collect(Collectors.toList());
                processLoadedItems(validItems);
                isLoading = false;
            })
            .exceptionally(e -> {
                e.printStackTrace();
                isLoading = false;
                return null;
            });
    }

    private void processLoadedItems(List<StorageItemDao.StorageItemData> items) {
        this.rawItems = items;
        processItems();
    }

    private List<StorageItemDao.StorageItemData> rawItems = new ArrayList<>();

    private void processItems() {
        if (rawItems == null || rawItems.isEmpty()) {
            allItems = new ArrayList<>();
            applyFiltersAndSort();
            return;
        }

        Map<String, List<StorageItemDao.StorageItemData>> grouped;

        switch (currentGrouping) {
            case NONE:
                // Group only by name
                grouped = rawItems.stream()
                        .collect(Collectors.groupingBy(StorageItemDao.StorageItemData::getName));
                break;
            case Q:
                // Group by name + exact quality (rounded to 2 decimals)
                grouped = rawItems.stream()
                        .collect(Collectors.groupingBy(item ->
                                item.getName() + "|" + String.format("%.2f", item.getQuality())));
                break;
            case Q5:
                // Group by name + quality rounded to 5
                grouped = rawItems.stream()
                        .collect(Collectors.groupingBy(item ->
                                item.getName() + "|" + ((int) Math.floor(item.getQuality() / 5) * 5)));
                break;
            case Q10:
                // Group by name + quality rounded to 10
                grouped = rawItems.stream()
                        .collect(Collectors.groupingBy(item ->
                                item.getName() + "|" + ((int) Math.floor(item.getQuality() / 10) * 10)));
                break;
            default:
                grouped = rawItems.stream()
                        .collect(Collectors.groupingBy(StorageItemDao.StorageItemData::getName));
        }

        List<GroupedItem> result = new ArrayList<>();

        for (Map.Entry<String, List<StorageItemDao.StorageItemData>> entry : grouped.entrySet()) {
            List<StorageItemDao.StorageItemData> itemGroup = entry.getValue();
            if (itemGroup.isEmpty()) continue;

            StorageItemDao.StorageItemData first = itemGroup.get(0);
            
            // For non-exact grouping, use -1 to indicate range display
            double quality;
            if (currentGrouping == Grouping.NONE) {
                quality = -1;
            } else if (currentGrouping == Grouping.Q) {
                quality = first.getQuality();
            } else {
                // For Q1, Q5, Q10 - show range
                quality = -1;
            }

            result.add(new GroupedItem(
                    first.getName(),
                    quality,
                    itemGroup.size(),
                    itemGroup
            ));
        }

        allItems = result;
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        // Apply search filter
        List<GroupedItem> filtered = allItems;
        
        if (searchText != null && !searchText.isEmpty()) {
            filtered = filtered.stream()
                    .filter(item -> item.name.toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
        }
        
        // Apply quality filter
        if (minQualityFilter != null) {
            filtered = filtered.stream()
                    .filter(item -> item.maxQuality >= minQualityFilter)
                    .collect(Collectors.toList());
        }
        
        displayedItems = new ArrayList<>(filtered);

        // Sort
        Comparator<GroupedItem> comparator;
        switch (currentSortColumn) {
            case NAME:
                comparator = Comparator.comparing(a -> a.name.toLowerCase());
                break;
            case COUNT:
                comparator = Comparator.comparingInt(a -> a.count);
                break;
            case QUALITY:
            default:
                comparator = Comparator.comparingDouble(a -> a.quality >= 0 ? a.quality : a.maxQuality);
                break;
        }
        
        if (sortDescending) {
            comparator = comparator.reversed();
        }
        
        displayedItems.sort(comparator);

        currentPage = 0;
        updateDisplayedItems();
    }

    private void updateDisplayedItems() {
        if (itemWidgets == null) {
            return; // Not yet initialized
        }
        
        int startIdx = currentPage * PAGE_SIZE;
        int endIdx = Math.min(startIdx + PAGE_SIZE, displayedItems.size());

        itemWidgets.clear();
        for (int i = startIdx; i < endIdx; i++) {
            itemWidgets.add(new StorageItemWidget(displayedItems.get(i)));
        }

        updatePageLabel();
    }

    private final ArrayList<StorageItemWidget> itemWidgets = new ArrayList<>();

    /**
     * Widget for displaying a single grouped item
     */
    public class StorageItemWidget extends Widget {
        private final GroupedItem item;

        public StorageItemWidget(GroupedItem item) {
            this.item = item;
            sz = UI.scale(new Coord(WINDOW_WIDTH - 40, 20));

            add(new Label(truncateName(item.name, 38)), new Coord(COL_NAME, 0));
            add(new Label(item.getQualityDisplay()), new Coord(COL_QUALITY, 0));
            add(new Label(String.valueOf(item.count)), new Coord(COL_COUNT, 0));
        }

        private String truncateName(String name, int maxLen) {
            if (name == null) return "";
            if (name.length() <= maxLen) return name;
            return name.substring(0, maxLen - 3) + "...";
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 1) {
                showItemDetails();
                return true;
            }
            return super.mousedown(ev);
        }

        private void showItemDetails() {
            StringBuilder sb = new StringBuilder();
            sb.append(item.name).append("\n");
            sb.append(L10n.get("storage.quality")).append(": ").append(item.getQualityDisplay()).append("\n");
            sb.append(L10n.get("storage.count")).append(": ").append(item.count);
            NUtils.getGameUI().msg(sb.toString());
        }
    }

    /**
     * List widget for displaying storage items
     */
    public class StorageItemsList extends SListBox<StorageItemWidget, Widget> {
        private final Color bg = new Color(30, 40, 40, 160);

        public StorageItemsList(Coord sz) {
            super(sz, UI.scale(22));
        }

        @Override
        protected List<StorageItemWidget> items() {
            synchronized (itemWidgets) {
                return itemWidgets;
            }
        }

        @Override
        protected Widget makeitem(StorageItemWidget item, int idx, Coord sz) {
            return new ItemWidget<StorageItemWidget>(this, sz, item) {
                {
                    add(item);
                }

                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    super.mousedown(ev);
                    return super.mousedown(ev);
                }
            };
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            g.chcolor();
            super.draw(g);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
