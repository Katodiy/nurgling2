package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.widgets.nsettings.Panel;

public class QoL extends Panel {
    private CheckBox showCropStage;
    private CheckBox simpleCrops;
    private CheckBox nightVision;
    private CheckBox autoDrink;
    private CheckBox showBB;
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
    private CheckBox shortWalls;
    private CheckBox printpfmap;
    private CheckBox uniformBiomeColors;
    private CheckBox showTerrainName;
    private CheckBox waypointRetryOnStuck;
    private CheckBox verboseCal;
    private CheckBox showPersonalClaims;
    private CheckBox showVillageClaims;
    private CheckBox showRealmOverlays;
    private CheckBox showFullPathLines;

    private TextEntry temsmarkdistEntry;
    private TextEntry temsmarktimeEntry;

    private Scrollport scrollport;
    private Widget content;

    public QoL() {
        super("");
        int margin = UI.scale(10);

        // Create scrollport to contain all settings
        int scrollWidth = UI.scale(560);
        int scrollHeight = UI.scale(550);
        scrollport = add(new Scrollport(new Coord(scrollWidth, scrollHeight)), new Coord(margin, margin));

        // Create content widget that will hold all settings
        content = new Widget(new Coord(scrollWidth - UI.scale(20), UI.scale(50))) {
            @Override
            public void pack() {
                // Auto-resize based on children
                resize(contentsz());
            }
        };
        scrollport.cont.add(content, Coord.z);

        // Add all settings to the content widget
        Widget prev = null;
        int contentMargin = UI.scale(5);

        prev = showCropStage = content.add(new CheckBox("Show crop stage"), new Coord(contentMargin, contentMargin));
        prev = simpleCrops = content.add(new CheckBox("Simple crops"), prev.pos("bl").adds(0, 5));
        prev = nightVision = content.add(new CheckBox("Night vision"), prev.pos("bl").adds(0, 5));
        prev = autoDrink = content.add(new CheckBox("Auto-drink"), prev.pos("bl").adds(0, 5));
        prev = showBB = content.add(new CheckBox("Bounding Boxes"), prev.pos("bl").adds(0, 5));
        prev = showCSprite = content.add(new CheckBox("Show decorative objects (need reboot)"), prev.pos("bl").adds(0, 5));
        prev = hideNature = content.add(new CheckBox("Hide nature objects"), prev.pos("bl").adds(0, 5));
        prev = miningOL = content.add(new CheckBox("Show mining overlay"), prev.pos("bl").adds(0, 5));
        prev = tracking = content.add(new CheckBox("Enable tracking when login"), prev.pos("bl").adds(0, 5));
        prev = crime = content.add(new CheckBox("Enable criminal acting when login"), prev.pos("bl").adds(0, 5));
        prev = swimming = content.add(new CheckBox("Enable swimming when login"), prev.pos("bl").adds(0, 5));
        prev = disableMenugridKeys = content.add(new CheckBox("Disable menugrid keys"), prev.pos("bl").adds(0, 5));
        prev = questNotified = content.add(new CheckBox("Enable quest notified"), prev.pos("bl").adds(0, 5));
        prev = lpassistent = content.add(new CheckBox("Enable LP assistant"), prev.pos("bl").adds(0, 5));
        prev = shortCupboards = content.add(new CheckBox("Short cupboards"), prev.pos("bl").adds(0, 5));
        prev = shortWalls = content.add(new CheckBox("Short mine walls"), prev.pos("bl").adds(0, 5));
        prev = useGlobalPf = content.add(new CheckBox("Use global PF"), prev.pos("bl").adds(0, 5));
        prev = uniformBiomeColors = content.add(new CheckBox("Uniform biome colors on minimap"), prev.pos("bl").adds(0, 5));
        prev = showTerrainName = content.add(new CheckBox("Show terrain name on minimap hover"), prev.pos("bl").adds(0, 5));
        prev = waypointRetryOnStuck = content.add(new CheckBox("Retry waypoint movement when stuck"), prev.pos("bl").adds(0, 5));
        prev = verboseCal = content.add(new CheckBox("Verbose calendar"), prev.pos("bl").adds(0, 5));
        prev = debug = content.add(new CheckBox("DEBUG"), prev.pos("bl").adds(0, 5));
        prev = printpfmap = content.add(new CheckBox("Path Finder map in debug"), prev.pos("bl").adds(0, 5));
        prev = showFullPathLines = content.add(new CheckBox("Show full path lines to destinations"), prev.pos("bl").adds(0, 5));

        prev = content.add(new Label("Map overlays:"), prev.pos("bl").adds(0, 15));
        prev = showPersonalClaims = content.add(new CheckBox("Show personal claims on minimap"), prev.pos("bl").adds(0, 5));
        prev = showVillageClaims = content.add(new CheckBox("Show village claims on minimap"), prev.pos("bl").adds(0, 5));
        prev = showRealmOverlays = content.add(new CheckBox("Show realm overlays on minimap"), prev.pos("bl").adds(0, 5));

        prev = content.add(new Label("Temporary marks:"), prev.pos("bl").adds(0, 15));
        prev = tempmark = content.add(new CheckBox("Save temporary marks"), prev.pos("bl").adds(0, 5));
        prev = content.add(new Label("Max distance (grids):"), prev.pos("bl").adds(0, 5));
        prev = temsmarkdistEntry = content.add(new TextEntry.NumberValue(50, ""), prev.pos("bl").adds(0, 5));
        prev = content.add(new Label("Storage duration (minutes):"), prev.pos("bl").adds(0, 5));
        prev = temsmarktimeEntry = content.add(new TextEntry.NumberValue(50, ""), prev.pos("bl").adds(0, 5));

        // Pack content and update scrollbar
        content.pack();
        scrollport.cont.update();

        pack();
    }

    @Override
    public void load() {
        showCropStage.a = getBool(NConfig.Key.showCropStage);
        simpleCrops.a = getBool(NConfig.Key.simplecrops);
        nightVision.a = getBool(NConfig.Key.nightVision);
        autoDrink.a = getBool(NConfig.Key.autoDrink);
        showBB.a = getBool(NConfig.Key.showBB);
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
        printpfmap.a = getBool(NConfig.Key.printpfmap);
        tempmark.a = getBool(NConfig.Key.tempmark);
        shortCupboards.a = getBool(NConfig.Key.shortCupboards);
        shortWalls.a = getBool(NConfig.Key.shortWalls);
        uniformBiomeColors.a = getBool(NConfig.Key.uniformBiomeColors);
        showTerrainName.a = getBool(NConfig.Key.showTerrainName);
        waypointRetryOnStuck.a = getBool(NConfig.Key.waypointRetryOnStuck);
        verboseCal.a = getBool(NConfig.Key.verboseCal);
        showFullPathLines.a = getBool(NConfig.Key.showFullPathLines);
        showPersonalClaims.a = getBool(NConfig.Key.minimapClaimol);
        showVillageClaims.a = getBool(NConfig.Key.minimapVilol);
        showRealmOverlays.a = getBool(NConfig.Key.minimapRealmol);

        Object dist = NConfig.get(NConfig.Key.temsmarkdist);
        temsmarkdistEntry.settext(dist == null ? "" : dist.toString());

        Object time = NConfig.get(NConfig.Key.temsmarktime);
        temsmarktimeEntry.settext(time == null ? "" : time.toString());
    }

    public void syncShowBB() {
        showBB.a = getBool(NConfig.Key.showBB);
    }

    public void syncHideNature() {
        hideNature.a = !getBool(NConfig.Key.hideNature);
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
        NConfig.set(NConfig.Key.printpfmap, printpfmap.a);
        NConfig.set(NConfig.Key.tempmark, tempmark.a);
        NConfig.set(NConfig.Key.shortCupboards, shortCupboards.a);

        // Save shortWalls and trigger map re-render if changed
        boolean oldShortWalls = getBool(NConfig.Key.shortWalls);
        NConfig.set(NConfig.Key.shortWalls, shortWalls.a);
        if(oldShortWalls != shortWalls.a) {
            // Force map mesh rebuild when short walls setting changes
            if(NUtils.getGameUI() != null && NUtils.getGameUI().map != null && NUtils.getGameUI().map.glob != null) {
                MCache map = NUtils.getGameUI().map.glob.map;
                synchronized(map.grids) {
                    // Invalidate all loaded grids to trigger mesh rebuild
                    for(Coord gc : map.grids.keySet()) {
                        map.invalidate(gc);
                    }
                }

                // Also rebuild the rock tile highlight overlay since it uses wall height
                try {
                    NMapView rockTileOverlay = (NMapView) NUtils.getGameUI().map;
                    if(rockTileOverlay != null) {
                        nurgling.overlays.map.NRockTileHighlightOverlay overlay = NMapView.getRockTileOverlay();
                        if(overlay != null) {
                            overlay.forceRebuild();
                        }
                    }
                } catch(Exception e) {
                    // Silently ignore if overlay doesn't exist
                }
            }
        }

        NConfig.set(NConfig.Key.showTerrainName, showTerrainName.a);
        NConfig.set(NConfig.Key.waypointRetryOnStuck, waypointRetryOnStuck.a);
        NConfig.set(NConfig.Key.verboseCal, verboseCal.a);
        NConfig.set(NConfig.Key.showFullPathLines, showFullPathLines.a);

        // Save minimap overlay settings (separate from 3D ground overlays)
        NConfig.set(NConfig.Key.minimapClaimol, showPersonalClaims.a);
        NConfig.set(NConfig.Key.minimapVilol, showVillageClaims.a);
        NConfig.set(NConfig.Key.minimapRealmol, showRealmOverlays.a);

        // Save uniform biome colors and update minimap if changed
        boolean oldUniformColors = getBool(NConfig.Key.uniformBiomeColors);
        NConfig.set(NConfig.Key.uniformBiomeColors, uniformBiomeColors.a);
        if(oldUniformColors != uniformBiomeColors.a) {
            // Force minimap update when uniform biome colors setting changes
            if(NUtils.getGameUI() != null && NUtils.getGameUI().mmapw != null && NUtils.getGameUI().mmapw.miniMap != null) {
                NUtils.getGameUI().mmapw.miniMap.needUpdate = true;
            }
            // Also update main map if it exists
            if(NUtils.getGameUI() != null && NUtils.getGameUI().mapfile != null && NUtils.getGameUI().mapfile.view != null) {
                NUtils.getGameUI().mapfile.view.needUpdate = true;
            }
        }

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
