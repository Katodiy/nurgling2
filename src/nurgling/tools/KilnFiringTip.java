package nurgling.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;

/**
 * Kiln-item tooltip helpers: remaining firing time from {@link KilnFuelCatalog}
 * and a progress bar matching the item meter. Shared with the smelter tip.
 */
public final class KilnFiringTip {
    public static final Color BAR_FILL = new Color(255, 196, 92);
    public static final Color BAR_BG = new Color(20, 24, 22);
    public static final Color BAR_BORDER = new Color(244, 247, 21, 192);

    private KilnFiringTip() {
    }

    public static boolean isKilnWindow(String cap) {
        return "Kiln".equals(cap);
    }

    /** Clip a GItem meter (0-100) to a usable percent. Missing/unlit meter is 0. */
    public static int meterPercent(int meter) {
        if (meter < 0)
            return 0;
        if (meter > 100)
            return 100;
        return meter;
    }

    /**
     * Same source as {@code WItem.draw}: {@code GItem.meter} (0-100) wins when
     * greater than 0; otherwise {@code MeterInfo.meter()} is a 0.0-1.0 fraction.
     */
    public static int resolvedPercent(int gItemMeter, Double meterInfo) {
        if (gItemMeter > 0)
            return meterPercent(gItemMeter);
        if (meterInfo == null)
            return 0;
        return meterPercent((int) Math.round(meterInfo * 100.0));
    }

    public static boolean meterChanged(int renderedPercent, int currentMeter) {
        return renderedPercent != meterPercent(currentMeter);
    }

    /**
     * Item name to render the extra kiln tip for, or null if the window is not a kiln
     * or the name is not in the catalog.
     */
    public static String shouldRender(String windowCap, String itemName) {
        if (!isKilnWindow(windowCap))
            return null;
        if (!KilnFuelCatalog.entryFor(itemName).isPresent())
            return null;
        return itemName;
    }

    public static int fillWidth(int barWidth, double meterPercent) {
        if (barWidth <= 0)
            return 0;
        double pct = Math.max(0, Math.min(100, meterPercent));
        return (int) Math.round(barWidth * pct / 100.0);
    }

    public static String formatRemaining(int seconds, String minutesPattern,
                                         String hoursMinutesPattern, String hoursPattern) {
        int totalMinutes = (int) Math.round(Math.max(0, seconds) / 60.0);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours > 0 && minutes > 0)
            return MessageFormat.format(hoursMinutesPattern, hours, minutes);
        if (hours > 0)
            return MessageFormat.format(hoursPattern, hours);
        return MessageFormat.format(minutesPattern, totalMinutes);
    }

    public static BufferedImage progressBar(int width, int height, double meterPercent,
                                            Color fill, Color bg, Color border) {
        BufferedImage img = new BufferedImage(Math.max(1, width), Math.max(1, height),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(bg);
        g.fillRect(0, 0, width, height);
        int filled = fillWidth(width, meterPercent);
        if (filled > 0) {
            g.setColor(fill);
            g.fillRect(0, 0, Math.min(filled, width), height);
        }
        if (border != null && width > 0 && height > 0) {
            g.setColor(border);
            g.drawRect(0, 0, width - 1, height - 1);
        }
        g.dispose();
        return img;
    }

    public static BufferedImage composeBarAndLabel(BufferedImage bar, BufferedImage label, int gap) {
        if (bar == null)
            return label;
        if (label == null)
            return bar;
        int width = Math.max(bar.getWidth(), label.getWidth());
        int height = bar.getHeight() + Math.max(0, gap) + label.getHeight();
        BufferedImage stacked = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = stacked.createGraphics();
        g.drawImage(bar, 0, 0, null);
        g.drawImage(label, 0, bar.getHeight() + Math.max(0, gap), null);
        g.dispose();
        return stacked;
    }
}
