package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.widgets.nsettings.Panel;

public class QoL extends Panel {
    private CheckBox showCropStage;
    private CheckBox simpleCrops;
    private CheckBox nightVision;
    private CheckBox autoDrink;
    private CheckBox showBB;
    private CheckBox flatSurface;
    private CheckBox showCSprite;
    private CheckBox hideNature;
    private CheckBox miningOL;
    private CheckBox tracking;
    private CheckBox crime;
    private CheckBox swimming;
    private CheckBox disableMenugridKeys;
    private CheckBox questNotified;
    private CheckBox lpassistent;
    private CheckBox useGlobalPf;
    private CheckBox debug;
    private CheckBox tempmark;
    private CheckBox shortCupboards;

    private TextEntry temsmarkdistEntry;
    private TextEntry temsmarktimeEntry;

    public QoL() {
        super("");
        int margin = UI.scale(10);
        Widget prev = null;

        prev = showCropStage = add(new CheckBox("Show crop stage"), new Coord(margin, margin));
        prev = simpleCrops = add(new CheckBox("Simple crops"), prev.pos("bl").adds(0, 5));
        prev = nightVision = add(new CheckBox("Night vision"), prev.pos("bl").adds(0, 5));
        prev = autoDrink = add(new CheckBox("Auto-drink"), prev.pos("bl").adds(0, 5));
        prev = showBB = add(new CheckBox("Bounding Boxes"), prev.pos("bl").adds(0, 5));
        prev = flatSurface = add(new CheckBox("Flat surface (need reboot)"), prev.pos("bl").adds(0, 5));
        prev = showCSprite = add(new CheckBox("Show decorative objects (need reboot)"), prev.pos("bl").adds(0, 5));
        prev = hideNature = add(new CheckBox("Hide nature objects"), prev.pos("bl").adds(0, 5));
        prev = miningOL = add(new CheckBox("Show mining overlay"), prev.pos("bl").adds(0, 5));
        prev = tracking = add(new CheckBox("Enable tracking when login"), prev.pos("bl").adds(0, 5));
        prev = crime = add(new CheckBox("Enable criminal acting when login"), prev.pos("bl").adds(0, 5));
        prev = swimming = add(new CheckBox("Enable swimming when login"), prev.pos("bl").adds(0, 5));
        prev = disableMenugridKeys = add(new CheckBox("Disable menugrid keys"), prev.pos("bl").adds(0, 5));
        prev = questNotified = add(new CheckBox("Enable quest notified"), prev.pos("bl").adds(0, 5));
        prev = lpassistent = add(new CheckBox("Enable LP assistant"), prev.pos("bl").adds(0, 5));
        prev = shortCupboards = add(new CheckBox("Short cupboards"), prev.pos("bl").adds(0, 5));
        prev = useGlobalPf = add(new CheckBox("Use global PF"), prev.pos("bl").adds(0, 5));
        prev = debug = add(new CheckBox("DEBUG"), prev.pos("bl").adds(0, 5));

        prev = add(new Label("Temporary marks:"), prev.pos("bl").adds(0, 15));
        prev = tempmark = add(new CheckBox("Save temporary marks"), prev.pos("bl").adds(0, 5));
        prev = add(new Label("Max distance (grids):"), prev.pos("bl").adds(0, 5));
        prev = temsmarkdistEntry = add(new TextEntry.NumberValue(50, ""), prev.pos("bl").adds(0, 5));
        prev = add(new Label("Storage duration (minutes):"), prev.pos("bl").adds(0, 5));
        prev = temsmarktimeEntry = add(new TextEntry.NumberValue(50, ""), prev.pos("bl").adds(0, 5));

        pack();
    }

    @Override
    public void load() {
        showCropStage.a = getBool(NConfig.Key.showCropStage);
        simpleCrops.a = getBool(NConfig.Key.simplecrops);
        nightVision.a = getBool(NConfig.Key.nightVision);
        autoDrink.a = getBool(NConfig.Key.autoDrink);
        showBB.a = getBool(NConfig.Key.showBB);
        flatSurface.a = getBool(NConfig.Key.nextflatsurface);
        showCSprite.a = getBool(NConfig.Key.nextshowCSprite);

        hideNature.a = !getBool(NConfig.Key.hideNature);
        miningOL.a = getBool(NConfig.Key.miningol);
        tracking.a = getBool(NConfig.Key.tracking);
        crime.a = getBool(NConfig.Key.crime);
        swimming.a = getBool(NConfig.Key.swimming);
        disableMenugridKeys.a = getBool(NConfig.Key.disableMenugridKeys);
        questNotified.a = getBool(NConfig.Key.questNotified);
        lpassistent.a = getBool(NConfig.Key.lpassistent);
        useGlobalPf.a = getBool(NConfig.Key.useGlobalPf);
        debug.a = getBool(NConfig.Key.debug);
        tempmark.a = getBool(NConfig.Key.tempmark);
        shortCupboards.a = getBool(NConfig.Key.shortCupboards);

        Object dist = NConfig.get(NConfig.Key.temsmarkdist);
        temsmarkdistEntry.settext(dist == null ? "" : dist.toString());

        Object time = NConfig.get(NConfig.Key.temsmarktime);
        temsmarktimeEntry.settext(time == null ? "" : time.toString());
    }

    @Override
    public void save() {
        boolean oldHideNature = false;
        if (NConfig.get(NConfig.Key.hideNature) instanceof Boolean) {
            oldHideNature = (Boolean) NConfig.get(NConfig.Key.hideNature);
        }
        boolean newHideNature = !hideNature.a;

        NConfig.set(NConfig.Key.showCropStage, showCropStage.a);
        NConfig.set(NConfig.Key.simplecrops, simpleCrops.a);
        NConfig.set(NConfig.Key.nightVision, nightVision.a);
        NConfig.set(NConfig.Key.autoDrink, autoDrink.a);
        NConfig.set(NConfig.Key.showBB, showBB.a);
        NConfig.set(NConfig.Key.nextflatsurface, flatSurface.a);
        NConfig.set(NConfig.Key.nextshowCSprite, showCSprite.a);
        NConfig.set(NConfig.Key.hideNature, newHideNature);
        NConfig.set(NConfig.Key.miningol, miningOL.a);
        NConfig.set(NConfig.Key.tracking, tracking.a);
        NConfig.set(NConfig.Key.crime, crime.a);
        NConfig.set(NConfig.Key.swimming, swimming.a);
        NConfig.set(NConfig.Key.disableMenugridKeys, disableMenugridKeys.a);
        NConfig.set(NConfig.Key.questNotified, questNotified.a);
        NConfig.set(NConfig.Key.lpassistent, lpassistent.a);
        NConfig.set(NConfig.Key.useGlobalPf, useGlobalPf.a);
        NConfig.set(NConfig.Key.debug, debug.a);
        NConfig.set(NConfig.Key.tempmark, tempmark.a);
        NConfig.set(NConfig.Key.shortCupboards, shortCupboards.a);

        int dist = parseIntOrDefault(temsmarkdistEntry.text(), 0);
        int time = parseIntOrDefault(temsmarktimeEntry.text(), 0);
        NConfig.set(NConfig.Key.temsmarkdist, dist);
        NConfig.set(NConfig.Key.temsmarktime, time);

        if(NUtils.getGameUI() != null) {
            if(NUtils.getGameUI().mmapw != null) {
                NUtils.getGameUI().mmapw.nightvision.a = nightVision.a;
                NUtils.getGameUI().mmapw.natura.a = hideNature.a;
            }
        }
        if(NUtils.getUI() != null && NUtils.getUI().core != null)
            NUtils.getUI().core.debug = debug.a;

        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            if (oldHideNature != newHideNature) {
                NUtils.showHideNature();
            }
        }
        NConfig.needUpdate();
    }

    private boolean getBool(NConfig.Key key) {
        Object val = NConfig.get(key);
        return val instanceof Boolean ? (Boolean) val : false;
    }
    private int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch(Exception e) { return def; }
    }
}
