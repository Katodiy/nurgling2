package nurgling.widgets.nsettings;

import haven.*;
import nurgling.*;
import nurgling.overlays.NModelBox;
import nurgling.widgets.NColorWidget;
import java.awt.Color;
import java.util.ConcurrentModificationException;

public class World extends Panel {
    // Temporary settings structure
    private static class WorldSettings {
        boolean flatSurface;
        boolean decorativeObjects;
        boolean hideNature;
        boolean showBB;
        boolean showBeehiveRadius;
        boolean showTroughRadius;
        boolean persistentBarrelLabels;
        Color boxFillColor = new Color(227, 28, 1, 195);
        Color boxEdgeColor = new Color(224, 193, 79, 255);
        int boxLineWidth = 4;
    }

    private final WorldSettings tempSettings = new WorldSettings();
    private CheckBox flatSurface;
    private CheckBox decorativeObjects;
    private CheckBox natura;
    private CheckBox boundingBoxes;
    private CheckBox beehiveRadius;
    private CheckBox troughRadius;
    private CheckBox persistentBarrels;
    private NColorWidget fillColorWidget;
    private NColorWidget edgeColorWidget;
    private HSlider lineWidthSlider;
    private Label lineWidthLabel;

    public World() {
        super("World Settings");

        // Flat surface setting
        add(new Label("Terrain:"), UI.scale(10, 40));
        flatSurface = add(new CheckBox("Flat surface (requires restart)") {
            public void set(boolean val) {
                tempSettings.flatSurface = val;
                a = val;
            }
        }, UI.scale(100, 40));

        // Decorative objects setting
        decorativeObjects = add(new CheckBox("Show decorative objects (requires restart)") {
            public void set(boolean val) {
                tempSettings.decorativeObjects = val;
                a = val;
            }
        }, UI.scale(100, 80));

        // Nature objects setting
        add(new Label("Objects:"), UI.scale(10, 120));
        natura = add(new CheckBox("Hide nature objects") {
            public void set(boolean val) {
                tempSettings.hideNature = !val;
                a = val;

            }
        }, UI.scale(100, 120));

        // Bounding boxes setting
        boundingBoxes = add(new CheckBox("Show object boundaries") {
            public void set(boolean val) {
                tempSettings.showBB = val;
                a = val;
            }
        }, UI.scale(100, 160));
        
        // Beehive radius setting
        beehiveRadius = add(new CheckBox("Show beehive radius") {
            public void set(boolean val) {
                tempSettings.showBeehiveRadius = val;
                a = val;
            }
        }, UI.scale(100, 200));
        
        // Trough radius setting
        troughRadius = add(new CheckBox("Show trough radius") {
            public void set(boolean val) {
                tempSettings.showTroughRadius = val;
                a = val;
            }
        }, UI.scale(100, 240));

        // Persistent barrel labels setting
        persistentBarrels = add(new CheckBox("Keep barrel labels visible during camera scroll") {
            public void set(boolean val) {
                tempSettings.persistentBarrelLabels = val;
                a = val;
            }
        }, UI.scale(100, 280));


        // Bounding box colors
        add(new Label("Bounding Box Colors:"), UI.scale(10, 320));
        
        fillColorWidget = add(new NColorWidget("Fill"), UI.scale(50, 350));
        fillColorWidget.color = tempSettings.boxFillColor;
        
        edgeColorWidget = add(new NColorWidget("Edge"), UI.scale(50, 400));
        edgeColorWidget.color = tempSettings.boxEdgeColor;

        // Line width setting
        lineWidthLabel = add(new Label("Line width: 4"), UI.scale(50, 450));
        lineWidthSlider = add(new HSlider(UI.scale(100), 1, 10, tempSettings.boxLineWidth) {
            public void changed() {
                tempSettings.boxLineWidth = val;
                lineWidthLabel.settext("Line width: " + val);
            }
        }, UI.scale(50, 470));

    }

    public void setNatureStatus(Boolean a) {
        tempSettings.hideNature = a;
        natura.a = !a;
    }

    @Override
    public void load() {
        // Load current settings into temporary structure
        tempSettings.flatSurface = (Boolean) NConfig.get(NConfig.Key.nextflatsurface);
        tempSettings.decorativeObjects = (Boolean) NConfig.get(NConfig.Key.nextshowCSprite);
        tempSettings.hideNature = (Boolean) NConfig.get(NConfig.Key.hideNature);
        tempSettings.showBB = (Boolean) NConfig.get(NConfig.Key.showBB);
        tempSettings.showBeehiveRadius = (Boolean) NConfig.get(NConfig.Key.showBeehiveRadius);
        tempSettings.showTroughRadius = (Boolean) NConfig.get(NConfig.Key.showTroughRadius);
        tempSettings.persistentBarrelLabels = (Boolean) NConfig.get(NConfig.Key.persistentBarrelLabels);

        // Load colors if they exist in config
        tempSettings.boxFillColor = NConfig.getColor(NConfig.Key.boxFillColor, new Color(227, 28, 1, 195));
        tempSettings.boxEdgeColor = NConfig.getColor(NConfig.Key.boxEdgeColor, new Color(224, 193, 79, 255));
        
        // Load line width
        Object lineWidthObj = NConfig.get(NConfig.Key.boxLineWidth);
        tempSettings.boxLineWidth = (lineWidthObj instanceof Integer) ? (Integer) lineWidthObj : 4;


        // Update UI components
        flatSurface.a = tempSettings.flatSurface;
        decorativeObjects.a = tempSettings.decorativeObjects;
        natura.a = !tempSettings.hideNature;
        boundingBoxes.a = tempSettings.showBB;
        beehiveRadius.a = tempSettings.showBeehiveRadius;
        troughRadius.a = tempSettings.showTroughRadius;
        persistentBarrels.a = tempSettings.persistentBarrelLabels;
        fillColorWidget.color = tempSettings.boxFillColor;
        edgeColorWidget.color = tempSettings.boxEdgeColor;
        lineWidthSlider.val = tempSettings.boxLineWidth;
        lineWidthLabel.settext("Line width: " + tempSettings.boxLineWidth);
    }

    @Override
    public void save() {
        // Save temporary settings to config
        NConfig.set(NConfig.Key.nextflatsurface, tempSettings.flatSurface);
        NConfig.set(NConfig.Key.nextshowCSprite, tempSettings.decorativeObjects);

        NConfig.set(NConfig.Key.showBB, tempSettings.showBB);
        
        // Save object radii settings (overlays will auto-update)
        NConfig.set(NConfig.Key.showBeehiveRadius, tempSettings.showBeehiveRadius);
        NConfig.set(NConfig.Key.showTroughRadius, tempSettings.showTroughRadius);
        
        NConfig.set(NConfig.Key.persistentBarrelLabels, tempSettings.persistentBarrelLabels);
        
        // Save hideNature setting
        NConfig.set(NConfig.Key.hideNature, tempSettings.hideNature);
        
        // Update colors from UI widgets
        tempSettings.boxFillColor = fillColorWidget.color;
        tempSettings.boxEdgeColor = edgeColorWidget.color;
        NConfig.setColor(NConfig.Key.boxFillColor, tempSettings.boxFillColor);
        NConfig.setColor(NConfig.Key.boxEdgeColor, tempSettings.boxEdgeColor);
        
        // Save line width setting
        NConfig.set(NConfig.Key.boxLineWidth, tempSettings.boxLineWidth);

        if ((Boolean) NConfig.get(NConfig.Key.hideNature) != tempSettings.hideNature) {
            // Sync with mini map
            if (NUtils.getGameUI() != null && NUtils.getGameUI().mmapw != null) {
                NUtils.getGameUI().mmapw.natura.a = !tempSettings.hideNature;
            }
            
            // Sync with QoL panel
            if (NUtils.getGameUI() != null && NUtils.getGameUI().opts != null && NUtils.getGameUI().opts.nqolwnd instanceof OptWnd.NSettingsPanel) {
                OptWnd.NSettingsPanel panel = (OptWnd.NSettingsPanel) NUtils.getGameUI().opts.nqolwnd;
                if (panel.settingsWindow != null && panel.settingsWindow.qol != null) {
                    panel.settingsWindow.qol.syncHideNature();
                }
            }
            
            NUtils.showHideNature();
        }

        // Force update of all NModelBox instances
        try {
            if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
            {
                for (Gob gob : NUtils.getGameUI().map.glob.oc)
                {
                    for (Gob.Overlay overlay : gob.ols)
                    {
                        if (overlay.spr instanceof NModelBox)
                        {
                            ((NModelBox) overlay.spr).updateMaterials();
                        }
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            // Ignore concurrent modification exceptions during iteration
        }
        
        // Mark configuration as needing update to file
        NConfig.needUpdate();
    }

    public static Color getBoxFillColor() {
        return NConfig.getColor(NConfig.Key.boxFillColor, new Color(227, 28, 1, 195));
    }

    public static Color getBoxEdgeColor() {
        return NConfig.getColor(NConfig.Key.boxEdgeColor, new Color(224, 193, 79, 255));
    }
}