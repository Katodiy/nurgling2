package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.res.lib.itemtex.ItemTex;
import nurgling.NConfig;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.conf.NChopperProp;
import nurgling.conf.NSmokProp;
import nurgling.widgets.bots.Checkable;
import nurgling.widgets.options.QuickActions;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static haven.TextEntry.mext;

public class SmokingSettings extends Window implements Checkable {
    SmockedContainer sc;
    TextEntry textEntry;
    RecipesList rl;
    public SmokingSettings() {
        super(UI.scale(500, 200), "Smoking Settings");
        prev = add(new Label("Smoking properties for:"));
        prev = add(sc = new SmockedContainer(), prev.pos("bl").add(UI.scale(0,5)));
        add(textEntry = new TextEntry(200,""),prev.pos("ur").add(UI.scale(10,sc.sz.y/2-mext.sz().y/2)));
        add(new Button(100,"Add"){
            @Override
            public void click() {
                super.click();
                synchronized (recipes)
                {
                    if(sc.iconItem!=null) {
                        IconItem item;
                        for (Recipe r : recipes) {
                            if (r.name.text().equalsIgnoreCase(sc.iconItem.name)) {
                                recipes.remove(r);
                                break;
                            }
                        }
                        recipes.add(new Recipe(item = new IconItem(sc.iconItem.name, sc.iconItem.tex), textEntry.text()));
                        item.src = sc.iconItem.src;
                    }
                }
            }
        },textEntry.pos("ur").add(UI.scale(5,-7)));
        prev = add(rl = new RecipesList(UI.scale(480, 150)), prev.pos("bl").add(UI.scale(0,5)));
        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
                isReady = true;
                ArrayList<NSmokProp> props = (ArrayList<NSmokProp>)NConfig.get(NConfig.Key.smokeprop);
                if(props!=null) {
                    props.clear();
                }
                for(Recipe r : recipes)
                {
                    NSmokProp.set(new NSmokProp(r.iconItem,r.fuel,r.isEnabled.a));
                }
                NConfig.needUpdate();
            }
        }, prev.pos("bl").add(UI.scale(0,5)));

        pack();
        recipes.clear();
        ArrayList<NSmokProp> smprop = (ArrayList<NSmokProp>)NConfig.get(NConfig.Key.smokeprop);
        if(smprop!=null)
        {
            for(NSmokProp p : smprop)
            {
                JSONObject icon = (JSONObject)((JSONObject) p.toJson().get("data")).get("icon");
                IconItem item = new IconItem(p.iconName, ItemTex.create(icon));
                item.src = icon;
                Recipe recipe = new Recipe(item,(String) ((JSONObject) p.toJson().get("data")).get("fuel"));
                recipe.isEnabled.a = (Boolean) ((JSONObject) p.toJson().get("data")).get("isSelected");
                recipes.add(recipe);
            }
        }
    }

    final ArrayList<Recipe> recipes = new ArrayList<Recipe>();

    @Override
    public boolean check() {
        return isReady;
    }

    boolean isReady = false;

    class RecipesList extends SListBox<Recipe, Widget> {
        RecipesList(Coord sz) {
            super(sz, UI.scale(15));
        }

        protected List<Recipe> items() {
            return recipes;
        }

        protected Widget makeitem(Recipe item, int idx, Coord sz) {
            return (new ItemWidget<Recipe>(this, sz, item) {
                {
                    add(item);
                }

                public boolean mousedown(Coord c, int button) {
                    boolean psel = sel == item;
                    boolean res = super.mousedown(c, button);
                    if (!psel ) {
                        textEntry.settext(item.fuel);
                        sc.addItem(item.iconItem.name, item.iconItem.src);
                    }
                    return (res);
                }
            });
        }

        @Override
        public void wdgmsg(String msg, Object... args) {
            super.wdgmsg(msg, args);
        }

        Color bg = new Color(30, 40, 40, 160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }
    }

    public class Recipe extends Widget {
        IconItem iconItem;
        Label name;
        Label fuelw;
        String fuel;
        IButton remove;
        public CheckBox isEnabled;

        public NArea area;

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(rl.sz.x, sz.y));
            remove.move(new Coord( rl.sz.x - NStyle.removei[0].sz().x - UI.scale(5), remove.c.y));

        }

        public Recipe(IconItem iconItem, String text) {
            this.iconItem = iconItem;
            this.fuel = text;
            prev = isEnabled = add(new CheckBox("") {
                public void set(boolean val) {
                    a = val;
                }
            });
            this.name = add(new Label(iconItem.name), prev.pos("ur").add(UI.scale(22), 0));
            this.fuelw = add(new Label(" - " + text), name.pos("ur").add(UI.scale(2), 0));
            remove = add(new IButton(NStyle.removei[0].back, NStyle.removei[1].back, NStyle.removei[2].back) {
                @Override
                public void click() {
                    recipes.remove(Recipe.this);
                }
            }, new Coord(/*al.sz.x*/ - NStyle.removei[0].sz().x, 0).sub(UI.scale(5), UI.scale(1)));
            remove.settip(Resource.remote().loadwait("nurgling/hud/buttons/removeItem/u").flayer(Resource.tooltip).t);

            pack();
        }

        @Override
        public void draw(GOut g, boolean strict) {
            super.draw(g, strict);
            g.image(iconItem.tex, UI.scale(16,0), UI.scale(16,16));
        }
    }

    @Override
    public void show() {
        recipes.clear();
        ArrayList<NSmokProp> smprop = (ArrayList<NSmokProp>)NConfig.get(NConfig.Key.smokeprop);
        if(smprop!=null)
        {
            for(NSmokProp p : smprop)
            {
                JSONObject icon = (JSONObject)((JSONObject) p.toJson().get("data")).get("icon");
                IconItem item = new IconItem(p.iconName, ItemTex.create(icon));
                item.src = icon;
                Recipe recipe = new Recipe(item,(String) ((JSONObject) p.toJson().get("data")).get("fuel"));
                recipe.isEnabled.a = (Boolean) ((JSONObject) p.toJson().get("data")).get("isSelected");
                recipes.add(recipe);
            }
        }
        super.show();
    }
}
