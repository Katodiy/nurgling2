package nurgling.widgets.options;

import haven.Button;
import haven.Label;
import haven.*;
import nurgling.NConfig;
import nurgling.NStyle;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AutoSelection extends Widget {
    public ArrayList<AutoSelectItem> petals = new ArrayList<>();
    ActionList al;
    TextEntry newPetall;
    int width = UI.scale(210);
    public AutoSelection() {
        prev = add(new CheckBox("Auto selection enabled") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.asenable);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.asenable, val);
                a = val;
            }
        });
        prev = add(new Label("Auto selected petals:"), prev.pos("bl").adds(0, 5));
        prev = add( al = new ActionList(new Coord(width,UI.scale(300))), prev.pos("bl").add(0,UI.scale(10)));
        prev = add(newPetall = new TextEntry(UI.scale(150), ""), prev.pos("bl").add(0,UI.scale(10)));
        add(new Button(UI.scale(45),"Add"){
            @Override
            public void click() {
                if(!newPetall.text().isEmpty()) {
                    AutoSelectItem ai = new AutoSelectItem(newPetall.text());
                    ai.isEnabled.a = true;
                    petals.add(ai);
                }
            }
        }, prev.pos("ur").add(UI.scale(10), UI.scale(prev.sz.y/2) - Button.hs/2));
        if(NConfig.get(NConfig.Key.petals)!=null) {
            for (HashMap<String,Object> item : (ArrayList< HashMap<String,Object>>) NConfig.get(NConfig.Key.petals))
            {
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
        }, prev.pos("bl").adds(0, 5));
//
//        prev = add(new CheckBox("Walking into doors in basic mode") {
//            {
//                a = (Boolean) NConfig.get(NConfig.Key.q_door);
//            }
//
//            public void set(boolean val) {
//                NConfig.set(NConfig.Key.q_door, val);
//                a = val;
//            }
//        }, prev.pos("bl").adds(0, 5));
        pack();
    }


    class ActionList extends SListBox<AutoSelectItem, Widget> {
        ActionList(Coord sz) {
            super(sz, UI.scale(15));
        }

        protected List<AutoSelectItem> items() {
            return petals;
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(width - UI.scale(6), sz.y));
        }

        protected Widget makeitem(AutoSelectItem item, int idx, Coord sz) {
            return (new ItemWidget<AutoSelectItem>(this, sz, item) {
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

    public class AutoSelectItem extends Widget {
        Label text;
        IButton remove;
        public CheckBox isEnabled;


        @Override
        public void resize(Coord sz) {
            remove.move(new Coord(sz.x - NStyle.removei[0].sz().x - UI.scale(5), remove.c.y));
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