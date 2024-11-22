package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import org.json.JSONObject;

import java.awt.image.*;
import java.util.*;

public class IconItem extends Widget
{
    public static final TexI frame = new TexI(Resource.loadimg("nurgling/hud/iconframe"));
    public static final TexI framet = new TexI(Resource.loadimg("nurgling/hud/iconframet"));
    public static final TexI bm = new TexI(Resource.loadimg("nurgling/hud/bartermark"));
    public static final TexI barm = new TexI(Resource.loadimg("nurgling/hud/barrelmark"));
    public JSONObject src;
    TexI tex = null;

    TexI tip;
    TexI q;
    boolean noOpts = false;
    boolean isThreshold = false;

    NArea.Ingredient.Type type = NArea.Ingredient.Type.CONTAINER;

    int val;

    String name;

    public IconItem(String name, BufferedImage img)
    {
        this.name = name;
        tip = new TexI(RichText.render(name).img);

        tex = new TexI(img);
        this.sz = UI.scale(new Coord(32, 42));
    }

    public IconItem(String name, TexI img)
    {
        this.name = name;
        tip = new TexI(RichText.render(name).img);

        tex = img;
        this.sz = UI.scale(new Coord(32, 42));
    }

    void update(String name, BufferedImage img)
    {
        this.name = name;
        tip = new TexI(RichText.render(name).img);
        tex = new TexI(img);
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
            if(type == NArea.Ingredient.Type.BARTER)
            {
                g.image(bm, UI.scale(16,16), UI.scale(16, 16));
            }
            if(type == NArea.Ingredient.Type.BARREL)
            {
                g.image(barm, UI.scale(16,16), UI.scale(16, 16));
            }
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
            if(!noOpts)
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
            add("Mark as barter");
            add("Mark as barrel");
        }
    };

    final ArrayList<String> uopt = new ArrayList<String>(){
        {
            add("Threshold");
            add("Delete");
            add("Unmark");
        }
    };

    NFlowerMenu menu;

    public void opts( Coord c ) {
        if(menu == null) {
            menu = new NFlowerMenu((type==NArea.Ingredient.Type.CONTAINER)?opt.toArray(new String[0]):uopt.toArray(new String[0])) {
                public boolean mousedown(Coord c, int button) {
                    if(super.mousedown(c, button))
                        nchoose(null);
                    return(true);
                }

                public void destroy() {
                    menu = null;
                    super.destroy();
                }

                @Override
                public void nchoose(NPetal option)
                {
                    if(option!=null)
                    {
                        if (option.name.equals("Threshold"))
                        {
                            Widget par = IconItem.this.parent;
                            Coord pos = IconItem.this.c.add(UI.scale(32, 38));
                            while (par != null && !(par instanceof GameUI))
                            {
                                pos = pos.add(par.c);
                                par = par.parent;
                            }
                            SetThreshold st = new SetThreshold(val);
                            ui.root.add(st, pos);

                        }
                        else if(option.name.equals("Delete"))
                        {
                            ((IngredientContainer)IconItem.this.parent).delete(IconItem.this.name);
                        }
                        else if(option.name.equals("Mark as barter"))
                        {
                            ((IngredientContainer)IconItem.this.parent).setType(IconItem.this.name, NArea.Ingredient.Type.BARTER);
                        }
                        else if(option.name.equals("Mark as barrel"))
                        {
                            ((IngredientContainer)IconItem.this.parent).setType(IconItem.this.name, NArea.Ingredient.Type.BARREL);
                        }
                        else if(option.name.equals("Unmark"))
                        {
                            ((IngredientContainer)IconItem.this.parent).setType(IconItem.this.name, NArea.Ingredient.Type.CONTAINER);
                        }
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

    public JSONObject toJson() {
        return src;
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
                    try
                    {
                        IconItem.this.isThreshold = true;
                        IconItem.this.val = Integer.valueOf(te.text());
                        IconItem.this.q = new TexI(NStyle.iiqual.render(te.text()).img);
                        ((IngredientContainer)IconItem.this.parent).setThreshold(IconItem.this.name,IconItem.this.val);
                    }
                    catch (NumberFormatException e)
                    {
                        IconItem.this.isThreshold = false;
                        ((IngredientContainer)IconItem.this.parent).setThreshold(IconItem.this.name,-1);
                    }
                    ui.destroy(SetThreshold.this);

                }
            },prev.pos("ur").add(5,-5));
        }

        @Override
        public void wdgmsg(String msg, Object... args)
        {
            if(msg.equals("close"))
            {
                destroy();
            }
            else
            {
                super.wdgmsg(msg, args);
            }
        }
    }
}
