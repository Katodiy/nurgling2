package nurgling.widgets.options;

import haven.*;
import haven.Button;
import haven.Label;
import nurgling.*;
import nurgling.areas.NArea;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuickActions extends Widget {
    public ArrayList<ActionsItem> patterns = new ArrayList<>();
    ActionList al;
    TextEntry newPattern;
    int width = UI.scale(210);
    public QuickActions() {
        prev = add(new Label("Patterns:"), Coord.z);
        prev = add( al = new ActionList(new Coord(width,UI.scale(300))), prev.pos("bl").add(0,UI.scale(10)));
        prev = add(newPattern = new TextEntry(UI.scale(150), ""), prev.pos("bl").add(0,UI.scale(10)));
        add(new Button(UI.scale(45),"Add"){
            @Override
            public void click() {
                if(!newPattern.text().isEmpty()) {
                    ActionsItem ai = new ActionsItem(newPattern.text());
                    ai.isEnabled.a = true;
                    patterns.add(ai);
                }
            }
        }, prev.pos("ur").add(UI.scale(10), UI.scale(prev.sz.y/2) - Button.hs/2));
        if(NConfig.get(NConfig.Key.q_pattern)!=null) {
            for (HashMap<String,Object> item : (ArrayList< HashMap<String,Object>>) NConfig.get(NConfig.Key.q_pattern))
            {
                ActionsItem aitem = new ActionsItem((String) item.get("name"));
                aitem.isEnabled.a = (Boolean) item.get("enabled");
                patterns.add(aitem);
            }
        }

        Label dpy = new Label("");
        addhlp(prev.pos("bl").adds(0, UI.scale(10)), UI.scale(5),
                prev = new HSlider(UI.scale(160), 1, 10, (Integer)NConfig.get(NConfig.Key.q_range)) {
                    protected void added() {
                        dpy();
                    }
                    void dpy() {
                        dpy.settext(this.val + " tiles");
                    }
                    public void changed() {
                        NConfig.set(NConfig.Key.q_range, val);
                        dpy();
                    }
                }, dpy);
        prev.settip("Set range of quick actions in tiles.", true);
        prev = add(new CheckBox("Disable opening/closing visitor gates") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.q_visitor);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.q_visitor, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        prev = add(new CheckBox("Walking into doors in basic mode") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.q_door);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.q_door, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        pack();
    }


    class ActionList extends SListBox<ActionsItem, Widget> {
        ActionList(Coord sz) {
            super(sz, UI.scale(15));
        }

        protected List<ActionsItem> items() {
            return patterns;
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(width - UI.scale(6), sz.y));
        }

        protected Widget makeitem(ActionsItem item, int idx, Coord sz) {
            return (new ItemWidget<ActionsItem>(this, sz, item) {
                {
                    add(item);
                }

                public boolean mousedown(Coord c, int button) {
                    boolean psel = sel == item;
                    super.mousedown(c, button);
                    if (!psel) {
                        String value = item.text.text();
                    }
                    return (true);
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

    public class ActionsItem extends Widget {
        Label text;
        IButton remove;
        public CheckBox isEnabled;

        public NArea area;

        @Override
        public void resize(Coord sz) {
            remove.move(new Coord(sz.x - NStyle.removei[0].sz().x - UI.scale(5), remove.c.y));
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
}