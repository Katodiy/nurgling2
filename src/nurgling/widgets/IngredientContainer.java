package nurgling.widgets;

import haven.*;
import haven.res.lib.itemtex.*;
import haven.res.lib.layspr.*;
import nurgling.*;
import nurgling.tools.*;
import org.json.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class IngredientContainer extends Widget implements NDTarget
{
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

    public IngredientContainer()
    {
        this.sz = UI.scale(new Coord(200,400));
    }

    @Override
    public void draw(GOut g)
    {

        g.chcolor(Color.BLACK);
        g.frect(Coord.z, g.sz());
        super.draw(g);
    }

    public void addIcon(JSONObject res)
    {
        if(res!=null && res.get("name")!=null)
        {
            Ingredient ing;
            items.add(ing = new Ingredient((String)res.get("name"), ItemTex.create(res)));
            add(new IconItem(ing.name, ing.image),UI.scale(new Coord(35*((items.size()-1)%5),51*((items.size()-1)/5))).add(new Coord(5,5)));
        }
    }

    @Override
    public boolean drop(WItem item, Coord cc, Coord ul)
    {
        String name = ((NGItem) item.item).name();
        JSONObject res = ItemTex.save(item.item.spr);
        if(res!=null)
        {
            res.put("name", name);
            addIcon(res);
        }
        return true;
    }
}
