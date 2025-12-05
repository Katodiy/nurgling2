package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;

import java.util.Arrays;
import java.util.List;

public class ParasiteSettings extends Panel {
    private CheckBox enabledCheckbox;
    private Dropbox<String> leechActionDropbox;
    private Dropbox<String> tickActionDropbox;

    private static final List<String> ACTIONS = Arrays.asList("ground", "inventory");
    private static final List<String> ACTION_LABELS = Arrays.asList("Drop to ground", "Move to inventory");

    public ParasiteSettings() {
        super("Parasite Bot Settings");

        int margin = UI.scale(10);
        int y = UI.scale(36);

        add(new Label("Automatically handle parasites (Leech, Tick) when they attach to your character:"), new Coord(margin, y));
        y += UI.scale(32);

        enabledCheckbox = add(new CheckBox("Enable parasite bot") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += UI.scale(40);

        add(new Label("Leech action:"), new Coord(margin, y));
        y += UI.scale(20);
        leechActionDropbox = add(new Dropbox<String>(UI.scale(180), ACTION_LABELS.size(), UI.scale(20)) {
            @Override
            protected String listitem(int i) {
                return ACTION_LABELS.get(i);
            }

            @Override
            protected int listitems() {
                return ACTION_LABELS.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, new Coord(UI.scale(5), 0));
            }

            @Override
            public void change(String item) {
                super.change(item);
            }
        }, new Coord(margin, y));
        y += UI.scale(40);

        add(new Label("Tick action:"), new Coord(margin, y));
        y += UI.scale(20);
        tickActionDropbox = add(new Dropbox<String>(UI.scale(180), ACTION_LABELS.size(), UI.scale(20)) {
            @Override
            protected String listitem(int i) {
                return ACTION_LABELS.get(i);
            }

            @Override
            protected int listitems() {
                return ACTION_LABELS.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, new Coord(UI.scale(5), 0));
            }

            @Override
            public void change(String item) {
                super.change(item);
            }
        }, new Coord(margin, y));
    }

    @Override
    public void load() {
        Boolean enabled = (Boolean) NConfig.get(NConfig.Key.parasiteBotEnabled);
        enabledCheckbox.a = enabled != null && enabled;

        String leechAction = (String) NConfig.get(NConfig.Key.leechAction);
        int leechIdx = ACTIONS.indexOf(leechAction);
        if (leechIdx >= 0) {
            leechActionDropbox.sel = ACTION_LABELS.get(leechIdx);
        } else {
            leechActionDropbox.sel = ACTION_LABELS.get(0);
        }

        String tickAction = (String) NConfig.get(NConfig.Key.tickAction);
        int tickIdx = ACTIONS.indexOf(tickAction);
        if (tickIdx >= 0) {
            tickActionDropbox.sel = ACTION_LABELS.get(tickIdx);
        } else {
            tickActionDropbox.sel = ACTION_LABELS.get(0);
        }
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.parasiteBotEnabled, enabledCheckbox.a);

        int leechIdx = ACTION_LABELS.indexOf(leechActionDropbox.sel);
        if (leechIdx >= 0) {
            NConfig.set(NConfig.Key.leechAction, ACTIONS.get(leechIdx));
        }

        int tickIdx = ACTION_LABELS.indexOf(tickActionDropbox.sel);
        if (tickIdx >= 0) {
            NConfig.set(NConfig.Key.tickAction, ACTIONS.get(tickIdx));
        }

        NConfig.needUpdate();
    }
}
