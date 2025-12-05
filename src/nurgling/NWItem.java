package nurgling;

import haven.*;
import haven.res.lib.itemtex.*;
import haven.res.ui.tt.drying.Drying;
import nurgling.iteminfo.NSearchable;
import nurgling.tools.NSearchItem;
import org.json.*;

import java.util.List;
import java.util.Optional;

public class NWItem extends WItem
{
    public NWItem(GItem item)
    {
        super(item);
    }

    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        // Update overlays only when their tick() method indicates a change is needed
        GItem.InfoOverlay<Tex>[] ols = (GItem.InfoOverlay<Tex>[]) getItemols().get();
        if(ols != null) {
            for(GItem.InfoOverlay<Tex> ol : ols) {
                // tick() returns false when overlay needs to be updated
                // Only recreate texture when actually needed
                if (!ol.inf.tick(dt)) {
                    ol.data = ol.inf.overlay();
                }
            }
        }
        
        search();
        
        if((Boolean)NConfig.get(NConfig.Key.autoDropper) && ((NGItem) item).isSearched) {
            if(parent instanceof NInventory && NUtils.getGameUI() !=null && NUtils.getGameUI().maininv == parent) {
                NUtils.drop(this);
            }
        }
    }

    private void search()
    {
        if(NUtils.getGameUI()!=null) {
            if (NUtils.getGameUI().itemsForSearch != null && !NUtils.getGameUI().itemsForSearch.isEmpty()) {
                String name = ((NGItem) item).name();
                if (name != null) {
                    if (NUtils.getGameUI().itemsForSearch.onlyName()) {
                        if (name.toLowerCase().contains(NUtils.getGameUI().itemsForSearch.name)) {
                            if (!((NGItem) item).isSearched) {
                                ((NGItem) item).isSearched = true;
                            }
                            return;
                        }
                    }
                }
                if (item.spr != null) {
                    if (item.info != null) {
                        for (ItemInfo inf : item.info) {
                            if (inf instanceof NSearchable) {
                                if (((NSearchable) inf).search()) {
                                    if (!((NGItem) item).isSearched) {
                                        if (!NUtils.getGameUI().itemsForSearch.q.isEmpty() && !searchQuality()) return;
                                        ((NGItem) item).isSearched = true;
                                    }
                                    return;
                                }
                            }
                        }
                        if (!NUtils.getGameUI().itemsForSearch.q.isEmpty() && searchQuality()) {
                            if (!((NGItem) item).isSearched) {
                                ((NGItem) item).isSearched = true;
                            }
                        }
                    }
                }
            }

            if (((NGItem) item).isSearched) {
                if (NUtils.getGameUI().itemsForSearch != null && !NUtils.getGameUI().itemsForSearch.q.isEmpty() && searchQuality())
                    return;
                ((NGItem) item).isSearched = false;
            }
        }
    }

    private boolean searchQuality() {
        for(NSearchItem.Quality q : NUtils.getGameUI().itemsForSearch.q)
        {
            if(((NGItem) item).quality!=null)
            {
                switch (q.type)
                {
                    case MORE:
                        if (((NGItem) item).quality <= q.val) return false;
                        break;
                    case LOW:
                        if (((NGItem) item).quality >= q.val) return false;
                        break;
                    case EQ:
                        if (((NGItem) item).quality != q.val) return false;
                }
            }
            else
            {
                return false;
            }
        }

        if (!NUtils.getGameUI().itemsForSearch.name.isEmpty()) {
            String name = ((NGItem) item).name();
            if(name!=null) {
                return name.toLowerCase().contains(NUtils.getGameUI().itemsForSearch.name);
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean mousedown(MouseDownEvent ev)
    {
        // Alt+Shift+Click: transfer all same items sorted by quality
        // Right-click (button 3): ascending order (lowest quality first)
        // Left-click (button 1): descending order (highest quality first)
        if(ui.modshift)
        {
            if (ui.modmeta)
            {
                if (parent instanceof NInventory)
                {
                    wdgmsg("transfer-same", item, ev.b == 3);
                    return true;
                }
            }
        }
        // Alt+Ctrl+Click: drop all same items sorted by quality
        // Right-click (button 3): ascending order (lowest quality first)
        // Left-click (button 1): descending order (highest quality first)
        else if(ui.modctrl)
        {
            if (ui.modmeta)
            {
                if (parent instanceof NInventory)
                {
                    wdgmsg("drop-same", item, ev.b == 3);
                    return true;
                }
            }
        }
        return super.mousedown(ev);
    }
    public Optional<Double> getDryingProgress() {
        try {
            List<ItemInfo> infos = item.info();
            for (ItemInfo info : infos) {
                if (info instanceof Drying) {
                    return Optional.of(((Drying) info).done);
                }
            }
        } catch (Loading e) {
            // Item info not fully loaded yet
        }
        return Optional.empty();
    }

}
