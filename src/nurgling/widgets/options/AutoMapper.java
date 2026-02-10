package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import nurgling.widgets.nsettings.Panel;

public class AutoMapper extends Panel {
    public CheckBox navTrack;
    private CheckBox enableAutoMapper;
    private TextEntry te;
    private CheckBox uploadGreen;
    private CheckBox sendOverlays;

    public AutoMapper() {
        super();

        final int margin = UI.scale(10);

        Widget prev = add(new Label(L10n.get("automapper.settings_title")), new Coord(margin, margin));

        prev = enableAutoMapper = add(new CheckBox(L10n.get("automapper.enable")), prev.pos("bl").adds(0, 5));

        Label urlLabel = add(new Label(L10n.get("automapper.server_url")), prev.pos("bl").adds(0, 5));
        te = add(new TextEntry(UI.scale(300), ""), urlLabel.pos("ur").adds(UI.scale(10), 0));
        Button setBtn = add(new Button(UI.scale(80), L10n.get("automapper.set")) {
            @Override
            public void click() {
                te.settext(te.text());
                super.click();
            }
        }, te.pos("ur").adds(UI.scale(10), 0));

        prev = add(navTrack = new CheckBox(L10n.get("automapper.nav_track")), urlLabel.pos("bl").adds(0, 5));

        prev = uploadGreen = add(new CheckBox(L10n.get("automapper.upload_green")), prev.pos("bl").adds(0, 5));

        prev = sendOverlays = add(new CheckBox(L10n.get("automapper.send_overlays")), prev.pos("bl").adds(0, 5));

        load();
        pack();
    }

    @Override
    public void load() {
        enableAutoMapper.a = getBool(NConfig.Key.autoMapper);
        te.settext(asString(NConfig.get(NConfig.Key.endpoint)));
        navTrack.a = getBool(NConfig.Key.automaptrack);
        uploadGreen.a = getBool(NConfig.Key.unloadgreen);
        sendOverlays.a = getBool(NConfig.Key.sendOverlays);
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.autoMapper, enableAutoMapper.a);
        if (!enableAutoMapper.a)
            NUtils.setAutoMapperState(enableAutoMapper.a);
        NConfig.set(NConfig.Key.endpoint, te.text());
        NConfig.set(NConfig.Key.automaptrack, navTrack.a);
        NConfig.set(NConfig.Key.unloadgreen, uploadGreen.a);
        NConfig.set(NConfig.Key.sendOverlays, sendOverlays.a);
        // Reset overlay support flag when user enables the setting (allows retry)
        if (sendOverlays.a && NUtils.getUI() != null && NUtils.getUI().core != null
                && NUtils.getUI().core.mappingClient != null) {
            NUtils.getUI().core.mappingClient.resetOverlaySupport();
        }
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
