package nurgling.actions.bots.pickling;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PicklingBot implements Action {

    private static final Map<NConfig.Key, VegetableConfig> VEGETABLE_CONFIGS = new HashMap<>();

    static {
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingBeetroots, new VegetableConfig("Beetroots", "Beetroot", "Pickled Beetroot", haven.Coord.of(1, 1)));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingCarrots, new VegetableConfig("Carrots", "Carrot", "Pickled Carrot", haven.Coord.of(1, 1)));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingEggs, new VegetableConfig("Eggs", "Boiled Egg", "Pickled Egg", haven.Coord.of(1, 1)));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingHerring, new VegetableConfig("Herring", "Herring", "Pickled Herring", haven.Coord.of(1, 1)));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingOlives, new VegetableConfig("Olives", "Olive", "Pickled Olive", haven.Coord.of(1, 1)));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingCucumbers, new VegetableConfig("Cucumbers", "Cucumber", "Pickled Cucumber", haven.Coord.of(2, 1)));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingRedOnion, new VegetableConfig("Red Onion", "Red Onion", "Pickled Onion", haven.Coord.of(1, 1)));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingYellowOnion, new VegetableConfig("Yellow Onion", "Yellow Onion", "Pickled Onion", haven.Coord.of(1, 1)));
    }

    public static class VegetableConfig {
        public final String subSpec;
        public final String freshAlias;
        public final String pickledAlias;
        public final haven.Coord itemSize;

        public VegetableConfig(String subSpec, String freshAlias, String pickledAlias, haven.Coord itemSize) {
            this.subSpec = subSpec;
            this.freshAlias = freshAlias;
            this.pickledAlias = pickledAlias;
            this.itemSize = itemSize;
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        List<VegetableConfig> enabledVegetables = getEnabledVegetables();
        if (enabledVegetables.isEmpty()) {
            NUtils.getGameUI().msg("No vegetables enabled for pickling. Check Pickling Settings.");
            return Results.SUCCESS();
        }

        boolean anyProcessed = false;

        // Process each enabled vegetable individually
        for (VegetableConfig vegetableConfig : enabledVegetables) {
            if (processVegetable(gui, vegetableConfig)) {
                anyProcessed = true;
            }
        }

        return anyProcessed ? Results.SUCCESS() : Results.FAIL();
    }

    private List<VegetableConfig> getEnabledVegetables() {
        List<VegetableConfig> enabled = new ArrayList<>();

        for (Map.Entry<NConfig.Key, VegetableConfig> entry : VEGETABLE_CONFIGS.entrySet()) {
            Boolean isEnabled = (Boolean) NConfig.get(entry.getKey());
            if (isEnabled != null && isEnabled) {
                enabled.add(entry.getValue());
            }
        }

        return enabled;
    }

    private boolean processVegetable(NGameUI gui, VegetableConfig vegetableConfig) throws InterruptedException {
        // Check if required areas exist
        NArea jarArea = NContext.findSpecGlobal(Specialisation.SpecName.picklingJars.toString(), vegetableConfig.subSpec);

        if (jarArea == null) {
            NUtils.getGameUI().msg("Missing picklingJars area. Skipping " + vegetableConfig.subSpec + ".");
            return false;
        }

        boolean processed = false;

        // Special handling for cucumbers: Extract → Fresh Fill → Brine
        if (isCucumber(vegetableConfig)) {
            // Phase 1: Extract all ready cucumbers first
            Results extractResult = new GlobalExtractionPhase(vegetableConfig).run(gui);
            if (extractResult.isSuccess) {
                processed = true;
            }

            // Phase 2: Fill pickling jars with fresh cucumbers
            Results fillResult = new GlobalFreshFillingPhase(vegetableConfig).run(gui);
            if (fillResult.isSuccess) {
                processed = true;
            }

            // Phase 3: Re-fill brine
            Results brineResult = new GlobalBrinePhase(vegetableConfig).run(gui);
            if (brineResult.isSuccess) {
                processed = true;
            }
        } else {
            // Standard workflow for all other vegetables: Fresh Fill → Brine → Extract → Fresh Fill Again

            // Phase 1: Fill pickling jars with fresh inputs
            Results fillResult = new GlobalFreshFillingPhase(vegetableConfig).run(gui);
            if (fillResult.isSuccess) {
                processed = true;
            }

            // Phase 2: Re-fill brine
            Results brineResult = new GlobalBrinePhase(vegetableConfig).run(gui);
            if (brineResult.isSuccess) {
                processed = true;
            }

            // Phase 3: Extract all ready items
            Results extractResult = new GlobalExtractionPhase(vegetableConfig).run(gui);
            if (extractResult.isSuccess) {
                processed = true;
            }

            // Phase 4: Fill pickling jars with fresh inputs again (to maximize utilization)
            Results refillResult = new GlobalFreshFillingPhase(vegetableConfig).run(gui);
            if (refillResult.isSuccess) {
                processed = true;
            }
        }

        return processed;
    }

    private boolean isCucumber(VegetableConfig vegetableConfig) {
        return "Cucumbers".equals(vegetableConfig.subSpec) || "Cucumber".equals(vegetableConfig.freshAlias);
    }
}