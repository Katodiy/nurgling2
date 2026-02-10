package nurgling.widgets.options;

import haven.*;
import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import nurgling.overlays.NLPassistant;
import nurgling.widgets.nsettings.Panel;

public class QoL extends Panel {
    private CheckBox showCropStage;
    private CheckBox simpleCrops;
    private CheckBox nightVision;
    private HSlider nightVisionBrightnessSlider;
    private Label nightVisionBrightnessLabel;
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
    private CheckBox debug;
    private CheckBox tempmark;
    private CheckBox tempmarkIgnoreDist;
    private CheckBox shortCupboards;
    private CheckBox shortWalls;
    private CheckBox decalsOnTop;
    private CheckBox thinOutlines;
    private CheckBox printpfmap;
    private CheckBox uniformBiomeColors;
    private CheckBox showTerrainName;
    private CheckBox verboseCal;
    private CheckBox showPersonalClaims;
    private CheckBox showVillageClaims;
    private CheckBox showRealmOverlays;
    private CheckBox disableDrugEffects;
    private CheckBox simpleInspect;
    private CheckBox alwaysObfuscate;
    private CheckBox randomAreaColor;
    private CheckBox treeScaleDisableZoomHide;
    private TextEntry treeScaleMinThresholdEntry;

    private Dropbox<String> preferredSpeedDropbox;
    private Dropbox<String> preferredHorseSpeedDropbox;
    private Dropbox<String> languageDropbox;
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
        Widget leftPrev = leftColumn.add(new Label("● " + L10n.get("qol.section.visual")), new Coord(5, 5));
        leftPrev = showCropStage = leftColumn.add(new CheckBox(L10n.get("qol.show_crop_stage")), leftPrev.pos("bl").adds(0, 10));
        leftPrev = simpleCrops = leftColumn.add(new CheckBox(L10n.get("qol.simple_crops")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = nightVision = leftColumn.add(new CheckBox(L10n.get("qol.night_vision")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = leftColumn.add(new Label(L10n.get("qol.night_vision_brightness")), leftPrev.pos("bl").adds(10, 3));
        {
            nightVisionBrightnessLabel = new Label("65%");
            nightVisionBrightnessSlider = new HSlider(UI.scale(150), 0, 100, 65) {
                public void changed() {
                    nightVisionBrightnessLabel.settext(String.format("%d%%", this.val));
                }
            };
            leftColumn.addhlp(leftPrev.pos("bl").adds(0, 2), UI.scale(5), nightVisionBrightnessSlider, nightVisionBrightnessLabel);
            leftPrev = nightVisionBrightnessSlider;
        }
        leftPrev = showBB = leftColumn.add(new CheckBox(L10n.get("qol.bounding_boxes")), leftPrev.pos("bl").adds(-10, 5));
        leftPrev = showCSprite = leftColumn.add(new CheckBox(L10n.get("qol.show_decorative")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = hideNature = leftColumn.add(new CheckBox(L10n.get("qol.hide_nature")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = uniformBiomeColors = leftColumn.add(new CheckBox(L10n.get("qol.uniform_biome")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showTerrainName = leftColumn.add(new CheckBox(L10n.get("qol.show_terrain_name")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = simpleInspect = leftColumn.add(new CheckBox(L10n.get("qol.simple_inspect")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = shortCupboards = leftColumn.add(new CheckBox(L10n.get("qol.short_cupboards")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = shortWalls = leftColumn.add(new CheckBox(L10n.get("qol.short_walls")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = decalsOnTop = leftColumn.add(new CheckBox(L10n.get("qol.decals_on_top")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = thinOutlines = leftColumn.add(new CheckBox(L10n.get("qol.thin_outlines")), leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label("● " + L10n.get("qol.section.tree_growth")), leftPrev.pos("bl").adds(0, 15));
        leftPrev = treeScaleDisableZoomHide = leftColumn.add(new CheckBox(L10n.get("qol.tree_always_show")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = leftColumn.add(new Label(L10n.get("qol.tree_min_threshold")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = treeScaleMinThresholdEntry = leftColumn.add(new TextEntry.NumberValue(50, "0"), leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label("● " + L10n.get("qol.section.network")), leftPrev.pos("bl").adds(0, 15));
        leftPrev = alwaysObfuscate = leftColumn.add(new CheckBox(L10n.get("qol.always_obfuscate")), leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label("● " + L10n.get("qol.section.login")), leftPrev.pos("bl").adds(0, 15));
        leftPrev = tracking = leftColumn.add(new CheckBox(L10n.get("qol.tracking")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = crime = leftColumn.add(new CheckBox(L10n.get("qol.crime")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = swimming = leftColumn.add(new CheckBox(L10n.get("qol.swimming")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = openInventoryOnLogin = leftColumn.add(new CheckBox(L10n.get("qol.open_inventory")), leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label(L10n.get("qol.preferred_speed")), leftPrev.pos("bl").adds(0, 10));
        leftPrev = preferredSpeedDropbox = leftColumn.add(new Dropbox<String>(UI.scale(150), 4, UI.scale(16)) {
            private String[] getSpeedNames() {
                return new String[]{L10n.get("qol.speed.crawl"), L10n.get("qol.speed.walk"), L10n.get("qol.speed.run"), L10n.get("qol.speed.sprint")};
            }

            @Override
            protected String listitem(int i) {
                return getSpeedNames()[i];
            }

            @Override
            protected int listitems() {
                return 4;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                String[] speeds = getSpeedNames();
                for (int i = 0; i < speeds.length; i++) {
                    if (speeds[i].equals(item)) {
                        NConfig.set(NConfig.Key.preferredMovementSpeed, i);
                        NConfig.needUpdate();
                        break;
                    }
                }
            }
        }, leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label(L10n.get("qol.preferred_horse_speed")), leftPrev.pos("bl").adds(0, 10));
        leftPrev = preferredHorseSpeedDropbox = leftColumn.add(new Dropbox<String>(UI.scale(150), 4, UI.scale(16)) {
            private String[] getSpeedNames() {
                return new String[]{L10n.get("qol.speed.crawl"), L10n.get("qol.speed.walk"), L10n.get("qol.speed.run"), L10n.get("qol.speed.sprint")};
            }

            @Override
            protected String listitem(int i) {
                return getSpeedNames()[i];
            }

            @Override
            protected int listitems() {
                return 4;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                String[] speeds = getSpeedNames();
                for (int i = 0; i < speeds.length; i++) {
                    if (speeds[i].equals(item)) {
                        NConfig.set(NConfig.Key.preferredHorseSpeed, i);
                        NConfig.needUpdate();
                        break;
                    }
                }
            }
        }, leftPrev.pos("bl").adds(0, 5));

        leftPrev = leftColumn.add(new Label("● " + L10n.get("qol.section.map_overlays")), leftPrev.pos("bl").adds(0, 15));
        leftPrev = miningOL = leftColumn.add(new CheckBox(L10n.get("qol.mining_overlay")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showPersonalClaims = leftColumn.add(new CheckBox(L10n.get("qol.personal_claims")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showVillageClaims = leftColumn.add(new CheckBox(L10n.get("qol.village_claims")), leftPrev.pos("bl").adds(0, 5));
        leftPrev = showRealmOverlays = leftColumn.add(new CheckBox(L10n.get("qol.realm_overlays")), leftPrev.pos("bl").adds(0, 5));

        // RIGHT COLUMN - Advanced Settings
        Widget rightPrev = null;
        rightPrev = rightColumn.add(new Label("● " + L10n.get("qol.section.language")), new Coord(5, 5));
        rightPrev = languageDropbox = rightColumn.add(new Dropbox<String>(UI.scale(150), L10n.SUPPORTED_LANGUAGES.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return L10n.SUPPORTED_LANGUAGES[i][1]; // Display name
            }

            @Override
            protected int listitems() {
                return L10n.SUPPORTED_LANGUAGES.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                // Find language code by display name
                for (String[] lang : L10n.SUPPORTED_LANGUAGES) {
                    if (lang[1].equals(item)) {
                        NConfig.set(NConfig.Key.language, lang[0]);
                        NConfig.needUpdate();
                        L10n.setLanguage(lang[0]);
                        // Notify user that restart may be needed for full effect
                        if (NUtils.getGameUI() != null) {
                            NUtils.getGameUI().msg(L10n.get("msg.language_changed"));
                        }
                        break;
                    }
                }
            }
        }, rightPrev.pos("bl").adds(0, 5));

        rightPrev = rightColumn.add(new Label("● " + L10n.get("qol.section.qol")), rightPrev.pos("bl").adds(0, 15));
        rightPrev = autoDrink = rightColumn.add(new CheckBox(L10n.get("qol.auto_drink")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = autoSaveTableware = rightColumn.add(new CheckBox(L10n.get("qol.auto_save_tableware")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = questNotified = rightColumn.add(new CheckBox(L10n.get("qol.quest_notified")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = lpassistent = rightColumn.add(new CheckBox(L10n.get("qol.lp_assistant")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = disableMenugridKeys = rightColumn.add(new CheckBox(L10n.get("qol.disable_menugrid")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = verboseCal = rightColumn.add(new CheckBox(L10n.get("qol.verbose_cal")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = disableDrugEffects = rightColumn.add(new CheckBox(L10n.get("qol.disable_drugs")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = randomAreaColor = rightColumn.add(new CheckBox(L10n.get("qol.random_area_color")), rightPrev.pos("bl").adds(0, 5));

        rightPrev = rightColumn.add(new Label("● " + L10n.get("qol.section.debug")), rightPrev.pos("bl").adds(0, 15));
        rightPrev = debug = rightColumn.add(new CheckBox(L10n.get("qol.debug")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = printpfmap = rightColumn.add(new CheckBox(L10n.get("qol.printpfmap")), rightPrev.pos("bl").adds(0, 5));

        rightPrev = rightColumn.add(new Label("● " + L10n.get("qol.section.temp_marks")), rightPrev.pos("bl").adds(0, 15));
        rightPrev = tempmark = rightColumn.add(new CheckBox(L10n.get("qol.save_temp_marks")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = tempmarkIgnoreDist = rightColumn.add(new CheckBox(L10n.get("qol.ignore_distance")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = rightColumn.add(new Label(L10n.get("qol.max_distance")), rightPrev.pos("bl").adds(0, 5));
        rightPrev = temsmarkdistEntry = rightColumn.add(new TextEntry.NumberValue(50, ""), rightPrev.pos("bl").adds(0, 5));
        rightPrev = rightColumn.add(new Label(L10n.get("qol.storage_duration")), rightPrev.pos("bl").adds(0, 5));
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
        
        // Load night vision brightness
        Object brightnessPref = NConfig.get(NConfig.Key.nightVisionBrightness);
        int brightnessValue = 65; // Default
        if (brightnessPref instanceof Number) {
            brightnessValue = (int)(((Number) brightnessPref).doubleValue() * 100);
        }
        nightVisionBrightnessSlider.val = brightnessValue;
        nightVisionBrightnessLabel.settext(String.format("%d%%", brightnessValue));
        
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
        debug.a = getBool(NConfig.Key.debug);
        printpfmap.a = getBool(NConfig.Key.printpfmap);
        tempmark.a = getBool(NConfig.Key.tempmark);
        tempmarkIgnoreDist.a = getBool(NConfig.Key.tempmarkIgnoreDist);
        shortCupboards.a = getBool(NConfig.Key.shortCupboards);
        shortWalls.a = getBool(NConfig.Key.shortWalls);
        decalsOnTop.a = getBool(NConfig.Key.decalsOnTop);
        thinOutlines.a = getBool(NConfig.Key.thinOutlines);
        uniformBiomeColors.a = getBool(NConfig.Key.uniformBiomeColors);
        showTerrainName.a = getBool(NConfig.Key.showTerrainName);
        simpleInspect.a = getBool(NConfig.Key.simpleInspect);
        verboseCal.a = getBool(NConfig.Key.verboseCal);
        showPersonalClaims.a = getBool(NConfig.Key.minimapClaimol);
        showVillageClaims.a = getBool(NConfig.Key.minimapVilol);
        showRealmOverlays.a = getBool(NConfig.Key.minimapRealmol);
        disableDrugEffects.a = getBool(NConfig.Key.disableDrugEffects);
        alwaysObfuscate.a = getBool(NConfig.Key.alwaysObfuscate);
        randomAreaColor.a = getBool(NConfig.Key.randomAreaColor);
        treeScaleDisableZoomHide.a = getBool(NConfig.Key.treeScaleDisableZoomHide);
        
        Object minThreshold = NConfig.get(NConfig.Key.treeScaleMinThreshold);
        treeScaleMinThresholdEntry.settext(minThreshold == null ? "0" : minThreshold.toString());

        // Load language setting
        Object langPref = NConfig.get(NConfig.Key.language);
        String currentLang = langPref != null ? langPref.toString() : L10n.getLanguage();
        for (int i = 0; i < L10n.SUPPORTED_LANGUAGES.length; i++) {
            if (L10n.SUPPORTED_LANGUAGES[i][0].equals(currentLang)) {
                languageDropbox.change(L10n.SUPPORTED_LANGUAGES[i][1]);
                break;
            }
        }

        // Load preferred movement speed
        Object speedPref = NConfig.get(NConfig.Key.preferredMovementSpeed);
        int speedIndex = 2; // Default to Run
        if (speedPref instanceof Number) {
            speedIndex = ((Number) speedPref).intValue();
        }
        if (speedIndex >= 0 && speedIndex < 4) {
            String[] speeds = {L10n.get("qol.speed.crawl"), L10n.get("qol.speed.walk"), L10n.get("qol.speed.run"), L10n.get("qol.speed.sprint")};
            preferredSpeedDropbox.change(speeds[speedIndex]);
        }

        // Load preferred horse speed
        Object horseSpeedPref = NConfig.get(NConfig.Key.preferredHorseSpeed);
        int horseSpeedIndex = 2; // Default to Run
        if (horseSpeedPref instanceof Number) {
            horseSpeedIndex = ((Number) horseSpeedPref).intValue();
        }
        if (horseSpeedIndex >= 0 && horseSpeedIndex < 4) {
            String[] horseSpeeds = {L10n.get("qol.speed.crawl"), L10n.get("qol.speed.walk"), L10n.get("qol.speed.run"), L10n.get("qol.speed.sprint")};
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

    public void syncMiningOverlay() {
        miningOL.a = getBool(NConfig.Key.miningol);
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
        NConfig.set(NConfig.Key.nightVisionBrightness, nightVisionBrightnessSlider.val / 100.0);
        
        // Update brightness immediately
        if(NUtils.getGameUI() != null && NUtils.getGameUI().ui != null && NUtils.getGameUI().ui.sess != null && NUtils.getGameUI().ui.sess.glob != null) {
            NUtils.getGameUI().ui.sess.glob.brighten();
        }
        
        NConfig.set(NConfig.Key.autoDrink, autoDrink.a);
        NConfig.set(NConfig.Key.autoSaveTableware, autoSaveTableware.a);
        NConfig.set(NConfig.Key.showBB, showBB.a);
        NConfig.set(NConfig.Key.nextshowCSprite, showCSprite.a);
        NConfig.set(NConfig.Key.hideNature, newHideNature);
        
        // Save mining overlay and sync with minimap button
        boolean oldMiningOL = getBool(NConfig.Key.miningol);
        NConfig.set(NConfig.Key.miningol, miningOL.a);
        if(oldMiningOL != miningOL.a) {
            // Sync with minimap button
            if(NUtils.getGameUI() != null && NUtils.getGameUI().mmapw != null && NUtils.getGameUI().mmapw.minesup != null) {
                NUtils.getGameUI().mmapw.minesup.a = miningOL.a;
            }
        }
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
        
        NConfig.set(NConfig.Key.debug, debug.a);
        NConfig.set(NConfig.Key.printpfmap, printpfmap.a);
        NConfig.set(NConfig.Key.tempmark, tempmark.a);
        NConfig.set(NConfig.Key.tempmarkIgnoreDist, tempmarkIgnoreDist.a);
        
        // Save cupboard settings and rebuild cupboards if changed
        boolean oldShortCupboards = getBool(NConfig.Key.shortCupboards);
        boolean oldDecalsOnTop = getBool(NConfig.Key.decalsOnTop);
        NConfig.set(NConfig.Key.shortCupboards, shortCupboards.a);
        NConfig.set(NConfig.Key.decalsOnTop, decalsOnTop.a);
        if(oldShortCupboards != shortCupboards.a || oldDecalsOnTop != decalsOnTop.a) {
            rebuildCupboards();
        }

        NConfig.set(NConfig.Key.thinOutlines, thinOutlines.a);

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
        NConfig.set(NConfig.Key.simpleInspect, simpleInspect.a);
        NConfig.set(NConfig.Key.verboseCal, verboseCal.a);
        NConfig.set(NConfig.Key.disableDrugEffects, disableDrugEffects.a);
        NConfig.set(NConfig.Key.alwaysObfuscate, alwaysObfuscate.a);
        NConfig.set(NConfig.Key.randomAreaColor, randomAreaColor.a);
        NConfig.set(NConfig.Key.treeScaleDisableZoomHide, treeScaleDisableZoomHide.a);
        
        int minThreshold = parseIntOrDefault(treeScaleMinThresholdEntry.text(), 0);
        NConfig.set(NConfig.Key.treeScaleMinThreshold, minThreshold);

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
                if(NUtils.getGameUI().mmapw.miniMap instanceof nurgling.widgets.NMiniMap) {
                    ((nurgling.widgets.NMiniMap)NUtils.getGameUI().mmapw.miniMap).invalidateDisplayCache();
                }
                NUtils.getGameUI().mmapw.miniMap.needUpdate = true;
            }
            // Also update main map if it exists
            if(NUtils.getGameUI() != null && NUtils.getGameUI().mapfile != null && NUtils.getGameUI().mapfile.view != null) {
                if(NUtils.getGameUI().mapfile.view instanceof nurgling.widgets.NMiniMap) {
                    ((nurgling.widgets.NMiniMap)NUtils.getGameUI().mapfile.view).invalidateDisplayCache();
                }
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
    
    /**
     * Rebuilds all cupboard gobs to apply changed settings (shortCupboards, decalsOnTop).
     * Updates NCustomScale attribute and recreates decal overlays.
     */
    private void rebuildCupboards() {
        if(NUtils.getGameUI() == null || NUtils.getGameUI().ui == null || NUtils.getGameUI().ui.sess == null) {
            return;
        }
        OCache oc = NUtils.getGameUI().ui.sess.glob.oc;
        synchronized(oc) {
            for(Gob gob : oc) {
                if(gob != null && gob.ngob != null && gob.ngob.name != null 
                    && gob.ngob.name.contains("cupboard")) {
                    // Update config cache to reflect new settings
                    gob.ngob.updateConfigCache(true);
                    
                    // Update NCustomScale for short cupboards
                    if(shortCupboards.a) {
                        if(gob.getattr(nurgling.gattrr.NCustomScale.class) == null) {
                            gob.setattr(new nurgling.gattrr.NCustomScale(gob));
                        }
                    } else {
                        gob.delattr(nurgling.gattrr.NCustomScale.class);
                    }
                    
                    // Recreate parchment-decal overlays so bone offset is re-evaluated
                    java.util.List<Gob.Overlay> decalsToRecreate = new java.util.ArrayList<>();
                    for(Gob.Overlay ol : gob.ols) {
                        if(ol.spr != null && ol.spr.res != null 
                            && ol.spr.res.name.contains("parchment-decal")
                            && ol.sm instanceof OCache.OlSprite) {
                            decalsToRecreate.add(ol);
                        }
                    }
                    
                    for(Gob.Overlay ol : decalsToRecreate) {
                        OCache.OlSprite os = (OCache.OlSprite) ol.sm;
                        int olid = ol.id;
                        // Remove old overlay
                        ol.remove(false);
                        // Create new overlay with same data - bone offset will be re-evaluated
                        Gob.Overlay newOl = new Gob.Overlay(gob, olid, new OCache.OlSprite(os.res, os.sdt));
                        gob.addol(newOl, false);
                    }
                }
            }
        }
    }
}
