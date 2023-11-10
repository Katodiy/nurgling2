package nurgling.widgets;

import haven.*;
import haven.res.lib.itemtex.*;
import nurgling.*;

import java.awt.image.*;
import java.util.*;

public class IconItem extends Widget
{
    public static final TexI frame = new TexI(Resource.loadimg("nurgling/hud/iconframe"));
    public static final TexI framet = new TexI(Resource.loadimg("nurgling/hud/iconframet"));

    Tex tex = null;

    TexI tip;
    TexI q;

    boolean isThreshold = false;

    int val;

    public IconItem(String name, BufferedImage img)
    {
        tip = new TexI(RichText.render(name).img);

        tex = new TexI(img);
        this.sz = UI.scale(new Coord(32, 42));
    }

    public IconItem()
    {
        this.sz = UI.scale(new Coord(32, 42));
    }

    @Override
    public void draw(GOut g)
    {
        if (tex != null)
        {
            if(isThreshold)
            {
                g.image(framet, Coord.z, UI.scale(32, 42));
                g.image(q, new Coord(UI.scale(16)-q.sz().x/2,UI.scale(28)));
            }
            else
            {
                g.image(frame, Coord.z, UI.scale(32, 32));
            }
            g.image(tex, Coord.z, UI.scale(32,32));
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev)
    {
        return tip;
    }

    @Override
    public boolean mousedown(Coord c, int button)
    {
        if(button==3)
        {
            opts(c);
            return true;
        }
        else
        {
            return super.mousedown(c, button);
        }
    }

    final ArrayList<String> opt = new ArrayList<String>(){
        {
            add("Threshold");
            add("Delete");
        }
    };

    NFlowerMenu menu;

    public void opts( Coord c ) {
        if(menu == null) {
            menu = new NFlowerMenu(opt.toArray(new String[0])) {
                public boolean mousedown(Coord c, int button) {
                    if(!super.mousedown(c, button))
                        choose(null);
                    return(true);
                }

                public void destroy() {
                    menu = null;
                    super.destroy();
                }

                @Override
                public void nchoose(NPetal option)
                {
                    if(option.name.equals("Threshold"))
                    {
                        Widget par = IconItem.this.parent;
                        Coord pos = IconItem.this.c.add(UI.scale(32,38));
                        while(par!=null && !(par instanceof GameUI))
                        {
                            pos = pos.add(par.c);
                            par = par.parent;
                        }
                        SetThreshold st = new SetThreshold(val);
                        ui.root.add(st, pos);
                    }
                    uimsg("cancel");
                }

            };
            Widget par = parent;
            Coord pos = c.add(IconItem.this.c).add(UI.scale(32,38));
            while(par!=null && !(par instanceof GameUI))
            {
                pos = pos.add(par.c);
                par = par.parent;
            }
            ui.root.add(menu, pos);
        }
    }



    class SetThreshold extends Window
    {
        public SetThreshold(int val)
        {
            super(UI.scale(140,25), "Threshold");
            TextEntry te;
            prev = add(te = new TextEntry(UI.scale(80),String.valueOf(val)));
            add(new Button(UI.scale(50),"Set"){
                @Override
                public void click()
                {
                    super.click();
                    IconItem.this.isThreshold = true;
                    IconItem.this.val = Integer.valueOf(te.text());
                    IconItem.this.q = new TexI(NStyle.iiqual.render(te.text()).img);
                    ui.destroy(SetThreshold.this);
                }
            },prev.pos("ur").add(5,-5));
        }
    }
}
