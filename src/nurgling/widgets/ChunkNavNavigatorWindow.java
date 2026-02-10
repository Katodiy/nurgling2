package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.i18n.L10n;
import nurgling.navigation.*;

import java.awt.Color;
import java.util.*;

/**
 * UI window for testing ChunkNav navigation system.
 * Allows selecting an area from a dropdown and navigating to it.
 */
public class ChunkNavNavigatorWindow extends Window {

    private final AreaListbox areaList;
    private final Button navigateButton;
    private final Button cancelButton;
    private final Label statusLabel;
    private final List<NArea> areas = new ArrayList<>();

    public ChunkNavNavigatorWindow() {
        super(new Coord(UI.scale(300), UI.scale(350)), L10n.get("chunknav.title"));

        // Title/instructions
        Widget prev = add(new Label(L10n.get("chunknav.select_area")), new Coord(UI.scale(10), UI.scale(5)));

        // Area list
        refreshAreas();
        areaList = new AreaListbox(UI.scale(280), 12);
        prev = add(areaList, prev.pos("bl").add(0, UI.scale(10)));

        // Status label
        statusLabel = new Label("");
        prev = add(statusLabel, prev.pos("bl").add(0, UI.scale(10)));

        // Buttons row
        Widget buttonRow = add(new Widget(new Coord(UI.scale(280), UI.scale(30))), prev.pos("bl").add(0, UI.scale(10)));

        navigateButton = buttonRow.add(new Button(UI.scale(100), L10n.get("chunknav.navigate")) {
            @Override
            public void click() {
                super.click();
                startNavigation();
            }
        }, new Coord(UI.scale(30), 0));

        cancelButton = buttonRow.add(new Button(UI.scale(100), L10n.get("common.cancel")) {
            @Override
            public void click() {
                super.click();
                closeWindow();
            }
        }, new Coord(UI.scale(150), 0));

        // Refresh button
        add(new Button(UI.scale(80), L10n.get("chunknav.refresh")) {
            @Override
            public void click() {
                super.click();
                refreshAreas();
                areaList.sel = null;
                statusLabel.settext("Areas refreshed: " + areas.size() + " found");
            }
        }, buttonRow.pos("bl").add(UI.scale(100), UI.scale(10)));

        // ChunkNav stats
        updateStats();

        pack();
    }

    private void refreshAreas() {
        areas.clear();
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui != null && gui.map != null && gui.map.glob != null && gui.map.glob.map != null) {
                areas.addAll(gui.map.glob.map.areas.values());
                // Sort by name
                areas.sort(Comparator.comparing(a -> a.name));
            }
        } catch (Exception e) {
            System.err.println("ChunkNavNavigator: Error loading areas: " + e.getMessage());
        }
    }

    private void updateStats() {
        ChunkNavManager manager = getChunkNavManager();
        if (manager != null && manager.isInitialized()) {
            ChunkNavGraph graph = manager.getGraph();
            statusLabel.settext("Chunks: " + graph.getChunkCount() + ", Areas: " + areas.size());
        } else {
            statusLabel.settext("ChunkNav not initialized");
        }
    }

    private ChunkNavManager getChunkNavManager() {
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui != null && gui.map != null && gui.map instanceof NMapView) {
                return ((NMapView)gui.map).getChunkNavManager();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private void startNavigation() {
        NArea selectedArea = areaList.sel;
        if (selectedArea == null) {
            statusLabel.settext("Please select an area first");
            return;
        }

        ChunkNavManager manager = getChunkNavManager();
        if (manager == null || !manager.isInitialized()) {
            statusLabel.settext("ChunkNav not initialized!");
            return;
        }

        statusLabel.settext("Navigating to: " + selectedArea.name + "...");

        // Start navigation in a new thread with gear/stop button support
        Thread navThread = new Thread(() -> {
            try {
                NGameUI gui = NUtils.getGameUI();
                if (gui == null) {
                    updateStatusFromThread("Error: No GUI available");
                    return;
                }

                // First try ChunkNav
                ChunkPath path = manager.planToArea(selectedArea);
                // Note: path with 0 waypoints is valid when already in target chunk
                if (path != null) {
                    if (path.isEmpty()) {
                        updateStatusFromThread("Already in target chunk, navigating to area...");
                    } else {
                        updateStatusFromThread("Path found! " + path.waypoints.size() + " waypoints");
                    }

                    ChunkNavExecutor executor = new ChunkNavExecutor(path, selectedArea, manager);
                    nurgling.actions.Results result = executor.run(gui);

                    if (result.IsSuccess()) {
                        updateStatusFromThread("Arrived at: " + selectedArea.name);
                    } else {
                        updateStatusFromThread("Navigation failed - path blocked?");
                    }
                } else {
                    updateStatusFromThread("No path found to " + selectedArea.name);
                }
            } catch (InterruptedException e) {
                updateStatusFromThread("Navigation cancelled");
            } catch (Exception e) {
                updateStatusFromThread("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "ChunkNav-Navigator");

        // Register thread with the bot interrupt widget (shows gear with stop button)
        NGameUI gui = NUtils.getGameUI();
        if (gui != null && gui.biw != null) {
            gui.biw.addObserve(navThread);
        }
        navThread.start();
    }

    private void updateStatusFromThread(String text) {
        // Update UI from game thread
        try {
            if (ui != null) {
                ui.sess.glob.loader.defer(() -> statusLabel.settext(text), null);
            }
        } catch (Exception e) {
            // Fallback
            statusLabel.settext(text);
        }
    }

    private void closeWindow() {
        hide();
        reqdestroy();
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            closeWindow();
        } else {
            super.wdgmsg(msg, args);
        }
    }

    private static final int AREA_ITEM_HEIGHT = UI.scale(20);

    /**
     * Simple listbox for displaying areas.
     */
    private class AreaListbox extends Listbox<NArea> {

        public AreaListbox(int w, int h) {
            super(w, h, AREA_ITEM_HEIGHT);
        }

        @Override
        protected int listitems() {
            return areas.size();
        }

        @Override
        protected NArea listitem(int i) {
            return areas.get(i);
        }

        @Override
        protected void drawitem(GOut g, NArea item, int i) {
            // Background for alternating rows
            if (i % 2 == 1) {
                g.chcolor(new Color(30, 30, 30, 128));
                g.frect(Coord.z, g.sz());
                g.chcolor();
            }

            // Draw area name
            String displayName = item.name;
            if (displayName.length() > 35) {
                displayName = displayName.substring(0, 32) + "...";
            }
            g.text(displayName, new Coord(UI.scale(5), UI.scale(2)));

            // Show specializations count
            if (!item.spec.isEmpty()) {
                String specText = "(" + item.spec.size() + " specs)";
                g.chcolor(Color.GRAY);
                g.text(specText, new Coord(sz.x - UI.scale(70), UI.scale(2)));
                g.chcolor();
            }
        }

        @Override
        public void change(NArea item) {
            super.change(item);
            if (item != null) {
                // Check if we have nav data for this area
                ChunkNavManager manager = getChunkNavManager();
                if (manager != null && manager.isInitialized()) {
                    boolean hasData = manager.hasDataForArea(item);
                    if (hasData) {
                        statusLabel.settext("Selected: " + item.name + " (path data available)");
                    } else {
                        statusLabel.settext("Selected: " + item.name + " (no path data yet)");
                    }
                } else {
                    statusLabel.settext("Selected: " + item.name);
                }
            }
        }
    }
}
