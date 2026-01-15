package nurgling.widgets;

import haven.Button;
import haven.*;
import haven.Frame;
import haven.Label;
import static haven.Inventory.*;

import haven.render.Render;
import haven.res.lib.itemtex.*;
import nurgling.*;
import nurgling.actions.bots.*;
import nurgling.areas.*;
import nurgling.tools.*;
import org.json.*;

import java.awt.*;
import java.awt.image.*;
import java.util.List;
import java.util.*;

public class NMakewindow extends Widget {
//    public static final Text.Foundry fnd = new Text.Foundry(Text.sans, 12);

    public static Text.Furnace fnd = new PUtils.BlurFurn(new Text.Foundry(Text.sans.deriveFont(java.awt.Font.BOLD), 12).aa(true), UI.scale(1), UI.scale(1), Color.BLACK);
    public static Text.Furnace fnd2 = new Text.Foundry(Text.sans, 12).aa(true);
    public static final Text qmodl = fnd.render(("Quality:"));
    public static final TexI aready = new TexI(Resource.loadsimg("nurgling/hud/autocraft/ready"));
    public static final TexI anotfound = new TexI(Resource.loadsimg("nurgling/hud/autocraft/notfound"));
    public static final TexI categories = new TexI(Resource.loadsimg("nurgling/hud/autocraft/spec"));
    public static final TexI ignoreOverlay = new TexI(Resource.loadsimg("nurgling/hud/autocraft/ignore"));
    public static final Text tooll = fnd.render(("Tools:"));
    public static final Coord boff = UI.scale(new Coord(7, 9));
    public String rcpnm;
    public String recipeResource;
    public List<Spec> inputs = Collections.emptyList();
    public List<Spec> outputs = Collections.emptyList();
    public List<Indir<Resource>> qmod = Collections.emptyList();
    public List<Indir<Resource>> tools = new ArrayList<>();;
    private int xoff = UI.scale(45), qmy = UI.scale(38), outy = UI.scale(65);
    public static final Text.Foundry nmf = new Text.Foundry(Text.serif, 20).aa(true);
    private static double softcap = 0;
    private static Tex softTex = null;
    private static Tex softTexLabel = null;
    public CheckBox noTransfer = null;
    public boolean autoMode = false;
    private IButton savePresetBtn = null;

    private static final OwnerContext.ClassResolver<NMakewindow> ctxr = new OwnerContext.ClassResolver<NMakewindow>()
            .add(Glob.class, wdg -> wdg.ui.sess.glob)
            .add(Session.class, wdg -> wdg.ui.sess);
    public class Spec implements GSprite.Owner, ItemInfo.SpriteOwner {
        public Indir<Resource> res;
        public MessageBuf sdt;
        public Tex num;
        public String name;
        public int count;
        public GSprite spr;
        private Object[] rawinfo;
        private List<ItemInfo> info;

        public Ingredient ing = null;

        public Spec(Indir<Resource> res, Message sdt, int num, Object[] info) {
            this.res = res;
            this.sdt = new MessageBuf(sdt);
            if(num >= 0)
                this.num = new TexI(Utils.outline2(Text.render(Integer.toString(num), Color.WHITE).img, Utils.contrast(Color.WHITE)));
            else
                this.num = null;
            this.rawinfo = info;
            this.count = num;
        }

        public GSprite sprite() {
            if(spr == null)
                spr = GSprite.create(this, res.get(), sdt.clone());;
            return(spr);
        }

        public void draw(GOut g) {
            try {
                if(ing==null || !autoMode)
                {
                    sprite().draw(g);
                }
                else
                {
                    g.image(new TexI(ing.img), Coord.z, UI.scale(32,32));
                }
            } catch(Loading e) {}
            if(num != null)
                g.aimage(num, Inventory.sqsz, 1.0, 1.0);
        }

        private int opt = 0;
        public boolean opt() {
            if(opt == 0) {
                try {
                    List<ItemInfo> infoList = info();
                    boolean found = false;
                    for (ItemInfo inf : infoList) {
                        String className = inf.getClass().getName();
                        if (className.endsWith("$Optional")) {
                            found = true;
                            break;
                        }
                    }
                    opt = found ? 1 : 2;
                } catch(Loading l) {
                    return(false);
                } catch(Exception e) {
                    e.printStackTrace();
                    return(false);
                }
            }
            return(opt == 1);
        }

        public BufferedImage shorttip() {
            List<ItemInfo> info = info();
            if(info.isEmpty()) {
                Resource.Tooltip tt = res.get().layer(Resource.tooltip);
                if(tt == null)
                    return(null);
                return(Text.render(tt.text()).img);
            }
            return(ItemInfo.shorttip(info()));
        }
        public BufferedImage longtip() {
            List<ItemInfo> info = info();
            BufferedImage img;
            if(info.isEmpty()) {
                Resource.Tooltip tt = res.get().layer(Resource.tooltip);
                if(tt == null)
                    return(null);
                img = Text.render(tt.text()).img;
            } else {
                img = ItemInfo.longtip(info);
            }
            Resource.Pagina pg = res.get().layer(Resource.pagina);
            if(pg != null)
                img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, 200).img);
            return(img);
        }

        private Random rnd = null;
        public Random mkrandoom() {
            if(rnd == null)
                rnd = new Random();
            return(rnd);
        }
        public Resource getres() {return(res.get());}
        public <T> T context(Class<T> cl) {return(ctxr.context(cl, NMakewindow.this));}
        @Deprecated
        public Glob glob() {return(ui.sess.glob);}

        public List<ItemInfo> info() {
            if(info == null)
                info = ItemInfo.buildinfo(this, rawinfo);
            return(info);
        }
        public Resource resource() {return(res.get());}


        void tick(double dt)
        {
            if (name == null && spr != null)
            {
                if (!res.get().name.contains("coin"))
                {
                    if (res.get() != null)
                    {
                        name = ItemInfo.Name.Default.get(this);
                    }
                }
            }
            if(NMakewindow.this.autoMode && name!=null)
            {
                logisticin = (NContext.findIn(name) != null);
                if(!logisticin)
                {
                    categories = (VSpec.categories.get(name)!=null);
                }
                logisticout = (NContext.findOut(name,1) != null);
                if(!logisticout)
                {
                    categories = (VSpec.categories.get(name)!=null);
                }
                for(Spec s : inputs) {
                    if(s.categories && s.ing!=null)
                    {
                        s.ing.logistic = (NContext.findIn(s.ing.name) != null);
                    }
                }
                for(Spec s : outputs) {
                    if(s.categories && s.ing!=null)
                    {
                        s.ing.logistic = (NContext.findOut(s.ing.name, 1) != null);
                    }
                }
            }
        }

        String name()
        {
            return name;
        }

        public boolean logisticin = false;
        public boolean logisticout = false;
        public boolean categories = false;


    }

    public void tick(double dt) {
        for(Spec s : inputs) {
            if(s.spr != null)
                s.spr.tick(dt);
            s.tick(dt);
        }
        for(Spec s : outputs) {
            if(s.spr != null)
                s.spr.tick(dt);
            s.tick(dt);
        }
        if(cat!=null) {
            cat.raise();
            cat.tick(dt);
        }

        // Update Save Preset button visibility
        if (savePresetBtn != null) {
            savePresetBtn.visible = autoMode && allInputsConfigured();
        }
    }

    /**
     * Check if all category inputs have their ingredient selected.
     */
    private boolean allInputsConfigured() {
        for (Spec s : inputs) {
            // If it's a category input, it needs to have an ingredient selected (or be ignored)
            if (s.categories && s.ing == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if(autoMode)
        {
            Coord sc = new Coord(xoff, 0);
            boolean popt = false;
            if (clickForCategories(inputs, popt, sc, ev.c)) return true;
            sc = new Coord(xoff, outy);
            if (clickForCategories(outputs, popt, sc, ev.c)) return true;
        }
        return super.mousedown(ev);
    }

    private boolean clickForCategories(List<Spec> outputs, boolean popt, Coord sc, Coord c) {
        for (Spec s : outputs) {
            boolean opt = s.opt();
            if (opt != popt)
                sc = sc.add(10, 0);
            if (s.categories) {
                if (c.isect(sc, Inventory.sqsz)) {
                    boolean isOpt = s.opt();
                if (cat == null) {
                        NUtils.getGameUI().add(cat = new Categories(VSpec.categories.get(s.name), s, isOpt), sc.add(this.parent.c).add(this.c).add(Inventory.sqsz.x / 2, Inventory.sqsz.y * 2).add(UI.scale(2, 2)));
                        pack();
                        NUtils.getGameUI().craftwnd.lower();
                        cat.raise();
                        return true;
                    }
                }
            }
            sc = sc.add(Inventory.sqsz.x, 0);
            popt = opt;
        }
        return false;
    }


    TextEntry craft_num;
    public static final KeyBinding kb_make = KeyBinding.get("make/one", KeyMatch.forcode(java.awt.event.KeyEvent.VK_ENTER, 0));
    public static final KeyBinding kb_makeall = KeyBinding.get("make/all", KeyMatch.forcode(java.awt.event.KeyEvent.VK_ENTER, KeyMatch.C));
    public NMakewindow(String rcpnm) {
        int inputW = add(new Label("Input:"), new Coord(0, UI.scale(8))).sz.x;
        int resultW = add(new Label("Result:"), new Coord(0, outy + UI.scale(8))).sz.x;
        xoff = Math.max(inputW, resultW) + UI.scale(10);

        add(new Button(UI.scale(85), "Craft"), UI.scale(new Coord(230, 75))).action(() -> craft()).setgkey(kb_make);
        add(craft_num = new TextEntry(UI.scale(55), ""), UI.scale(new Coord(165, 82)));
        add(new Button(UI.scale(85), "Craft All"), UI.scale(new Coord(325, 75))).action(() -> craftAll()).setgkey(kb_makeall);
        add(new ICheckBox(NStyle.auto[0],NStyle.auto[1],NStyle.auto[2],NStyle.auto[3]){
            @Override
            public void changed(boolean val)
            {
                super.changed(val);
                autoMode = val;
                noTransfer.visible = val;
            }
        }, UI.scale(new Coord(365, 5)));

        add(noTransfer = new CheckBox("No transfer")
        {
            @Override
            public void changed(boolean val) {
                super.changed(val);
            }
        }, UI.scale(new Coord(325, 38)));
        noTransfer.visible = false;

        // Save Preset button - only visible in auto mode when all inputs are configured
        // Scale icons to 2/3 size and position left of quantity input
        int scaledW = NStyle.savei[0].back.getWidth() * 2 / 3;
        int scaledH = NStyle.savei[0].back.getHeight() * 2 / 3;
        Coord scaledSz = new Coord(scaledW, scaledH);
        BufferedImage scaledUp = PUtils.convolve(NStyle.savei[0].back, scaledSz, CharWnd.iconfilter);
        BufferedImage scaledDown = PUtils.convolve(NStyle.savei[1].back, scaledSz, CharWnd.iconfilter);
        BufferedImage scaledHover = PUtils.convolve(NStyle.savei[2].back, scaledSz, CharWnd.iconfilter);
        savePresetBtn = add(new IButton(scaledUp, scaledDown, scaledHover) {
            @Override
            public void click() {
                openSavePresetDialog();
            }
        }, UI.scale(new Coord(340, 5)));
        savePresetBtn.visible = false;

        pack();
        this.rcpnm = rcpnm;

        // Capture recipe resource from MenuGrid.lastPagina while it's still valid
        if (MenuGrid.lastPagina != null) {
            try {
                this.recipeResource = MenuGrid.lastPagina.res().name;
            } catch (Loading l) {
                // Resource not loaded yet
            }
        }
    }

    private void openSavePresetDialog() {
        NUtils.getGameUI().add(new SaveCraftPresetDialog(this), UI.scale(new Coord(200, 200)));
    }

    public void uimsg(String msg, Object... args) {
        if(msg == "inpop") {
            List<Spec> inputs = new LinkedList<Spec>();
            for(int i = 0; i < args.length;) {
                int resid = (Integer)args[i++];
                Message sdt = (args[i] instanceof byte[])?new MessageBuf((byte[])args[i++]):MessageBuf.nil;
                int num = (Integer)args[i++];
                Object[] info = {};
                if((i < args.length) && (args[i] instanceof Object[]))
                    info = (Object[])args[i++];
                inputs.add(new Spec(ui.sess.getres(resid), sdt, num, info));
            }
            this.inputs = inputs;
        } else if(msg == "opop") {
            List<Spec> outputs = new LinkedList<Spec>();
            for(int i = 0; i < args.length;) {
                int resid = (Integer)args[i++];
                Message sdt = (args[i] instanceof byte[])?new MessageBuf((byte[])args[i++]):MessageBuf.nil;
                int num = (Integer)args[i++];
                Object[] info = {};
                if((i < args.length) && (args[i] instanceof Object[]))
                    info = (Object[])args[i++];
                outputs.add(new Spec(ui.sess.getres(resid), sdt, num, info));
            }
            this.outputs = outputs;
        } else if(msg == "qmod") {
            List<Indir<Resource>> qmod = new ArrayList<Indir<Resource>>();
            for(Object arg : args)
                qmod.add(ui.sess.getres((Integer)arg));
            this.qmod = qmod;
        } else if(msg == "tool") {
            tools.add(ui.sess.getres((Integer)args[0]));
        } else {
            super.uimsg(msg, args);
        }
    }

    public static final Coord qmodsz = UI.scale(20, 20);
    private static final WeakHashMap<Indir<Resource>, Tex> qmicons = new WeakHashMap<>();
    private Tex qmicon(Indir<Resource> qm) {
        synchronized (qmicons) {
            return qmicons.computeIfAbsent(qm, NMakewindow.this::buildQTex);
        }
    }

    public void draw(GOut g) {
        Coord c = new Coord(xoff, 0);
        boolean popt = false;
        for(Spec s : inputs) {
            boolean opt = s.opt();
            if(opt != popt)
                c = c.add(10, 0);
            GOut sg = g.reclip(c, invsq.sz());
            if(opt) {
                sg.chcolor(0, 255, 0, 255);
                sg.image(invsq, Coord.z);
                sg.chcolor();
            } else {
                sg.image(invsq, Coord.z);
            }
            s.draw(sg);
            c = c.add(Inventory.sqsz.x, 0);
            popt = opt;
            if(autoMode)
            {
                if(s.logisticin)
                {
                    sg.image(aready, Coord.z);
                }
                else
                {
                    if(s.categories)
                    {
                        if(s.ing==null)
                            sg.image(categories, Coord.z);
                        else
                        {
                            if(s.ing.isIgnored)
                            {
                                sg.image(ignoreOverlay, Coord.z);
                            }
                            else if(s.ing.logistic)
                            {
                                sg.image(aready, Coord.z);
                            }
                            else
                            {
                                sg.image(anotfound, Coord.z);
                            }
                        }
                    }
                    else
                    {
                        sg.image(anotfound, Coord.z);
                    }
                }
            }
        }
        {
            int x = 0;
            if(!qmod.isEmpty()) {
                x += qmodl.sz().x + UI.scale(5);
                x = Math.max(x, xoff);
                qmx = x;
                int count = 0;
                double product = 1.0;
                for(Indir<Resource> qm : qmod) {
                    try {
                        Tex t = buildQTex(qm);
                        g.image(t, new Coord(x, qmy));
                        x += t.sz().x + UI.scale(1);

                        for(BAttrWnd.Attr attr: ui.gui.chrwdg.battr.attrs)
                        {
                            if(attr.attr.nm.equals(qm.get().basename()))
                            {
                                count++;
                                product = product * attr.attr.comp;

                                BufferedImage texVal = fnd2.render(String.valueOf(attr.attr.comp)).img;
                                g.image(texVal,new Coord(x, qmy + UI.scale(1)));
                                x += texVal.getWidth() + UI.scale(1);
                                break;
                            }
                        }
                        for(SAttrWnd.SAttr attr: ui.gui.chrwdg.sattr.attrs)
                        {
                            if(attr.attr.nm.equals(qm.get().basename()))
                            {
                                count++;
                                product = product * attr.attr.comp;
                                BufferedImage texVal = fnd2.render(String.valueOf(attr.attr.comp)).img;
                                g.image(texVal,new Coord(x, qmy + UI.scale(1)));
                                x += texVal.getWidth() + UI.scale(1);
                                break;
                            }
                        }
                    } catch(Loading l) {
                    }
                }
                if(count > 0) {
                    x += drawSoftcap(g, new Coord(x, qmy), product, count);
                }
                x += UI.scale(25);

            }
            if(!tools.isEmpty()) {
                g.aimage(tooll.tex(), new Coord(x, qmy + (qmodsz.y / 2) - UI.scale(2)), 0, 0.5);
                x += tooll.sz().x + UI.scale(5);
                x = Math.max(x, xoff);
                toolx = x;
                for(Indir<Resource> tool : tools) {
                    try {
                        Tex t = qmicon(tool);
                        g.image(t, new Coord(x, qmy));
                        x += t.sz().x + UI.scale(1);
                    } catch(Loading l) {
                    }
                }
                x += UI.scale(25);
            }
        }
        c = new Coord(xoff, outy);
        for(Spec s : outputs) {
            GOut sg = g.reclip(c, invsq.sz());
            sg.image(invsq, Coord.z);
            s.draw(sg);
            c = c.add(Inventory.sqsz.x, 0);
            if(autoMode)
            {
                if(s.logisticout)
                {
                    sg.image(aready, Coord.z);
                }
                else
                {
                    if(s.categories)
                    {
                        if(s.ing==null)
                            sg.image(categories, Coord.z);
                        else
                        {
                            if(s.ing.isIgnored)
                            {
                                sg.image(ignoreOverlay, Coord.z);
                            }
                            else if(s.ing.logistic)
                            {
                                sg.image(aready, Coord.z);
                            }
                            else
                            {
                                sg.image(anotfound, Coord.z);
                            }
                        }
                    }
                    else
                    {
                        sg.image(anotfound, Coord.z);
                    }
                }
            }
        }
        super.draw(g);
    }

    private int drawSoftcap(GOut g, Coord p, double product, int count) {
        if(count > 0) {
            double current = Math.pow(product, 1.0 / count);
            if (current != softcap || softTex == null) {
                softcap = current;
                String format = String.format("%.1f", softcap);
                if (softTex != null) {
                    softTex.dispose();
                }
                softTexLabel = new TexI(fnd.render("Softcap:").img);
                softTex = new TexI(fnd2.render(format).img);
            }
            g.image(softTexLabel, p.add(UI.scale(5), UI.scale(-2)));
            g.image(softTex, p.add(UI.scale(5) + softTexLabel.sz().x + UI.scale(3), UI.scale(1)));
            return softTex.sz().x + softTexLabel.sz().x + UI.scale(9);
        }
        return 0;
    }

    private Tex buildQTex(Indir<Resource> res) {
        BufferedImage result = PUtils.convolve(res.get().layer(Resource.imgc).img, qmodsz, CharWnd.iconfilter);
        try {
//            Glob.CAttr attr = NUtils.getGameUI().chrwdg.findattr(res.get().basename());
//            if(attr != null) {
//                result = ItemInfo.catimgsh(1, result, attr.compline().img);
//            }
        } catch (Exception ignored) {
        }
        return new TexI(result);
    }

    public static void invalidate(String name) {
        synchronized (qmicons) {
            LinkedList<Indir<Resource>> tmp = new LinkedList<>(qmicons.keySet());
            tmp.forEach(res -> {
                if(name.equals(res.get().basename())) {
                    qmicons.remove(res);
                }
            });
        }
    }

    private int qmx, toolx;
    private long hoverstart;
    private Spec lasttip;
    private Indir<Object> stip, ltip;
    public Object tooltip(Coord mc, Widget prev) {
        String name = null;
        Spec tspec = null;
        Coord c;
        if(!qmod.isEmpty()) {
            c = new Coord(qmx, qmy);
            try {
                for(Indir<Resource> qm : qmod) {
                    Tex t = qmicon(qm);
                    Coord sz = t.sz();
                    if(mc.isect(c, sz))
                        return(qm.get().layer(Resource.tooltip).text());
                    c = c.add(sz.x + UI.scale(1), 0);
                }
            } catch(Loading l) {
            }
        }
        if(!tools.isEmpty()) {
            c = new Coord(toolx, qmy);
            try {
                for(Indir<Resource> tool : tools) {
                    Coord tsz = qmicon(tool).sz();
                    if(mc.isect(c, tsz))
                        return(tool.get().layer(Resource.tooltip).text());
                    c = c.add(tsz.x + UI.scale(1), 0);
                }
            } catch(Loading l) {
            }
        }
        find: {
            c = new Coord(xoff, 0);
            boolean popt = false;
            for(Spec s : inputs) {
                boolean opt = s.opt();
                if(opt != popt)
                    c = c.add(UI.scale(10), 0);
                if(mc.isect(c, Inventory.invsq.sz())) {
                    name = getDynamicName(s.spr);
                    if(name == null || name.contains("Raw")){
                        tspec = s;
                    }
                    break find;
                }
                c = c.add(Inventory.sqsz.x, 0);
                popt = opt;
            }
            c = new Coord(xoff, outy);
            for(Spec s : outputs) {
                if(mc.isect(c, invsq.sz())) {
                    tspec = s;
                    break find;
                }
                c = c.add(Inventory.sqsz.x, 0);
            }
        }
        if(lasttip != tspec) {
            lasttip = tspec;
            stip = ltip = null;
        }
        if(tspec == null)
            return(super.tooltip(mc, prev));
        long now = System.currentTimeMillis();
        boolean sh = true;
        if(prev != this)
            hoverstart = now;
        else if(now - hoverstart > 1000)
            sh = false;
        if(sh) {
            if(stip == null) {
                BufferedImage tip = tspec.shorttip();
                if(tip == null) {
                    stip = () -> null;
                } else {
                    Tex tt = new TexI(tip);
                    stip = () -> tt;
                }
            }
            return(stip);
        } else {
            if(ltip == null) {
                BufferedImage tip = tspec.longtip();
                if(tip == null) {
                    ltip = () -> null;
                } else {
                    Tex tt = new TexI(tip);
                    ltip = () -> tt;
                }
            }
            return(ltip);
        }
    }

    private static String getDynamicName(GSprite spr) {
        if(spr != null) {
            if(spr instanceof ItemInfo.Name.Dynamic)
            {
                return ((ItemInfo.Name.Dynamic)spr).name();
            }
        }
        return null;
    }

    public static Class[] interfaces(Class c) {
        try {
            return c.getInterfaces();
        } catch (Exception ignored) {}
        return new Class[0];
    }

    public static boolean hasInterface(String name, Class c) {
        Class[] interfaces = interfaces(c);
        for (Class in : interfaces) {
            if(in.getCanonicalName().equals(name)) {return true; }
        }
        return false;
    }

    public boolean globtype(GlobKeyEvent ev) {
        if(ev.c == '\n') {
            wdgmsg("make", ui.modctrl?1:0);
            return(true);
        }
        return(super.globtype(ev));
    }

    void craft()
    {
        if(!autoMode)
            wdgmsg("make", 0);
        else
        {
            Thread t;
            (t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        int num = 1;
                        try
                        {
                            String cand = craft_num.text();
                            if(!cand.isEmpty())
                                num = Integer.parseInt(cand);
                        }
                        catch (NumberFormatException e)
                        {
                            NUtils.getGameUI().error("Incorrect target num");
                        }
                        new Craft(NMakewindow.this, num).run(NUtils.getGameUI());
                    }
                    catch (InterruptedException e)
                    {
                        NUtils.getGameUI().tickmsg(Craft.class.getName() + "stopped");
                    }
                }
            }, "Auto craft(BOT)")).start();
            NUtils.getGameUI().biw.addObserve(t);
        }
    }

    void craftAll()
    {
        if(!autoMode)
        {
            wdgmsg("make", 1);
        }
        else
        {
            Thread t;
            (t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        new Craft(NMakewindow.this, 9999).run(NUtils.getGameUI());
                    }
                    catch (InterruptedException e)
                    {
                        NUtils.getGameUI().tickmsg(Craft.class.getName() + "stopped");
                    }
                }
            }, "Auto craft(BOT)")).start();
            NUtils.getGameUI().biw.addObserve(t);
        }
    }



    public static class Optional extends ItemInfo.Tip {
        public static final Text text = RichText.render(String.format("$i{%s}", "Optional"), 0);
        public Optional(Owner owner) {
            super(owner);
        }

        public BufferedImage tipimg() {
            return(text.img);
        }

        public Tip shortvar() {return(this);}
    }

    public static class MakePrep extends ItemInfo implements GItem.ColorInfo {
        private final static Color olcol = new Color(0, 255, 0, 64);
        public MakePrep(Owner owner) {
            super(owner);
        }

        public Color olcol() {
            return(olcol);
        }
    }

    Categories cat = null;

    public class Ingredient{
        public BufferedImage img;
        public String name;
        boolean logistic;
        public boolean isIgnored = false;

        public Ingredient(JSONObject obj)
        {
            img = ItemTex.create(obj);
            name = (String) obj.get("name");
        }

        public Ingredient(BufferedImage img, String name, boolean isIgnored)
        {
            this.img = img;
            this.name = name;
            this.isIgnored = isIgnored;
        }

        void tick(double dt)
        {
            if (!isIgnored) {
                logistic = (NContext.findIn(name) != null);
            }
        }
    }

    final static Coord catoff = UI.scale(8,8);
    final static Coord catend = UI.scale(15,15);
    static final int width = 9;
    
    private static Coord calculateSize(int totalSize) {
        return new Coord(
            Math.max((Inventory.sqsz.x+UI.scale(1))*((totalSize/width>=1)?width:0),
                    (Inventory.sqsz.x+UI.scale(1))*(totalSize%width))- UI.scale(2),
            (Inventory.sqsz.x+UI.scale(1))*(totalSize/width+(totalSize%width!=0?1:0))
        ).add(UI.scale(20,18));
    }
    
    public class Categories extends Widget
    {

        Color bg = new Color(30,40,40,160);
        ArrayList<Ingredient> data = new ArrayList<>();
        Frame fr;


        Spec s;
        boolean isOptional;
        
        public Categories(ArrayList<JSONObject> objs, Spec s, boolean isOptional)
        {
            super(calculateSize(objs.size() + (isOptional ? 1 : 0)));
            this.s = s;
            this.isOptional = isOptional;
            add(fr = new Frame(sz.sub(catend),true));
            
            // Add "ignore" option first if this is optional
            if (isOptional) {
                try {
                    BufferedImage ignoreImg = Resource.loadsimg("nurgling/hud/autocraft/ignore");
                    data.add(new Ingredient(ignoreImg, "Ignore ingredient", true));
                } catch (Exception e) {
                    System.out.println("Failed to load ignore resource: " + e.getMessage());
                }
            }
            
            for(JSONObject obj: objs)
            {
                data.add(new Ingredient(obj));
            }
            setfocustab(true);
            autofocus =true;
        }

        @Override
        public void draw(GOut g)
        {
            super.draw(g);
            g.chcolor(bg);
            g.frect(UI.scale(4,4), fr.inner());
            Coord pos = new Coord(catoff);
            Coord shift = new Coord(0,0);
            for(Ingredient ing: data)
            {
                GOut sg = g.reclip(pos, invsq.sz());
                sg.image(new TexI(ing.img), Coord.z,UI.scale(32,32));
                if(ing.isIgnored)
                {
                    sg.image(ignoreOverlay, Coord.z,UI.scale(32,32));
                }
                else if(ing.logistic)
                {
                    sg.image(aready, Coord.z,UI.scale(32,32));
                }
                else
                {
                    sg.image(anotfound, Coord.z,UI.scale(32,32));
                }
                if(shift.x<width-1)
                {
                    pos = pos.add(Inventory.sqsz.x + UI.scale(1), 0);
                    shift.x+=1;
                }
                else
                {
                    pos.x = UI.scale(8);
                    shift.x = 0;
                    pos = pos.add(0, Inventory.sqsz.y + UI.scale(1));
                }
            }

        }
        UI.Grab mg;
        @Override
        protected void added()
        {
            mg = NUtils.getUI().grabmouse(this);
        }

        @Override
        public void remove()
        {
            mg.remove();
            super.remove();
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            Coord pos = new Coord(catoff);
            if(!ev.c.isect(pos, sz.sub(catend)))
            {
                destroy();
                cat = null;
                return true;
            }
            else
            {
                Coord shift = new Coord(0,0);
                for(Ingredient ing: data)
                {
                    if(ev.c.isect(pos, invsq.sz()))
                    {
                        s.ing = ing;
                        destroy();
                        cat = null;
                        return true;
                    }
                    if(shift.x<width-1)
                    {
                        pos = pos.add(Inventory.sqsz.x + UI.scale(1), 0);
                        shift.x+=1;
                    }
                    else
                    {
                        pos.x = UI.scale(8);
                        pos = pos.add(0, Inventory.sqsz.y + UI.scale(1));
                        shift.x = 0;
                    }
                }
                return true;
            }
        }


        @Override
        public void tick(double dt)
        {
            for(Ingredient ing: data)
            {
                ing.tick(dt);
            }
            super.tick(dt);
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            Coord pos = new Coord(catoff);
            if(!c.isect(pos, sz.sub(catend)))
            {
                return null;
            }
            else
            {
                Coord shift = new Coord(0,0);
                for(Ingredient ing: data)
                {
                    if(c.isect(pos, invsq.sz()))
                    {
                        return ing.name;
                    }
                    if(shift.x<width-1)
                    {
                        pos = pos.add(Inventory.sqsz.x + UI.scale(1), 0);
                        shift.x+=1;
                    }
                    else
                    {
                        pos.x = UI.scale(8);
                        pos = pos.add(0, Inventory.sqsz.y + UI.scale(1));
                        shift.x = 0;
                    }
                }
                return true;
            }
        }
    }
}
