package nurgling.actions.bots.registry;

import nurgling.actions.Action;
import nurgling.actions.PumpkinFarmer;
import nurgling.actions.bots.*;
import java.util.*;

public class BotRegistry {
    private static final Map<String, BotDescriptor> registry = new HashMap<>();

    public interface BotFactory {
        List<Setting> requiredSettings();
        Action create(Map<String, Object> settings);
    }

    static {
        // ----- NAVIGATION -----
        registry.put("goto_area", new BotDescriptor(
                "goto_area", 1, BotDescriptor.BotType.UTILS, "Go to area", "nurgling/bots/icons/attacknearcurs/u",
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

        // ----- FIELD CROPS -----
        registry.put("turnip", new BotDescriptor(
                "turnip", 2, BotDescriptor.BotType.FARMING, "Turnip Farmer", "nurgling/bots/icons/turnip/u",
                "Automatically harvests and replants turnips.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new TurnipsFarmer(); }
                }
        ));
        registry.put("carrot", new BotDescriptor(
                "carrot", 3, BotDescriptor.BotType.FARMING, "Carrot Farmer", "nurgling/bots/icons/carrot/u",
                "Automatically harvests and replants carrots.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new CarrotFarmer(); }
                }
        ));
        registry.put("beetroot", new BotDescriptor(
                "beetroot", 4, BotDescriptor.BotType.FARMING, "Beetroot Farmer", "nurgling/bots/icons/beetroot/u",
                "Automatically harvests and replants beetroots.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new BeetrootFarmer(); }
                }
        ));
        registry.put("red_onion", new BotDescriptor(
                "red_onion", 5, BotDescriptor.BotType.FARMING, "Red Onion Farmer", "nurgling/bots/icons/red_onion/u",
                "Automatically harvests and replants red onions.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new RedOnionFarmer(); }
                }
        ));
        registry.put("yellow_onion", new BotDescriptor(
                "yellow_onion", 6, BotDescriptor.BotType.FARMING, "Yellow Onion Farmer", "nurgling/bots/icons/yellow_onion/u",
                "Automatically harvests and replants yellow onions.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new YellowOnionFarmer(); }
                }
        ));
        registry.put("garlic", new BotDescriptor(
                "garlic", 7, BotDescriptor.BotType.FARMING, "Garlic Farmer", "nurgling/bots/icons/garlic/u",
                "Automatically harvests and replants garlic.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new GarlicFarmer(); }
                }
        ));
        registry.put("hemp", new BotDescriptor(
                "hemp", 8, BotDescriptor.BotType.FARMING, "Hemp Farmer", "nurgling/bots/icons/hemp/u",
                "Automatically harvests and replants hemp.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new HempFarmer(); }
                }
        ));
        registry.put("flax", new BotDescriptor(
                "flax", 9, BotDescriptor.BotType.FARMING, "Flax Farmer", "nurgling/bots/icons/flax/u",
                "Automatically harvests and replants flax.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new FlaxFarmer(); }
                }
        ));
        registry.put("lettuce", new BotDescriptor(
                "lettuce", 10, BotDescriptor.BotType.FARMING, "Lettuce Farmer", "nurgling/bots/icons/lettuce/u",
                "Automatically harvests and replants lettuce.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new LettuceFarmer(); }
                }
        ));
        registry.put("pumpkin", new BotDescriptor(
                "pumpkin", 11, BotDescriptor.BotType.FARMING, "Pumpkin Farmer", "nurgling/bots/icons/pumpkin/u",
                "Automatically harvests and replants pumpkins.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new PumpkinFarmer(); }
                }
        ));
        registry.put("barley", new BotDescriptor(
                "barley", 12, BotDescriptor.BotType.FARMING, "Barley Farmer", "nurgling/bots/icons/barley/u",
                "Automatically harvests and replants barley.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new BarleyFarmer(); }
                }
        ));
        registry.put("millet", new BotDescriptor(
                "millet", 13, BotDescriptor.BotType.FARMING, "Millet Farmer", "nurgling/bots/icons/millet/u",
                "Automatically harvests and replants millet.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new MilletFarmer(); }
                }
        ));
        registry.put("wheat", new BotDescriptor(
                "wheat", 14, BotDescriptor.BotType.FARMING, "Wheat Farmer", "nurgling/bots/icons/wheat/u",
                "Automatically harvests and replants wheat.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new WheatFarmer(); }
                }
        ));
        registry.put("poppy", new BotDescriptor(
                "poppy", 15, BotDescriptor.BotType.FARMING, "Poppy Farmer", "nurgling/bots/icons/poppy/u",
                "Automatically harvests and replants poppies.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new PoppyFarmer(); }
                }
        ));
        registry.put("pipeweed", new BotDescriptor(
                "pipeweed", 16, BotDescriptor.BotType.FARMING, "Pipeweed Farmer", "nurgling/bots/icons/pipeweed/u",
                "Automatically harvests and replants pipeweed.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new PipeweedFarmer(); }
                }
        ));

        // ----- LIVESTOCK -----
        registry.put("goats", new BotDescriptor(
                "goats", 17, BotDescriptor.BotType.LIVESTOCK, "Goat Manager", "nurgling/bots/icons/goats/u",
                "Manages goat herds.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new GoatsAction(); }
                }
        ));
        registry.put("sheeps", new BotDescriptor(
                "sheeps", 18, BotDescriptor.BotType.LIVESTOCK, "Sheep Manager", "nurgling/bots/icons/sheeps/u",
                "Manages sheep herds.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new SheepsAction(); }
                }
        ));
        registry.put("pigs", new BotDescriptor(
                "pigs", 19, BotDescriptor.BotType.LIVESTOCK, "Pig Manager", "nurgling/bots/icons/pigs/u",
                "Manages pig herds.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new PigsAction(); }
                }
        ));
        registry.put("horses", new BotDescriptor(
                "horses", 20, BotDescriptor.BotType.LIVESTOCK, "Horse Manager", "nurgling/bots/icons/horses/u",
                "Manages horses.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new HorsesAction(); }
                }
        ));
        registry.put("cows", new BotDescriptor(
                "cows", 21, BotDescriptor.BotType.LIVESTOCK, "Cow Manager", "nurgling/bots/icons/cows/u",
                "Manages cows.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new nurgling.actions.bots.CowsAction(); }
                }
        ));
        registry.put("chicken", new BotDescriptor(
                "chicken", 22, BotDescriptor.BotType.LIVESTOCK, "Chicken Manager", "nurgling/bots/icons/chicken/u",
                "Manages chicken coops.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new KFC(); }
                }
        ));
        registry.put("bee", new BotDescriptor(
                "bee", 23, BotDescriptor.BotType.LIVESTOCK, "Beehive Manager", "nurgling/bots/icons/bee/u",
                "Collects honey and wax from beehives.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new HoneyAndWaxCollector(); }
                }
        ));

        // ----- PRODUCTION -----
        registry.put("butcher", new BotDescriptor(
                "butcher", 24, BotDescriptor.BotType.LABORING, "Butcher", "nurgling/bots/icons/butcher/u",
                "Automatically butchers animals.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new Butcher(); }
                }
        ));
        registry.put("hides", new BotDescriptor(
                "hides", 25, BotDescriptor.BotType.LABORING, "Frame Hides", "nurgling/bots/icons/hides/u",
                "Automates drying and handling of hides.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new DFrameHidesAction(); }
                }
        ));
        registry.put("leather", new BotDescriptor(
                "leather", 26, BotDescriptor.BotType.LABORING, "Leather Worker", "nurgling/bots/icons/leather/u",
                "Processes hides into leather automatically.",
                new BotFactory() {
                    public List<Setting> requiredSettings() { return List.of(); }
                    public Action create(Map<String, Object> settings) { return new LeatherAction(); }
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
