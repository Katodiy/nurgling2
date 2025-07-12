package nurgling.widgets.options;

import haven.Label;
import haven.*;
import nurgling.NConfig;
import nurgling.conf.NAreaRad;
import nurgling.widgets.nsettings.Panel;

import java.util.ArrayList;

public class NRingSettings extends Panel {

    public NRingSettings() {
        final int margin = UI.scale(10);

        prev = add(new Label("Animal rings settings:"), new Coord(margin, margin));
        ArrayList<NAreaRad> radProps = ((ArrayList<NAreaRad>) NConfig.get(NConfig.Key.animalrad));
        for (NAreaRad prop : radProps)
        {
            prev = add(new ElementSettings(prop, UI.scale(320), UI.scale(22)), prev.pos("bl").adds(0, 5));
        }
        pack();
    }

    public class ElementSettings extends Widget {
        final NAreaRad rad;
        final int itemHeight;

        CheckBox visBox;
        Label nameLabel;
        TextEntry radEntry;

        public ElementSettings(NAreaRad rad, int width, int height) {
            super(new Coord(width, height));
            this.rad = rad;
            this.itemHeight = height;

            int checkX = 0;
            int labelX = UI.scale(24);
            int entryX = UI.scale(170);

            visBox = add(new CheckBox("") {
                {
                    a = rad.vis;
                }
                @Override
                public void changed(boolean val) {
                    super.changed(val);
                    rad.vis = val;
                    NConfig.needUpdate();
                }
            }, new Coord(checkX, (itemHeight - UI.scale(16)) / 2));

            nameLabel = add(new Label(rad.name), new Coord(labelX, (itemHeight - UI.scale(16)) / 2));

            radEntry = add(new TextEntry(UI.scale(80), String.valueOf(rad.radius)) {
                @Override
                public void done(ReadLine buf) {
                    super.done(buf);
                    try {
                        rad.radius = Integer.parseInt(buf.line());
                        NConfig.needUpdate();
                    } catch (Exception ignored) { }
                }
            }, new Coord(entryX, (itemHeight - UI.scale(16)) / 2));

            resize(new Coord(width, itemHeight));
        }

        @Override
        public void resize(Coord sz) {
            super.resize(sz);
            int cy = (itemHeight - UI.scale(16)) / 2;
            if (visBox != null)
                visBox.move(new Coord(0, cy));
            if (nameLabel != null)
                nameLabel.move(new Coord(UI.scale(24), cy));
            if (radEntry != null)
                radEntry.move(new Coord(UI.scale(170), cy));
        }
    }
}
