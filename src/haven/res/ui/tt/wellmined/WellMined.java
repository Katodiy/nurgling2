package haven.res.ui.tt.wellmined;

/* $use: ui/tt/wellMined */

import haven.*;

/* >tt: WellMined */
public class WellMined extends ItemInfo.Tip implements GItem.OverlayInfo<Tex> {
    public WellMined(Owner owner) {
        super(owner);
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new WellMined(owner));
    }

    public static Tex wm_img = Resource.loadtex("nurgling/hud/items/overlays/wellmined");

    @Override
    public Tex overlay() {
        return wm_img;
    }

    @Override
    public void drawoverlay(GOut g, Tex data)
    {
        if(data!=null) {
            g.aimage(data, data.sz(), 1, 1);
        }
    }
}
