package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.overlays.NLPassistant;
import nurgling.widgets.nsettings.Panel;

public class QoL extends Panel {
    private CheckBox showCropStage;
    private CheckBox simpleCrops;
    private CheckBox nightVision;
    private CheckBox autoDrink;
    private CheckBox autoSaveTableware;
    private CheckBox showBB;
    private CheckBox showCSprite;
    private CheckBox hideNature;
    private CheckBox miningOL;
    private CheckBox tracking;
    private CheckBox crime;
    private CheckBox swimming;
    private CheckBox openInventoryOnLogin;
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

    private Dropbox<String> preferredSpeedDropbox;
    private Dropbox<String> preferredHorseSpeedDropbox;
    private TextEntry temsmarkdistEntry;
    private TextEntry temsmarktimeEntry;

    private Scrollport scrollport;
    private Widget content;
    private Widget leftColumn;
    private Widget rightColumn;

    public QoL() {
        super("");
        int margin = UI.scale(10);

        // Create scrollport to contain all settings (wider for 2 columns)
        int scrollWidth = UI.scale(720);
        int scrollHeight = UI.scale(550);
        scrollport = add(new Scrollport(new Coord(scrollWidth, scrollHeight)), new Coord(margin, margin));

        // Create main content container
        content = new Widget(new Coord(scrollWidth - UI.scale(20), UI.scale(50))) {
            @Override
            public void pack() {
                // Auto-resize based on children
                resize(contentsz());
            }
        };
        scrollport.cont.add(content, Coord.z);

        // Create two columns
        int columnWidth = UI.scale(340);
        int contentMargin = UI.scale(5);

        leftColumn = new Widget(new Coord(columnWidth, UI.scale(50))) {
            @Override
            public void pack() {
                resize(contentsz());
            }
        };

        rightColumn = new Widget(new Coord(columnWidth, UI.scale(50))) {
            @Override
            public void pack() {
                resize(contentsz());
            }
        };

        content.add(leftColumn, new Coord(contentMargin, contentMargin));
        content.add(rightColumn, new Coord(contentMargin + columnWidth + UI.scale(10), contentMargin));

        // LEFT COLUMN - Visual & Interface Settings
        Widget leftPrev = leftColumn.add(new Label("● Visual & Interface"), new Coord(5, 5));
        leftPrev = showCropStage = leftColumn.add(new CheckBox("Show crop stage"), leftPrev.pos("bl").adds(0, 10));
        leftPrev = simpleCrops = leftColumn.add(new CheckBox("Simple crops"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = nightVision = leftColumn.add(new CheckBox("Night vision"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showBB = leftColumn.add(new CheckBox("Bounding Boxes"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showCSprite = leftColumn.add(new CheckBox("Show decorative objects (need reboot)"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = hideNature = leftColumn.add(new CheckBox("Hide nature objects"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = uniformBiomeColors = leftColumn.add(new CheckBox("Uniform biome colors on minimap"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showTerrainName = leftColumn.add(new CheckBox("Show terrain name on minimap hover"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = shortCupboards = leftColumn.add(new CheckBox("Short cupboards"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = shortWalls = leftColumn.add(new CheckBox("Short mine walls"), leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label("● Login Settings"), leftPrev.pos("bl").adds(0, 15));
        leftPrev = tracking = leftColumn.add(new CheckBox("Enable tracking when login"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = crime = leftColumn.add(new CheckBox("Enable criminal acting when login"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = swimming = leftColumn.add(new CheckBox("Enable swimming when login"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = openInventoryOnLogin = leftColumn.add(new CheckBox("Open player inventory when login"), leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label("Preferred movement speed on login:"), leftPrev.pos("bl").adds(0, 10));
        leftPrev = preferredSpeedDropbox = leftColumn.add(new Dropbox<String>(UI.scale(150), 4, UI.scale(16)) {
            private final String[] speeds = {"Crawl", "Walk", "Run", "Sprint"};

            @Override
            protected String listitem(int i) {
                return speeds[i];
            }

            @Override
            protected int listitems() {
                return speeds.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                for (int i = 0; i < speeds.length; i++) {
                    if (speeds[i].equals(item)) {
                        NConfig.set(NConfig.Key.preferredMovementSpeed, i);
                        NConfig.needUpdate();
                        break;
                    }
                }
            }
        }, leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label("Preferred horse speed on mount:"), leftPrev.pos("bl").adds(0, 10));
        leftPrev = preferredHorseSpeedDropbox = leftColumn.add(new Dropbox<String>(UI.scale(150), 4, UI.scale(16)) {
            private final String[] speeds = {"Crawl", "Walk", "Run", "Sprint"};

            @Override
            protected String listitem(int i) {
                return speeds[i];
            }

            @Override
            protected int listitems() {
                return speeds.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                for (int i = 0; i < speeds.length; i++) {
                    if (speeds[i].equals(item)) {
                        NConfig.set(NConfig.Key.preferredHorseSpeed, i);
                        NConfig.needUpdate();
                        break;
                    }
                }
            }
        }, leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label("● Map Overlays"), leftPrev.pos("bl").adds(0, 15));
        leftPrev = miningOL = leftColumn.add(new CheckBox("Show mining overlay"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showPersonalClaims = leftColumn.add(new CheckBox("Show personal claims on minimap"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showVillageClaims = leftColumn.add(new CheckBox("Show village claims on minimap"), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showRealmOverlays = leftColumn.add(new CheckBox("Show realm overlays on minimap"), leftPrev.pos("bl").adds(0, 5));

        // RIGHT COLUMN - Advanced Settings
        Widget rightPrev = null;
        rightPrev = rightColumn.add(new Label("● Pathfinding & Navigation"), new Coord(5, 5));
        rightPrev = useGlobalPf = rightColumn.add(new CheckBox("Use global PF"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = waypointRetryOnStuck = rightColumn.add(new CheckBox("Retry waypoint movement when stuck"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = showFullPathLines = rightColumn.add(new CheckBox("Show full path lines to destinations"), rightPrev.pos("bl").adds(0, 5));

        rightPrev = rightColumn.add(new Label("● Quality of Life"), rightPrev.pos("bl").adds(0, 15));
        rightPrev = autoDrink = rightColumn.add(new CheckBox("Auto-drink"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = autoSaveTableware = rightColumn.add(new CheckBox("Auto-save tableware"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = questNotified = rightColumn.add(new CheckBox("Enable quest notified"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = lpassistent = rightColumn.add(new CheckBox("Enable LP assistant"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = disableMenugridKeys = rightColumn.add(new CheckBox("Disable menugrid keys"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = verboseCal = rightColumn.add(new CheckBox("Verbose calendar"), rightPrev.pos("bl").adds(0, 5));

        rightPrev = rightColumn.add(new Label("● Debug & Development"), rightPrev.pos("bl").adds(0, 15));
        rightPrev = debug = rightColumn.add(new CheckBox("DEBUG"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = printpfmap = rightColumn.add(new CheckBox("Path Finder map in debug"), rightPrev.pos("bl").adds(0, 5));

        rightPrev = rightColumn.add(new Label("● Temporary Marks"), rightPrev.pos("bl").adds(0, 15));
        rightPrev = tempmark = rightColumn.add(new CheckBox("Save temporary marks"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = rightColumn.add(new Label("Max distance (grids):"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = temsmarkdistEntry = rightColumn.add(new TextEntry.NumberValue(50, ""), rightPrev.pos("bl").adds(0, 5));
        rightPrev = rightColumn.add(new Label("Storage duration (minutes):"), rightPrev.pos("bl").adds(0, 5));
        rightPrev = temsmarktimeEntry = rightColumn.add(new TextEntry.NumberValue(50, ""), rightPrev.pos("bl").adds(0, 5));

        // Pack columns and update content
        leftColumn.pack();
        rightColumn.pack();

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
        autoSaveTableware.a = getBool(NConfig.Key.autoSaveTableware);
        showBB.a = getBool(NConfig.Key.showBB);
        showCSprite.a = getBool(NConfig.Key.nextshowCSprite);

        hideNature.a = !getBool(NConfig.Key.hideNature);
        miningOL.a = getBool(NConfig.Key.miningol);
        tracking.a = getBool(NConfig.Key.tracking);
        crime.a = getBool(NConfig.Key.crime);
        swimming.a = getBool(NConfig.Key.swimming);
        openInventoryOnLogin.a = getBool(NConfig.Key.openInventoryOnLogin);
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

        // Load preferred movement speed
        Object speedPref = NConfig.get(NConfig.Key.preferredMovementSpeed);
        int speedIndex = 2; // Default to Run
        if (speedPref instanceof Number) {
            speedIndex = ((Number) speedPref).intValue();
        }
        if (speedIndex >= 0 && speedIndex < 4) {
            String[] speeds = {"Crawl", "Walk", "Run", "Sprint"};
            preferredSpeedDropbox.change(speeds[speedIndex]);
        }

        // Load preferred horse speed
        Object horseSpeedPref = NConfig.get(NConfig.Key.preferredHorseSpeed);
        int horseSpeedIndex = 2; // Default to Run
        if (horseSpeedPref instanceof Number) {
            horseSpeedIndex = ((Number) horseSpeedPref).intValue();
        }
        if (horseSpeedIndex >= 0 && horseSpeedIndex < 4) {
            String[] horseSpeeds = {"Crawl", "Walk", "Run", "Sprint"};
            preferredHorseSpeedDropbox.change(horseSpeeds[horseSpeedIndex]);
        }

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
        NConfig.set(NConfig.Key.autoSaveTableware, autoSaveTableware.a);
        NConfig.set(NConfig.Key.showBB, showBB.a);
        NConfig.set(NConfig.Key.nextshowCSprite, showCSprite.a);
        NConfig.set(NConfig.Key.hideNature, newHideNature);
        NConfig.set(NConfig.Key.miningol, miningOL.a);
        NConfig.set(NConfig.Key.tracking, tracking.a);
        NConfig.set(NConfig.Key.crime, crime.a);
        NConfig.set(NConfig.Key.swimming, swimming.a);
        NConfig.set(NConfig.Key.openInventoryOnLogin, openInventoryOnLogin.a);
        NConfig.set(NConfig.Key.disableMenugridKeys, disableMenugridKeys.a);
        NConfig.set(NConfig.Key.questNotified, questNotified.a);
        
        // Handle LP assistant setting change - remove overlays if disabled
        boolean oldLpassistent = getBool(NConfig.Key.lpassistent);
        NConfig.set(NConfig.Key.lpassistent, lpassistent.a);
        if(oldLpassistent != lpassistent.a) {
            if(!lpassistent.a) {
                // LP assistant was disabled - remove all LP assistant overlays
                if(NUtils.getGameUI() != null && NUtils.getGameUI().ui != null && NUtils.getGameUI().ui.sess != null) {
                    OCache oc = NUtils.getGameUI().ui.sess.glob.oc;
                    synchronized(oc) {
                        for(Gob gob : oc) {
                            if(gob != null) {
                                Gob.Overlay ol = gob.findol(NLPassistant.class);
                                if(ol != null) {
                                    ol.remove();
                                }
                            }
                        }
                    }
                }
            }
            // Force update config cache in all NGob instances to reflect the change immediately
            if(NUtils.getGameUI() != null && NUtils.getGameUI().ui != null && NUtils.getGameUI().ui.sess != null) {
                OCache oc = NUtils.getGameUI().ui.sess.glob.oc;
                synchronized(oc) {
                    for(Gob gob : oc) {
                        if(gob != null && gob.ngob != null) {
                            gob.ngob.updateConfigCache(true);
                        }
                    }
                }
            }
        }
        
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
