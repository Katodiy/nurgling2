package nurgling;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.res.ui.rbuff.*;
import haven.res.ui.relcnt.RelCont;
import nurgling.conf.*;
import nurgling.notifications.*;
import nurgling.tools.*;
import nurgling.widgets.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static haven.Inventory.invsq;

public class NGameUI extends GameUI
{
    NBotsMenu botsMenu;
    public NAlarmWdg alarmWdg;
    public NQuestInfo questinfo;
    public NGUIInfo guiinfo;
    public NSearchItem itemsForSearch = null;
    public NCraftWindow craftwnd;
    public NEditAreaName nean;
    public Specialisation spec;
    public BotsInterruptWidget biw;
    public NEquipProxy nep;
    public NGameUI(String chrid, long plid, String genus, NUI nui)
    {
        super(chrid, plid, genus, nui);
        itemsForSearch = new NSearchItem();
        add(new NDraggableWidget(alarmWdg = new NAlarmWdg(),"alarm",NStyle.alarm[0].sz().add(NDraggableWidget.delta)));
        add(new NDraggableWidget(nep = new NEquipProxy(NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT, NEquipory.Slots.BELT), "EquipProxy",  UI.scale(138, 55)));
        for(int i = 0; i<(Integer)NConfig.get(NConfig.Key.numbelts); i++)
        {
            String name = "belt" + String.valueOf(i);
            NDraggableWidget belt = add(new NDraggableWidget(new NToolBelt(name, i * 12, 4, 12), name, UI.scale(new Coord(500, 56))));
            belt.setFlipped(true);
        }

        add(new NDraggableWidget(botsMenu = new NBotsMenu(), "botsmenu", botsMenu.sz.add(NDraggableWidget.delta)));
        add(new NDraggableWidget(questinfo = new NQuestInfo(), "quests", questinfo.sz.add(NDraggableWidget.delta)));
        add(guiinfo = new NGUIInfo(),new Coord(sz.x/2 - NGUIInfo.xs/2,sz.y/5 ));
        if(!(Boolean) NConfig.get(NConfig.Key.show_drag_menu))
            guiinfo.hide();
        add(nean = new NEditAreaName());
        nean.hide();
        add(spec = new Specialisation());
        spec.hide();
        add(biw = new BotsInterruptWidget());
    }

    @Override
    public void dispose() {
        if(nurgling.NUtils.getUI().core!=null)
            NUtils.getUI().core.dispose();
        super.dispose();
    }

    public int getMaxBase(){
        return 0;
//        return chrwdg.base.stream().max(new Comparator<CharWnd.Attr>() {
//            @Override
//            public int compare(CharWnd.Attr o1, CharWnd.Attr o2) {
//                return Integer.compare(o1.attr.base,o2.attr.base);
//            }
//        }).get().attr.base;
    }

    public NCharacterInfo getCharInfo() {
        return ((NUI)ui).sessInfo.characterInfo;
    }

    public Window getWindow ( String cap ) {
        for ( Widget w = lchild ; w != null ; w = w.prev ) {
            if ( w instanceof Window ) {
                Window wnd = ( Window ) w;
                if ( wnd.cap != null && wnd.cap.equals(cap)) {
                    return wnd;
                }
            }
        }
        return null;
    }

    public Window getWindowWithButton ( String cap, String button ) {
        for ( Widget w = lchild ; w != null ; w = w.prev ) {
            if ( w instanceof Window ) {
                Window wnd = ( Window ) w;
                if ( wnd.cap != null && wnd.cap.equals(cap)) {
                    for(Widget w2 = wnd.lchild ; w2 !=null ; w2= w2.prev )
                    {
                        if ( w2 instanceof Button ) {
                            Button b = ((Button)w2);
                            if(b.text!=null && b.text.text.equals(button)){
                                return (Window)w;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean isWindowExist ( Window twnd )
    {
        for (Widget w = lchild; w != null; w = w.prev)
        {
            if (w instanceof Window)
            {
                Window wnd = (Window) w;
                if (wnd.equals(twnd))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public double getTableMod() {
        double table_mod = 1;
        Window table = getWindowWithButton("Table", "Feast!");
        if(table!=null)
        {
            for (Widget wdg = table.child; wdg != null; wdg = wdg.next) {
                if (wdg instanceof Label) {
                    Label text = (Label) wdg;
                    if (text.text().contains("Food")) {
                        table_mod = table_mod + Double.parseDouble(text.text().substring(text.text().indexOf(":") + 1, text.text().indexOf("%"))) / 100.;
                        break;
                    }
                }
            }
        }
        return table_mod;
    }

    public double getRealmMod()
    {
        double realmBuff = 0;

        for (Widget wdg1 = child; wdg1 != null; wdg1 = wdg1.next)
        {
            if (wdg1 instanceof Bufflist)
            {
                for (Widget pbuff = wdg1.child; pbuff != null; pbuff = pbuff.next)
                {
                    if (pbuff instanceof RealmBuff)
                    {
                        if (((Buff) pbuff).info!=null)
                        {
                            ArrayList<ItemInfo> realm = new ArrayList<>(((Buff) pbuff).info);
                            for (Object data : realm)
                            {
                                if (data instanceof ItemInfo.AdHoc)
                                {
                                    ItemInfo.AdHoc ah = ((ItemInfo.AdHoc) data);
                                    if (NParser.checkName(ah.str.text, new NAlias("Food event")))
                                    {
                                        realmBuff = realmBuff + Double.parseDouble(ah.str.text.substring(ah.str.text.indexOf("+") + 1, ah.str.text.indexOf("%"))) / 100.;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return realmBuff;
    }

    @Override
    public void addchild(Widget child, Object... args)
    {
        String place = ((String) args[0]).intern();
        if (place == "craft") {
            if (craftwnd == null) {
                craftwnd = add(new NCraftWindow(), new Coord(400, 200));
            }
            craftwnd.add(child);
            craftwnd.pack();
            craftwnd.raise();
            craftwnd.show();
        }
        else
        {
            super.addchild(child, args);
            if (maininv != null && ((NInventory) maininv).searchwdg == null)
            {
                ((NInventory) maininv).installMainInv();
            }
        }
    }

    public void tickmsg(String msg) {
        msg("TICK#" + NUtils.getTickId() + " MSG: " + msg, Color.WHITE, Color.WHITE);
    }

    public NInventory getInventory ( String name ) {
        Window spwnd = getWindow ( name );
        if(spwnd == null){
            return null;
        }
        for ( Widget sp = spwnd.lchild ; sp != null ; sp = sp.prev ) {
            if ( sp instanceof Inventory ) {
                return ( ( NInventory ) sp );
            }
        }
        return null;
    }

    public NInventory getInventory () {
        return (NInventory) maininv;
    }

    public NISBox getStockpile () {
        Window spwnd = getWindow ( "Stockpile" );
        if(spwnd == null){
            return null;
        }
        for ( Widget sp = spwnd.lchild ; sp != null ; sp = sp.prev ) {
            if ( sp instanceof NISBox ) {
                return ( ( NISBox ) sp );
            }
        }
        return null;
    }

    @Override
    public void resize(Coord sz)
    {
        super.resize(sz);
        guiinfo.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 5));
        areas.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 5));
        nean.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 7));
        spec.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 7));
        biw.move(new Coord(sz.x / 2 - biw.sz.x / 2, sz.y / 2 - biw.sz.y / 2));
    }

    public List<IMeter.Meter> getmeters (String name ) {
        for ( Widget meter : meters ) {
            if ( meter instanceof IMeter ) {
                IMeter im = ( IMeter ) meter;
                Resource res = im.bg.get ();
                if ( res != null ) {
                    if ( res.basename ().equals ( name ) ) {
                        return im.meters;
                    }
                }
            }
        }
        return null;
    }

    public IMeter.Meter getmeter (
            String name,
            int midx
    ) {
        List<IMeter.Meter> meters = getmeters ( name );
        if ( meters != null && midx < meters.size () ) {
            return meters.get ( midx );
        }
        return null;
    }

    public double getBarrelContent()
    {
        return getBarrelContent(new NAlias(""));
    }

    public double getBarrelContent(NAlias content){
        Window spwnd = getWindow ( "Barrel" );
        if(spwnd!=null) {
            for (Widget sp = spwnd.lchild; sp != null; sp = sp.prev) {
                /// Выбираем внутренний контейнер
                if (sp instanceof RelCont) {
                    for(Pair<Widget, Supplier<Coord>> pair:((RelCont) sp).childpos) {
                        if (pair.a.getClass().getName().contains("TipLabel")) {
                            try {
                                ///TODO
                                for (ItemInfo inf : (Collection<ItemInfo>) (pair.a.getClass().getField("info").get(pair.a))) {
                                    if (inf instanceof ItemInfo.Name) {
                                        String name = ((ItemInfo.Name) inf).str.text;
                                        if (NParser.checkName(name.toLowerCase(), content))
                                            return Double.parseDouble(name.substring(0, name.indexOf(' ')));
                                    } else if (inf instanceof ItemInfo.AdHoc) {
                                        if (NParser.checkName(((ItemInfo.AdHoc) inf).str.text, "Empty")) {
                                            return 0;
                                        }
                                    }
                                }
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }


    public void msgToDiscord(NDiscordNotification settings, String message)
    {
        if (message != null && !message.isEmpty())
        {
            if (settings != null)
            {
                DiscordHookObject webhook = new DiscordHookObject(settings.webhookUrl);

                webhook.setContent(message);

                webhook.setAvatarUrl(settings.webhookIcon);
                webhook.setUsername(settings.webhookUsername);
                webhook.addEmbed(new nurgling.notifications.DiscordHookObject.EmbedObject()
                        .setColor(java.awt.Color.RED)
                        .setThumbnail(settings.webhookIcon)
                        .setAuthor("Nurgling2", "https://github.com/Katodiy/nurgling2", "https://raw.githubusercontent.com/Katodiy/nurgling2/master/resources/src/nurgling/hud/dragmode/title.res/image/image_0.png")
                        .setUrl("https://github.com/Katodiy/nurgling2"));
                new Thread(webhook).start();

            }
            else
            {
                error("No discord wrapper settings");
            }
        }
    }

    public void toggleol(String tag, boolean a) {
        if(map != null) {
            if(a)
                map.enol(tag);
            else
                map.disol(tag);
        }
    }

    public class NToolBelt extends Belt implements KeyBinding.Bindable{

        public static final int GAP = 10;
        public static final int PAD = 2;
        public static final int BTNSZ = 17;
        public final Coord INVSZ = invsq.sz();

        final int group;
        final int start;
        final int size;
        final String name;
        private boolean vertical = false;
        ArrayList<NKeyBinding> beltkeys = new ArrayList<>();
        public NToolBelt(String name, int start, int group, int size) {
            super( new Coord(0,0) );
            this.start = start;
            this.group = group;
            this.size = size;
            this.name = name;
            sz = beltc(size - 1).add(INVSZ);
            NToolBeltProp prop = NToolBeltProp.get(name);
            for(KeyBinding kb: prop.getKb())
            {
                beltkeys.add(new NKeyBinding(kb));
            }
        }

        @Override
        public void flip(boolean val) {
            vertical = val;
            resize();
        }

        private void resize() {
            sz = beltc(size - 1).add(INVSZ);
        }

        @Override
        public int beltslot(Coord c) {
            for (int i = 0; i < size; i++) {
                if(c.isect(beltc(i), invsq.sz())) {
                    return slot(i);
                }
            }
            return (-1);
        }

        @Override
        public KeyBinding getbinding(Coord cc) {
            int slot = beltslot(cc);
            if(slot!=-1)
                return beltkeys.get(slot - start).kb;
            return null;
        }

        @Override
        public void draw(GOut g) {
            for (int i = 0; i < size; i++) {
                Coord c = beltc(i);
                int slot = slot(i);
                g.image(invsq, c);
                try {
                    Object item = belt(slot);
                    if (item != null) {
                        if(item instanceof PagBeltSlot)
                            ((PagBeltSlot)item).draw(g.reclip(c.add(1, 1), invsq.sz().sub(2, 2)));
                        else if (item instanceof NBotsMenu.NButton)
                            ((NBotsMenu.NButton)item).btn.draw(g.reclip(c.add(1, 1), invsq.sz().sub(2, 2)));
                    }
                } catch (Loading ignored) {
                }
                if (beltkeys.get(i).tex != null) {
                    g.aimage(beltkeys.get(i).tex, c.add(INVSZ.sub(2, 0)), 1, 1);
                }
            }
            super.draw(g);
        }

        @Override
        public void keyact(int slot) {
            if(map != null) {
                NToolBeltProp prop = NToolBeltProp.get(name);
                String path;
                if((path = prop.custom.get(slot))!=null) {
                    NBotsMenu.NButton btn = NUtils.getGameUI().botsMenu.find(path);
                    if(btn!=null) {
                        btn.btn.click();
                        return;
                    }
                }
                super.keyact(slot);
            }
        }


        @Override
        public boolean mousedown(Coord c, int button) {
            NToolBeltProp prop = NToolBeltProp.get(name);
            int slot = beltslot(c);
            if(button == 3)
            {
                if(prop.custom.get(slot)!=null) {
                    prop.custom.remove(slot);
                    NToolBeltProp.set(name, prop);
                    return true;
                }
            }
            else if (button == 1)
            {
                String path;
                if((path = prop.custom.get(slot))!=null) {
                    NBotsMenu.NButton btn = NUtils.getGameUI().botsMenu.find(path);
                    if(btn!=null) {
                        btn.btn.click();
                        return true;
                    }
                }
            }
            return super.mousedown(c, button);
        }

        private Object belt(int slot) {
            if(slot < 0) {return null;}
            String path;
            if((path = NToolBeltProp.get(name).custom.get(slot) )== null) {
                GameUI.PagBeltSlot res = null;
                if (ui != null && belt[slot] != null)
                    if (belt[slot] instanceof GameUI.PagBeltSlot) {
                        res = (GameUI.PagBeltSlot) belt[slot];
                    }
                return res;
            }
            else
            {
                return botsMenu.find(path);
            }
        }

        private int slot(int i) {return i + start;}

        private Coord beltc(int i) {
            return vertical ?
                    new Coord(0, BTNSZ + ((INVSZ.y + PAD) * i) + (GAP * (i / group))) :
                    new Coord(BTNSZ + ((INVSZ.x + PAD) * i) + (GAP * (i /group )), 0);
        }

        @Override
        public void tick(double dt) {
            super.tick(dt);
            boolean res = false;
            for(NKeyBinding kb : beltkeys)
                res |= kb.tick();
            if(res)
            {
                NToolBeltProp.set(name,NToolBeltProp.get(name));
            }
        }

        @Override
        public boolean globtype(char key, KeyEvent ev) {
            if (!visible) {
                return false;
            }
            for (int i = 0; i < beltkeys.size(); i++) {
                if ((beltkeys.get(i).key != null && ev.getKeyCode() == beltkeys.get(i).key.code && ui.modflags() == beltkeys.get(i).key.modmatch)) {
                    keyact(slot(i));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean dropthing(Coord c, Object thing) {
            boolean res = super.dropthing(c,thing);
            int slot = beltslot(c);
            if(res) {
                if(slot!=-1)
                {
                    NToolBeltProp prop = NToolBeltProp.get(name);
                    prop.custom.remove(slot);
                    NToolBeltProp.set(name,prop);
                }
                return true;
            }

            if(slot != -1) {
                if(thing instanceof NBotsMenu.NButton) {
                    NBotsMenu.NButton pag = (NBotsMenu.NButton)thing;
                    NToolBeltProp prop = NToolBeltProp.get(name);
                    prop.custom.put(slot,pag.path);
                    NToolBeltProp.set(name,prop);
                    return(true);
                }
            }
            return(false);
        }

    }

    public static class NKeyBinding
    {
        public int modign;
        public KeyMatch key;
        KeyBinding kb;
        public NKeyBinding(KeyBinding old) {
            this.kb = old;
            this.key = old.key;
            this.modign = old.modign;
            updateTex();
        }

        Tex tex;
        public void set(KeyMatch key) {
            kb.set(key);
            updateTex();
        }

        void updateTex()
        {
            String hotKey;
            int mode  = 0;
            if( key != null)
            {
                hotKey = KeyEvent.getKeyText(key.code);
                mode = key.modmatch;

                if (NParser.checkName(hotKey, new NAlias("Num")))
                {
                    hotKey = "N" + hotKey.substring(hotKey.indexOf("-") + 1);
                }
                if (NParser.checkName(hotKey, new NAlias("inus")))
                {
                    hotKey = "-";
                }
                else if (NParser.checkName(hotKey, new NAlias("quals")))
                {
                    hotKey = "=";
                }
                if ((mode & KeyMatch.C) != 0)
                    hotKey = "C" + hotKey;
                if ((mode & KeyMatch.S) != 0)
                    hotKey = "S" + hotKey;
                if ((mode & KeyMatch.M) != 0)
                    hotKey = "A" + hotKey;
                tex = NStyle.hotkey.render(hotKey).tex();
            }
        }

        boolean tick()
        {
            if(kb.key!=key || kb.modign!=modign)
            {
                key = kb.key;
                modign = kb.modign;
                updateTex();
                return true;
            }
            return false;
        }


    }
}
