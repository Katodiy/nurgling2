package nurgling.widgets.nsettings;

import haven.CheckBox;
import haven.Label;
import haven.UI;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.widgets.NColorWidget;

import java.awt.*;

public class FeedClover extends Panel {

    boolean tempUseRope = false;
    CheckBox ropeAfterFeeding;
    public FeedClover() {
        super("Feed Clover");

        ropeAfterFeeding = add(new CheckBox("Tie the animal on a rope after feeding it") {
            public void set(boolean val) {
                tempUseRope = val;
                a = val;
            }
        });

    }

    @Override
    public void load() {
        tempUseRope = (Boolean) NConfig.get(NConfig.Key.ropeAfterFeeding);
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.ropeAfterFeeding, tempUseRope);
    }

}