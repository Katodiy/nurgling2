package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.widgets.NMiniMapWnd;

public class QoL extends Widget {
    final public CheckBox natura;
    final public CheckBox night;
    public QoL() {

        prev = add(new Label("Other:"), new Coord(0, 0));
        prev = add(new CheckBox("Show crop stage") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.showCropStage);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.showCropStage, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Simple crops") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.simplecrops);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.simplecrops, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        prev = night = add(new CheckBox("Night vision") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.nightVision);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.nightVision, val);
                NUtils.getGameUI().mmapw.nightvision.a = a;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        prev = add(new CheckBox("Auto-drink") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.autoDrink);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.autoDrink, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        prev = add(new CheckBox("Bounding Boxes") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.showBB);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.showBB, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        prev = add(new CheckBox("Flat surface (need reboot)") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.nextflatsurface);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.nextflatsurface, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Show decorative objects (need reboot)") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.nextshowCSprite);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.nextshowCSprite, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));

        prev = natura = add(new CheckBox("Hide nature objects") {
            {
                a = !(Boolean) NConfig.get(NConfig.Key.hideNature);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.hideNature, !val);
                a = val;
                NUtils.getGameUI().mmapw.natura.a = a;
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
        prev = add(new CheckBox("Disable menugrid keys") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.disableMenugridKeys);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.disableMenugridKeys, val);
                a = val;
            }
        },prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Enable quest notified") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.questNotified);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.questNotified, val);
                a = val;
            }
        },prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Enable LP assistant") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.lpassistent);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.lpassistent, val);
                a = val;
            }
        },prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Refill water from waters containers for farmers") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.harvestautorefill);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.harvestautorefill, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(new CheckBox("Use global PF") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.useGlobalPf);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.useGlobalPf, val);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
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

        // Add Temporary marks section
        prev = add(new Label("Temporary marks:"), prev.pos("bl").adds(0, 15));
        prev = add(new CheckBox("Save temporary marks") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.tempmark);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.tempmark, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        prev = add(new Label("Max distance (grids):"), prev.pos("bl").adds(0, 5));
        prev = add(new TextEntry.NumberValue(50, String.valueOf(NConfig.get(NConfig.Key.temsmarkdist))) {
            {
                settext(String.valueOf(NConfig.get(NConfig.Key.temsmarkdist)));
            }

            @Override
            public void done(ReadLine buf) {
                super.done(buf);
                NConfig.set(NConfig.Key.temsmarkdist,  Integer.parseInt(buf.line()));
                NConfig.needUpdate();
            }
        }, prev.pos("bl").adds(0, 5));
        prev = add(new Label("Storage duration (minutes):"), prev.pos("bl").adds(0, 5));

        prev = add(new TextEntry.NumberValue(50, String.valueOf(NConfig.get(NConfig.Key.temsmarktime))) {
            {
                settext(String.valueOf(NConfig.get(NConfig.Key.temsmarktime)));
            }
            @Override
            public void done(ReadLine buf) {
                super.done(buf);
                NConfig.set(NConfig.Key.temsmarktime,  Integer.parseInt(buf.line()));
                NConfig.needUpdate();
            }
        }, prev.pos("bl").adds(0, 5));

        pack();
    }
}