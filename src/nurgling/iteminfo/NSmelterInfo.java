package nurgling.iteminfo;

import haven.ItemInfo;
import haven.res.ui.tt.wellmined.WellMined;
import nurgling.NGItem;
import nurgling.tools.KilnFiringTip;
import nurgling.tools.SmelterOreTip;

import java.awt.image.BufferedImage;

/**
 * Ore / Smith's Smelter inventory tooltip: meter progress bar and remaining smelt
 * time. Hidden outside those windows and for fuel / unknown items without a meter.
 */
public class NSmelterInfo extends ItemInfo.Tip {
    private int lastMeterPercent = Integer.MIN_VALUE;

    public NSmelterInfo(Owner owner) {
        super(owner);
    }

    public boolean needUpdate() {
        if (!(owner instanceof NGItem))
            return false;
        NGItem item = (NGItem) owner;
        int percent = resolvedPercent(item);
        if (!SmelterOreTip.shouldRender(NKilnInfo.windowCap(item), NKilnInfo.itemName(item),
                isWellMined(item), percent > 0))
            return false;
        return KilnFiringTip.meterChanged(lastMeterPercent, percent);
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public BufferedImage tipimg() {
        if (!(owner instanceof NGItem))
            return null;
        NGItem item = (NGItem) owner;
        boolean wellMined = isWellMined(item);
        int percent = resolvedPercent(item);
        if (!SmelterOreTip.shouldRender(NKilnInfo.windowCap(item), NKilnInfo.itemName(item),
                wellMined, percent > 0))
            return null;
        lastMeterPercent = percent;
        return NKilnInfo.render(percent, SmelterOreTip.remainingSeconds(percent, wellMined));
    }

    private static boolean isWellMined(NGItem item) {
        return ItemInfo.find(WellMined.class, item.info()) != null;
    }

    private static int resolvedPercent(NGItem item) {
        return KilnFiringTip.resolvedPercent(item.meter, NKilnInfo.meterInfoFraction(item));
    }
}
