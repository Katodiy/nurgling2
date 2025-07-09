package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;

public class FarmingSettingsPanel extends Panel {
    private TextEntry xEntry, yEntry;
    private CheckBox harvestRefillCheck;
    private CheckBox cleanupQContainersCheck;

    public FarmingSettingsPanel() {
        super("Farming Settings");
        int y = UI.scale(36);
        int margin = UI.scale(10);

        harvestRefillCheck = new CheckBox("Refill water from water containers for farmers") {
            public void set(boolean val) {
                a = val;
            }
        };
        add(harvestRefillCheck, new Coord(margin, y));
        y += UI.scale(28);

        cleanupQContainersCheck = new CheckBox(
                "Keep quality grind containers at most half full.") {
            public void set(boolean val) {
                a = val;
            }
        };
        add(cleanupQContainersCheck, new Coord(margin, y));
        y += UI.scale(18);

        add(new Label("Excess lowest quality seeds will be moved to the trough if defined."),
                new Coord(UI.scale(30), y));
        y += UI.scale(28);

        add(new Label("Seeding Pattern X (columns):"), new Coord(margin, y));
        y += UI.scale(24);

        xEntry = new TextEntry.NumberValue(50, "") {
            @Override
            public void done(ReadLine buf) {
                super.done(buf);
            }
        };
        add(xEntry, new Coord(margin, y));
        y += UI.scale(32);

        add(new Label("Seeding Pattern Y (rows):"), new Coord(margin, y));
        y += UI.scale(24);

        yEntry = new TextEntry.NumberValue(50, "") {
            @Override
            public void done(ReadLine buf) {
                super.done(buf);
            }
        };
        add(yEntry, new Coord(margin, y));
    }

    @Override
    public void load() {
        Boolean refill = (Boolean) NConfig.get(NConfig.Key.harvestautorefill);
        harvestRefillCheck.a = refill != null && refill;

        Boolean cleanupQContainers = (Boolean) NConfig.get(NConfig.Key.cleanupQContainers);
        cleanupQContainersCheck.a = cleanupQContainers != null && cleanupQContainers;

        String pat = (String) NConfig.get(NConfig.Key.qualityGrindSeedingPatter);
        if (pat == null || !pat.matches("\\d+x\\d+")) pat = "3x3";
        String[] parts = pat.split("x");
        xEntry.settext(parts[0]);
        yEntry.settext(parts[1]);
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.harvestautorefill, harvestRefillCheck.a);
        NConfig.set(NConfig.Key.cleanupQContainers, cleanupQContainersCheck.a);
        String xVal = xEntry.text();
        String yVal = yEntry.text();
        if (!xVal.matches("\\d+")) xVal = "3";
        if (!yVal.matches("\\d+")) yVal = "3";
        NConfig.set(NConfig.Key.qualityGrindSeedingPatter, xVal + "x" + yVal);
        NConfig.needUpdate();
    }
}
