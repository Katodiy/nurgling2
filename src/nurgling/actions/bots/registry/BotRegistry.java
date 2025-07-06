package nurgling.actions.bots.registry;

import nurgling.actions.FillWaterskins;
import nurgling.actions.PumpkinFarmer;
import nurgling.actions.bots.*;
import nurgling.actions.bots.CarrotFarmerQ;
import nurgling.actions.test.*;

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
                RoutePointNavigator.class,
                "attacknearcurs",
                false
        ));

        // RESOURCES
        bots.add(new BotDescriptor("choper", BotDescriptor.BotType.RESOURCES, "Chopper", "Chops trees.", false, true, Chopper.class, "choper", false));
        bots.add(new BotDescriptor("chipper", BotDescriptor.BotType.RESOURCES, "Chipper", "Chips stuff.", false, true, Chipper.class, "chipper", true));
        bots.add(new BotDescriptor("pblocks", BotDescriptor.BotType.RESOURCES, "Prepare Blocks", "Prepares blocks.", false, true, PrepareBlocks.class, "pblocks", false));
        bots.add(new BotDescriptor("pboards", BotDescriptor.BotType.RESOURCES, "Prepare Boards", "Prepares boards.", false, true, PrepareBoards.class, "pboards", false));
        bots.add(new BotDescriptor("clay", BotDescriptor.BotType.RESOURCES, "Clay Digger", "Digs clay.", false, true, ClayDigger.class, "clay", true));
        bots.add(new BotDescriptor("bark", BotDescriptor.BotType.RESOURCES, "Collect Bark", "Collects bark.", false, true, CollectBark.class, "bark", true));
        bots.add(new BotDescriptor("bough", BotDescriptor.BotType.RESOURCES, "Collect Bough", "Collects boughs.", false, true, CollectBough.class, "bough", true));
        bots.add(new BotDescriptor("leaf", BotDescriptor.BotType.RESOURCES, "Collect Leaf", "Collects leaves.", false, true, CollectLeaf.class, "leaf", true));
        bots.add(new BotDescriptor("fisher", BotDescriptor.BotType.RESOURCES, "Fishing", "Fishes fish.", false, true, Fishing.class, "fisher", true));
        bots.add(new BotDescriptor("plower", BotDescriptor.BotType.RESOURCES, "Plower", "Plows fields.", false, true, Plower.class, "plower", true));

        // PRODUCTIONS
        bots.add(new BotDescriptor("smelter", BotDescriptor.BotType.PRODUCTIONS, "Smelter", "Smelts ore.", true, true, SmelterAction.class, "smelter", true));
        bots.add(new BotDescriptor("backer", BotDescriptor.BotType.PRODUCTIONS, "Baker", "Bakes stuff.", true, true, BackerAction.class, "backer", true));
        bots.add(new BotDescriptor("ugardenpot", BotDescriptor.BotType.PRODUCTIONS, "Ungarden Pot", "Ungardens pots.", true, true, UnGardentPotAction.class, "ugardenpot", true));
        bots.add(new BotDescriptor("butcher", BotDescriptor.BotType.PRODUCTIONS, "Butcher", "Butchers animals.", true, true, Butcher.class, "butcher", false));
        bots.add(new BotDescriptor("hides", BotDescriptor.BotType.PRODUCTIONS, "Handle hides.", "Handles hides.", true, true, DFrameHidesAction.class, "hides", true));
        bots.add(new BotDescriptor("fishroast", BotDescriptor.BotType.PRODUCTIONS, "Spit Roast", "Roasts fish.", false, true, FriedFish.class, "fishroast", true));
        bots.add(new BotDescriptor("leather", BotDescriptor.BotType.PRODUCTIONS, "Leather Action", "Makes leather.", true, true, LeatherAction.class, "leather", true));
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

        // BATTLE
        bots.add(new BotDescriptor("reagro", BotDescriptor.BotType.BATTLE, "Reagro", "Reagros enemies.", true, true, Reagro.class, "reagro", false));
        bots.add(new BotDescriptor("attacknearcurs", BotDescriptor.BotType.BATTLE, "Aggro Near Cursor", "Aggros near cursor.", true, true, AggroNearCurs.class, "attacknearcurs", false));
        bots.add(new BotDescriptor("attacknear", BotDescriptor.BotType.BATTLE, "Aggro Nearest", "Aggros nearest enemy.", true, true, AggroNearest.class, "attacknear", false));
        bots.add(new BotDescriptor("attacknearborka", BotDescriptor.BotType.BATTLE, "Aggro Nearest Borka", "Aggros nearest Borka.", true, true, AggroNearestBorka.class, "attacknearborka", false));
        bots.add(new BotDescriptor("attackall", BotDescriptor.BotType.BATTLE, "Attack All", "Attacks all enemies.", true, true, AttackAll.class, "attackall", false));
        bots.add(new BotDescriptor("taming", BotDescriptor.BotType.BATTLE, "Tame an animal", "Attacks the nearest animal waiting to be tamed, allows it to escape. Ties the animal on a rope if the corresponding setting is set. Fighting on your own", false, true, TaimingAnimal.class, "taming", false));

        // FARMING
        bots.add(new BotDescriptor("turnip", BotDescriptor.BotType.FARMING, "Turnip Farmer", "Automatically harvests and replants turnips.", true, true, TurnipsFarmer.class, "turnip", false));
        bots.add(new BotDescriptor("carrot", BotDescriptor.BotType.FARMING, "Carrot Farmer", "Automatically harvests and replants carrots.", true, true, CarrotFarmer.class, "carrot", false));
        bots.add(new BotDescriptor("beetroot", BotDescriptor.BotType.FARMING, "Beetroot Farmer", "Automatically harvests and replants beetroots.", true, true, BeetrootFarmer.class, "beetroot", false));
        bots.add(new BotDescriptor("red_onion", BotDescriptor.BotType.FARMING, "Red Onion Farmer", "Automatically harvests and replants red onions.", true, true, RedOnionFarmer.class, "red_onion", false));
        bots.add(new BotDescriptor("yellow_onion", BotDescriptor.BotType.FARMING, "Yellow Onion Farmer", "Automatically harvests and replants yellow onions.", true, true, YellowOnionFarmer.class, "yellow_onion", false));
        bots.add(new BotDescriptor("garlic", BotDescriptor.BotType.FARMING, "Garlic Farmer", "Automatically harvests and replants garlic.", true, true, GarlicFarmer.class, "garlic", false));
        bots.add(new BotDescriptor("hemp", BotDescriptor.BotType.FARMING, "Hemp Farmer", "Automatically harvests and replants hemp.", true, true, HempFarmer.class, "hemp", false));
        bots.add(new BotDescriptor("flax", BotDescriptor.BotType.FARMING, "Flax Farmer", "Automatically harvests and replants flax.", true, true, FlaxFarmer.class, "flax", false));
        bots.add(new BotDescriptor("lettuce", BotDescriptor.BotType.FARMING, "Lettuce Farmer", "Automatically harvests and replants lettuce.", true, true, LettuceFarmer.class, "lettuce", true));
        bots.add(new BotDescriptor("pumpkin", BotDescriptor.BotType.FARMING, "Pumpkin Farmer", "Automatically harvests and replants pumpkins.", true, true, PumpkinFarmer.class, "pumpkin", true));
        bots.add(new BotDescriptor("barley", BotDescriptor.BotType.FARMING, "Barley Farmer", "Automatically harvests and replants barley.", true, true, BarleyFarmer.class, "barley", false));
        bots.add(new BotDescriptor("millet", BotDescriptor.BotType.FARMING, "Millet Farmer", "Automatically harvests and replants millet.", true, true, MilletFarmer.class, "millet", false));
        bots.add(new BotDescriptor("wheat", BotDescriptor.BotType.FARMING, "Wheat Farmer", "Automatically harvests and replants wheat.", true, true, WheatFarmer.class, "wheat", false));
        bots.add(new BotDescriptor("poppy", BotDescriptor.BotType.FARMING, "Poppy Farmer", "Automatically harvests and replants poppies.", true, true, PoppyFarmer.class, "poppy", true));

        bots.add(new BotDescriptor("pipeweed", BotDescriptor.BotType.FARMING, "Pipeweed Farmer", "Automatically harvests and replants pipeweed.", true, true, PipeweedFarmer.class, "pipeweed", false));
        bots.add(new BotDescriptor("goats", BotDescriptor.BotType.FARMING, "Goat Manager", "Manages goat herds.", true, true, GoatsAction.class, "goats", false));
        bots.add(new BotDescriptor("sheeps", BotDescriptor.BotType.FARMING, "Sheep Manager", "Manages sheep herds.", true, true, SheepsAction.class, "sheeps", false));
        bots.add(new BotDescriptor("pigs", BotDescriptor.BotType.FARMING, "Pig Manager", "Manages pig herds.", true, true, PigsAction.class, "pigs", false));
        bots.add(new BotDescriptor("horses", BotDescriptor.BotType.FARMING, "Horse Manager", "Manages horses.", true, true, HorsesAction.class, "horses", false));
        bots.add(new BotDescriptor("cows", BotDescriptor.BotType.FARMING, "Cow Manager", "Manages cows.", true, true, nurgling.actions.bots.CowsAction.class, "cows", false));
        bots.add(new BotDescriptor("chicken", BotDescriptor.BotType.FARMING, "Chicken Manager", "Manages chicken coops.", true, true, KFC.class, "chicken", true));
        bots.add(new BotDescriptor("rabbit", BotDescriptor.BotType.FARMING, "Rabbit Manager", "Manages rabbit hutches.", true, true, RabbitMaster.class, "rabbit", true));
        bots.add(new BotDescriptor("bee", BotDescriptor.BotType.FARMING, "Beehive Manager", "Collects honey and wax from beehives.", true, true, HoneyAndWaxCollector.class, "bee", true));
        bots.add(new BotDescriptor("cows", BotDescriptor.BotType.FARMING, "Teimdeer Manager", "Manages teimdeer.", true, true, nurgling.actions.bots.DeersAction.class, "cows", false));

        // FARMING QUALITY
        bots.add(new BotDescriptor("turnipq", BotDescriptor.BotType.FARMING_QUALITY, "Turnip Farmer Quality", "Automatically harvests and replants turnips in X*Y cell patches.", true, true, TurnipsFarmerQ.class, "turnipq", false));
        bots.add(new BotDescriptor("carrotq", BotDescriptor.BotType.FARMING_QUALITY, "Carrot Farmer Quality", "Automatically harvests and replants carrots in X*Y cell patches.", true, true, CarrotFarmerQ.class, "carrotq", false));
        bots.add(new BotDescriptor("beetrootq", BotDescriptor.BotType.FARMING_QUALITY, "Beetroot Farmer Quality", "Automatically harvests and replants beetroot in X*Y cell patches.", true, true, BeetrootFarmerQ.class, "beetrootq", false));
        bots.add(new BotDescriptor("red_onionq", BotDescriptor.BotType.FARMING_QUALITY, "Red Onion Farmer Quality", "Automatically harvests and replants red onions in X*Y cell patches.", true, true, RedOnionFarmerQ.class, "red_onionq", false));
        bots.add(new BotDescriptor("yellow_onionq", BotDescriptor.BotType.FARMING_QUALITY, "Yellow Onion Farmer Quality", "Automatically harvests and replants yellow onions in X*Y cell patches.", true, true, YellowOnionFarmerQ.class, "yellow_onionq", false));
        bots.add(new BotDescriptor("garlicq", BotDescriptor.BotType.FARMING_QUALITY, "Garlic Farmer Quality", "Automatically harvests and replants garlic in X*Y cell patches.", true, true, GarlicFarmerQ.class, "garlicq", false));
        bots.add(new BotDescriptor("hempq", BotDescriptor.BotType.FARMING_QUALITY, "Hemp Farmer Quality", "Automatically harvests and replants hemp in X*Y cell patches.", true, true, HempFarmerQ.class, "hempq", false));
        bots.add(new BotDescriptor("flaxq", BotDescriptor.BotType.FARMING_QUALITY, "Flax Farmer Quality", "Automatically harvests and replants flax in X*Y cell patches.", true, true, FlaxFarmerQ.class, "flaxq", false));
        bots.add(new BotDescriptor("barleyq", BotDescriptor.BotType.FARMING_QUALITY, "Barley Farmer Quality", "Automatically harvests and replants barley in X*Y cell patches.", true, true, BarleyFarmerQ.class, "barleyq", false));
        bots.add(new BotDescriptor("milletq", BotDescriptor.BotType.FARMING_QUALITY, "Millet Farmer Quality", "Automatically harvests and replants millet in X*Y cell patches.", true, true, MilletFarmerQ.class, "milletq", false));
        bots.add(new BotDescriptor("wheatq", BotDescriptor.BotType.FARMING_QUALITY, "Wheat Farmer Quality", "Automatically harvests and replants wheat in X*Y cell patches.", true, true, WheatFarmerQ.class, "wheatq", false));
        bots.add(new BotDescriptor("poppyq", BotDescriptor.BotType.FARMING_QUALITY, "Poppy Farmer Quality", "Automatically harvests and replants poppy in X*Y cell patches.", true, true, PoppyFarmerQ.class, "poppyq", false));

        // UTILS
        bots.add(new BotDescriptor("shieldsword", BotDescriptor.BotType.UTILS, "Equip Shield/Sword", "Equips shield and sword.", true, true, EquipShieldSword.class, "shieldsword", false));
        bots.add(new BotDescriptor("filwater", BotDescriptor.BotType.UTILS, "Fill Waterskins", "Fills waterskins.", true, true, FillWaterskins.class, "filwater", false));
        bots.add(new BotDescriptor("unbox", BotDescriptor.BotType.UTILS, "Free Containers", "Frees containers in area.", false, true, FreeContainersInArea.class, "unbox", false));
        bots.add(new BotDescriptor("water_cheker", BotDescriptor.BotType.UTILS, "Check Water", "Checks water.", false, true, CheckWater.class, "water_cheker", false));
        bots.add(new BotDescriptor("clay_cheker", BotDescriptor.BotType.UTILS, "Check Clay", "Checks clay.", false, true, CheckClay.class, "clay_cheker", true));
        bots.add(new BotDescriptor("clover", BotDescriptor.BotType.UTILS, "Feed Clover", "Feeds clover.", false, true, FeedClover.class, "clover", false));
        bots.add(new BotDescriptor("collectalltopile", BotDescriptor.BotType.UTILS, "Collect To Pile", "Collects same items from earth.", false, true, CollectSameItemsFromEarth.class, "collectalltopile", true));
        bots.add(new BotDescriptor("worldexplorer", BotDescriptor.BotType.UTILS, "World Explorer", "Explores the world.", false, true, WorldExplorer.class, "worldexplorer", true));
        bots.add(new BotDescriptor("lift", BotDescriptor.BotType.UTILS, "Transfer Liftable", "Lifts items.", false, true, TransferLiftable.class, "lift", false));
        bots.add(new BotDescriptor("loading", BotDescriptor.BotType.UTILS, "Transfer To Vehicle", "Loads vehicle.", false, true, TransferToVeh.class, "loading", false));
        bots.add(new BotDescriptor("unloading", BotDescriptor.BotType.UTILS, "Transfer From Vehicle", "Unloads vehicle.", false, true, TransferFromVeh.class, "unloading", false));
        bots.add(new BotDescriptor("swap", BotDescriptor.BotType.UTILS, "Swap Vehicles", "Swaps between vehicles.", false, true, TransferFromVehToVeh.class, "swap", false));
        bots.add(new BotDescriptor("eater", BotDescriptor.BotType.UTILS, "Eating bot", "Eat in the food area.", true, true, Eater.class, "eater", false));
        bots.add(new BotDescriptor("zoneminer", BotDescriptor.BotType.UTILS, "Mine in area", "Mine rocks in the area.", false, true, MineAction.class, "zoneminer", true));
        bots.add(new BotDescriptor("bed", BotDescriptor.BotType.UTILS, "Go to bed", "Go to any free bed in a bed area nearby.", true, false, Sleep.class, "bed", false));
        bots.add(new BotDescriptor("unbox", BotDescriptor.BotType.UTILS, "Create piles with soil", "Create piles with soil in area.", false, true, CreateSoilPiles.class, "unbox", false));
        bots.add(new BotDescriptor("test1", BotDescriptor.BotType.UTILS, "Destroyer", "Destroy objects in area.", false, true, Destroyer.class, "destroy", false));
        bots.add(new BotDescriptor("test2", BotDescriptor.BotType.UTILS, "Survey Supporter", "Survey Supporter.", false, true, SurveySupport.class, "flag", false));
        bots.add(new BotDescriptor("dream_catcher", BotDescriptor.BotType.UTILS, "Collect dreams", "Collect dreams from all dream catchers in a dream catcher area.", true, true, CollectDreams.class, "dream_catcher", false));
        bots.add(new BotDescriptor("bugs", BotDescriptor.BotType.UTILS, "Catch bugs", "Catch bugs around player.", false, true, CatchBugsAround.class, "bugs", false));

        // BUILD
        bots.add(new BotDescriptor("dframe", BotDescriptor.BotType.BUILD, "Build Drying Frame", "Builds drying frame.", false, true, BuildDryingFrame.class, "dframe", true));
        bots.add(new BotDescriptor("cellar", BotDescriptor.BotType.BUILD, "Build Cellar", "Builds cellar.", false, true, BuildCellar.class, "cellar", false));
        bots.add(new BotDescriptor("ttub", BotDescriptor.BotType.BUILD, "Build Tub", "Builds tub.", false, true, BuildTtub.class, "ttub", false));
        bots.add(new BotDescriptor("cupboard", BotDescriptor.BotType.BUILD, "Build Cupboard", "Builds cupboard.", false, true, BuildCupboard.class, "cupboard", false));
        bots.add(new BotDescriptor("cheese_rack", BotDescriptor.BotType.BUILD, "Build Cheese Rack", "Builds cheese rack.", false, true, BuildCheeseRack.class, "cheese_rack", false));
        bots.add(new BotDescriptor("kiln", BotDescriptor.BotType.BUILD, "Build Kiln", "Builds kiln.", false, true, BuildKiln.class, "kiln", false));
        bots.add(new BotDescriptor("barrel", BotDescriptor.BotType.BUILD, "Build Barrel", "Builds barrel.", false, true, BuildBarrel.class, "barrel", false));
        bots.add(new BotDescriptor("chest", BotDescriptor.BotType.BUILD, "Build Chest", "Builds chest.", false, true, BuildChest.class, "chest", false));
        bots.add(new BotDescriptor("lchest", BotDescriptor.BotType.BUILD, "Build Large Chest", "Builds large chest.", false, true, BuildLargeChest.class, "lchest", true));
        bots.add(new BotDescriptor("tarkilnb", BotDescriptor.BotType.BUILD, "Build Tar Kiln", "Builds tar kiln.", false, true, BuildTarKiln.class, "tarkilnb", false));
        bots.add(new BotDescriptor("smoke_shed", BotDescriptor.BotType.BUILD, "Build Smoke Shed", "Builds smoke shed.", false, true, BuildSmokeShed.class, "smoke_shed", false));

        // TOOLS (for debug)
        bots.add(new BotDescriptor("test1", BotDescriptor.BotType.TOOLS, "Test 1", "Debug test 1.", false, true, TESTMapv4.class, "test1", false));
        bots.add(new BotDescriptor("test2", BotDescriptor.BotType.TOOLS, "Test 2", "Debug test 2.", false, true, TESTFillCauldron.class, "test2", false));
        bots.add(new BotDescriptor("test4", BotDescriptor.BotType.TOOLS, "Test 4", "Debug test 4.", false, true, TESTbranchinvtransferpacks.class, "test4", false));
        bots.add(new BotDescriptor("test5", BotDescriptor.BotType.TOOLS, "Test 5", "Debug test 5.", false, true, TESTfreeStockpilesAndTransfer.class, "test5", false));
        bots.add(new BotDescriptor("test7", BotDescriptor.BotType.TOOLS, "Test 7", "Debug test 7.", false, true, TESTselectfloweraction.class, "test7", false));
        bots.add(new BotDescriptor("test8", BotDescriptor.BotType.TOOLS, "Test 8", "Debug test 8.", false, true, TESTpf.class, "test8", false));
        bots.add(new BotDescriptor("test9", BotDescriptor.BotType.TOOLS, "Test 9", "Debug test 9.", false, true, TESTAvalaible.class, "test9", false));
        bots.add(new BotDescriptor("test10", BotDescriptor.BotType.TOOLS, "Test 10", "Debug test 10.", false, true, TESTGlobalPf.class, "test10", false));
        bots.add(new BotDescriptor("test10", BotDescriptor.BotType.TOOLS, "Test 10", "Debug test 10.", false, true, TESTGlobalPFCheckOrphans.class, "test10", false));

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