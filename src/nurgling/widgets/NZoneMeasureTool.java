package nurgling.widgets;

import haven.*;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.overlays.NZoneMeasureOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NZoneMeasureTool extends Window {
    private final NGameUI gui;

    // State management
    private boolean isClearingSelection = false;

    // Collection of active zone overlays
    private final List<NZoneMeasureOverlay> zones = new ArrayList<>();

    // UI Components
    private Button selectAreaBtn;
    private Button clearOneBtn;
    private Button clearAllBtn;
    private Label statusLabel;

    public NZoneMeasureTool(NGameUI gui) {
        super(UI.scale(200, 130), "Zone Measure Tool", true);
        this.gui = gui;

        Widget prev;

        // Row 1: Select Area button
        prev = add(selectAreaBtn = new Button(UI.scale(180), "Select Area") {
            @Override
            public void click() {
                startAreaSelection();
            }
        }, UI.scale(5, 5));

        // Row 2: Clear One button
        prev = add(clearOneBtn = new Button(UI.scale(180), "Clear Selection") {
            @Override
            public void click() {
                startClearSelectionMode();
            }
        }, prev.pos("bl").adds(0, 5));

        // Row 3: Clear All button
        prev = add(clearAllBtn = new Button(UI.scale(180), "Clear All") {
            @Override
            public void click() {
                clearAllZones();
            }
        }, prev.pos("bl").adds(0, 5));

        // Row 4: Status label
        prev = add(statusLabel = new Label("Ready"), prev.pos("bl").adds(0, 10));

        pack();
    }

    private void startAreaSelection() {
        isClearingSelection = false;
        statusLabel.settext("Click and drag to select...");

        // Set flag on NMapView to intercept mouse events
        NMapView mapView = (NMapView) gui.map;
        mapView.zoneMeasureMode = true;
        mapView.zoneMeasureTool = this;
    }

    public void onAreaSelected(Coord tileStart, Coord tileEnd) {
        NMapView mapView = (NMapView) gui.map;
        mapView.zoneMeasureMode = false;

        // Calculate dimensions
        int width = Math.abs(tileEnd.x - tileStart.x) + 1;
        int height = Math.abs(tileEnd.y - tileStart.y) + 1;

        // Create overlay
        NZoneMeasureOverlay overlay = new NZoneMeasureOverlay(
            gui.map.glob.map, tileStart, tileEnd, width, height
        );
        zones.add(overlay);

        statusLabel.settext("Zone: " + width + " x " + height + " tiles");
    }

    public void onSelectionCancelled() {
        NMapView mapView = (NMapView) gui.map;
        mapView.zoneMeasureMode = false;
        statusLabel.settext("Selection cancelled");
    }

    private void startClearSelectionMode() {
        if (zones.isEmpty()) {
            statusLabel.settext("No zones to clear");
            return;
        }
        isClearingSelection = true;
        statusLabel.settext("Click a zone to clear...");

        NMapView mapView = (NMapView) gui.map;
        mapView.zoneClearMode = true;
        mapView.zoneMeasureTool = this;
    }

    public void onZoneClicked(Coord tileCoord) {
        if (!isClearingSelection) return;

        // Find zone at click position
        NZoneMeasureOverlay toRemove = null;
        for (NZoneMeasureOverlay zone : zones) {
            if (zone.contains(tileCoord)) {
                toRemove = zone;
                break;
            }
        }

        if (toRemove != null) {
            toRemove.destroy();
            zones.remove(toRemove);
            statusLabel.settext("Zone cleared. " + zones.size() + " remaining");
        } else {
            statusLabel.settext("No zone at that location");
        }

        isClearingSelection = false;
        NMapView mapView = (NMapView) gui.map;
        mapView.zoneClearMode = false;
    }

    private void clearAllZones() {
        for (NZoneMeasureOverlay zone : zones) {
            zone.destroy();
        }
        zones.clear();
        statusLabel.settext("All zones cleared");
    }

    public void cleanup() {
        clearAllZones();

        // Reset map view modes
        NMapView mapView = (NMapView) gui.map;
        mapView.zoneMeasureMode = false;
        mapView.zoneClearMode = false;
        mapView.zoneMeasureTool = null;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && Objects.equals(msg, "close")) {
            cleanup();
            reqdestroy();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public void destroy() {
        cleanup();
        super.destroy();
    }
}
