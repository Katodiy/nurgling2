package nurgling;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;
import nurgling.conf.FontSettings;
import nurgling.iteminfo.NCuriosity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom tooltip builder for all items.
 * Renders name + quality icon + quality number on one line using Open Sans fonts.
 */
public class NTooltip {
    // Font sizes
    private static final int NAME_FONT_SIZE = 12;
    private static final int RESOURCE_FONT_SIZE = 9;

    // Spacing constants (matching Figma design, in logical pixels - will be scaled)
    // Note: Outer padding (10px) is handled by NWItem.PaddedTip
    private static final int SECTION_SPACING = 10;      // Between Name, LP Info group, Resource
    private static final int INTERNAL_SPACING = 7;      // Within LP Info group (LP, Study time, Mental weight)
    private static final int HORIZONTAL_SPACING = 7;    // Between elements on the name line
    private static final int ICON_TO_TEXT_SPACING = 3;  // Between quality icon and quality number

    // Cached foundries
    private static Text.Foundry nameFoundry = null;
    private static Text.Foundry qualityFoundry = null;
    private static Text.Foundry resourceFoundry = null;

    private static Font getOpenSansRegular() {
        FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
        return fontSettings != null ? fontSettings.getFont("Open Sans") : null;
    }

    private static Font getOpenSansSemibold() {
        FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
        return fontSettings != null ? fontSettings.getFont("Open Sans Semibold") : null;
    }

    private static Text.Foundry getNameFoundry() {
        if (nameFoundry == null) {
            Font font = getOpenSansSemibold();
            int size = UI.scale(NAME_FONT_SIZE);
            if (font == null) {
                font = new Font("SansSerif", Font.BOLD, size);
            } else {
                font = font.deriveFont(Font.PLAIN, (float) size);
            }
            nameFoundry = new Text.Foundry(font, Color.WHITE).aa(true);
        }
        return nameFoundry;
    }

    private static Text.Foundry getQualityFoundry() {
        if (qualityFoundry == null) {
            Font font = getOpenSansSemibold();
            int size = UI.scale(NAME_FONT_SIZE);
            if (font == null) {
                font = new Font("SansSerif", Font.BOLD, size);
            } else {
                font = font.deriveFont(Font.PLAIN, (float) size);
            }
            qualityFoundry = new Text.Foundry(font, Color.WHITE).aa(true);
        }
        return qualityFoundry;
    }

    private static Text.Foundry getResourceFoundry() {
        if (resourceFoundry == null) {
            Font font = getOpenSansRegular();
            int size = UI.scale(RESOURCE_FONT_SIZE);
            if (font == null) {
                font = new Font("SansSerif", Font.PLAIN, size);
            } else {
                font = font.deriveFont(Font.PLAIN, (float) size);
            }
            resourceFoundry = new Text.Foundry(font, new Color(128, 128, 128)).aa(true);
        }
        return resourceFoundry;
    }

    /**
     * Build a custom tooltip for an item.
     * Renders name + quality on one line, then other info, then resource path.
     */
    public static BufferedImage build(List<ItemInfo> info) {
        if (info == null || info.isEmpty()) {
            return null;
        }

        ItemInfo.Owner owner = info.get(0).owner;
        List<BufferedImage> lines = new ArrayList<>();

        // Find Name, QBuff, and NCuriosity info
        String nameText = null;
        QBuff qbuff = null;
        NCuriosity curiosity = null;

        for (ItemInfo ii : info) {
            if (ii instanceof ItemInfo.Name) {
                nameText = ((ItemInfo.Name) ii).str.text;
            }
            if (ii instanceof QBuff) {
                qbuff = (QBuff) ii;
            }
            if (ii instanceof NCuriosity) {
                curiosity = (NCuriosity) ii;
            }
        }

        // If no name found, try default
        if (nameText == null) {
            try {
                nameText = ItemInfo.Name.Default.get(owner);
            } catch (Exception ignored) {}
        }

        // Get remaining time for curios
        String remainingTime = null;
        if (curiosity != null && NCuriosity.isCompactMode()) {
            remainingTime = curiosity.getCompactRemainingTime();
        }

        // Render name line with quality and optional remaining time
        BufferedImage nameLine = null;
        if (nameText != null) {
            nameLine = cropTopOnly(renderNameLine(nameText, qbuff, remainingTime));
        }

        // Render other tips (excluding Name and QBuff which we've handled)
        BufferedImage otherTips = cropTopOnly(renderOtherTips(info));

        // Render resource line
        BufferedImage resLine = null;
        if (owner instanceof GItem) {
            String resPath = ((GItem) owner).res.get().name;
            resLine = cropTopOnly(getResourceFoundry().render(resPath, new Color(128, 128, 128)).img);
        }

        // Calculate baseline-relative spacing (all spacing values are scaled)
        // Spacing = desired_baseline_to_top - descent_of_line_above
        int nameDescentVal = getFontDescent(NAME_FONT_SIZE);  // 12px font for name line
        int bodyDescentVal = getFontDescent(11);              // 11px font for curio stats (from NCuriosity)
        int scaledSectionSpacing = UI.scale(SECTION_SPACING);

        // Combine sections with SECTION_SPACING (10px scaled) between main groups:
        // Group structure: Name | LP Info group | Resource
        // LP Info group internal spacing (7px) is handled by NCuriosity

        // First combine otherTips (LP Info group) and resLine with 10px section spacing
        BufferedImage statsAndRes = null;
        if (otherTips != null && resLine != null) {
            int statsToResSpacing = scaledSectionSpacing - bodyDescentVal;
            statsAndRes = ItemInfo.catimgs(statsToResSpacing, otherTips, resLine);
        } else if (otherTips != null) {
            statsAndRes = otherTips;
        } else if (resLine != null) {
            statsAndRes = resLine;
        }

        // Then combine nameLine with statsAndRes using 10px section spacing
        // Note: Outer padding is handled by NWItem.PaddedTip
        if (nameLine != null && statsAndRes != null) {
            int nameToStatsSpacing = scaledSectionSpacing - nameDescentVal;
            return ItemInfo.catimgs(nameToStatsSpacing, nameLine, statsAndRes);
        } else if (nameLine != null) {
            return nameLine;
        } else if (statsAndRes != null) {
            return statsAndRes;
        }

        return null;
    }

    /**
     * Crop top of image to first visible pixel, but keep bottom at original position.
     * This ensures baseline-relative spacing.
     */
    private static BufferedImage cropTopOnly(BufferedImage img) {
        if (img == null) {
            return null;
        }
        int width = img.getWidth();
        int height = img.getHeight();
        int alphaThreshold = 128; // Ignore anti-aliased pixels

        // Find top-most row with visible pixels
        int top = 0;
        topSearch:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > alphaThreshold) {
                    top = y;
                    break topSearch;
                }
            }
        }

        // If no top cropping needed, return original
        if (top == 0) {
            return img;
        }

        // Crop only from the top, keep the bottom at original position
        int newHeight = height - top;
        BufferedImage cropped = new BufferedImage(width, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = cropped.getGraphics();
        g.drawImage(img, 0, 0, width, newHeight, 0, top, width, height, null);
        g.dispose();

        return cropped;
    }

    /** Get font descent for a given font size (used for baseline-relative spacing) */
    private static int getFontDescent(int fontSize) {
        Font font = getOpenSansRegular();
        int size = UI.scale(fontSize);
        if (font == null) {
            font = new Font("SansSerif", Font.PLAIN, size);
        } else {
            font = font.deriveFont(Font.PLAIN, (float) size);
        }
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        java.awt.FontMetrics fm = tmp.getGraphics().getFontMetrics(font);
        return fm.getDescent();
    }

    /**
     * Render the name line: Name + Quality Icon + Quality Value + Optional Remaining Time
     */
    private static BufferedImage renderNameLine(String nameText, QBuff qbuff, String remainingTime) {
        BufferedImage nameImg = getNameFoundry().render(nameText, Color.WHITE).img;
        int hSpacing = UI.scale(HORIZONTAL_SPACING);
        int iconToTextSpacing = UI.scale(ICON_TO_TEXT_SPACING);

        int totalWidth = nameImg.getWidth();
        int maxHeight = nameImg.getHeight();

        // Quality icon and value
        BufferedImage qIcon = null;
        BufferedImage qImg = null;
        if (qbuff != null && qbuff.q > 0) {
            totalWidth += hSpacing;
            qIcon = qbuff.icon;
            if (qIcon != null) {
                totalWidth += qIcon.getWidth() + iconToTextSpacing;
                maxHeight = Math.max(maxHeight, qIcon.getHeight());
            }
            qImg = getQualityFoundry().render(String.valueOf((int) qbuff.q), Color.WHITE).img;
            totalWidth += qImg.getWidth();
            maxHeight = Math.max(maxHeight, qImg.getHeight());
        }

        // Remaining time for curios
        BufferedImage timeImg = null;
        if (remainingTime != null && !remainingTime.isEmpty()) {
            totalWidth += hSpacing;
            timeImg = getNameFoundry().render("(" + remainingTime + ")", new Color(128, 128, 128)).img;
            totalWidth += timeImg.getWidth();
            maxHeight = Math.max(maxHeight, timeImg.getHeight());
        }

        // Compose the line
        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;

        // Draw name
        g.drawImage(nameImg, x, (maxHeight - nameImg.getHeight()) / 2, null);
        x += nameImg.getWidth();

        // Draw quality icon and value
        if (qbuff != null && qbuff.q > 0) {
            x += hSpacing;
            if (qIcon != null) {
                g.drawImage(qIcon, x, (maxHeight - qIcon.getHeight()) / 2, null);
                x += qIcon.getWidth() + iconToTextSpacing;
            }
            if (qImg != null) {
                g.drawImage(qImg, x, (maxHeight - qImg.getHeight()) / 2, null);
                x += qImg.getWidth();
            }
        }

        // Draw remaining time
        if (timeImg != null) {
            x += hSpacing;
            g.drawImage(timeImg, x, (maxHeight - timeImg.getHeight()) / 2, null);
        }

        g.dispose();
        return result;
    }

    /**
     * Render other tips (excluding Name and QBuff.Table)
     */
    private static BufferedImage renderOtherTips(List<ItemInfo> info) {
        if (info.isEmpty()) {
            return null;
        }

        // Create a layout and add tips, excluding Name and QBuff.Table
        ItemInfo.Layout l = new ItemInfo.Layout(info.get(0).owner);
        boolean hasTips = false;

        for (ItemInfo ii : info) {
            if (ii instanceof ItemInfo.Tip) {
                ItemInfo.Tip tip = (ItemInfo.Tip) ii;
                // Skip Name - we render it ourselves
                if (tip instanceof ItemInfo.Name) {
                    continue;
                }
                // Skip QBuff and QBuff.Table - we render quality in name line
                if (tip instanceof QBuff || tip.getClass().getName().contains("QBuff")) {
                    continue;
                }
                l.add(tip);
                hasTips = true;
            }
        }

        if (!hasTips) {
            return null;
        }

        try {
            BufferedImage rendered = l.render();
            // Check if the rendered image has valid dimensions
            if (rendered == null || rendered.getWidth() <= 0 || rendered.getHeight() <= 0) {
                return null;
            }
            return rendered;
        } catch (Exception e) {
            // Layout had no visible content
            return null;
        }
    }
}
