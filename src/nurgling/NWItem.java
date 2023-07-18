package nurgling;

import haven.GItem;
import haven.ItemInfo;
import haven.Tex;
import haven.WItem;
import haven.res.ui.tt.q.quality.Quality;

public class NWItem extends WItem
{
    boolean withContent = false;
    public NWItem(GItem item)
    {
        super(item);
    }

    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        GItem.InfoOverlay<Tex>[] ols = (GItem.InfoOverlay<Tex>[]) getItemols().get();
        if(ols != null) {
            for(GItem.InfoOverlay<Tex> ol : ols)
                if (!ol.inf.tick(dt))
                    ol.data = ol.inf.overlay();
        }
    }
}
