package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.widgets.nsettings.Panel;

public class AutoMapper extends Panel {
    public CheckBox navTrack;
    private CheckBox enableAutoMapper;
    private TextEntry te;
    private CheckBox uploadGreen;

    public AutoMapper() {
        super();

        final int margin = UI.scale(10);

        Widget prev = add(new Label("ONLINE Auto-Mapper settings:"), new Coord(margin, margin));

        prev = enableAutoMapper = add(new CheckBox("Enable Auto Mapper"), prev.pos("bl").adds(0, 5));

        Label urlLabel = add(new Label("Server URL:"), prev.pos("bl").adds(0, 5));
        te = add(new TextEntry(UI.scale(300), ""), urlLabel.pos("ur").adds(UI.scale(10), 0));
        Button setBtn = add(new Button(UI.scale(80), "Set") {
            @Override
            public void click() {
                te.settext(te.text());
                super.click();
            }
        }, te.pos("ur").adds(UI.scale(10), 0));

        prev = add(navTrack = new CheckBox("Enable navigation tracking"), urlLabel.pos("bl").adds(0, 5));

        prev = uploadGreen = add(new CheckBox("Upload custom green marks"), prev.pos("bl").adds(0, 5));

        load();
        pack();
    }

    @Override
    public void load() {
        enableAutoMapper.a = getBool(NConfig.Key.autoMapper);
        te.settext(asString(NConfig.get(NConfig.Key.endpoint)));
        navTrack.a = getBool(NConfig.Key.automaptrack);
        uploadGreen.a = getBool(NConfig.Key.unloadgreen);
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.autoMapper, enableAutoMapper.a);
        if (!enableAutoMapper.a)
            NUtils.setAutoMapperState(enableAutoMapper.a);
        NConfig.set(NConfig.Key.endpoint, te.text());
        NConfig.set(NConfig.Key.automaptrack, navTrack.a);
        NConfig.set(NConfig.Key.unloadgreen, uploadGreen.a);
        NConfig.needUpdate();
    }

    private boolean getBool(NConfig.Key key) {
        Object val = NConfig.get(key);
        return val instanceof Boolean ? (Boolean) val : false;
    }
    private String asString(Object v) {
        return v == null ? "" : v.toString();
    }
}
