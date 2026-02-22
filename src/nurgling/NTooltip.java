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
    private static Text.Foundry gildingStatNameFoundry = null;  // 9px for gilding stat names

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




    private static int getIntField(Class<?> clazz, Object obj, String... names) {
        for (String name : names) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.getInt(obj);
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private static double getDoubleField(Class<?> clazz, Object obj, String... names) {
        for (String name : names) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.getDouble(obj);
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private static Object getObjectField(Class<?> clazz, Object obj, String... names) {
        for (String name : names) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (Exception ignored) {}
        }
        return null;
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
     * UNIFIED STAT EXTRACTION: Extract stats from a single AttrMod object.
     * This is the core logic used by all stat extraction scenarios (gilding, base, tool).
     *
     * @param attrModObj An AttrMod instance
     * @return List of GildingStatData with icon, name, and formatted value
     */
    private static java.util.List<GildingStatData> extractStatsFromAttrMod(Object attrModObj) {
        java.util.List<GildingStatData> stats = new java.util.ArrayList<>();
        if (attrModObj == null) return stats;

        try {
            // Get the 'tab' field (Collection of Entry)
            Field tabField = attrModObj.getClass().getDeclaredField("tab");
            tabField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Collection<?> tabCollection = (java.util.Collection<?>) tabField.get(attrModObj);

            if (tabCollection == null) return stats;

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

                    // Check if it's a Mod entry (has 'mod' field) or Transfer entry
                    String formattedValue = "";
                    String entryClassName = entry.getClass().getSimpleName();

                    if (entryClassName.equals("Mod") || entry.getClass().getName().contains("Mod")) {
                        // It's a Mod entry - get mod value
                        try {
                            Field modField = entry.getClass().getField("mod");
                            double modValue = modField.getDouble(entry);

                            // Check if it's percentage or integer type
                            boolean isPercent = isPercentageAttribute(attr.getClass());
                            String sign = modValue >= 0 ? "+" : "";
                            if (isPercent) {
                                double percent = modValue * 100;
                                if (percent == Math.floor(percent)) {
                                    formattedValue = String.format("%s%.0f%%", sign, percent);
                                } else {
                                    formattedValue = String.format("%s%.1f%%", sign, percent);
                                }
                            } else {
                                formattedValue = String.format("%s%d", sign, (int) modValue);
                            }
                        } catch (NoSuchFieldException e) {
                            // Not a Mod with numeric value
                        }
                    } else if (entryClassName.equals("Transfer") || entry.getClass().getName().contains("Transfer")) {
                        // It's a Transfer entry - use fmtvalue()
                        try {
                            java.lang.reflect.Method fmtMethod = entry.getClass().getMethod("fmtvalue");
                            String richText = (String) fmtMethod.invoke(entry);
                            formattedValue = parseRichTextValue(richText);
                        } catch (Exception ignored) {}
                    }

                    if (icon != null && name != null && !formattedValue.isEmpty()) {
                        stats.add(new GildingStatData(icon, name, formattedValue));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return stats;
    }

    /**
     * UNIFIED STAT EXTRACTION: Extract stats from a list of ItemInfo objects.
     * Finds all AttrMod instances and extracts their stats.
     *
     * @param infoList List of ItemInfo objects to search
     * @return List of GildingStatData with icon, name, and formatted value
     */
    private static java.util.List<GildingStatData> extractAttrModStats(java.util.List<ItemInfo> infoList) {
        java.util.List<GildingStatData> stats = new java.util.ArrayList<>();
        if (infoList == null) return stats;

        for (ItemInfo info : infoList) {
            if (!info.getClass().getSimpleName().equals("AttrMod")) {
                continue;
            }
            stats.addAll(extractStatsFromAttrMod(info));
        }

        return stats;
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

    private static Text.Foundry getGildingStatNameFoundry() {
        if (gildingStatNameFoundry == null) {
            gildingStatNameFoundry = TooltipStyle.createFoundry(false, TooltipStyle.FONT_SIZE_RESOURCE, Color.WHITE);  // 9px regular
        }
        return gildingStatNameFoundry;
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

        // Armor class stats (hard/soft)
        Integer armorHard = null;
        Integer armorSoft = null;

        // Tool stats (mining attributes like Cave-in Damage, Mining Speed, etc.)
        java.util.List<ToolStatData> toolStats = new java.util.ArrayList<>();

        Object islotsObj = null;  // Can be ISlots or slots_alt.ISlots
        Object baseAttrMod = null;  // Base item stats (non-gildable)
        for (ItemInfo ii : info) {
            String className = ii.getClass().getSimpleName();
            String fullName = ii.getClass().getName();


            // Capture base AttrMod (non-gilding stats)
            if (className.equals("AttrMod") && fullName.contains("attrmod")) {
                baseAttrMod = ii;
            }

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
            // Check for both ISlots classes (slots and slots_alt)
            if (ii instanceof ISlots) {
                islots = (ISlots) ii;
            } else if (className.equals("ISlots") && fullName.contains("slots")) {
                // Handle dynamically loaded slots_alt.ISlots
                islotsObj = ii;
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
            } else if (className.equals("Armor")) {
                // Armor class has "hard" and "soft" fields
                String hardStr = getIntField(ii, "hard");
                String softStr = getIntField(ii, "soft");
                if (hardStr != null) armorHard = Integer.parseInt(hardStr);
                if (softStr != null) armorSoft = Integer.parseInt(softStr);
            }
        }

        // If no name found, try default
        if (nameText == null) {
            try {
                nameText = ItemInfo.Name.Default.get(owner);
            } catch (Exception ignored) {}
        }

        // Extract gilding data (works for both ISlots and alternative ISlots via reflection)
        Integer gildingLeft = null;
        Integer gildingTotal = null;
        double gildingPmin = 0;
        double gildingPmax = 0;
        Resource[] gildingAttrs = null;
        java.util.Collection<?> gildingItems = null;

        if (islots != null) {
            // Standard ISlots path
            gildingLeft = islots.left;
            int used = islots.s.size();
            gildingTotal = islots.left + used;  // total = remaining + used
            gildingPmin = islots.pmin;
            gildingPmax = islots.pmax;
            gildingAttrs = islots.attrs;
            gildingItems = islots.s;
        } else if (islotsObj != null) {
            // Alternative ISlots via reflection
            try {
                Class<?> clazz = islotsObj.getClass();
                int uses = getIntField(clazz, islotsObj, "uses");
                int used = getIntField(clazz, islotsObj, "used");
                gildingLeft = uses - used;
                gildingTotal = uses;
                gildingPmin = getDoubleField(clazz, islotsObj, "pmin");
                gildingPmax = getDoubleField(clazz, islotsObj, "pmax");
                gildingAttrs = (Resource[]) getObjectField(clazz, islotsObj, "attrs");
                gildingItems = (java.util.Collection<?>) getObjectField(clazz, islotsObj, "s");
            } catch (Exception e) {
                // Ignore extraction errors
            }
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

        // Render name line with star icon (if starred), quality, optional wear percentage, optional remaining time, and gilding count
        LineResult nameLineResult = null;
        BufferedImage nameLine = null;
        int nameTextBottomOffset = 0;
        if (nameText != null) {
            nameLineResult = renderNameLine(nameText, qbuff, wearPercent, remainingTime, starred, gildingLeft, gildingTotal);
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

        // Render custom lines for Wear, Armor class, Hunger reduction, Food event bonus
        BufferedImage wearLine = null;
        if (wear != null && wear.m > 0) {
            wearLine = TooltipStyle.cropTopOnly(renderWearLine(wear));
        }

        BufferedImage armorClassLine = null;
        if (armorHard != null && armorSoft != null) {
            armorClassLine = TooltipStyle.cropTopOnly(renderArmorClassLine(armorHard, armorSoft));
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

        // Render base stats section (non-gilding stats from AttrMod at item level)
        LineResult baseStatsResult = null;
        if (baseAttrMod != null) {
            java.util.List<GildingStatData> baseStats = extractStatsFromAttrMod(baseAttrMod);
            if (!baseStats.isEmpty()) {
                baseStatsResult = renderBaseStatsSection(baseStats);
            }
        }

        // Render gilding chance line (single line: "Gilding chance X% to Y%")
        LineResult gildingChanceLineResult = null;
        if (gildingLeft != null && gildingTotal != null) {
            gildingChanceLineResult = renderGildingChanceLine(gildingPmin, gildingPmax, gildingAttrs);
        }

        // Render gilding sections (hierarchical: header + indented stats)
        LineResult gildingSectionsResult = null;
        if (gildingItems != null && !gildingItems.isEmpty()) {
            // For ISlots.SItem, extract fields directly
            // For reflection-based access, use reflection extractors
            if (islots != null) {
                // ISlots.SItem path - cast to known type
                @SuppressWarnings("unchecked")
                java.util.Collection<ISlots.SItem> typedItems = (java.util.Collection<ISlots.SItem>) gildingItems;
                gildingSectionsResult = renderGildingSections(
                    typedItems,
                    item -> item.name,
                    item -> item.res,
                    item -> item.spr,
                    item -> item.info
                );
            } else {
                // Reflection-based path
                gildingSectionsResult = renderGildingSections(
                    gildingItems,
                    item -> (String) getObjectField(item.getClass(), item, "name"),
                    item -> (Resource) getObjectField(item.getClass(), item, "res"),
                    item -> (GSprite) getObjectField(item.getClass(), item, "spr"),
                    item -> {
                        @SuppressWarnings("unchecked")
                        List<ItemInfo> itemInfo = (List<ItemInfo>) getObjectField(item.getClass(), item, "info");
                        return itemInfo;
                    }
                );
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
        // armorClassLine is handled separately below with 10px section spacing after it
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
        // Base stats (non-gilding stats from item's AttrMod) - above gilding chance
        // Note: baseStatsResult and gildingChanceLineResult are handled separately below
        // to use section spacing (10px) between them instead of internal spacing (7px)

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

        // Add armor class with internal spacing (7px) to itemInfo, then use section spacing (10px) after it
        if (armorClassLine != null) {
            if (itemInfo != null) {
                int armorClassSpacing = scaledInternalSpacing - bodyDescentVal - prevTextBottomOffset;
                itemInfo = ItemInfo.catimgs(armorClassSpacing, itemInfo, armorClassLine);
            } else {
                itemInfo = armorClassLine;
            }
            prevTextBottomOffset = 0;  // Reset for section spacing after armor class
        }

        // Add base stats with SECTION spacing (10px) after armor class, or internal spacing (7px) otherwise
        if (baseStatsResult != null) {
            if (itemInfo != null) {
                // Use section spacing (10px) if armor class was added, otherwise internal spacing (7px)
                int spacingBase = (armorClassLine != null) ? scaledSectionSpacing : scaledInternalSpacing;
                int baseStatsSpacing = spacingBase - bodyDescentVal - prevTextBottomOffset - baseStatsResult.textTopOffset;
                itemInfo = ItemInfo.catimgs(baseStatsSpacing, itemInfo, baseStatsResult.image);
                prevTextBottomOffset = baseStatsResult.textBottomOffset;
            } else {
                itemInfo = baseStatsResult.image;
                prevTextBottomOffset = baseStatsResult.textBottomOffset;
            }
        }

        // Add gilding chance with SECTION spacing (10px) to separate from base stats
        // Note: Don't subtract bodyDescentVal here since prevTextBottomOffset already accounts for baseline
        if (gildingChanceLineResult != null) {
            if (itemInfo != null) {
                int gildingChanceSpacing = scaledSectionSpacing - prevTextBottomOffset - gildingChanceLineResult.textTopOffset;
                itemInfo = ItemInfo.catimgs(gildingChanceSpacing, itemInfo, gildingChanceLineResult.image);
                prevTextBottomOffset = gildingChanceLineResult.textBottomOffset;
            } else {
                itemInfo = gildingChanceLineResult.image;
                prevTextBottomOffset = gildingChanceLineResult.textBottomOffset;
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

        // Combine itemInfoAndToolStats with gildingSections (10px section spacing)
        BufferedImage itemInfoAndGilding = null;
        int gildingBottomOffset = toolStatsBottomOffset;
        if (itemInfoAndToolStats != null && gildingSectionsResult != null) {
            int itemToGildingSpacing = scaledSectionSpacing - bodyDescentVal - toolStatsBottomOffset - gildingSectionsResult.textTopOffset;
            itemInfoAndGilding = ItemInfo.catimgs(itemToGildingSpacing, itemInfoAndToolStats, gildingSectionsResult.image);
            gildingBottomOffset = gildingSectionsResult.textBottomOffset;
        } else if (itemInfoAndToolStats != null) {
            itemInfoAndGilding = itemInfoAndToolStats;
        } else if (gildingSectionsResult != null) {
            itemInfoAndGilding = gildingSectionsResult.image;
            gildingBottomOffset = gildingSectionsResult.textBottomOffset;
        }

        // Render curio stats separately (NCuriosity is skipped in renderOtherTips)
        BufferedImage curioStats = null;
        if (curiosity != null) {
            curioStats = TooltipStyle.cropTopOnly(curiosity.tipimg());
        }

        // Combine itemInfoAndGilding with curioStats (10px section spacing)
        BufferedImage itemInfoAndCurio = null;
        int curioBottomOffset = gildingBottomOffset;  // Track current bottom offset
        if (itemInfoAndGilding != null && curioStats != null) {
            int itemToCurioSpacing = scaledSectionSpacing - bodyDescentVal - gildingBottomOffset;
            itemInfoAndCurio = ItemInfo.catimgs(itemToCurioSpacing, itemInfoAndGilding, curioStats);
            curioBottomOffset = 0;  // Curio stats have no special bottom offset
        } else if (itemInfoAndGilding != null) {
            itemInfoAndCurio = itemInfoAndGilding;
            // curioBottomOffset stays as gildingBottomOffset
        } else if (curioStats != null) {
            itemInfoAndCurio = curioStats;
            curioBottomOffset = 0;
        }

        // Combine itemInfoAndCurio with statsAndRes (10px spacing to resource line)
        BufferedImage contentAndBelow = null;
        if (itemInfoAndCurio != null && statsAndRes != null) {
            // Use full section spacing (10px) for clean visual spacing to resource line
            int itemToStatsSpacing = scaledSectionSpacing;
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
     * Render the name line: [Star Icon] + Name + Quality Icon + Quality Value + Wear% + Optional Remaining Time + Gilding Count
     * Returns LineResult with text position info for proper spacing.
     */
    private static LineResult renderNameLine(String nameText, QBuff qbuff, Integer wearPercent, String remainingTime, boolean starred, Integer gildingLeft, Integer gildingTotal) {
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

        // Gilding count (remaining/total) in cyan - shown after quality
        BufferedImage gildingCountImg = null;
        if (gildingLeft != null && gildingTotal != null && gildingTotal > 0) {
            String gildingText = "(" + gildingLeft + "/" + gildingTotal + ")";
            gildingCountImg = getNameFoundry().render(gildingText, TooltipStyle.COLOR_LPH).img;  // Cyan
            totalWidth += hSpacing + gildingCountImg.getWidth();
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

        // Draw gilding count
        if (gildingCountImg != null) {
            x += hSpacing;
            g.drawImage(gildingCountImg, x, textY, null);
            x += gildingCountImg.getWidth();
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
        return TooltipStyle.composePair(labelImg, valueImg);
    }

    /**
     * Render the armor class line: "Armor class: " (regular white) + "X/Y" (semibold pink #FF94E8)
     */
    private static BufferedImage renderArmorClassLine(int hard, int soft) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Armor class: ", Color.WHITE).img;
        String valueText = String.format("%d/%d", hard, soft);
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_MENTAL_WEIGHT).img;
        return TooltipStyle.composePair(labelImg, valueImg);
    }

    /**
     * Render the hunger reduction line: "Hunger reduction: " (regular white) + "XX.X%" (semibold yellow)
     */
    private static BufferedImage renderHungerLine(double glut) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Hunger reduction: ", Color.WHITE).img;
        String valueText = Utils.odformat2(100 * glut, 1) + "%";
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_FOOD_HUNGER).img;
        return TooltipStyle.composePair(labelImg, valueImg);
    }

    /**
     * Render the food event bonus line: "Food event bonus: " (regular white) + "X.X%" (semibold purple)
     */
    private static BufferedImage renderFoodBonusLine(double fev) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Food event bonus: ", Color.WHITE).img;
        String valueText = Utils.odformat2(100 * fev, 1) + "%";
        BufferedImage valueImg = getContentFoundry().render(valueText, TooltipStyle.COLOR_LP).img;
        return TooltipStyle.composePair(labelImg, valueImg);
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
            parts.add(TooltipStyle.composePair(labelImg, valueImg));
        }

        if (rangeValue != null) {
            BufferedImage labelImg = getBodyRegularFoundry().render("Range: ", Color.WHITE).img;
            BufferedImage valueImg = getContentFoundry().render(rangeValue, TooltipStyle.COLOR_FOOD_ENERGY).img;  // #00EEFF
            parts.add(TooltipStyle.composePair(labelImg, valueImg));
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
        return TooltipStyle.composePair(labelImg, valueImg);
    }

    /**
     * Render the Armor penetration line: "Armor penetration: " (regular) + "X%" (semibold pink)
     */
    private static BufferedImage renderArmorPenLine(String value) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Armor penetration: ", Color.WHITE).img;
        BufferedImage valueImg = getContentFoundry().render(value, TooltipStyle.COLOR_MENTAL_WEIGHT).img;  // #FF94E8
        return TooltipStyle.composePair(labelImg, valueImg);
    }

    /**
     * Render the Attack cooldown line: "Attack cooldown: " (regular) + "X%" (semibold cyan)
     */
    private static BufferedImage renderCoolmodLine(String value) {
        BufferedImage labelImg = getBodyRegularFoundry().render("Attack cooldown: ", Color.WHITE).img;
        BufferedImage valueImg = getContentFoundry().render(value, TooltipStyle.COLOR_FOOD_ENERGY).img;  // #00EEFF
        return TooltipStyle.composePair(labelImg, valueImg);
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
    /**
     * Render the gilding chance line: "Gilding chance X% to Y%" with attribute icons.
     * UNIFIED METHOD: Accepts primitives to work with both ISlots and reflection-based extraction.
     * Returns LineResult with text position info for proper spacing.
     */
    private static LineResult renderGildingChanceLine(double pmin, double pmax, Resource[] attrs) {
        int hSpacing = UI.scale(TooltipStyle.HORIZONTAL_SPACING);

        // Build the text: "Gilding chance X% to Y%"
        int pminPercent = (int) Math.round(100 * pmin);
        int pmaxPercent = (int) Math.round(100 * pmax);

        // Render parts
        BufferedImage labelImg = getBodyRegularFoundry().render("Gilding chance ", Color.WHITE).img;
        BufferedImage pminImg = getContentFoundry().render(pminPercent + "%", new Color(192, 192, 255)).img;  // Cyan-ish
        BufferedImage toImg = getBodyRegularFoundry().render(" to ", Color.WHITE).img;
        BufferedImage pmaxImg = getContentFoundry().render(pmaxPercent + "%", new Color(192, 192, 255)).img;  // Cyan-ish

        // Crop text images
        BufferedImage croppedLabel = TooltipStyle.cropTopOnly(labelImg);
        BufferedImage croppedPmin = TooltipStyle.cropTopOnly(pminImg);
        BufferedImage croppedTo = TooltipStyle.cropTopOnly(toImg);
        BufferedImage croppedPmax = TooltipStyle.cropTopOnly(pmaxImg);

        int textHeight = Math.max(Math.max(croppedLabel.getHeight(), croppedPmin.getHeight()),
                                  Math.max(croppedTo.getHeight(), croppedPmax.getHeight()));

        // Calculate total width (text parts + icons)
        int totalWidth = croppedLabel.getWidth() + croppedPmin.getWidth() + croppedTo.getWidth() + croppedPmax.getWidth();

        // Add attribute icons at the end
        java.util.List<BufferedImage> scaledIcons = new java.util.ArrayList<>();
        if (attrs != null && attrs.length > 0) {
            totalWidth += hSpacing;
            for (Resource attr : attrs) {
                try {
                    BufferedImage icon = attr.layer(Resource.imgc).img;
                    BufferedImage scaledIcon = PUtils.convolvedown(icon, new Coord(textHeight, textHeight), CharWnd.iconfilter);
                    scaledIcons.add(scaledIcon);
                    totalWidth += scaledIcon.getWidth() + UI.scale(2);  // 2px between icons
                } catch (Exception ignored) {}
            }
        }

        // Create line image
        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, textHeight));
        Graphics g = result.getGraphics();

        int x = 0;
        // Draw text parts
        g.drawImage(croppedLabel, x, (textHeight - croppedLabel.getHeight()) / 2, null);
        x += croppedLabel.getWidth();
        g.drawImage(croppedPmin, x, (textHeight - croppedPmin.getHeight()) / 2, null);
        x += croppedPmin.getWidth();
        g.drawImage(croppedTo, x, (textHeight - croppedTo.getHeight()) / 2, null);
        x += croppedTo.getWidth();
        g.drawImage(croppedPmax, x, (textHeight - croppedPmax.getHeight()) / 2, null);
        x += croppedPmax.getWidth();

        // Draw attribute icons
        if (!scaledIcons.isEmpty()) {
            x += hSpacing;
            for (BufferedImage icon : scaledIcons) {
                g.drawImage(icon, x, (textHeight - icon.getHeight()) / 2, null);
                x += icon.getWidth() + UI.scale(2);
            }
        }

        g.dispose();
        return new LineResult(result, 0, 0);
    }

    /**
     * Data for a single stat within a gilding item.
     */
    private static class GildingStatData {
        final BufferedImage icon;
        final String name;
        final String formattedValue;

        GildingStatData(BufferedImage icon, String name, String formattedValue) {
            this.icon = icon;
            this.name = name;
            this.formattedValue = formattedValue;
        }
    }

    /**
     * Render the gilding sections: hierarchical display of gilding items and their stats.
     * UNIFIED METHOD: Uses Function extractors to work with both ISlots.SItem and reflection-based access.
     * Each gilding item has a header (icon + name) followed by indented stat lines.
     * Returns LineResult with text position info for proper spacing.
     */
    private static <T> LineResult renderGildingSections(
            java.util.Collection<T> items,
            java.util.function.Function<T, String> nameExtractor,
            java.util.function.Function<T, Resource> resExtractor,
            java.util.function.Function<T, GSprite> sprExtractor,
            java.util.function.Function<T, java.util.List<ItemInfo>> infoExtractor) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        int iconToTextSpacing = UI.scale(TooltipStyle.ICON_TO_TEXT_SPACING);
        int scaledSectionSpacing = UI.scale(TooltipStyle.SECTION_SPACING);  // 10px between gilding groups
        int scaledInternalSpacing = UI.scale(TooltipStyle.GILDING_INTERNAL_SPACING);  // 6px between stat lines
        int bodyDescentVal = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);  // 11px descent
        int statNameDescentVal = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_RESOURCE);  // 9px descent for stat names
        int indent = UI.scale(20);  // Indent for stat lines

        // First pass: calculate max widths across all gildings for alignment
        int maxStatNameWidth = 0;
        int maxStatValueWidth = 0;
        int textHeight = 0;

        java.util.List<java.util.List<GildingStatData>> allGildingStats = new java.util.ArrayList<>();
        for (T item : items) {
            java.util.List<GildingStatData> stats = extractAttrModStats(infoExtractor.apply(item));
            allGildingStats.add(stats);

            for (GildingStatData stat : stats) {
                BufferedImage nameImg = TooltipStyle.cropTopOnly(getGildingStatNameFoundry().render(stat.name, Color.WHITE).img);  // 9px for stat names
                BufferedImage valueImg = TooltipStyle.cropTopOnly(getContentFoundry().render(stat.formattedValue, TooltipStyle.COLOR_STUDY_TIME).img);  // 11px semibold
                maxStatNameWidth = Math.max(maxStatNameWidth, nameImg.getWidth());
                maxStatValueWidth = Math.max(maxStatValueWidth, valueImg.getWidth());
                textHeight = Math.max(textHeight, Math.max(nameImg.getHeight(), valueImg.getHeight()));
            }
        }

        // Calculate header text height
        BufferedImage testHeader = TooltipStyle.cropTopOnly(getBodyRegularFoundry().render("Test", Color.WHITE).img);
        int headerTextHeight = testHeader.getHeight();

        // Icon size matches text height
        int iconSize = textHeight > 0 ? textHeight : headerTextHeight;

        // Calculate total width for stat lines
        int gapBetweenNameAndValue = UI.scale(7);
        int statLineWidth = indent + iconSize + iconToTextSpacing + maxStatNameWidth + gapBetweenNameAndValue + maxStatValueWidth;

        // Second pass: render each gilding section
        java.util.List<LineResult> sectionResults = new java.util.ArrayList<>();

        int sectionIndex = 0;
        for (T item : items) {
            String name = nameExtractor.apply(item);
            Resource res = resExtractor.apply(item);
            GSprite spr = sprExtractor.apply(item);

            java.util.List<GildingStatData> stats = allGildingStats.get(sectionIndex);
            java.util.List<LineResult> sectionLines = new java.util.ArrayList<>();

            // Render header line: icon + name
            BufferedImage headerIcon = null;
            try {
                if (spr instanceof GSprite.ImageSprite) {
                    headerIcon = ((GSprite.ImageSprite) spr).image();
                } else if (res != null) {
                    headerIcon = res.layer(Resource.imgc).img;
                }
            } catch (Exception ignored) {}

            BufferedImage headerNameImg = TooltipStyle.cropTopOnly(getBodyRegularFoundry().render(name, Color.WHITE).img);
            int headerHeight = Math.max(headerNameImg.getHeight(), iconSize);

            BufferedImage scaledHeaderIcon = null;
            if (headerIcon != null) {
                scaledHeaderIcon = PUtils.convolvedown(headerIcon, new Coord(iconSize, iconSize), CharWnd.iconfilter);
            }

            int headerWidth = (scaledHeaderIcon != null ? scaledHeaderIcon.getWidth() + iconToTextSpacing : 0) + headerNameImg.getWidth();
            headerWidth = Math.max(headerWidth, statLineWidth);  // Ensure consistent width

            // Calculate header text position (vertically centered)
            int headerTextY = (headerHeight - headerNameImg.getHeight()) / 2;
            int headerTextTopOffset = headerTextY;
            int headerTextBottomOffset = headerHeight - headerTextY - headerNameImg.getHeight();

            BufferedImage headerLine = TexI.mkbuf(new Coord(headerWidth, headerHeight));
            Graphics hg = headerLine.getGraphics();
            int hx = 0;
            if (scaledHeaderIcon != null) {
                hg.drawImage(scaledHeaderIcon, hx, (headerHeight - scaledHeaderIcon.getHeight()) / 2, null);
                hx += scaledHeaderIcon.getWidth() + iconToTextSpacing;
            }
            hg.drawImage(headerNameImg, hx, headerTextY, null);
            hg.dispose();

            sectionLines.add(new LineResult(headerLine, headerTextTopOffset, headerTextBottomOffset));

            // Render stat lines (indented): 9px for stat name, 11px semibold for value
            // Use proper text offset tracking like tool stats does
            for (GildingStatData stat : stats) {
                BufferedImage statIcon = stat.icon;
                BufferedImage statNameImg = TooltipStyle.cropTopOnly(getGildingStatNameFoundry().render(stat.name, Color.WHITE).img);  // 9px
                // Use red for negative values, green for positive
                Color statValueColor = stat.formattedValue.startsWith("-") ? TooltipStyle.COLOR_NEGATIVE_STAT : TooltipStyle.COLOR_STUDY_TIME;
                BufferedImage statValueImg = TooltipStyle.cropTopOnly(getContentFoundry().render(stat.formattedValue, statValueColor).img);  // 11px semibold

                // Text height is max of name and value
                int statTextHeight = Math.max(statNameImg.getHeight(), statValueImg.getHeight());

                // Scale icon to match text height
                BufferedImage scaledStatIcon = PUtils.convolvedown(statIcon, new Coord(statTextHeight, statTextHeight), CharWnd.iconfilter);

                // Calculate visual text center (excluding descent) for icon positioning
                int statDescent = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);  // Use 11px descent since value is 11px
                int visualTextHeight = statTextHeight - statDescent;
                int visualTextCenter = visualTextHeight / 2;

                // Center icon on visual text center
                int iconYRelative = visualTextCenter - scaledStatIcon.getHeight() / 2;

                // Calculate canvas dimensions
                int canvasHeight = statTextHeight;
                int textY = 0;
                int iconY = iconYRelative;

                // If icon extends above text, expand canvas
                if (iconYRelative < 0) {
                    canvasHeight = statTextHeight - iconYRelative;
                    textY = -iconYRelative;
                    iconY = 0;
                }
                // If icon extends below text, expand canvas
                int iconBottom = iconY + scaledStatIcon.getHeight();
                if (iconBottom > canvasHeight) {
                    canvasHeight = iconBottom;
                }

                BufferedImage statLine = TexI.mkbuf(new Coord(statLineWidth, canvasHeight));
                Graphics sg = statLine.getGraphics();

                int sx = indent;
                sg.drawImage(scaledStatIcon, sx, iconY, null);
                sx += scaledStatIcon.getWidth() + iconToTextSpacing;

                // Draw text at textY, vertically centered within text area
                sg.drawImage(statNameImg, sx, textY + (statTextHeight - statNameImg.getHeight()) / 2, null);

                int valueX = statLineWidth - statValueImg.getWidth();
                sg.drawImage(statValueImg, valueX, textY + (statTextHeight - statValueImg.getHeight()) / 2, null);

                sg.dispose();

                // Track text offsets for baseline-relative spacing
                int textTopOffset = textY;
                int textBottomOffset = canvasHeight - textY - statTextHeight;
                sectionLines.add(new LineResult(statLine, textTopOffset, textBottomOffset));
            }

            // Combine section lines with 7px internal spacing (baseline to text top)
            BufferedImage sectionImage = sectionLines.get(0).image;
            int prevBottomOffset = sectionLines.get(0).textBottomOffset;
            for (int i = 1; i < sectionLines.size(); i++) {
                LineResult current = sectionLines.get(i);
                int spacing = scaledInternalSpacing - bodyDescentVal - prevBottomOffset - current.textTopOffset;
                sectionImage = ItemInfo.catimgs(spacing, sectionImage, current.image);
                prevBottomOffset = current.textBottomOffset;
            }

            sectionResults.add(new LineResult(sectionImage, sectionLines.get(0).textTopOffset, prevBottomOffset));
            sectionIndex++;
        }

        // Combine all sections with 10px spacing between gilding groups
        if (sectionResults.isEmpty()) {
            return null;
        }

        BufferedImage result = sectionResults.get(0).image;
        int firstTextTop = sectionResults.get(0).textTopOffset;
        int lastTextBottom = sectionResults.get(0).textBottomOffset;

        for (int i = 1; i < sectionResults.size(); i++) {
            LineResult current = sectionResults.get(i);
            // Account for previous section's bottom offset and current section's top offset
            int spacing = scaledSectionSpacing - bodyDescentVal - lastTextBottom - current.textTopOffset;
            result = ItemInfo.catimgs(spacing, result, current.image);
            lastTextBottom = current.textBottomOffset;
        }

        return new LineResult(result, firstTextTop, lastTextBottom);
    }

    /**
     * Render base stats section (non-gilding stats): icon + name + right-aligned value.
     */
    private static LineResult renderBaseStatsSection(java.util.List<GildingStatData> stats) {
        if (stats == null || stats.isEmpty()) {
            return null;
        }

        int iconToTextSpacing = UI.scale(TooltipStyle.ICON_TO_TEXT_SPACING);
        int scaledInternalSpacing = UI.scale(TooltipStyle.INTERNAL_SPACING);
        int bodyDescentVal = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);

        // First pass: calculate max widths
        int maxStatNameWidth = 0;
        int maxStatValueWidth = 0;
        int textHeight = 0;

        for (GildingStatData stat : stats) {
            BufferedImage nameImg = TooltipStyle.cropTopOnly(getBodyRegularFoundry().render(stat.name, Color.WHITE).img);
            BufferedImage valueImg = TooltipStyle.cropTopOnly(getContentFoundry().render(stat.formattedValue, TooltipStyle.COLOR_STUDY_TIME).img);
            maxStatNameWidth = Math.max(maxStatNameWidth, nameImg.getWidth());
            maxStatValueWidth = Math.max(maxStatValueWidth, valueImg.getWidth());
            textHeight = Math.max(textHeight, Math.max(nameImg.getHeight(), valueImg.getHeight()));
        }

        int iconSize = textHeight;
        int gapBetweenNameAndValue = UI.scale(7);
        int rightPadding = UI.scale(25);
        int statLineWidth = iconSize + iconToTextSpacing + maxStatNameWidth + gapBetweenNameAndValue + maxStatValueWidth + rightPadding;

        // Render stat lines
        java.util.List<LineResult> statLines = new java.util.ArrayList<>();

        for (GildingStatData stat : stats) {
            BufferedImage statIcon = stat.icon;
            BufferedImage scaledStatIcon = PUtils.convolvedown(statIcon, new Coord(iconSize, iconSize), CharWnd.iconfilter);
            BufferedImage statNameImg = TooltipStyle.cropTopOnly(getBodyRegularFoundry().render(stat.name, Color.WHITE).img);
            // Use red for negative values, green for positive
            Color statValueColor = stat.formattedValue.startsWith("-") ? TooltipStyle.COLOR_NEGATIVE_STAT : TooltipStyle.COLOR_STUDY_TIME;
            BufferedImage statValueImg = TooltipStyle.cropTopOnly(getContentFoundry().render(stat.formattedValue, statValueColor).img);

            int lineHeight = Math.max(Math.max(scaledStatIcon.getHeight(), statNameImg.getHeight()), statValueImg.getHeight());

            BufferedImage statLine = TexI.mkbuf(new Coord(statLineWidth, lineHeight));
            Graphics sg = statLine.getGraphics();

            int sx = 0;
            sg.drawImage(scaledStatIcon, sx, (lineHeight - scaledStatIcon.getHeight()) / 2, null);
            sx += scaledStatIcon.getWidth() + iconToTextSpacing;

            sg.drawImage(statNameImg, sx, (lineHeight - statNameImg.getHeight()) / 2, null);

            int valueX = statLineWidth - statValueImg.getWidth();
            sg.drawImage(statValueImg, valueX, (lineHeight - statValueImg.getHeight()) / 2, null);
            sg.dispose();

            statLines.add(new LineResult(statLine, 0, 0));
        }

        // Combine stat lines with internal spacing
        int baselineSpacing = scaledInternalSpacing - bodyDescentVal;
        java.util.List<BufferedImage> lineImages = new java.util.ArrayList<>();
        for (LineResult lr : statLines) {
            lineImages.add(lr.image);
        }
        BufferedImage combined = ItemInfo.catimgs(baselineSpacing, lineImages.toArray(new BufferedImage[0]));

        return new LineResult(combined, 0, bodyDescentVal);
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
                // Skip ISlots - we render gilding ourselves (both slots.ISlots and slots_alt.ISlots)
                if (tip instanceof ISlots) {
                    continue;
                }
                // Skip slots_alt.ISlots (different class, detected by name)
                String tipClassName = tip.getClass().getSimpleName();
                String tipFullName = tip.getClass().getName();
                if (tipClassName.equals("ISlots") && tipFullName.contains("slots")) {
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
                // Skip DynTex - it may render gilding slots visually and causes extra space
                if (tipClassName.equals("DynTex")) {
                    continue;
                }
                // Skip Armor - we render armor class ourselves
                if (tipClassName.equals("Armor")) {
                    continue;
                }
                // Skip AttrMod at item level - we render base stats ourselves
                if (tipClassName.equals("AttrMod") && tipFullName.contains("attrmod")) {
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
