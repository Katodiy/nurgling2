package nurgling.widgets;

import haven.*;
import haven.Locked;
import nurgling.FishLocation;
import nurgling.FishLocationService;
import nurgling.NGameUI;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Window for searching saved fish locations by fish name, moon phase, and catch percentage
 */
public class FishSearchWindow extends Window {
    private final NGameUI gui;
    private final FishLocationService fishService;

    private Dropbox<String> fishNameDropdown;
    private Dropbox<String> moonPhaseDropdown;
    private TextEntry percentageEntry;
    private FishResultsList resultsList;

    private static final int WINDOW_WIDTH = UI.scale(400);
    private static final int WINDOW_HEIGHT = UI.scale(500);

    public FishSearchWindow(NGameUI gui) {
        super(new Coord(WINDOW_WIDTH, WINDOW_HEIGHT), "Fish Location Search", true);
        this.gui = gui;
        this.fishService = gui.fishLocationService;

        int y = UI.scale(10);
        int labelX = UI.scale(10);
        int controlX = UI.scale(120);
        int lineHeight = UI.scale(30);

        // Fish name filter
        add(new Label("Fish Name:"), labelX, y + UI.scale(5));
        List<String> fishNames = getDistinctFishNames();
        fishNames.add(0, "Any"); // Add "Any" option at the beginning
        fishNameDropdown = add(new Dropbox<String>(UI.scale(250), Math.min(fishNames.size(), 10), UI.scale(20)) {
            @Override
            protected String listitem(int i) {
                return fishNames.get(i);
            }

            @Override
            protected int listitems() {
                return fishNames.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
        }, controlX, y);
        fishNameDropdown.change(fishNames.get(0)); // Select "Any" by default
        y += lineHeight;

        // Moon phase filter
        add(new Label("Moon Phase:"), labelX, y + UI.scale(5));
        List<String> moonPhases = getDistinctMoonPhases();
        moonPhases.add(0, "Any"); // Add "Any" option at the beginning
        moonPhaseDropdown = add(new Dropbox<String>(UI.scale(250), Math.min(moonPhases.size(), 10), UI.scale(20)) {
            @Override
            protected String listitem(int i) {
                return moonPhases.get(i);
            }

            @Override
            protected int listitems() {
                return moonPhases.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
        }, controlX, y);
        moonPhaseDropdown.change(moonPhases.get(0)); // Select "Any" by default
        y += lineHeight;

        // Percentage filter (minimum)
        add(new Label("Min Percentage:"), labelX, y + UI.scale(5));
        percentageEntry = add(new TextEntry(UI.scale(100), "0"), controlX, y);
        y += lineHeight;

        // Search button
        Button searchBtn = add(new Button(UI.scale(150), "Search") {
            @Override
            public void click() {
                performSearch();
            }
        }, UI.scale(125), y);
        y += lineHeight + UI.scale(10);

        // Results list
        add(new Label("Results:"), labelX, y);
        y += UI.scale(25);

        Coord resultsSize = new Coord(WINDOW_WIDTH - UI.scale(20), WINDOW_HEIGHT - y - UI.scale(10));
        resultsList = add(new FishResultsList(resultsSize), labelX, y);

        pack();
    }

    /**
     * Get list of distinct fish names from saved locations
     */
    private List<String> getDistinctFishNames() {
        if (fishService == null) return new ArrayList<>();

        return fishService.getAllFishLocations().stream()
            .map(FishLocation::getFishName)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Get list of distinct moon phases from saved locations
     */
    private List<String> getDistinctMoonPhases() {
        if (fishService == null) return new ArrayList<>();

        return fishService.getAllFishLocations().stream()
            .map(FishLocation::getMoonPhase)
            .filter(phase -> !phase.equals("Unknown"))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Perform search based on selected filters
     */
    private void performSearch() {
        if (fishService == null) return;

        String selectedFish = fishNameDropdown.sel;
        String selectedMoon = moonPhaseDropdown.sel;

        // Parse percentage once and make it final for lambda
        int parsedPercentage;
        try {
            parsedPercentage = Integer.parseInt(percentageEntry.text());
        } catch (NumberFormatException e) {
            parsedPercentage = 0;
        }
        final int minPercentage = parsedPercentage;

        // Get all locations and filter
        List<FishLocation> results = fishService.getAllFishLocations().stream()
            .filter(loc -> {
                // Filter by fish name
                if (!selectedFish.equals("Any") && !loc.getFishName().equals(selectedFish)) {
                    return false;
                }

                // Filter by moon phase
                if (!selectedMoon.equals("Any") && !loc.getMoonPhase().equals(selectedMoon)) {
                    return false;
                }

                // Filter by percentage
                String percentStr = loc.getPercentage().replace("%", "").trim();
                try {
                    int percent = Integer.parseInt(percentStr);
                    if (percent < minPercentage) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // If we can't parse percentage, exclude it
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
    private class FishResultsList extends SListBox<FishLocation, Widget> {
        private List<FishLocation> results = new ArrayList<>();

        public FishResultsList(Coord sz) {
            super(sz, UI.scale(25));
        }

        public void setResults(List<FishLocation> results) {
            this.results = results;
        }

        @Override
        protected List<FishLocation> items() {
            return results;
        }

        @Override
        protected Widget makeitem(FishLocation location, int idx, Coord sz) {
            return new ItemWidget<FishLocation>(this, sz, location) {
                {
                    int deleteButtonWidth = UI.scale(20);
                    int panButtonWidth = sz.x - deleteButtonWidth - UI.scale(2);

                    // Main button for panning to location
                    Button panBtn = add(new Button(panButtonWidth, "") {
                        @Override
                        public void draw(GOut g) {
                            // Custom drawing to show fish info
                            String text = String.format("%s - %s - %s @ %s",
                                location.getFishName(),
                                location.getPercentage(),
                                location.getMoonPhase(),
                                location.getGameTime());
                            g.text(text, Coord.z);
                        }

                        @Override
                        public void click() {
                            panMapToLocation(location);
                        }
                    }, Coord.z);

                    // X button for deletion
                    Button deleteBtn = add(new Button(deleteButtonWidth, "X") {
                        @Override
                        public void click() {
                            if (gui != null && gui.fishLocationService != null) {
                                gui.fishLocationService.removeFishLocation(location.getLocationId());
                                gui.msg("Removed " + location.getFishName() + " location", java.awt.Color.YELLOW);
                                // Refresh the search results to remove the deleted item
                                performSearch();
                            }
                        }
                    }, panButtonWidth + UI.scale(2), 0);
                }
            };
        }
    }

    /**
     * Pan the main map to the selected fish location
     */
    private void panMapToLocation(FishLocation location) {
        if (gui == null || gui.mapfile == null) return;

        NMapWnd mapWnd = gui.mapfile;
        if (mapWnd == null || mapWnd.view == null) return;

        // Open map window if not visible
        if (!mapWnd.visible()) {
            gui.togglewnd(mapWnd);
        }

        // Get the segment for this fish location
        if (gui.mmap != null && gui.mmap.file != null) {
            try (Locked lk = new Locked(gui.mmap.file.lock.readLock())) {
                MapFile.Segment segment = gui.mmap.file.segments.get(location.getSegmentId());
                if (segment != null) {
                    // Create a location for the fish coordinates
                    MiniMap.Location targetLoc = new MiniMap.Location(segment, location.getTileCoords());
                    // Center the map view on this location
                    mapWnd.view.center(targetLoc);
                    mapWnd.view.follow(null);
                    gui.msg("Map centered on " + location.getFishName() + " location", java.awt.Color.GREEN);
                } else {
                    gui.msg("Fish location is in a different area", java.awt.Color.YELLOW);
                }
            }
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
