package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import java.awt.Color;

public class World extends Panel {


    // Temporary settings structure
    private static class WorldSettings {
        boolean flatSurface;
        boolean decorativeObjects;
        boolean hideNature;
        boolean showBB;
    }

    private final WorldSettings tempSettings = new WorldSettings();
    private CheckBox flatSurface;
    private CheckBox decorativeObjects;
    private CheckBox natura;
    private CheckBox boundingBoxes;

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
                tempSettings.hideNature = !val; // Inverted logic
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

        // Update UI components
        flatSurface.a = tempSettings.flatSurface;
        decorativeObjects.a = tempSettings.decorativeObjects;
        natura.a = !tempSettings.hideNature; // Inverted logic
        boundingBoxes.a = tempSettings.showBB;
    }

    @Override
    public void save() {
        // Save temporary settings to config
        NConfig.set(NConfig.Key.nextflatsurface, tempSettings.flatSurface);
        NConfig.set(NConfig.Key.nextshowCSprite, tempSettings.decorativeObjects);

        if((Boolean) NConfig.get(NConfig.Key.hideNature)!=tempSettings.hideNature) {
            NConfig.set(NConfig.Key.hideNature, tempSettings.hideNature);
            NUtils.showHideNature();
            NUtils.getGameUI().mmapw.natura.a = !tempSettings.hideNature;
        }
        NConfig.set(NConfig.Key.showBB, tempSettings.showBB);
    }
}