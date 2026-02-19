package nurgling;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;
import nurgling.iteminfo.NCuriosity;
import nurgling.styles.TooltipStyle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom tooltip builder for all items.
 * Renders name + quality icon + quality number on one line using Open Sans fonts.
 */
public class NTooltip {

    // Cached foundries
    private static Text.Foundry nameFoundry = null;
    private static Text.Foundry resourceFoundry = null;
    private static Text.Foundry contentFoundry = null;

    // Pattern for parsing liquid content names like "3.00 l of Water"
    private static final Pattern CONTENT_PATTERN = Pattern.compile("^([\\d.]+)\\s*(l of .+)$");

    /**
     * Result of rendering a line with mixed text and icons.
     * Tracks text position for proper baseline-relative spacing.
     */
    private static class LineResult {
        final BufferedImage image;
        final int textTopOffset;     // Pixels from image top to text top
        final int textBottomOffset;  // Pixels from text bottom to image bottom

        LineResult(BufferedImage image, int textTopOffset, int textBottomOffset) {
            this.image = image;
            this.textTopOffset = textTopOffset;
            this.textBottomOffset = textBottomOffset;
        }
    }

    private static Text.Foundry getNameFoundry() {
        if (nameFoundry == null) {
            nameFoundry = TooltipStyle.createFoundry(true, TooltipStyle.FONT_SIZE_NAME, Color.WHITE);
        }
        return nameFoundry;
    }

    private static Text.Foundry getResourceFoundry() {
        if (resourceFoundry == null) {
            resourceFoundry = TooltipStyle.createFoundry(false, TooltipStyle.FONT_SIZE_RESOURCE, TooltipStyle.COLOR_RESOURCE_PATH);
        }
        return resourceFoundry;
    }

    private static Text.Foundry getContentFoundry() {
        if (contentFoundry == null) {
            contentFoundry = TooltipStyle.createFoundry(true, TooltipStyle.FONT_SIZE_BODY, Color.WHITE);
        }
        return contentFoundry;
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

        // Find Name, QBuff, NCuriosity, and Contents info
        String nameText = null;
        QBuff qbuff = null;
        NCuriosity curiosity = null;
        ItemInfo.Contents contents = null;

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
            if (ii instanceof ItemInfo.Contents) {
                contents = (ItemInfo.Contents) ii;
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
        if (curiosity != null) {
            remainingTime = curiosity.getCompactRemainingTime();
        }

        // Extract content info (for vessels like waterskins)
        String contentName = null;
        QBuff contentQBuff = null;

        // Extract content info from Contents
        if (contents != null && contents.sub != null && !contents.sub.isEmpty()) {
            for (ItemInfo subInfo : contents.sub) {
                if (subInfo instanceof ItemInfo.Name) {
                    contentName = ((ItemInfo.Name) subInfo).str.text;
                }
                if (subInfo instanceof QBuff) {
                    contentQBuff = (QBuff) subInfo;
                }
            }
        }

        // Render name line with quality and optional remaining time
        LineResult nameLineResult = null;
        BufferedImage nameLine = null;
        int nameTextBottomOffset = 0;
        if (nameText != null) {
            nameLineResult = renderNameLine(nameText, qbuff, remainingTime);
            nameLine = nameLineResult.image;  // Don't crop - we need accurate text position
            nameTextBottomOffset = nameLineResult.textBottomOffset;
        }

        // Render content line if there's content
        BufferedImage contentLine = null;
        int contentTextTopOffset = 0;
        if (contentName != null) {
            LineResult contentLineResult = renderContentLine(contentName, contentQBuff);
            contentLine = contentLineResult.image;  // Don't crop - need accurate text position
            contentTextTopOffset = contentLineResult.textTopOffset;
        }

        // Render other tips (excluding Name, QBuff, and Contents which we've handled)
        BufferedImage otherTips = TooltipStyle.cropTopOnly(renderOtherTips(info, contents != null));

        // Render resource line
        BufferedImage resLine = null;
        if (owner instanceof GItem) {
            String resPath = ((GItem) owner).res.get().name;
            resLine = TooltipStyle.cropTopOnly(getResourceFoundry().render(resPath, TooltipStyle.COLOR_RESOURCE_PATH).img);
        }

        // Calculate baseline-relative spacing (all spacing values are scaled)
        // For 10px from baseline to next text top: gap = 10 - descent - textBottomOffset
        int nameDescentVal = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_NAME);
        int bodyDescentVal = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);
        int scaledSectionSpacing = UI.scale(TooltipStyle.SECTION_SPACING);

        // Combine sections with SECTION_SPACING (10px scaled) between main groups:
        // Group structure: Name | Content | Other Tips | Resource

        // Start from bottom: combine otherTips and resLine
        BufferedImage statsAndRes = null;
        if (otherTips != null && resLine != null) {
            int statsToResSpacing = scaledSectionSpacing - bodyDescentVal;
            statsAndRes = ItemInfo.catimgs(statsToResSpacing, otherTips, resLine);
        } else if (otherTips != null) {
            statsAndRes = otherTips;
        } else if (resLine != null) {
            statsAndRes = resLine;
        }

        // Add content line above statsAndRes
        BufferedImage contentAndBelow = null;
        if (contentLine != null && statsAndRes != null) {
            int contentToStatsSpacing = scaledSectionSpacing - bodyDescentVal;
            contentAndBelow = ItemInfo.catimgs(contentToStatsSpacing, contentLine, statsAndRes);
        } else if (contentLine != null) {
            contentAndBelow = contentLine;
        } else {
            contentAndBelow = statsAndRes;
        }

        // Then combine nameLine with contentAndBelow using 10px section spacing
        // For 10px from baseline to next text top, accounting for centered icons
        // Note: Outer padding is handled by NWItem.PaddedTip
        if (nameLine != null && contentAndBelow != null) {
            // For vessels with content line: account for text position within content canvas
            // The content line has icon + text centered, so we need contentTextTopOffset
            // Also need to account for the text image's internal top padding (~4px)
            // For food/other items without content line: contentTextTopOffset is 0
            int nameToContentSpacing = scaledSectionSpacing - nameDescentVal - nameTextBottomOffset - contentTextTopOffset;
            if (contentLine != null) {
                // Content line text images have internal padding not captured by textTopOffset
                nameToContentSpacing -= UI.scale(4);
            }
            return ItemInfo.catimgs(nameToContentSpacing, nameLine, contentAndBelow);
        } else if (nameLine != null) {
            return nameLine;
        } else if (contentAndBelow != null) {
            return contentAndBelow;
        }

        return null;
    }

    /**
     * Render the name line: Name + Quality Icon + Quality Value + Optional Remaining Time
     * Returns LineResult with text position info for proper spacing.
     */
    private static LineResult renderNameLine(String nameText, QBuff qbuff, String remainingTime) {
        BufferedImage nameImg = getNameFoundry().render(nameText, Color.WHITE).img;
        int hSpacing = UI.scale(TooltipStyle.HORIZONTAL_SPACING);
        int iconToTextSpacing = UI.scale(TooltipStyle.ICON_TO_TEXT_SPACING);

        int totalWidth = nameImg.getWidth();
        int textHeight = nameImg.getHeight();
        int maxHeight = textHeight;

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
            // Show exact quality with 1 decimal place if not a whole number
            String qText = (qbuff.q == Math.floor(qbuff.q))
                ? String.valueOf((int) qbuff.q)
                : String.format("%.1f", qbuff.q);
            qImg = getNameFoundry().render(qText, Color.WHITE).img;
            totalWidth += qImg.getWidth();
            // Text height should be consistent
        }

        // Remaining time for curios
        BufferedImage timeImg = null;
        if (remainingTime != null && !remainingTime.isEmpty()) {
            totalWidth += hSpacing;
            timeImg = getNameFoundry().render("(" + remainingTime + ")", TooltipStyle.COLOR_RESOURCE_PATH).img;
            totalWidth += timeImg.getWidth();
        }

        // Canvas must fit both text and icon, but spacing ignores icon size
        int canvasHeight = Math.max(textHeight, qIcon != null ? qIcon.getHeight() : 0);

        // Center text and icon in canvas - their centers will align
        int textY = (canvasHeight - textHeight) / 2;

        // Compose the line
        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, canvasHeight));
        Graphics g = result.getGraphics();
        int x = 0;

        // Draw name (centered vertically)
        g.drawImage(nameImg, x, textY, null);
        x += nameImg.getWidth();

        // Draw quality icon and value
        if (qbuff != null && qbuff.q > 0) {
            x += hSpacing;
            if (qIcon != null) {
                // Icon centered vertically (same center as text)
                int iconY = (canvasHeight - qIcon.getHeight()) / 2;
                g.drawImage(qIcon, x, iconY, null);
                x += qIcon.getWidth() + iconToTextSpacing;
            }
            if (qImg != null) {
                g.drawImage(qImg, x, textY, null);
                x += qImg.getWidth();
            }
        }

        // Draw remaining time
        if (timeImg != null) {
            x += hSpacing;
            g.drawImage(timeImg, x, textY, null);
        }

        // Track text position for spacing calculations
        int textTopOffset = textY;
        int textBottomOffset = canvasHeight - textY - textHeight;

        g.dispose();
        return new LineResult(result, textTopOffset, textBottomOffset);
    }

    /**
     * Render the content line for vessels: "3.00 l of Water [icon] 81"
     * Amount is colored cyan, rest is white.
     * Returns LineResult with text position info for proper spacing.
     */
    private static LineResult renderContentLine(String contentName, QBuff contentQBuff) {
        int hSpacing = UI.scale(TooltipStyle.HORIZONTAL_SPACING);
        int iconToTextSpacing = UI.scale(TooltipStyle.ICON_TO_TEXT_SPACING);

        // Parse the content name to separate amount from the rest
        // Format: "3.00 l of Water" -> amount="3.00", rest="l of Water"
        String amount = null;
        String rest = null;
        Matcher matcher = CONTENT_PATTERN.matcher(contentName);
        if (matcher.matches()) {
            amount = matcher.group(1);
            rest = matcher.group(2);
        } else {
            // Fallback: just use the whole name as white text
            rest = contentName;
        }

        // Build the content line - track text height separately from icon height
        int totalWidth = 0;
        int textHeight = 0;
        int maxHeight = 0;

        // Amount (cyan colored)
        BufferedImage amountImg = null;
        if (amount != null) {
            amountImg = getContentFoundry().render(amount + " ", TooltipStyle.COLOR_FOOD_ENERGY).img;
            totalWidth += amountImg.getWidth();
            textHeight = Math.max(textHeight, amountImg.getHeight());
            maxHeight = Math.max(maxHeight, amountImg.getHeight());
        }

        // Rest of the name (white)
        BufferedImage restImg = getContentFoundry().render(rest, Color.WHITE).img;
        totalWidth += restImg.getWidth();
        textHeight = Math.max(textHeight, restImg.getHeight());
        maxHeight = Math.max(maxHeight, restImg.getHeight());

        // Quality icon and value
        BufferedImage qIcon = null;
        BufferedImage qImg = null;
        if (contentQBuff != null && contentQBuff.q > 0) {
            totalWidth += hSpacing;
            qIcon = contentQBuff.icon;
            if (qIcon != null) {
                totalWidth += qIcon.getWidth() + iconToTextSpacing;
                maxHeight = Math.max(maxHeight, qIcon.getHeight());
            }
            // Show exact quality with 1 decimal place if not a whole number
            String qText = (contentQBuff.q == Math.floor(contentQBuff.q))
                ? String.valueOf((int) contentQBuff.q)
                : String.format("%.1f", contentQBuff.q);
            qImg = getContentFoundry().render(qText, Color.WHITE).img;
            totalWidth += qImg.getWidth();
            // qImg is text, add to textHeight
            textHeight = Math.max(textHeight, qImg.getHeight());
            maxHeight = Math.max(maxHeight, qImg.getHeight());
        }

        // Canvas must fit both text and icon, but spacing ignores icon size
        int canvasHeight = Math.max(textHeight, qIcon != null ? qIcon.getHeight() : 0);

        // Center text and icon in canvas - their centers will align
        int textY = (canvasHeight - textHeight) / 2;

        // Compose the line
        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, canvasHeight));
        Graphics g = result.getGraphics();
        int x = 0;

        // Draw amount (cyan) - centered vertically
        if (amountImg != null) {
            g.drawImage(amountImg, x, textY, null);
            x += amountImg.getWidth();
        }

        // Draw rest of name (white) - centered vertically
        g.drawImage(restImg, x, textY, null);
        x += restImg.getWidth();

        // Draw quality icon and value
        if (contentQBuff != null && contentQBuff.q > 0) {
            x += hSpacing;
            if (qIcon != null) {
                // Icon centered vertically (same center as text)
                int iconY = (canvasHeight - qIcon.getHeight()) / 2;
                g.drawImage(qIcon, x, iconY, null);
                x += qIcon.getWidth() + iconToTextSpacing;
            }
            if (qImg != null) {
                g.drawImage(qImg, x, textY, null);
            }
        }

        // Track text position for spacing calculations
        int textTopOffset = textY;
        int textBottomOffset = canvasHeight - textY - textHeight;

        g.dispose();
        return new LineResult(result, textTopOffset, textBottomOffset);
    }

    /**
     * Render other tips (excluding Name, QBuff.Table, and optionally Contents)
     */
    private static BufferedImage renderOtherTips(List<ItemInfo> info, boolean skipContents) {
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
                // Skip FoodTypes - NFoodInfo renders food types with icons
                if (tip.getClass().getName().contains("FoodTypes")) {
                    continue;
                }
                // Skip Contents - we render it ourselves with custom format
                if (skipContents && tip instanceof ItemInfo.Contents) {
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
