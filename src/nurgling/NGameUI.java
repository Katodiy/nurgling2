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
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class NGameUI extends GameUI
{
    NBotsMenu botsMenu;
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
        add(new NDraggableWidget(nep = new NEquipProxy(NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT, NEquipory.Slots.BELT), "EquipProxy",  UI.scale(138, 55)));
        for(int i = 0; i<(Integer)NConfig.get(NConfig.Key.numbelts); i++)
        {
            String name = "belt" + String.valueOf(i);
            NDraggableWidget belt = add(new NDraggableWidget(new NToolBelt(name, i * 12, 4, 12), name, UI.scale(new Coord(500, 56))));
            belt.setFlipped(true);
        }

        add(new NDraggableWidget(botsMenu = new NBotsMenu(), "botsmenu", botsMenu.sz.add(NDraggableWidget.delta)));
        add(guiinfo = new NGUIInfo(),new Coord(sz.x/2 - NGUIInfo.xs/2,sz.y/5 ));
        if(!(Boolean) NConfig.get(NConfig.Key.show_drag_menu))
            guiinfo.hide();
        add(nean = new NEditAreaName());
        nean.hide();
        add(spec = new Specialisation());
        spec.hide();
        add(biw = new BotsInterruptWidget());
    }

    public int getMaxBase(){
        return chrwdg.base.stream().max(new Comparator<CharWnd.Attr>() {
            @Override
            public int compare(CharWnd.Attr o1, CharWnd.Attr o2) {
                return Integer.compare(o1.attr.base,o2.attr.base);
            }
        }).get().attr.base;
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
            if (place.equals("chr") && chrwdg != null)
            {
                ((NUI) ui).sessInfo.characterInfo.setCharWnd(chrwdg);
            }
            if (maininv != null && ((NInventory) maininv).searchwdg == null)
            {
                ((NInventory) maininv).installMainInv();
            }
        }
    }

    public void tickmsg(String msg) {
        msg("TICK#" + NUtils.getTickId() + " MSG: " + msg, Color.WHITE, Color.WHITE);
        double now = Utils.rtime();
        if(now - lastmsgsfx > 0.1) {
            ui.sfx(RootWidget.msgsfx);
            lastmsgsfx = now;
        }
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

}
