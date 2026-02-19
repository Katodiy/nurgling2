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

    /** Render value text (Open Sans Semibold, colored) */
    private static BufferedImage value(String text, Color color) {
        return getValueFoundry().render(text, color).img;
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
            label("Energy: "), value(Utils.odformat2(end * 100, 2) + "%", TooltipStyle.COLOR_ENERGY),
            label("  Hunger: "), value(Utils.odformat2(glut * 100, 2) + "%", TooltipStyle.COLOR_HUNGER)));
        l.cmp.add(energyLine, Coord.of(0, l.cmp.sz.y));

        // Line 2: FEP Sum + FEP/Hunger (7px after energy line)
        BufferedImage fepLine = TooltipStyle.cropTopOnly(catimgsh(0,
            label("FEP Sum: "), value(Utils.odformat2(fepSum, 2), TooltipStyle.COLOR_FEP_SUM),
            label("  FEP/Hunger: "), value(Utils.odformat2(fepSum / (100 * glut), 2), TooltipStyle.COLOR_FEP_HUNGER)));
        l.cmp.add(fepLine, Coord.of(0, l.cmp.sz.y + lineSpacing));

        // ===== GROUP 2: Stats (10px gap before, 7px between each stat) =====
        boolean firstStat = true;
        for (int i = 0; i < evs.length; i++) {
            Color col = Utils.blendcol(evs[i].ev.col, Color.WHITE, 0.5);
            BufferedImage line = TooltipStyle.cropTopOnly(catimgsh(0, evs[i].img,
                label(" "),
                value(evs[i].ev.nm, col),
                label("  "),
                value(Utils.odformat2(evs[i].a, 2), col),
                label(" "),
                value("(" + Utils.odformat2(evs[i].a / fepSum * 100, 0) + "%)", TooltipStyle.COLOR_PERCENTAGE)));
            int spacing = firstStat ? groupSpacing : lineSpacing;
            l.cmp.add(line, Coord.of(0, l.cmp.sz.y + spacing));
            firstStat = false;
        }

        // Effects (continue in stats group)
        for (int i = 0; i < efs.length; i++) {
            BufferedImage efi = ItemInfo.longtip(efs[i].info);
            if (efi == null) continue;
            if (efs[i].p != 1) {
                efi = catimgsh(0, efi, label(" "), value("(" + (int) Math.round(efs[i].p * 100) + "% chance)", TooltipStyle.COLOR_PERCENTAGE));
            }
            efi = TooltipStyle.cropTopOnly(efi);
            int spacing = firstStat ? groupSpacing : lineSpacing;
            l.cmp.add(efi, Coord.of(0, l.cmp.sz.y + spacing));
            firstStat = false;
        }

        // ===== GROUP 3: Expected FEP + Expected total (10px gap before, 7px between) =====
        double error = expeted_fep * 0.005;
        String deltaStr = (delta >= 0 ? "+" : "") + String.format("%.2f", delta) + " \u00B1 " + String.format("%.2f", error);
        BufferedImage expectedLine = TooltipStyle.cropTopOnly(catimgsh(0,
            label("Expected FEP: "), value(String.format("%.2f", expeted_fep), TooltipStyle.COLOR_EXPECTED_FEP),
            label(" "), value("(" + deltaStr + ")", TooltipStyle.COLOR_DELTA)));
        l.cmp.add(expectedLine, Coord.of(0, l.cmp.sz.y + groupSpacing));

        // Expected total (7px after expected FEP)
        if (NUtils.getGameUI() != null && NUtils.getGameUI().chrwdg != null && NUtils.getGameUI().chrwdg.battr != null) {
            double cur_fep = 0;
            for (BAttrWnd.FoodMeter.El el : NUtils.getGameUI().chrwdg.battr.feps.els) {
                cur_fep += el.a;
            }
            BufferedImage totalLine = TooltipStyle.cropTopOnly(catimgsh(0,
                label("Expected total: "), value(String.format("%.2f", expeted_fep + cur_fep), TooltipStyle.COLOR_EXPECTED_FEP)));
            l.cmp.add(totalLine, Coord.of(0, l.cmp.sz.y + lineSpacing));
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

                    // Get food type icon from resource image
                    BufferedImage typeIcon = null;
                    Resource.Image img = typeRes.layer(Resource.imgc);
                    if (img != null) {
                        typeIcon = convolvedown(img.img, UI.scale(new Coord(16, 16)), tflt);
                    }

                    // Build line: icon + food type name
                    List<BufferedImage> parts = new ArrayList<>();
                    if (typeIcon != null) {
                        parts.add(typeIcon);
                    }
                    parts.add(value(foodTypeName, new Color(192, 255, 192)));

                    // Add drinks with vessel icons (if dataTables available)
                    if (NUtils.getUI().dataTables != null) {
                        List<String> drinks = NUtils.getUI().dataTables.data_drinks.get(foodTypeName);
                        if (drinks != null && !drinks.isEmpty()) {
                            for (String drink : drinks) {
                                String vessel = NUtils.getUI().dataTables.data_vessel.getOrDefault(drink, "");
                                if (vessel == null) vessel = "Any";
                                String vesselRes = NUtils.getUI().dataTables.vessel_res.get(vessel);
                                if (vesselRes != null) {
                                    BufferedImage vesselIcon = convolvedown(Resource.loadsimg(vesselRes), UI.scale(new Coord(16, 16)), iconfilter);
                                    parts.add(vesselIcon);
                                }
                                parts.add(value(drink, new Color(255, 255, 128)));
                            }
                        }
                    }

                    // Compose line
                    if (!parts.isEmpty()) {
                        BufferedImage line = parts.get(0);
                        for (int i = 1; i < parts.size(); i++) {
                            line = catimgsh(UI.scale(2), line, parts.get(i));
                        }
                        line = TooltipStyle.cropTopOnly(line);
                        int spacing = firstFoodType ? groupSpacing : lineSpacing;
                        l.cmp.add(line, Coord.of(0, l.cmp.sz.y + spacing));
                        firstFoodType = false;
                    }
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
