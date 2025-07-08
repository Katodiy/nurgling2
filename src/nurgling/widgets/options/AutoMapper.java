package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.widgets.nsettings.Panel;

public class AutoMapper extends Panel {
    public CheckBox navTrack;

    public AutoMapper() {
        super();

        final int margin = UI.scale(10);

        Widget prev = add(new Label("ONLINE Auto-Mapper settings:"), new Coord(margin, margin));

        prev = add(new CheckBox("Enable Auto Mapper") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.autoMapper);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.autoMapper, val);
                a = val;
                if (!a)
                    NUtils.setAutoMapperState(a);
            }
        }, prev.pos("bl").adds(0, 5));

        Label urlLabel = add(new Label("Server URL:"), prev.pos("bl").adds(0, 5));
        TextEntry te = add(new TextEntry(UI.scale(300), (String) NConfig.get(NConfig.Key.endpoint)),
                urlLabel.pos("ur").adds(UI.scale(10), 0));
        Button setBtn = add(new Button(UI.scale(80), "Set") {
            @Override
            public void click() {
                NConfig.set(NConfig.Key.endpoint, te.text());
                super.click();
            }
        }, te.pos("ur").adds(UI.scale(10), 0));

        prev = add(navTrack = new CheckBox("Enable navigation tracking") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.automaptrack);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.automaptrack, val);
                a = val;
            }
        }, urlLabel.pos("bl").adds(0, 5));

        prev = add(new CheckBox("Upload custom green marks") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.unloadgreen);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.unloadgreen, val);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        pack();
    }
}
