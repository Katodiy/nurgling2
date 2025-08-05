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
        Color boxFillColor = new Color(227, 28, 1, 195);
        Color boxEdgeColor = new Color(224, 193, 79, 255);
    }

    private final WorldSettings tempSettings = new WorldSettings();
    private CheckBox flatSurface;
    private CheckBox decorativeObjects;
    private CheckBox natura;
    private CheckBox boundingBoxes;
    private NColorWidget fillColorWidget;
    private NColorWidget edgeColorWidget;

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


        // Bounding box colors
        add(new Label("Bounding Box Colors:"), UI.scale(10, 200));
        
        fillColorWidget = add(new NColorWidget("Fill"), UI.scale(50, 230));
        fillColorWidget.color = tempSettings.boxFillColor;
        
        edgeColorWidget = add(new NColorWidget("Edge"), UI.scale(50, 280));
        edgeColorWidget.color = tempSettings.boxEdgeColor;

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

        // Load colors if they exist in config
        tempSettings.boxFillColor = NConfig.getColor(NConfig.Key.boxFillColor, new Color(227, 28, 1, 195));
        tempSettings.boxEdgeColor = NConfig.getColor(NConfig.Key.boxEdgeColor, new Color(224, 193, 79, 255));


        // Update UI components
        flatSurface.a = tempSettings.flatSurface;
        decorativeObjects.a = tempSettings.decorativeObjects;
        natura.a = !tempSettings.hideNature;
        boundingBoxes.a = tempSettings.showBB;
        fillColorWidget.color = tempSettings.boxFillColor;
        edgeColorWidget.color = tempSettings.boxEdgeColor;
    }

    @Override
    public void save() {
        // Save temporary settings to config
        NConfig.set(NConfig.Key.nextflatsurface, tempSettings.flatSurface);
        NConfig.set(NConfig.Key.nextshowCSprite, tempSettings.decorativeObjects);

        NConfig.set(NConfig.Key.showBB, tempSettings.showBB);
        
        // Save hideNature setting
        NConfig.set(NConfig.Key.hideNature, tempSettings.hideNature);
        
        // Update colors from UI widgets
        tempSettings.boxFillColor = fillColorWidget.color;
        tempSettings.boxEdgeColor = edgeColorWidget.color;
        NConfig.setColor(NConfig.Key.boxFillColor, tempSettings.boxFillColor);
        NConfig.setColor(NConfig.Key.boxEdgeColor, tempSettings.boxEdgeColor);

        if ((Boolean) NConfig.get(NConfig.Key.hideNature) != tempSettings.hideNature) {
            NUtils.getGameUI().mmapw.natura.a = !tempSettings.hideNature;
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