package nurgling;

import haven.*;
import haven.render.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class NStyle {
    public static Text.Foundry fcomboitems = new Text.Foundry(Text.sans, 16).aa(true);
    public static Text.Furnace meter = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 12, Color.WHITE).aa(true), 2, 1, new Color(60, 30, 30));
    public static Text.Furnace gmeter = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 12, new Color(102, 178, 12)).aa(true), 2, 1, new Color(60, 30, 30));
    public static Text.Foundry areastitle = new Text.Foundry(Text.serif, 15, Color.WHITE);
    public static Text.Foundry flower = new Text.Foundry(Text.sans, 12, new Color(255, 250, 205)).aa(true);
    public static Text.Foundry iiqual = new Text.Foundry(Text.sans, 12, new Color(0, 0, 0)).aa(true);

    public static final RichText.Foundry nifnd = new RichText.Foundry(Resource.remote(), java.awt.font.TextAttribute.FAMILY, "SansSerif", java.awt.font.TextAttribute.SIZE, UI.scale(14)).aa(true);
    public static Text.Furnace openings = new PUtils.BlurFurn(new Text.Foundry(Text.sans.deriveFont(Font.BOLD, UI.scale(16)), 16, Color.WHITE).aa(true), 1, 1, new Color(60, 30, 30));
    public static Text.Furnace selopenings = new PUtils.BlurFurn(new Text.Foundry(Text.sans.deriveFont(Font.BOLD, UI.scale(16)), 16, Color.GREEN).aa(true), 1, 1, new Color(60, 30, 30));
    public static Text.Furnace slotnums = new Text.Foundry(Text.sans.deriveFont(Font.BOLD, UI.scale(16)), 16, new Color(119, 153, 116,255)).aa(true);
    public static Text.Furnace mip = new PUtils.BlurFurn(new Text.Foundry(Text.sans.deriveFont(Font.BOLD, UI.scale(16)), 16, Color.GREEN).aa(true), 1, 1, new Color(60, 30, 30));
    public static Text.Furnace eip = new PUtils.BlurFurn(new Text.Foundry(Text.sans.deriveFont(Font.BOLD, UI.scale(16)), 16, Color.RED).aa(true), 1, 1, new Color(60, 30, 30));
    public static Text.Furnace hotkey = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 12, Color.WHITE).aa(true), 1, 1, new Color(0, 0, 0));
    public static final TexI[] removei = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/removeItem/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/removeItem/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/removeItem/h"))};

    public static final BufferedImage[] cbtni = new BufferedImage[]{
            Resource.loadsimg("nurgling/hud/wnd/cbtnu"),
            Resource.loadsimg("nurgling/hud/wnd/cbtnd"),
            Resource.loadsimg("nurgling/hud/wnd/cbtnh")};

    public static final TexI[] settingsi = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/settings/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/settings/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/settings/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/settings/dh"))};

    public static final TexI[] locki = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lock/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lock/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lock/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lock/dh"))};

    public static final TexI[] visi = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/vis/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/vis/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/vis/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/vis/dh"))};

    public static final TexI[] flipi = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/flip/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/flip/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/flip/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/flip/dh"))};

    public static final Tex[] gear = new Tex[]{
            Resource.loadtex("nurgling/hud/gear/0"),
            Resource.loadtex("nurgling/hud/gear/1"),
            Resource.loadtex("nurgling/hud/gear/2"),
            Resource.loadtex("nurgling/hud/gear/3"),
            Resource.loadtex("nurgling/hud/gear/4"),
            Resource.loadtex("nurgling/hud/gear/5"),
            Resource.loadtex("nurgling/hud/gear/6"),
            Resource.loadtex("nurgling/hud/gear/7"),
            Resource.loadtex("nurgling/hud/gear/8"),
            Resource.loadtex("nurgling/hud/gear/9"),
            Resource.loadtex("nurgling/hud/gear/10"),
            Resource.loadtex("nurgling/hud/gear/11")};

    public static final Tex[] alarm = new Tex[]{
            Resource.loadtex("nurgling/hud/alarm/0"),
            Resource.loadtex("nurgling/hud/alarm/1"),
            Resource.loadtex("nurgling/hud/alarm/2"),
            Resource.loadtex("nurgling/hud/alarm/3"),
            Resource.loadtex("nurgling/hud/alarm/4"),
            Resource.loadtex("nurgling/hud/alarm/5"),
            Resource.loadtex("nurgling/hud/alarm/6"),
            Resource.loadtex("nurgling/hud/alarm/7"),
            Resource.loadtex("nurgling/hud/alarm/8"),
            Resource.loadtex("nurgling/hud/alarm/9"),
            Resource.loadtex("nurgling/hud/alarm/10"),
            Resource.loadtex("nurgling/hud/alarm/11")};

    public static final Tex[] question = new Tex[]{
            Resource.loadtex("nurgling/hud/question/0"),
            Resource.loadtex("nurgling/hud/question/1"),
            Resource.loadtex("nurgling/hud/question/2"),
            Resource.loadtex("nurgling/hud/question/3"),
            Resource.loadtex("nurgling/hud/question/4"),
            Resource.loadtex("nurgling/hud/question/5"),
            Resource.loadtex("nurgling/hud/question/6"),
            Resource.loadtex("nurgling/hud/question/7"),
            Resource.loadtex("nurgling/hud/question/8"),
            Resource.loadtex("nurgling/hud/question/9"),
            Resource.loadtex("nurgling/hud/question/10"),
            Resource.loadtex("nurgling/hud/question/11")};

    public static final TexI[] canceli = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/cancel/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/cancel/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/cancel/h"))};

    public static final TexI[] addarea = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addarea/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addarea/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addarea/h"))};

    public static final TexI[] catmenu = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/catmenu/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/catmenu/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/catmenu/h"))};

    public static final TexI[] importb = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/importb/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/importb/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/importb/h"))};

    public static final TexI[] exportb = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/exportb/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/exportb/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/exportb/h"))};


    public static final TexI[] addfolder = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addfolder/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addfolder/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addfolder/h"))};

    public static final TexI[] toTake = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/toTake/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/toTake/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/toTake/h"))};

    public static final TexI[] toPut = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/toPut/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/toPut/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/toPut/h"))};

    public static final TexI[] add = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/add/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/add/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/add/h"))};

    public static final TexI[] remove = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/remove/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/remove/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/remove/h"))};

    public static final TexI[] auto = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/auto/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/auto/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/auto/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/auto/dh"))
    };

    public static final TexI[] record = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/record_4states/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/record_4states/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/record_4states/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/record_4states/dh"))
    };

    public static final TexI[] hearthFire = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/fire/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/fire/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/fire/h")),
    };

    public static final TexI[] upSquareArrow = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v1/UP/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v1/UP/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v1/UP/h")),
    };

    public static final TexI[] downSquareArrow = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v1/DOWN/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v1/DOWN/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v1/DOWN/h")),
    };


    public static final TexI[] crossSquare = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/square/cross/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/square/cross/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/square/cross/h")),
    };

    private final static ArrayList<BufferedImage> hlight = new ArrayList<>();

    static {
        for (int i = 0; i < 6; i++) {
            hlight.add(Resource.loadsimg("nurgling/hud/buttons/hlight/" + i));
        }
    }

    public static BufferedImage getHLight(NUI ui) {
        return hlight.get((int) ((ui.tickId / 5) % 6));
    }

    public enum CropMarkers {
        RED,
        BLUE,
        GRAY,
        YELLOW,
        ORANGE,
        GREEN
    }

    public static final HashMap<CropMarkers, TexI> iCropMap = new HashMap<CropMarkers, TexI>() {
        {
            put(CropMarkers.RED, new TexI(Resource.loadsimg("crop/red")));
            put(CropMarkers.ORANGE, new TexI(Resource.loadsimg("crop/orange")));
            put(CropMarkers.YELLOW, new TexI(Resource.loadsimg("crop/yellow")));
            put(CropMarkers.BLUE, new TexI(Resource.loadsimg("crop/blue")));
            put(CropMarkers.GRAY, new TexI(Resource.loadsimg("crop/gray")));
            put(CropMarkers.GREEN, new TexI(Resource.loadsimg("crop/green")));
        }
    };
    static final HashMap<Long, TexI> iCropStageMap3 = new HashMap<Long, TexI>() {
        {
            put(1L, new TexI(Resource.loadsimg("crop/yellow_1_3")));
            put(2L, new TexI(Resource.loadsimg("crop/yellow_2_3")));
        }
    };
    static final HashMap<Long, TexI> iCropStageMap4 = new HashMap<Long, TexI>() {
        {
            put(1L, new TexI(Resource.loadsimg("crop/yellow_1_4")));
            put(2L, new TexI(Resource.loadsimg("crop/yellow_2_4")));
            put(3L, new TexI(Resource.loadsimg("crop/yellow_3_4")));
        }
    };
    static final HashMap<Long, TexI> iCropStageMap5 = new HashMap<Long, TexI>() {
        {
            put(1L, new TexI(Resource.loadsimg("crop/yellow_1_5")));
            put(2L, new TexI(Resource.loadsimg("crop/yellow_2_5")));
            put(3L, new TexI(Resource.loadsimg("crop/yellow_3_5")));
            put(4L, new TexI(Resource.loadsimg("crop/yellow_4_5")));
        }
    };
    static final HashMap<Long, TexI> iCropStageMap6 = new HashMap<Long, TexI>() {
        {
            put(1L, new TexI(Resource.loadsimg("crop/yellow_1_6")));
            put(2L, new TexI(Resource.loadsimg("crop/yellow_2_6")));
            put(3L, new TexI(Resource.loadsimg("crop/yellow_3_6")));
            put(4L, new TexI(Resource.loadsimg("crop/yellow_4_6")));
            put(5L, new TexI(Resource.loadsimg("crop/yellow_5_6")));
        }
    };

    public static TexI getCropTexI(long curent, long max) {
        switch ((int) max) {
            case 3:
                return iCropStageMap3.get(curent);
            case 4:
                return iCropStageMap4.get(curent);
            case 5:
                return iCropStageMap5.get(curent);
            case 6:
                return iCropStageMap6.get(curent);
        }
        return null;
    }

    private static Tiler ridge;

    public static HashMap<String, Tileset> customTileRes = new HashMap<String, Tileset>() {
        {
            put("ridge", Resource.local().loadwait("tiles/ridge").layer(Tileset.class));
        }
    };

    public static Tiler getRidge() {
        if (ridge == null)
            ridge = customTileRes.get("ridge").tfac().create(7001, customTileRes.get("ridge"));
        return ridge;
    }

    public static enum Container {
        FREE,
        NOTFREE,
        FULL
    }

    public static HashMap<Integer, Texture2D.Sampler2D> dkinAlt = new HashMap<>();

    static {
        dkinAlt.put(0, new TexI(Resource.loadimg("marks/kintears/white")).st().data);
        dkinAlt.put(1, new TexI(Resource.loadimg("marks/kintears/green")).st().data);
        dkinAlt.put(2, new TexI(Resource.loadimg("marks/kintears/red")).st().data);
        dkinAlt.put(3, new TexI(Resource.loadimg("marks/kintears/blue")).st().data);
        dkinAlt.put(4, new TexI(Resource.loadimg("marks/kintears/turquoise")).st().data);
        dkinAlt.put(5, new TexI(Resource.loadimg("marks/kintears/yellow")).st().data);
        dkinAlt.put(6, new TexI(Resource.loadimg("marks/kintears/violet")).st().data);
        dkinAlt.put(7, new TexI(Resource.loadimg("marks/kintears/pink")).st().data);
    }

    public static HashMap<String, Resource.Saved> iconMap = new HashMap<>();
    static {
        iconMap.put("gfx/terobjs/vehicle/wheelbarrow", new Resource.Saved(Resource.remote(),"mm/wheelbarrow",-1));
        iconMap.put("gfx/terobjs/items/truffle",new Resource.Saved(Resource.remote(),"mm/truffle",-1));
        iconMap.put("gfx/terobjs/cauldron",new Resource.Saved(Resource.remote(),"mm/cauldron",-1));
        iconMap.put("gfx/kritter/horse/stallion",new Resource.Saved(Resource.remote(),"mm/horse",-1));
        iconMap.put("gfx/kritter/horse/mare",new Resource.Saved(Resource.remote(),"mm/horse",-1));
        iconMap.put("gfx/terobjs/anvil",new Resource.Saved(Resource.remote(),"mm/anvil",-1));
        iconMap.put("gfx/terobjs/vehicle/rowboat",new Resource.Saved(Resource.remote(),"mm/rowboat",-1));
        iconMap.put("gfx/terobjs/vehicle/knarr",new Resource.Saved(Resource.remote(),"mm/knarr",-1));
        iconMap.put("gfx/terobjs/vehicle/snekkja",new Resource.Saved(Resource.remote(),"mm/snekkja",-1));
        iconMap.put("gfx/terobjs/vehicle/dugout",new Resource.Saved(Resource.remote(),"mm/dugout",-1));
        iconMap.put("gfx/terobjs/road/milestone-stone-m",new Resource.Saved(Resource.remote(),"mm/milestones",-1));
        iconMap.put("gfx/terobjs/road/milestone-stone-e",new Resource.Saved(Resource.remote(),"mm/milestonese",-1));
        iconMap.put("gfx/terobjs/road/milestone-wood-m",new Resource.Saved(Resource.remote(),"mm/milestonew",-1));
        iconMap.put("gfx/terobjs/road/milestone-wood-e",new Resource.Saved(Resource.remote(),"mm/milestonewe",-1));
        iconMap.put("gfx/terobjs/candelabrum",new Resource.Saved(Resource.remote(),"mm/candelabrum",-1));
        iconMap.put("gfx/kritter/stalagoomba/stalagoomba",new Resource.Saved(Resource.remote(),"mm/stalagoomba",-1));
        iconMap.put("gfx/terobjs/claim",new Resource.Saved(Resource.remote(),"mm/claim",-1));
        iconMap.put("gfx/terobjs/items/gems/gemstone",new Resource.Saved(Resource.remote(),"mm/gem",-1));
        iconMap.put("gfx/terobjs/vehicle/cart",new Resource.Saved(Resource.remote(),"mm/cart",-1));
        iconMap.put("gfx/terobjs/vehicle/plow",new Resource.Saved(Resource.remote(),"mm/plow",-1));
        iconMap.put("gfx/terobjs/map/cavepuddle",new Resource.Saved(Resource.remote(),"mm/clay-cave",-1));
        iconMap.put("gfx/terobjs/minehole",new Resource.Saved(Resource.remote(),"mm/down",-1));
        iconMap.put("gfx/terobjs/ladder",new Resource.Saved(Resource.remote(),"mm/up",-1));
    }

    public static HashMap<String, String> iconName = new HashMap<>();
    static {
        iconName.put("mm/wheelbarrow", "Wheelbarrow");
        iconName.put("mm/truffle", "Truffle");
        iconName.put("mm/cauldron", "Cauldron");
        iconName.put("mm/horse", "Horse");
        iconName.put("mm/anvil", "Anvil");
        iconName.put("mm/rowboat", "Rowboat");
        iconName.put("mm/knarr", "Knarr");
        iconName.put("mm/snekkja", "Snekkja");
        iconName.put("mm/dugout", "Dugout");
        iconName.put("mm/milestones", "Milestone");
        iconName.put("mm/milestonese", "Milestone");
        iconName.put("mm/milestonew", "Milestone");
        iconName.put("mm/milestonewe", "Milestone");
        iconName.put("mm/candelabrum", "Candelabrum");
        iconName.put("mm/stalagoomba", "Stalagoomba");
        iconName.put("mm/claim", "Claim");
        iconName.put("mm/gem", "Gem");
        iconName.put("mm/cart", "Cart");
        iconName.put("mm/plow", "Plow");
        iconName.put("mm/clay-cave", "Cave clay");
    }
}
