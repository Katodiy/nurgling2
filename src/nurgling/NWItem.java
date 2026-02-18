package nurgling;

import haven.*;
import haven.res.lib.itemtex.*;
import nurgling.iteminfo.NSearchable;
import nurgling.styles.TooltipStyle;
import nurgling.tools.NSearchItem;
import org.json.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class NWItem extends WItem
{
    public NWItem(GItem item)
    {
        super(item);
    }

    /**
     * Calculate actual padding needed.
     * GLPanel.drawtooltip adds GLPANEL_MARGIN background margin around the image,
     * so we subtract that to achieve the target total padding.
     * Both values are scaled to maintain proper proportions at any UI scale.
     */
    private static int getTooltipPadding() {
        return Math.max(0, UI.scale(TooltipStyle.OUTER_PADDING) - UI.scale(TooltipStyle.GLPANEL_MARGIN));
    }

    private static int getTooltipPaddingBottom() {
        return Math.max(0, UI.scale(TooltipStyle.OUTER_PADDING_BOTTOM) - UI.scale(TooltipStyle.GLPANEL_MARGIN));
    }

    /**
     * Custom tooltip class that wraps the image with padding
     */
    public class PaddedTip implements Indir<Tex>, ItemInfo.InfoTip {
        private final List<ItemInfo> info;
        private final TexI tex;

        public PaddedTip(List<ItemInfo> info, BufferedImage img) {
            this.info = info;
            if (img == null)
                throw new Loading();
            // Add padding around the tooltip
            BufferedImage padded = addPadding(img);
            tex = new TexI(padded);
        }

        public GItem item() { return item; }
        public List<ItemInfo> info() { return info; }
        public Tex get() { return tex; }

        private BufferedImage addPadding(BufferedImage img) {
            int padding = getTooltipPadding();
            int paddingBottom = getTooltipPaddingBottom();
            int newWidth = img.getWidth() + padding * 2;
            int newHeight = img.getHeight() + padding + paddingBottom;
            BufferedImage result = TexI.mkbuf(new Coord(newWidth, newHeight));
            Graphics g = result.getGraphics();
            g.drawImage(img, padding, padding, null);
            g.dispose();
            return result;
        }
    }

    private PaddedTip nlongtip = null;
    private List<ItemInfo> nttinfo = null;
    private boolean nlastModshift = false;

    @Override
    public Object tooltip(Coord c, Widget prev) {
        List<ItemInfo> info = item.info();
        if (info.size() < 1)
            return null;
        // Reset tooltip cache if Shift state changed
        if (ui.modshift != nlastModshift) {
            nlongtip = null;
            nlastModshift = ui.modshift;
        }
        if (info != nttinfo) {
            nlongtip = null;
            nttinfo = info;
        }
        if (nlongtip == null || ((NGItem) item).needlongtip()) {
            BufferedImage img = NTooltip.build(info);
            if (img != null) {
                nlongtip = new PaddedTip(info, img);
            }
        }
        return nlongtip;
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
}
