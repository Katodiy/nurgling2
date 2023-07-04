package nurgling;

import haven.*;
import nurgling.tools.*;
import nurgling.widgets.*;

import java.util.*;

public class NGameUI extends GameUI
{
    public NGameUI(String chrid, long plid, String genus, NUI nui)
    {
        super(chrid, plid, genus, nui);
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
                    if (pbuff instanceof Buff)
                    {
                        if (NParser.checkName(((Buff) pbuff).res.get().name, new NAlias("realm")))
                        {
                            ArrayList<ItemInfo> realm = new ArrayList<>(((Buff) pbuff).info());
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

}
