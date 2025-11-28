package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import java.util.List;
import java.util.ArrayList;

public class FarmingSettingsPanel extends Panel {
    private TextEntry xEntry, yEntry;
    private CheckBox harvestRefillCheck;
    private CheckBox cleanupQContainersCheck;
    private CheckBox fillCompostWithSwill;
    private CheckBox ignoreStrawInFarmers;
    private CheckBox autoEquipTravellersSacksCheck;
    private CheckBox validateAllCropsBeforeHarvestCheck;
    private Dropbox<String> harvestTimingDropbox;

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
                "Keep quality grind containers at most half full") {
            public void set(boolean val) {
                a = val;
            }
        };
        add(cleanupQContainersCheck, new Coord(margin, y));
        y += UI.scale(18);

        add(new Label("Excess lowest quality seeds will be moved to the trough if defined"),
                new Coord(UI.scale(30), y));
        y += UI.scale(28);

        fillCompostWithSwill = new CheckBox(
                "Fill compost bins with swill") {
            public void set(boolean val) {
                a = val;
            }
        };

        add(fillCompostWithSwill, new Coord(margin, y));
        y += UI.scale(28);

        ignoreStrawInFarmers = new CheckBox(
                "Do NOT pick up straw in farmers.") {
            public void set(boolean val) {
                a = val;
            }
        };

        add(ignoreStrawInFarmers, new Coord(margin, y));
        y += UI.scale(28);

        autoEquipTravellersSacksCheck = new CheckBox(
                "Auto equip traveller's sacks after harvesting") {
            public void set(boolean val) {
                a = val;
            }
        };

        add(autoEquipTravellersSacksCheck, new Coord(margin, y));
        y += UI.scale(28);

        validateAllCropsBeforeHarvestCheck = new CheckBox(
                "Validate all crops are ready before harvesting") {
            public void set(boolean val) {
                a = val;
            }
        };

        add(validateAllCropsBeforeHarvestCheck, new Coord(margin, y));
        y += UI.scale(28);

        add(new Label("Quality Farmer Harvest Timing:"), new Coord(margin, y));
        y += UI.scale(24);

        List<String> harvestOptions = new ArrayList<>();
        harvestOptions.add("Earliest");
        harvestOptions.add("Latest");
        harvestTimingDropbox = new Dropbox<String>(UI.scale(120), harvestOptions.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return harvestOptions.get(i);
            }

            @Override
            protected int listitems() {
                return harvestOptions.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, new Coord(2, 1));
            }
        };
        add(harvestTimingDropbox, new Coord(margin, y));
        y += UI.scale(28);

        add(new Label("Earliest: harvest at minimum stage+ | Latest: harvest only at highest stage"),
            new Coord(margin, y));
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

        Boolean fillConstBinsWithSwill = (Boolean) NConfig.get(NConfig.Key.fillCompostWithSwill);
        fillCompostWithSwill.a = fillConstBinsWithSwill != null && fillConstBinsWithSwill;

        Boolean ignoreStraw = (Boolean) NConfig.get(NConfig.Key.ignoreStrawInFarmers);
        ignoreStrawInFarmers.a = ignoreStraw != null && ignoreStraw;

        Boolean autoEquipSacks = (Boolean) NConfig.get(NConfig.Key.autoEquipTravellersSacks);
        autoEquipTravellersSacksCheck.a = autoEquipSacks != null && autoEquipSacks;

        Boolean validateAllCrops = (Boolean) NConfig.get(NConfig.Key.validateAllCropsBeforeHarvest);
        validateAllCropsBeforeHarvestCheck.a = validateAllCrops != null && validateAllCrops;

        String harvestTiming = (String) NConfig.get(NConfig.Key.harvestTiming);
        if (harvestTiming == null) harvestTiming = "Latest";
        harvestTimingDropbox.sel = harvestTiming;

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
        NConfig.set(NConfig.Key.ignoreStrawInFarmers, ignoreStrawInFarmers.a);
        NConfig.set(NConfig.Key.fillCompostWithSwill, fillCompostWithSwill.a);
        NConfig.set(NConfig.Key.autoEquipTravellersSacks, autoEquipTravellersSacksCheck.a);
        NConfig.set(NConfig.Key.validateAllCropsBeforeHarvest, validateAllCropsBeforeHarvestCheck.a);
        NConfig.set(NConfig.Key.harvestTiming, harvestTimingDropbox.sel);
        String xVal = xEntry.text();
        String yVal = yEntry.text();
        if (!xVal.matches("\\d+")) xVal = "3";
        if (!yVal.matches("\\d+")) yVal = "3";
        NConfig.set(NConfig.Key.qualityGrindSeedingPatter, xVal + "x" + yVal);
        NConfig.needUpdate();
    }
}
