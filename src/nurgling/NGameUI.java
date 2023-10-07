package nurgling;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.res.ui.rbuff.*;
import nurgling.tools.*;
import nurgling.widgets.*;

import java.awt.*;
import java.util.*;

public class NGameUI extends GameUI
{

    NBotsMenu botsMenu;
    public NSearchItem itemsForSearch = null;

    public NGameUI(String chrid, long plid, String genus, NUI nui)
    {
        super(chrid, plid, genus, nui);
        itemsForSearch = new NSearchItem();
        for(int i = 0; i<(Integer)NConfig.get(NConfig.Key.numbelts); i++)
        {
            String name = "belt" + String.valueOf(i);
            NDraggableWidget belt = add(new NDraggableWidget(new NToolBelt(name, i * 12, 4, 12), name, UI.scale(new Coord(500, 56))));
            belt.setFlipped(true);
        }

        add(new NDraggableWidget(botsMenu = new NBotsMenu(), "botsmenu", botsMenu.sz.add(NDraggableWidget.delta)));
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
        super.addchild(child, args);
        String place = ((String) args[0]).intern();
        if (place.equals("chr") && chrwdg != null) {
            ((NUI) ui).sessInfo.characterInfo.setCharWnd(chrwdg);
        }
        if (maininv != null && ((NInventory) maininv).searchwdg == null)
        {
            ((NInventory) maininv).installMainInv();
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
}
