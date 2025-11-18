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
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingBeetroots, new VegetableConfig("Beetroots", "Beetroot", "Pickled Beetroot"));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingCarrots, new VegetableConfig("Carrots", "Carrot", "Pickled Carrot"));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingEggs, new VegetableConfig("Eggs", "Egg", "Pickled Egg"));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingHerring, new VegetableConfig("Herring", "Herring", "Pickled Herring"));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingOlives, new VegetableConfig("Olives", "Olive", "Pickled Olive"));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingCucumbers, new VegetableConfig("Cucumbers", "cucumber", "Pickled Cucumber"));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingRedOnion, new VegetableConfig("Red Onion", "redonion", "Pickled Onion"));
        VEGETABLE_CONFIGS.put(NConfig.Key.picklingYellowOnion, new VegetableConfig("Yellow Onion", "yellowonion", "Pickled Onion"));
    }

    public static class VegetableConfig {
        public final String subSpec;
        public final String freshAlias;
        public final String pickledAlias;

        public VegetableConfig(String subSpec, String freshAlias, String pickledAlias) {
            this.subSpec = subSpec;
            this.freshAlias = freshAlias;
            this.pickledAlias = pickledAlias;
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (true) {
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

            if (!anyProcessed) {
                break;
            }
        }
        return Results.SUCCESS();
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
        NContext context = new NContext(gui);

        // Check if required areas exist
        NArea jarArea = NContext.findSpecGlobal(Specialisation.SpecName.picklingJars.toString(), vegetableConfig.subSpec);

        if (jarArea == null) {
            NUtils.getGameUI().msg("Missing picklingJars area. Skipping " + vegetableConfig.subSpec + ".");
            return false;
        }

        boolean processed = false;

        // Phase 1: Fill pickling jars with fresh inputs
        Results fillResult = new GlobalFreshFillingPhase(jarArea, vegetableConfig).run(gui);
        if (fillResult.isSuccess) {
            processed = true;
        }

        // Phase 2: Re-fill brine
        Results brineResult = new GlobalBrinePhase(vegetableConfig).run(gui);
        if (brineResult.isSuccess) {
            processed = true;
        }

        // Phase 3: Extract all ready items
        Results extractResult = new GlobalExtractionPhase(jarArea, vegetableConfig).run(gui);
        if (extractResult.isSuccess) {
            processed = true;
        }

        // Phase 4: Fill pickling jars with fresh inputs again (to maximize utilization)
        Results refillResult = new GlobalFreshFillingPhase(jarArea, vegetableConfig).run(gui);
        if (refillResult.isSuccess) {
            processed = true;
        }

        return processed;
    }
}