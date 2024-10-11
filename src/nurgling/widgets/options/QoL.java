package nurgling.widgets.options;

import haven.CheckBox;
import haven.Coord;
import haven.Label;
import haven.Widget;
import nurgling.NConfig;
import nurgling.NUtils;

public class QoL extends Widget {
    public QoL() {

        prev = add(new Label("Other:"), new Coord(0, 0));
        prev = add(new CheckBox("Show crop stage:") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.showCropStage);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.showCropStage, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Night vision:") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.nightVision);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.nightVision, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        prev = add(new CheckBox("Bounding Boxes:") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.showBB);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.showBB, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        prev = add(new CheckBox("Flat surface (need reboot):") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.nextflatsurface);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.nextflatsurface, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Show decorative objects (need reboot):") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.nextshowCSprite);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.nextshowCSprite, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));

        prev = add(new CheckBox("Hide nature objects:") {
            {
                a = !(Boolean) NConfig.get(NConfig.Key.hideNature);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.hideNature, !val);
                a = val;
                NUtils.showHideNature();
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Show mining overlay") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.miningol);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.miningol, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Enable tracking when login") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.tracking);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.tracking, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Enable criminal acting when login") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.crime);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.crime, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Enable swimming when login") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.swimming);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.swimming, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Disable menugrid keys:") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.disableMenugridKeys);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.disableMenugridKeys, val);
                a = val;
            }
        },prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("DEBUG") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.debug);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.debug, val);
                NUtils.getUI().core.debug = val;
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));

        pack();
    }
}