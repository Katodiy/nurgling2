package nurgling.widgets;

import haven.*;
import haven.res.lib.itemtex.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.tools.*;
import org.json.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class IngredientContainer extends Widget implements NDTarget
{
    static Color bg = new Color(30,40,40,160);


    public class Ingredient{
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
        this.sz = UI.scale(new Coord(200,400));
    }

    String type;

    @Override
    public void draw(GOut g)
    {

        g.chcolor(bg);
        g.frect(Coord.z, g.sz());
        super.draw(g);
    }

    public void addIcon(JSONObject res)
    {
        if(res!=null && res.get("name")!=null)
        {
            Ingredient ing;
            items.add(ing = new Ingredient((String)res.get("name"), ItemTex.create(res)));
            IconItem it = add(new IconItem(ing.name, ing.image),UI.scale(new Coord(35*((items.size()-1)%5),51*((items.size()-1)/5))).add(new Coord(5,5)));
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
        }
    }

    ArrayList<IconItem> icons = new ArrayList<>();

    @Override
    public boolean drop(WItem item, Coord cc, Coord ul)
    {
        if(id!=-1)
        {
            String name = ((NGItem) item.item).name();
            JSONObject res = ItemTex.save(item.item.spr);
            addItem(name, res);
        }
        return true;
    }

    public void addItem(String name, JSONObject res)
    {
        if (res != null)
        {
            JSONArray data;
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

    public void setType(String name, NArea.Ingredient.Type val)
    {
        JSONArray data;
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
}
