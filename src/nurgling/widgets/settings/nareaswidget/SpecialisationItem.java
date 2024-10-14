package nurgling.widgets.settings.nareaswidget;

import haven.*;
import nurgling.NConfig;
import nurgling.NFlowerMenu;
import nurgling.areas.NArea;
import nurgling.tools.SpecialisationData;

public class SpecialisationItem extends Widget {
    Label text;
    public NArea.Specialisation item;
    IButton spec = null;
    NFlowerMenu menu;

    public SpecialisationItem(NArea.Specialisation item) {
        this.item = item;
        if (item.subtype == null) {
            this.text = add(new Label(item.name));
        } else {
            this.text = add(new Label(item.name + "(" + item.subtype + ")"));
        }
        if (SpecialisationData.data.get(item.name) != null) {
            add(spec = new IButton("nurgling/hud/buttons/settingsnf/", "u", "d", "h") {
                @Override
                public void click() {
                    super.click();
                    menu = new NFlowerMenu(SpecialisationData.data.get(item.name)) {
                        public boolean mousedown(Coord c, int button) {
                            if (super.mousedown(c, button))
                                nchoose(null);
                            return (true);
                        }

                        public void destroy() {
                            menu = null;
                            super.destroy();
                        }

                        @Override
                        public void nchoose(NPetal option) {
                            if (option != null) {
                                SpecialisationItem.this.text.settext(item.name + "(" + option.name + ")");
                                item.subtype = option.name;
                                NConfig.needAreasUpdate();
                            }
                            uimsg("cancel");
                        }

                    };
                    Widget par = parent;
                    Coord pos = c.add(UI.scale(32, 43));
                    while (par != null && !(par instanceof GameUI)) {
                        pos = pos.add(par.c);
                        par = par.parent;
                    }
                    ui.root.add(menu, pos);
                }
            }, UI.scale(new Coord(135, 0)));
        }
        pack();
    }
}
