package nurgling.widgets.options;

import haven.Button;
import haven.Label;
import haven.*;
import nurgling.NConfig;
import nurgling.NStyle;
import nurgling.conf.NAreaRad;
import nurgling.widgets.nsettings.Panel;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NRingSettings extends Panel {

    public NRingSettings() {
        prev = add(new Label("Animal rings settings:"));
        ArrayList<NAreaRad> radProps = ((ArrayList<NAreaRad>) NConfig.get(NConfig.Key.animalrad));
        for (NAreaRad prop : radProps)
        {
            prev = add(new ElementSettings(prop),prev.pos("bl").adds(0,5));
        }
        pack();
    }


    public class ElementSettings extends Widget {
        final NAreaRad rad;

        public ElementSettings(NAreaRad rad) {
            this.rad = rad;
            add(prev = new CheckBox(""){
                {
                    a = rad.vis;
                }
                @Override
                public void changed(boolean val) {
                    super.changed(val);
                    rad.vis = val;
                    NConfig.needUpdate();
                }
            });
            add(prev = new Label(rad.name), UI.scale(20, 0));
            add(new TextEntry(UI.scale(120),String.valueOf(rad.radius)){
                @Override
                public void done(ReadLine buf) {
                    super.done(buf);
                    rad.radius = Integer.parseInt(buf.line());
                    NConfig.needUpdate();
                }
            },UI.scale(220, 0));
            pack();
        }
    }
}