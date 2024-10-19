package nurgling;

import haven.*;
import nurgling.areas.*;
import nurgling.conf.*;
import org.json.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class NConfig
{

    public enum Key
    {
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
        goatsprop,
        chopperprop,
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
        animalrad
    }


    public NConfig() {
        conf = new HashMap<>();
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
    String path = ((HashDirCache) ResCache.global).base + "\\..\\" + "nconfig.nurgling.json";
    public String path_areas = ((HashDirCache) ResCache.global).base + "\\..\\" + "areas.nurgling.json";

    public boolean isUpdated()
    {
        return isUpd;
    }

    public boolean isAreasUpdated()
    {
        return isAreasUpd;
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
        if (current != null)
        {
            current.isAreasUpd = true;
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


    public void writeAreas()
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
                FileWriter f = new FileWriter(path_areas,StandardCharsets.UTF_8);
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
}
