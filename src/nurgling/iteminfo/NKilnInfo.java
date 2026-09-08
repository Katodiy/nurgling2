package nurgling.iteminfo;

import haven.GItem;
import haven.ItemInfo;
import haven.Text;
import haven.UI;
import haven.WItem;
import haven.Window;
import nurgling.NGItem;
import nurgling.i18n.L10n;
import nurgling.styles.TooltipStyle;
import nurgling.tools.KilnFiringTip;
import nurgling.tools.KilnFuelCatalog;

import java.awt.image.BufferedImage;
import java.util.OptionalInt;

/**
 * Kiln inventory tooltip: meter progress bar and remaining firing time from
 * {@link KilnFuelCatalog}. Hidden outside kiln windows and for unknown items.
 */
public class NKilnInfo extends ItemInfo.Tip {
    private static Text.Foundry timeFoundry = null;
    private int lastMeterPercent = Integer.MIN_VALUE;

    public NKilnInfo(Owner owner) {
        super(owner);
    }

    public boolean needUpdate() {
        if (!(owner instanceof NGItem))
            return false;
        NGItem item = (NGItem) owner;
        String name = itemName(item);
        if (KilnFiringTip.shouldRender(windowCap(item), name) == null)
            return false;
        return KilnFiringTip.meterChanged(lastMeterPercent, resolvedPercent(item));
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
        String name = itemName(item);
        if (KilnFiringTip.shouldRender(windowCap(item), name) == null)
            return null;
        int percent = resolvedPercent(item);
        OptionalInt remaining = KilnFuelCatalog.remainingSeconds(name, percent);
        if (!remaining.isPresent())
            return null;
        lastMeterPercent = percent;
        return render(percent, remaining.getAsInt());
    }

    private static int resolvedPercent(NGItem item) {
        return KilnFiringTip.resolvedPercent(item.meter, meterInfoFraction(item));
    }

    static Double meterInfoFraction(NGItem item) {
        GItem.MeterInfo minf = ItemInfo.find(GItem.MeterInfo.class, item.info());
        return minf != null ? minf.meter() : null;
    }

    static String itemName(NGItem item) {
        String name = item.name();
        if (name != null)
            return name;
        ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, item.info());
        return nm != null ? nm.str.text : null;
    }

    /** Meter bar plus the remaining-time label, shared with {@link NSmelterInfo}. */
    static BufferedImage render(int meterPercent, int remainingSeconds) {
        int barWidth = UI.scale(120);
        int barHeight = UI.scale(8);
        BufferedImage bar = KilnFiringTip.progressBar(barWidth, barHeight, meterPercent,
                KilnFiringTip.BAR_FILL, KilnFiringTip.BAR_BG, KilnFiringTip.BAR_BORDER);
        String time = KilnFiringTip.formatRemaining(remainingSeconds,
                L10n.get("kiln.item.time.minutes"),
                L10n.get("kiln.item.time.hours_minutes"),
                L10n.get("kiln.item.time.hours"));
        BufferedImage label = TooltipStyle.cropTopOnly(timeFoundry().render(time, TooltipStyle.COLOR_STUDY_TIME).img);
        return KilnFiringTip.composeBarAndLabel(bar, label, UI.scale(TooltipStyle.INTERNAL_SPACING));
    }

    private static Text.Foundry timeFoundry() {
        if (timeFoundry == null) {
            timeFoundry = TooltipStyle.createFoundry(false, TooltipStyle.FONT_SIZE_BODY, TooltipStyle.COLOR_STUDY_TIME);
        }
        return timeFoundry;
    }

    static String windowCap(GItem item) {
        WItem wi = item.wi;
        if (wi == null)
            return null;
        Window wnd = wi.getparent(Window.class);
        return wnd != null ? wnd.cap : null;
    }
}
