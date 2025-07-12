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

public class IngredientContainer extends BaseIngredientContainer {
    protected Integer id = -1;
    public IngredientContainer(String type) {
        super(type);
    }

    public static class RuleButton extends Button {
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

        final ArrayList<String> opt = new ArrayList<String>(){{
            add("Set Thresholds");
            add("Delete Thresholds");
            add("Clear");
        }};

        public void draw(BufferedImage img) {
            Graphics g = img.getGraphics();
            Coord tc = sz.sub(Utils.imgsz(cont)).div(2);
            g.drawImage(cont, tc.x, tc.y, null);
            g.dispose();
        }

        class SetThreshold extends Window {
            public SetThreshold(int val) {
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
            public void wdgmsg(String msg, Object... args) {
                if(msg.equals("close")) {
                    destroy();
                } else {
                    super.wdgmsg(msg, args);
                }
            }
        }

        public void opts(Coord c) {
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
                    public void nchoose(NPetal option) {
                        if(option != null) {
                            if (option.name.equals("Set Thresholds")) {
                                SetThreshold st = new SetThreshold(0);
                                ui.root.add(st, c);
                            } else if(option.name.equals("Delete Thresholds")) {
                                for (IconItem item : ic.icons) {
                                    item.isThreshold = false;
                                    item.val = 1;
                                    item.q = null;
                                    ic.delThreshold(item.name);
                                }
                            } else if(option.name.equals("Clear")) {
                                ic.deleteAll();
                            }
                        }
                        uimsg("cancel");
                    }
                };
                Widget par = parent;
                Coord pos = c;
                while(par != null && !(par instanceof GameUI)) {
                    pos = c.add(par.c);
                    par = par.parent;
                }
                ui.root.add(menu, pos);
            }
        }
    }

    @Override
    public void addIcon(JSONObject res) {
        super.addIcon(res);
        if(res.has("th")) {
            IconItem it = icons.get(icons.size()-1);
            it.isThreshold = true;
            it.val = (Integer)res.get("th");
            it.q = new TexI(NStyle.iiqual.render(String.valueOf(it.val)).img);
        }
        if(res.has("type")) {
            IconItem it = icons.get(icons.size()-1);
            it.type = NArea.Ingredient.Type.valueOf((String)res.get("type"));
        }
    }

    @Override
    public void addItem(String name, JSONObject res) {
        if(res != null) {
            JSONArray data;
            if(NUtils.getArea(id) == null) return;
            if(type.equals("in"))
                data = NUtils.getArea(id).jin;
            else
                data = NUtils.getArea(id).jout;

            boolean find = false;
            for(int i = 0; i < data.length(); i++) {
                if(((JSONObject) data.get(i)).get("name").equals(name)) {
                    find = true;
                    break;
                }
            }
            if(!find) {
                res.put("name", name);
                res.put("type", NArea.Ingredient.Type.CONTAINER.toString());
                addIcon(res);
                data.put(res);
                NConfig.needAreasUpdate();
            }
        }
    }

    public void load(Integer id) {
        this.id = id;
        items.clear();
        for(IconItem it : icons) {
            it.destroy();
        }
        icons.clear();
        if(id != -1) {
            JSONArray data;
            if(type.equals("in"))
                data = NUtils.getArea(id).jin;
            else
                data = NUtils.getArea(id).jout;

            for(int i = 0; i < data.length(); i++) {
                addIcon(((JSONObject) data.get(i)));
            }
        }
    }


    @Override
    public boolean drop(Drop ev) {
        if(id != -1) {
            String name = ((NGItem) ev.src.item).name();
            JSONObject res = ItemTex.save(((NGItem) ev.src.item).spr);
            addItem(name, res);
        }
        return super.drop(ev);
    }

    public void setThreshold(String name, int val) {
        JSONArray data;
        if(NUtils.getArea(id) == null) return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        for(int i = 0; i < data.length(); i++) {
            if(((JSONObject) data.get(i)).get("name").equals(name)) {
                ((JSONObject) data.get(i)).put("th",val);
                NConfig.needAreasUpdate();
                return;
            }
        }
    }

    public void delThreshold(String name) {
        JSONArray data;
        if(NUtils.getArea(id) == null) return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        for(int i = 0; i < data.length(); i++) {
            if(((JSONObject) data.get(i)).get("name").equals(name)) {
                ((JSONObject) data.get(i)).remove("th");
                NConfig.needAreasUpdate();
                return;
            }
        }
    }

    public void setType(String name, NArea.Ingredient.Type val) {
        JSONArray data;
        if(NUtils.getArea(id) == null) return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        for(int i = 0; i < data.length(); i++) {
            if(((JSONObject) data.get(i)).get("name").equals(name)) {
                ((JSONObject) data.get(i)).put("type",val.toString());
                icons.get(i).type = val;
                NConfig.needAreasUpdate();
                return;
            }
        }
    }

    @Override
    public void delete(String name) {
        JSONArray data;
        if(NUtils.getArea(id) == null) return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        for(int i = 0; i < data.length(); i++) {
            if(((JSONObject) data.get(i)).get("name").equals(name)) {
                data.remove(i);
                NConfig.needAreasUpdate();
                load(id);
                return;
            }
        }
    }

    @Override
    public void deleteAll() {
        JSONArray data;
        if(NUtils.getArea(id) == null) return;
        if(type.equals("in"))
            data = NUtils.getArea(id).jin;
        else
            data = NUtils.getArea(id).jout;

        data.clear();
        NConfig.needAreasUpdate();
        load(id);
    }
}