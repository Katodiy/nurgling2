package nurgling;

import haven.*;
import nurgling.areas.*;
import nurgling.conf.*;
import nurgling.conf.QuickActionPreset;
import nurgling.profiles.ProfileManager;
import nurgling.scenarios.Scenario;
import nurgling.widgets.NCornerMiniMap;
import org.json.*;

import java.awt.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.text.ParseException;
import java.util.*;
import java.util.stream.*;

public class NConfig
{
    public static NCore.BotmodSettings botmod = null;

    public enum Key
    {
        vilol, claimol, realmol,
        minimapVilol, minimapClaimol, minimapRealmol,
        selectedWorld,
        showVarity,
        autoFlower,
        autoSplitter,
        autoDropper,
        is_real_time,
        baseurl,
        credentials,
        dragprop,
        numbelts,
        toolbelts,
        resizeprop,
        showCropStage,
        nightVision,
        nightVisionBrightness,
        showBB,
        nextflatsurface,
        flatsurface,
        nextshowCSprite,
        showCSprite,
        hideNature,
        hideEarthworm,
        invert_hor,
        invert_ver,
        kinprop,
        show_drag_menu,
        cowsprop,
        horseprop,
        goatsprop,
        chopperprop,
        blueprintplanterprop,
        autofloweractionprop,
        carrierprop,
        prepblockprop,
        prepboardprop,
        sheepsprop,
        pigsprop,
        discordNotification,
        discordWebhookUrl,
        showGrid,
        showView,
        disableWinAnim,
        disableMenugridKeys,
        crime,
        tracking,
        swimming,
        debug,
        claydiggerprop,
        miningol,
        q_pattern,
        q_range,
        q_visitor,
        q_door,
        q_presets,
        q_current_preset,
        petals,
        singlePetal,
        asenable,
        autoMapper,
        endpoint,
        automaptrack,
        unloadgreen,
        sendOverlays,
        showInventoryNums,
        hidecredo,
        autoDrink,
        autoSaveTableware,
        chipperprop,
        animalrad,
        smokeprop,
        worldexplorerprop,
        questNotified, lpassistent, fishingsettings,
        serverNode, serverUser, serverPass, postgresMaxConnections, ndbenable, dbStatsOverlay, harvestautorefill, cleanupQContainers, autoEquipTravellersSacks, qualityGrindSeedingPatter, postgres, sqlite, dbFilePath, simplecrops,
        temsmarktime, exploredAreaEnable, chunkNavOverlay, player_box, player_fov, temsmarkdist, tempmark, tempmarkIgnoreDist, gridbox, useGlobalPf, useHFinGlobalPF, boxFillColor, boxEdgeColor, boxLineWidth, ropeAfterFeeding, ropeAfterTaiming, eatingConf, deersprop,dropConf, printpfmap, fonts,
        areaRankPresets,  // Map of areaId -> Map of animalType -> presetName
        shortCupboards,
        shortWalls,
        decalsOnTop,
        fillCompostWithSwill,
        ignoreStrawInFarmers,
        persistentBarrelLabels,
        uniformBiomeColors,
        inventoryRightPanelShow,
        inventoryRightPanelMode,
        showTerrainName,
        validateAllCropsBeforeHarvest,
        studyDeskLayout,
        waypointRetryOnStuck,
        verboseCal,
        highlightRockTiles,
        preferredMovementSpeed,
        preferredHorseSpeed,
        uiOpacity,
        useSolidBackground,
        windowBackgroundColor,
        picklingBeetroots,
        picklingCarrots,
        picklingEggs,
        picklingHerring,
        picklingOlives,
        picklingCucumbers,
        picklingRedOnion,
        picklingYellowOnion,
        openInventoryOnLogin,
        bbDisplayMode,
        showBeehiveRadius,
        showTroughRadius,
        showMoundBedRadius,
        showDamageShields,
        disableTileSmoothing,
        disableTileTransitions,
        disableCloudShadows,
        darkenDeepOcean,
        disableDrugEffects,
        simpleInspect,
        showSpeedometer,
        showPathLine,
        pathLineWidth,
        pathLineColor,
        parasiteBotEnabled,
        leechAction,
        tickAction,
        autoHearthOnUnknown,
        autoLogoutOnUnknown,
        alarmDelayFrames,
        alwaysObfuscate,
        boughbeeprop,
        foragerprop,
        trufflepigprop,
        buttonStyle,
        showQuestGiverNames,
        showThingwallNames,
        showPartyMemberNames,
        trackingVectors,
        randomAreaColor,
        treeScaleDisableZoomHide,
        treeScaleMinThreshold,
        thinOutlines,
        itemQualityOverlay,
        stackQualityOverlay,
        amountOverlay,
        studyInfoOverlay,
        progressOverlay,
        volumeOverlay,
        equipProxySlots,
        equipmentBotConfig,
        // Starvation alert settings
        starvationAlertEnabled,
        starvationPopup1Threshold,
        starvationPopup2Threshold,
        starvationVignetteStartThreshold,
        starvationVignetteCriticalThreshold,
        starvationSoundThreshold,
        starvationSoundInterval,
        // Localization
        language
    }

    public enum BBDisplayMode
    {
        FILLED,           // Fill and outline both with depth test (both hidden behind objects)
        FILLED_ALWAYS,    // Fill with depth test (hidden), outline without depth test (always visible)
        OUTLINE,          // Outline only with depth test (hidden behind objects)
        OUTLINE_ALWAYS,   // Outline only without depth test (always visible)
        OFF               // Disabled
    }


    public NConfig() {
        this(null);
    }

    public NConfig(String genus) {
        this.genus = genus;
        if (genus != null && !genus.isEmpty()) {
            this.profileManager = new ProfileManager(genus);
            this.profileManager.ensureProfileExists();
        }
        conf = new HashMap<>();

        conf.put(Key.vilol, false);
        conf.put(Key.claimol, false);
        conf.put(Key.realmol, false);
        conf.put(Key.minimapVilol, false);
        conf.put(Key.minimapClaimol, false);
        conf.put(Key.minimapRealmol, false);
        conf.put(Key.selectedWorld, null);
        conf.put(Key.showVarity, false);
        conf.put(Key.autoFlower, false);
        conf.put(Key.autoSplitter, false);
        conf.put(Key.autoDropper, false);
        conf.put(Key.is_real_time, true);
        conf.put(Key.numbelts, 3);
        conf.put(Key.showCropStage, false);
        conf.put(Key.nightVision, false);
        conf.put(Key.nightVisionBrightness, 0.65);
        conf.put(Key.showBB, false);
        conf.put(Key.bbDisplayMode, "FILLED");
        conf.put(Key.nextflatsurface, false);
        conf.put(Key.flatsurface, false);
        conf.put(Key.nextshowCSprite, false);
        conf.put(Key.showCSprite, true);
        conf.put(Key.hideNature, false);
        conf.put(Key.hideEarthworm, true);  // true = show earthworms (checkbox unchecked by default)
        conf.put(Key.invert_hor, false);
        conf.put(Key.invert_ver, false);
        conf.put(Key.show_drag_menu, true);
        conf.put(Key.discordWebhookUrl, "");
        conf.put(Key.showGrid, false);
        conf.put(Key.showView, false);
        conf.put(Key.disableWinAnim, true);
        conf.put(Key.disableMenugridKeys, false);
        conf.put(Key.baseurl, " https://raw.githubusercontent.com/Katodiy/nurgling-release/master/ver");
        conf.put(Key.miningol, true);
        conf.put(Key.crime, false);
        conf.put(Key.tracking, false);
        conf.put(Key.swimming, false);
        conf.put(Key.debug, false);
        conf.put(Key.hidecredo, true);
        conf.put(Key.q_visitor, false);
        conf.put(Key.q_door, true);
        conf.put(Key.q_range, 2);
        conf.put(Key.q_current_preset, "Default");
        conf.put(Key.singlePetal, false);
        conf.put(Key.asenable, true);
        conf.put(Key.autoMapper, false);
        conf.put(Key.automaptrack, false);
        conf.put(Key.unloadgreen, false);
        conf.put(Key.sendOverlays, false);
        conf.put(Key.showInventoryNums, true);
        conf.put(Key.autoDrink, false);
        conf.put(Key.autoSaveTableware, true);
        conf.put(Key.endpoint, "");
        conf.put(Key.questNotified, false);
        conf.put(Key.lpassistent, false);
        conf.put(Key.simplecrops, true);
        conf.put(Key.simpleInspect, false);
        conf.put(Key.ndbenable, false);
        conf.put(Key.dbStatsOverlay, false);
        conf.put(Key.harvestautorefill, false);
        conf.put(Key.cleanupQContainers, false);
        conf.put(Key.autoEquipTravellersSacks, false);
        conf.put(Key.qualityGrindSeedingPatter, "4x1");
        conf.put(Key.useGlobalPf, false);
        conf.put(Key.useHFinGlobalPF, false);
        conf.put(Key.sqlite, false);
        conf.put(Key.postgres, false);
        conf.put(Key.postgresMaxConnections, 5);
        conf.put(Key.dbFilePath, "");
        conf.put(Key.serverNode, "");
        conf.put(Key.serverPass, "");
        conf.put(Key.serverUser, "");
        conf.put(Key.exploredAreaEnable, false);
        conf.put(Key.chunkNavOverlay, false);
        conf.put(Key.player_box, false);
        conf.put(Key.player_fov, false);
        conf.put(Key.gridbox, false);
        conf.put(Key.tempmark, false);
        conf.put(Key.tempmarkIgnoreDist, false);
        conf.put(Key.temsmarkdist, 4);
        conf.put(Key.temsmarktime, 3);
        conf.put(Key.fonts, new FontSettings());
        conf.put(Key.ropeAfterFeeding, false);
        conf.put(Key.ropeAfterTaiming, true);
        conf.put(Key.shortCupboards, false);
        conf.put(Key.shortWalls, false);
        conf.put(Key.decalsOnTop, false);
        conf.put(Key.fillCompostWithSwill, false);
        conf.put(Key.ignoreStrawInFarmers, false);
        conf.put(Key.printpfmap, false);
        conf.put(Key.boxLineWidth, 4);
        conf.put(Key.persistentBarrelLabels, false);
        conf.put(Key.uniformBiomeColors, false);
        conf.put(Key.inventoryRightPanelShow, false);
        conf.put(Key.inventoryRightPanelMode, "EXPANDED");
        conf.put(Key.showTerrainName, false);
        conf.put(Key.validateAllCropsBeforeHarvest, false);
        conf.put(Key.studyDeskLayout, "");
        conf.put(Key.waypointRetryOnStuck, true);
        conf.put(Key.verboseCal, false);
        conf.put(Key.highlightRockTiles, true);
        conf.put(Key.showSpeedometer, false);
        conf.put(Key.showPathLine, false);
        conf.put(Key.pathLineWidth, 4);
        conf.put(Key.pathLineColor, new Color(255, 255, 0));  // Yellow

        ArrayList<HashMap<String, Object>> qpattern = new ArrayList<>();
        HashMap<String, Object> res1 = new HashMap<>();
        res1.put("type", "NPattern");
        res1.put("name", ".*cart");
        res1.put("enabled", true);
        qpattern.add(res1);
        HashMap<String, Object> res2 = new HashMap<>();
        res2.put("type", "NPattern");
        res2.put("name", "gfx/kritter/.*");
        res2.put("enabled", true);
        qpattern.add(res2);
        HashMap<String, Object> res3 = new HashMap<>();
        res3.put("type", "NPattern");
        res3.put("name", "gfx/terobjs/herbs.*");
        res3.put("enabled", true);
        qpattern.add(res3);
        conf.put(Key.q_pattern, qpattern);

        // Quick Action Presets
        ArrayList<QuickActionPreset> qpresets = new ArrayList<>();
        qpresets.add(QuickActionPreset.createDefault());
        conf.put(Key.q_presets, qpresets);

        ArrayList<HashMap<String, Object>> petal = new ArrayList<>();
        HashMap<String, Object> pres1 = new HashMap<>();
        pres1.put("type", "NPetal");
        pres1.put("name", "Giddyup!");
        pres1.put("enabled", true);
        petal.add(pres1);
        conf.put(Key.petals, petal);

        ArrayList<NDragProp> dragprop = new ArrayList<>();
        dragprop.add(new NDragProp(new Coord(570, 108), false, true, "Fightview"));
        dragprop.add(new NDragProp(new Coord(549, -12), false, true, "minimap"));
        dragprop.add(new NDragProp(new Coord(524, 84), false, true, "quests"));
        dragprop.add(new NDragProp(new Coord(493, 441), false, true, "menugrid"));
        dragprop.add(new NDragProp(new Coord(-4, 84), false, true, "speedmeter"));
        dragprop.add(new NDragProp(new Coord(-4, 400), false, true, "ChatUI"));
        dragprop.add(new NDragProp(new Coord(-4, 348), false, true, "belt0"));
        dragprop.add(new NDragProp(new Coord(-4, 318), false, true, "belt1"));
        dragprop.add(new NDragProp(new Coord(-4, 288), false, true, "belt2"));
        dragprop.add(new NDragProp(new Coord(508, 396), false, true, "mainmenu"));
        dragprop.add(new NDragProp(new Coord(-4, 124), false, true, "metergfx/hud/meter/hp"));
        dragprop.add(new NDragProp(new Coord(-4, 164), false, true, "metergfx/hud/meter/stam"));
        dragprop.add(new NDragProp(new Coord(-4, 204), false, true, "metergfx/hud/meter/nrj"));
        dragprop.add(new NDragProp(new Coord(-4, 244), false, true, "botsmenu"));
        dragprop.add(new NDragProp(new Coord(-4, 300), false, true, "EquipProxy"));
        dragprop.add(new NDragProp(new Coord(620, 212), false, true, "alarm"));
        dragprop.add(new NDragProp(new Coord(156, -4), false, true, "Calendar"));
        dragprop.add(new NDragProp(new Coord(428, -4), false, true, "bufflist"));
        dragprop.add(new NDragProp(new Coord(60, 244), false, true, "party"));
        dragprop.add(new NDragProp(new Coord(300, 550), false, true, "BeltProxy"));
        conf.put(Key.dragprop, dragprop);

        ArrayList<NAreaRad> arearadprop = new ArrayList<>();
        arearadprop.add(new NAreaRad("gfx/kritter/bat/bat", 50));
        arearadprop.add(new NAreaRad("gfx/kritter/boar/boar", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/bear/bear", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/adder/adder", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/wildgoat/wildgoat", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/badger/badger", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/lynx/lynx", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/mammoth/mammoth", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/moose/moose", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/wolf/wolf", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/walrus/walrus", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/orca/orca", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/wolverine/wolverine", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/eagleowl/eagleowl", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/goldeneagle/goldeneagle", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/goat/goat", 100));
        arearadprop.add(new NAreaRad("gfx/kritter/troll/troll", 200));
        arearadprop.add(new NAreaRad("gfx/kritter/rat/rat", 200));
        arearadprop.add(new NAreaRad("gfx/kritter/eagle/eagle", 200));
        arearadprop.add(new NAreaRad("gfx/kritter/cavelouse/cavelouse", 200));
        arearadprop.add(new NAreaRad("gfx/kritter/boreworm/boreworm", 200));
        conf.put(Key.animalrad, arearadprop);

        // Movement speed setting (0=Crawl, 1=Walk, 2=Run, 3=Sprint)
        conf.put(Key.preferredMovementSpeed, 2);  // Default to Run (unchanged)
        conf.put(Key.preferredHorseSpeed, 2);     // Default to Run for horses (unchanged)

        // UI Opacity settings
        conf.put(Key.uiOpacity, 1.0f);  // Default to fully opaque
        conf.put(Key.useSolidBackground, false);  // Default to texture mode
        conf.put(Key.windowBackgroundColor, new java.awt.Color(32, 32, 32));  // Default dark gray
        conf.put(Key.buttonStyle, "tbtn");  // Default button style

        // Pickling settings
        conf.put(Key.picklingBeetroots, true);
        conf.put(Key.picklingCarrots, true);
        conf.put(Key.picklingEggs, true);
        conf.put(Key.picklingHerring, true);
        conf.put(Key.picklingOlives, true);
        conf.put(Key.picklingCucumbers, true);
        conf.put(Key.picklingRedOnion, true);
        conf.put(Key.picklingYellowOnion, true);

        // Login settings
        conf.put(Key.openInventoryOnLogin, false);  // Default to closed (current behavior)

        // Object radius overlays - simple boolean flags
        conf.put(Key.showBeehiveRadius, false);
        conf.put(Key.showTroughRadius, false);
        conf.put(Key.showMoundBedRadius, false);

        // Damage shields display
        conf.put(Key.showDamageShields, true);

        // Terrain tile rendering settings
        conf.put(Key.disableTileSmoothing, false);
        conf.put(Key.disableTileTransitions, false);
        conf.put(Key.disableCloudShadows, false);
        conf.put(Key.darkenDeepOcean, false);
        conf.put(Key.disableDrugEffects, true);  // Default to disabled for better performance
        
        // Parasite bot settings
        conf.put(Key.parasiteBotEnabled, false);
        conf.put(Key.leechAction, "ground");  // "nothing", "ground" or "inventory"
        conf.put(Key.tickAction, "ground");   // "nothing", "ground" or "inventory"
        
        // Safety settings - auto hearth/logout on unknown players
        conf.put(Key.autoHearthOnUnknown, false);
        conf.put(Key.autoLogoutOnUnknown, false);
        conf.put(Key.alarmDelayFrames, 20);  // Delay before unknown player triggers alarm
        
        // Auth obfuscation - bypass firewall blocks
        conf.put(Key.alwaysObfuscate, false);
        
        // Map marker name display settings
        conf.put(Key.showQuestGiverNames, true);
        conf.put(Key.showThingwallNames, true);
        conf.put(Key.showPartyMemberNames, true);
        
        // Map tracking vectors
        conf.put(Key.trackingVectors, false);
        
        // Random area color on creation
        conf.put(Key.randomAreaColor, false);
        
        // Tree scale overlay settings
        conf.put(Key.treeScaleDisableZoomHide, false);  // If true, always show full label (don't hide on zoom out)
        conf.put(Key.treeScaleMinThreshold, 0);  // Minimum growth % to display tree scale (0 = show all)

        // Outline rendering settings
        conf.put(Key.thinOutlines, false);  // If true, use thinner object outlines

        // Item quality overlay settings
        conf.put(Key.itemQualityOverlay, new ItemQualityOverlaySettings());
        // Stack quality overlay settings
        ItemQualityOverlaySettings stackDefaults = new ItemQualityOverlaySettings();
        stackDefaults.corner = ItemQualityOverlaySettings.Corner.TOP_LEFT;
        conf.put(Key.stackQualityOverlay, stackDefaults);
        // Amount overlay settings
        ItemQualityOverlaySettings amountDefaults = new ItemQualityOverlaySettings();
        amountDefaults.corner = ItemQualityOverlaySettings.Corner.BOTTOM_RIGHT;
        amountDefaults.useThresholds = false;
        conf.put(Key.amountOverlay, amountDefaults);
        // Study info overlay settings
        ItemQualityOverlaySettings studyDefaults = new ItemQualityOverlaySettings();
        studyDefaults.corner = ItemQualityOverlaySettings.Corner.BOTTOM_LEFT;
        studyDefaults.useThresholds = false;
        studyDefaults.defaultColor = new java.awt.Color(255, 255, 50);
        conf.put(Key.studyInfoOverlay, studyDefaults);
        // Progress/meter overlay settings
        ItemQualityOverlaySettings progressDefaults = new ItemQualityOverlaySettings();
        progressDefaults.corner = ItemQualityOverlaySettings.Corner.BOTTOM_LEFT;
        progressDefaults.useThresholds = false;
        progressDefaults.defaultColor = new java.awt.Color(234, 164, 101);
        progressDefaults.showBackground = true;
        conf.put(Key.progressOverlay, progressDefaults);
        // Volume overlay settings (CustomName - kg/l)
        ItemQualityOverlaySettings volumeDefaults = new ItemQualityOverlaySettings();
        volumeDefaults.corner = ItemQualityOverlaySettings.Corner.TOP_LEFT;
        volumeDefaults.useThresholds = false;
        volumeDefaults.defaultColor = new java.awt.Color(65, 255, 115);
        volumeDefaults.showBackground = true;
        conf.put(Key.volumeOverlay, volumeDefaults);

        // Equipment proxy slots - default to Left Hand, Right Hand, Belt
        ArrayList<Integer> defaultEquipProxySlots = new ArrayList<>();
        defaultEquipProxySlots.add(6);  // HAND_LEFT
        defaultEquipProxySlots.add(7);  // HAND_RIGHT
        defaultEquipProxySlots.add(5);  // BELT
        conf.put(Key.equipProxySlots, defaultEquipProxySlots);

        // Starvation alert settings
        conf.put(Key.starvationAlertEnabled, true);
        conf.put(Key.starvationPopup1Threshold, 2700);  // First warning popup (0 to disable)
        conf.put(Key.starvationPopup2Threshold, 2500);  // Critical warning popup (0 to disable)
        conf.put(Key.starvationVignetteStartThreshold, 2300);  // Vignette starts (0 to disable)
        conf.put(Key.starvationVignetteCriticalThreshold, 2000);  // Vignette intensifies (0 to disable)
        conf.put(Key.starvationSoundThreshold, 2000);  // Sound alarm threshold (0 to disable)
        conf.put(Key.starvationSoundInterval, 10000);  // Sound interval in milliseconds
    }


    HashMap<Key, Object> conf = new HashMap<>();
    private boolean isUpd = false;
    private boolean isAreasUpd = false;
    private long lastAreasChangeTime = 0;
    private static final long AREAS_DEBOUNCE_MS = 3000; // 3 seconds debounce for area changes
    private boolean isExploredUpd = false;
    private long lastExploredChangeTime = 0;
    private static final long EXPLORED_DEBOUNCE_MS = 5000; // 5 seconds debounce for explored area changes
    private boolean isRoutesUpd = false;
    private boolean isScenariosUpd = false;
    String path = NUtils.getDataFile("nconfig.nurgling.json");

    public boolean isUpdated()
    {
        return isUpd;
    }

    public boolean isAreasUpdated()
    {
        // Only return true if areas changed AND debounce period has passed
        // This batches multiple rapid changes into a single DB update
        if (isAreasUpd && lastAreasChangeTime > 0) {
            long elapsed = System.currentTimeMillis() - lastAreasChangeTime;
            return elapsed >= AREAS_DEBOUNCE_MS;
        }
        return false;
    }

    public boolean isRoutesUpdated() {
        return isRoutesUpd;
    }

    public boolean isScenariosUpdated() {
        return isScenariosUpd;
    }

    public boolean isExploredUpdated() {
        // Only return true if explored area changed AND debounce period has passed
        // This batches multiple rapid changes into a single file update
        if (isExploredUpd && lastExploredChangeTime > 0) {
            long elapsed = System.currentTimeMillis() - lastExploredChangeTime;
            return elapsed >= EXPLORED_DEBOUNCE_MS;
        }
        return false;
    }

    public static Object get(Key key)
    {
        if (current == null)
            return null;
        else
            return current.conf.get(key);
    }

    public static void set(Key key, Object val)
    {
        if (current != null)
        {
            current.isUpd = true;
            current.conf.put(key, val);
        }
    }

    public static void needUpdate()
    {
        if (current != null)
        {
            current.isUpd = true;
        }
    }

    public static void needAreasUpdate()
    {
        // Only update profile-specific config (areas are per-world)
        // Record timestamp for debouncing - actual save happens after AREAS_DEBOUNCE_MS of inactivity
        long now = System.currentTimeMillis();
        try {
            if (nurgling.NUtils.getGameUI() != null && nurgling.NUtils.getUI() != null && nurgling.NUtils.getUI().core != null) {
                nurgling.NUtils.getUI().core.config.isAreasUpd = true;
                nurgling.NUtils.getUI().core.config.lastAreasChangeTime = now;
            }
        } catch (Exception e) {
            // Fallback to global config if profile config not available
            if (current != null)
            {
                current.isAreasUpd = true;
                current.lastAreasChangeTime = now;
            }
        }
    }



    public static void needExploredUpdate()
    {
        // Only update profile-specific config (explored area is per-world)
        // Record timestamp for debouncing - actual save happens after EXPLORED_DEBOUNCE_MS of inactivity
        long now = System.currentTimeMillis();
        try {
            if (nurgling.NUtils.getGameUI() != null && nurgling.NUtils.getUI() != null && nurgling.NUtils.getUI().core != null) {
                nurgling.NUtils.getUI().core.config.isExploredUpd = true;
                nurgling.NUtils.getUI().core.config.lastExploredChangeTime = now;
            }
        } catch (Exception e) {
            // Fallback to global config if profile config not available
            if (current != null)
            {
                current.isExploredUpd = true;
                current.lastExploredChangeTime = now;
            }
        }
    }

    // Area rank preset bindings - stored separately from areas (which sync from DB)
    @SuppressWarnings("unchecked")
    public static String getAreaRankPreset(int areaId, String animalType) {
        Map<Integer, Map<String, String>> presets = (Map<Integer, Map<String, String>>) get(Key.areaRankPresets);
        if (presets == null) return null;
        Map<String, String> areaPresets = presets.get(areaId);
        if (areaPresets == null) return null;
        return areaPresets.get(animalType);
    }

    @SuppressWarnings("unchecked")
    public static void setAreaRankPreset(int areaId, String animalType, String presetName) {
        Map<Integer, Map<String, String>> presets = (Map<Integer, Map<String, String>>) get(Key.areaRankPresets);
        if (presets == null) {
            presets = new HashMap<>();
        }
        Map<String, String> areaPresets = presets.computeIfAbsent(areaId, k -> new HashMap<>());
        if (presetName == null || presetName.isEmpty()) {
            areaPresets.remove(animalType);
            if (areaPresets.isEmpty()) {
                presets.remove(areaId);
            }
        } else {
            areaPresets.put(animalType, presetName);
        }
        set(Key.areaRankPresets, presets);
    }

    /**
     * Get all area IDs that have a rank preset configured for a specific animal type
     * @param animalType Animal type (cows, goats, sheeps, pigs, horses, deers)
     * @return Set of area IDs with configured presets
     */
    @SuppressWarnings("unchecked")
    public static Set<Integer> getAreasWithRankPreset(String animalType) {
        Set<Integer> result = new HashSet<>();
        Map<Integer, Map<String, String>> presets = (Map<Integer, Map<String, String>>) get(Key.areaRankPresets);
        if (presets == null) return result;
        for (Map.Entry<Integer, Map<String, String>> entry : presets.entrySet()) {
            if (entry.getValue() != null && entry.getValue().containsKey(animalType)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static NConfig current;

    // Profile management - World-specific configurations
    private static final Map<String, NConfig> profileInstances = new HashMap<>();
    private ProfileManager profileManager;
    private String genus;

    /**
     * Gets a profile-specific NConfig instance for the given genus
     */
    public static NConfig getProfileInstance(String genus) {
        if (genus == null || genus.isEmpty()) {
            return getGlobalInstance();
        }

        synchronized (profileInstances) {
            return profileInstances.computeIfAbsent(genus, g -> new NConfig(g));
        }
    }

    /**
     * Gets the global (non-profiled) NConfig instance
     */
    public static NConfig getGlobalInstance() {
        if (current == null) {
            current = new NConfig();
        }
        return current;
    }

    /**
     * Gets the current genus for this config instance
     */
    public String getGenus() {
        return genus;
    }

    /**
     * Helper method for profile-aware path resolution
     * Public to allow other components (like NCharacterInfo) to use profile paths
     */
    public String getProfileAwarePath(String filename) {
        if (profileManager != null) {
            return profileManager.getConfigPathString(filename);
        }
        return NUtils.getDataFile(filename);
    }

    /**
     * Gets the dynamic path for areas configuration file
     */
    public String getAreasPath() {
        return getProfileAwarePath("areas.nurgling.json");
    }

    /**
     * Gets the dynamic path for routes configuration file
     */
    public String getRoutesPath() {
        return getProfileAwarePath("routes.nurgling.json");
    }

    /**
     * Gets the dynamic path for explored configuration file
     */
    public String getExploredPath() {
        return getProfileAwarePath("explored.nurgling.json");
    }

    /**
     * Gets the dynamic path for session explored configuration file
     */
    public String getSessionExploredPath() {
        return getProfileAwarePath("session_explored.nurgling.json");
    }

    /**
     * Gets the dynamic path for cheese orders configuration file
     */
    public String getCheeseOrdersPath() {
        return getProfileAwarePath("cheese_orders.nurgling.json");
    }

    /**
     * Gets the dynamic path for fish locations configuration file
     */
    public String getFishLocationsPath() {
        return getProfileAwarePath("fish_locations.nurgling.json");
    }

    /**
     * Gets the dynamic path for tree locations configuration file
     */
    public String getTreeLocationsPath() {
        return getProfileAwarePath("tree_locations.nurgling.json");
    }
    
    /**
     * Gets the dynamic path for labeled marks configuration file
     * (water/soil quality marks from Checker bots)
     */
    public String getLabeledMarksPath() {
        return getProfileAwarePath("labeled_marks.nurgling.json");
    }

    /**
     * Gets the dynamic path for resource timers configuration file
     */
    public String getResourceTimersPath() {
        return getProfileAwarePath("resource_timers.nurgling.json");
    }

    /**
     * Gets the dynamic path for scenarios configuration file
     * Note: scenarios are always stored globally, not per-profile
     */
    public String getScenariosPath() {
        return NUtils.getDataFile("scenarios.nurgling.json");
    }

    /**
     * Gets the dynamic path for equipment presets configuration file
     * Note: equipment presets are always stored globally, not per-profile
     */
    public String getEquipmentPresetsPath() {
        return NUtils.getDataFile("equipment_presets.nurgling.json");
    }

    /**
     * Gets the dynamic path for craft presets configuration file
     * Note: craft presets are always stored globally, not per-profile
     */
    public String getCraftPresetsPath() {
        return NUtils.getDataFile("craft_presets.nurgling.json");
    }

    /**
     * Gets the dynamic path for custom icons configuration file
     * Note: custom icons are always stored globally, not per-profile
     */
    public String getCustomIconsPath() {
        return ((HashDirCache) ResCache.global).base + "\\..\\" + "custom_icons.nurgling.json";
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Object> readArray(ArrayList<HashMap<String, Object>> objs)
    {
        if (objs.size() > 0)
        {
            ArrayList<Object> res = new ArrayList<>();

            for (Object jobj : objs) {
                if (jobj instanceof HashMap) {
                    HashMap<String, Object> obj = (HashMap<String, Object>) jobj;
                    if(obj.get("type")!=null) {
                        switch ((String) obj.get("type")) {
                            case "NLoginData":
                                res.add(new NLoginData(obj));
                                break;
                            case "NDragProp":
                                res.add(new NDragProp(obj));
                                break;
                            case "NResizeProp":
                                res.add(new NResizeProp(obj));
                                break;
                            case "NKinProp":
                                res.add(new NKinProp(obj));
                                break;
                            case "NToolBeltProp":
                                res.add(new NToolBeltProp(obj));
                                break;
                            case "NDiscordNotification":
                                res.add(new NDiscordNotification(obj));
                                break;
                            case "CowsHerd":
                                res.add(new CowsHerd(obj));
                                break;
                            case "DeersHerd":
                                res.add(new TeimDeerHerd(obj));
                                break;
                            case "HorseHerd":
                                res.add(new HorseHerd(obj));
                                break;
                            case "GoatsHerd":
                                res.add(new GoatsHerd(obj));
                                break;
                            case "SheepsHerd":
                                res.add(new SheepsHerd(obj));
                                break;
                            case "PigsHerd":
                                res.add(new PigsHerd(obj));
                                break;
                            case "NChopperProp":
                                res.add(new NChopperProp(obj));
                                break;
                            case "NBoughBeeProp":
                                res.add(new NBoughBeeProp(obj));
                                break;
                            case "NForagerProp":
                                res.add(new NForagerProp(obj));
                                break;
                            case "NTrufflePigProp":
                                res.add(new NTrufflePigProp(obj));
                                break;
                            case "NBlueprintPlanterProp":
                                res.add(new NBlueprintPlanterProp(obj));
                                break;
                            case "NAutoFlowerActionProp":
                                res.add(new NAutoFlowerActionProp(obj));
                                break;
                            case "NChipperProp":
                                res.add(new NChipperProp(obj));
                                break;
                            case "NPrepBProp":
                                res.add(new NPrepBlocksProp(obj));
                                break;
                            case "NPrepBoardProp":
                                res.add(new NPrepBoardsProp(obj));
                                break;
                            case "NClayDiggerProp":
                                res.add(new NClayDiggerProp(obj));
                                break;
                            case "NAreaRad":
                                res.add(new NAreaRad(obj));
                                break;
                            case "NSmokeProp":
                                res.add(new NSmokProp(obj));
                                break;
                            case "NWorldExplorer":
                                res.add(new NWorldExplorerProp(obj));
                                break;
                            case "NFishingSettings":
                                res.add(new NFishingSettings(obj));
                                break;
                            case "NCarrierProp":
                                res.add(new NCarrierProp(obj));
                                break;
                            case "QuickActionPreset":
                                res.add(new QuickActionPreset(obj));
                                break;

                            default:
                                res.add(obj);
                        }
                    }
                    else
                    {
                        res.add(obj);
                    }
                }
                else if (jobj instanceof String) {
                    res.addAll(objs);
                    break;
                }
                else if (jobj instanceof Number) {
                    // Handle arrays of numbers (integers, longs, etc.)
                    res.addAll(objs);
                    break;
                }
            }
            return res;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public void read() {
        current = this;
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(path), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException ignore)
        {
        }

        if (!contentBuilder.toString().isEmpty())
        {
            JSONObject main = new JSONObject(contentBuilder.toString());
            Map<String, Object> map = main.toMap();
            for (Map.Entry<String, Object> entry : map.entrySet())
            {
                try {
                    if (entry.getValue() instanceof HashMap<?, ?>) {
                        HashMap<String, Object> hobj = ((HashMap<String, Object>) entry.getValue());
                        String type;
                        if ((type = (String) hobj.get("type")) != null) {
                            switch (type) {
                                case "NLoginData":
                                    conf.put(Key.valueOf(entry.getKey()), new NLoginData(hobj));
                                    break;
                                case "FontSettings":
                                    conf.put(Key.fonts, new FontSettings(hobj));
                                    break;
                                case "ItemQualityOverlaySettings":
                                    conf.put(Key.valueOf(entry.getKey()), new ItemQualityOverlaySettings(hobj));
                                    break;
                                case "Color":
                                    try {
                                        int red = ((Number) hobj.get("red")).intValue();
                                        int green = ((Number) hobj.get("green")).intValue();
                                        int blue = ((Number) hobj.get("blue")).intValue();
                                        int alpha = ((Number) hobj.get("alpha")).intValue();
                                        Color col = new Color(red, green, blue, alpha);
                                        conf.put(Key.valueOf(entry.getKey()), col);
                                    } catch (Exception e) {
                                        conf.put(Key.valueOf(entry.getKey()), entry.getValue());
                                    }
                                    break;
                                default:
                                    conf.put(Key.valueOf(entry.getKey()), entry.getValue());
                                    break;
                            }
                        } else if (entry.getKey().equals(Key.areaRankPresets.name())) {
                            // Special handling for areaRankPresets: convert String keys to Integer
                            Map<Integer, Map<String, String>> converted = new HashMap<>();
                            for (Map.Entry<String, Object> areaEntry : hobj.entrySet()) {
                                try {
                                    int areaId = Integer.parseInt(areaEntry.getKey());
                                    if (areaEntry.getValue() instanceof Map) {
                                        Map<String, String> animalPresets = new HashMap<>();
                                        Map<String, Object> rawPresets = (Map<String, Object>) areaEntry.getValue();
                                        for (Map.Entry<String, Object> presetEntry : rawPresets.entrySet()) {
                                            if (presetEntry.getValue() instanceof String) {
                                                animalPresets.put(presetEntry.getKey(), (String) presetEntry.getValue());
                                            }
                                        }
                                        converted.put(areaId, animalPresets);
                                    }
                                } catch (NumberFormatException ignore) {}
                            }
                            conf.put(Key.areaRankPresets, converted);
                        } else {
                            conf.put(Key.valueOf(entry.getKey()), entry.getValue());
                        }
                    } else if (Key.valueOf(entry.getKey()) != null && entry.getValue() instanceof ArrayList<?>) {
                        conf.put(Key.valueOf(entry.getKey()), readArray((ArrayList<HashMap<String, Object>>) entry.getValue()));
                    } else {
                        conf.put(Key.valueOf(entry.getKey()), entry.getValue());
                    }
                }catch (IllegalArgumentException ignore)
                {}
            }
        }

        // Migration: Ensure new config keys have default values if not present in loaded config
        if (!conf.containsKey(Key.showSpeedometer)) {
            conf.put(Key.showSpeedometer, true);
        }

        conf.put(Key.showCSprite,conf.get(Key.nextshowCSprite));
        conf.put(Key.flatsurface,conf.get(Key.nextflatsurface));
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Object> prepareArray(ArrayList<Object> objs)
    {
        if (objs.size() > 0)
        {
            ArrayList<Object> res = new ArrayList<>();
            if (objs.get(0) instanceof JConf)
            {
                for (Object obj : objs)
                {
                    res.add(((JConf) obj).toJson());
                }
            }
            else if (objs.get(0) instanceof ArrayList<?>)
            {
                for (Object obj : objs)
                {
                    res.add(prepareArray((ArrayList<Object>) obj));
                }
            }
            else
            {
                res.addAll(objs);
            }
            return res;
        }
        return objs;
    }

    @SuppressWarnings("unchecked")
    public void write()
    {
        Map<String, Object> prep = new HashMap<>();
        for (Map.Entry<Key, Object> entry : conf.entrySet())
        {
            if (entry.getValue() instanceof JConf)
            {
                prep.put(entry.getKey().toString(), ((JConf) entry.getValue()).toJson());
            }
            else if (entry.getValue() instanceof ArrayList<?>)
            {
                prep.put(entry.getKey().toString(), prepareArray((ArrayList<Object>) entry.getValue()));
            }
            else if (entry.getValue() instanceof Color)
            {
                // Convert Color objects back to Map format for JSON serialization
                Color color = (Color) entry.getValue();
                Map<String, Object> colorMap = new HashMap<>();
                colorMap.put("type", "Color");
                colorMap.put("red", color.getRed());
                colorMap.put("green", color.getGreen());
                colorMap.put("blue", color.getBlue());
                colorMap.put("alpha", color.getAlpha());
                prep.put(entry.getKey().toString(), colorMap);
            }
            else
            {
                prep.put(entry.getKey().toString(), entry.getValue());
            }
        }

        JSONObject main = new JSONObject(prep);
        try
        {
            FileWriter f = new FileWriter(path, StandardCharsets.UTF_8);
            main.write(f);
            f.close();
            current.isUpd = false;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }


    public void writeAreas(String customPath)
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            // If customPath is provided, write to file (for manual export only)
            if (customPath != null) {
                writeAreasToFile(customPath);
                return;
            }

            // If DB is enabled - ONLY use DB, never fallback to file
            if ((Boolean) NConfig.get(NConfig.Key.ndbenable)) {
                // Reset flags to prevent repeated calls (use 'this' not 'current' - they may be different instances)
                this.isAreasUpd = false;
                this.lastAreasChangeTime = 0;
                
                if (NCore.databaseManager != null && NCore.databaseManager.isReady()) {
                    try {
                        String profile = NUtils.getGameUI().getGenus();
                        if (profile == null || profile.isEmpty()) {
                            profile = "global";
                        }
                        java.util.Map<Integer, NArea> areas = ((NMapView)NUtils.getGameUI().map).glob.map.areas;
                        // Capture 'this' for use in async callback
                        final NConfig self = this;
                        NCore.databaseManager.getAreaService().exportAreasToDatabaseAsync(areas, profile)
                            .thenAccept(count -> {
                                // Silent save - no spam
                            })
                            .exceptionally(e -> {
                                System.err.println("Failed to save areas to database: " + e.getMessage());
                                if (e.getCause() != null) {
                                    e.getCause().printStackTrace();
                                }
                                // Set flag back to retry later (with timestamp for debounce)
                                self.isAreasUpd = true;
                                self.lastAreasChangeTime = System.currentTimeMillis();
                                return null;
                            });
                    } catch (Exception e) {
                        System.err.println("Failed to save areas to database: " + e.getMessage());
                        this.isAreasUpd = true;
                        this.lastAreasChangeTime = System.currentTimeMillis();
                    }
                }
                // DB enabled but not ready - just skip, will retry on next tick
                return;
            }

            // DB not enabled - write to file
            writeAreasToFile(getAreasPath());
        }
    }

    private void writeAreasToFile(String path) {
        JSONObject main = new JSONObject();
        JSONArray jareas = new JSONArray();
        for(NArea area : ((NMapView)NUtils.getGameUI().map).glob.map.areas.values())
        {
            jareas.put(area.toJson());
        }
        main.put("areas",jareas);
        try
        {
            FileWriter f = new FileWriter(path, StandardCharsets.UTF_8);
            main.write(f);
            f.close();
            this.isAreasUpd = false;
            this.lastAreasChangeTime = 0;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeExploredArea(String customPath)
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            try
            {
                String filePath = customPath == null ? getExploredPath() : customPath;
                // Merge with existing data on disk to prevent data loss when multiple clients run
                ((NCornerMiniMap)NUtils.getGameUI().mmap).exploredArea.mergeAndSaveToFile(filePath);
                this.isExploredUpd = false;
                this.lastExploredChangeTime = 0;
            }
            catch (Exception e)
            {
                // Log error but don't crash
                System.err.println("Error saving explored area: " + e.getMessage());
            }
        }
    }


    /**
     * Merge areas - duplicate strategy (rename conflicts with "Other_" prefix)
     */
    public void mergeAreas(File file) {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException ignore)
        {
        }

        if (!contentBuilder.toString().isEmpty()) {
            JSONObject main = new JSONObject(contentBuilder.toString());
            JSONArray array = (JSONArray) main.get("areas");
            for (int i = 0; i < array.length(); i++) {
                NArea a = new NArea((JSONObject) array.get(i));
                int id = 1;
                for (NArea area : ((NMapView) NUtils.getGameUI().map).glob.map.areas.values()) {
                    if (area.name.equals(a.name)) {
                        a.name = "Other_" + a.name;
                    }
                    if (area.id >= id) {
                        id = area.id + 1;
                    }
                }
                a.id = id;
                ((NMapView) NUtils.getGameUI().map).glob.map.areas.put(a.id, a);
            }
        }
    }
    
    /**
     * Replace areas - full replace strategy (delete all old, add new)
     */
    public void replaceAreas(File file) {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException ignore)
        {
        }

        if (!contentBuilder.toString().isEmpty()) {
            // Remove all existing areas and their overlays
            NMapView mapView = (NMapView) NUtils.getGameUI().map;
            synchronized (mapView.glob.map.areas) {
                // Remove overlays for all areas
                for (Integer areaId : new java.util.ArrayList<>(mapView.glob.map.areas.keySet())) {
                    if (mapView.nols.containsKey(areaId)) {
                        mapView.nols.get(areaId).remove();
                        mapView.nols.remove(areaId);
                    }
                    NArea area = mapView.glob.map.areas.get(areaId);
                    if (area != null) {
                        Gob dummy = mapView.dummys.get(area.gid);
                        if (dummy != null) {
                            mapView.glob.oc.remove(dummy);
                            mapView.dummys.remove(area.gid);
                        }
                    }
                }
                // Clear all areas
                mapView.glob.map.areas.clear();
            }
            
            // Add new areas from file
            JSONObject main = new JSONObject(contentBuilder.toString());
            JSONArray array = (JSONArray) main.get("areas");
            for (int i = 0; i < array.length(); i++) {
                NArea a = new NArea((JSONObject) array.get(i));
                a.id = i + 1;
                ((NMapView) NUtils.getGameUI().map).glob.map.areas.put(a.id, a);
            }
        }
    }
    
    /**
     * Overwrite areas - replace areas with same name, add new ones
     */
    public void overwriteAreas(File file) {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException ignore)
        {
        }

        if (!contentBuilder.toString().isEmpty()) {
            NMapView mapView = (NMapView) NUtils.getGameUI().map;
            JSONObject main = new JSONObject(contentBuilder.toString());
            JSONArray array = (JSONArray) main.get("areas");
            
            for (int i = 0; i < array.length(); i++) {
                NArea newArea = new NArea((JSONObject) array.get(i));
                
                // Find existing area with same name
                NArea existingArea = null;
                for (NArea area : mapView.glob.map.areas.values()) {
                    if (area.name.equals(newArea.name)) {
                        existingArea = area;
                        break;
                    }
                }
                
                if (existingArea != null) {
                    // Remove old area's overlays
                    if (mapView.nols.containsKey(existingArea.id)) {
                        mapView.nols.get(existingArea.id).remove();
                        mapView.nols.remove(existingArea.id);
                    }
                    Gob dummy = mapView.dummys.get(existingArea.gid);
                    if (dummy != null) {
                        mapView.glob.oc.remove(dummy);
                        mapView.dummys.remove(existingArea.gid);
                    }
                    
                    // Replace with new area using same id
                    newArea.id = existingArea.id;
                    mapView.glob.map.areas.put(newArea.id, newArea);
                } else {
                    // Add as new area with new id
                    int maxId = 0;
                    for (NArea area : mapView.glob.map.areas.values()) {
                        if (area.id > maxId) {
                            maxId = area.id;
                        }
                    }
                    newArea.id = maxId + 1;
                    mapView.glob.map.areas.put(newArea.id, newArea);
                }
            }
        }
    }

    public void writeScenarios(String customPath) {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            JSONObject main = new JSONObject();
            JSONArray jscenarios = new JSONArray();

            for (Scenario scenario : NUtils.getUI().core.scenarioManager.getScenarios().values()) {
                jscenarios.put(scenario.toJson());
            }
            main.put("scenarios", jscenarios);

            try {
                FileWriter f = new FileWriter(customPath == null ? getScenariosPath() : customPath, StandardCharsets.UTF_8);
                main.write(f);
                f.close();
                current.isScenariosUpd = false;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static Set<NPathVisualizer.PathCategory> getPathCategories() {
        HashSet<NPathVisualizer.PathCategory> res = new HashSet<>();
        res.add(NPathVisualizer.PathCategory.ME);
        res.add(NPathVisualizer.PathCategory.FOE);
        res.add(NPathVisualizer.PathCategory.OTHER);
        res.add(NPathVisualizer.PathCategory.FRIEND);
        res.add(NPathVisualizer.PathCategory.GPF);
        res.add(NPathVisualizer.PathCategory.QUEUED);
        res.add(NPathVisualizer.PathCategory.PF);
        return res;
    }

    public static void enableBotMod(String path) {
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");

            JSONObject jsonObject = new JSONObject(jsonString);
            botmod = new NCore.BotmodSettings((String) jsonObject.get("user"), (String) jsonObject.get("password"), (String) jsonObject.get("character"), jsonObject.getInt("scenarioId"));

            // Set stack trace file if provided (for autorunner debugging)
            if (jsonObject.has("stackTraceFile")) {
                botmod.stackTraceFile = jsonObject.getString("stackTraceFile");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static boolean isBotMod() {
        return botmod != null;
    }

    public static Color getColor(Key key, Color defaultColor) {
        Object colorObj = get(key);
        if (colorObj instanceof Color) {
            return (Color) colorObj;
        } else if (colorObj instanceof Map) {
            Map<String, Object> colorMap = (Map<String, Object>) colorObj;
            try {
                return new Color(
                        ((Number)colorMap.get("red")).intValue(),
                        ((Number)colorMap.get("green")).intValue(),
                        ((Number)colorMap.get("blue")).intValue(),
                        ((Number)colorMap.get("alpha")).intValue()
                );
        } catch (Exception e) {
                return defaultColor;
            }
        }
        return defaultColor;
    }

    public static void setColor(Key key, Color color) {
        Map<String, Object> colorMap = new HashMap<>();
        colorMap.put("type", "Color");
        colorMap.put("red", color.getRed());
        colorMap.put("green", color.getGreen());
        colorMap.put("blue", color.getBlue());
        colorMap.put("alpha", color.getAlpha());
        set(key, colorMap);
    }

}
