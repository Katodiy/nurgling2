package nurgling.widgets.nsettings;

import haven.*;
import nurgling.cheese.CheeseBranch;
import nurgling.cheese.CheeseOrder;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.NUtils;

import java.util.*;
import java.util.stream.Collectors;

public class CheeseOrdersPanel extends Panel {
    private final CheeseOrdersManager manager = new CheeseOrdersManager();
    private final int margin = UI.scale(10);

    private final Widget listPanel;
    private Widget editorPanel = null;

    private final SListBox<CheeseOrder, Widget> orderList;
    private final List<String> cheeseTypes = CheeseBranch.allProducts();

    // --- Editor Panel widgets ---
    private Dropbox<String> cheeseTypeDropdown;
    private TextEntry countEntry;

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
        orderList = listPanel.add(
                new SListBox<CheeseOrder, Widget>(new Coord(olistWidth, olistHeight), UI.scale(32)) {
                    @Override
                    protected List<CheeseOrder> items() {
                        return new ArrayList<>(manager.getOrders().values()).stream()
                                .sorted(Comparator.comparingInt(CheeseOrder::getId))
                                .collect(Collectors.toList());
                    }
                    @Override
                    protected Widget makeitem(CheeseOrder order, int idx, Coord sz) {
                        Widget w = new Widget(sz);
                        Label label = new Label(order.getCheeseType() + " x" + order.getCount());
                        int btnW = UI.scale(70);
                        int rightPad = UI.scale(10);
                        int delBtnX = sz.x - rightPad - btnW;

                        int labelAreaWidth = delBtnX - margin;
                        int labelX = margin + (labelAreaWidth - label.sz.x) / 2;

                        w.add(label, new Coord(labelX, (sz.y - label.sz.y) / 2));
                        w.add(new Button(btnW, "Delete", () -> {
                            manager.deleteOrder(order.getId());
                            manager.writeOrders();
                            orderList.update();
                        }), new Coord(delBtnX, (sz.y - btnHeight) / 2));
                        return w;
                    }
                },
                new Coord(margin, margin + UI.scale(32))
        );

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
        orderList.update();
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

        int id = getNextOrderId();

        List<CheeseBranch.Cheese> chain = CheeseBranch.getChainToProduct(cheeseType);
        if (chain == null || chain.isEmpty()) {
            NUtils.getGameUI().msg("Could not find a recipe chain for: " + cheeseType);
            return;
        }
        List<CheeseOrder.StepStatus> status = new ArrayList<>();
        CheeseBranch.Cheese firstStep = chain.get(0);
        status.add(new CheeseOrder.StepStatus(firstStep.name, firstStep.place.name(), count));

        CheeseOrder order = new CheeseOrder(id, cheeseType, count, status);
        manager.addOrUpdateOrder(order);
        manager.writeOrders();

        showListPanel();
        NUtils.getGameUI().msg("Order added: " + cheeseType + " x" + count);
    }

    private int getNextOrderId() {
        if (manager.getOrders().isEmpty()) return 1;
        return manager.getOrders().keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }
}
