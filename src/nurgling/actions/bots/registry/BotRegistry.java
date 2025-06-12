package nurgling.actions.bots.registry;

import nurgling.actions.Action;
import nurgling.actions.bots.*;
import java.util.*;

public class BotRegistry {
    private static final Map<String, BotDescriptor> registry = new HashMap<>();

    public interface BotFactory {
        List<Setting> requiredSettings();
        Action create(Map<String, Object> settings);
    }

    static {
        registry.put("carrot", new BotDescriptor(
                "carrot", "Carrot Farmer", "nurgling/bots/icons/carrot/u",
                "Automatically harvests and replants carrots.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new CarrotFarmer(); }
                }
        ));
        registry.put("beetroot", new BotDescriptor(
                "beetroot", "Beetroot Farmer", "nurgling/bots/icons/beetroot/u",
                "Automatically harvests and replants beetroots.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new BeetrootFarmer(); }
                }
        ));
        registry.put("red_onion", new BotDescriptor(
                "red_onion", "Red Onion Farmer", "nurgling/bots/icons/red_onion/u",
                "Automatically harvests and replants red onions.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new RedOnionFarmer(); }
                }
        ));
        registry.put("yellow_onion", new BotDescriptor(
                "yellow_onion", "Yellow Onion Farmer", "nurgling/bots/icons/yellow_onion/u",
                "Automatically harvests and replants yellow onions.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new YellowOnionFarmer(); }
                }
        ));
        registry.put("garlic", new BotDescriptor(
                "garlic", "Garlic Farmer", "nurgling/bots/icons/garlic/u",
                "Automatically harvests and replants garlic.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new GarlicFarmer(); }
                }
        ));
        registry.put("hemp", new BotDescriptor(
                "hemp", "Hemp Farmer", "nurgling/bots/icons/hemp/u",
                "Automatically harvests and replants hemp.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new HempFarmer(); }
                }
        ));
        registry.put("flax", new BotDescriptor(
                "flax", "Flax Farmer", "nurgling/bots/icons/flax/u",
                "Automatically harvests and replants flax.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new FlaxFarmer(); }
                }
        ));
        registry.put("lettuce", new BotDescriptor(
                "lettuce", "Lettuce Farmer", "nurgling/bots/icons/lettuce/u",
                "Automatically harvests and replants lettuce.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new LettuceFarmer(); }
                }
        ));
        registry.put("barley", new BotDescriptor(
                "barley", "Barley Farmer", "nurgling/bots/icons/barley/u",
                "Automatically harvests and replants barley.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new BarleyFarmer(); }
                }
        ));
        registry.put("millet", new BotDescriptor(
                "millet", "Millet Farmer", "nurgling/bots/icons/millet/u",
                "Automatically harvests and replants millet.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new MilletFarmer(); }
                }
        ));
        registry.put("wheat", new BotDescriptor(
                "wheat", "Wheat Farmer", "nurgling/bots/icons/wheat/u",
                "Automatically harvests and replants wheat.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new WheatFarmer(); }
                }
        ));

        registry.put("KFC", new BotDescriptor(
                "KFC", "KFC", "nurgling/bots/icons/chicken/u",
                "Manage chicken coops.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new KFC(); }
                }
        ));

        registry.put("goto_area", new BotDescriptor(
                "goto_area", "Go to area", "nurgling/bots/icons/wheat/u", // <- use correct icon!
                "Global PF navigate to an area id",
                new BotFactory() {
                    public List<Setting> requiredSettings() {
                        return List.of(new Setting("areaId", Integer.class));
                    }
                    public Action create(Map<String, Object> settings) {
                        int areaId = (int) settings.get("areaId");
                        return new RoutePointNavigator(areaId);
                    }
                }
        ));
    }

    public static Action createBot(String key, Map<String, Object> settings) {
        BotDescriptor desc = registry.get(key);
        if (desc == null) throw new IllegalArgumentException("Unknown bot: " + key);
        return desc.factory.create(settings);
    }

    public static Collection<BotDescriptor> listBots() {
        return registry.values();
    }

    public static BotDescriptor getDescriptor(String key) {
        return registry.get(key);
    }
}
