package nurgling;

import haven.*;
import nurgling.areas.*;
import nurgling.conf.*;
import nurgling.routes.Route;
import nurgling.scenarios.Scenario;
import nurgling.widgets.NMiniMap;
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
        chipperprop,
        animalrad,
        smokeprop,
        worldexplorerprop,
        questNotified, lpassistent, fishingsettings,
        serverNode, serverUser, serverPass, ndbenable, harvestautorefill, postgres, sqlite, dbFilePath, simplecrops,
        temsmarktime, fogEnable, player_box, player_fov, temsmarkdist, tempmark, gridbox, useGlobalPf, useHFinGlobalPF, boxFillColor, boxEdgeColor, fonts
    }


    public NConfig() {
        conf = new HashMap<>();

        conf.put(Key.vilol, false);
        conf.put(Key.claimol, false);
        conf.put(Key.realmol, false);
        conf.put(Key.showVarity, false);
        conf.put(Key.autoFlower, false);
        conf.put(Key.autoSplitter, false);
        conf.put(Key.autoDropper, false);
        conf.put(Key.is_real_time, true);
        conf.put(Key.numbelts, 3);
        conf.put(Key.showCropStage, false);
        conf.put(Key.nightVision, false);
        conf.put(Key.showBB, false);
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
        conf.put(Key.endpoint, "");
        conf.put(Key.questNotified, false);
        conf.put(Key.lpassistent, false);
        conf.put(Key.simplecrops, true);
        conf.put(Key.ndbenable, false);
        conf.put(Key.harvestautorefill, false);
        conf.put(Key.useGlobalPf, false);
        conf.put(Key.useHFinGlobalPF, false);
        conf.put(Key.sqlite, false);
        conf.put(Key.postgres, false);
        conf.put(Key.dbFilePath, "");
        conf.put(Key.serverNode, "");
        conf.put(Key.serverPass, "");
        conf.put(Key.serverUser, "");
        conf.put(Key.fogEnable, false);
        conf.put(Key.player_box, false);
        conf.put(Key.player_fov, false);
        conf.put(Key.gridbox, false);
        conf.put(Key.tempmark, false);
        conf.put(Key.temsmarkdist, 4);
        conf.put(Key.temsmarktime, 3);
        conf.put(Key.fonts, new FontSettings());


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
    }


    HashMap<Key, Object> conf = new HashMap<>();
    private boolean isUpd = false;
    private boolean isAreasUpd = false;
    private boolean isFogUpd = false;
    private boolean isRoutesUpd = false;
    private boolean isScenariosUpd = false;
    String path = ((HashDirCache) ResCache.global).base + "\\..\\" + "nconfig.nurgling.json";
    public String path_areas = ((HashDirCache) ResCache.global).base + "\\..\\" + "areas.nurgling.json";
    public String path_fog = ((HashDirCache) ResCache.global).base + "\\..\\" + "fog.nurgling.json";
    public String path_routes = ((HashDirCache) ResCache.global).base + "\\..\\" + "routes.nurgling.json";
    public String path_scenarios = ((HashDirCache) ResCache.global).base + "\\..\\" + "scenarios.nurgling.json";

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

    public boolean isFogUpdated() { return isFogUpd; }

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
        if (current != null)
        {
            current.isAreasUpd = true;
        }
    }

    public static void needRoutesUpdate()
    {
        if (current != null)
        {
            current.isRoutesUpd = true;
        }
    }

    public static void needScenariosUpdate() {
        if (current != null)
        {
            current.isScenariosUpd = true;
        }
    }

    public static void needFogUpdate()
    {
        if (current != null)
        {
            current.isFogUpd = true;
        }
    }

    public static NConfig current;

    private ArrayList<Object> readArray(ArrayList<HashMap<String, Object>> objs)
    {
        if (objs.size() > 0)
        {
            ArrayList<Object> res = new ArrayList<>();

            for (Object jobj : objs) {
                if (jobj instanceof HashMap) {
                    HashMap<String, Object> obj = (HashMap<String, Object>) jobj;
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
                else if (jobj instanceof String) {
                    res.addAll(objs);
                    break;
                }
            }
            return res;
        }
        return new ArrayList<>();
    }

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
                            }
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
        conf.put(Key.showCSprite,conf.get(Key.nextshowCSprite));
        conf.put(Key.flatsurface,conf.get(Key.nextflatsurface));
    }

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
                FileWriter f = new FileWriter(customPath==null?path_areas:customPath,StandardCharsets.UTF_8);
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

    public void writeFogOfWar(String customPath)
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            try
            {
                FileWriter f = new FileWriter(customPath==null?path_fog:customPath,StandardCharsets.UTF_8);
                ((NMiniMap)NUtils.getGameUI().mmap).fogArea.toJson().write(f);
                f.close();
                current.isFogUpd = false;
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
                FileWriter f = new FileWriter(customPath==null?path_routes:customPath,StandardCharsets.UTF_8);
                main.write(f);
                f.close();
                current.isRoutesUpd = false;
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
                FileWriter f = new FileWriter(customPath == null ? path_scenarios : customPath, StandardCharsets.UTF_8);
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
        return res;
    }

    public static void enableBotMod(String path) {
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");

            JSONObject jsonObject = new JSONObject(jsonString);
            botmod = new NCore.BotmodSettings((String) jsonObject.get("user"), (String) jsonObject.get("password"), (String) jsonObject.get("character"), jsonObject.getInt("scenarioId"));
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
            return new Color(
                    ((Number)colorMap.get("red")).intValue(),
                    ((Number)colorMap.get("green")).intValue(),
                    ((Number)colorMap.get("blue")).intValue(),
                    ((Number)colorMap.get("alpha")).intValue()
            );
        }
        return defaultColor;
    }

    public static void setColor(Key key, Color color) {
        Map<String, Object> colorMap = new HashMap<>();
        colorMap.put("red", color.getRed());
        colorMap.put("green", color.getGreen());
        colorMap.put("blue", color.getBlue());
        colorMap.put("alpha", color.getAlpha());
        set(key, colorMap);
    }

}
