package nurgling.iteminfo;

import haven.*;
import static haven.BAttrWnd.Constipations.tflt;
import static haven.CharWnd.iconfilter;
import static haven.PUtils.convolvedown;
import haven.resutil.*;
import nurgling.*;
import nurgling.styles.TooltipStyle;
import nurgling.tools.NSearchItem;
import nurgling.widgets.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

public class NFoodInfo extends FoodInfo  implements GItem.OverlayInfo<Tex>, NSearchable
{
    public NFoodInfo(Owner owner, double end, double glut, double sev, Event[] evs, Effect[] efs, int[] types)
    {
        this(owner, end, glut, sev, 0, evs, efs, types);
    }

    public static boolean show;

    static double coefSubscribe = 1.5;
    static double coefVerif = 1.2;
    static double coefVar = 0.3999;
    String name;
    String s_end;
    String s_glut;
    String s_end_glut;
    public double fepSum = 0;
    double efficiency = 100;
    boolean isVarity;
    double expeted_fep;
    double needed;
    public boolean needToolTip = false;
    double delta = 0;
    HashMap<String, Double> searchImage = new HashMap<>();

    double energy;

    // Cached foundries for Open Sans tooltip rendering
    private static Text.Foundry labelFoundry = null;
    private static Text.Foundry valueFoundry = null;

    private static Text.Foundry getLabelFoundry() {
        if (labelFoundry == null) {
            labelFoundry = TooltipStyle.createFoundry(false, TooltipStyle.FONT_SIZE_BODY, Color.WHITE);
        }
        return labelFoundry;
    }

    private static Text.Foundry getValueFoundry() {
        if (valueFoundry == null) {
            valueFoundry = TooltipStyle.createFoundry(true, TooltipStyle.FONT_SIZE_BODY, Color.WHITE);
        }
        return valueFoundry;
    }

    /** Render label text (Open Sans Regular, white) */
    private static BufferedImage label(String text) {
        return getLabelFoundry().render(text, Color.WHITE).img;
    }

    /** Render label text with custom color (Open Sans Regular, colored) */
    private static BufferedImage labelColored(String text, Color color) {
        return getLabelFoundry().render(text, color).img;
    }

    /** Render value text (Open Sans Semibold, colored) */
    private static BufferedImage value(String text, Color color) {
        return getValueFoundry().render(text, color).img;
    }

    /**
     * Result of composing elements - contains image and text positioning info.
     */
    private static class IconLineResult {
        final BufferedImage image;
        final int textTopOffset;     // Pixels from image top to text top
        final int textBottomOffset;  // Pixels from text bottom to image bottom

        IconLineResult(BufferedImage image, int textTopOffset, int textBottomOffset) {
            this.image = image;
            this.textTopOffset = textTopOffset;
            this.textBottomOffset = textBottomOffset;
        }
    }

    /**
     * Element for composing lines with mixed text and icons.
     */
    private static class LineElement {
        final BufferedImage image;
        final boolean isIcon;

        LineElement(BufferedImage image, boolean isIcon) {
            this.image = image;
            this.isIcon = isIcon;
        }

        static LineElement text(BufferedImage img) {
            return new LineElement(img, false);
        }

        static LineElement icon(BufferedImage img) {
            return new LineElement(img, true);
        }
    }

    /**
     * Compose multiple elements (text and icons) horizontally.
     * TEXT elements define the line height - icons are centered vertically.
     * Returns the composed image and text top offset for spacing calculations.
     */
    private static IconLineResult composeElements(int gap, List<LineElement> elements) {
        if (elements.isEmpty()) {
            return new IconLineResult(TexI.mkbuf(new Coord(1, 1)), 0, 0);
        }

        // Get font descent - text images include descent below baseline
        // We need to account for this when centering to align visual text center with icon center
        int descent = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);

        // First pass: find max text height (only from non-icon elements)
        int maxTextHeight = 0;
        for (LineElement elem : elements) {
            if (!elem.isIcon) {
                maxTextHeight = Math.max(maxTextHeight, elem.image.getHeight());
            }
        }

        // If no text elements, fall back to max of all heights
        if (maxTextHeight == 0) {
            for (LineElement elem : elements) {
                maxTextHeight = Math.max(maxTextHeight, elem.image.getHeight());
            }
        }

        // Find max icon height to determine total line height
        int maxIconHeight = 0;
        for (LineElement elem : elements) {
            if (elem.isIcon) {
                maxIconHeight = Math.max(maxIconHeight, elem.image.getHeight());
            }
        }

        // Total height: text height + any icon extension above/below
        int iconExtension = Math.max(0, (maxIconHeight - maxTextHeight) / 2);
        int totalHeight = maxTextHeight + iconExtension * 2;

        // Calculate actual text Y position (accounting for descent shift for visual alignment)
        // Text is centered then shifted down by descent/2
        int textTopOffset = (totalHeight - maxTextHeight) / 2 + descent / 2;
        // Text bottom offset is NOT the same due to descent shift
        int textBottomOffset = totalHeight - textTopOffset - maxTextHeight;

        // Calculate total width
        int totalWidth = 0;
        for (int i = 0; i < elements.size(); i++) {
            totalWidth += elements.get(i).image.getWidth();
            if (i > 0) totalWidth += gap;
        }

        // Create result image
        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, totalHeight));
        Graphics g = result.getGraphics();

        int x = 0;
        for (int i = 0; i < elements.size(); i++) {
            LineElement elem = elements.get(i);
            int y;
            if (elem.isIcon) {
                // Center icon vertically
                y = (totalHeight - elem.image.getHeight()) / 2;
            } else {
                // Center text, but adjust for descent so visual text center aligns with icon center
                // Text visual center is at (height - descent) / 2 from top, not height / 2
                // So we shift text DOWN by descent / 2 to compensate
                y = (totalHeight - elem.image.getHeight()) / 2 + descent / 2;
            }

            g.drawImage(elem.image, x, y, null);
            x += elem.image.getWidth();
            if (i < elements.size() - 1) x += gap;
        }

        g.dispose();

        return new IconLineResult(result, textTopOffset, textBottomOffset);
    }

    /**
     * Compose a single icon with a text line.
     * Convenience wrapper for composeElements with just icon + text.
     */
    private static IconLineResult composeIconLine(BufferedImage icon, BufferedImage textLine) {
        List<LineElement> elements = new ArrayList<>();
        elements.add(LineElement.icon(icon));
        elements.add(LineElement.text(textLine));
        return composeElements(UI.scale(2), elements);
    }


    /**
     * Scale an icon to the standard tooltip icon size (75% of 16px = 12px).
     */
    private static BufferedImage scaleIcon(BufferedImage icon) {
        if (icon == null) return null;
        return convolvedown(icon, UI.scale(new Coord(TooltipStyle.ICON_SIZE, TooltipStyle.ICON_SIZE)), iconfilter);
    }

    public NFoodInfo(Owner owner, double end, double glut, double sev, double cons, Event[] evs, Effect[] efs, int[] types)
    {
        super(owner, end, glut, sev, cons, evs, efs, types);
        s_end = Utils.odformat2(end * 100, 2);
        this.energy = end;
        s_glut = Utils.odformat2(glut * 100, 2);
        s_end_glut = Utils.odformat2(end / glut, 2);
        for (Event event : evs)
        {
            fepSum += event.a;
        }
        for (Event ev : evs)
        {
            double probability = ev.a / fepSum;
            fepVis.add(new FepVis(ev.img, ev.ev.nm, String.format("%s (%s%%)", Utils.odformat2(ev.a, 2), Utils.odformat2(probability * 100, 2)), Utils.blendcol(ev.ev.col, Color.WHITE, 0.5)));
            searchImage.put(fep_map.get(ev.ev.nm), probability * 100);
        }
    }

    double calcExpectedFep()
    {
        if (NUtils.getGameUI().chrwdg != null && NUtils.getUI().sessInfo != null && NUtils.getGameUI().chrwdg.battr != null)
        {
            boolean isSubscribed = NUtils.getUI().sessInfo.isSubscribed;
            boolean isVerified = NUtils.getUI().sessInfo.isVerified;
            return (((isSubscribed) ? coefSubscribe : (isVerified) ? coefVerif : 1) * fepSum * NUtils.getGameUI().chrwdg.battr.glut.gmod * NUtils.getGameUI().getTableMod() + fepSum * NUtils.getGameUI().chrwdg.battr.glut.gmod * NUtils.getGameUI().getTableMod() * NUtils.getGameUI().getRealmMod()) * efficiency / 100;
        }
        return 0;
    }

    double calcNeededFep()
    {
        if (nurgling.NUtils.getGameUI().chrwdg != null && NUtils.getGameUI().chrwdg.battr!=null)
        {
            double cur_fep = 0;
            for (BAttrWnd.FoodMeter.El el : NUtils.getGameUI().chrwdg.battr.feps.els)
            {
                cur_fep += el.a;
            }
            if (isVarity)
            {
                return (NUtils.getGameUI().chrwdg.battr.feps.cap - Math.sqrt(coefVar * NUtils.getGameUI().getMaxBase() * NUtils.getGameUI().chrwdg.battr.glut.gmod / (NUtils.getGameUI().getCharInfo().varity.size() + 1)) - cur_fep);
            }
            else
            {
                return NUtils.getGameUI().chrwdg.battr.feps.cap - cur_fep;
            }
        }
        return 0;
    }

    public boolean check()
    {
        if (NUtils.getGameUI().chrwdg != null && NUtils.getGameUI().chrwdg.battr!=null)
        {

            NCharacterInfo ci = NUtils.getGameUI().getCharInfo();
            if (ci != null)
            {
                if (name == null)
                {
                    name = ((NGItem) owner).name();
                }
                boolean res = !(isVarity == !ci.varity.contains(name));
                if (res)
                {
                    needToolTip = true;
                    return true;
                }
            }
            if (!NUtils.getGameUI().chrwdg.battr.cons.els.isEmpty())
            {
                for (int type : types)
                {
                    if(NUtils.getGameUI().chrwdg.battr.cons.els.size()>type) {
                        BAttrWnd.Constipations.El c = NUtils.getGameUI().chrwdg.battr.cons.els.get(type);
                        if (efficiency != ((c != null) ? Math.min(100, c.a * 100) : 100)) {
                            needToolTip = true;
                            return true;
                        }
                    }
                }
            }
            if (expeted_fep != calcExpectedFep() || needed != calcNeededFep())
            {
                expeted_fep = calcExpectedFep();
                needed = calcNeededFep();
                needToolTip = true;
            }
        }
        return false;
    }

    final static HashMap<String, String> fep_map = new HashMap<>();

    static
    {
        synchronized (fep_map)
        {
            fep_map.put("Strength +2", "str2");
            fep_map.put("Strength +1", "str");
            fep_map.put("Agility +2", "agi2");
            fep_map.put("Agility +1", "agi");
            fep_map.put("Constitution +1", "con");
            fep_map.put("Constitution +2", "con2");
            fep_map.put("Perception +2", "per2");
            fep_map.put("Perception +1", "per");
            fep_map.put("Will +1", "wil");
            fep_map.put("Will +2", "wil2");
            fep_map.put("Psyche +1", "psy");
            fep_map.put("Psyche +2", "psy2");
            fep_map.put("Intelligence +1", "int");
            fep_map.put("Intelligence +2", "int2");
            fep_map.put("Dexterity +1", "dex");
            fep_map.put("Dexterity +2", "dex2");
            fep_map.put("Charisma +1", "csm");
            fep_map.put("Charisma +2", "csm");
        }
    }


    @Override
    public boolean search()
    {
        if(name!=null) {
            calcData();
            NSearchItem si = NUtils.getGameUI().itemsForSearch;
            if(!si.food.isEmpty()) {
                for (NSearchItem.Stat fep : NUtils.getGameUI().itemsForSearch.food) {
                    if (searchImage.get(fep.v) == null || (fep.a!=0 && !(fep.isMore == (searchImage.get(fep.v) > fep.a))))
                        return false;
                }
                if (si.fgs)
                    return (delta > 0);
                if(!NUtils.getGameUI().itemsForSearch.name.isEmpty())
                {
                    return name.toLowerCase().contains(NUtils.getGameUI().itemsForSearch.name.toLowerCase());
                }
                return true;
            }
            if (si.fgs)
                return (delta > 0);
        }
        return false;
    }

    public double energy() {
        return energy;
    }

    class FepVis
    {
        BufferedImage img;
        String nm;
        String str;
        Color col;

        public FepVis(BufferedImage img, String nm, String str, Color col)
        {
            this.img = img;
            this.nm = nm;
            this.str = str;
            this.col = col;
        }
    }

    ArrayList<FepVis> fepVis = new ArrayList<>();


    BufferedImage headImg = null;

    public BufferedImage headImg()
    {
        if (headImg == null)
        {
            String head = String.format("Energy: $col[128,128,255]{%s%%}, Hunger: $col[255,192,128]{%s%%}, Energy/Hunger: $col[128,128,255]{%s%%}", s_end, s_glut, s_end_glut);

            headImg = RichText.render(head, 0).img;
        }
        return headImg;
    }

    ArrayList<BufferedImage> fepImgs = null;

    public ArrayList<BufferedImage> fepImg()
    {
        if (fepImgs == null)
        {
            fepImgs = new ArrayList<>();
            for (FepVis value : fepVis)
            {
                fepImgs.add(catimgsh(5, value.img, RichText.render(String.format("%s: $col[%d,%d,%d]{%s}", value.nm, value.col.getRed(), value.col.getGreen(), value.col.getBlue(), value.str), 0).img));
            }
        }
        return fepImgs;
    }

    ArrayList<BufferedImage> effImgs = null;

    public ArrayList<BufferedImage> effImg()
    {
        if (effImgs == null)
        {
            effImgs = new ArrayList<>();
            for (Effect ef : efs)
            {
                BufferedImage efi = ItemInfo.longtip(ef.info);
                if (ef.p != 1)
                    efi = catimgsh(5, efi, RichText.render(String.format("$i{($col[192,192,255]{%d%%} chance)}", (int) Math.round(ef.p * 100)), 0).img);
                effImgs.add(efi);
            }
        }
        return effImgs;
    }

    BufferedImage extentTitle = null;

    public BufferedImage extentTitle()
    {
        if (extentTitle == null)
        {
            extentTitle = RichText.render(String.format("$col[0,255,255]{%s}:", "Extended info"), 0).img;
        }
        return extentTitle;
    }

    HashMap<Integer, BufferedImage> consImgs = new HashMap<>();

    BufferedImage getConsImg(int value)
    {
        if (consImgs.get(value) == null)
        {
            BAttrWnd.Constipations.El c = NUtils.getGameUI().chrwdg.battr.cons.els.get(value);
            if (c != null)
                consImgs.put(value, convolvedown(new ItemSpec(OwnerContext.uictx.curry(NUtils.getUI()), c.t, null).image(), new Coord(UI.scale(16), UI.scale(16)), tflt));
        }
        return consImgs.get(value);
    }

    void calcData()
    {
        if (name != null)
        {
            expeted_fep = calcExpectedFep();
            needed = calcNeededFep();
            delta = expeted_fep - needed;
        }
    }
    @Override
    public void layout(Layout l) {
        if (owner instanceof GItem && NUtils.getGameUI() != null) {
            name = ((NGItem) owner).name();
            if (name == null)
                return;

            NCharacterInfo ci = NUtils.getGameUI().getCharInfo();
            if (ci != null) {
                isVarity = !ci.varity.contains(name);
            }
            if (NUtils.getGameUI().chrwdg != null) {
                for (int type : types) {
                    if (NUtils.getGameUI().chrwdg.battr.cons.els.size() > type) {
                        BAttrWnd.Constipations.El c = NUtils.getGameUI().chrwdg.battr.cons.els.get(type);
                        if (c != null) {
                            efficiency = c.a * 100;
                        }
                    }
                }
            }
            calcData();
        }

        // Get font descent for baseline-relative spacing (like NCuriosity)
        int descent = TooltipStyle.getFontDescent(TooltipStyle.FONT_SIZE_BODY);
        int groupSpacing = UI.scale(TooltipStyle.SECTION_SPACING) - descent;  // 10px between groups
        int lineSpacing = UI.scale(TooltipStyle.INTERNAL_SPACING) - descent;  // 7px within groups

        // ===== GROUP 1: Energy + FEP Sum =====
        // Line 1: Energy + Hunger
        BufferedImage energyLine = TooltipStyle.cropTopOnly(catimgsh(0,
            label("Energy: "), value(Utils.odformat2(end * 100, 2) + "%", TooltipStyle.COLOR_FOOD_ENERGY),
            label("  Hunger: "), value(Utils.odformat2(glut * 100, 2) + "%", TooltipStyle.COLOR_FOOD_HUNGER)));

        // If there's already content in the layout (e.g., curio info), add section spacing before food info
        int yPos = l.cmp.sz.y;
        if (yPos > 0) {
            yPos += groupSpacing;  // Add 10px section spacing between curio and food info
        }
        l.cmp.add(energyLine, Coord.of(0, yPos));

        // Line 2: FEP Sum + FEP/Hunger (7px after energy line)
        BufferedImage fepLine = TooltipStyle.cropTopOnly(catimgsh(0,
            label("FEP Sum: "), value(Utils.odformat2(fepSum, 2), TooltipStyle.COLOR_FOOD_FEP_SUM),
            label("  FEP/Hunger: "), value(Utils.odformat2(fepSum / (100 * glut), 2), TooltipStyle.COLOR_FOOD_FEP_HUNGER)));
        l.cmp.add(fepLine, Coord.of(0, l.cmp.sz.y + lineSpacing));

        // ===== GROUP 2: Stats (10px gap before, 7px between each stat) =====
        // Track previous line's text bottom offset for proper spacing
        int prevTextBottomOffset = 0;  // For text-only lines, this is 0

        // First pass: calculate column widths for tabular alignment
        int maxNameWidth = 0;
        int maxValueWidth = 0;
        int columnGap = UI.scale(TooltipStyle.HORIZONTAL_SPACING);

        for (int i = 0; i < evs.length; i++) {
            BufferedImage nameImg = label(evs[i].ev.nm);
            BufferedImage valueImg = value(Utils.odformat2(evs[i].a, 2), Color.WHITE);
            maxNameWidth = Math.max(maxNameWidth, nameImg.getWidth());
            maxValueWidth = Math.max(maxValueWidth, valueImg.getWidth());
        }

        // Second pass: render stats with aligned columns
        boolean firstStat = true;
        for (int i = 0; i < evs.length; i++) {
            Color col = Utils.blendcol(evs[i].ev.col, Color.WHITE, 0.5);

            // Render each part separately
            BufferedImage nameImg = label(evs[i].ev.nm);
            BufferedImage valueImg = value(Utils.odformat2(evs[i].a, 2), col);
            BufferedImage pctImg = labelColored("(" + Utils.odformat2(evs[i].a / fepSum * 100, 0) + "%)", TooltipStyle.COLOR_PERCENTAGE);

            // Calculate positions for tabular layout
            // Name is left-aligned, value is right-aligned within its column, percentage follows
            int nameColWidth = maxNameWidth;
            int valueColWidth = maxValueWidth;

            int totalWidth = nameColWidth + columnGap + valueColWidth + columnGap + pctImg.getWidth();
            int maxHeight = Math.max(Math.max(nameImg.getHeight(), valueImg.getHeight()), pctImg.getHeight());

            BufferedImage textPart = TexI.mkbuf(new Coord(totalWidth, maxHeight));
            Graphics g = textPart.getGraphics();

            // Draw name (left-aligned in column)
            int x = 0;
            g.drawImage(nameImg, x, (maxHeight - nameImg.getHeight()) / 2, null);

            // Draw value (right-aligned in column)
            x = nameColWidth + columnGap + (valueColWidth - valueImg.getWidth());
            g.drawImage(valueImg, x, (maxHeight - valueImg.getHeight()) / 2, null);

            // Draw percentage (after value column)
            x = nameColWidth + columnGap + valueColWidth + columnGap;
            g.drawImage(pctImg, x, (maxHeight - pctImg.getHeight()) / 2, null);

            g.dispose();

            textPart = TooltipStyle.cropTopOnly(textPart);

            // Scale stat icon and compose with text
            BufferedImage scaledIcon = scaleIcon(evs[i].img);
            IconLineResult result = composeIconLine(scaledIcon, textPart);

            // Adjust spacing: subtract current line's textTopOffset and previous line's textBottomOffset
            int baseSpacing = firstStat ? groupSpacing : lineSpacing;
            int adjustedSpacing = baseSpacing - result.textTopOffset - prevTextBottomOffset;

            l.cmp.add(result.image, Coord.of(0, l.cmp.sz.y + adjustedSpacing));

            // Use text bottom offset for next iteration (not same as top due to descent shift)
            prevTextBottomOffset = result.textBottomOffset;
            firstStat = false;
        }

        // Effects (continue in stats group) - these are text-only lines
        for (int i = 0; i < efs.length; i++) {
            BufferedImage efi = ItemInfo.longtip(efs[i].info);
            if (efi == null) continue;
            if (efs[i].p != 1) {
                efi = catimgsh(0, efi, label(" "), labelColored("(" + (int) Math.round(efs[i].p * 100) + "% chance)", TooltipStyle.COLOR_PERCENTAGE));
            }
            efi = TooltipStyle.cropTopOnly(efi);
            int baseSpacing = firstStat ? groupSpacing : lineSpacing;
            // Adjust for previous icon line's text bottom offset
            int adjustedSpacing = baseSpacing - prevTextBottomOffset;
            l.cmp.add(efi, Coord.of(0, l.cmp.sz.y + adjustedSpacing));
            prevTextBottomOffset = 0;  // Text-only line has no offset
            firstStat = false;
        }

        // ===== GROUP 3: Expected FEP + Expected total (10px gap before, 7px between) =====
        double error = expeted_fep * 0.005;
        String deltaStr = (delta >= 0 ? "+" : "") + String.format("%.2f", delta) + " \u00B1 " + String.format("%.2f", error);
        BufferedImage expectedLine = TooltipStyle.cropTopOnly(catimgsh(0,
            label("Expected FEP: "), value(String.format("%.2f", expeted_fep), TooltipStyle.COLOR_FOOD_FEP_HUNGER),
            label(" "), labelColored("(" + deltaStr + ")", TooltipStyle.COLOR_PERCENTAGE)));
        // Adjust for previous icon line's text bottom offset
        int expectedSpacing = groupSpacing - prevTextBottomOffset;
        l.cmp.add(expectedLine, Coord.of(0, l.cmp.sz.y + expectedSpacing));
        prevTextBottomOffset = 0;  // Reset for text-only line

        // Expected total (7px after expected FEP)
        if (NUtils.getGameUI() != null && NUtils.getGameUI().chrwdg != null && NUtils.getGameUI().chrwdg.battr != null) {
            double cur_fep = 0;
            for (BAttrWnd.FoodMeter.El el : NUtils.getGameUI().chrwdg.battr.feps.els) {
                cur_fep += el.a;
            }
            BufferedImage totalLine = TooltipStyle.cropTopOnly(catimgsh(0,
                label("Expected total: "), value(String.format("%.2f", expeted_fep + cur_fep), TooltipStyle.COLOR_FOOD_FEP_SUM)));
            l.cmp.add(totalLine, Coord.of(0, l.cmp.sz.y + lineSpacing));
            // prevTextBottomOffset stays 0 for text-only line
        }

        // ===== GROUP 4: Food types with icons (10px gap before, 7px between each type) =====
        // Find FoodTypes ItemInfo and extract types using reflection
        if (owner instanceof GItem && NUtils.getUI() != null) {

            Resource[] foodTypeResources = null;

            // Find FoodTypes in item info list and extract types via reflection
            try {
                List<ItemInfo> infos = ((GItem) owner).info();
                for (ItemInfo info : infos) {
                    if (info.getClass().getName().contains("FoodTypes")) {
                        // types field is Resource[] not int[]
                        java.lang.reflect.Field typesField = info.getClass().getDeclaredField("types");
                        typesField.setAccessible(true);
                        foodTypeResources = (Resource[]) typesField.get(info);
                        break;
                    }
                }
            } catch (Exception ignored) {}

            if (foodTypeResources != null && foodTypeResources.length > 0) {
                boolean firstFoodType = true;
                for (Resource typeRes : foodTypeResources) {
                    if (typeRes == null) continue;

                    // Get food type name from resource tooltip
                    String foodTypeName = null;
                    Resource.Tooltip tt = typeRes.layer(Resource.tooltip);
                    if (tt != null) {
                        foodTypeName = tt.t;
                    }
                    if (foodTypeName == null) continue;

                    // Get food type icon from resource image (scaled to 80%)
                    BufferedImage typeIcon = null;
                    Resource.Image img = typeRes.layer(Resource.imgc);
                    if (img != null) {
                        typeIcon = convolvedown(img.img, UI.scale(new Coord(TooltipStyle.ICON_SIZE, TooltipStyle.ICON_SIZE)), tflt);
                    }

                    // Build elements list with proper icon/text marking
                    List<LineElement> elements = new ArrayList<>();

                    // Add food type icon
                    if (typeIcon != null) {
                        elements.add(LineElement.icon(typeIcon));
                    }

                    // Add food type name (text, cropped)
                    elements.add(LineElement.text(TooltipStyle.cropTopOnly(value(foodTypeName, TooltipStyle.COLOR_FOOD_TYPE))));

                    // Add drinks with vessel icons (if dataTables available)
                    if (NUtils.getUI().dataTables != null) {
                        List<String> drinks = NUtils.getUI().dataTables.data_drinks.get(foodTypeName);
                        if (drinks != null && !drinks.isEmpty()) {
                            for (String drink : drinks) {
                                String vessel = NUtils.getUI().dataTables.data_vessel.getOrDefault(drink, "");
                                if (vessel == null) vessel = "Any";
                                String vesselRes = NUtils.getUI().dataTables.vessel_res.get(vessel);
                                if (vesselRes != null) {
                                    BufferedImage vesselIcon = convolvedown(Resource.loadsimg(vesselRes), UI.scale(new Coord(TooltipStyle.ICON_SIZE, TooltipStyle.ICON_SIZE)), iconfilter);
                                    elements.add(LineElement.icon(vesselIcon));
                                }
                                elements.add(LineElement.text(TooltipStyle.cropTopOnly(value(drink, TooltipStyle.COLOR_FOOD_VESSEL))));
                            }
                        }
                    }

                    // Compose all elements - TEXT defines height, icons centered
                    IconLineResult result = composeElements(UI.scale(2), elements);

                    int baseSpacing = firstFoodType ? groupSpacing : lineSpacing;
                    int adjustedSpacing = baseSpacing - result.textTopOffset - prevTextBottomOffset;
                    l.cmp.add(result.image, Coord.of(0, l.cmp.sz.y + adjustedSpacing));
                    prevTextBottomOffset = result.textBottomOffset;

                    firstFoodType = false;
                }
            }
        }
    }

    public static Tex var_img = Resource.loadtex("nurgling/hud/items/overlays/varity");

    @Override
    public Tex overlay()
    {
        return var_img;
    }

    @Override
    public void drawoverlay(GOut g, Tex data)
    {
        if (show && isVarity)
        {
             g.aimage(data, data.sz(), 1, 1);
        }
    }

    @Override
    public boolean tick(double dt)
    {
        if(NUtils.getGameUI()==null)
            return false;
        NCharacterInfo ci = NUtils.getGameUI().getCharInfo();
        if (ci != null)
        {
            name = ((NGItem) owner).name();
            if (name != null)
            {
                isVarity = !ci.varity.contains(name);
            }
        }
        return check();
    }
}
