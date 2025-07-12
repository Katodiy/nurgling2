package nurgling.widgets.options;

import haven.Button;
import haven.Label;
import haven.*;
import nurgling.NConfig;
import nurgling.NStyle;
import nurgling.widgets.nsettings.Panel;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AutoSelection extends Panel {
    public ArrayList<AutoSelectItem> petals = new ArrayList<>();
    ActionList al;
    TextEntry newPetall;
    int width = UI.scale(210);
    private CheckBox autoSelectEnabled;
    private CheckBox singlePetal;

    public AutoSelection() {
        super("");

        final int margin = UI.scale(10);

        prev = autoSelectEnabled = add(new CheckBox("Auto selection enabled"), new Coord(margin, margin));

        prev = add(new Label("Auto selected petals:"), prev.pos("bl").adds(0, 5));
        prev = add(al = new ActionList(new Coord(width, UI.scale(300))), prev.pos("bl").adds(0, 10));

        newPetall = add(new TextEntry(UI.scale(150), ""), prev.pos("bl").adds(0, 10));
        add(new Button(UI.scale(45), "Add") {
            @Override
            public void click() {
                if (!newPetall.text().isEmpty()) {
                    AutoSelectItem ai = new AutoSelectItem(newPetall.text());
                    ai.isEnabled.a = true;
                    petals.add(ai);
                }
            }
        }, newPetall.pos("ur").adds(10, 0));

        singlePetal = add(new CheckBox("Auto select single petal"), newPetall.pos("bl").adds(0, 10));

        load();
        pack();
    }

    @Override
    public void load() {
        petals.clear();
        autoSelectEnabled.a = getBool(NConfig.Key.asenable);

        if (NConfig.get(NConfig.Key.petals) != null) {
            for (HashMap<String, Object> item : (ArrayList<HashMap<String, Object>>) NConfig.get(NConfig.Key.petals)) {
                AutoSelectItem aitem = new AutoSelectItem((String) item.get("name"));
                aitem.isEnabled.a = (Boolean) item.get("enabled");
                petals.add(aitem);
            }
        }

        singlePetal.a = getBool(NConfig.Key.singlePetal);
        if (al != null)
            al.update();
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.asenable, autoSelectEnabled.a);

        ArrayList<HashMap<String, Object>> plist = new ArrayList<>();
        for (AutoSelectItem petal : petals) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", petal.text());
            map.put("enabled", petal.isEnabled.a);
            plist.add(map);
        }
        NConfig.set(NConfig.Key.petals, plist);

        NConfig.set(NConfig.Key.singlePetal, singlePetal.a);

        NConfig.needUpdate();
    }

    class ActionList extends SListBox<AutoSelectItem, Widget> {
        ActionList(Coord sz) {
            super(sz, UI.scale(22));
        }

        protected List<AutoSelectItem> items() {
            return petals;
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(width - UI.scale(6), sz.y));
        }

        protected Widget makeitem(AutoSelectItem item, int idx, Coord sz) {
            return new ItemWidget<AutoSelectItem>(this, sz, item) {
                {
                    add(item);
                    item.resize(sz);
                }

                public boolean mousedown(Coord c, int button) {
                    boolean psel = sel == item;
                    super.mousedown(c, button);
                    if (!psel) {
                        String value = item.text.text();
                    }
                    return (true);
                }
            };
        }

        Color bg = new Color(30, 40, 40, 160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            g.chcolor();
            super.draw(g);
        }
    }

    public class AutoSelectItem extends Widget {
        Label text;
        IButton remove;
        public CheckBox isEnabled;

        @Override
        public void resize(Coord sz) {
            isEnabled.move(new Coord(isEnabled.c.x, (sz.y - isEnabled.sz.y) / 2));
            text.move(new Coord(text.c.x, (sz.y - text.sz.y) / 2));
            remove.move(new Coord(sz.x - NStyle.removei[0].sz().x - UI.scale(5),
                    (sz.y - remove.sz.y) / 2));
            super.resize(sz);
        }

        public AutoSelectItem(String text) {
            prev = isEnabled = add(new CheckBox("") {
                public void set(boolean val) {
                    a = val;
                }
            });
            this.text = add(new Label(text), prev.pos("ur").add(UI.scale(2), 0));
            remove = add(new IButton(NStyle.removei[0].back, NStyle.removei[1].back, NStyle.removei[2].back) {
                @Override
                public void click() {
                    petals.remove(AutoSelectItem.this);
                }
            }, new Coord(al.sz.x - NStyle.removei[0].sz().x, 0).sub(UI.scale(5), UI.scale(1)));
            remove.settip(Resource.remote().loadwait("nurgling/hud/buttons/removeItem/u").flayer(Resource.tooltip).t);

            pack();
        }

        public String text() {
            return text.text();
        }
    }

    private boolean getBool(NConfig.Key key) {
        Object val = NConfig.get(key);
        return val instanceof Boolean ? (Boolean) val : false;
    }
}
