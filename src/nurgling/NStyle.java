package nurgling;

import haven.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class NStyle
{
    public static Text.Foundry fcomboitems = new Text.Foundry(Text.sans, 16).aa(true);
    public static Text.Furnace meter = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 12, Color.WHITE).aa(true), 2, 1, new Color(60,30,30));
    public static Text.Foundry areastitle = new Text.Foundry(Text.serif, 15, Color.WHITE);
    public static Text.Foundry flower = new Text.Foundry(Text.sans, 12,new Color(255, 250, 205)).aa(true);
    public static Text.Foundry iiqual = new Text.Foundry(Text.sans, 12,new Color(0, 0, 0)).aa(true);

    public static final RichText.Foundry nifnd = new RichText.Foundry(Resource.remote(), java.awt.font.TextAttribute.FAMILY, "SansSerif", java.awt.font.TextAttribute.SIZE, UI.scale(14)).aa(true);
    public static Text.Furnace hotkey = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 12, Color.WHITE).aa(true), 1, 1, new Color(0,0,0));
    public static final TexI[] removei = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/removeItem/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/removeItem/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/removeItem/h"))};

    public static final BufferedImage[] cbtni = new BufferedImage[] {
            Resource.loadsimg("nurgling/hud/wnd/cbtnu"),
            Resource.loadsimg("nurgling/hud/wnd/cbtnd"),
            Resource.loadsimg("nurgling/hud/wnd/cbtnh")};

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

    public static final TexI[] canceli = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/cancel/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/cancel/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/cancel/h"))};

    public static final TexI[] addarea = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addarea/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addarea/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/addarea/h"))};

    public static final TexI[] add = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/add/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/add/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/add/h"))};

    public static final TexI[] remove = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/remove/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/remove/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/remove/h"))};

    private final static ArrayList<BufferedImage> hlight = new ArrayList<>();
    static {
        for(int i = 0 ; i < 6; i ++)
        {
            hlight.add(Resource.loadsimg("nurgling/hud/buttons/hlight/"+i));
        }
    }

    public static BufferedImage getHLight(NUI ui)
    {
        return hlight.get((int) ((ui.tickId/5)%6));
    }

    public enum CropMarkers
    {
        RED,
        BLUE,
        GRAY,
        YELLOW,
        ORANGE,
        GREEN
    }

    public static final HashMap<CropMarkers,TexI> iCropMap = new HashMap<CropMarkers,TexI>()
    {
        {
            put(CropMarkers.RED,new TexI(Resource.loadsimg("crop/red")));
            put(CropMarkers.ORANGE,new TexI(Resource.loadsimg("crop/orange")));
            put(CropMarkers.YELLOW,new TexI(Resource.loadsimg("crop/yellow")));
            put(CropMarkers.BLUE,new TexI(Resource.loadsimg("crop/blue")));
            put(CropMarkers.GRAY,new TexI(Resource.loadsimg("crop/gray")));
            put(CropMarkers.GREEN,new TexI(Resource.loadsimg("crop/green")));
        }
    };
    static final HashMap<Long,TexI> iCropStageMap3 = new HashMap<Long,TexI>()
    {
        {
            put(1L,new TexI(Resource.loadsimg("crop/yellow_1_3")));
            put(2L,new TexI(Resource.loadsimg("crop/yellow_2_3")));
        }
    };
    static final HashMap<Long,TexI> iCropStageMap4 = new HashMap<Long,TexI>()
    {
        {
            put(1L,new TexI(Resource.loadsimg("crop/yellow_1_4")));
            put(2L,new TexI(Resource.loadsimg("crop/yellow_2_4")));
            put(3L,new TexI(Resource.loadsimg("crop/yellow_3_4")));
        }
    };
    static final HashMap<Long,TexI> iCropStageMap5 = new HashMap<Long,TexI>()
    {
        {
            put(1L,new TexI(Resource.loadsimg("crop/yellow_1_5")));
            put(2L,new TexI(Resource.loadsimg("crop/yellow_2_5")));
            put(3L,new TexI(Resource.loadsimg("crop/yellow_3_5")));
            put(4L,new TexI(Resource.loadsimg("crop/yellow_4_5")));
        }
    };
    static final HashMap<Long,TexI> iCropStageMap6 = new HashMap<Long,TexI>()
    {
        {
            put(1L,new TexI(Resource.loadsimg("crop/yellow_1_6")));
            put(2L,new TexI(Resource.loadsimg("crop/yellow_2_6")));
            put(3L,new TexI(Resource.loadsimg("crop/yellow_3_6")));
            put(4L,new TexI(Resource.loadsimg("crop/yellow_4_6")));
            put(5L,new TexI(Resource.loadsimg("crop/yellow_5_6")));
        }
    };

    public static TexI getCropTexI(long curent, long max)
    {
        switch ((int) max)
        {
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

    public static HashMap<String, Tileset> customTileRes = new HashMap<String, Tileset>(){
        {put("ridge", Resource.local().loadwait("tiles/ridge").layer(Tileset.class));}
    };
    public static Tiler getRidge() {
        if(ridge==null)
            ridge = customTileRes.get("ridge").tfac().create(7001, customTileRes.get("ridge"));
        return ridge;
    }
}
