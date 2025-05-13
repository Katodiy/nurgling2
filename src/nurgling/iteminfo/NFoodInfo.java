package nurgling.iteminfo;

import haven.*;
import static haven.BAttrWnd.Constipations.tflt;
import static haven.CharWnd.iconfilter;
import static haven.PUtils.convolvedown;
import haven.resutil.*;
import nurgling.*;
import nurgling.tools.NSearchItem;
import nurgling.widgets.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

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
            return (((NUtils.getUI().sessInfo.isSubscribed) ? coefSubscribe : (NUtils.getUI().sessInfo.isVerified) ? coefVerif : 1) * fepSum * NUtils.getGameUI().chrwdg.battr.glut.gmod * NUtils.getGameUI().getTableMod() + fepSum * NUtils.getGameUI().chrwdg.battr.glut.gmod * NUtils.getGameUI().getTableMod() * NUtils.getGameUI().getRealmMod()) * efficiency / 100;
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

        if (owner instanceof GItem && NUtils.getGameUI()!=null)
        {
            name = ((NGItem) owner).name();
            if (name == null)
                return;

            NCharacterInfo ci = NUtils.getGameUI().getCharInfo();
            if (ci != null)
            {
                isVarity = !ci.varity.contains(name);
            }
            if (NUtils.getGameUI().chrwdg != null)
            {

                for (int type : types)
                {
                    if(NUtils.getGameUI().chrwdg.battr.cons.els.size()>type) {
                        BAttrWnd.Constipations.El c = NUtils.getGameUI().chrwdg.battr.cons.els.get(type);
                        if (c != null) {
                            efficiency = c.a * 100;
                        }
                    }
                }
            }
            calcData();
        }


        String head = String.format("Energy: $col[128,128,255]{%s%%}, Hunger: $col[255,192,128]{%s\u2030}", Utils.odformat2(end * 100, 2), Utils.odformat2(glut * 1000, 2));
        if(cons != 0)
            head += String.format(", Satiation: $col[192,192,128]{%s%%}", Utils.odformat2(cons * 100, 2));
        l.cmp.add(RichText.render(head, 0).img, Coord.of(0, l.cmp.sz.y));
        l.cmp.add(RichText.render(String.format("FEP Sum: $col[128,255,0]{%s}, FEP/Hunger: $col[128,255,128]{%s}", Utils.odformat2(fepSum, 2), Utils.odformat2(fepSum / (100 * glut), 2)), 0).img,Coord.of(0, l.cmp.sz.y));

        for(int i = 0; i < evs.length; i++) {
            Color col = Utils.blendcol(evs[i].ev.col, Color.WHITE, 0.5);
            l.cmp.add(catimgsh(5, evs[i].img, RichText.render(String.format("%s: %s{%s} (%s%%)", evs[i].ev.nm, RichText.Parser.col2a(col), Utils.odformat2(evs[i].a, 2), Utils.odformat2(evs[i].a/fepSum*100, 0)), 0).img),
                    Coord.of(UI.scale(5), l.cmp.sz.y));
        }
        if(sev > 0)
            l.cmp.add(RichText.render(String.format("Total: $col[128,192,255]{%s} ($col[128,192,255]{%s}/\u2030 hunger)", Utils.odformat2(sev, 2), Utils.odformat2(sev / (1000 * glut), 2)), 0).img,
                    Coord.of(UI.scale(5), l.cmp.sz.y));
        for(int i = 0; i < efs.length; i++) {
            BufferedImage efi = ItemInfo.longtip(efs[i].info);
            if(efs[i].p != 1)
                efi = catimgsh(5, efi, RichText.render(String.format("$i{($col[192,192,255]{%d%%} chance)}", (int)Math.round(efs[i].p * 100)), 0).img);
            l.cmp.add(efi, Coord.of(UI.scale(5), l.cmp.sz.y));
        }


        l.cmp.add(RichText.render(String.format("$col[205,125,255]{%s}:", "Calculation"), 0).img,Coord.of(0, l.cmp.sz.y));


        double error = expeted_fep * 0.005;
        if (delta < 0)
            l.cmp.add(RichText.render(String.format("Expected FEP: $col[128,255,0]{%.2f} $col[0,196,255]{(%.2f \u00B1 %.2f)}", expeted_fep, delta, error), 0).img,Coord.of(UI.scale(5), l.cmp.sz.y));
        else
            l.cmp.add(RichText.render(String.format("Expected FEP: $col[128,255,0]{%.2f} $col[255,0,0]{(+%.2f \u00B1 %.2f)} ", expeted_fep, delta, error), 0).img,Coord.of(UI.scale(5), l.cmp.sz.y));
        double cur_fep = 0;
        for (BAttrWnd.FoodMeter.El el : NUtils.getGameUI().chrwdg.battr.feps.els)
        {
            cur_fep += el.a;
        }
        l.cmp.add(RichText.render(String.format("Expected total: $col[128,255,0]{%.2f}", expeted_fep + cur_fep), 0).img,Coord.of(UI.scale(5), l.cmp.sz.y));

        if (NUtils.getUI().dataTables.data_food != null && NUtils.getUI().dataTables.data_food.containsKey(name))
        {
            drinkImg = drinkImg();
            if (drinkImg!= null && !drinkImg.isEmpty())
            {
                l.cmp.add(RichText.render(String.format("$col[175,175,255]{%s}:", "Drink info"), 0).img,Coord.of(0, l.cmp.sz.y));

                for(BufferedImage cand : drinkImg)
                {
                    l.cmp.add(cand,Coord.of(UI.scale(5), l.cmp.sz.y));
                }
            }
        }
    }

    ArrayList<BufferedImage> drinkImg = null;

    private ArrayList<BufferedImage> drinkImg()
    {
        if (drinkImg == null && NUtils.getUI().dataTables.data_food.get(name) != null)
        {
            drinkImg = new ArrayList<>();
            for (String type : NUtils.getUI().dataTables.data_food.get(name))
            {
                if (NUtils.getUI().dataTables.data_drinks.get(type) != null)
                {
                    Iterator<String> iter = NUtils.getUI().dataTables.data_drinks.get(type).iterator();
                    BufferedImage img = null;
                    while (iter.hasNext())
                    {
                        String drink = iter.next();
                        String vessel = (NUtils.getUI().dataTables.data_vessel.getOrDefault(drink, ""));
                        if (vessel == null) vessel = "Any";
                        img = RichText.render(String.format("%s$col[192,255,192]{%s}:", "\t", type), 0).img;
                        img = catimgsh(5, img, RichText.render(String.format("$col[255,255,128]{%s} (%s)", drink, vessel), 0).img);
                        img = catimgsh(5, img, convolvedown(Resource.loadsimg(NUtils.getUI().dataTables.vessel_res.get(vessel)), UI.scale(new Coord(16, 16)), iconfilter));
                        drinkImg.add(img);
                    }
                }
            }
        }
        return drinkImg;
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
