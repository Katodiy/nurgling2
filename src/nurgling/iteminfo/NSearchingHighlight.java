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


        private int lastFrameIndex = -1;

        @Override
        public Tex overlay() {
            int frameIndex = ((int) NUtils.getTickId()/2) % 45;
            lastFrameIndex = frameIndex;
            return frames.get(frameIndex);
        }

        @Override
        public void drawoverlay(GOut g, Tex data)
        {
            if(higlighted.isSearched)
                g.aimage(data, Coord.z, 0, 0, g.sz());
        }

        @Override
        public boolean tick(double dt)
        {
            // Calculate current frame index
            int currentFrameIndex = ((int) NUtils.getTickId()/2) % 45;
            // Only update if frame changed
            if (currentFrameIndex != lastFrameIndex) {
                return false; // Need to update overlay
            }
            return true; // No update needed
        }
}
