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

    public AutoSelection() {
        super("");

        final int margin = UI.scale(10);

        prev = add(new CheckBox("Auto selection enabled") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.asenable);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.asenable, val);
                a = val;
            }
        }, new Coord(margin, margin));

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

        if (NConfig.get(NConfig.Key.petals) != null) {
            for (HashMap<String, Object> item : (ArrayList<HashMap<String, Object>>) NConfig.get(NConfig.Key.petals)) {
                AutoSelectItem aitem = new AutoSelectItem((String) item.get("name"));
                aitem.isEnabled.a = (Boolean) item.get("enabled");
                petals.add(aitem);
            }
        }

        prev = add(new CheckBox("Auto select single petal") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.singlePetal);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.singlePetal, val);
                a = val;
            }
        }, newPetall.pos("bl").adds(0, 10));

        pack();
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
}
