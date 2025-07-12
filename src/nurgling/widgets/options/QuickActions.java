package nurgling.widgets.options;

import haven.*;
import haven.Button;
import haven.Label;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.widgets.nsettings.Panel;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuickActions extends Panel {
    public ArrayList<ActionsItem> patterns = new ArrayList<>();
    ActionList al;
    TextEntry newPattern;
    int width = UI.scale(210);
    private HSlider rangeSlider;
    private Label dpy;
    private CheckBox visitorCheck;
    private CheckBox doorCheck;

    public QuickActions() {
        final int margin = UI.scale(10);

        prev = add(al = new ActionList(new Coord(width, UI.scale(300))), new Coord(margin, margin));
        prev = add(newPattern = new TextEntry(UI.scale(150), ""), prev.pos("bl").adds(0, 10));
        add(new Button(UI.scale(45), "Add") {
            @Override
            public void click() {
                if (!newPattern.text().isEmpty()) {
                    ActionsItem ai = new ActionsItem(newPattern.text());
                    ai.isEnabled.a = true;
                    patterns.add(ai);
                }
            }
        }, newPattern.pos("ur").adds(10, 0));

        dpy = new Label("");
        rangeSlider = new HSlider(UI.scale(160), 1, 10, 1) {
            protected void added() {
                updateDpyLabel();
            }
            public void changed() {
                updateDpyLabel();
            }
        };

        // Correctly update prev for chaining layout
        addhlp(prev.pos("bl").adds(0, UI.scale(10)), UI.scale(5), rangeSlider, dpy);
        prev = rangeSlider;
        prev.settip("Set range of quick actions in tiles.", true);

        prev = visitorCheck = add(new CheckBox("Disable opening/closing visitor gates"), prev.pos("bl").adds(0, 5));
        prev = doorCheck = add(new CheckBox("Walking into doors in basic mode"), prev.pos("bl").adds(0, 5));
        pack();
        load();
    }

    private void updateDpyLabel() {
        if (dpy != null && rangeSlider != null)
            dpy.settext(rangeSlider.val + " tiles");
    }

    @Override
    public void load() {
        patterns.clear();
        if (NConfig.get(NConfig.Key.q_pattern) != null) {
            for (HashMap<String, Object> item : (ArrayList<HashMap<String, Object>>) NConfig.get(NConfig.Key.q_pattern)) {
                ActionsItem aitem = new ActionsItem((String) item.get("name"));
                aitem.isEnabled.a = (Boolean) item.get("enabled");
                patterns.add(aitem);
            }
        }
        int range = 1;
        Object rv = NConfig.get(NConfig.Key.q_range);
        if (rv instanceof Integer)
            range = (Integer) rv;
        rangeSlider.val = range;
        updateDpyLabel();
        visitorCheck.a = getBool(NConfig.Key.q_visitor);
        doorCheck.a = getBool(NConfig.Key.q_door);
        if (al != null)
            al.update();
    }

    @Override
    public void save() {
        ArrayList<HashMap<String, Object>> plist = new ArrayList<>();
        for (ActionsItem pattern : patterns) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", pattern.text());
            map.put("enabled", pattern.isEnabled.a);
            plist.add(map);
        }
        NConfig.set(NConfig.Key.q_pattern, plist);
        NConfig.set(NConfig.Key.q_range, rangeSlider.val);
        NConfig.set(NConfig.Key.q_visitor, visitorCheck.a);
        NConfig.set(NConfig.Key.q_door, doorCheck.a);
        NConfig.needUpdate();
    }

    class ActionList extends SListBox<ActionsItem, Widget> {
        ActionList(Coord sz) {
            super(sz, UI.scale(22));
        }

        protected List<ActionsItem> items() {
            return patterns;
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(width - UI.scale(6), sz.y));
        }

        protected Widget makeitem(ActionsItem item, int idx, Coord sz) {
            return new ItemWidget<ActionsItem>(this, sz, item) {
                {
                    add(item);
                    item.resize(sz);
                }

                @Override
                public void resize(Coord sz) {
                    super.resize(sz);
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

        @Override
        public void wdgmsg(String msg, Object... args) {
            super.wdgmsg(msg, args);
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

    public class ActionsItem extends Widget {
        Label text;
        IButton remove;
        public CheckBox isEnabled;

        public NArea area;

        @Override
        public void resize(Coord sz) {
            if (isEnabled != null)
                isEnabled.move(new Coord(isEnabled.c.x, (sz.y - isEnabled.sz.y) / 2));
            if (text != null)
                text.move(new Coord(text.c.x, (sz.y - text.sz.y) / 2));
            if (remove != null)
                remove.move(new Coord(sz.x - NStyle.removei[0].sz().x - UI.scale(5),
                        (sz.y - remove.sz.y) / 2));
            super.resize(sz);
        }

        public ActionsItem(String text) {
            prev = isEnabled = add(new CheckBox("") {
                public void set(boolean val) {
                    a = val;
                }
            });
            this.text = add(new Label(text), prev.pos("ur").add(UI.scale(2), 0));
            remove = add(new IButton(NStyle.removei[0].back, NStyle.removei[1].back, NStyle.removei[2].back) {
                @Override
                public void click() {
                    patterns.remove(ActionsItem.this);
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
