package nurgling;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.res.ui.tt.q.starred.Starred;
import haven.res.ui.tt.wear.Wear;
import haven.res.ui.tt.gast.Gast;
import haven.res.ui.tt.slots.ISlots;
import nurgling.iteminfo.NCuriosity;
import nurgling.styles.TooltipStyle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
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
    private static Text.Foundry bodyRegularFoundry = null;

    // Pattern for parsing liquid content names like "3.00 l of Water"
    private static final Pattern CONTENT_PATTERN = Pattern.compile("^([\\d.]+)\\s*(l of .+)$");

    // Weapon stat class names (dynamically loaded from .res files)
    private static final String[] WEAPON_STAT_CLASSES = {"Damage", "Range", "Grievous", "Armpen", "Weight", "Coolmod"};

    // Tool stat class (for mining stats like Cave-in Damage, Mining Speed, etc.)
    private static final String TOOL_CLASS = "Tool";

    /**
     * Get an integer field value from a dynamically loaded class via reflection.
     */
    private static String getIntField(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getField(fieldName);
            int value = f.getInt(obj);
            return String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a percentage field value from a dynamically loaded class via reflection.
     * Shows whole numbers without decimal (e.g., "120%") and decimals only when needed (e.g., "15.0%").
     */
    private static String getPercentField(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getField(fieldName);
            double value = f.getDouble(obj);
            double percent = value * 100;
            // Round to 1 decimal place to avoid floating point precision issues
            double rounded = Math.round(percent * 10) / 10.0;
            // Show whole number if no fractional part
            if (rounded == Math.floor(rounded)) {
                return String.format("%.0f%%", rounded);
            }
            return String.format("%.1f%%", rounded);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the icon from a Weight object's attr Resource.
     */
    private static BufferedImage getWeightAttrIcon(Object obj) {
        try {
            Field f = obj.getClass().getDeclaredField("attr");
            f.setAccessible(true);
            Object attr = f.get(obj);
            if (attr instanceof Resource) {
                Resource res = (Resource) attr;
                Resource.Image imgLayer = res.layer(Resource.imgc);
                if (imgLayer != null) {
                    return imgLayer.img;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Check if an ItemInfo is a weapon stat class we handle ourselves.
     */
    private static boolean isWeaponStat(ItemInfo ii) {
        String className = ii.getClass().getSimpleName();
        for (String name : WEAPON_STAT_CLASSES) {
            if (className.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Data extracted from an AttrMod Entry.
     * Represents a single mining stat like Cave-in Damage, Mining Speed, etc.
     */
    private static class ToolStatData {
        final BufferedImage icon;
        final String name;
        final double modValue;
        final boolean isPercent;  // true for percentage values (normattr), false for integer values (intattr)
        final boolean isTransfer; // true for Transfer entries that show "+AttributeName"
        final String transferValue; // The transfer value text (e.g., "+Strength")

        ToolStatData(BufferedImage icon, String name, double modValue, boolean isPercent) {
            this.icon = icon;
            this.name = name;
            this.modValue = modValue;
            this.isPercent = isPercent;
            this.isTransfer = false;
            this.transferValue = null;
        }

        ToolStatData(BufferedImage icon, String name, String transferValue) {
            this.icon = icon;
            this.name = name;
            this.modValue = 0;
            this.isPercent = false;
            this.isTransfer = true;
            this.transferValue = transferValue;
        }

        /**
         * Format the value as a string (e.g., "+2" for integer, "+15%" for percentage, "+Strength" for transfer)
         */
        String getFormattedValue() {
            if (isTransfer) {
                return transferValue != null ? transferValue : "";
            }
            String sign = modValue >= 0 ? "+" : "";
            if (isPercent) {
                // Format as percentage (normattr style)
                double percent = modValue * 100;
                if (percent == Math.floor(percent)) {
                    return String.format("%s%.0f%%", sign, percent);
                }
                return String.format("%s%.1f%%", sign, percent);
            } else {
                // Format as integer (intattr style)
                return String.format("%s%d", sign, (int) modValue);
            }
        }
    }

    /**
     * Parse RichText formatted value to extract plain text.
     * Format: $col[r,g,b]{text} -> returns "text"
     * Also handles multiple formats or plain text.
     */
    private static String parseRichTextValue(String richText) {
        if (richText == null || richText.isEmpty()) {
            return "";
        }
        // Pattern to match $col[...]{text}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$col\\[[^\\]]*\\]\\{([^}]*)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(richText);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            result.append(matcher.group(1));
        }
        // If no matches, return original text (might be plain text)
        return result.length() > 0 ? result.toString() : richText;
    }

    /**
     * Check if an attribute class is a percentage type by checking the class hierarchy.
     * Percentage types: normattr, inormattr, pmattr
     * Integer types: intattr
     */
    private static boolean isPercentageAttribute(Class<?> clazz) {
        // Walk up the class hierarchy looking for known attribute type names
        // Check both simple name and full name patterns since classes might be dynamically loaded
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            String simpleName = current.getSimpleName();
            String fullName = current.getName();

            // Check for percentage attribute types (check both simple and full names)
            if (simpleName.equals("normattr") || simpleName.equals("inormattr") || simpleName.equals("pmattr") ||
                fullName.contains("normattr") || fullName.contains("inormattr") || fullName.contains("pmattr")) {
                return true;
            }
            // Check for integer attribute type (explicitly not percentage)
            if (simpleName.equals("intattr") || fullName.contains("intattr")) {
                return false;
            }
            current = current.getSuperclass();
        }
        // Default to percentage if unknown (safer for display)
        return true;
    }

    /**
     * Extract tool stats from a Tool instance.
     * Tool has: sub (List of AttrMod), each AttrMod has tab (Collection of Entry).
     * Each Entry (Mod) has attr.name(), attr.icon(), and mod (double value).
     */
    private static java.util.List<ToolStatData> extractToolStats(Object toolObj) {
        java.util.List<ToolStatData> stats = new java.util.ArrayList<>();
        try {
            // Get the 'sub' field (List of ItemInfo, containing AttrMod objects)
            Field subField = toolObj.getClass().getDeclaredField("sub");
            subField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<?> subList = (java.util.List<?>) subField.get(toolObj);

            if (subList == null) return stats;

            for (Object subItem : subList) {
                // Check if this is an AttrMod
                if (!subItem.getClass().getSimpleName().equals("AttrMod")) {
                    continue;
                }

                // Get the 'tab' field (Collection of Entry)
                Field tabField = subItem.getClass().getDeclaredField("tab");
                tabField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Collection<?> tabCollection = (java.util.Collection<?>) tabField.get(subItem);

                if (tabCollection == null) continue;

                for (Object entry : tabCollection) {
                    try {
                        // Get the 'attr' field from Entry
                        Field attrField = entry.getClass().getField("attr");
                        Object attr = attrField.get(entry);

                        // Call attr.name()
                        java.lang.reflect.Method nameMethod = attr.getClass().getMethod("name");
                        String name = (String) nameMethod.invoke(attr);

                        // Call attr.icon()
                        java.lang.reflect.Method iconMethod = attr.getClass().getMethod("icon");
                        BufferedImage icon = (BufferedImage) iconMethod.invoke(attr);

                        // Get the 'mod' field from Mod (Entry subclass)
                        // If no mod field, check for Transfer entry type
                        double modValue = 0;
                        boolean hasMod = false;
                        boolean isTransfer = false;
                        String transferValue = null;
                        try {
                            Field modField = entry.getClass().getField("mod");
                            modValue = modField.getDouble(entry);
                            hasMod = true;
                        } catch (NoSuchFieldException e) {
                            // Not a Mod - might be a Transfer entry type
                            // Try to call fmtvalue() and parse the result
                            try {
                                java.lang.reflect.Method fmtMethod = entry.getClass().getMethod("fmtvalue");
                                String fmtValue = (String) fmtMethod.invoke(entry);

                                // Check if this is a Transfer entry (class name contains "Transfer")
                                if (entry.getClass().getName().contains("Transfer")) {
                                    isTransfer = true;
                                    // Parse the fmtvalue to extract the text (strip RichText formatting)
                                    // Format: $col[r,g,b]{text} -> extract "text"
                                    transferValue = parseRichTextValue(fmtValue);
                                } else {
                                    // Unknown entry type, skip
                                    continue;
                                }
                            } catch (Exception ex) {
                                continue;
                            }
                        }

                        // Check if this is a percentage attribute (normattr) or integer (intattr)
                        // by checking the class hierarchy for known attribute type names
                        boolean isPercent = isPercentageAttribute(attr.getClass());

                        if (name != null && icon != null) {
                            if (isTransfer) {
                                stats.add(new ToolStatData(icon, name, transferValue));
                            } else {
                                stats.add(new ToolStatData(icon, name, modValue, isPercent));
                            }
                        }
                    } catch (Exception e) {
                        // Skip entries we can't extract
                    }
                }
            }
        } catch (Exception e) {
            // Error extracting tool stats
        }
        return stats;
    }

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

    private static Text.Foundry getBodyRegularFoundry() {
        if (bodyRegularFoundry == null) {
            bodyRegularFoundry = TooltipStyle.createFoundry(false, TooltipStyle.FONT_SIZE_BODY, Color.WHITE);
        }
        return bodyRegularFoundry;
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

        // Find Name, QBuff, NCuriosity, Contents, Wear, Gast, ISlots, Starred, and weapon stats
        String nameText = null;
        QBuff qbuff = null;
        NCuriosity curiosity = null;
        ItemInfo.Contents contents = null;
        Wear wear = null;
        Gast gast = null;
        ISlots islots = null;
        boolean starred = false;

        // Weapon stats
        String damageValue = null;
        String rangeValue = null;
        String grievousValue = null;
        String armorPenValue = null;
        String coolmodValue = null;
        BufferedImage weightIcon = null;

        // Tool stats (mining attributes like Cave-in Damage, Mining Speed, etc.)
        java.util.List<ToolStatData> toolStats = new java.util.ArrayList<>();

        for (ItemInfo ii : info) {
            String className = ii.getClass().getSimpleName();

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
            if (ii instanceof ISlots) {
                islots = (ISlots) ii;
            }
            if (ii instanceof Starred) {
                starred = true;
            }

            // Extract weapon stats by class name (dynamically loaded from .res files)
            if (className.equals("Damage")) {
                damageValue = getIntField(ii, "dmg");
            } else if (className.equals("Range")) {
                rangeValue = getPercentField(ii, "mod");  // field is "mod"
            } else if (className.equals("Grievous")) {
                grievousValue = getPercentField(ii, "deg");  // field is "deg"
            } else if (className.equals("Armpen")) {
                armorPenValue = getPercentField(ii, "deg");  // field is "deg"
            } else if (className.equals("Coolmod")) {
                coolmodValue = getPercentField(ii, "mod");  // Attack cooldown modifier
            } else if (className.equals("Weight")) {
                // Weight stores an attr Resource (e.g., "gfx/hud/chr/melee")
                // Extract the icon from the resource
                weightIcon = getWeightAttrIcon(ii);
            } else if (className.equals(TOOL_CLASS)) {
                // Tool stores mining attributes (Cave-in Damage, Mining Speed, etc.)
                // Extract all stats from this Tool's sub list of AttrMod objects
                toolStats.addAll(extractToolStats(ii));
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

        // Render name line with star icon (if starred), quality, optional wear percentage, and optional remaining time
        LineResult nameLineResult = null;
        BufferedImage nameLine = null;
        int nameTextBottomOffset = 0;
        if (nameText != null) {
            nameLineResult = renderNameLine(nameText, qbuff, wearPercent, remainingTime, starred);
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

        // Render weapon stats
        BufferedImage damageRangeLine = null;
        if (damageValue != null || rangeValue != null) {
            damageRangeLine = TooltipStyle.cropTopOnly(renderDamageRangeLine(damageValue, rangeValue));
        }

        BufferedImage grievousLine = null;
        if (grievousValue != null) {
            grievousLine = TooltipStyle.cropTopOnly(renderGrievousLine(grievousValue));
        }

        BufferedImage armorPenLine = null;
        if (armorPenValue != null) {
            armorPenLine = TooltipStyle.cropTopOnly(renderArmorPenLine(armorPenValue));
        }

        BufferedImage coolmodLine = null;
        if (coolmodValue != null) {
            coolmodLine = TooltipStyle.cropTopOnly(renderCoolmodLine(coolmodValue));
        }

        // Render Attack weight - returns LineResult with text offsets for proper spacing
        LineResult weaponWeightLineResult = null;
        if (weightIcon != null) {
            weaponWeightLineResult = renderWeightLine(weightIcon);
        }

        // Render gilding names from ISlots
        BufferedImage gildingLine = null;
        if (islots != null && !islots.s.isEmpty()) {
            java.util.List<String> gildingNames = new java.util.ArrayList<>();
            for (ISlots.SItem si : islots.s) {
                if (si.name != null && !si.name.isEmpty()) {
                    gildingNames.add(si.name);
                }
            }
            if (!gildingNames.isEmpty()) {
                gildingLine = TooltipStyle.cropTopOnly(renderGildingLine(gildingNames));
            }
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

        // Build item info section with 7px internal spacing
        // Order: Content | Damage+Range | Attack Weight | Wear | Grievous | Armor Pen | Hunger | Food Bonus | Gilding
        // Build item info lines with LineResult tracking for proper spacing with icons
        // Each entry is either a plain BufferedImage (textTopOffset=0, textBottomOffset=0)
        // or a LineResult with actual offsets for lines containing icons
        java.util.List<LineResult> itemInfoResults = new java.util.ArrayList<>();
        if (contentLine != null) {
            itemInfoResults.add(new LineResult(contentLine, 0, 0));
        }
        if (damageRangeLine != null) {
            itemInfoResults.add(new LineResult(damageRangeLine, 0, 0));
        }
        if (weaponWeightLineResult != null) {
            itemInfoResults.add(weaponWeightLineResult);
        }
        if (wearLine != null) {
            itemInfoResults.add(new LineResult(wearLine, 0, 0));
        }
        if (grievousLine != null) {
            itemInfoResults.add(new LineResult(grievousLine, 0, 0));
        }
        if (armorPenLine != null) {
            itemInfoResults.add(new LineResult(armorPenLine, 0, 0));
        }
        if (coolmodLine != null) {
            itemInfoResults.add(new LineResult(coolmodLine, 0, 0));
        }
        // Tool stats are rendered separately with section spacing (see below)
        if (hungerLine != null) {
            itemInfoResults.add(new LineResult(hungerLine, 0, 0));
        }
        if (foodBonusLine != null) {
            itemInfoResults.add(new LineResult(foodBonusLine, 0, 0));
        }
        if (gildingLine != null) {
            itemInfoResults.add(new LineResult(gildingLine, 0, 0));
        }

        // Combine item info lines with 7px baseline-to-text-top spacing
        // Adjust spacing based on text offsets to ignore icons in spacing calculation
        BufferedImage itemInfo = null;
        int prevTextBottomOffset = 0;
        if (!itemInfoResults.isEmpty()) {
            LineResult first = itemInfoResults.get(0);
            itemInfo = first.image;
            prevTextBottomOffset = first.textBottomOffset;
            for (int i = 1; i < itemInfoResults.size(); i++) {
                LineResult current = itemInfoResults.get(i);
                // Adjust spacing: subtract previous line's bottom offset and current line's top offset
                int spacing = scaledInternalSpacing - bodyDescentVal - prevTextBottomOffset - current.textTopOffset;
                itemInfo = ItemInfo.catimgs(spacing, itemInfo, current.image);
                prevTextBottomOffset = current.textBottomOffset;
            }
        }

        // Render tool stats section with right-aligned values (mining attributes)
        LineResult toolStatsSectionResult = null;
        if (!toolStats.isEmpty()) {
            toolStatsSectionResult = renderToolStatsSection(toolStats);
        }

        // Combine itemInfo with toolStatsSection (10px section spacing)
        // Use text offsets to ensure baseline-relative spacing
        BufferedImage itemInfoAndToolStats = null;
        int toolStatsBottomOffset = 0;
        if (itemInfo != null && toolStatsSectionResult != null) {
            // Adjust spacing: 10px baseline-to-text-top, accounting for tool stats top offset
            int itemToToolSpacing = scaledSectionSpacing - bodyDescentVal - prevTextBottomOffset - toolStatsSectionResult.textTopOffset;
            itemInfoAndToolStats = ItemInfo.catimgs(itemToToolSpacing, itemInfo, toolStatsSectionResult.image);
            toolStatsBottomOffset = toolStatsSectionResult.textBottomOffset;
        } else if (itemInfo != null) {
            itemInfoAndToolStats = itemInfo;
            // prevTextBottomOffset already set from itemInfo loop
            toolStatsBottomOffset = prevTextBottomOffset;
        } else if (toolStatsSectionResult != null) {
            itemInfoAndToolStats = toolStatsSectionResult.image;
            toolStatsBottomOffset = toolStatsSectionResult.textBottomOffset;
        }

        // Render curio stats separately (NCuriosity is skipped in renderOtherTips)
        BufferedImage curioStats = null;
        if (curiosity != null) {
            curioStats = TooltipStyle.cropTopOnly(curiosity.tipimg());
        }

        // Combine itemInfoAndToolStats with curioStats (10px section spacing)
        // Account for tool stats bottom offset for proper baseline spacing
        BufferedImage itemInfoAndCurio = null;
        if (itemInfoAndToolStats != null && curioStats != null) {
            int itemToCurioSpacing = scaledSectionSpacing - bodyDescentVal - toolStatsBottomOffset;
            itemInfoAndCurio = ItemInfo.catimgs(itemToCurioSpacing, itemInfoAndToolStats, curioStats);
        } else if (itemInfoAndToolStats != null) {
            itemInfoAndCurio = itemInfoAndToolStats;
        } else if (curioStats != null) {
            itemInfoAndCurio = curioStats;
        }

        // Combine itemInfoAndCurio with statsAndRes (10px spacing)
        BufferedImage contentAndBelow = null;
        if (itemInfoAndCurio != null && statsAndRes != null) {
            int itemToStatsSpacing = scaledSectionSpacing - bodyDescentVal;
            contentAndBelow = ItemInfo.catimgs(itemToStatsSpacing, itemInfoAndCurio, statsAndRes);
        } else if (itemInfoAndCurio != null) {
            contentAndBelow = itemInfoAndCurio;
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
     * Render the name line: [Star Icon] + Name + Quality Icon + Quality Value + Wear% + Optional Remaining Time
     * Returns LineResult with text position info for proper spacing.
     */
    private static LineResult renderNameLine(String nameText, QBuff qbuff, Integer wearPercent, String remainingTime, boolean starred) {
        BufferedImage nameImg = getNameFoundry().render(nameText, Color.WHITE).img;
        int hSpacing = UI.scale(TooltipStyle.HORIZONTAL_SPACING);
        int iconToTextSpacing = UI.scale(TooltipStyle.ICON_TO_TEXT_SPACING);

        int totalWidth = 0;
        int textHeight = nameImg.getHeight();
        int maxHeight = textHeight;

        // Star icon (if starred)
        BufferedImage starIcon = null;
        if (starred) {
            try {
                starIcon = Resource.classres(Starred.class).layer(Resource.imgc).scaled();
                if (starIcon != null) {
                    totalWidth += starIcon.getWidth() + hSpacing;  // 7px between star and name
                    maxHeight = Math.max(maxHeight, starIcon.getHeight());
                }
            } catch (Exception ignored) {}
        }

        totalWidth += nameImg.getWidth();

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
        // Color based on percentage: 0-20% red, 20-60% yellow, 60-100% green
        BufferedImage wearImg = null;
        if (wearPercent != null) {
            totalWidth += hSpacing;
            Color wearColor;
            if (wearPercent <= 20) {
                wearColor = new Color(255, 80, 80);  // Red
            } else if (wearPercent <= 60) {
                wearColor = new Color(255, 255, 80);  // Yellow
            } else {
                wearColor = new Color(80, 255, 80);  // Green
            }
            wearImg = getNameFoundry().render("(" + wearPercent + "%)", wearColor).img;
            totalWidth += wearImg.getWidth();
        }

        // Remaining time for curios
        BufferedImage timeImg = null;
        if (remainingTime != null && !remainingTime.isEmpty()) {
            totalWidth += hSpacing;
            timeImg = getNameFoundry().render("(" + remainingTime + ")", TooltipStyle.COLOR_RESOURCE_PATH).img;
            totalWidth += timeImg.getWidth();
        }

        // Canvas must fit both text and icons
        int canvasHeight = maxHeight;

        // Center text and icons in canvas
        int textY = (canvasHeight - textHeight) / 2;

        // Compose the line
        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, canvasHeight));
        Graphics g = result.getGraphics();
        int x = 0;

        // Draw star icon (if starred)
        if (starIcon != null) {
            int iconY = (canvasHeight - starIcon.getHeight()) / 2;
            g.drawImage(starIcon, x, iconY, null);
            x += starIcon.getWidth() + hSpacing;  // 7px between star and name
        }

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
     * Render the wear line: "Wear: " (regular white) + "X/Y" (semibold cyan)
     */
    private static BufferedImage renderWearLine(Wear wear) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Wear: ", Color.WHITE).img;
        String valueText = String.format("%,d/%,d", wear.d, wear.m);
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_FOOD_ENERGY).img;
        return composePair(labelImg, valueImg);
    }

    /**
     * Render the hunger reduction line: "Hunger reduction: " (regular white) + "XX.X%" (semibold yellow)
     */
    private static BufferedImage renderHungerLine(double glut) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Hunger reduction: ", Color.WHITE).img;
        String valueText = Utils.odformat2(100 * glut, 1) + "%";
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_FOOD_HUNGER).img;
        return composePair(labelImg, valueImg);
    }

    /**
     * Render the food event bonus line: "Food event bonus: " (regular white) + "X.X%" (semibold purple)
     */
    private static BufferedImage renderFoodBonusLine(double fev) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Food event bonus: ", Color.WHITE).img;
        String valueText = Utils.odformat2(100 * fev, 1) + "%";
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_LP).img;
        return composePair(labelImg, valueImg);
    }

    /**
     * Render the Damage + Range line: "Damage: " (regular) + "X" (semibold purple) + "Range: " (regular) + "X%" (semibold cyan)
     */
    private static BufferedImage renderDamageRangeLine(String damageValue, String rangeValue) {
        int hSpacing = UI.scale(TooltipStyle.HORIZONTAL_SPACING);
        java.util.List<BufferedImage> parts = new java.util.ArrayList<>();

        if (damageValue != null) {
            BufferedImage labelImg = getBodyRegularFoundry().render("Damage: ", Color.WHITE).img;
            BufferedImage valueImg = getContentFoundry().render(damageValue, TooltipStyle.COLOR_LP).img;  // #D2B2FF
            parts.add(composePair(labelImg, valueImg));
        }

        if (rangeValue != null) {
            BufferedImage labelImg = getBodyRegularFoundry().render("Range: ", Color.WHITE).img;
            BufferedImage valueImg = getContentFoundry().render(rangeValue, TooltipStyle.COLOR_FOOD_ENERGY).img;  // #00EEFF
            parts.add(composePair(labelImg, valueImg));
        }

        if (parts.isEmpty()) {
            return null;
        }

        // Combine with horizontal spacing
        int totalWidth = 0;
        int maxHeight = 0;
        for (BufferedImage img : parts) {
            totalWidth += img.getWidth();
            maxHeight = Math.max(maxHeight, img.getHeight());
        }
        totalWidth += hSpacing * (parts.size() - 1);

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;
        for (int i = 0; i < parts.size(); i++) {
            BufferedImage img = parts.get(i);
            g.drawImage(img, x, (maxHeight - img.getHeight()) / 2, null);
            x += img.getWidth();
            if (i < parts.size() - 1) {
                x += hSpacing;
            }
        }
        g.dispose();
        return result;
    }

    /**
     * Render the Grievous damage line: "Grievous damage: " (regular) + "X%" (semibold yellow)
     */
    private static BufferedImage renderGrievousLine(String value) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Grievous damage: ", Color.WHITE).img;
        BufferedImage valueImg = getContentFoundry().render(value, TooltipStyle.COLOR_FOOD_HUNGER).img;  // #FFFF82
        return composePair(labelImg, valueImg);
    }

    /**
     * Render the Armor penetration line: "Armor penetration: " (regular) + "X%" (semibold pink)
     */
    private static BufferedImage renderArmorPenLine(String value) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Armor penetration: ", Color.WHITE).img;
        BufferedImage valueImg = getContentFoundry().render(value, TooltipStyle.COLOR_MENTAL_WEIGHT).img;  // #FF94E8
        return composePair(labelImg, valueImg);
    }

    /**
     * Render the Attack cooldown line: "Attack cooldown: " (regular) + "X%" (semibold cyan)
     */
    private static BufferedImage renderCoolmodLine(String value) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Attack cooldown: ", Color.WHITE).img;
        BufferedImage valueImg = getContentFoundry().render(value, TooltipStyle.COLOR_FOOD_ENERGY).img;  // #00EEFF
        return composePair(labelImg, valueImg);
    }

    /**
     * Render all tool stats as a section with right-aligned values.
     * Returns a LineResult with proper text offsets for baseline-relative spacing.
     */
    private static LineResult renderToolStatsSection(java.util.List<ToolStatData> toolStats) {
        if (toolStats == null || toolStats.isEmpty()) {
            return null;
        }

        int iconToTextSpacing = UI.scale(TooltipStyle.ICON_TO_TEXT_SPACING);
        int scaledInternalSpacing = UI.scale(TooltipStyle.INTERNAL_SPACING);
        int bodyDescentVal = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);

        // First pass: calculate max widths for alignment
        java.util.List<BufferedImage> icons = new java.util.ArrayList<>();
        java.util.List<BufferedImage> labels = new java.util.ArrayList<>();
        java.util.List<BufferedImage> values = new java.util.ArrayList<>();
        int maxLabelWidth = 0;
        int maxValueWidth = 0;
        int textHeight = 0;

        for (ToolStatData tool : toolStats) {
            // Render label (Regular) and value (Semibold)
            BufferedImage labelImg = TooltipStyle.cropTopOnly(getBodyRegularFoundry().render(tool.name, Color.WHITE).img);
            BufferedImage valueImg = TooltipStyle.cropTopOnly(getContentFoundry().render(tool.getFormattedValue(), TooltipStyle.COLOR_STUDY_TIME).img);

            labels.add(labelImg);
            values.add(valueImg);
            icons.add(tool.icon);

            maxLabelWidth = Math.max(maxLabelWidth, labelImg.getWidth());
            maxValueWidth = Math.max(maxValueWidth, valueImg.getWidth());
            textHeight = Math.max(textHeight, Math.max(labelImg.getHeight(), valueImg.getHeight()));
        }

        // Scale icons to match text height
        int iconSize = textHeight;
        java.util.List<BufferedImage> scaledIcons = new java.util.ArrayList<>();
        for (BufferedImage icon : icons) {
            scaledIcons.add(PUtils.convolvedown(icon, new Coord(iconSize, iconSize), CharWnd.iconfilter));
        }

        // Calculate total width: icon + spacing + maxLabelWidth + spacing + maxValueWidth
        int gapBetweenLabelAndValue = UI.scale(7);  // Gap between label and value
        int totalWidth = iconSize + iconToTextSpacing + maxLabelWidth + gapBetweenLabelAndValue + maxValueWidth;

        // Get font descent for visual text centering
        int descent = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);
        int visualTextHeight = textHeight - descent;
        int visualTextCenter = visualTextHeight / 2;

        // Second pass: render each line as LineResult with text offsets
        java.util.List<LineResult> lineResults = new java.util.ArrayList<>();
        for (int i = 0; i < toolStats.size(); i++) {
            BufferedImage scaledIcon = scaledIcons.get(i);
            BufferedImage labelImg = labels.get(i);
            BufferedImage valueImg = values.get(i);

            // Calculate icon Y position (centered on visual text)
            int iconYRelative = visualTextCenter - scaledIcon.getHeight() / 2;

            // Calculate canvas height and positions
            int canvasHeight = textHeight;
            int textY = 0;
            int iconY = iconYRelative;

            // If icon extends above text, expand canvas
            if (iconYRelative < 0) {
                canvasHeight = textHeight - iconYRelative;
                textY = -iconYRelative;
                iconY = 0;
            }
            // If icon extends below text, expand canvas
            int iconBottom = iconY + scaledIcon.getHeight();
            if (iconBottom > canvasHeight) {
                canvasHeight = iconBottom;
            }

            // Create line image
            BufferedImage line = TexI.mkbuf(new Coord(totalWidth, canvasHeight));
            Graphics g = line.getGraphics();

            int x = 0;
            // Draw icon
            g.drawImage(scaledIcon, x, iconY, null);
            x += scaledIcon.getWidth() + iconToTextSpacing;

            // Draw label (left-aligned within label area)
            g.drawImage(labelImg, x, textY + (textHeight - labelImg.getHeight()) / 2, null);

            // Draw value (right-aligned)
            int valueX = totalWidth - valueImg.getWidth();
            g.drawImage(valueImg, valueX, textY + (textHeight - valueImg.getHeight()) / 2, null);

            g.dispose();

            // Track text offsets for proper spacing
            int textTopOffset = textY;
            int textBottomOffset = canvasHeight - textY - textHeight;
            lineResults.add(new LineResult(line, textTopOffset, textBottomOffset));
        }

        // Combine lines with internal spacing (7px baseline-to-text-top)
        if (lineResults.isEmpty()) {
            return null;
        }

        // Combine all lines, tracking cumulative text offsets
        LineResult first = lineResults.get(0);
        BufferedImage result = first.image;
        int firstTextTopOffset = first.textTopOffset;
        int prevTextBottomOffset = first.textBottomOffset;

        for (int i = 1; i < lineResults.size(); i++) {
            LineResult current = lineResults.get(i);
            // Adjust spacing: subtract previous line's bottom offset and current line's top offset
            int spacing = scaledInternalSpacing - bodyDescentVal - prevTextBottomOffset - current.textTopOffset;
            result = ItemInfo.catimgs(spacing, result, current.image);
            prevTextBottomOffset = current.textBottomOffset;
        }

        // Return with first line's top offset and last line's bottom offset
        return new LineResult(result, firstTextTopOffset, prevTextBottomOffset);
    }

    /**
     * Render a tool stat line: icon + "Name " (regular) + formatted value (semibold green #99FF84)
     * Returns LineResult with text offsets for proper spacing.
     * @deprecated Use renderToolStatsSection instead for right-aligned values
     */
    private static LineResult renderToolStatLine(ToolStatData tool) {
        // Format value ourselves to control color (#99FF84)
        BufferedImage labelImg = getBodyRegularFoundry().render(tool.name + " ", Color.WHITE).img;
        BufferedImage valueImg = getContentFoundry().render(tool.getFormattedValue(), TooltipStyle.COLOR_STUDY_TIME).img;  // #99FF84

        // Crop text images
        BufferedImage croppedLabel = TooltipStyle.cropTopOnly(labelImg);
        BufferedImage croppedValue = TooltipStyle.cropTopOnly(valueImg);

        int textHeight = Math.max(croppedLabel.getHeight(), croppedValue.getHeight());

        // Scale icon to match text height
        int iconSize = textHeight;
        BufferedImage scaledIcon = PUtils.convolvedown(tool.icon, new Coord(iconSize, iconSize), CharWnd.iconfilter);

        // Get font descent for visual text centering
        int descent = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);
        int visualTextHeight = textHeight - descent;
        int visualTextCenter = visualTextHeight / 2;

        // Center icon on visual text center
        int iconYRelative = visualTextCenter - scaledIcon.getHeight() / 2;

        // Calculate canvas height and positions
        int canvasHeight = textHeight;
        int textY = 0;
        int iconY = iconYRelative;

        // If icon extends above text, expand canvas
        if (iconYRelative < 0) {
            canvasHeight = textHeight - iconYRelative;
            textY = -iconYRelative;
            iconY = 0;
        }
        // If icon extends below text, expand canvas
        int iconBottom = iconY + scaledIcon.getHeight();
        if (iconBottom > canvasHeight) {
            canvasHeight = iconBottom;
        }

        // Spacing between icon and text
        int iconToTextSpacing = UI.scale(TooltipStyle.ICON_TO_TEXT_SPACING);

        // Compose: icon + label + value
        int totalWidth = scaledIcon.getWidth() + iconToTextSpacing + croppedLabel.getWidth() + croppedValue.getWidth();
        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, canvasHeight));
        Graphics g = result.getGraphics();

        int x = 0;
        g.drawImage(scaledIcon, x, iconY, null);
        x += scaledIcon.getWidth() + iconToTextSpacing;
        g.drawImage(croppedLabel, x, textY + (textHeight - croppedLabel.getHeight()) / 2, null);
        x += croppedLabel.getWidth();
        g.drawImage(croppedValue, x, textY + (textHeight - croppedValue.getHeight()) / 2, null);
        g.dispose();

        // Return with text offsets
        int textTopOffset = textY;
        int textBottomOffset = canvasHeight - textY - textHeight;
        return new LineResult(result, textTopOffset, textBottomOffset);
    }

    /**
     * Render the Attack weight line: "Attack weight: " (regular) + icon
     * Crops text first to remove top padding, then composes with icon.
     * Icon is vertically centered on the visual text area (excluding descent).
     */
    private static LineResult renderWeightLine(BufferedImage icon) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Attack weight: ", Color.WHITE).img;
        // Crop the text to remove top padding (like other lines do)
        BufferedImage croppedLabel = TooltipStyle.cropTopOnly(labelImg);

        int textHeight = croppedLabel.getHeight();

        // Scale icon to match text height (icon can be same size as text)
        int iconSize = textHeight;
        BufferedImage scaledIcon = PUtils.convolvedown(icon, new Coord(iconSize, iconSize), CharWnd.iconfilter);

        // Get font descent to find visual text center (excluding descent area)
        int descent = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);
        int visualTextHeight = textHeight - descent;
        int visualTextCenter = visualTextHeight / 2;

        // Center icon on visual text center
        int iconYRelative = visualTextCenter - scaledIcon.getHeight() / 2;

        // Calculate canvas height and positions
        int canvasHeight = textHeight;
        int textY = 0;
        int iconY = iconYRelative;

        // If icon extends above text, expand canvas
        if (iconYRelative < 0) {
            canvasHeight = textHeight - iconYRelative;
            textY = -iconYRelative;
            iconY = 0;
        }
        // If icon extends below text, expand canvas
        int iconBottom = iconY + scaledIcon.getHeight();
        if (iconBottom > canvasHeight) {
            canvasHeight = iconBottom;
        }

        // Compose: text + icon
        int totalWidth = croppedLabel.getWidth() + scaledIcon.getWidth();
        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, canvasHeight));
        Graphics g = result.getGraphics();
        g.drawImage(croppedLabel, 0, textY, null);
        g.drawImage(scaledIcon, croppedLabel.getWidth(), iconY, null);
        g.dispose();

        // Return with text offsets so spacing calculations ignore the icon
        int textTopOffset = textY;
        int textBottomOffset = canvasHeight - textY - textHeight;
        return new LineResult(result, textTopOffset, textBottomOffset);
    }

    /**
     * Render the gilding names line: comma-separated gilding names
     */
    private static BufferedImage renderGildingLine(java.util.List<String> gildingNames) {
        String gildingsText = String.join(", ", gildingNames);
        return getContentFoundry().render(gildingsText, Color.WHITE).img;
    }

    /**
     * Helper to compose a label + value pair
     */
    private static BufferedImage composePair(BufferedImage labelImg, BufferedImage valueImg) {
        int totalWidth = labelImg.getWidth() + valueImg.getWidth();
        int maxHeight = Math.max(labelImg.getHeight(), valueImg.getHeight());

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        g.drawImage(labelImg, 0, (maxHeight - labelImg.getHeight()) / 2, null);
        g.drawImage(valueImg, labelImg.getWidth(), (maxHeight - valueImg.getHeight()) / 2, null);
        g.dispose();
        return result;
    }

    /**
     * Render other tips (excluding Name, QBuff.Table, Contents, Wear, Gast, ISlots, weapon stats which we handle ourselves)
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
                // Skip ISlots - we render gilding names ourselves
                if (tip instanceof ISlots) {
                    continue;
                }
                // Skip NCuriosity - we render curio stats ourselves with section spacing
                if (tip instanceof NCuriosity) {
                    continue;
                }
                // Skip weapon stat classes (Damage, Range, Grievous, Armpen, Coolmod) - we render them ourselves
                if (isWeaponStat(ii)) {
                    continue;
                }
                // Skip Quality class - it renders duplicate resource path
                if (tip.getClass().getSimpleName().equals("Quality")) {
                    continue;
                }
                // Skip NSearchingHighlight and NQuestItem - nurgling classes (overlay only, no tooltip)
                if (tip.getClass().getSimpleName().equals("NSearchingHighlight") ||
                    tip.getClass().getSimpleName().equals("NQuestItem")) {
                    continue;
                }
                // Skip Tool class - it renders "When used:" header and resource path
                // TODO: Render mining stats ourselves without the header
                if (tip.getClass().getSimpleName().equals("Tool")) {
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
