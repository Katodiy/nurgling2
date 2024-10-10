package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;

public class AutoMapper extends Widget {
    public CheckBox navTrack;
    public AutoMapper() {

        prev = add(new Label("ONLINE Auto-Mapper settings:"), new Coord(0, 0));

        prev = add(new CheckBox("Enable Auto Mapper") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.autoMapper);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.autoMapper, val);
                a = val;
                if(!a)
                    NUtils.setAutoMapperState(a);
            }
        }, prev.pos("bl").adds(5, 5));
        prev = add(new Label("Server URL:"), prev.pos("bl").adds(0, 5));
        TextEntry te;

        add(te = new TextEntry(UI.scale(300),(String)NConfig.get(NConfig.Key.endpoint)), prev.pos("ur").adds(5, -1));
        add(new Button(UI.scale(80),"Set"){
            @Override
            public void click() {
                NConfig.set(NConfig.Key.endpoint, te.text());
                super.click();
            }
        },te.pos("ur").adds(5, -9));
        prev = add(navTrack = new CheckBox("Enable navigation tracking") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.automaptrack);
            }

            public void set(boolean val) {
                NConfig.set(NConfig.Key.automaptrack, val);
                a = val;

            }
        }, prev.pos("bl").adds(0, 5));

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