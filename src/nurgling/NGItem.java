package nurgling;

import haven.*;
import static haven.Inventory.sqsz;

import haven.res.ui.tt.slots.ISlots;
import haven.res.ui.tt.stackn.StackName;
import monitoring.ItemWatcher;
import nurgling.actions.ReadJsonAction;
import nurgling.iteminfo.NCuriosity;
import nurgling.iteminfo.NFoodInfo;
import nurgling.iteminfo.NQuestItem;
import nurgling.tools.VSpec;
import nurgling.widgets.NQuestInfo;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NGItem extends GItem
{
    public boolean isSearched = false;
    public boolean isQuested = false;
    int lastQuestUpdate = 0;
    String name = null;
    public Float quality = null;
    public long meterUpdated = 0;
    public int hardArmor = 0;
    public int softArmor = 0;

    boolean sent = false;
    public NGItem(Indir<Resource> res, Message sdt)
    {
        super(res, sdt);
    }

    public NGItem(Indir<Resource> res)
    {
        super(res);
    }

    public String name()
    {
        return name;
    }

    public boolean needlongtip()
    {
        for (ItemInfo inf : info()) {
            if (inf instanceof NFoodInfo) {
                return ((NFoodInfo) inf).needToolTip;
            }
            else if (inf instanceof NCuriosity) {
                return ((NCuriosity) inf).needUpdate();
            }
            if (inf instanceof ISlots) {
                return this.ui.modshift!=((ISlots)inf).isShifted;
            }
        }
        return false;
    }

    public ArrayList<NContent> content(){
        return content;
    }

    public boolean findContent(String item) {
        for(NGItem.NContent content : content())
        {
            if(content.name().endsWith(item))
            {
                return true;
            }

        }
        return false;
    }

    public static class NContent
    {
        private double quality = -1;
        private String name = null;

        public NContent(double quality, String name)
        {
            this.quality = quality;
            this.name = name;
        }

        public double quality()
        {
            return quality;
        }

        public String name()
        {
            return name;
        }

        public String type() {
            String regex = "of\\s+([A-Za-z]+)\\s*";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(name);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        }

    }

    private ArrayList<NContent> content = new ArrayList<>();

    public Coord sprsz()
    {
        if (spr != null)
        {
            return spr.sz().div(new Coord(sqsz.x - UI.scale(1), sqsz.y - UI.scale(1)));
        }
        return null;
    }

    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        if (name == null && spr != null)
        {
            if (!res.get().name.contains("coin"))
            {
                if (res.get() != null)
                {
                    name = ItemInfo.Name.Default.get(this);
                }
            }
            else
            {
                name = StackName.getname(this);
            }
            if(name!=null)
            {
                if(NUtils.getGameUI().map.clickedGob!=null)
                {
                    VSpec.checkLpExplorer(NUtils.getGameUI().map.clickedGob.gob, name);
                }
            }

        }
        if(name!= null) {
            if (!sent && info != null && getInfo(NFoodInfo.class) != null) {
                ui.core.writeNGItem(this);
                sent = true;
            }
            if (lastQuestUpdate < NQuestInfo.lastUpdate.get()) {
                isQuested = NUtils.getGameUI().questinfo.isQuestedItem(this);
                lastQuestUpdate = NQuestInfo.lastUpdate.get();
            }
        }
    }


    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if (name != null)
        {
            if (msg.equals("take") || (msg.equals("iact")))
            {
                NUtils.getGameUI().getCharInfo().setCandidate(name());
                if (msg.equals("iact"))
                {
                    NUtils.getGameUI().getCharInfo().setFlowerCandidate(this);
                }
            }
        }
        super.wdgmsg(msg, args);
    }


    @Override
    public void uimsg(String name, Object... args) {
        super.uimsg(name, args);
        if(name.equals("tt") || name.equals("meter")) {
            meterUpdated = System.currentTimeMillis();
        }
    }
    @Override
    protected void updateraw()
    {
        if (rawinfo != null)
        {
            content.clear();
            for (Object o : rawinfo.data)
            {
                if (o instanceof Object[])
                {
                    Object[] a = (Object[]) o;
                    if (a[0] instanceof Integer)
                    {
                        if(nurgling.NUtils.getUI().sess!=null) {
                            String resName = NUtils.getUI().sess.getResName((Integer) a[0]);
                            if (resName != null) {
                                switch (resName) {
                                    case "ui/tt/q/quality":
                                        if (a.length >= 2) quality = (Float) a[1];
                                        break;
                                    case "ui/tt/armor":
                                        if (a.length >= 3) {
                                            hardArmor = (Integer) a[1];  // Жесткая броня
                                            softArmor = (Integer) a[2];  // Мягкая броня
                                        }
                                        break;
                                    case "ui/tt/cont":
                                        double q = -1;
                                        String name = null;
                                        for (Object so : a) {
                                            if (so instanceof Object[]) {
                                                Object[] cont = (Object[]) so;
                                                for (Object sso : cont) {
                                                    if (sso instanceof Object[]) {
                                                        Object[] b = (Object[]) sso;
                                                        if (b[0] instanceof Integer) {
                                                            String resName2 = NUtils.getUI().sess.getResName((Integer) b[0]);
                                                            if (b.length >= 2) if (resName2 != null) {
                                                                if (resName2.equals("ui/tt/cn")) {
                                                                    name = (String) b[1];
                                                                } else if (resName2.equals("ui/tt/q/quality")) {
                                                                    q = (Float) b[1];
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (name != null && q != -1) {
                                            content.add(new NContent(q, name));
                                        }
                                        break;
                                    case "ui/tt/coin":
                                        this.name = (String) a[1];
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public <C extends ItemInfo> C getInfo(Class<C> c){
        if(info!=null)
        {
            for(ItemInfo inf : info)
            {
                if(inf.getClass() == c)
                    return (C)inf;
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        if(parent!=null && parent instanceof NInventory && (((NInventory) parent).parentGob)!=null && (((NInventory) parent).parentGob).gob!=null && name!=null) {
            ((NInventory) parent).iis.add(new ItemWatcher.ItemInfo(name, quality != null ? quality : -1, wi != null ? wi.c : Coord.z, (((NInventory) parent).parentGob).hash));
            ((NInventory) parent).lastUpdate = NUtils.getTickId();
        }
        super.destroy();
    }
}
