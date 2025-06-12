package nurgling.actions.bots.registry;

import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.bots.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BotRegistry {
    private static final Map<String, BotDescriptor> registry = new HashMap<>();

    static {
        // Farming bots
        registry.put("carrot", new BotDescriptor(
                "carrot",
                "Carrot Farmer",
                "nurgling/bots/icons/carrot/u",
                "Automatically harvests and replants carrots.",
                () -> new CarrotFarmer()
        ));
        registry.put("beetroot", new BotDescriptor(
                "beetroot",
                "Beetroot Farmer",
                "nurgling/bots/icons/beetroot/u",
                "Automatically harvests and replants beetroots.",
                () -> new BeetrootFarmer()
        ));
        registry.put("red_onion", new BotDescriptor(
                "red_onion",
                "Red Onion Farmer",
                "nurgling/bots/icons/red_onion/u",
                "Automatically harvests and replants red onions.",
                () -> new RedOnionFarmer()
        ));
        registry.put("yellow_onion", new BotDescriptor(
                "yellow_onion",
                "Yellow Onion Farmer",
                "nurgling/bots/icons/yellow_onion/u",
                "Automatically harvests and replants yellow onions.",
                () -> new YellowOnionFarmer()
        ));
        registry.put("garlic", new BotDescriptor(
                "garlic",
                "Garlic Farmer",
                "nurgling/bots/icons/garlic/u",
                "Automatically harvests and replants garlic.",
                () -> new GarlicFarmer()
        ));

        registry.put("garlic", new BotDescriptor(
                "garlic",
                "Garlic Farmer",
                "nurgling/bots/icons/garlic/u",
                "Automatically harvests and replants garlic.",
                () -> new GarlicFarmer()
        ));
        registry.put("hemp", new BotDescriptor(
                "hemp",
                "Hemp Farmer",
                "nurgling/bots/icons/hemp/u",
                "Automatically harvests and replants hemp.",
                () -> new HempFarmer()
        ));
        registry.put("flax", new BotDescriptor(
                "flax",
                "Flax Farmer",
                "nurgling/bots/icons/flax/u",
                "Automatically harvests and replants flax.",
                () -> new FlaxFarmer()
        ));
        registry.put("lettuce", new BotDescriptor(
                "lettuce",
                "Lettuce Farmer",
                "nurgling/bots/icons/lettuce/u",
                "Automatically harvests and replants lettuce.",
                () -> new LettuceFarmer()
        ));
        registry.put("barley", new BotDescriptor(
                "barley",
                "Barley Farmer",
                "nurgling/bots/icons/barley/u",
                "Automatically harvests and replants barley.",
                () -> new BarleyFarmer()
        ));
        registry.put("millet", new BotDescriptor(
                "millet",
                "Millet Farmer",
                "nurgling/bots/icons/millet/u",
                "Automatically harvests and replants millet.",
                () -> new MilletFarmer()
        ));
        registry.put("wheat", new BotDescriptor(
                "wheat",
                "Wheat Farmer",
                "nurgling/bots/icons/wheat/u",
                "Automatically harvests and replants wheat.",
                () -> new WheatFarmer()
        ));
    }

    public static Action createBot(String key) {
        BotDescriptor desc = registry.get(key);
        if (desc == null) throw new IllegalArgumentException("Unknown bot: " + key);
        return desc.factory.create();
    }

    public static Collection<BotDescriptor> listBots() {
        return registry.values();
    }

    public static BotDescriptor getDescriptor(String key) {
        return registry.get(key);
    }
}
