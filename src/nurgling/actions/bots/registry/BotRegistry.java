package nurgling.actions.bots.registry;

import nurgling.actions.*;
import nurgling.actions.bots.farmers.PumpkinFarmer;
import nurgling.actions.bots.*;
import nurgling.actions.bots.CarrotFarmerQ;
import nurgling.actions.bots.silk.RefillSilkwormFeedingCupboards;
import nurgling.actions.bots.silk.SilkProductionBot;
import nurgling.actions.bots.CollectSwillInArea;
import nurgling.actions.bots.farmers.WheatFarmer;
import nurgling.actions.bots.farmers.YellowOnionFarmer;
import nurgling.actions.bots.farmers.StringGrassFarmer;
import nurgling.actions.bots.farmers.WildKaleFarmer;
import nurgling.actions.bots.farmers.WildOnionFarmer;
import nurgling.actions.bots.farmers.WildTuberFarmer;
import nurgling.actions.bots.farmers.WildFlowerFarmer;
import nurgling.actions.test.*;
import nurgling.actions.bots.pickling.PicklingBot;
import nurgling.bots.ChunkNavNavigatorBot;

import java.util.*;
import java.util.stream.Collectors;

public class BotRegistry {
    private static final List<BotDescriptor> bots = new ArrayList<>();

    static {
        // NAVIGATION
        bots.add(new BotDescriptor(
                "goto_area",
                BotDescriptor.BotType.UTILS,
                "Go to area",
                "Global PF navigate to an area id",
                true,
                false,
                GotoArea.class,
                "goto",
                false
        ));
        bots.add(new BotDescriptor(
                "hearthfire",
                BotDescriptor.BotType.UTILS,
                "Travel to Hearthfire",
                "Travel to Hearthfire",
                true,
                false,
                TravelToHearthFire.class,
                "tohome",
                false
        ));
        bots.add(new BotDescriptor(
                "wait_bot",
                BotDescriptor.BotType.UTILS,
                "Wait",
                "Waits for a specified duration (hh:mm:ss)",
                true,
                false,
                WaitBot.class,
                "pause",
                false
        ));
        bots.add(new BotDescriptor(
                "autocraft_bot",
                BotDescriptor.BotType.UTILS,
                "Autocraft",
                "Automatically crafts items using a saved preset",
                true,
                false,
                AutocraftBot.class,
                "autocraft",
                false
        ));
        bots.add(new BotDescriptor(
                "maintain_stock_bot",
                BotDescriptor.BotType.UTILS,
                "Maintain Stock",
                "Maintains a target stock level by counting items in an area and crafting as needed",
                true,
                false,
                MaintainStockBot.class,
                "maintainstock",
                false
        ));

        // RESOURCES (using localization keys: bot.<id>.title and bot.<id>.desc)
        bots.add(new BotDescriptor("choper", BotDescriptor.BotType.RESOURCES, "bot.chopper.title", "bot.chopper.desc", false, true, Chopper.class, "choper", false));
        bots.add(new BotDescriptor("chipper", BotDescriptor.BotType.RESOURCES, "bot.chipper.title", "bot.chipper.desc", false, true, Chipper.class, "chipper", true));
        bots.add(new BotDescriptor("pblocks", BotDescriptor.BotType.RESOURCES, "bot.pblocks.title", "bot.pblocks.desc", false, true, PrepareBlocks.class, "pblocks", false));
        bots.add(new BotDescriptor("pboards", BotDescriptor.BotType.RESOURCES, "bot.pboards.title", "bot.pboards.desc", false, true, PrepareBoards.class, "pboards", false));
        bots.add(new BotDescriptor("clay", BotDescriptor.BotType.RESOURCES, "bot.clay.title", "bot.clay.desc", false, true, ClayDigger.class, "clay", true));
        bots.add(new BotDescriptor("bark", BotDescriptor.BotType.RESOURCES, "bot.bark.title", "bot.bark.desc", false, true, CollectBark.class, "bark", true));
        bots.add(new BotDescriptor("bough", BotDescriptor.BotType.RESOURCES, "bot.bough.title", "bot.bough.desc", false, true, CollectBough.class, "bough", true));
        bots.add(new BotDescriptor("leaf", BotDescriptor.BotType.RESOURCES, "bot.leaf.title", "bot.leaf.desc", false, true, CollectLeaf.class, "leaf", true));
        bots.add(new BotDescriptor("fisher", BotDescriptor.BotType.RESOURCES, "bot.fisher.title", "bot.fisher.desc", false, true, Fishing.class, "fisher", true));
        bots.add(new BotDescriptor("plower", BotDescriptor.BotType.RESOURCES, "bot.plower.title", "bot.plower.desc", false, true, Plower.class, "plower", true));
        bots.add(new BotDescriptor("plant_trees", BotDescriptor.BotType.RESOURCES, "bot.plant_trees.title", "bot.plant_trees.desc", false, true, PlantTrees.class, "treePlanter", false));
        bots.add(new BotDescriptor("blueprint_tree_planter", BotDescriptor.BotType.RESOURCES, "bot.blueprint_tree_planter.title", "bot.blueprint_tree_planter.desc", false, true, BlueprintTreePlanter.class, "treegardener", false));
        bots.add(new BotDescriptor("boughbee", BotDescriptor.BotType.RESOURCES, "bot.boughbee.title", "bot.boughbee.desc", false, true, BoughBee.class, "boughpyre", false));
        bots.add(new BotDescriptor("forager", BotDescriptor.BotType.RESOURCES, "bot.forager.title", "bot.forager.desc", true, true, Forager.class, "forager", false));
        bots.add(new BotDescriptor("tunneling", BotDescriptor.BotType.RESOURCES, "Tunneling Bot", "Automatically digs tunnels from mine supports, placing new supports along the way. Supports wings (perpendicular tunnels).", false, true, TunnelingBot.class, "tunelling", false));

        // PRODUCTIONS
        bots.add(new BotDescriptor("smelter", BotDescriptor.BotType.PRODUCTIONS, "Smelter", "Smelts ore.", true, true, SmelterAction.class, "smelter", true));
        bots.add(new BotDescriptor("backer", BotDescriptor.BotType.PRODUCTIONS, "Baker", "Bakes stuff.", true, true, BakerAction.class, "backer", true));
        bots.add(new BotDescriptor("ugardenpot", BotDescriptor.BotType.PRODUCTIONS, "Ungarden Pot", "Ungardens pots.", true, true, UnGardentPotAction.class, "ugardenpot", true));
        bots.add(new BotDescriptor("butcher", BotDescriptor.BotType.PRODUCTIONS, "Butcher", "Butchers animals.", true, true, Butcher.class, "butcher", false));
        bots.add(new BotDescriptor("hides", BotDescriptor.BotType.PRODUCTIONS, "Handle hides.", "Handles hides.", true, true, DFrameHidesAction.class, "hides", true));
        bots.add(new BotDescriptor("dryedfish", BotDescriptor.BotType.PRODUCTIONS, "Dry Fish", "Dries fish on drying frames.", true, true, DFrameFishAction.class, "dryedfish", true));
        bots.add(new BotDescriptor("fishroast", BotDescriptor.BotType.PRODUCTIONS, "Spit Roast", "Roasts fish.", false, true, FriedFish.class, "fishroast", true));
        bots.add(new BotDescriptor("leather", BotDescriptor.BotType.PRODUCTIONS, "Leather Action", "Makes leather.", true, true, LeatherAction.class, "leather", true));
        bots.add(new BotDescriptor("gelatin", BotDescriptor.BotType.PRODUCTIONS, "Gelatin", "Crafts gelatin from hides. Fills inventory with hides from readyHides zone and crafts.", true, true, GelatinAction.class, "gelatin", true));
        bots.add(new BotDescriptor("smoking", BotDescriptor.BotType.PRODUCTIONS, "Smoking", "Smokes stuff.", false, true, Smoking.class, "smoking", true));
        bots.add(new BotDescriptor("tarkiln", BotDescriptor.BotType.PRODUCTIONS, "Tarkiln Action", "Burns stuff in tarkiln.", true, true, TarkilnAction.class, "tarkiln", true));
        bots.add(new BotDescriptor("tabaco", BotDescriptor.BotType.PRODUCTIONS, "Tabaco Action", "Processes tabaco.", true, true, TabacoAction.class, "tabaco", true));
        bots.add(new BotDescriptor("brick", BotDescriptor.BotType.PRODUCTIONS, "Bricks Action", "Makes bricks.", true, true, BricksAction.class, "brick", true));
        bots.add(new BotDescriptor("branch", BotDescriptor.BotType.PRODUCTIONS, "Branch Action", "Processes branches.", true, true, BranchAction.class, "branch", false));
        bots.add(new BotDescriptor("wrap", BotDescriptor.BotType.PRODUCTIONS, "Wrap Action", "Wraps items.", true, true, WrapAction.class, "wrap", true));
        bots.add(new BotDescriptor("bonestoash", BotDescriptor.BotType.PRODUCTIONS, "Bone Ash", "Burns bones to ash.", true, true, BoneAshAction.class, "bonestoash", true));
        bots.add(new BotDescriptor("ash", BotDescriptor.BotType.PRODUCTIONS, "Block Ash", "Blocks ash.", true, true, BlockAshAction.class, "ash", true));
        bots.add(new BotDescriptor("lye", BotDescriptor.BotType.PRODUCTIONS, "Lye Boiler", "Boils lye.", true, true, LyeBoiler.class, "lye", true));
        bots.add(new BotDescriptor("steel", BotDescriptor.BotType.PRODUCTIONS, "Steel Action", "Makes steel.", true, true, SteelAction.class, "steel", true));
        bots.add(new BotDescriptor("fineryforge", BotDescriptor.BotType.PRODUCTIONS, "Forging ", "Forging.", true, true, FFAction.class, "fineryforge", true));

        // Silk
        bots.add(new BotDescriptor("mulberry_leaf_refiller", BotDescriptor.BotType.PRODUCTIONS, "Refill silkworm cupboards with mulberry leafs", "Refill silkworm cupboards with mulberry leafs.", true, true, RefillSilkwormFeedingCupboards.class, "mulberry_leaf", false));
        bots.add(new BotDescriptor("silk_production", BotDescriptor.BotType.PRODUCTIONS, "Manages silk production starting at eggs all the way to silkworm cocoons.", "Silk cocoons production.", true, true, SilkProductionBot.class, "silkworm_cocoon", false));

        bots.add(new BotDescriptor("pickling", BotDescriptor.BotType.PRODUCTIONS, "Pickling Bot", "Complete automated pickling system. Manages brine levels, fills jars with fresh vegetables, extracts ready pickled items, and maintains continuous production cycle.", true, true, PicklingBot.class, "pickle", true));

        // BATTLE
        bots.add(new BotDescriptor("reagro", BotDescriptor.BotType.BATTLE, "Reagro", "Reagros enemies.", true, true, Reagro.class, "reagro", false));
        bots.add(new BotDescriptor("attacknearcurs", BotDescriptor.BotType.BATTLE, "Aggro Near Cursor", "Aggros near cursor.", true, true, AggroNearCurs.class, "attacknearcurs", false));
        bots.add(new BotDescriptor("attacknear", BotDescriptor.BotType.BATTLE, "Aggro Nearest", "Aggros nearest enemy.", true, true, AggroNearest.class, "attacknear", false));
        bots.add(new BotDescriptor("attacknearborka", BotDescriptor.BotType.BATTLE, "Aggro Nearest Borka", "Aggros nearest Borka.", true, true, AggroNearestBorka.class, "attacknearborka", false));
        bots.add(new BotDescriptor("attackall", BotDescriptor.BotType.BATTLE, "Attack All", "Attacks all enemies.", true, true, AttackAll.class, "attackall", false));
        bots.add(new BotDescriptor("taming", BotDescriptor.BotType.BATTLE, "Tame an animal", "Attacks the nearest animal waiting to be tamed, allows it to escape. Ties the animal on a rope if the corresponding setting is set. Fighting on your own", false, true, TaimingAnimal.class, "taming", false));
        bots.add(new BotDescriptor("combatdist", BotDescriptor.BotType.BATTLE, "Combat Distance Tool", "Opens a window to manage combat distance. Shows current distance to target, allows manual distance input, and auto-calculates optimal kiting distance based on enemy type and vehicle.", false, true, CombatDistanceTool.class, "combatdist", false));

        // FARMING
        bots.add(new BotDescriptor("turnip", BotDescriptor.BotType.FARMING, "Turnip Farmer", "Automatically harvests and replants turnips.", true, true, nurgling.actions.bots.farmers.TurnipsFarmer.class, "turnip", false));
        bots.add(new BotDescriptor("carrot", BotDescriptor.BotType.FARMING, "Carrot Farmer", "Automatically harvests and replants carrots.", true, true, nurgling.actions.bots.farmers.CarrotFarmer.class, "carrot", false));
        bots.add(new BotDescriptor("beetroot", BotDescriptor.BotType.FARMING, "Beetroot Farmer", "Automatically harvests and replants beetroots.", true, true, nurgling.actions.bots.farmers.BeetrootFarmer.class, "beetroot", false));
        bots.add(new BotDescriptor("red_onion", BotDescriptor.BotType.FARMING, "Red Onion Farmer", "Automatically harvests and replants red onions.", true, true, nurgling.actions.bots.farmers.RedOnionFarmer.class, "red_onion", false));
        bots.add(new BotDescriptor("yellow_onion", BotDescriptor.BotType.FARMING, "Yellow Onion Farmer", "Automatically harvests and replants yellow onions.", true, true, YellowOnionFarmer.class, "yellow_onion", false));
        bots.add(new BotDescriptor("leek", BotDescriptor.BotType.FARMING, "Leek Farmer", "Automatically harvests and replants leek.", true, true, nurgling.actions.bots.farmers.LeekFarmer.class, "leek", false));
        bots.add(new BotDescriptor("garlic", BotDescriptor.BotType.FARMING, "Garlic Farmer", "Automatically harvests and replants garlic.", true, true, nurgling.actions.bots.farmers.GarlicFarmer.class, "garlic", false));
        bots.add(new BotDescriptor("hemp", BotDescriptor.BotType.FARMING, "Hemp Farmer", "Automatically harvests and replants hemp.", true, true, nurgling.actions.bots.farmers.HempFarmer.class, "hemp", false));
        bots.add(new BotDescriptor("flax", BotDescriptor.BotType.FARMING, "Flax Farmer", "Automatically harvests and replants flax.", true, true, nurgling.actions.bots.farmers.FlaxFarmer.class, "flax", false));
        bots.add(new BotDescriptor("lettuce", BotDescriptor.BotType.FARMING, "Lettuce Farmer", "Automatically harvests and replants lettuce.", true, true, nurgling.actions.bots.farmers.LettuceFarmer.class, "lettuce", true));
        bots.add(new BotDescriptor("green_kale", BotDescriptor.BotType.FARMING, "Green Kale Farmer", "Automatically harvests and replants green kale.", true, true, nurgling.actions.bots.farmers.GreenKaleFarmer.class, "green_kale", false));
        bots.add(new BotDescriptor("pumpkin", BotDescriptor.BotType.FARMING, "Pumpkin Farmer", "Automatically harvests and replants pumpkins.", true, true, PumpkinFarmer.class, "pumpkin", true));
        bots.add(new BotDescriptor("barley", BotDescriptor.BotType.FARMING, "Barley Farmer", "Automatically harvests and replants barley.", true, true, nurgling.actions.bots.farmers.BarleyFarmer.class, "barley", false));
        bots.add(new BotDescriptor("millet", BotDescriptor.BotType.FARMING, "Millet Farmer", "Automatically harvests and replants millet.", true, true, nurgling.actions.bots.farmers.MilletFarmer.class, "millet", false));
        bots.add(new BotDescriptor("wheat", BotDescriptor.BotType.FARMING, "Wheat Farmer", "Automatically harvests and replants wheat.", true, true, WheatFarmer.class, "wheat", false));
        bots.add(new BotDescriptor("poppy", BotDescriptor.BotType.FARMING, "Poppy Farmer", "Automatically harvests and replants poppies.", true, true, nurgling.actions.bots.farmers.PoppyFarmer.class, "poppy", true));
        bots.add(new BotDescriptor("pipeweed", BotDescriptor.BotType.FARMING, "Pipeweed Farmer", "Automatically harvests and replants pipeweed.", true, true, nurgling.actions.bots.farmers.PipeweedFarmer.class, "pipeweed", false));
        bots.add(new BotDescriptor("grape", BotDescriptor.BotType.FARMING, "Grape Farmer", "Automatically harvests grapes from trellis.", true, true, GrapeFarmer.class, "grape", false));
        bots.add(new BotDescriptor("hops", BotDescriptor.BotType.FARMING, "Hops Farmer", "Automatically harvests hops from trellis.", true, true, HopsFarmer.class, "hops", false));
        bots.add(new BotDescriptor("peppercorn", BotDescriptor.BotType.FARMING, "Peppercorn Farmer", "Automatically harvests peppercorn from trellis.", true, true, PeppercornFarmer.class, "peppercorn", false));
        bots.add(new BotDescriptor("pea", BotDescriptor.BotType.FARMING, "Pea Farmer", "Automatically harvests and replants peas.", true, true, PeaFarmer.class, "pea", false));
        bots.add(new BotDescriptor("cucumber", BotDescriptor.BotType.FARMING, "Cucumber Farmer", "Automatically harvests and replants cucumbers.", true, true, CucumberFarmer.class, "cucumber", false));
        bots.add(new BotDescriptor("goats", BotDescriptor.BotType.FARMING, "Goat Manager", "Manages goat herds.", true, true, GoatsAction.class, "goats", false));
        bots.add(new BotDescriptor("sheeps", BotDescriptor.BotType.FARMING, "Sheep Manager", "Manages sheep herds.", true, true, SheepsAction.class, "sheeps", false));
        bots.add(new BotDescriptor("pigs", BotDescriptor.BotType.FARMING, "Pig Manager", "Manages pig herds.", true, true, PigsAction.class, "pigs", false));
        bots.add(new BotDescriptor("horses", BotDescriptor.BotType.FARMING, "Horse Manager", "Manages horses.", true, true, HorsesAction.class, "horses", false));
        bots.add(new BotDescriptor("cows", BotDescriptor.BotType.FARMING, "Cow Manager", "Manages cows.", true, true, nurgling.actions.bots.CowsAction.class, "cows", false));
        bots.add(new BotDescriptor("reindeers", BotDescriptor.BotType.FARMING, "Teimdeer Manager", "Manages teimdeer.", true, true, nurgling.actions.bots.DeersAction.class, "reindeers", false));
        bots.add(new BotDescriptor("chicken", BotDescriptor.BotType.FARMING, "Chicken Manager", "Manages chicken coops.", true, true, KFC.class, "chicken", false));
        bots.add(new BotDescriptor("rabbit", BotDescriptor.BotType.FARMING, "Rabbit Manager", "Manages rabbit hutches.", true, true, RabbitMaster.class, "rabbit", false));
        bots.add(new BotDescriptor("bee", BotDescriptor.BotType.FARMING, "Beehive Manager", "Collects honey and wax from beehives.", true, true, HoneyAndWaxCollector.class, "bee", true));
        bots.add(new BotDescriptor("stringgrass", BotDescriptor.BotType.FARMING, "String Grass Farmer", "Automatically harvests and replants string grass.", true, true, StringGrassFarmer.class, "stringgrass", false));
        bots.add(new BotDescriptor("wildkale", BotDescriptor.BotType.FARMING, "Wild Kale Farmer", "Automatically harvests and replants wild kale.", true, true, WildKaleFarmer.class, "wildkale", false));
        bots.add(new BotDescriptor("wildonion", BotDescriptor.BotType.FARMING, "Wild Onion Farmer", "Automatically harvests and replants wild onions.", true, true, WildOnionFarmer.class, "wildonion", false));
        bots.add(new BotDescriptor("wildtuber", BotDescriptor.BotType.FARMING, "Wild Tuber Farmer", "Automatically harvests and replants wild tubers.", true, true, WildTuberFarmer.class, "wildtuber", false));
        bots.add(new BotDescriptor("wildgourd", BotDescriptor.BotType.FARMING, "Wild Gourd Farmer", "Automatically harvests wild gourds from trellis.", true, true, WildGourdFarmer.class, "wildgourd", false));
        bots.add(new BotDescriptor("wildflower", BotDescriptor.BotType.FARMING, "Wild Flower Farmer", "Automatically harvests and replants wild flowers.", true, true, WildFlowerFarmer.class, "wildflower", false));

        bots.add(new BotDescriptor("compostbin", BotDescriptor.BotType.FARMING, "Compost Bin", "Pull mulch out of compost bins.", true, true, CompostBinUnloader.class, "compostbin", false));
        bots.add(new BotDescriptor("curdingtub", BotDescriptor.BotType.FARMING, "Unload curding Tubs", "Pull curd out of curding tubs.", true, true, CurdingTubUnloader.class, "cheese_empty", false));
        bots.add(new BotDescriptor("cheese", BotDescriptor.BotType.FARMING, "Cheese Production Bot", "Process cheese orders.", true, true, CheeseProductionBot.class, "cheese", false));
        // Garden pot farming (harvest, fill, plant cycle)
        bots.add(new BotDescriptor("gardenpot_farmer", BotDescriptor.BotType.FARMING, "Garden Pot Farmer", "Complete garden pot farming cycle: harvests ready plants, fills with soil/water, and plants new items.", true, true, GardenPotFarmer.class, "gardenpot", false));

        // FARMING QUALITY
        bots.add(new BotDescriptor("turnipq", BotDescriptor.BotType.FARMING_QUALITY, "Turnip Farmer Quality", "Automatically harvests and replants turnips in X*Y cell patches.", true, true, TurnipsFarmerQ.class, "turnipq", false));
        bots.add(new BotDescriptor("carrotq", BotDescriptor.BotType.FARMING_QUALITY, "Carrot Farmer Quality", "Automatically harvests and replants carrots in X*Y cell patches.", true, true, CarrotFarmerQ.class, "carrotq", false));
        bots.add(new BotDescriptor("beetrootq", BotDescriptor.BotType.FARMING_QUALITY, "Beetroot Farmer Quality", "Automatically harvests and replants beetroot in X*Y cell patches.", true, true, BeetrootFarmerQ.class, "beetrootq", false));
        bots.add(new BotDescriptor("red_onionq", BotDescriptor.BotType.FARMING_QUALITY, "Red Onion Farmer Quality", "Automatically harvests and replants red onions in X*Y cell patches.", true, true, RedOnionFarmerQ.class, "red_onionq", false));
        bots.add(new BotDescriptor("yellow_onionq", BotDescriptor.BotType.FARMING_QUALITY, "Yellow Onion Farmer Quality", "Automatically harvests and replants yellow onions in X*Y cell patches.", true, true, YellowOnionFarmerQ.class, "yellow_onionq", false));
        bots.add(new BotDescriptor("leekq", BotDescriptor.BotType.FARMING_QUALITY, "Leek Farmer Quality", "Automatically harvests and replants leek in X*Y cell patches.", true, true, LeekFarmerQ.class, "leekq", false));
        bots.add(new BotDescriptor("garlicq", BotDescriptor.BotType.FARMING_QUALITY, "Garlic Farmer Quality", "Automatically harvests and replants garlic in X*Y cell patches.", true, true, GarlicFarmerQ.class, "garlicq", false));
        bots.add(new BotDescriptor("hempq", BotDescriptor.BotType.FARMING_QUALITY, "Hemp Farmer Quality", "Automatically harvests and replants hemp in X*Y cell patches.", true, true, HempFarmerQ.class, "hempq", false));
        bots.add(new BotDescriptor("flaxq", BotDescriptor.BotType.FARMING_QUALITY, "Flax Farmer Quality", "Automatically harvests and replants flax in X*Y cell patches.", true, true, FlaxFarmerQ.class, "flaxq", false));
        bots.add(new BotDescriptor("green_kaleq", BotDescriptor.BotType.FARMING_QUALITY, "Green Kale Farmer Quality", "Automatically harvests and replants green kale in X*Y cell patches.", true, true, GreenKaleFarmerQ.class, "green_kaleq", false));
        bots.add(new BotDescriptor("lettuceq", BotDescriptor.BotType.FARMING_QUALITY, "Lettuce Farmer Quality", "Automatically harvests and replants lettuce in X*Y cell patches.", true, true, LettuceFarmerQ.class, "lettuceq", false));
        bots.add(new BotDescriptor("pumpkinq", BotDescriptor.BotType.FARMING_QUALITY, "Pumpkin Farmer Quality", "Automatically harvests and replants pumpkins in X*Y cell patches.", true, true, PumpkinFarmerQ.class, "pumpkinq", false));

        bots.add(new BotDescriptor("barleyq", BotDescriptor.BotType.FARMING_QUALITY, "Barley Farmer Quality", "Automatically harvests and replants barley in X*Y cell patches.", true, true, BarleyFarmerQ.class, "barleyq", false));
        bots.add(new BotDescriptor("milletq", BotDescriptor.BotType.FARMING_QUALITY, "Millet Farmer Quality", "Automatically harvests and replants millet in X*Y cell patches.", true, true, MilletFarmerQ.class, "milletq", false));
        bots.add(new BotDescriptor("wheatq", BotDescriptor.BotType.FARMING_QUALITY, "Wheat Farmer Quality", "Automatically harvests and replants wheat in X*Y cell patches.", true, true, WheatFarmerQ.class, "wheatq", false));
        bots.add(new BotDescriptor("poppyq", BotDescriptor.BotType.FARMING_QUALITY, "Poppy Farmer Quality", "Automatically harvests and replants poppy in X*Y cell patches.", true, true, PoppyFarmerQ.class, "poppyq", false));
        bots.add(new BotDescriptor("pipeweedq", BotDescriptor.BotType.FARMING_QUALITY, "Pipeweed Farmer Quality", "Automatically harvests and replants pipeweed in X*Y cell patches.", true, true, PipeweedFarmerQ.class, "pipeweedq", false));

        // UTILS
        bots.add(new BotDescriptor("equipment_bot", BotDescriptor.BotType.UTILS, "Equipment Bot", "Equip items from a saved preset.", true, false, EquipmentBot.class, "shieldsword", false));
        bots.add(new BotDescriptor("shieldsword", BotDescriptor.BotType.UTILS, "Equip Shield/Sword", "Equips shield and sword.", true, true, EquipShieldSword.class, "shieldsword", false));
        bots.add(new BotDescriptor("filwater", BotDescriptor.BotType.UTILS, "Fill Waterskins (Select Zone)", "Fills waterskins - always prompts to select water zone.", false, true, FillWaterskins.class, "filwater", false));
        bots.add(new BotDescriptor("filwaterzone", BotDescriptor.BotType.UTILS, "Fill Waterskins (Global Zone)", "Fills waterskins using global water zone with chunk navigation.", true, true, FillWaterskinsGlobal.class, "filwaterzone", false));
        bots.add(new BotDescriptor("unbox", BotDescriptor.BotType.UTILS, "Free Containers", "Frees containers in area.", false, true, FreeContainersInArea.class, "unbox", false));
        bots.add(new BotDescriptor("unbox_zone", BotDescriptor.BotType.UTILS, "Free Containers in Unbox Zone", "Automatically navigates to unbox zone and frees containers.", true, true, FreeContainersInUnboxZone.class, "unbox_zone", false));
        bots.add(new BotDescriptor("water_cheker", BotDescriptor.BotType.UTILS, "Check Water", "Checks water.", false, true, CheckWater.class, "water_cheker", false));
        bots.add(new BotDescriptor("clay_cheker", BotDescriptor.BotType.UTILS, "Check Clay", "Checks clay.", false, true, CheckClay.class, "clay_cheker", true));
        bots.add(new BotDescriptor("clover", BotDescriptor.BotType.UTILS, "Feed Clover", "Feeds clover.", false, true, FeedClover.class, "clover", false));
        bots.add(new BotDescriptor("collectalltopile", BotDescriptor.BotType.UTILS, "Collect To Pile", "Collects same items from earth.", false, true, CollectSameItemsFromEarth.class, "collectalltopile", false));
        bots.add(new BotDescriptor("worldexplorer", BotDescriptor.BotType.UTILS, "World Explorer", "Explores the world.", false, true, WorldExplorer.class, "worldexplorer", true));
        bots.add(new BotDescriptor("lift", BotDescriptor.BotType.UTILS, "Transfer Liftable (Select Zone)", "Lifts items - always prompts for input zone selection.", false, true, TransferLiftable.class, "lift", false));
        bots.add(new BotDescriptor("lift_global", BotDescriptor.BotType.UTILS, "Transfer Liftable (Global Zone)", "Lifts items using global zones with chunk navigation.", true, true, TransferLiftableGlobal.class, "liftzone", false));
        bots.add(new BotDescriptor("loading", BotDescriptor.BotType.UTILS, "Transfer To Vehicle", "Loads vehicle.", false, true, TransferToVeh.class, "loading", false));
        bots.add(new BotDescriptor("unloading", BotDescriptor.BotType.UTILS, "Transfer From Vehicle", "Unloads vehicle.", false, true, TransferFromVeh.class, "unloading", false));
        bots.add(new BotDescriptor("swap", BotDescriptor.BotType.UTILS, "Swap Vehicles", "Swaps between vehicles.", false, true, TransferFromVehToVeh.class, "swap", false));
        bots.add(new BotDescriptor("eater", BotDescriptor.BotType.UTILS, "Eating bot", "Eat in the food area.", true, true, Eater.class, "eater", false));
        bots.add(new BotDescriptor("zoneminer", BotDescriptor.BotType.UTILS, "Mine in area", "Mine rocks in the area.", false, true, MineAction.class, "zoneminer", true));
        bots.add(new BotDescriptor("bed", BotDescriptor.BotType.UTILS, "Go to bed", "Go to any free bed in a bed area nearby.", true, false, Sleep.class, "bed", false));
        bots.add(new BotDescriptor("soil", BotDescriptor.BotType.UTILS, "Create piles with soil", "Create piles with soil in area.", false, true, CreateSoilPiles.class, "soil", false));
        bots.add(new BotDescriptor("destroy", BotDescriptor.BotType.UTILS, "Destroyer", "Destroy objects in area.", false, true, Destroyer.class, "destroy", false));
        bots.add(new BotDescriptor("destroytrellisplants", BotDescriptor.BotType.UTILS, "Destroy Trellis Plants", "Destroys all trellis plants in selected area.", false, true, DestroyTrellisPlants.class, "trellis_cleaner", false));
        bots.add(new BotDescriptor("flag", BotDescriptor.BotType.UTILS, "Survey Supporter", "Survey Supporter.", false, true, SurveySupport.class, "flag", false));
        bots.add(new BotDescriptor("dream_catcher", BotDescriptor.BotType.UTILS, "Collect dreams", "Collect dreams from all dream catchers in a dream catcher area.", true, true, CollectDreams.class, "dream_catcher", false));
        bots.add(new BotDescriptor("bugs", BotDescriptor.BotType.UTILS, "Catch bugs", "Catch bugs around player.", false, true, CatchBugsAround.class, "bugs", false));
        bots.add(new BotDescriptor("freeinv", BotDescriptor.BotType.UTILS, "Free inventory", "Free inventory with Area system.", true, true, FreeInvBot.class, "freeinv", false));
        bots.add(new BotDescriptor("travellerssack", BotDescriptor.BotType.UTILS, "Equip travellers sacks", "Equip travellers sacks.", false, true, EquipTravellersSacksFromBelt.class, "travellerssack", false));
        bots.add(new BotDescriptor("studytable", BotDescriptor.BotType.UTILS, "bot.studytable.title", "bot.studytable.desc", true, true, StudyDeskFiller.class, "studytable", false));
        bots.add(new BotDescriptor("swill_collector", BotDescriptor.BotType.UTILS, "Swill Collector", "Collects swill items from area and feeds to troughs/cisterns.", false, true, CollectSwillToTrough.class, "swillcollector", false));
        bots.add(new BotDescriptor("swill_to_trough", BotDescriptor.BotType.UTILS, "Swill To Trough", "Collects swill from area to selected trough (click to select).", false, true, CollectSwillInArea.class, "swillzone", false));
        bots.add(new BotDescriptor("qzone", BotDescriptor.BotType.UTILS, "Quality in Zone", "Scan the quality of all typical objects in the area.", false, true, InspectQualityBot.class, "qzone", false));
        bots.add(new BotDescriptor("autoflaction", BotDescriptor.BotType.UTILS, "Auto Flower Action", "Perform the specified flower pop-up menu action for all objects in the area.", false, true, AutoFlowerActionBot.class, "autoflaction", false));
        bots.add(new BotDescriptor("autoflaction", BotDescriptor.BotType.UTILS, "Drop Soil", "Drops soil from stockpile until there is 10 soil left in the stockpile..", false, true, SoilStockpileDropper.class, "dropsoil", false));
        bots.add(new BotDescriptor("measure_length", BotDescriptor.BotType.UTILS, "Zone Measure Tool", "Measure and mark zones on the ground. Select areas, view dimensions, and manage multiple selections.", false, true, ZoneMeasureTool.class, "measuring_length", false));
        bots.add(new BotDescriptor("fire", BotDescriptor.BotType.UTILS, "Fire Starter", "Ignites objects (Ovens, Smelters, Kilns, etc.) and refuels them if needed.", false, true, FireStarterAction.class, "fire", true));

        // BUILD
        bots.add(new BotDescriptor("dframe", BotDescriptor.BotType.BUILD, "Build Drying Frame", "Builds drying frame.", false, true, BuildDryingFrame.class, "dframe", true));
        bots.add(new BotDescriptor("cellar", BotDescriptor.BotType.BUILD, "Build Cellar", "Builds cellar.", false, true, BuildCellar.class, "cellar", false));
        bots.add(new BotDescriptor("ttub", BotDescriptor.BotType.BUILD, "Build Tub", "Builds tub.", false, true, BuildTtub.class, "ttub", false));
        bots.add(new BotDescriptor("cupboard", BotDescriptor.BotType.BUILD, "Build Cupboard", "Builds cupboard.", false, true, BuildCupboard.class, "cupboard", false));
        bots.add(new BotDescriptor("cheese_rack", BotDescriptor.BotType.BUILD, "Build Cheese Rack", "Builds cheese rack.", false, true, BuildCheeseRack.class, "cheese_rack", false));
        bots.add(new BotDescriptor("kiln", BotDescriptor.BotType.BUILD, "Build Kiln", "Builds kiln.", false, true, BuildKiln.class, "kiln", false));
        bots.add(new BotDescriptor("barrel", BotDescriptor.BotType.BUILD, "Build Barrel", "Builds barrel.", false, true, BuildBarrel.class, "barrel", false));
        bots.add(new BotDescriptor("chest", BotDescriptor.BotType.BUILD, "Build Chest", "Builds chest.", false, true, BuildChest.class, "chest", false));
        bots.add(new BotDescriptor("stone_casket", BotDescriptor.BotType.BUILD, "Build Stone Casket", "Builds stone casket.", false, true, BuildStoneCasket.class, "stonecasket", false));
        bots.add(new BotDescriptor("lchest", BotDescriptor.BotType.BUILD, "Build Large Chest", "Builds large chest.", false, true, BuildLargeChest.class, "lchest", true));
        bots.add(new BotDescriptor("tarkilnb", BotDescriptor.BotType.BUILD, "Build Tar Kiln", "Builds tar kiln.", false, true, BuildTarKiln.class, "tarkilnb", false));
        bots.add(new BotDescriptor("smoke_shed", BotDescriptor.BotType.BUILD, "Build Smoke Shed", "Builds smoke shed.", false, true, BuildSmokeShed.class, "smoke_shed", false));
        bots.add(new BotDescriptor("trellis", BotDescriptor.BotType.BUILD, "Build Trellis", "Builds trellis.", false, true, BuildTrellis.class, "trellis", false));
        bots.add(new BotDescriptor("htable", BotDescriptor.BotType.BUILD, "Build Herbalist Tables", "Builds herbalist tables.", false, true, BuildHerbalistTable.class, "htable", true));
        bots.add(new BotDescriptor("moundbed", BotDescriptor.BotType.BUILD, "Build Mound Bed", "Builds mound bed.", false, true, BuildMoundBed.class, "moundbed", true));
        bots.add(new BotDescriptor("crate", BotDescriptor.BotType.BUILD, "Build Crate", "Builds crates.", false, true, BuildCrate.class, "crate", true));

        // TOOLS (for debug)
        bots.add(new BotDescriptor("test1", BotDescriptor.BotType.TOOLS, "Test 1", "Debug test 1.", false, true, TESTMapv4.class, "test1", false));
        bots.add(new BotDescriptor("test2", BotDescriptor.BotType.TOOLS, "Show Active Tasks", "Shows all active tasks in NCore.", false, true, TESTShowActiveTasks.class, "test2", false));
        bots.add(new BotDescriptor("test4", BotDescriptor.BotType.TOOLS, "Test 4", "Debug test 4.", false, true, TESTbranchinvtransferpacks.class, "test4", false));
        bots.add(new BotDescriptor("test5", BotDescriptor.BotType.TOOLS, "Test 5", "Auxiliary deferred callback iterator for async reference validation.", false, true, TESTAuxIterProc.class, "test5", false));
        bots.add(new BotDescriptor("test7", BotDescriptor.BotType.TOOLS, "Test 7", "Debug test 7.", false, true, TESTselectfloweraction.class, "test7", false));
        bots.add(new BotDescriptor("test8", BotDescriptor.BotType.TOOLS, "Test 8", "Debug test 8.", false, true, TESTpf.class, "test8", false));
        bots.add(new BotDescriptor("test9", BotDescriptor.BotType.TOOLS, "Test 9", "Debug test 9.", false, true, TESTAvalaible.class, "test9", false));
        bots.add(new BotDescriptor("test12", BotDescriptor.BotType.TOOLS, "Test 12", "Debug test 12.", false, true, TestBot.class, "test12", false));
        bots.add(new BotDescriptor("chunknav_navigator", BotDescriptor.BotType.TOOLS, "ChunkNav Navigator", "Opens UI to navigate to areas using chunk-based navigation.", false, true, ChunkNavNavigatorBot.class, "test14", false));
        bots.add(new BotDescriptor("navstresstest", BotDescriptor.BotType.TOOLS, "Navigation Stress Test", "Continuously tests chunk navigation between random areas. Results saved to JSON file.", false, true, NavigationStressTest.class, "test15", true));
        //bots.add(new BotDescriptor("testzonepatroller", BotDescriptor.BotType.UTILS, "Test Zone Patroller", "Patrols 15 test zones (test1-test15) every 30 minutes, returning to test16 between cycles.", true, true, TestZonePatroller.class, "worldexplorer", false));
    }

    public static BotDescriptor byId(String id) {
        for (BotDescriptor bot : bots) {
            if (bot.id.equals(id)) return bot;
        }
        return null;
    }

    public static List<BotDescriptor> byType(BotDescriptor.BotType type) {
        return bots.stream().filter(b -> b.type == type).collect(Collectors.toList());
    }

    public static List<BotDescriptor> allowedInBotMenu() {
        return bots.stream().filter(b -> b.allowedAsItemInBotMenu).collect(Collectors.toList());
    }
}