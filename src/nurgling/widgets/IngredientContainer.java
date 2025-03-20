package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Scrollbar;
import haven.Window;
import haven.res.lib.itemtex.*;
import nurgling.*;
import nurgling.areas.*;
import org.json.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class IngredientContainer extends Widget implements DTarget, Scrollable
{

    public static class RuleButton extends Button
    {
        NFlowerMenu menu;

        final IngredientContainer ic;
        public RuleButton(IngredientContainer ing) {
            super(UI.scale(30), Resource.loadsimg("nurgling/hud/buttons/settings/u"));
            this.ic = ing;
        }

        @Override
        public void click() {
            super.click();
            opts(this.c);
        }

        final ArrayList<String> opt = new ArrayList<String>(){
            {
                add("Set Thresholds");
                add("Delete Thresholds");
                add("Clear");
            }
        };

        public void draw(BufferedImage img) {
            Graphics g = img.getGraphics();
            Coord tc = sz.sub(Utils.imgsz(cont)).div(2);
            g.drawImage(cont, tc.x, tc.y, null);
            g.dispose();
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
                    public void click() {
                        super.click();
                        try {
                            int val = Integer.parseInt(te.text());
                            for (IconItem item : ic.icons) {

                                item.isThreshold = true;
                                item.val = val;
                                item.q = new TexI(NStyle.iiqual.render(te.text()).img);
                                ic.setThreshold(item.name, item.val);
                            }
                        } catch (NumberFormatException ignored) {
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

        public void opts( Coord c ) {
            if(menu == null) {
                menu = new NFlowerMenu(opt.toArray(new String[0])) {
                    public boolean mousedown(MouseDownEvent ev) {
                        if(super.mousedown(ev))
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
                            if (option.name.equals("Set Thresholds"))
                            {
                                SetThreshold st = new SetThreshold(0);
                                ui.root.add(st, c);

                            }
                            else if(option.name.equals("Delete Thresholds"))
                            {
                                for (IconItem item : ic.icons) {

                                    item.isThreshold = false;
                                    item.val = 1;
                                    item.q = null;
                                    ic.delThreshold(item.name);
                                }
                            }
                            else if(option.name.equals("Clear")) {
                                ic.deleteAll();
                            }
                        }
                        uimsg("cancel");
                    }

                };
                Widget par = parent;
                Coord pos = c;
                while(par!=null && !(par instanceof GameUI))
                {
                    pos = c.add(par.c);
                    par = par.parent;
                }
                ui.root.add(menu, pos);
            }
        }
    }


    static Color bg = new Color(30,40,40,160);

    private static TexI freeLabel = new TexI(Text.render("Drag and drop an item here").img);
    private static TexI freeLabel2 = new TexI(Text.render("or select it from the categories").img);

    public Scrollbar sb;
    private int maxy = 0, cury = 0;

    @Override
    public int scrollmin() {
        return 0;
    }
    @Override
    public int scrollmax() {return(maxy);}
    @Override
    public int scrollval() {return(cury);}
    @Override
    public void scrollval(int val) {
        cury = val;
        for(IconItem icon: icons)
        {
            icon.c.y = icon.basec.y-val;
        }
    }


    public static class Ingredient{
        public String name;
        public BufferedImage image;

        public Ingredient(String name, BufferedImage image)
        {
            this.name = name;
            this.image = image;
        }
    }

    ArrayList<Ingredient> items = new ArrayList<>();

    public IngredientContainer(String type)
    {
        this.type = type;
        this.sb = add(new Scrollbar(UI.scale(385), this));
        this.sz = UI.scale(new Coord(205,400));
        this.sb.c= new Coord(UI.scale(180),0);
    }

    String type;

    @Override
    public void draw(GOut g)
    {
        g.chcolor(bg);
        g.frect(Coord.z, g.sz());
        if(items.isEmpty())
        {
            g.chcolor(Color.WHITE);
            g.image(freeLabel,UI.scale(5,5));
            g.image(freeLabel2,UI.scale(5,20));
        }
        super.draw(g);
    }

    public void addIcon(JSONObject res)
    {
        if(res!=null && res.get("name")!=null)
        {
            Ingredient ing;
            items.add(ing = new Ingredient((String)res.get("name"), ItemTex.create(res)));
            IconItem it = add(new IconItem(ing.name, ing.image),UI.scale(new Coord(35*((items.size()-1)%5),51*((items.size()-1)/5))).add(new Coord(5,5)));
            it.basec = new Coord(it.c);
            if(res.has("th"))
            {
                it.isThreshold = true;
                it.val = (Integer)res.get("th");
                it.q = new TexI(NStyle.iiqual.render(String.valueOf(it.val)).img);
            }
            if(res.has("type"))
            {
                it.type = NArea.Ingredient.Type.valueOf((String)res.get("type"));
            }
            icons.add(it);
            maxy = UI.scale(51)*((items.size()-1)/5 - 5);
            cury = Math.min(cury, Math.max(maxy, 0));
        }
    }

    ArrayList<IconItem> icons = new ArrayList<>();

    @Override
    public boolean drop(Drop ev) {
        if(id!=-1)
        {
            String name = ((NGItem) ev.src.item).name();
            JSONObject res = ItemTex.save(((NGItem) ev.src.item).spr);
            addItem(name, res);
        }
        return DTarget.super.drop(ev);

    }

    public void addItem(String name, JSONObject res)
    {
        if (res != null)
        {
            JSONArray data;
            if(NUtils.getArea(id)==null)
                return;
            if(type.equals("in"))
                data = NUtils.getArea(id).jin;
            else
                data = NUtils.getArea(id).jout;
            boolean find = false;
            for (int i = 0; i < data.length(); i++)
            {
                if (((JSONObject) data.get(i)).get("name").equals(name))
                {
                    find = true;
                    break;
                }
            }
            if(!find)
            {
                res.put("name", name);
                res.put("type", NArea.Ingredient.Type.CONTAINER.toString());
                addIcon(res);
                data.put(res);
                NConfig.needAreasUpdate();
            }

        }
    }

    Integer id = -1;

    public void load(Integer id)
    {
        this.id = id;
        items.clear();
        for (IconItem it : icons)
        {
            it.destroy();
        }
        icons.clear();
        if (id != -1)
        {
            JSONArray data;
            if (type.equals("in"))
                data = NUtils.getArea(id).jin;
            else
                data = NUtils.getArea(id).jout;
            for (int i = 0; i < data.length(); i++)
            {
                addIcon(((JSONObject) data.get(i)));
            }
        }
    }


    public void setThreshold(String name, int val)
    {
        JSONArray data;
        if(NUtils.getArea(id)==null)
            return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        for (int i = 0; i < data.length(); i++)
        {
            if (((JSONObject) data.get(i)).get("name").equals(name))
            {
                ((JSONObject) data.get(i)).put("th",val);
                NConfig.needAreasUpdate();
                return;
            }
        }
    }

    public void delThreshold(String name)
    {
        JSONArray data;
        if(NUtils.getArea(id)==null)
            return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        for (int i = 0; i < data.length(); i++)
        {
            if (((JSONObject) data.get(i)).get("name").equals(name))
            {
                ((JSONObject) data.get(i)).remove("th");
                NConfig.needAreasUpdate();
                return;
            }
        }
    }

    public void setType(String name, NArea.Ingredient.Type val)
    {
        JSONArray data;
        if(NUtils.getArea(id)==null)
            return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        for (int i = 0; i < data.length(); i++)
        {
            if (((JSONObject) data.get(i)).get("name").equals(name))
            {
                ((JSONObject) data.get(i)).put("type",val.toString());
                icons.get(i).type = val;
                NConfig.needAreasUpdate();
                return;
            }
        }
    }


    public void delete(String name)
    {
        JSONArray data;
        if(NUtils.getArea(id)==null)
            return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        for (int i = 0; i < data.length(); i++)
        {
            if (((JSONObject) data.get(i)).get("name").equals(name))
            {
                data.remove(i);
                NConfig.needAreasUpdate();
                load(id);
                return;
            }
        }
    }

    public void deleteAll()
    {
        JSONArray data;
        if(NUtils.getArea(id)==null)
            return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        data.clear();
        NConfig.needAreasUpdate();
        load(id);
    }
}
