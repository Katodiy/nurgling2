package nurgling;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.res.ui.tt.wear.Wear;
import haven.res.ui.tt.gast.Gast;
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

        // Find Name, QBuff, NCuriosity, Contents, Wear, and Gast info
        String nameText = null;
        QBuff qbuff = null;
        NCuriosity curiosity = null;
        ItemInfo.Contents contents = null;
        Wear wear = null;
        Gast gast = null;

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
            if (ii instanceof Wear) {
                wear = (Wear) ii;
            }
            if (ii instanceof Gast) {
                gast = (Gast) ii;
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

        // Calculate wear percentage (only if item has wear)
        Integer wearPercent = null;
        if (wear != null && wear.m > 0) {
            wearPercent = (int) Math.round(((double)(wear.m - wear.d) / wear.m) * 100);
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

        // Render name line with quality, optional wear percentage, and optional remaining time
        LineResult nameLineResult = null;
        BufferedImage nameLine = null;
        int nameTextBottomOffset = 0;
        if (nameText != null) {
            nameLineResult = renderNameLine(nameText, qbuff, wearPercent, remainingTime);
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

        // Render custom lines for Wear, Hunger reduction, Food event bonus
        BufferedImage wearLine = null;
        if (wear != null && wear.m > 0) {
            wearLine = TooltipStyle.cropTopOnly(renderWearLine(wear));
        }

        BufferedImage hungerLine = null;
        if (gast != null && gast.glut != 0.0) {
            hungerLine = TooltipStyle.cropTopOnly(renderHungerLine(gast.glut));
        }

        BufferedImage foodBonusLine = null;
        if (gast != null && gast.fev != 0.0) {
            foodBonusLine = TooltipStyle.cropTopOnly(renderFoodBonusLine(gast.fev));
        }

        // Render other tips (excluding Name, QBuff, Contents, Wear, Gast which we've handled)
        BufferedImage otherTips = TooltipStyle.cropTopOnly(renderOtherTips(info, contents != null));

        // Render resource line
        BufferedImage resLine = null;
        if (owner instanceof GItem) {
            String resPath = ((GItem) owner).res.get().name;
            resLine = TooltipStyle.cropTopOnly(getResourceFoundry().render(resPath, TooltipStyle.COLOR_RESOURCE_PATH).img);
        }

        // Calculate baseline-relative spacing (all spacing values are scaled)
        int nameDescentVal = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_NAME);
        int bodyDescentVal = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);
        int scaledSectionSpacing = UI.scale(TooltipStyle.SECTION_SPACING);
        int scaledInternalSpacing = UI.scale(TooltipStyle.INTERNAL_SPACING);  // 7px

        // Combine sections with proper spacing:
        // Group structure: Name | Content | Wear | Hunger | Food Bonus | Other Tips | Resource
        // 10px between Name and Content
        // 7px between Content, Wear, Hunger, Food Bonus (internal spacing)
        // 10px between Food Bonus and Other Tips, and Other Tips and Resource

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

        // Build vessel info section (content, wear, hunger, food bonus) with 7px internal spacing
        java.util.List<BufferedImage> vesselInfoLines = new java.util.ArrayList<>();
        if (contentLine != null) {
            vesselInfoLines.add(contentLine);
        }
        if (wearLine != null) {
            vesselInfoLines.add(wearLine);
        }
        if (hungerLine != null) {
            vesselInfoLines.add(hungerLine);
        }
        if (foodBonusLine != null) {
            vesselInfoLines.add(foodBonusLine);
        }

        // Combine vessel info lines with 7px baseline-to-text-top spacing
        BufferedImage vesselInfo = null;
        if (!vesselInfoLines.isEmpty()) {
            vesselInfo = vesselInfoLines.get(0);
            for (int i = 1; i < vesselInfoLines.size(); i++) {
                int spacing = scaledInternalSpacing - bodyDescentVal;
                vesselInfo = ItemInfo.catimgs(spacing, vesselInfo, vesselInfoLines.get(i));
            }
        }

        // Combine vesselInfo with statsAndRes (10px spacing)
        BufferedImage contentAndBelow = null;
        if (vesselInfo != null && statsAndRes != null) {
            int vesselToStatsSpacing = scaledSectionSpacing - bodyDescentVal;
            contentAndBelow = ItemInfo.catimgs(vesselToStatsSpacing, vesselInfo, statsAndRes);
        } else if (vesselInfo != null) {
            contentAndBelow = vesselInfo;
        } else {
            contentAndBelow = statsAndRes;
        }

        // Then combine nameLine with contentAndBelow using 10px section spacing
        // Note: Outer padding is handled by NWItem.PaddedTip
        if (nameLine != null && contentAndBelow != null) {
            // For vessels with content line: account for text position within content canvas
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
     * Render the name line: Name + Quality Icon + Quality Value + Wear% + Optional Remaining Time
     * Returns LineResult with text position info for proper spacing.
     */
    private static LineResult renderNameLine(String nameText, QBuff qbuff, Integer wearPercent, String remainingTime) {
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
        }

        // Wear percentage (only if item has wear)
        BufferedImage wearImg = null;
        if (wearPercent != null) {
            totalWidth += hSpacing;
            wearImg = getNameFoundry().render("(" + wearPercent + "%)", TooltipStyle.COLOR_FOOD_FEP_SUM).img;
            totalWidth += wearImg.getWidth();
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

        // Draw wear percentage
        if (wearImg != null) {
            x += hSpacing;
            g.drawImage(wearImg, x, textY, null);
            x += wearImg.getWidth();
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
     * Render the wear line: "Wear: " (white) + "X/Y" (cyan)
     */
    private static BufferedImage renderWearLine(Wear wear) {
        BufferedImage labelImg = getContentFoundry().render("Wear: ", Color.WHITE).img;
        String valueText = String.format("%,d/%,d", wear.d, wear.m);
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_FOOD_ENERGY).img;

        int totalWidth = labelImg.getWidth() + valueImg.getWidth();
        int maxHeight = Math.max(labelImg.getHeight(), valueImg.getHeight());

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;

        g.drawImage(labelImg, x, (maxHeight - labelImg.getHeight()) / 2, null);
        x += labelImg.getWidth();
        g.drawImage(valueImg, x, (maxHeight - valueImg.getHeight()) / 2, null);

        g.dispose();
        return result;
    }

    /**
     * Render the hunger reduction line: "Hunger reduction: " (white) + "XX.X%" (yellow)
     */
    private static BufferedImage renderHungerLine(double glut) {
        BufferedImage labelImg = getContentFoundry().render("Hunger reduction: ", Color.WHITE).img;
        String valueText = Utils.odformat2(100 * glut, 1) + "%";
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_FOOD_HUNGER).img;

        int totalWidth = labelImg.getWidth() + valueImg.getWidth();
        int maxHeight = Math.max(labelImg.getHeight(), valueImg.getHeight());

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;

        g.drawImage(labelImg, x, (maxHeight - labelImg.getHeight()) / 2, null);
        x += labelImg.getWidth();
        g.drawImage(valueImg, x, (maxHeight - valueImg.getHeight()) / 2, null);

        g.dispose();
        return result;
    }

    /**
     * Render the food event bonus line: "Food event bonus: " (white) + "X.X%" (purple)
     */
    private static BufferedImage renderFoodBonusLine(double fev) {
        BufferedImage labelImg = getContentFoundry().render("Food event bonus: ", Color.WHITE).img;
        String valueText = Utils.odformat2(100 * fev, 1) + "%";
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_LP).img;

        int totalWidth = labelImg.getWidth() + valueImg.getWidth();
        int maxHeight = Math.max(labelImg.getHeight(), valueImg.getHeight());

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;

        g.drawImage(labelImg, x, (maxHeight - labelImg.getHeight()) / 2, null);
        x += labelImg.getWidth();
        g.drawImage(valueImg, x, (maxHeight - valueImg.getHeight()) / 2, null);

        g.dispose();
        return result;
    }

    /**
     * Render other tips (excluding Name, QBuff.Table, Contents, Wear, Gast which we handle ourselves)
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
                // Skip Wear - we render it ourselves with custom format
                if (tip instanceof Wear) {
                    continue;
                }
                // Skip Gast - we render hunger reduction and food event bonus ourselves
                if (tip instanceof Gast) {
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
