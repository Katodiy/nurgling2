package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;

public class FeedClover extends Panel {

    boolean tempUseRope = false;
    CheckBox ropeAfterFeeding;
    public FeedClover() {
        super("Feed Clover");

        int margin = UI.scale(10);

        Widget prev = ropeAfterFeeding = add(new CheckBox("Tie the animal on a rope after feeding it") {
            public void set(boolean val) {
                tempUseRope = val;
                a = val;
            }
        }, new Coord(margin, margin));

    }

    @Override
    public void load() {
        tempUseRope = (Boolean) NConfig.get(NConfig.Key.ropeAfterFeeding);
        ropeAfterFeeding.a = tempUseRope;
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.ropeAfterFeeding, tempUseRope);
    }
}
