package nurgling.iteminfo;

import haven.*;
import nurgling.NGItem;
import nurgling.NUtils;

import java.util.ArrayList;

public class NSearchingHighlight  extends ItemInfo.Tip implements GItem.OverlayInfo<Tex> {
        NGItem higlighted;
        public NSearchingHighlight(Owner owner) {
            super(owner);
            higlighted = (NGItem) owner;
        }

        public static ItemInfo mkinfo(Owner owner, Object... args) {
            return(new NSearchingHighlight(owner));
        }

        static ArrayList<Tex> frames = new ArrayList<>();
        static
        {
            for (int i = 0; i < 45; i++)
            {
                frames.add(Resource.loadtex("nurgling/hud/items/overlays/frame/frame" + String.valueOf(i)));
            }
        }


        @Override
        public Tex overlay() {
            return frames.get(0);
        }

        @Override
        public void drawoverlay(GOut g, Tex data)
        {
            if(higlighted.isSearched)
                g.aimage(frames.get(((int) NUtils.getTickId()/2)%45),Coord.z, 0, 0,g.sz());
        }


}
