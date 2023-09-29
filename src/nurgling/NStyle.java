package nurgling;

import haven.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class NStyle
{
    public static Text.Foundry fcomboitems = new Text.Foundry(Text.sans, 16).aa(true);
    public static Text.Furnace meter = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 12, Color.WHITE).aa(true), 2, 1, new Color(60,30,30));
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
}
