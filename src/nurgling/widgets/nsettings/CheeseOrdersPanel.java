package nurgling.widgets.nsettings;

import haven.*;
import haven.res.lib.itemtex.ItemTex;
import nurgling.cheese.CheeseBranch;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.NUtils;
import nurgling.tools.VSpec;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class CheeseOrdersPanel extends Panel {
    private final CheeseOrdersManager manager = new CheeseOrdersManager();
    private final int margin = UI.scale(10);

    private final Widget listPanel;
    private Widget editorPanel = null;

    private Scrollport orderListContainer;
    private Widget orderListContent;
    private final List<String> cheeseTypes = CheeseBranch.allProducts();
    
    private final Set<Integer> expandedOrders = new HashSet<>();

    // --- Editor Panel widgets ---
    private Dropbox<String> cheeseTypeDropdown;
    private TextEntry countEntry;
    
    // Icon cache for cheese types
    private final Map<String, BufferedImage> cheeseIcons = new HashMap<>();
    // Track failed icon loading attempts to avoid repeated failures
    private final Set<String> failedIcons = new HashSet<>();
    
    // File watching for real-time updates
    private Thread watchThread;
    private volatile boolean watchingEnabled = false;
    private WatchService watchService;
    private WatchKey watchKey;

    public CheeseOrdersPanel() {
        super("");

        int btnWidth = UI.scale(120);
        int btnHeight = UI.scale(28);
        int titleY = UI.scale(40);

        int contentWidth = sz.x - margin * 2;
        int contentHeight = sz.y - titleY;
        int olistHeight = UI.scale(400);

        // -------- List Panel -----------
        listPanel = add(new Widget(new Coord(contentWidth, contentHeight)), new Coord(margin, margin));
        listPanel.add(new Label("Cheese Orders:"), new Coord(0, 0));

        int olistWidth = contentWidth - margin * 2;
        
        // Create scrollable container for the order list
        orderListContainer = listPanel.add(new Scrollport(new Coord(olistWidth, olistHeight)), new Coord(margin, margin + UI.scale(32)));
        orderListContent = new Widget(new Coord(olistWidth, UI.scale(50))) {
            @Override
            public void pack() {
                // Auto-resize based on children
                resize(contentsz());
            }
        };
        orderListContainer.cont.add(orderListContent, Coord.z);
        
        rebuildOrderList();

        int bottomY = contentHeight - margin - btnHeight;

        // "Add Order" button
        listPanel.add(
                new Button(btnWidth, "Add Order", this::showEditorPanel),
                new Coord((contentWidth - btnWidth) / 2, bottomY - btnHeight - UI.scale(8))
        );

        // -------- Editor Panel -----------
        editorPanel = add(new Widget(new Coord(contentWidth, contentHeight)), new Coord(margin, margin));
        editorPanel.hide();

        int y = margin;
        editorPanel.add(new Label("Add Cheese Order:"), new Coord(0, 0));
        y += UI.scale(22);

        cheeseTypeDropdown = editorPanel.add(new Dropbox<String>(UI.scale(200), cheeseTypes.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) { return cheeseTypes.get(i); }
            @Override
            protected int listitems() { return cheeseTypes.size(); }
            @Override
            protected void drawitem(GOut g, String item, int i) { g.text(item, Coord.z); }
        }, new Coord(margin, y));
        cheeseTypeDropdown.sel = cheeseTypes.get(0);

        y += UI.scale(40);

        editorPanel.add(new Label("Quantity:"), new Coord(margin, y + UI.scale(6)));
        countEntry = editorPanel.add(new TextEntry(UI.scale(60), "1"), new Coord(margin + UI.scale(120), y));
        y += UI.scale(40);

        int editorBtnY = contentHeight - margin - btnHeight;

        // "Save" and "Cancel" buttons
        editorPanel.add(
                new Button(btnWidth, "Save", this::saveOrder),
                new Coord((contentWidth - btnWidth * 2 - UI.scale(20)) / 2, editorBtnY)
        );
        editorPanel.add(
                new Button(btnWidth, "Cancel", this::showListPanel),
                new Coord((contentWidth - btnWidth * 2 - UI.scale(20)) / 2 + btnWidth + UI.scale(20), editorBtnY)
        );

        editorPanel.pack();
        pack();

        showListPanel();
    }

    private void showListPanel() {
        listPanel.show();
        editorPanel.hide();
        rebuildOrderList();
    }

    private void showEditorPanel() {
        listPanel.hide();
        editorPanel.show();
        cheeseTypeDropdown.sel = cheeseTypes.get(0);
        countEntry.settext("1");
    }

    private void saveOrder() {
        String cheeseType = cheeseTypeDropdown.sel != null ? cheeseTypeDropdown.sel : cheeseTypes.get(0);
        int count;
        try {
            count = Integer.parseInt(countEntry.text().trim());
        } catch (NumberFormatException e) {
            NUtils.getGameUI().msg("Enter a valid number for count.");
            countEntry.settext("1");
            return;
        }
        if (count <= 0) {
            NUtils.getGameUI().msg("Enter a positive count.");
            return;
        }

        List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(cheeseType);
        if (chain == null || chain.isEmpty()) {
            NUtils.getGameUI().msg("Could not find a recipe chain for: " + cheeseType);
            return;
        }
        CheeseBranch.Cheese firstStep = chain.get(0);

        // Check for existing order of same cheese type and merge if found
        CheeseOrder existingOrder = null;
        for (CheeseOrder order : manager.getOrders().values()) {
            if (order.getCheeseType().equals(cheeseType)) {
                existingOrder = order;
                break;
            }
        }

        if (existingOrder != null) {
            // Merge with existing order: add to start step and update total count
            CheeseOrder.StepStatus startStep = null;
            for (CheeseOrder.StepStatus step : existingOrder.getStatus()) {
                if (step.name.equals(firstStep.name) && step.place.equals(firstStep.place.name())) {
                    startStep = step;
                    break;
                }
            }

            if (startStep != null) {
                startStep.left += count;
            } else {
                // Start step missing (shouldn't happen), create it at beginning
                existingOrder.getStatus().add(0, new CheeseOrder.StepStatus(firstStep.name, firstStep.place.name(), count));
            }

            existingOrder.addToCount(count);
            manager.addOrUpdateOrder(existingOrder);
            manager.writeOrders();

            showListPanel();
            NUtils.getGameUI().msg("Added " + count + " to existing " + cheeseType + " order (total: " + existingOrder.getCount() + ")");
        } else {
            // Create new order
            int id = getNextOrderId();
            List<CheeseOrder.StepStatus> status = new ArrayList<>();
            status.add(new CheeseOrder.StepStatus(firstStep.name, firstStep.place.name(), count));

            CheeseOrder order = new CheeseOrder(id, cheeseType, count, status);
            manager.addOrUpdateOrder(order);
            manager.writeOrders();

            showListPanel();
            NUtils.getGameUI().msg("Order added: " + cheeseType + " x" + count);
        }
    }

    private int getNextOrderId() {
        if (manager.getOrders().isEmpty()) return 1;
        return manager.getOrders().keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }
    
    private Widget createOrderWidget(CheeseOrder order, Coord sz) {
        boolean isExpanded = expandedOrders.contains(order.getId());
        List<CheeseOrder.StepStatus> steps = order.getStatus();
        
        // Calculate height
        int baseHeight = UI.scale(32);
        int stepCount = (isExpanded && steps != null) ? steps.size() : 0;
        int headerHeight = (isExpanded && stepCount > 0) ? UI.scale(18) : 0;
        int stepLineHeight = UI.scale(24); // Increased from 20 to 24 for better icon display
        int totalHeight = baseHeight + headerHeight + (stepCount * stepLineHeight) + (isExpanded ? UI.scale(5) : 0);
        
        Widget w = new Widget(new Coord(sz.x, totalHeight));
        
        // Column widths for alignment
        int expandBtnW = UI.scale(30);
        int iconColW = UI.scale(25);
        int statusColW = UI.scale(50);
        int nameColW = UI.scale(115);
        int locationColW = UI.scale(80);
        int progressColW = UI.scale(120);
        int btnW = UI.scale(70);
        int rightPad = UI.scale(10);
        
        // Column positions
        int expandBtnX = margin;
        int iconColX = expandBtnX + expandBtnW + UI.scale(5);
        int statusColX = iconColX + iconColW + UI.scale(5);
        int nameColX = statusColX + statusColW;
        int locationColX = nameColX + nameColW;
        int progressColX = locationColX + locationColW;
        int delBtnX = sz.x - rightPad - btnW;
        
        // Main row background (subtle)
        w.add(new Widget(new Coord(sz.x - margin * 2, baseHeight)) {
            public void draw(GOut g) {
                g.chcolor(255, 255, 255, 8);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
        }, new Coord(margin, 0));
        
        // Expand/collapse button
        String expandBtnText = isExpanded ? "▼" : "▶";
        Button expandBtn = new Button(expandBtnW, expandBtnText, () -> {
            if (expandedOrders.contains(order.getId())) {
                expandedOrders.remove(order.getId());
            } else {
                expandedOrders.add(order.getId());
            }
            rebuildOrderList();
        });
        w.add(expandBtn, new Coord(expandBtnX, (baseHeight - expandBtn.sz.y) / 2));
        
        // Cheese icon
        BufferedImage cheeseIcon = getCheeseIcon(order.getCheeseType());
        if (cheeseIcon != null) {
            final TexI iconTex = new TexI(cheeseIcon);
            w.add(new Widget(new Coord(UI.scale(20), UI.scale(20))) {
                public void draw(GOut g) {
                    g.image(iconTex, Coord.z);
                }
            }, new Coord(iconColX + UI.scale(2), (baseHeight - UI.scale(20)) / 2));
        } else {
            System.out.println("cheeseIcon is null for: " + order.getCheeseType()); // Debug
        }
        
        // Order title (cheese type)
        Label orderLabel = new Label(order.getCheeseType());
        w.add(orderLabel, new Coord(nameColX, (baseHeight - orderLabel.sz.y) / 2));
        
        // Quantity
        Label qtyLabel = new Label("x" + order.getCount());
        w.add(qtyLabel, new Coord(progressColX, (baseHeight - qtyLabel.sz.y) / 2));
        
        // Delete button
        w.add(new Button(btnW, "Delete", () -> {
            manager.deleteOrder(order.getId());
            manager.writeOrders();
            expandedOrders.remove(order.getId());
            rebuildOrderList();
        }), new Coord(delBtnX, (baseHeight - UI.scale(28)) / 2));
        
        // Step details (if expanded)
        if (isExpanded && steps != null && !steps.isEmpty()) {
            int stepY = baseHeight + UI.scale(5);
            
            // Header row for steps
            Label headerStatus = new Label("Status");
            Label headerName = new Label("Cheese Type");
            Label headerLocation = new Label("Location");
            Label headerProgress = new Label("Progress");
            
            w.add(headerStatus, new Coord(statusColX, stepY));
            w.add(headerName, new Coord(nameColX, stepY));
            w.add(headerLocation, new Coord(locationColX, stepY));
            w.add(headerProgress, new Coord(progressColX, stepY));
            
            stepY += UI.scale(18);
            
            // Step rows
            for (CheeseOrder.StepStatus step : steps) {
                // Status icon (simple text indicator)
                String statusIcon = step.left == 0 ? "✓" : "○";
                Label statusLabel = new Label(statusIcon);
                w.add(statusLabel, new Coord(statusColX, stepY + UI.scale(4))); // Centered vertically
                
                // Cheese icon + name (icon as part of name column)
                BufferedImage stepIcon = getCheeseIcon(step.name);
                if (stepIcon != null) {
                    final TexI stepIconTex = new TexI(stepIcon);
                    // Add icon
                    w.add(new Widget(new Coord(UI.scale(18), UI.scale(18))) {
                        public void draw(GOut g) {
                            g.image(stepIconTex, Coord.z, new Coord(UI.scale(18), UI.scale(18)));
                        }
                    }, new Coord(nameColX, stepY + UI.scale(3))); // Slightly offset for better alignment
                    
                    // Add name label next to icon
                    Label nameLabel = new Label(step.name);
                    w.add(nameLabel, new Coord(nameColX + UI.scale(22), stepY + UI.scale(4)));
                } else {
                    // No icon, just name
                    Label nameLabel = new Label(step.name);
                    w.add(nameLabel, new Coord(nameColX, stepY + UI.scale(4)));
                }
                
                // Location
                Label locationLabel = new Label(step.place);
                w.add(locationLabel, new Coord(locationColX, stepY + UI.scale(4)));
                
                // Progress
                String progressText;
                if (step.left > 0) {
                    progressText = step.left + " remaining";
                } else {
                    progressText = "Complete";
                }
                Label progressLabel = new Label(progressText);
                w.add(progressLabel, new Coord(progressColX, stepY + UI.scale(4)));
                
                stepY += stepLineHeight;
            }
        }
        
        return w;
    }
    
    @Override
    public void show() {
        // Refresh orders from disk when panel becomes visible
        manager.loadOrders();
        rebuildOrderList();
        
        // Start file watching when panel becomes visible
        startFileWatching();

        super.show();
    }
    
    @Override
    public void hide() {
        // Stop file watching when panel is hidden
        stopFileWatching();
        super.hide();
    }
    
    @Override
    public void destroy() {
        // Stop file watching when panel is destroyed
        stopFileWatching();
        super.destroy();
    }
    
    private void startFileWatching() {
        try {
            Path filePath = Paths.get(manager.getOrdersFilePath());
            Path directory = filePath.getParent();
            
            if (directory != null && Files.exists(directory)) {
                watchService = FileSystems.getDefault().newWatchService();
                watchKey = directory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                
                watchingEnabled = true;
                watchThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String ordersFileName = filePath.getFileName().toString();
                        
                        while (watchingEnabled) {
                            try {
                                WatchKey key = watchService.take();
                                
                                for (WatchEvent<?> event : key.pollEvents()) {
                                    WatchEvent.Kind<?> kind = event.kind();
                                    
                                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                                        continue;
                                    }
                                    
                                    Path filename = (Path) event.context();
                                    
                                    if (filename.toString().equals(ordersFileName)) {
                                        manager.loadOrders();
                                        rebuildOrderList();
                                    }
                                }
                                
                                boolean valid = key.reset();
                                if (!valid) {
                                    break;
                                }
                            } catch (InterruptedException e) {
                                break;
                            } catch (Exception e) {
                                break;
                            }
                        }
                    }
                }, "CheeseOrders-FileWatcher");
                watchThread.setDaemon(true);
                watchThread.start();
            }
        } catch (IOException e) {
            System.err.println("Failed to start file watching: " + e.getMessage());
        }
    }
    
    private void stopFileWatching() {
        watchingEnabled = false;
        
        if (watchKey != null) {
            watchKey.cancel();
            watchKey = null;
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                // Ignore
            }
            watchService = null;
        }
        
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
    }
    
    
    
    private void rebuildOrderList() {
        // Clear existing widgets from content
        for (Widget child : new ArrayList<>(orderListContent.children())) {
            child.destroy();
        }
        
        List<CheeseOrder> orders = new ArrayList<>(manager.getOrders().values()).stream()
                .sorted(Comparator.comparingInt(CheeseOrder::getId))
                .collect(Collectors.toList());
        
        int y = 0;
        int contentWidth = orderListContainer.cont.sz.x;
        
        for (CheeseOrder order : orders) {
            Widget orderWidget = createOrderWidget(order, new Coord(contentWidth, UI.scale(32)));
            orderListContent.add(orderWidget, new Coord(0, y));
            y += orderWidget.sz.y + UI.scale(2);
        }
        
        // Let the content widget auto-resize and update scrollbar
        orderListContent.pack();
        orderListContainer.cont.update();
    }
    
    /**
     * Get cheese icon for a cheese type name
     * Uses VSpec cheese category with ItemTex.create() - loads only once per type
     */
    private BufferedImage getCheeseIcon(String cheeseType) {
        // Return cached icon if available
        if (cheeseIcons.containsKey(cheeseType)) {
            return cheeseIcons.get(cheeseType);
        }
        
        // Don't retry icons that have already failed to load
        if (failedIcons.contains(cheeseType)) {
            BufferedImage placeholder = createPlaceholderIcon(cheeseType);
            cheeseIcons.put(cheeseType, placeholder);
            return placeholder;
        }
        
        BufferedImage icon = null;
        try {
            // Find the cheese JSON object in VSpec categories (same as NCatSelection)
            JSONObject cheeseRes = null;
            if (VSpec.categories.containsKey("Cheese")) {
                for (JSONObject obj : VSpec.categories.get("Cheese")) {
                    if (obj.getString("name").equals(cheeseType)) {
                        cheeseRes = obj;
                        break;
                    }
                }
            }
            
            if (cheeseRes != null) {
                // Use ItemTex.create() exactly like NCatSelection does
                icon = ItemTex.create(cheeseRes);
                if (icon != null) {
                    // Success - cache the icon and return
                    cheeseIcons.put(cheeseType, icon);
                    return icon;
                }
            }
        } catch (Exception e) {
            // Silent failure
        }
        
        // Mark as failed to prevent repeated attempts
        failedIcons.add(cheeseType);
        
        // Create and cache placeholder
        BufferedImage placeholder = createPlaceholderIcon(cheeseType);
        cheeseIcons.put(cheeseType, placeholder);
        return placeholder;
    }
    
    /**
     * Create a simple colored square as placeholder icon
     */
    private BufferedImage createPlaceholderIcon(String cheeseType) {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = img.createGraphics();
        
        // Set color based on cheese type (simple hash-based coloring)
        int hash = cheeseType.hashCode();
        int r = 150 + (Math.abs(hash) % 100);
        int g = 120 + (Math.abs(hash >> 8) % 100);  
        int b = 80 + (Math.abs(hash >> 16) % 100);
        
        g2d.setColor(new java.awt.Color(r, g, b));
        g2d.fillRect(0, 0, size, size);
        
        // Add a simple border
        g2d.setColor(java.awt.Color.BLACK);
        g2d.drawRect(0, 0, size-1, size-1);
        
        g2d.dispose();
        return img;
    }
    
}
