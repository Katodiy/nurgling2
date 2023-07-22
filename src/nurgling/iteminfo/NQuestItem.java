package nurgling.iteminfo;

/* $use: ui/tt/wellMined */

import haven.*;
import nurgling.NGItem;

/* >tt: WellMined */
public class NQuestItem extends ItemInfo.Tip implements GItem.OverlayInfo<Tex> {
    NGItem higlighted;

    public NQuestItem(Owner owner) {
        super(owner);
        higlighted = (NGItem) owner;
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new NQuestItem(owner));
    }

    public static Tex frame = Resource.loadtex("nurgling/hud/items/overlays/quests/frame");
    public static Tex mark = Resource.loadtex("nurgling/hud/items/overlays/quests/mark");


    @Override
    public Tex overlay() {
        return frame;
    }

    @Override
    public void drawoverlay(GOut g, Tex data)
    {
        if(higlighted.isQuested) {
            g.aimage(frame, Coord.z, 0, 0, g.sz());
            g.aimage(mark, Coord.z, 0, 0);
        }
    }
}
