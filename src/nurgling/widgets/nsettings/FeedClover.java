package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.i18n.L10n;

public class FeedClover extends Panel {

    boolean tempUseRope = false;
    CheckBox ropeAfterFeeding;
    public FeedClover() {
        super(L10n.get("feedclover.title"));

        int margin = UI.scale(10);

        Widget prev = add(new Label(L10n.get("feedclover.options")), new Coord(margin, margin));

        prev = ropeAfterFeeding = add(new CheckBox(L10n.get("feedclover.rope_after")) {
            public void set(boolean val) {
                tempUseRope = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 10));

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
