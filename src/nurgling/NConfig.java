package nurgling;

import haven.*;
import nurgling.areas.*;
import nurgling.conf.*;
import nurgling.profiles.ProfileManager;
import nurgling.routes.Route;
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
        petals,
        singlePetal,
        asenable,
        autoMapper,
        endpoint,
        automaptrack,
        unloadgreen,
        showInventoryNums,
        hidecredo,
        autoDrink,
        autoSaveTableware,
        chipperprop,
        animalrad,
        smokeprop,
        worldexplorerprop,
        questNotified, lpassistent, fishingsettings,
        serverNode, serverUser, serverPass, ndbenable, harvestautorefill, cleanupQContainers, autoEquipTravellersSacks, qualityGrindSeedingPatter, postgres, sqlite, dbFilePath, simplecrops,
        temsmarktime, exploredAreaEnable, player_box, player_fov, temsmarkdist, tempmark, gridbox, useGlobalPf, useHFinGlobalPF, boxFillColor, boxEdgeColor, boxLineWidth, ropeAfterFeeding, ropeAfterTaiming, eatingConf, deersprop,dropConf, printpfmap, fonts,
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
        showFullPathLines,
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
        showDamageShields,
        disableTileSmoothing,
        disableTileTransitions,
        disableCloudShadows,
        disableDrugEffects,
        simpleInspect,
        showSpeedometer,
        showPathLine,
        parasiteBotEnabled,
        leechAction,
        tickAction,
        autoHearthOnUnknown,
        autoLogoutOnUnknown,
        alwaysObfuscate,
        boughbeeprop,
        foragerprop
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
        conf.put(Key.invert_hor, false);
        conf.put(Key.invert_ver, false);
        conf.put(Key.show_drag_menu, true);
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
        conf.put(Key.singlePetal, false);
        conf.put(Key.asenable, true);
        conf.put(Key.autoMapper, false);
        conf.put(Key.automaptrack, false);
        conf.put(Key.unloadgreen, false);
        conf.put(Key.showInventoryNums, true);
        conf.put(Key.autoDrink, false);
        conf.put(Key.autoSaveTableware, true);
        conf.put(Key.endpoint, "");
        conf.put(Key.questNotified, false);
        conf.put(Key.lpassistent, false);
        conf.put(Key.simplecrops, true);
        conf.put(Key.simpleInspect, false);
        conf.put(Key.ndbenable, false);
        conf.put(Key.harvestautorefill, false);
        conf.put(Key.cleanupQContainers, false);
        conf.put(Key.autoEquipTravellersSacks, false);
        conf.put(Key.qualityGrindSeedingPatter, "4x1");
        conf.put(Key.useGlobalPf, false);
        conf.put(Key.useHFinGlobalPF, false);
        conf.put(Key.sqlite, false);
        conf.put(Key.postgres, false);
        conf.put(Key.dbFilePath, "");
        conf.put(Key.serverNode, "");
        conf.put(Key.serverPass, "");
        conf.put(Key.serverUser, "");
        conf.put(Key.exploredAreaEnable, false);
        conf.put(Key.player_box, false);
        conf.put(Key.player_fov, false);
        conf.put(Key.gridbox, false);
        conf.put(Key.tempmark, false);
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
        conf.put(Key.showFullPathLines, false);
        conf.put(Key.showSpeedometer, false);
        conf.put(Key.showPathLine, false);

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
        arearadprop.add(new NAreaRad("gfx/kritter/troll/troll", 200));
        conf.put(Key.animalrad, arearadprop);

        // Movement speed setting (0=Crawl, 1=Walk, 2=Run, 3=Sprint)
        conf.put(Key.preferredMovementSpeed, 2);  // Default to Run (unchanged)
        conf.put(Key.preferredHorseSpeed, 2);     // Default to Run for horses (unchanged)

        // UI Opacity settings
        conf.put(Key.uiOpacity, 1.0f);  // Default to fully opaque
        conf.put(Key.useSolidBackground, false);  // Default to texture mode
        conf.put(Key.windowBackgroundColor, new java.awt.Color(32, 32, 32));  // Default dark gray

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

        // Damage shields display
        conf.put(Key.showDamageShields, true);

        // Terrain tile rendering settings
        conf.put(Key.disableTileSmoothing, false);
        conf.put(Key.disableTileTransitions, false);
        conf.put(Key.disableCloudShadows, false);
        conf.put(Key.disableDrugEffects, true);  // Default to disabled for better performance
        
        // Parasite bot settings
        conf.put(Key.parasiteBotEnabled, false);
        conf.put(Key.leechAction, "ground");  // "ground" or "inventory"
        conf.put(Key.tickAction, "ground");   // "ground" or "inventory"
        
        // Safety settings - auto hearth/logout on unknown players
        conf.put(Key.autoHearthOnUnknown, false);
        conf.put(Key.autoLogoutOnUnknown, false);
        
        // Auth obfuscation - bypass firewall blocks
        conf.put(Key.alwaysObfuscate, false);
    }


    HashMap<Key, Object> conf = new HashMap<>();
    private boolean isUpd = false;
    private boolean isAreasUpd = false;
    private boolean isExploredUpd = false;
    private boolean isRoutesUpd = false;
    private boolean isScenariosUpd = false;
    String path = ((HashDirCache) ResCache.global).base + "\\..\\" + "nconfig.nurgling.json";

    public boolean isUpdated()
    {
        return isUpd;
    }

    public boolean isAreasUpdated()
    {
        return isAreasUpd;
    }

    public boolean isRoutesUpdated() {
        return isRoutesUpd;
    }

    public boolean isScenariosUpdated() {
        return isScenariosUpd;
    }

    public boolean isExploredUpdated() { return isExploredUpd; }

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
        try {
            if (nurgling.NUtils.getGameUI() != null && nurgling.NUtils.getUI() != null && nurgling.NUtils.getUI().core != null) {
                nurgling.NUtils.getUI().core.config.isAreasUpd = true;
            }
        } catch (Exception e) {
            // Fallback to global config if profile config not available
            if (current != null)
            {
                current.isAreasUpd = true;
            }
        }
    }

    public static void needRoutesUpdate()
    {
        // Only update profile-specific config (routes are per-world)
        try {
            if (nurgling.NUtils.getGameUI() != null && nurgling.NUtils.getUI() != null && nurgling.NUtils.getUI().core != null) {
                nurgling.NUtils.getUI().core.config.isRoutesUpd = true;
            }
        } catch (Exception e) {
            // Fallback to global config if profile config not available
            if (current != null)
            {
                current.isRoutesUpd = true;
            }
        }
    }

    public static void needExploredUpdate()
    {
        // Only update profile-specific config (explored area is per-world)
        try {
            if (nurgling.NUtils.getGameUI() != null && nurgling.NUtils.getUI() != null && nurgling.NUtils.getUI().core != null) {
                nurgling.NUtils.getUI().core.config.isExploredUpd = true;
            }
        } catch (Exception e) {
            // Fallback to global config if profile config not available
            if (current != null)
            {
                current.isExploredUpd = true;
            }
        }
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
        return ((HashDirCache) ResCache.global).base + "\\..\\" + filename;
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
        return ((HashDirCache) ResCache.global).base + "\\..\\" + "scenarios.nurgling.json";
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
            JSONObject main = new JSONObject();
            JSONArray jareas = new JSONArray();
            for(NArea area : ((NMapView)NUtils.getGameUI().map).glob.map.areas.values())
            {
                jareas.put(area.toJson());
            }
            main.put("areas",jareas);
            try
            {
                FileWriter f = new FileWriter(customPath==null?getAreasPath():customPath,StandardCharsets.UTF_8);
                main.write(f);
                f.close();
                current.isAreasUpd = false;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void writeExploredArea(String customPath)
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            try
            {
                FileWriter f = new FileWriter(customPath==null?getExploredPath():customPath,StandardCharsets.UTF_8);
                ((NCornerMiniMap)NUtils.getGameUI().mmap).exploredArea.toJson().write(f);
                f.close();
                current.isExploredUpd = false;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }


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

    public void writeRoutes(String customPath)
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            JSONObject main = new JSONObject();
            JSONArray jroutes = new JSONArray();
            for(Route route : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().values())
            {
                jroutes.put(route.toJson());
            }
            main.put("routes",jroutes);

            try
            {
                FileWriter f = new FileWriter(customPath==null?getRoutesPath():customPath,StandardCharsets.UTF_8);
                main.write(f);
                f.close();
                this.isRoutesUpd = false;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void mergeRoutes(File file) {
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
            JSONArray array = (JSONArray) main.get("routes");
            for (int i = 0; i < array.length(); i++) {
                Route a = new Route((JSONObject) array.get(i));
                int id = 1;
                for (Route route : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().values()) {
                    if (route.name.equals(a.name)) {
                        a.name = "Other_" + a.name;
                    }
                    if (route.id >= id) {
                        id = route.id + 1;
                    }
                }
                a.id = id;
                ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().put(a.id, a);
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
