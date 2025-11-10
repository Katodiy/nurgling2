package nurgling.widgets;

import haven.*;
import haven.Locked;
import nurgling.TreeLocation;
import nurgling.TreeLocationService;
import nurgling.NGameUI;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Window for searching saved tree locations by tree type
 * Simplified version of FishSearchWindow - only tree type filter
 */
public class TreeSearchWindow extends Window {
    private final NGameUI gui;
    private final TreeLocationService treeService;

    private Dropbox<String> treeTypeDropdown;
    private TreeResultsList resultsList;
    private List<String> treeTypes;
    private int controlX;
    private int treeDropdownY;

    private static final int WINDOW_WIDTH = UI.scale(350);
    private static final int WINDOW_HEIGHT = UI.scale(400);

    public TreeSearchWindow(NGameUI gui) {
        super(new Coord(WINDOW_WIDTH, WINDOW_HEIGHT), "Tree Location Search", true);
        this.gui = gui;
        this.treeService = gui.treeLocationService;

        int y = UI.scale(10);
        int labelX = UI.scale(10);
        controlX = UI.scale(100);
        treeDropdownY = y;
        int lineHeight = UI.scale(30);

        // Tree type filter
        add(new Label("Tree Type:"), labelX, y + UI.scale(5));
        refreshTreeTypeDropdown();
        y += lineHeight;

        // Search button
        Button searchBtn = add(new Button(UI.scale(150), "Search") {
            @Override
            public void click() {
                performSearch();
            }
        }, UI.scale(100), y);
        y += lineHeight + UI.scale(10);

        // Results list
        add(new Label("Results:"), labelX, y);
        y += UI.scale(25);

        Coord resultsSize = new Coord(WINDOW_WIDTH - UI.scale(20), WINDOW_HEIGHT - y - UI.scale(10));
        resultsList = add(new TreeResultsList(resultsSize), labelX, y);

        pack();
    }

    /**
     * Get list of distinct tree types from saved locations
     */
    private List<String> getDistinctTreeTypes() {
        if (treeService == null) return new ArrayList<>();

        return treeService.getAllTreeLocations().stream()
            .map(TreeLocation::getTreeName)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Refresh the tree type dropdown with current saved tree types
     */
    private void refreshTreeTypeDropdown() {
        // Remove old dropdown if it exists
        if (treeTypeDropdown != null) {
            ui.destroy(treeTypeDropdown);
        }

        // Get fresh tree types
        treeTypes = getDistinctTreeTypes();
        treeTypes.add(0, "Any"); // Add "Any" option at the beginning

        // Create new dropdown
        treeTypeDropdown = add(new Dropbox<String>(UI.scale(230), Math.min(treeTypes.size(), 10), UI.scale(20)) {
            @Override
            protected String listitem(int i) {
                return treeTypes.get(i);
            }

            @Override
            protected int listitems() {
                return treeTypes.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
        }, controlX, treeDropdownY);
        treeTypeDropdown.change(treeTypes.get(0)); // Select "Any" by default
    }

    /**
     * Perform search based on selected filter
     */
    private void performSearch() {
        if (treeService == null) return;

        String selectedTree = treeTypeDropdown.sel;

        // Get all locations and filter
        List<TreeLocation> results = treeService.getAllTreeLocations().stream()
            .filter(loc -> {
                // Filter by tree type
                if (!selectedTree.equals("Any") && !loc.getTreeName().equals(selectedTree)) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        resultsList.setResults(results);
    }

    /**
     * List widget for displaying search results
     */
    private class TreeResultsList extends SListBox<TreeLocation, Widget> {
        private List<TreeLocation> results = new ArrayList<>();

        public TreeResultsList(Coord sz) {
            super(sz, UI.scale(25));
        }

        public void setResults(List<TreeLocation> results) {
            this.results = results;
        }

        @Override
        protected List<TreeLocation> items() {
            return results;
        }

        @Override
        protected Widget makeitem(TreeLocation location, int idx, Coord sz) {
            return new ItemWidget<TreeLocation>(this, sz, location) {
                {
                    Button panBtn = add(new Button(sz.x, "") {
                        @Override
                        public void draw(GOut g) {
                            // Custom drawing to show tree info with quantity
                            String text = location.getTreeName() + " (Qty: " + location.getQuantity() + ")";
                            g.text(text, Coord.z);
                        }

                        @Override
                        public void click() {
                            panMapToLocation(location);
                        }
                    }, Coord.z);
                }
            };
        }
    }

    /**
     * Pan the main map to the selected tree location
     */
    private void panMapToLocation(TreeLocation location) {
        if (gui == null || gui.mapfile == null) return;

        NMapWnd mapWnd = gui.mapfile;
        if (mapWnd == null || mapWnd.view == null) return;

        // Open map window if not visible
        if (!mapWnd.visible()) {
            gui.togglewnd(mapWnd);
        }

        // Get the segment for this tree location
        if (gui.mmap != null && gui.mmap.file != null) {
            try (Locked lk = new Locked(gui.mmap.file.lock.readLock())) {
                MapFile.Segment segment = gui.mmap.file.segments.get(location.getSegmentId());
                if (segment != null) {
                    // Create a location for the tree coordinates
                    MiniMap.Location targetLoc = new MiniMap.Location(segment, location.getTileCoords());
                    // Center the map view on this location
                    mapWnd.view.center(targetLoc);
                    mapWnd.view.follow(null);
                    gui.msg("Map centered on " + location.getTreeName() + " location", java.awt.Color.GREEN);
                } else {
                    gui.msg("Tree location is in a different area", java.awt.Color.YELLOW);
                }
            }
        }
    }

    @Override
    public void show() {
        // Refresh tree types when window is shown
        refreshTreeTypeDropdown();
        super.show();
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
