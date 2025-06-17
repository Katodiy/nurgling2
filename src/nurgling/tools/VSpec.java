package nurgling.tools;

import haven.Gob;
import nurgling.*;
import org.json.*;

import java.util.*;

public class VSpec {
    public static HashMap<String, ArrayList<JSONObject>> categories = new HashMap<>();
    public static HashMap<String,ArrayList<String>> object = new HashMap<>();
    static {
        // Acacia Tree
        ArrayList<String> acaciaOp = new ArrayList<>();
        acaciaOp.add("Acacia Pod"); // Семена
        object.put("gfx/terobjs/trees/acacia", acaciaOp);

        // Alder Tree
        ArrayList<String> alderOp = new ArrayList<>();
        alderOp.add("Alder Bough"); // Ветки
        alderOp.add("Alder Catkin"); // Семена
        object.put("gfx/terobjs/trees/alder", alderOp);

        // Almond Tree
        ArrayList<String> almondtreeOp = new ArrayList<>();
        almondtreeOp.add("Almonds"); // Семена
        object.put("gfx/terobjs/trees/almondtree", almondtreeOp);

        // Apple Tree
        ArrayList<String> appletreeOp = new ArrayList<>();
        appletreeOp.add("Red Apple"); // Семена
        object.put("gfx/terobjs/trees/appletree", appletreeOp);

        // Ash Tree
        ArrayList<String> ashOp = new ArrayList<>();
        ashOp.add("Ash Samaras"); // Семена
        object.put("gfx/terobjs/trees/ash", ashOp);

        // Aspen Tree
        ArrayList<String> aspenOp = new ArrayList<>();
        aspenOp.add("Aspen Catkin"); // Семена
        object.put("gfx/terobjs/trees/aspen", aspenOp);

        // Bay Willow Tree
        ArrayList<String> baywillowOp = new ArrayList<>();
        baywillowOp.add("Bay Willow Catkins"); // Семена
        object.put("gfx/terobjs/trees/baywillow", baywillowOp);

        // Beech Tree
        ArrayList<String> beechOp = new ArrayList<>();
        beechOp.add("Beech Nuts"); // Семена
        object.put("gfx/terobjs/trees/beech", beechOp);

        // Birch Tree
        ArrayList<String> birchOp = new ArrayList<>();
        birchOp.add("Birch Catkin"); // Семена
        object.put("gfx/terobjs/trees/birch", birchOp);

        // Bird Cherry Tree
        ArrayList<String> birdcherryOp = new ArrayList<>();
        birdcherryOp.add("Bird Cherries"); // Семена
        object.put("gfx/terobjs/trees/birdcherrytree", birdcherryOp);

        // Black Pine Tree
        ArrayList<String> blackpineOp = new ArrayList<>();
        blackpineOp.add("Black Pine Cone"); // Семена
        object.put("gfx/terobjs/trees/blackpine", blackpineOp);

        // Black Poplar Tree
        ArrayList<String> blackpoplarOp = new ArrayList<>();
        blackpoplarOp.add("Blackpoplar Catkin"); // Семена
        object.put("gfx/terobjs/trees/blackpoplar", blackpoplarOp);

        // Buckthorn Tree
        ArrayList<String> buckthornOp = new ArrayList<>();
        buckthornOp.add("Buckthorn Drupes"); // Семена
        object.put("gfx/terobjs/trees/buckthorn", buckthornOp);

        // Carob Tree
        ArrayList<String> carobOp = new ArrayList<>();
        carobOp.add("Carob Pod"); // Семена
        object.put("gfx/terobjs/trees/carobtree", carobOp);

        // Cedar Tree
        ArrayList<String> cedarOp = new ArrayList<>();
        cedarOp.add("Cedar Cone"); // Семена
        object.put("gfx/terobjs/trees/cedar", cedarOp);

        // Chaste Tree
        ArrayList<String> chasteOp = new ArrayList<>();
        chasteOp.add("Chastetree Seeds"); // Семена
        object.put("gfx/terobjs/trees/chastetree", chasteOp);

        // Checker Tree
        ArrayList<String> checkerOp = new ArrayList<>();
        checkerOp.add("Checker Tree Fruits"); // Семена
        object.put("gfx/terobjs/trees/checkertree", checkerOp);

        // Cherry Tree
        ArrayList<String> cherryOp = new ArrayList<>();
        cherryOp.add("Cherries"); // Плоды
        object.put("gfx/terobjs/trees/cherry", cherryOp);

        // Chestnut Tree
        ArrayList<String> chestnutOp = new ArrayList<>();
        chestnutOp.add("Chestnut"); // Семена
        object.put("gfx/terobjs/trees/chestnuttree", chestnutOp);

        // Conker Tree
        ArrayList<String> conkerOp = new ArrayList<>();
        conkerOp.add("Conker Leaf"); // Листья
        conkerOp.add("Conker"); // Семена
        object.put("gfx/terobjs/trees/conkertree", conkerOp);

        // Cork Oak Tree
        ArrayList<String> corkoakOp = new ArrayList<>();
        corkoakOp.add("Cork"); // Семена
        object.put("gfx/terobjs/trees/corkoak", corkoakOp);

        // Cypress Tree
        ArrayList<String> cypressOp = new ArrayList<>();
        cypressOp.add("Cypress Cone"); // Семена
        object.put("gfx/terobjs/trees/cypress", cypressOp);

        // Dogwood Tree
        ArrayList<String> dogwoodOp = new ArrayList<>();
        dogwoodOp.add("Dogwood Seeds"); // Семена
        object.put("gfx/terobjs/trees/dogwood", dogwoodOp);

        // Elm Tree
        ArrayList<String> elmOp = new ArrayList<>();
        elmOp.add("Elm Bough"); // Ветки
        elmOp.add("Elm Seeds"); // Семена
        object.put("gfx/terobjs/trees/elm", elmOp);

        // Fir Tree
        ArrayList<String> firOp = new ArrayList<>();
        firOp.add("Fir Bough"); // Ветки
        firOp.add("Fir Cone"); // Семена
        object.put("gfx/terobjs/trees/fir", firOp);

        // Fig Tree
        ArrayList<String> figOp = new ArrayList<>();
        figOp.add("Fig Leaf"); // Листья
        figOp.add("Fig"); // Семена
        object.put("gfx/terobjs/trees/figtree", figOp);

        // Gray Alder Tree
        ArrayList<String> grayalderOp = new ArrayList<>();
        grayalderOp.add("Gray Alder Bough"); // Ветки
        grayalderOp.add("Gray Alder Cones"); // Семена
        object.put("gfx/terobjs/trees/grayalder", grayalderOp);

        // Hazel Tree
        ArrayList<String> hazelOp = new ArrayList<>();
        hazelOp.add("Hazelnut"); // Семена
        object.put("gfx/terobjs/trees/hazel", hazelOp);

        // Hornbeam Tree
        ArrayList<String> hornbeamOp = new ArrayList<>();
        hornbeamOp.add("Hornbeam Catkins"); // Семена
        object.put("gfx/terobjs/trees/hornbeam", hornbeamOp);

        // Juniper Tree
        ArrayList<String> juniperOp = new ArrayList<>();
        juniperOp.add("Juniper Berries"); // Семена
        object.put("gfx/terobjs/trees/juniper", juniperOp);

        // King's Oak Tree
        ArrayList<String> kingsoakOp = new ArrayList<>();
        kingsoakOp.add("King's Acorn"); // Семена
        object.put("gfx/terobjs/trees/kingsoak", kingsoakOp);

        // Larch Tree
        ArrayList<String> larchOp = new ArrayList<>();
        larchOp.add("Larch Cones"); // Семена
        object.put("gfx/terobjs/trees/larch", larchOp);

        // Laurel Tree
        ArrayList<String> laurelOp = new ArrayList<>();
        laurelOp.add("Laurel Leaves"); // Листья
        laurelOp.add("Laurel Seeds"); // Семена
        object.put("gfx/terobjs/trees/laurel", laurelOp);

        // Lemon Tree
        ArrayList<String> lemonOp = new ArrayList<>();
        lemonOp.add("Lemon"); // Плоды
        object.put("gfx/terobjs/trees/lemontree", lemonOp);

        // Linden Tree
        ArrayList<String> lindenOp = new ArrayList<>();
        lindenOp.add("Linden Bough"); // Ветки
        lindenOp.add("Linden Fruits"); // Семена
        object.put("gfx/terobjs/trees/linden", lindenOp);

        // Lote Tree
        ArrayList<String> loteOp = new ArrayList<>();
        loteOp.add("Lote Tree Drupes"); // Семена
        object.put("gfx/terobjs/trees/lotetree", loteOp);

        // Maple Tree
        ArrayList<String> mapleOp = new ArrayList<>();
        mapleOp.add("Maple Samara"); // Семена
        mapleOp.add("Maple Leaf"); // Листья
        object.put("gfx/terobjs/trees/maple", mapleOp);

        // Mayflower Tree
        ArrayList<String> mayflowerOp = new ArrayList<>();
        mayflowerOp.add("Mayflower Pomes"); // Семена
        object.put("gfx/terobjs/trees/mayflower", mayflowerOp);

        // Medlar Tree
        ArrayList<String> medlarOp = new ArrayList<>();
        medlarOp.add("Medlar Seed"); // Семена
        object.put("gfx/terobjs/trees/medlartree", medlarOp);

        // Mound Tree
        ArrayList<String> moundOp = new ArrayList<>();
        moundOp.add("Mound Beans"); // Семена
        object.put("gfx/terobjs/trees/moundtree", moundOp);

        // Mulberry Tree
        ArrayList<String> mulberryOp = new ArrayList<>();
        mulberryOp.add("Mulberry"); // Семена
        mulberryOp.add("Mulberry Leaf"); // Листья
        object.put("gfx/terobjs/trees/mulberry", mulberryOp);

        // Oak Tree
        ArrayList<String> oakOp = new ArrayList<>();
        oakOp.add("Oak Acorn"); // Семена
        object.put("gfx/terobjs/trees/oak", oakOp);

        // Olive Tree
        ArrayList<String> oliveOp = new ArrayList<>();
        oliveOp.add("Olive Branch"); // Ветки
        oliveOp.add("Olive"); // Семена
        object.put("gfx/terobjs/trees/olivetree", oliveOp);

        // Orange Tree
        ArrayList<String> orangeOp = new ArrayList<>();
        orangeOp.add("Orange"); // Плоды
        object.put("gfx/terobjs/trees/orangetree", orangeOp);

        // Osier Tree
        ArrayList<String> osierOp = new ArrayList<>();
        osierOp.add("Osier Catkin"); // Семена
        object.put("gfx/terobjs/trees/osier", osierOp);

        // Pear Tree
        ArrayList<String> pearOp = new ArrayList<>();
        pearOp.add("Pear"); // Плоды
        object.put("gfx/terobjs/trees/peartree", pearOp);

        // Persimmon Tree
        ArrayList<String> persimmonOp = new ArrayList<>();
        persimmonOp.add("Persimmon"); // Плоды
        object.put("gfx/terobjs/trees/persimmontree", persimmonOp);

        // Pine Tree
        ArrayList<String> pineOp = new ArrayList<>();
        pineOp.add("Pine Cone"); // Семена
        object.put("gfx/terobjs/trees/pine", pineOp);

        // Plane Tree
        ArrayList<String> planeOp = new ArrayList<>();
        planeOp.add("Plane Seedpods"); // Семена
        object.put("gfx/terobjs/trees/planetree", planeOp);

        // Plum Tree
        ArrayList<String> plumOp = new ArrayList<>();
        plumOp.add("Plum"); // Плоды
        object.put("gfx/terobjs/trees/plumtree", plumOp);

        // Poplar Tree
        ArrayList<String> poplarOp = new ArrayList<>();
        poplarOp.add("Poplar Catkin"); // Семена
        object.put("gfx/terobjs/trees/poplar", poplarOp);

        // Quince Tree
        ArrayList<String> quinceOp = new ArrayList<>();
        quinceOp.add("Quince"); // Плоды
        object.put("gfx/terobjs/trees/quincetree", quinceOp);

        // Rowan Tree
        ArrayList<String> rowanOp = new ArrayList<>();
        rowanOp.add("Rowan Berries"); // Семена
        object.put("gfx/terobjs/trees/rowan", rowanOp);

        // Sallow Tree
        ArrayList<String> sallowOp = new ArrayList<>();
        sallowOp.add("Sallow Catkin"); // Семена
        object.put("gfx/terobjs/trees/sallow", sallowOp);

        // Silver Fir Tree
        ArrayList<String> silverfirOp = new ArrayList<>();
        silverfirOp.add("Silverfir Cone"); // Семена
        object.put("gfx/terobjs/trees/silverfir", silverfirOp);

        // Sorb Tree
        ArrayList<String> sorbOp = new ArrayList<>();
        sorbOp.add("Sorb Apple"); // Семена
        object.put("gfx/terobjs/trees/sorbtree", sorbOp);

        // Spruce Tree
        ArrayList<String> spruceOp = new ArrayList<>();
        spruceOp.add("Spruce Bough"); // Ветки
        spruceOp.add("Spruce Cone"); // Семена
        object.put("gfx/terobjs/trees/spruce", spruceOp);

        // Stone Pine Tree
        ArrayList<String> stonepineOp = new ArrayList<>();
        stonepineOp.add("Stone Pine Cone"); // Семена
        object.put("gfx/terobjs/trees/stonepine", stonepineOp);

        // Sweetgum Tree
        ArrayList<String> sweetgumOp = new ArrayList<>();
        sweetgumOp.add("Sweetgum Bough"); // Ветки
        sweetgumOp.add("Sweetgum Seedpod"); // Семена
        object.put("gfx/terobjs/trees/sweetgum", sweetgumOp);

        // Sycamore Tree
        ArrayList<String> sycamoreOp = new ArrayList<>();
        sycamoreOp.add("Sycamore Seed"); // Семена
        object.put("gfx/terobjs/trees/sycamore", sycamoreOp);

        // Tamarisk Tree
        ArrayList<String> tamariskOp = new ArrayList<>();
        tamariskOp.add("Tamarisk Seeds"); // Семена
        object.put("gfx/terobjs/trees/tamarisk", tamariskOp);

        // Wood Strawberry Tree
        ArrayList<String> woodstrawberryOp = new ArrayList<>();
        woodstrawberryOp.add("Wood Strawberry"); // Плоды
        object.put("gfx/terobjs/trees/strawberrytree", woodstrawberryOp);

        // Terebinth Tree
        ArrayList<String> terebinthOp = new ArrayList<>();
        terebinthOp.add("Terebinth Seed"); // Семена
        object.put("gfx/terobjs/trees/terebinth", terebinthOp);

        // Tree Heath Tree
        ArrayList<String> treeheathOp = new ArrayList<>();
        treeheathOp.add("Tree Heath Seed"); // Семена
        object.put("gfx/terobjs/trees/treeheath", treeheathOp);

        // Walnut Tree
        ArrayList<String> walnutOp = new ArrayList<>();
        walnutOp.add("Walnut"); // Семена
        object.put("gfx/terobjs/trees/walnuttree", walnutOp);

        // Warty Birch Tree
        ArrayList<String> wartybirchOp = new ArrayList<>();
        wartybirchOp.add("Warty Birch Catkin"); // Семена
        object.put("gfx/terobjs/trees/wartybirch", wartybirchOp);

        // Whitebeam Tree
        ArrayList<String> whitebeamOp = new ArrayList<>();
        whitebeamOp.add("Whitebeam Fruits"); // Семена
        object.put("gfx/terobjs/trees/whitebeam", whitebeamOp);

        // Willow Tree
        ArrayList<String> willowOp = new ArrayList<>();
        willowOp.add("Willow Catkin"); // Семена
        object.put("gfx/terobjs/trees/willow", willowOp);

        // Wych Elm Tree
        ArrayList<String> wychelmOp = new ArrayList<>();
        wychelmOp.add("Wych Elm Samara"); // Семена
        object.put("gfx/terobjs/trees/wychelm", wychelmOp);

        // Yew Tree
        ArrayList<String> yewOp = new ArrayList<>();
        yewOp.add("Yew Bough"); // Ветки
        yewOp.add("Yew Cones"); // Семена
        object.put("gfx/terobjs/trees/yew", yewOp);

        // Zelkova Tree
        ArrayList<String> zelkovaOp = new ArrayList<>();
        zelkovaOp.add("Zelkova Catkin"); // Семена
        object.put("gfx/terobjs/trees/zelkova", zelkovaOp);

        // Gloomcap Tree
        ArrayList<String> gloomcapOp = new ArrayList<>();
        gloomcapOp.add("Gloomcap Spores"); // Семена
        object.put("gfx/terobjs/trees/gloomcap", gloomcapOp);

        // Gnome's Cap Tree
        ArrayList<String> gnomescapOp = new ArrayList<>();
        gnomescapOp.add("Gnome's Cap Spore"); // Семена
        object.put("gfx/terobjs/trees/gnomeshat", gnomescapOp);

        // Goldenchain Tree
        ArrayList<String> goldenchainOp = new ArrayList<>();
        goldenchainOp.add("Goldenchain Seeds"); // Семена
        object.put("gfx/terobjs/trees/goldenchain", goldenchainOp);

        // Towercap Tree
        ArrayList<String> towercapOp = new ArrayList<>();
        towercapOp.add("Towercap Spore"); // Семена
        object.put("gfx/terobjs/trees/towercap", towercapOp);

        // Trumpet Chantrelle Tree
        ArrayList<String> trumpetchantrelleOp = new ArrayList<>();
        trumpetchantrelleOp.add("Trombone Chantrelle Spore"); // Семена
        object.put("gfx/terobjs/trees/trombonechantrelle", trumpetchantrelleOp);

        // Crabapple Tree
        ArrayList<String> crabappleOp = new ArrayList<>();
        crabappleOp.add("Crabapples"); // Плоды
        object.put("gfx/terobjs/trees/crabappletree", crabappleOp);

        // Dwarf Pine Tree
        ArrayList<String> dwarfpineOp = new ArrayList<>();
        dwarfpineOp.add("Dwarf Pine Cone"); // Семена
        object.put("gfx/terobjs/trees/dwarfpine", dwarfpineOp);


        // Arrowwood Bush
        ArrayList<String> arrowwoodOp = new ArrayList<>();
        arrowwoodOp.add("Arrowwood Berries"); // Плоды
        object.put("gfx/terobjs/bushes/arrowwood", arrowwoodOp);

        // Bittersweet Nightshade Bush
        ArrayList<String> bittersweetnightshadeOp = new ArrayList<>();
        bittersweetnightshadeOp.add("Bittersweet Nightshade Berries"); // Плоды
        object.put("gfx/terobjs/bushes/bsnightshade", bittersweetnightshadeOp);

        // Blackberry Bush
        ArrayList<String> blackberryOp = new ArrayList<>();
        blackberryOp.add("Blackberry"); // Плоды
        object.put("gfx/terobjs/bushes/blackberrybush", blackberryOp);

        // Blackcurrant Bush
        ArrayList<String> blackcurrantOp = new ArrayList<>();
        blackcurrantOp.add("Blackcurrant"); // Плоды
        object.put("gfx/terobjs/bushes/blackcurrant", blackcurrantOp);

        // Blackthorn Bush
        ArrayList<String> blackthornOp = new ArrayList<>();
        blackthornOp.add("Sloan Berries"); // Плоды
        object.put("gfx/terobjs/bushes/blackthorn", blackthornOp);

        // Bog-Myrtle Bush
        ArrayList<String> bogmyrtleOp = new ArrayList<>();
        bogmyrtleOp.add("Bog-Myrtle Cones"); // Семена
        object.put("gfx/terobjs/bushes/bogmyrtle", bogmyrtleOp);

        // Caprifole Bush
        ArrayList<String> caprifoleOp = new ArrayList<>();
        caprifoleOp.add("Caprifole Berries"); // Плоды
        object.put("gfx/terobjs/bushes/caprifole", caprifoleOp);

        // Cave Fern
        ArrayList<String> cavefernOp = new ArrayList<>();
        cavefernOp.add("Cave Fern Spores"); // Семена
        object.put("gfx/terobjs/bushes/cavefern", cavefernOp);

        // Crampbark Bush
        ArrayList<String> crampbarkOp = new ArrayList<>();
        crampbarkOp.add("Crampbark Berries"); // Плоды
        object.put("gfx/terobjs/bushes/crampbark", crampbarkOp);

        // Dog Rose Bush
        ArrayList<String> dogroseOp = new ArrayList<>();
        dogroseOp.add("Dog Rose Hips"); // Плоды
        object.put("gfx/terobjs/bushes/dogrose", dogroseOp);

        // Elderberry Bush
        ArrayList<String> elderberryOp = new ArrayList<>();
        elderberryOp.add("Elderberries"); // Плоды
        object.put("gfx/terobjs/bushes/elderberrybush", elderberryOp);

        // Fly Woodbine Bush
        ArrayList<String> flywoodbineOp = new ArrayList<>();
        flywoodbineOp.add("Fly Woodbine Berries"); // Плоды
        object.put("gfx/terobjs/bushes/woodbine", flywoodbineOp);

        // Ghostpipe
        ArrayList<String> ghostpipeOp = new ArrayList<>();
        ghostpipeOp.add("Ghostpipe"); // Семена
        object.put("gfx/terobjs/bushes/ghostpipe", ghostpipeOp);

        // Gooseberry Bush
        ArrayList<String> gooseberryOp = new ArrayList<>();
        gooseberryOp.add("Gooseberry"); // Плоды
        object.put("gfx/terobjs/bushes/gooseberrybush", gooseberryOp);

        // Gorse Bush
        ArrayList<String> gorseOp = new ArrayList<>();
        gorseOp.add("Gorse"); // Плоды
        object.put("gfx/terobjs/bushes/gorse", gorseOp);

        // Hawthorn Bush
        ArrayList<String> hawthornOp = new ArrayList<>();
        hawthornOp.add("Hawthorn Fruits"); // Плоды
        object.put("gfx/terobjs/bushes/hawthorn", hawthornOp);

        // Hoarwithy Bush
        ArrayList<String> hoarwithyOp = new ArrayList<>();
        hoarwithyOp.add("Hoarwithy Berries"); // Плоды
        object.put("gfx/terobjs/bushes/hoarwithy", hoarwithyOp);

        // Holly Bush
        ArrayList<String> hollyOp = new ArrayList<>();
        hollyOp.add("Hollyberries"); // Плоды
        object.put("gfx/terobjs/bushes/holly", hollyOp);

        // Mastic Bush
        ArrayList<String> masticOp = new ArrayList<>();
        masticOp.add("Mastic Fruit"); // Плоды
        object.put("gfx/terobjs/bushes/mastic", masticOp);

        // Poppycaps
        ArrayList<String> poppycapsOp = new ArrayList<>();
        poppycapsOp.add("Poppycaps"); // Плоды
        object.put("gfx/terobjs/bushes/poppycaps", poppycapsOp);

        // Raspberry Bush
        ArrayList<String> raspberryOp = new ArrayList<>();
        raspberryOp.add("Raspberry"); // Плоды
        object.put("gfx/terobjs/bushes/raspberrybush", raspberryOp);

        // Sandthorn Bush
        ArrayList<String> sandthornOp = new ArrayList<>();
        sandthornOp.add("Seaberries"); // Плоды
        object.put("gfx/terobjs/bushes/sandthorn", sandthornOp);

        // Spindle Bush
        ArrayList<String> spindleOp = new ArrayList<>();
        spindleOp.add("Spindleberries"); // Плоды
        object.put("gfx/terobjs/bushes/spindlebush", spindleOp);

        // Tea Bush
        ArrayList<String> teabushOp = new ArrayList<>();
        teabushOp.add("Teabush Seedpod"); // Семена
        teabushOp.add("Fresh Tea Leaves"); // Листья
        object.put("gfx/terobjs/bushes/teabush", teabushOp);

        // Tibast Bush
        ArrayList<String> tibastOp = new ArrayList<>();
        tibastOp.add("Tibast Berries"); // Плоды
        object.put("gfx/terobjs/bushes/tibast", tibastOp);

        // Tundra Rose Bush
        ArrayList<String> tundraroseOp = new ArrayList<>();
        tundraroseOp.add("Tundra Rose Fruit"); // Плоды
        object.put("gfx/terobjs/bushes/tundrarose", tundraroseOp);

        // Witherstand Bush
        ArrayList<String> witherstandOp = new ArrayList<>();
        witherstandOp.add("Withercorn"); // Плоды
        object.put("gfx/terobjs/bushes/witherstand", witherstandOp);

        // Redcurrant Bush
        ArrayList<String> redcurrantOp = new ArrayList<>();
        redcurrantOp.add("Redcurrant"); // Плоды
        object.put("gfx/terobjs/bushes/redcurrant", redcurrantOp);

        // Boxwood Bush
        ArrayList<String> boxwoodOp = new ArrayList<>();
        boxwoodOp.add("Boxwood Seeds"); // Плоды
        object.put("gfx/terobjs/bushes/boxwood", boxwoodOp);

        ArrayList<String> acaciaOp_log = new ArrayList<>();
        acaciaOp_log.add("Acacia Board"); // Доска
        acaciaOp_log.add("Acacia Block"); // Блок
        object.put("gfx/terobjs/trees/acacialog", acaciaOp_log);

        ArrayList<String> alderOp_log = new ArrayList<>();
        alderOp_log.add("Alder Board"); // Доска
        alderOp_log.add("Alder Block"); // Блок
        object.put("gfx/terobjs/trees/alderlog", alderOp_log);

        ArrayList<String> almondOp_log = new ArrayList<>();
        almondOp_log.add("Almond Board"); // Доска
        almondOp_log.add("Almond Block"); // Блок
        object.put("gfx/terobjs/trees/almondtreelog", almondOp_log);

        ArrayList<String> appleOp_log = new ArrayList<>();
        appleOp_log.add("Apple Board"); // Доска
        appleOp_log.add("Apple Block"); // Блок
        object.put("gfx/terobjs/trees/appletreelog", appleOp_log);

        ArrayList<String> ashOp_log = new ArrayList<>();
        ashOp_log.add("Ash Board"); // Доска
        ashOp_log.add("Ash Block"); // Блок
        object.put("gfx/terobjs/trees/ashlog", ashOp_log);

        ArrayList<String> aspenOp_log = new ArrayList<>();
        aspenOp_log.add("Aspen Board"); // Доска
        aspenOp_log.add("Aspen Block"); // Блок
        object.put("gfx/terobjs/trees/aspenlog", aspenOp_log);

        ArrayList<String> baywillowOp_log = new ArrayList<>();
        baywillowOp_log.add("Bay Willow Board"); // Доска
        baywillowOp_log.add("Bay Willow Block"); // Блок
        object.put("gfx/terobjs/trees/baywillowlog", baywillowOp_log);

        ArrayList<String> beechOp_log = new ArrayList<>();
        beechOp_log.add("Beech Board"); // Доска
        beechOp_log.add("Beech Block"); // Блок
        object.put("gfx/terobjs/trees/beechlog", beechOp_log);

        ArrayList<String> birchOp_log = new ArrayList<>();
        birchOp_log.add("Birch Board"); // Доска
        birchOp_log.add("Birch Block"); // Блок
        object.put("gfx/terobjs/trees/birchlog", birchOp_log);

        ArrayList<String> birdcherryOp_log = new ArrayList<>();
        birdcherryOp_log.add("Birdcherry Board"); // Доска
        birdcherryOp_log.add("Bird Cherry Block"); // Блок
        object.put("gfx/terobjs/trees/birdcherrytreelog", birdcherryOp_log);

        ArrayList<String> blackpineOp_log = new ArrayList<>();
        blackpineOp_log.add("Black Pine Board"); // Доска
        blackpineOp_log.add("Black Pine Block"); // Блок
        object.put("gfx/terobjs/trees/blackpinelog", blackpineOp_log);

        ArrayList<String> blackpOp_loglarOp_log = new ArrayList<>();
        blackpOp_loglarOp_log.add("Black POp_loglar Board"); // Доска
        blackpOp_loglarOp_log.add("Black POp_loglar Block"); // Блок
        object.put("gfx/terobjs/trees/blackpOp_loglarlog", blackpOp_loglarOp_log);

        ArrayList<String> briarwoodOp_log = new ArrayList<>();
        briarwoodOp_log.add("Briarwood Board (Heath)"); // Доска
        briarwoodOp_log.add("Tree Heath Block"); // Блок
        object.put("gfx/terobjs/trees/treeheathlog", briarwoodOp_log);

        ArrayList<String> buckthornOp_log = new ArrayList<>();
        buckthornOp_log.add("Buckthorn Board"); // Доска
        buckthornOp_log.add("Buckthorn Block"); // Блок
        object.put("gfx/terobjs/trees/buckthornlog", buckthornOp_log);

        ArrayList<String> carobOp_log = new ArrayList<>();
        carobOp_log.add("Carob Board"); // Доска
        carobOp_log.add("Carob Block"); // Блок
        object.put("gfx/terobjs/trees/carobtreelog", carobOp_log);

        ArrayList<String> cedarOp_log = new ArrayList<>();
        cedarOp_log.add("Cedar Board"); // Доска
        cedarOp_log.add("Cedar Block"); // Блок
        object.put("gfx/terobjs/trees/cedarlog", cedarOp_log);

        ArrayList<String> charredOp_log = new ArrayList<>();
        charredOp_log.add("Charred Board (Valhalla)"); // Доска
        charredOp_log.add("Charred Block"); // Блок
        object.put("gfx/terobjs/trees/charredtreelog", charredOp_log);

        ArrayList<String> chasteOp_log = new ArrayList<>();
        chasteOp_log.add("Chaste Board"); // Доска
        chasteOp_log.add("Chaste Tree Block"); // Блок
        object.put("gfx/terobjs/trees/chastetreelog", chasteOp_log);

        ArrayList<String> checkerOp_log = new ArrayList<>();
        checkerOp_log.add("Checker Board"); // Доска
        checkerOp_log.add("Checker Tree Block"); // Блок
        object.put("gfx/terobjs/trees/checkertreelog", checkerOp_log);

        ArrayList<String> cherryOp_log = new ArrayList<>();
        cherryOp_log.add("Cherry Board"); // Доска
        cherryOp_log.add("Cherry Block"); // Блок
        object.put("gfx/terobjs/trees/cherrylog", cherryOp_log);

        ArrayList<String> chestnutOp_log = new ArrayList<>();
        chestnutOp_log.add("Chestnut Board"); // Доска
        chestnutOp_log.add("Chestnut Block"); // Блок
        object.put("gfx/terobjs/trees/chestnuttreelog", chestnutOp_log);

        ArrayList<String> conkerOp_log = new ArrayList<>();
        conkerOp_log.add("Conker Board"); // Доска
        conkerOp_log.add("Conker Block"); // Блок
        object.put("gfx/terobjs/trees/conkertreelog", conkerOp_log);

        ArrayList<String> corkOakOp_log = new ArrayList<>();
        corkOakOp_log.add("Cork Oak Board"); // Доска
        corkOakOp_log.add("Cork Oak Block"); // Блок
        object.put("gfx/terobjs/trees/corkoaklog", corkOakOp_log);

        ArrayList<String> crabappleOp_log = new ArrayList<>();
        crabappleOp_log.add("Crabapple Board"); // Доска
        crabappleOp_log.add("Crabapple Block"); // Блок
        object.put("gfx/terobjs/trees/crabappletreelog", crabappleOp_log);

        ArrayList<String> cypressOp_log = new ArrayList<>();
        cypressOp_log.add("Cypress Board"); // Доска
        cypressOp_log.add("Cypress Block"); // Блок
        object.put("gfx/terobjs/trees/cypresslog", cypressOp_log);

        ArrayList<String> dogwoodOp_log = new ArrayList<>();
        dogwoodOp_log.add("Dogwood Board"); // Доска
        dogwoodOp_log.add("Dogwood Block"); // Блок
        object.put("gfx/terobjs/trees/dogwoodlog", dogwoodOp_log);

        ArrayList<String> dwarfpineOp_log = new ArrayList<>();
        dwarfpineOp_log.add("Dwarf Pine Board"); // Доска
        dwarfpineOp_log.add("Dwarf Pine Block"); // Блок
        object.put("gfx/terobjs/trees/dwarfpinelog", dwarfpineOp_log);

        ArrayList<String> elmOp_log = new ArrayList<>();
        elmOp_log.add("Elm Board"); // Доска
        elmOp_log.add("Elm Block"); // Блок
        object.put("gfx/terobjs/trees/elmlog", elmOp_log);

        ArrayList<String> figOp_log = new ArrayList<>();
        figOp_log.add("Fig Board"); // Доска
        figOp_log.add("Fig Block"); // Блок
        object.put("gfx/terobjs/trees/figtreelog", figOp_log);

        ArrayList<String> firOp_log = new ArrayList<>();
        firOp_log.add("Fir Board"); // Доска
        firOp_log.add("Fir Block"); // Блок
        object.put("gfx/terobjs/trees/firlog", firOp_log);

        ArrayList<String> gloomcapOp_log = new ArrayList<>();
        gloomcapOp_log.add("Gloomcap Board"); // Доска
        gloomcapOp_log.add("Gloomcap Block"); // Блок
        object.put("gfx/terobjs/trees/gloomcaplog", gloomcapOp_log);

        ArrayList<String> gnomeshatOp_log = new ArrayList<>();
        gnomeshatOp_log.add("Gnome's Hat Board"); // Доска
        gnomeshatOp_log.add("Gnome's Hat Block"); // Блок
        object.put("gfx/terobjs/trees/gnomeshatlog", gnomeshatOp_log);

        ArrayList<String> goldenchainOp_log = new ArrayList<>();
        goldenchainOp_log.add("Golden-chain Board"); // Доска
        goldenchainOp_log.add("Golden Chain Block"); // Блок
        object.put("gfx/terobjs/trees/goldenchainlog", goldenchainOp_log);

        ArrayList<String> grayalderOp_log = new ArrayList<>();
        grayalderOp_log.add("Gray Alder Board"); // Доска
        grayalderOp_log.add("Gray Alder Block"); // Блок
        object.put("gfx/terobjs/trees/grayalderlog", grayalderOp_log);

        ArrayList<String> hazelOp_log = new ArrayList<>();
        hazelOp_log.add("Hazel Board"); // Доска
        hazelOp_log.add("Hazel Block"); // Блок
        object.put("gfx/terobjs/trees/hazellog", hazelOp_log);

        ArrayList<String> hornbeamOp_log = new ArrayList<>();
        hornbeamOp_log.add("Hornbeam Board"); // Доска
        hornbeamOp_log.add("Hornbeam Block"); // Блок
        object.put("gfx/terobjs/trees/hornbeamlog", hornbeamOp_log);

        ArrayList<String> juniperOp_log = new ArrayList<>();
        juniperOp_log.add("Juniper Board"); // Доска
        juniperOp_log.add("Juniper Block"); // Блок
        object.put("gfx/terobjs/trees/juniperlog", juniperOp_log);

        ArrayList<String> kingsoakOp_log = new ArrayList<>();
        kingsoakOp_log.add("King's Oak Board"); // Доска
        kingsoakOp_log.add("King's Oak Block"); // Блок
        object.put("gfx/terobjs/trees/kingsoaklog", kingsoakOp_log);

        ArrayList<String> larchOp_log = new ArrayList<>();
        larchOp_log.add("Larch Board"); // Доска
        larchOp_log.add("Larch Block"); // Блок
        object.put("gfx/terobjs/trees/larchlog", larchOp_log);

        ArrayList<String> laurelOp_log = new ArrayList<>();
        laurelOp_log.add("Laurel Board"); // Доска
        laurelOp_log.add("Laurel Block"); // Блок
        object.put("gfx/terobjs/trees/laurellog", laurelOp_log);

        ArrayList<String> lemonOp_log = new ArrayList<>();
        lemonOp_log.add("Lemon Board"); // Доска
        lemonOp_log.add("Lemon Block"); // Блок
        object.put("gfx/terobjs/trees/lemontreelog", lemonOp_log);

        ArrayList<String> lindenOp_log = new ArrayList<>();
        lindenOp_log.add("Linden Board"); // Доска
        lindenOp_log.add("Linden Block"); // Блок
        object.put("gfx/terobjs/trees/lindenlog", lindenOp_log);

        ArrayList<String> loteOp_log = new ArrayList<>();
        loteOp_log.add("Lote Board"); // Доска
        loteOp_log.add("Lote Tree Block"); // Блок
        object.put("gfx/terobjs/trees/lotetreelog", loteOp_log);

        ArrayList<String> mapleOp_log = new ArrayList<>();
        mapleOp_log.add("Maple Board"); // Доска
        mapleOp_log.add("Maple Block"); // Блок
        object.put("gfx/terobjs/trees/maplelog", mapleOp_log);

        ArrayList<String> mayflowerOp_log = new ArrayList<>();
        mayflowerOp_log.add("Mayflower Board"); // Доска
        mayflowerOp_log.add("Mayflower Block"); // Блок
        object.put("gfx/terobjs/trees/mayflowerlog", mayflowerOp_log);

        ArrayList<String> medlarOp_log = new ArrayList<>();
        medlarOp_log.add("Medlar Board"); // Доска
        medlarOp_log.add("Medlar Block"); // Блок
        object.put("gfx/terobjs/trees/medlartreelog", medlarOp_log);

        ArrayList<String> moundOp_log = new ArrayList<>();
        moundOp_log.add("Mound Board"); // Доска
        moundOp_log.add("Mound Block"); // Блок
        object.put("gfx/terobjs/trees/moundtreelog", moundOp_log);

        ArrayList<String> mulberryOp_log = new ArrayList<>();
        mulberryOp_log.add("Mulberry Board"); // Доска
        mulberryOp_log.add("Mulberry Block"); // Блок
        object.put("gfx/terobjs/trees/mulberrylog", mulberryOp_log);

        ArrayList<String> oakOp_log = new ArrayList<>();
        oakOp_log.add("Oak Board"); // Доска
        oakOp_log.add("Oak Block"); // Блок
        object.put("gfx/terobjs/trees/oaklog", oakOp_log);

        ArrayList<String> oliveOp_log = new ArrayList<>();
        oliveOp_log.add("Olive Board"); // Доска
        oliveOp_log.add("Olive Block"); // Блок
        object.put("gfx/terobjs/trees/olivetreelog", oliveOp_log);

        ArrayList<String> orangeOp_log = new ArrayList<>();
        orangeOp_log.add("Orange Board"); // Доска
        orangeOp_log.add("Orange Block"); // Блок
        object.put("gfx/terobjs/trees/orangetreelog", orangeOp_log);

        ArrayList<String> osierOp_log = new ArrayList<>();
        osierOp_log.add("Osier Board"); // Доска
        osierOp_log.add("Osier Block"); // Блок
        object.put("gfx/terobjs/trees/osierlog", osierOp_log);

        ArrayList<String> pearOp_log = new ArrayList<>();
        pearOp_log.add("Pear Board"); // Доска
        pearOp_log.add("Pear Block"); // Блок
        object.put("gfx/terobjs/trees/peartreelog", pearOp_log);

        ArrayList<String> persimmonOp_log = new ArrayList<>();
        persimmonOp_log.add("Persimmon Board"); // Доска
        persimmonOp_log.add("Persimmon Block"); // Блок
        object.put("gfx/terobjs/trees/persimmontreelog", persimmonOp_log);

        ArrayList<String> pineOp_log = new ArrayList<>();
        pineOp_log.add("Pine Board"); // Доска
        pineOp_log.add("Pine Block"); // Блок
        object.put("gfx/terobjs/trees/pinelog", pineOp_log);

        ArrayList<String> planeOp_log = new ArrayList<>();
        planeOp_log.add("Plane Board"); // Доска
        planeOp_log.add("Plane Block"); // Блок
        object.put("gfx/terobjs/trees/planetreelog", planeOp_log);

        ArrayList<String> plumOp_log = new ArrayList<>();
        plumOp_log.add("Plum Board"); // Доска
        plumOp_log.add("Plum Block"); // Блок
        object.put("gfx/terobjs/trees/plumtreelog", plumOp_log);

        ArrayList<String> pOp_loglarOp_log = new ArrayList<>();
        pOp_loglarOp_log.add("POp_loglar Board"); // Доска
        pOp_loglarOp_log.add("POp_loglar Block"); // Блок
        object.put("gfx/terobjs/trees/pOp_loglarlog", pOp_loglarOp_log);

        ArrayList<String> quinceOp_log = new ArrayList<>();
        quinceOp_log.add("Quince Board"); // Доска
        quinceOp_log.add("Quince Block"); // Блок
        object.put("gfx/terobjs/trees/quincetreelog", quinceOp_log);

        ArrayList<String> rowanOp_log = new ArrayList<>();
        rowanOp_log.add("Rowan Board"); // Доска
        rowanOp_log.add("Rowan Block"); // Блок
        object.put("gfx/terobjs/trees/rowanlog", rowanOp_log);

        ArrayList<String> sallowOp_log = new ArrayList<>();
        sallowOp_log.add("Sallow Board"); // Доска
        sallowOp_log.add("Sallow Block"); // Блок
        object.put("gfx/terobjs/trees/sallowlog", sallowOp_log);

        ArrayList<String> silverfirOp_log = new ArrayList<>();
        silverfirOp_log.add("Silver Fir Board"); // Доска
        silverfirOp_log.add("Silver Fir Block"); // Блок
        object.put("gfx/terobjs/trees/silverfirlog", silverfirOp_log);

        ArrayList<String> sorbOp_log = new ArrayList<>();
        sorbOp_log.add("Sorb Board"); // Доска
        sorbOp_log.add("Sorb Block"); // Блок
        object.put("gfx/terobjs/trees/sorbtreelog", sorbOp_log);

        ArrayList<String> spruceOp_log = new ArrayList<>();
        spruceOp_log.add("Spruce Board"); // Доска
        spruceOp_log.add("Spruce Block"); // Блок
        object.put("gfx/terobjs/trees/sprucelog", spruceOp_log);

        ArrayList<String> stonepineOp_log = new ArrayList<>();
        stonepineOp_log.add("Stone Pine Board"); // Доска
        stonepineOp_log.add("Stone Pine Block"); // Блок
        object.put("gfx/terobjs/trees/stonepinelog", stonepineOp_log);

        ArrayList<String> sweetgumOp_log = new ArrayList<>();
        sweetgumOp_log.add("Sweetgum Board"); // Доска
        sweetgumOp_log.add("Sweetgum Block"); // Блок
        object.put("gfx/terobjs/trees/sweetgumlog", sweetgumOp_log);

        ArrayList<String> sycamoreOp_log = new ArrayList<>();
        sycamoreOp_log.add("Sycamore Board"); // Доска
        sycamoreOp_log.add("Sycamore Block"); // Блок
        object.put("gfx/terobjs/trees/sycamorelog", sycamoreOp_log);

        ArrayList<String> tamariskOp_log = new ArrayList<>();
        tamariskOp_log.add("Tamarisk Board"); // Доска
        tamariskOp_log.add("Tamarisk Block"); // Блок
        object.put("gfx/terobjs/trees/tamarisklog", tamariskOp_log);

        ArrayList<String> terebinthOp_log = new ArrayList<>();
        terebinthOp_log.add("Terebinth Board"); // Доска
        terebinthOp_log.add("Terebinth Block"); // Блок
        object.put("gfx/terobjs/trees/terebinthlog", terebinthOp_log);

        ArrayList<String> towercapOp_log = new ArrayList<>();
        towercapOp_log.add("Towercap Board"); // Доска
        towercapOp_log.add("Towercap Block"); // Блок
        object.put("gfx/terobjs/trees/towercaplog", towercapOp_log);

        ArrayList<String> trombonechantrelleOp_log = new ArrayList<>();
        trombonechantrelleOp_log.add("Trumpet Chantrelle Board"); // Доска
        trombonechantrelleOp_log.add("Trombone Chantrelle Block"); // Блок
        object.put("gfx/terobjs/trees/trombonechantrellelog", trombonechantrelleOp_log);

        ArrayList<String> walnutOp_log = new ArrayList<>();
        walnutOp_log.add("Walnut Board"); // Доска
        walnutOp_log.add("Walnut Block"); // Блок
        object.put("gfx/terobjs/trees/walnuttreelog", walnutOp_log);

        ArrayList<String> wartybirchOp_log = new ArrayList<>();
        wartybirchOp_log.add("Warty Birch Board"); // Доска
        wartybirchOp_log.add("Warty Birch Block"); // Блок
        object.put("gfx/terobjs/trees/wartybirchlog", wartybirchOp_log);

        ArrayList<String> whitebeamOp_log = new ArrayList<>();
        whitebeamOp_log.add("Whitebeam Board"); // Доска
        whitebeamOp_log.add("Whitebeam Block"); // Блок
        object.put("gfx/terobjs/trees/whitebeamlog", whitebeamOp_log);

        ArrayList<String> willowOp_log = new ArrayList<>();
        willowOp_log.add("Willow Board"); // Доска
        willowOp_log.add("Willow Block"); // Блок
        object.put("gfx/terobjs/trees/willowlog", willowOp_log);

        ArrayList<String> woodStrawberryOp_log = new ArrayList<>();
        woodStrawberryOp_log.add("Wood Strawberry Board"); // Доска
        woodStrawberryOp_log.add("Wood Strawberry Block"); // Блок
        object.put("gfx/terobjs/trees/strawberrytreelog", woodStrawberryOp_log);

        ArrayList<String> wychelmOp_log = new ArrayList<>();
        wychelmOp_log.add("Wych Elm Board"); // Доска
        wychelmOp_log.add("Wych Elm Block"); // Блок
        object.put("gfx/terobjs/trees/wychelmlog", wychelmOp_log);

        ArrayList<String> yewOp_log = new ArrayList<>();
        yewOp_log.add("Yew Board"); // Доска
        yewOp_log.add("Yew Block"); // Блок
        object.put("gfx/terobjs/trees/yewlog", yewOp_log);

        ArrayList<String> zelkovaOp_log = new ArrayList<>();
        zelkovaOp_log.add("Zelkova Board"); // Доска
        zelkovaOp_log.add("Zelkova Block"); // Блок
        object.put("gfx/terobjs/trees/zelkovalog", zelkovaOp_log);
        ArrayList<String> alabasterOp = new ArrayList<>();
        alabasterOp.add("Alabaster"); // Камень
        object.put("gfx/terobjs/bumlings/alabaster", alabasterOp);

        ArrayList<String> apatiteOp = new ArrayList<>();
        apatiteOp.add("Apatite"); // Камень
        object.put("gfx/terobjs/bumlings/apatite", apatiteOp);

        ArrayList<String> arkoseOp = new ArrayList<>();
        arkoseOp.add("Arkose"); // Камень
        object.put("gfx/terobjs/bumlings/arkose", arkoseOp);

        ArrayList<String> basaltOp = new ArrayList<>();
        basaltOp.add("Basalt"); // Камень
        object.put("gfx/terobjs/bumlings/basalt", basaltOp);

        ArrayList<String> blackcoalOp = new ArrayList<>();
        blackcoalOp.add("Black Coal"); // Камень
        object.put("gfx/terobjs/bumlings/blackcoal", blackcoalOp);

        ArrayList<String> magnetiteOp = new ArrayList<>();
        magnetiteOp.add("Magnetite"); // Камень
        object.put("gfx/terobjs/bumlings/magnetite", magnetiteOp);

        ArrayList<String> hematiteOp = new ArrayList<>();
        hematiteOp.add("Bloodstone"); // Камень
        object.put("gfx/terobjs/bumlings/hematite", hematiteOp);

        ArrayList<String> brecciaOp = new ArrayList<>();
        brecciaOp.add("Breccia"); // Камень
        object.put("gfx/terobjs/bumlings/breccia", brecciaOp);

        ArrayList<String> cassiteriteOp = new ArrayList<>();
        cassiteriteOp.add("Cassiterite"); // Камень
        object.put("gfx/terobjs/bumlings/cassiterite", cassiteriteOp);

        ArrayList<String> chalcopyriteOp = new ArrayList<>();
        chalcopyriteOp.add("Chalcopyrite"); // Камень
        object.put("gfx/terobjs/bumlings/chalcopyrite", chalcopyriteOp);

        ArrayList<String> chertOp = new ArrayList<>();
        chertOp.add("Chert"); // Камень
        object.put("gfx/terobjs/bumlings/chert", chertOp);

        ArrayList<String> cinnabarOp = new ArrayList<>();
        cinnabarOp.add("Cinnabar"); // Камень
        object.put("gfx/terobjs/bumlings/cinnabar", cinnabarOp);

        ArrayList<String> diabaseOp = new ArrayList<>();
        diabaseOp.add("Diabase"); // Камень
        object.put("gfx/terobjs/bumlings/diabase", diabaseOp);

        ArrayList<String> dioriteOp = new ArrayList<>();
        dioriteOp.add("Diorite"); // Камень
        object.put("gfx/terobjs/bumlings/diorite", dioriteOp);

        ArrayList<String> petziteOp = new ArrayList<>();
        petziteOp.add("Direvein Ore"); // Камень
        object.put("gfx/terobjs/bumlings/petzite", petziteOp);

        ArrayList<String> dolomiteOp = new ArrayList<>();
        dolomiteOp.add("Dolomite"); // Камень
        object.put("gfx/terobjs/bumlings/dolomite", dolomiteOp);

        ArrayList<String> eclogiteOp = new ArrayList<>();
        eclogiteOp.add("Eclogite"); // Камень
        object.put("gfx/terobjs/bumlings/eclogite", eclogiteOp);

        ArrayList<String> feldsparOp = new ArrayList<>();
        feldsparOp.add("Feldspar"); // Камень
        object.put("gfx/terobjs/bumlings/feldspar", feldsparOp);

        ArrayList<String> flintOp = new ArrayList<>();
        flintOp.add("Flint"); // Камень
        object.put("gfx/terobjs/bumlings/flint", flintOp);

        ArrayList<String> fluorosparOp = new ArrayList<>();
        fluorosparOp.add("Fluorospar"); // Камень
        object.put("gfx/terobjs/bumlings/fluorospar", fluorosparOp);

        ArrayList<String> gabbroOp = new ArrayList<>();
        gabbroOp.add("Gabbro"); // Камень
        object.put("gfx/terobjs/bumlings/gabbro", gabbroOp);

        ArrayList<String> galenaOp = new ArrayList<>();
        galenaOp.add("Galena"); // Камень
        object.put("gfx/terobjs/bumlings/galena", galenaOp);

        ArrayList<String> gneissOp = new ArrayList<>();
        gneissOp.add("Gneiss"); // Камень
        object.put("gfx/terobjs/bumlings/gneiss", gneissOp);

        ArrayList<String> graniteOp = new ArrayList<>();
        graniteOp.add("Granite"); // Камень
        object.put("gfx/terobjs/bumlings/granite", graniteOp);

        ArrayList<String> graywackeOp = new ArrayList<>();
        graywackeOp.add("Graywacke"); // Камень
        object.put("gfx/terobjs/bumlings/graywacke", graywackeOp);

        ArrayList<String> greenschistOp = new ArrayList<>();
        greenschistOp.add("Greenschist"); // Камень
        object.put("gfx/terobjs/bumlings/greenschist", greenschistOp);

        ArrayList<String> ilmeniteOp = new ArrayList<>();
        ilmeniteOp.add("Heavy Earth Ore"); // Камень
        object.put("gfx/terobjs/bumlings/ilmenite", ilmeniteOp);

//        ArrayList<String> hornsilverOp = new ArrayList<>();
//        hornsilverOp.add("Horn Silver"); // Камень
//        object.put("gfx/terobjs/bumlings/hornsilver", hornsilverOp);

        ArrayList<String> hornblendeOp = new ArrayList<>();
        hornblendeOp.add("Hornblende"); // Камень
        object.put("gfx/terobjs/bumlings/hornblende", hornblendeOp);

//        ArrayList<String> limoniteOp = new ArrayList<>();
//        limoniteOp.add("Iron Ochre Ore"); // Камень
//        object.put("gfx/terobjs/bumlings/limonite", limoniteOp);

        ArrayList<String> jasperOp = new ArrayList<>();
        jasperOp.add("Jasper"); // Камень
        object.put("gfx/terobjs/bumlings/jasper", jasperOp);

        ArrayList<String> kyaniteOp = new ArrayList<>();
        kyaniteOp.add("Kyanite"); // Камень
        object.put("gfx/terobjs/bumlings/kyanite", kyaniteOp);

        ArrayList<String> lavarockOp = new ArrayList<>();
        lavarockOp.add("Lava Rock"); // Камень
        object.put("gfx/terobjs/bumlings/lavarock", lavarockOp);

        ArrayList<String> leadglanceOp = new ArrayList<>();
        leadglanceOp.add("Lead Glance"); // Камень
        object.put("gfx/terobjs/bumlings/leadglance", leadglanceOp);

//        ArrayList<String> nagyagiteOp = new ArrayList<>();
//        nagyagiteOp.add("Leaf Ore"); // Камень
//        object.put("gfx/terobjs/bumlings/nagyagite", nagyagiteOp);

        ArrayList<String> limestoneOp = new ArrayList<>();
        limestoneOp.add("Limestone"); // Камень
        object.put("gfx/terobjs/bumlings/limestone", limestoneOp);

        ArrayList<String> malachiteOp = new ArrayList<>();
        malachiteOp.add("Malachite"); // Камень
        object.put("gfx/terobjs/bumlings/malachite", malachiteOp);

        ArrayList<String> marbleOp = new ArrayList<>();
        marbleOp.add("Marble"); // Камень
        object.put("gfx/terobjs/bumlings/marble", marbleOp);

        ArrayList<String> micaOp = new ArrayList<>();
        micaOp.add("Mica"); // Камень
        object.put("gfx/terobjs/bumlings/mica", micaOp);

        ArrayList<String> microliteOp = new ArrayList<>();
        microliteOp.add("Microlite"); // Камень
        object.put("gfx/terobjs/bumlings/microlite", microliteOp);

        ArrayList<String> olivineOp = new ArrayList<>();
        olivineOp.add("Olivine"); // Камень
        object.put("gfx/terobjs/bumlings/olivine", olivineOp);

        ArrayList<String> orthoclaseOp = new ArrayList<>();
        orthoclaseOp.add("Orthoclase"); // Камень
        object.put("gfx/terobjs/bumlings/orthoclase", orthoclaseOp);

        ArrayList<String> peacockoreOp = new ArrayList<>();
        peacockoreOp.add("Peacock Ore"); // Камень
        object.put("gfx/terobjs/bumlings/peacockore", peacockoreOp);

        ArrayList<String> pegmatiteOp = new ArrayList<>();
        pegmatiteOp.add("Pegmatite"); // Камень
        object.put("gfx/terobjs/bumlings/pegmatite", pegmatiteOp);

        ArrayList<String> porphyryOp = new ArrayList<>();
        porphyryOp.add("Porphyry"); // Камень
        object.put("gfx/terobjs/bumlings/porphyry", porphyryOp);

        ArrayList<String> pumiceOp = new ArrayList<>();
        pumiceOp.add("Pumice"); // Камень
        object.put("gfx/terobjs/bumlings/pumice", pumiceOp);

        ArrayList<String> quartzOp = new ArrayList<>();
        quartzOp.add("Quartz"); // Камень
        object.put("gfx/terobjs/bumlings/quartz", quartzOp);

        ArrayList<String> rhyoliteOp = new ArrayList<>();
        rhyoliteOp.add("Rhyolite"); // Камень
        object.put("gfx/terobjs/bumlings/rhyolite", rhyoliteOp);

        ArrayList<String> sandstoneOp = new ArrayList<>();
        sandstoneOp.add("Sandstone"); // Камень
        object.put("gfx/terobjs/bumlings/sandstone", sandstoneOp);

        ArrayList<String> schistOp = new ArrayList<>();
        schistOp.add("Schist"); // Камень
        object.put("gfx/terobjs/bumlings/schist", schistOp);

//        ArrayList<String> sylvaniteOp = new ArrayList<>();
//        sylvaniteOp.add("Schrifterz Ore"); // Камень
//        object.put("gfx/terobjs/bumlings/sylvanite", sylvaniteOp);

        ArrayList<String> serpentineOp = new ArrayList<>();
        serpentineOp.add("Serpentine"); // Камень
        object.put("gfx/terobjs/bumlings/serpentine", serpentineOp);

        ArrayList<String> argentiteOp = new ArrayList<>();
        argentiteOp.add("Silvershine Ore"); // Камень
        object.put("gfx/terobjs/bumlings/argentite", argentiteOp);

        ArrayList<String> slateOp = new ArrayList<>();
        slateOp.add("Slate"); // Камень
        object.put("gfx/terobjs/bumlings/slate", slateOp);

        ArrayList<String> soapstoneOp = new ArrayList<>();
        soapstoneOp.add("Soapstone"); // Камень
        object.put("gfx/terobjs/bumlings/soapstone", soapstoneOp);

        ArrayList<String> sodaliteOp = new ArrayList<>();
        sodaliteOp.add("Sodalite"); // Камень
        object.put("gfx/terobjs/bumlings/sodalite", sodaliteOp);

        ArrayList<String> sunstoneOp = new ArrayList<>();
        sunstoneOp.add("Sunstone"); // Камень
        object.put("gfx/terobjs/bumlings/sunstone", sunstoneOp);

        ArrayList<String> cupriteOp = new ArrayList<>();
        cupriteOp.add("Wine Glance"); // Камень
        object.put("gfx/terobjs/bumlings/cuprite", cupriteOp);

        ArrayList<String> zincsparOp = new ArrayList<>();
        zincsparOp.add("Zincspar"); // Камень
        object.put("gfx/terobjs/bumlings/zincspar", zincsparOp);

    }

    static {
        ArrayList<JSONObject> spices = new ArrayList<>();
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/pepperdrupedried\",\"name\":\"Black Pepper\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/ambergris\",\"name\":\"Ambergris\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/propolis\",\"name\":\"Propolis\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/kvann\",\"name\":\"Kvann\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-juniper\",\"name\":\"Juniper Berries\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/clove-garlic\",\"name\":\"Clove of Garlic\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/chives\",\"name\":\"Chives\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-laurel\",\"name\":\"Laurel Leaves\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/salvia\",\"name\":\"Sage\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/thyme\",\"name\":\"Thyme\"}"));
        spices.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/dill\",\"name\":\"Dill\"}"));
        categories.put("Spices", spices);

        ArrayList<JSONObject> rootVegetables = new ArrayList<>();
        rootVegetables.add(new JSONObject("{\"static\":\"gfx/invobjs/beet\",\"name\":\"Beetroot\"}"));
        rootVegetables.add(new JSONObject("{\"static\":\"gfx/invobjs/carrot\",\"name\":\"Carrot\"}"));
        rootVegetables.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/cattailroots\",\"name\":\"Cattail Roots\"}"));
        rootVegetables.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/cavebulb\",\"name\":\"Cavebulb\"}"));
        rootVegetables.add(new JSONObject("{\"static\":\"gfx/invobjs/oddtuber\",\"name\":\"Odd Tuber\"}"));
        rootVegetables.add(new JSONObject("{\"static\":\"gfx/invobjs/turnip\",\"name\":\"Turnip\"}"));
        rootVegetables.add(new JSONObject("{\"static\":\"gfx/invobjs/beetweird\",\"name\":\"Weird Beetroot\"}"));
        categories.put("Tuber", rootVegetables);

        ArrayList<JSONObject> onions = new ArrayList<>();
        onions.add(new JSONObject("{\"static\":\"gfx/invobjs/yellowonion\",\"name\":\"Yellow Onion\"}"));
        onions.add(new JSONObject("{\"static\":\"gfx/invobjs/redonion\",\"name\":\"Red Onion\"}"));
        onions.add(new JSONObject("{\"static\":\"gfx/invobjs/garlic\",\"name\":\"Garlic\"}"));
        onions.add(new JSONObject("{\"static\":\"gfx/invobjs/small/leek\",\"name\":\"Leek\",\"x\":2,\"y\":1}"));
        onions.add(new JSONObject("{\"static\":\"gfx/invobjs/pickledonion\",\"name\":\"Pickled Onion\"}"));
        categories.put("Onion", onions);


        ArrayList<JSONObject> strings = new ArrayList<>();
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/flaxfibre\",\"name\":\"Flax Fibres\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/hempfibre\",\"name\":\"Hemp Fibres\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/spindlytaproot\",\"name\":\"Spindly Taproot\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/cattailfibre\",\"name\":\"Cattail Fibres\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/stingingnettle\",\"name\":\"Stinging Nettle\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/hidestrap\",\"name\":\"Hide Strap\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/strawstring\",\"name\":\"Straw Twine\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/barkcordage\",\"name\":\"Bark Cordage\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/toughroot\",\"name\":\"Tough Root\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/reedtwine\",\"name\":\"Reed Twine\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/toadflax\",\"name\":\"Toadflax\"}"));
        strings.add(new JSONObject("{\"static\":\"gfx/invobjs/trollhair\",\"name\":\"Troll Hair\"}"));
        categories.put("String", strings);

        ArrayList<JSONObject> salads = new ArrayList<>();
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/beetleaves\",\"name\":\"Beetroot Leaves\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/driftkelp\",\"name\":\"Driftkelp\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/duskfern\",\"name\":\"Dusk Fern\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/greenkelp\",\"name\":\"Green Kelp\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-heartwood\",\"name\":\"Heartwood Leaves\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/lettuceleaf\",\"name\":\"Lettuce Leaf\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/greenkale\",\"name\":\"Green Kale\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/marshmallow\",\"name\":\"Marsh-Mallow\"}"));
        salads.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/stingingnettle\",\"name\":\"Stinging Nettle\"}"));
        categories.put("Salad Greens", salads);

        ArrayList<JSONObject> maltedGrains = new ArrayList<>();
        maltedGrains.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-barleymalt\",\"name\":\"Malted Barley\"}"));
        maltedGrains.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-wheatmalt\",\"name\":\"Malted Wheat\"}"));
        categories.put("Malted Grains", maltedGrains);

        ArrayList<JSONObject> millableSeed = new ArrayList<>();
        millableSeed.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-barley\",\"name\":\"Barley Seed\"}"));
        millableSeed.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-wheat\",\"name\":\"Wheat Seed\"}"));
        millableSeed.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-millet\",\"name\":\"Millet Seed\"}"));
        categories.put("Millable Seed", millableSeed);

        ArrayList<JSONObject> seeds = new ArrayList<>();
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-carrot\",\"name\":\"Carrot Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-cucumber\",\"name\":\"Cucumber Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-flax\",\"name\":\"Flax Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-grape\",\"name\":\"Grape Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-hemp\",\"name\":\"Hemp Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-leek\",\"name\":\"Leek Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-lettuce\",\"name\":\"Lettuce Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-pipeweed\",\"name\":\"Pipeweed Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-poppy\",\"name\":\"Poppy Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-pumpkin\",\"name\":\"Pumpkin Seed\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-turnip\",\"name\":\"Turnip Seed\"}"));
        seeds.addAll(maltedGrains);
        seeds.addAll(millableSeed);
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-wheatgerm\",\"name\":\"Sprouted Wheat\"}"));
        seeds.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-barleygerm\",\"name\":\"Sprouted Barley\"}"));
        categories.put("Crop Seeds", seeds);

        ArrayList<JSONObject> eggs = new ArrayList<>();
        eggs.add(new JSONObject("{\"static\":\"gfx/invobjs/egg-bullfinch\",\"name\":\"Bullfinch Egg\"}"));
        eggs.add(new JSONObject("{\"static\":\"gfx/invobjs/egg-chicken\",\"name\":\"Chicken Egg\"}"));
        eggs.add(new JSONObject("{\"static\":\"gfx/invobjs/egg-magpie\",\"name\":\"Magpie Egg\"}"));
        eggs.add(new JSONObject("{\"static\":\"gfx/invobjs/egg-rockdove\",\"name\":\"Rock Dove Egg\"}"));
        eggs.add(new JSONObject("{\"static\":\"gfx/invobjs/egg-woodgrouse\",\"name\":\"Wood Grouse Egg\"}"));
//        eggs.add(new JSONObject("{\"static\":\"gfx/invobjs/pickledegg\",\"name\":\"Pickled Egg\"}"));
        categories.put("Egg", eggs);

        ArrayList<JSONObject> gelatinMaterials = new ArrayList<>();
        gelatinMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/caveslime\",\"name\":\"Cave Slime\"}"));
        gelatinMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/gelatin\",\"name\":\"Gelatin\"}"));
        categories.put("Gellant", gelatinMaterials);

        ArrayList<JSONObject> stuffings = new ArrayList<>();
        stuffings.add(new JSONObject("{\"static\":\"gfx/invobjs/stuffing-meat\",\"name\":\"Meat Stuffing\"}"));
        stuffings.add(new JSONObject("{\"static\":\"gfx/invobjs/stuffing-mushroom\",\"name\":\"Mushroom Stuffing\"}"));
        stuffings.add(new JSONObject("{\"static\":\"gfx/invobjs/stuffing-roe\",\"name\":\"Roe Stuffing\"}"));
        stuffings.add(new JSONObject("{\"static\":\"gfx/invobjs/stuffing-vegetable\",\"name\":\"Vegetable Stuffing\"}"));
        categories.put("Stuffing", stuffings);

        ArrayList<JSONObject> yarn = new ArrayList<>();
        yarn.add(new JSONObject("{\"static\":\"gfx/invobjs/yarn-sheep\",\"name\":\"Yarn\"}"));
        yarn.add(new JSONObject("{\"static\":\"gfx/invobjs/yarn-goat\",\"name\":\"Mohair Yarn\"}"));
        categories.put("Yarn", yarn);

        ArrayList<JSONObject> driedFruits = new ArrayList<>();
        driedFruits.add(new JSONObject("{\"static\":\"gfx/invobjs/fig-dried\",\"name\":\"Dried Fig\"}"));
        driedFruits.add(new JSONObject("{\"static\":\"gfx/invobjs/prune\",\"name\":\"Prune\"}"));
        driedFruits.add(new JSONObject("{\"static\":\"gfx/invobjs/raisins\",\"name\":\"Raisins\"}"));
        categories.put("Dried Fruit", driedFruits);

        ArrayList<JSONObject> mushrooms = new ArrayList<>();
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/baybolete\",\"name\":\"Bay Bolete\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/bloatedbolete\",\"name\":\"Bloated Bolete\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-trombonechantrelle\",\"name\":\"Block of Trombone Chantrelle\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/champignon-small\",\"name\":\"Button Mushroom\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/chantrelle\",\"name\":\"Chantrelles\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/champignon-medium\",\"name\":\"Cremini Mushroom\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/fieldblewit\",\"name\":\"Field Blewits\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/giantpuffball\",\"name\":\"Giant Puffball\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/libertycap\",\"name\":\"Liberty Caps\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/oystermushroom\",\"name\":\"Oyster Mushroom\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/parasolshroom\",\"name\":\"Parasol Mushroom\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/lorchelwet\",\"name\":\"Parboiled Morels\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/champignon-large\",\"name\":\"Portobello Mushroom\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/rubybolete\",\"name\":\"Ruby Bolete\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/snowtop\",\"name\":\"Snowtop\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/stalagoom\",\"name\":\"Stalagoom\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/trollshrooms\",\"name\":\"Troll Mushrooms\"}"));
        mushrooms.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/yellowfoot\",\"name\":\"Yellowfeet\"}"));
        categories.put("Edible Mushroom", mushrooms);

        ArrayList<JSONObject> nuts = new ArrayList<>();
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/almond\",\"name\":\"Almonds\"}"));
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-beech\",\"name\":\"Beech Seed\"}"));
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/carobfruit\",\"name\":\"Carob Pod\"}"));
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/chestnut\",\"name\":\"Chestnut\"}"));
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/hazelnut\",\"name\":\"Hazelnut\"}"));
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-kingsoak\",\"name\":\"King's Oak Seed\"}"));
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/walnut\",\"name\":\"Walnut\"}"));
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-oak\",\"name\":\"Oak Seed\"}"));
        nuts.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-witherstand\",\"name\":\"Witherstand Seed\"}"));
        categories.put("Nuts", nuts);

        ArrayList<JSONObject> cones = new ArrayList<>();
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackpine\",\"name\":\"Black Pine Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-cedar\",\"name\":\"Cedar Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-dwarfpine\",\"name\":\"Dwarf Pine Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-fir\",\"name\":\"Fir Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-larch\",\"name\":\"Larch Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-pine\",\"name\":\"Pine Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-silverfir\",\"name\":\"Silverfir Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-spruce\",\"name\":\"Spruce Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/stonepinecone\",\"name\":\"Stone Pine Cone\"}"));
        cones.add(new JSONObject("{\"static\":\"gfx/invobjs/hopcones-l\",\"name\":\"Unusually Large Hop Cone\"}"));
        categories.put("Decent-sized Conifer Cone", cones);

        ArrayList<JSONObject> berries = new ArrayList<>();
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackberrybush\",\"name\":\"Blackberry Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackcurrant\",\"name\":\"Blackcurrant Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/blueberry\",\"name\":\"Blueberry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/candleberry\",\"name\":\"Candleberry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/cherry\",\"name\":\"Cherry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-dogrose\",\"name\":\"Dog Rose Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-elderberrybush\",\"name\":\"Elderberry Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-gooseberrybush\",\"name\":\"Gooseberry Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/lingon\",\"name\":\"Lingonberry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/mulberry\",\"name\":\"Mulberry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-raspberrybush\",\"name\":\"Raspberry Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-redcurrant\",\"name\":\"Redcurrant Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-sandthorn\",\"name\":\"Seaberries\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackthorn\",\"name\":\"Sloan Berries Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/strawberry\",\"name\":\"Strawberry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/woodstrawberry\",\"name\":\"Wood Strawberry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackcurrant-yester\",\"name\":\"Yesteryear's Blackcurrant Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/cherry-yester\",\"name\":\"Yesteryear's Cherry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-elderberrybush-yester\",\"name\":\"Yesteryear's Elderberry Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-gooseberrybush-yester\",\"name\":\"Yesteryear's Gooseberry Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/mulberry-yester\",\"name\":\"Yesteryear's Mulberry\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-raspberrybush-yester\",\"name\":\"Yesteryear's Raspberry Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-redcurrant-yester\",\"name\":\"Yesteryear's Redcurrant Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-sandthorn-yester\",\"name\":\"Yesteryear's Seaberry Seed\"}"));
        berries.add(new JSONObject("{\"static\":\"gfx/invobjs/woodstrawberry-yester\",\"name\":\"Yesteryear's Wood Strawberry\"}"));
        categories.put("Berry", berries);

        ArrayList<JSONObject> fruits = new ArrayList<>();
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/cherry\",\"name\":\"Cherry\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/fig\",\"name\":\"Fig\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/grapes\",\"name\":\"Grapes\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/lemon\",\"name\":\"Lemon\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/medlar\",\"name\":\"Medlar\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/mulberry\",\"name\":\"Mulberry\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/orange\",\"name\":\"Orange\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/pear\",\"name\":\"Pear\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/persimmon\",\"name\":\"Persimmon\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/plum\",\"name\":\"Plum\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/quince\",\"name\":\"Quince\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-raspberrybush\",\"name\":\"Raspberry Bush Seed\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/apple\",\"name\":\"Red Apple\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-sandthorn\",\"name\":\"Seaberry Seed\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/sorbapple\",\"name\":\"Sorb Apple Seed\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/cherry-yester\",\"name\":\"Yesteryear's Cherry\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/fig-yester\",\"name\":\"Yesteryear's Fig\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/lemon-yester\",\"name\":\"Yesteryear's Lemon\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/medlar-yester\",\"name\":\"Yesteryear's Medlar\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/mulberry-yester\",\"name\":\"Yesteryear's Mulberry\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/orange-yester\",\"name\":\"Yesteryear's Orange\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/pear-yester\",\"name\":\"Yesteryear's Pear\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/persimmon-yester\",\"name\":\"Yesteryear's Persimmon\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/plum-yester\",\"name\":\"Yesteryear's Plum\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/quince-yester\",\"name\":\"Yesteryear's Quince\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-raspberrybush-yester\",\"name\":\"Yesteryear's Raspberry Bush Seed\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/apple-yester\",\"name\":\"Yesteryear's Red Apple\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-sandthorn-yester\",\"name\":\"Yesteryear's Seaberry Seed\"}"));
        fruits.add(new JSONObject("{\"static\":\"gfx/invobjs/sorbapple-yester\",\"name\":\"Yesteryear's Sorb Apple Seed\"}"));
        categories.put("Fruit", fruits);

        ArrayList<JSONObject> fruitOrBerry = new ArrayList<>();
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackberrybush\",\"name\":\"Blackberry Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackcurrant\",\"name\":\"Blackcurrant Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/blueberry\",\"name\":\"Blueberry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/candleberry\",\"name\":\"Candleberry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/cherry\",\"name\":\"Cherry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-dogrose\",\"name\":\"Dog Rose Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-elderberrybush\",\"name\":\"Elderberry Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-gooseberrybush\",\"name\":\"Gooseberry Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/lingon\",\"name\":\"Lingonberry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/mulberry\",\"name\":\"Mulberry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-raspberrybush\",\"name\":\"Raspberry Bush Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-redcurrant\",\"name\":\"Redcurrant Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-sandthorn\",\"name\":\"Seaberry Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackthorn\",\"name\":\"Sloan Berries Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/strawberry\",\"name\":\"Strawberry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/woodstrawberry\",\"name\":\"Wood Strawberry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/fig\",\"name\":\"Fig\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/grapes\",\"name\":\"Grapes\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/lemon\",\"name\":\"Lemon\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/medlar\",\"name\":\"Medlar\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/orange\",\"name\":\"Orange\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/pear\",\"name\":\"Pear\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/persimmon\",\"name\":\"Persimmon\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/plum\",\"name\":\"Plum\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/quince\",\"name\":\"Quince\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/apple\",\"name\":\"Red Apple\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackcurrant-yester\",\"name\":\"Yesteryear's Blackcurrant Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/cherry-yester\",\"name\":\"Yesteryear's Cherry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-elderberrybush-yester\",\"name\":\"Yesteryear's Elderberry Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-gooseberrybush-yester\",\"name\":\"Yesteryear's Gooseberry Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/mulberry-yester\",\"name\":\"Yesteryear's Mulberry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-raspberrybush-yester\",\"name\":\"Yesteryear's Raspberry Bush Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-redcurrant-yester\",\"name\":\"Yesteryear's Redcurrant Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-sandthorn-yester\",\"name\":\"Yesteryear's Seaberry Seed\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/woodstrawberry-yester\",\"name\":\"Yesteryear's Wood Strawberry\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/fig-yester\",\"name\":\"Yesteryear's Fig\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/lemon-yester\",\"name\":\"Yesteryear's Lemon\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/medlar-yester\",\"name\":\"Yesteryear's Medlar\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/orange-yester\",\"name\":\"Yesteryear's Orange\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/pear-yester\",\"name\":\"Yesteryear's Pear\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/persimmon-yester\",\"name\":\"Yesteryear's Persimmon\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/plum-yester\",\"name\":\"Yesteryear's Plum\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/quince-yester\",\"name\":\"Yesteryear's Quince\"}"));
        fruitOrBerry.add(new JSONObject("{\"static\":\"gfx/invobjs/apple-yester\",\"name\":\"Yesteryear's Red Apple\"}"));
        categories.put("Fruit or Berry", fruitOrBerry);

        ArrayList<JSONObject> flours = new ArrayList<>();
        flours.add(new JSONObject("{\"static\":\"gfx/invobjs/flour-barleyflour\",\"name\":\"Barley Flour\"}"));
        flours.add(new JSONObject("{\"static\":\"gfx/invobjs/flour-milletflour\",\"name\":\"Millet Flour\"}"));
        flours.add(new JSONObject("{\"static\":\"gfx/invobjs/flour-wheatflour\",\"name\":\"Wheat Flour\"}"));
        categories.put("Flour", flours);

        ArrayList<JSONObject> giantAntItems = new ArrayList<>();
        giantAntItems.add(new JSONObject("{\"static\":\"gfx/invobjs/ants-larvae\",\"name\":\"Ant Larvae\"}"));
        giantAntItems.add(new JSONObject("{\"static\":\"gfx/invobjs/ants-pupae\",\"name\":\"Ant Pupae\"}"));
        giantAntItems.add(new JSONObject("{\"static\":\"gfx/invobjs/ants-soldiers\",\"name\":\"Ant Soldiers\"}"));
        giantAntItems.add(new JSONObject("{\"static\":\"gfx/invobjs/ants-aphids\",\"name\":\"Aphids\"}"));
        categories.put("Giant Ant", giantAntItems);

        ArrayList<JSONObject> royalAntItems = new ArrayList<>();
        royalAntItems.add(new JSONObject("{\"static\":\"gfx/invobjs/ants-empress\",\"name\":\"Ant Empress\"}"));
        royalAntItems.add(new JSONObject("{\"static\":\"gfx/invobjs/ants-queen\",\"name\":\"Ant Queen\"}"));
        categories.put("Royal Ant", royalAntItems);

        ArrayList<JSONObject> bugs = new ArrayList<>();
        bugs.addAll(giantAntItems);
        bugs.addAll(royalAntItems);
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/beelarvae\",\"name\":\"Bee Larvae\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/brimstonebutterfly\",\"name\":\"Brimstone Butterfly\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/cavecentipede\",\"name\":\"Cave Centipede\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/cavemoth\",\"name\":\"Cave Moth\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/dragonfly-emerald\",\"name\":\"Emerald Dragonfly\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/firefly\",\"name\":\"Firefly\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/grasshopper\",\"name\":\"Grasshopper\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/grub\",\"name\":\"Grub\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/itsybitsyspider\",\"name\":\"Itsy Bitsy Spider\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/ladybug\",\"name\":\"Ladybug\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/monarchbutterfly\",\"name\":\"Monarch Butterfly\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/moonmoth\",\"name\":\"Moonmoth\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/dragonfly-ruby\",\"name\":\"Ruby Dragonfly\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/sandflea\",\"name\":\"Sand Flea\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/silkmoth-f\",\"name\":\"Silkmoth\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/silkmoth-m\",\"name\":\"Male Silkmoth\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/silkworm\",\"name\":\"Silkworm\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/springbumblebee\",\"name\":\"Springtime Bumblebee\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/stagbeetle\",\"name\":\"Stag Beetle\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/beethatstung\",\"name\":\"The Bee That Stung\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/waterstrider\",\"name\":\"Waterstrider\"}"));
        bugs.add(new JSONObject("{\"static\":\"gfx/invobjs/woodworm\",\"name\":\"Woodworm\"}"));
        categories.put("Bug", bugs);

        ArrayList<JSONObject> fishlines = new ArrayList<>();
        fishlines.add(new JSONObject("{\"static\":\"gfx/invobjs/fline-bush\",\"name\":\"Bushcraft Fishline\"}"));
        fishlines.add(new JSONObject("{\"static\":\"gfx/invobjs/fline-farmers\",\"name\":\"Farmer's Fishline\"}"));
        fishlines.add(new JSONObject("{\"static\":\"gfx/invobjs/fline-fine\",\"name\":\"Fine Fishline\"}"));
        fishlines.add(new JSONObject("{\"static\":\"gfx/invobjs/fline-macabre\",\"name\":\"Macabre Fishline\"}"));
        fishlines.add(new JSONObject("{\"static\":\"gfx/invobjs/fline-shepherds\",\"name\":\"Shepherd's Fishline\"}"));
        fishlines.add(new JSONObject("{\"static\":\"gfx/invobjs/fline-shoreline\",\"name\":\"Shoreline Fishline\"}"));
        fishlines.add(new JSONObject("{\"static\":\"gfx/invobjs/fline-tanners\",\"name\":\"Tanner's Fishline\"}"));
        fishlines.add(new JSONObject("{\"static\":\"gfx/invobjs/fline-woodsmans\",\"name\":\"Woodsman's Fishline\"}"));
        categories.put("Fishline", fishlines);

        ArrayList<JSONObject> sweeteners = new ArrayList<>();
        sweeteners.add(new JSONObject("{\"static\":\"gfx/invobjs/birchsap\",\"name\":\"Birchsap\"}"));
        sweeteners.add(new JSONObject("{\"static\":\"gfx/invobjs/honey\",\"name\":\"Domestic Honey\"}"));
        sweeteners.add(new JSONObject("{\"static\":\"gfx/invobjs/maplesap\",\"name\":\"Maplesap\"}"));
        sweeteners.add(new JSONObject("{\"static\":\"gfx/invobjs/nectar\",\"name\":\"Nectar\"}"));
        sweeteners.add(new JSONObject("{\"static\":\"gfx/invobjs/honey\",\"name\":\"Wild-bee Honey\"}"));
        categories.put("Sweetener", sweeteners);

        ArrayList<JSONObject> leaves = new ArrayList<>();
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/beetleaves\",\"name\":\"Beetroot Leaves\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/tea-black\",\"name\":\"Black Tea Leaves\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-conkertree\",\"name\":\"Conker Leaf\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/duskfern\",\"name\":\"Dusk Fern\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-fig\",\"name\":\"Fig Leaf\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/tea-fresh\",\"name\":\"Fresh Tea Leaves\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/greenkelp\",\"name\":\"Green Kelp\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/tea-green\",\"name\":\"Green Tea Leaves\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-heartwood\",\"name\":\"Heartwood Leaves\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-laurel\",\"name\":\"Laurel Leaf\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/lettuceleaf\",\"name\":\"Lettuce Leaf\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-maple\",\"name\":\"Maple Leaf\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-mulberrytree\",\"name\":\"Mulberry Leaf\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/perfectautumnleaf\",\"name\":\"Perfect Autumn Leaf\"}"));
        leaves.add(new JSONObject("{\"static\":\"gfx/invobjs/leaf-swamplily\",\"name\":\"Swamplily Leaf Shred\"}"));
        categories.put("Leaf", leaves);

        ArrayList<JSONObject> treeBoughs = new ArrayList<>();
        treeBoughs.add(new JSONObject("{\"static\":\"gfx/invobjs/bough-alder\",\"name\":\"Alder Bough\"}"));
        treeBoughs.add(new JSONObject("{\"static\":\"gfx/invobjs/bough-elm\",\"name\":\"Elm Bough\"}"));
        treeBoughs.add(new JSONObject("{\"static\":\"gfx/invobjs/bough-fir\",\"name\":\"Fir Bough\"}"));
        treeBoughs.add(new JSONObject("{\"static\":\"gfx/invobjs/bough-grayalder\",\"name\":\"Gray Alder Bough\"}"));
        treeBoughs.add(new JSONObject("{\"static\":\"gfx/invobjs/bough-linden\",\"name\":\"Linden Bough\"}"));
        treeBoughs.add(new JSONObject("{\"static\":\"gfx/invobjs/bough-spruce\",\"name\":\"Spruce Bough\"}"));
        treeBoughs.add(new JSONObject("{\"static\":\"gfx/invobjs/bough-sweetgum\",\"name\":\"Sweetgum Bough\"}"));
        treeBoughs.add(new JSONObject("{\"static\":\"gfx/invobjs/bough-yew\",\"name\":\"Yew Bough\"}"));
        categories.put("Tree Bough", treeBoughs);

        ArrayList<JSONObject> thatchingMaterials = new ArrayList<>();
        thatchingMaterials.addAll(treeBoughs);
        thatchingMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/glimmermoss\",\"name\":\"Glimmermoss\"}"));
        thatchingMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/reeds\",\"name\":\"Reeds\"}"));
        thatchingMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/straw\",\"name\":\"Straw\"}"));
        thatchingMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/tarsticks\",\"name\":\"Tarsticks\"}"));
        categories.put("Thatching Material", thatchingMaterials);

        ArrayList<JSONObject> vegetableOils = new ArrayList<>();
        vegetableOils.add(new JSONObject("{\"static\":\"gfx/invobjs/hempoil\",\"name\":\"Hempseed Oil\"}"));
        vegetableOils.add(new JSONObject("{\"static\":\"gfx/invobjs/flaxoil\",\"name\":\"Linseed Oil\"}"));
        vegetableOils.add(new JSONObject("{\"static\":\"gfx/invobjs/oliveoil\",\"name\":\"Olive Oil\"}"));
        categories.put("Vegetable Oil", vegetableOils);

        ArrayList<JSONObject> flowers = new ArrayList<>();
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/bloodstern\",\"name\":\"Blood Stern\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/camomile\",\"name\":\"Camomile\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/cavebulb\",\"name\":\"Cavebulb\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/chimingbluebell\",\"name\":\"Chiming Bluebell\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/clover\",\"name\":\"Clover\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/coltsfoot\",\"name\":\"Coltsfoot\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/dandelion\",\"name\":\"Dandelion\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/edelweiss\",\"name\":\"EdelweiГџ\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/frogscrown\",\"name\":\"Frog's Crown\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/heartsease\",\"name\":\"Heartsease\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/lupine\",\"name\":\"Lupine\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/marshmallow\",\"name\":\"Marsh-Mallow\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/flower-poppy\",\"name\":\"Poppy Flower\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/stingingnettle\",\"name\":\"Stinging Nettle\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/thornythistle\",\"name\":\"Thorny Thistle\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/snapdragon\",\"name\":\"Uncommon Snapdragon\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/wintergreen\",\"name\":\"Wintergreen\"}"));
        flowers.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/yarrow\",\"name\":\"Yarrow\"}"));
        categories.put("Flower", flowers);

        ArrayList<JSONObject> fats = new ArrayList<>();
        fats.add(new JSONObject("{\"static\":\"gfx/invobjs/spermaceti\",\"name\":\"Hvalraf\"}"));
        fats.add(new JSONObject("{\"static\":\"gfx/invobjs/animalfat-r\",\"name\":\"Rendered Animal Fat\"}"));
        fats.add(new JSONObject("{\"static\":\"gfx/invobjs/butter\",\"name\":\"Butter\"}"));
        categories.put("Solid Fat", fats);

        ArrayList<JSONObject> tea = new ArrayList<>();
        tea.add(new JSONObject("{\"static\":\"gfx/invobjs/tea-green\",\"name\":\"Green Tea Leaves\"}"));
        tea.add(new JSONObject("{\"static\":\"gfx/invobjs/tea-black\",\"name\":\"Black Tea Leaves\"}"));
        categories.put("Cured Tea", tea);

        ArrayList<JSONObject> snails = new ArrayList<>();
        snails.add(new JSONObject("{\"static\":\"gfx/invobjs/forestsnail\",\"name\":\"Forest Snail\"}"));
        snails.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/lakesnail\",\"name\":\"Lake Snail\"}"));
        categories.put("Snail", snails);

        ArrayList<JSONObject> cleanCarcasses = new ArrayList<>();
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/adder-clean\",\"name\":\"Clean Adder Carcass\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/hedgehog-clean\",\"name\":\"Clean Hedgehog Carcass\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/mole-clean\",\"name\":\"Clean Mole Carcass\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/rabbit-clean\",\"name\":\"Clean Rabbit Carcass\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/squirrel-clean\",\"name\":\"Clean Squirrel Carcass\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/stoat-clean\",\"name\":\"Clean Stoat Carcass\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/bat-clean\",\"name\":\"Cleaned Bat\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/bogturtle-cleaned\",\"name\":\"Cleaned Bog Turtle\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/bullfinch-cleaned\",\"name\":\"Cleaned Bullfinch\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/chicken-cleaned\",\"name\":\"Cleaned Chicken\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/eagleowl-cleaned\",\"name\":\"Cleaned Eagle Owl\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/goldeneagle-cleaned\",\"name\":\"Cleaned Golden Eagle\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/magpie-cleaned\",\"name\":\"Cleaned Magpie\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/mallard-cleaned\",\"name\":\"Cleaned Mallard\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/pelican-cleaned\",\"name\":\"Cleaned Pelican\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/ptarmigan-cleaned\",\"name\":\"Cleaned Ptarmigan\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/quail-cleaned\",\"name\":\"Cleaned Quail\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/rockdove-cleaned\",\"name\":\"Cleaned Rock Dove\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/seagull-cleaned\",\"name\":\"Cleaned Seagull\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/swan-cleaned\",\"name\":\"Cleaned Swan\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/woodgrouse-m-cleaned\",\"name\":\"Cleaned Wood Grouse Cock\"}"));
        cleanCarcasses.add(new JSONObject("{\"static\":\"gfx/invobjs/woodgrouse-f-cleaned\",\"name\":\"Cleaned Wood Grouse Hen\"}"));
        categories.put("Clean Animal Carcass", cleanCarcasses);

        ArrayList<JSONObject> baits = new ArrayList<>();
        baits.addAll(giantAntItems);
        baits.addAll(royalAntItems);
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/beelarvae\",\"name\":\"Bee Larvae\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/brimstonebutterfly\",\"name\":\"Brimstone Butterfly\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/cavemoth\",\"name\":\"Cave Moth\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/chumbait\",\"name\":\"Chum Bait\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/earthworm\",\"name\":\"Earthworm\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/dragonfly-emerald\",\"name\":\"Emerald Dragonfly\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/entrails\",\"name\":\"Entrails\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/firefly\",\"name\":\"Firefly\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/grasshopper\",\"name\":\"Grasshopper\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/grub\",\"name\":\"Grub\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/ladybug\",\"name\":\"Ladybug\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/leech\",\"name\":\"Leech\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/monarchbutterfly\",\"name\":\"Monarch Butterfly\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/moonmoth\",\"name\":\"Moonmoth\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/dragonfly-ruby\",\"name\":\"Ruby Dragonfly\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/sandflea\",\"name\":\"Sand Flea\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/silkmoth-f\",\"name\":\"Female Silkmoth\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/silkmoth-m\",\"name\":\"Male Silkmoth\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/silkworm\",\"name\":\"Silkworm\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/silkegg\",\"name\":\"Silkworm Egg\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/springbumblebee\",\"name\":\"Springtime Bumblebee\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/stagbeetle\",\"name\":\"Stag Beetle\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/waterstrider\",\"name\":\"Waterstrider\"}"));
        baits.add(new JSONObject("{\"static\":\"gfx/invobjs/woodworm\",\"name\":\"Woodworm\"}"));
        categories.put("Bait", baits);

        ArrayList<JSONObject> chitinItems = new ArrayList<>();
        chitinItems.add(new JSONObject("{\"static\":\"gfx/invobjs/antchitin\",\"name\":\"Ant Chitin\"}"));
        chitinItems.add(new JSONObject("{\"static\":\"gfx/invobjs/beechitin\",\"name\":\"Bee Chitin\"}"));
        chitinItems.add(new JSONObject("{\"static\":\"gfx/invobjs/cavelousechitin\",\"name\":\"Cave Louse Chitin\"}"));
        categories.put("Chitin", chitinItems);

        ArrayList<JSONObject> goatHorn = new ArrayList<>();
        goatHorn.add(new JSONObject("{\"static\":\"gfx/invobjs/billygoathorn\",\"name\":\"Billygoat Horn\"}"));
        goatHorn.add(new JSONObject("{\"static\":\"gfx/invobjs/wildgoathorn\",\"name\":\"Wildgoat Horn\"}"));
        categories.put("Goat Horn", goatHorn);

        ArrayList<JSONObject> carrot = new ArrayList<>();
        carrot.add(new JSONObject("{\"static\":\"gfx/invobjs/carrot\",\"name\":\"Carrot\"}"));
        carrot.add(new JSONObject("{\"static\":\"gfx/invobjs/pickledcarrot\",\"name\":\"Pickled Carrot\"}"));
        categories.put("Carrot", carrot);

        ArrayList<JSONObject> beetroot = new ArrayList<>();
        beetroot.add(new JSONObject("{\"static\":\"gfx/invobjs/beet\",\"name\":\"Beetroot\"}"));
        beetroot.add(new JSONObject("{\"static\":\"gfx/invobjs/pickledbeet\",\"name\":\"Pickled Beetroot\"}"));
        categories.put("Beetroot", beetroot);

        ArrayList<JSONObject> cucumber = new ArrayList<>();
        cucumber.add(new JSONObject("{\"static\":\"gfx/invobjs/small/cucumber\",\"name\":\"Cucumber\"}"));
        cucumber.add(new JSONObject("{\"static\":\"gfx/invobjs/pickledcucumber\",\"name\":\"Pickled Cucumber\"}"));
        categories.put("Cucumber", cucumber);

//        ArrayList<JSONObject> egg = new ArrayList<>();
//        egg.add(new JSONObject("{\"static\":\"gfx/invobjs/egg-chicken-boiled\",\"name\":\"Boiled Egg\"}"));
//        egg.add(new JSONObject("{\"static\":\"gfx/invobjs/pickledegg\",\"name\":\"Pickled Egg\"}"));
//        categories.put("Egg", egg);

        ArrayList<JSONObject> herring = new ArrayList<>();
        herring.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-herring\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Herring\"}"));
        herring.add(new JSONObject("{\"static\":\"gfx/invobjs/pickledherring\",\"name\":\"Pickled Herring\"}"));
        categories.put("Herring", herring);

        ArrayList<JSONObject> olives = new ArrayList<>();
        olives.add(new JSONObject("{\"static\":\"gfx/invobjs/olive\",\"name\":\"Olive\"}"));
        olives.add(new JSONObject("{\"static\":\"gfx/invobjs/pickledolive\",\"name\":\"Pickled Olive\"}"));
        categories.put("Olive", olives);

        ArrayList<JSONObject> finebones = new ArrayList<>();
        finebones.addAll(goatHorn);
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/beartooth\",\"name\":\"Bear Tooth\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/boartusk\",\"name\":\"Boar Tusk\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/bogturtleshell\",\"name\":\"Bog Turtle Shell\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/borewormbeak\",\"name\":\"Boreworm Beak\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/spermwhaletooth\",\"name\":\"Cachalot Tooth\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/flipperbone\",\"name\":\"Flipper Bones\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/lynxclaws\",\"name\":\"Lynx Claws\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/molepaw\",\"name\":\"Mole's Pawbone\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/orcatooth\",\"name\":\"Orca Tooth\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/antlers-reddeer\",\"name\":\"Red Deer Antlers\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/antlers-reindeer\",\"name\":\"Reindeer Antlers\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/antlers-roedeer\",\"name\":\"Roe Deer Antlers\"}"));
        finebones.add(new JSONObject("{\"static\":\"gfx/invobjs/wolfclaw\",\"name\":\"Wolf's Claw\"}"));
        categories.put("Finebone", finebones);

        ArrayList<JSONObject> boneMaterials = new ArrayList<>();
        boneMaterials.addAll(finebones);
        boneMaterials.addAll(chitinItems);
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/bone\",\"name\":\"Bone Material\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/adderskeleton\",\"name\":\"Adder Skeleton\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/crabshell\",\"name\":\"Crabshell\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/mammothtusk\",\"name\":\"Mammoth Tusk\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/antlers-moose\",\"name\":\"Moose Antlers\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/trollskull\",\"name\":\"Troll Skull\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/trolltusks\",\"name\":\"Troll Tusks\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/trollbone\",\"name\":\"Trollbone\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/walrustusk\",\"name\":\"Walrus Tusk\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/whalebone\",\"name\":\"Whale Bone Material\"}"));
        boneMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/wishbone\",\"name\":\"Wishbone\"}"));
        categories.put("Bone Material", boneMaterials);

        ArrayList<JSONObject> cleanedBirds = new ArrayList<>();
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/bullfinch-cleaned\",\"name\":\"Cleaned Bullfinch\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/chicken-cleaned\",\"name\":\"Cleaned Chicken\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/goldeneagle-cleaned\",\"name\":\"Cleaned Golden Eagle\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/magpie-cleaned\",\"name\":\"Cleaned Magpie\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/mallard-cleaned\",\"name\":\"Cleaned Mallard\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/pelican-cleaned\",\"name\":\"Cleaned Pelican\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/ptarmigan-cleaned\",\"name\":\"Cleaned Ptarmigan\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/quail-cleaned\",\"name\":\"Cleaned Quail\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/rockdove-cleaned\",\"name\":\"Cleaned Rock Dove\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/seagull-cleaned\",\"name\":\"Cleaned Seagull\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/swan-cleaned\",\"name\":\"Cleaned Swan\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/woodgrouse-m-cleaned\",\"name\":\"Cleaned Wood Grouse Cock\"}"));
        cleanedBirds.add(new JSONObject("{\"static\":\"gfx/invobjs/woodgrouse-f-cleaned\",\"name\":\"Cleaned Wood Grouse Hen\"}"));
        categories.put("Clean Bird Carcass", cleanedBirds);

        ArrayList<JSONObject> mollusksAndSnails = new ArrayList<>();
        mollusksAndSnails.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/goosebarnacle\",\"name\":\"Gooseneck Barnacle\"}"));
        mollusksAndSnails.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/lakesnail\",\"name\":\"Lake Snail\"}"));
        mollusksAndSnails.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/oyster\",\"name\":\"Oyster\"}"));
        mollusksAndSnails.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/razorclams\",\"name\":\"Razor Clam\"}"));
        mollusksAndSnails.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/mussels\",\"name\":\"River Pearl Mussel\"}"));
        mollusksAndSnails.add(new JSONObject("{\"static\":\"gfx/invobjs/roundclam\",\"name\":\"Round Clam\"}"));
        categories.put("Edible Seashell", mollusksAndSnails);

        ArrayList<JSONObject> poultry = new ArrayList<>();
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-chicken\"], \"name\": \"Chicken Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-eagleowl\"], \"name\": \"Eagle Owl Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-goldeneagle\"], \"name\": \"Golden Eagle Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-magpie\"], \"name\": \"Magpie Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-mallard\"], \"name\": \"Mallard Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-bullfinch\"], \"name\": \"Mammoth Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-pelican\"], \"name\": \"Pelican Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-ptarmigan\"], \"name\": \"Ptarmigan Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-quail\"], \"name\": \"Quail Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-rockdove\"], \"name\": \"Rock Dove Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-seagull\"], \"name\": \"Seagull Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-swan\"], \"name\": \"Swan Meat\"}"));
        poultry.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-poultry\", \"gfx/invobjs/meat-woodgrouse\"], \"name\": \"Wood Grouse Meat\"}"));
        categories.put("Poultry", poultry);

        ArrayList<JSONObject> weirdMeat = new ArrayList<>();
        weirdMeat.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-weird\", \"gfx/invobjs/meat-troll\"], \"name\": \"Troll Meat\"}"));
        weirdMeat.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-weird\", \"gfx/invobjs/meat-bee\"], \"name\": \"Bee Meat\"}"));
        weirdMeat.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-weird\", \"gfx/invobjs/meat-boreworm\"], \"name\": \"Boreworm Meat\"}"));
        weirdMeat.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-weird\", \"gfx/invobjs/meat-ant\"], \"name\": \"Ant Meat\"}"));
        weirdMeat.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-weird\", \"gfx/invobjs/meat-cavelouse\"], \"name\": \"Cave Louse Meat\"}"));
        weirdMeat.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-weird\", \"gfx/invobjs/meat-chasmconch\"], \"name\": \"Chasm Conch Meat\"}"));
        categories.put(" Meat", weirdMeat);

        ArrayList<JSONObject> bollock = new ArrayList<>();
        bollock.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-testis\", \"gfx/invobjs/meat-horse\"], \"name\": \"Horse Bollock\"}"));
        bollock.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-testis\", \"gfx/invobjs/meat-sheep\"], \"name\": \"Mutton Bollock\"}"));
        bollock.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-testis\", \"gfx/invobjs/meat-goat\"], \"name\": \"Chevon Bollock\"}"));
        bollock.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-testis\", \"gfx/invobjs/meat-pig\"], \"name\": \"Pork Bollock\"}"));
        bollock.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-testis\", \"gfx/invobjs/meat-cow\"], \"name\": \"Beef Bollock\"}"));
        categories.put("Bollock", bollock);

        ArrayList<JSONObject> fishFilet = new ArrayList<>();
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-abyssgazer\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Abyss Gazer\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-asp\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Asp\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-bass\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Bass\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-bream\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Bream\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-brill\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Brill\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-burbot\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Burbot\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-carp\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Carp\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-catfish\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Catfish\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-caveangler\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Cave Angler\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-cavesculpin\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Cave Sculpin\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-cavelacanth\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Cavelacanth\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-chub\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Chub\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-cod\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Cod\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-eel\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Eel\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-grayling\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Grayling\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-haddock\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Haddock\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-herring\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Herring\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-ide\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Ide\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-lavaret\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Lavaret\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-mackerel\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Mackerel\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-mullet\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Mullet\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-paleghostfish\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Pale Ghostfish\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-perch\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Perch\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-pike\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Pike\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-plaice\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Plaice\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-pomfret\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Pomfret\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-roach\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Roach\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-rosefish\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Rose Fish\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-ruffe\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Ruffe\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-saithe\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Saithe\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-salmon\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Salmon\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-silverbream\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Silver Bream\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-smelt\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Smelt\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-sturgeon\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Sturgeon\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-tench\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Tench\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-trout\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Trout\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-whiting\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Whiting\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-zander\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Zander\"}"));
        fishFilet.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-zope\",\"gfx/invobjs/meat-filet\"],\"name\":\"Filet of Zope\"}"));
        categories.put("Filet of ", fishFilet);

        ArrayList<JSONObject> rawChevon = new ArrayList<>();
        rawChevon.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-goat\"], \"name\": \"Raw Chevon\"}"));
        rawChevon.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-wildgoat\"], \"name\": \"Raw Wildgoat\"}"));
        categories.put("Raw Chevon", rawChevon);

        ArrayList<JSONObject> rawBeef = new ArrayList<>();
        rawBeef.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-cow\"], \"name\": \"Raw Beef\"}"));
        rawBeef.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-aurochs\"], \"name\": \"Raw Wild Beef\"}"));
        categories.put("Raw Beef", rawBeef);

        ArrayList<JSONObject> rawMutton = new ArrayList<>();
        rawMutton.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-sheep\"], \"name\": \"Raw Mutton\"}"));
        rawMutton.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-mouflon\"], \"name\": \"Raw Wild Mutton\"}"));
        categories.put("Raw Mutton", rawMutton);

        ArrayList<JSONObject> rawPork = new ArrayList<>();
        rawPork.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-pig\"], \"name\": \"Raw Pork\"}"));
        rawPork.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-boar\"], \"name\": \"Raw Wild Pork\"}"));
        categories.put("Raw Pork", rawPork);

        ArrayList<JSONObject> rawHorse = new ArrayList<>();
        rawHorse.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-horse\"], \"name\": \"Raw Horse\"}"));
        rawHorse.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-wildhorse\"], \"name\": \"Raw Wildhorse\"}"));
        categories.put("Raw Horse", rawHorse);

        ArrayList<JSONObject> raw = new ArrayList<>();
        raw.addAll(rawChevon);
        raw.addAll(rawBeef);
        raw.addAll(rawMutton);
        raw.addAll(rawPork);
        raw.addAll(rawHorse);
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-mammoth\"], \"name\": \"Mammoth Meat\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-adder\"], \"name\": \"Raw Adder\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-badger\"], \"name\": \"Raw Badger\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-bat\"], \"name\": \"Raw Bat\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-bear\"], \"name\": \"Raw Bear\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-beaver\"], \"name\": \"Raw Beaver\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-bogturtle\"], \"name\": \"Raw Bog Turtle\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-spermwhale\"], \"name\": \"Raw Cachalot\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-caverat\"], \"name\": \"Raw Caverat\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-fox\"], \"name\": \"Raw Fox\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-greyseal\"], \"name\": \"Raw Grey Seal\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-hedgehog\"], \"name\": \"Raw Hedgehog\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-lynx\"], \"name\": \"Raw Lynx\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-mole\"], \"name\": \"Raw Mole\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-moose\"], \"name\": \"Raw Moose\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-orca\"], \"name\": \"Raw Orca\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-otter\"], \"name\": \"Raw Otter\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-rabbit\"], \"name\": \"Raw Rabbit\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-reindeer\"], \"name\": \"Raw Reindeer Venison\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-roedeer\"], \"name\": \"Raw Roe Venison\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-squirrel\"], \"name\": \"Raw Squirrel\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-stoat\"], \"name\": \"Raw Stoat\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-reddeer\"], \"name\": \"Raw Venison\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-walrus\"], \"name\": \"Raw Walrus\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-wolf\"], \"name\": \"Raw Wolf\"}"));
        raw.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-raw\", \"gfx/invobjs/meat-wolverine\"], \"name\": \"Raw Wolverine\"}"));
        categories.put("Raw ", raw);

        ArrayList<JSONObject> crust = new ArrayList<>();
        crust.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-crust\", \"gfx/invobjs/meat-crab\"], \"name\": \"Raw Crab\"}"));
        crust.add(new JSONObject("{\"layer\": [\"gfx/invobjs/meat-crust\", \"gfx/invobjs/meat-lobster\"], \"name\": \"Raw Lobster\"}"));
        categories.put("Crab Meat", crust);

        ArrayList<JSONObject> rawMeat = new ArrayList<>();
        rawMeat.addAll(raw);
        rawMeat.addAll(fishFilet);
        rawMeat.addAll(poultry);
        rawMeat.addAll(weirdMeat);
        rawMeat.addAll(bollock);
        rawMeat.addAll(crust);
        categories.put("Raw Meat", rawMeat);

        ArrayList<JSONObject> candles = new ArrayList<>();
        candles.add(new JSONObject("{\"static\":\"gfx/invobjs/tallowcandle\",\"name\":\"Tallow Candle\"}"));
        candles.add(new JSONObject("{\"static\":\"gfx/invobjs/beeswaxcandle\",\"name\":\"Wax Candle\"}"));
        categories.put("Candle", candles);

        ArrayList<JSONObject> pearls = new ArrayList<>();
        pearls.add(new JSONObject("{\"static\":\"gfx/invobjs/oysterpearl\",\"name\":\"Oyster Pearl\"}"));
        pearls.add(new JSONObject("{\"static\":\"gfx/invobjs/riverpearl\",\"name\":\"River Pearl\"}"));
        categories.put("Pearl", pearls);

        ArrayList<JSONObject> wool = new ArrayList<>();
        wool.add(new JSONObject("{\"static\":\"gfx/invobjs/wool-sheep\",\"name\":\"Wool\"}"));
        wool.add(new JSONObject("{\"static\":\"gfx/invobjs/wool-goat\",\"name\":\"Mohair\"}"));
        categories.put("Wool", wool);

        ArrayList<JSONObject> feathers = new ArrayList<>();
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-bullfinch\",\"name\":\"Bullfinch Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-chicken\",\"name\":\"Chicken Feathers\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-eagleowl\",\"name\":\"Eagle Owl Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-goldeneagle\",\"name\":\"Golden Eagle Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-magpie\",\"name\":\"Magpie Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-mallard\",\"name\":\"Mallard Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-pelican\",\"name\":\"Pelican Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-ptarmigan\",\"name\":\"Ptarmigan Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-quail\",\"name\":\"Quail Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-rockdove\",\"name\":\"Rock Dove Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-seagull\",\"name\":\"Seagull Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-swan\",\"name\":\"Swan Feather\"}"));
        feathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-woodgrouse\",\"name\":\"Wood Grouse Feather\"}"));
        categories.put("Feather", feathers);

        ArrayList<JSONObject> fineFeathers = new ArrayList<>();
        fineFeathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-eagleowl\",\"name\":\"Eagle Owl Feather\"}"));
        fineFeathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-goldeneagle\",\"name\":\"Golden Eagle Feather\"}"));
        fineFeathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-mallard\",\"name\":\"Mallard Feather\"}"));
        fineFeathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-quail\",\"name\":\"Quail Feather\"}"));
        fineFeathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-swan\",\"name\":\"Swan Feather\"}"));
        fineFeathers.add(new JSONObject("{\"static\":\"gfx/invobjs/feather-woodgrouse\",\"name\":\"Wood Grouse Feather\"}"));
        categories.put("Fine Feather", fineFeathers);

        ArrayList<JSONObject> finerPlantFibres = new ArrayList<>();
        finerPlantFibres.add(new JSONObject("{\"static\":\"gfx/invobjs/flaxfibre\",\"name\":\"Flax Fibre\"}"));
        finerPlantFibres.add(new JSONObject("{\"static\":\"gfx/invobjs/hempfibre\",\"name\":\"Hemp Fibre\"}"));
        categories.put("Finer Plant Fibre", finerPlantFibres);

        ArrayList<JSONObject> wickerItems = new ArrayList<>();
        wickerItems.add(new JSONObject("{\"static\":\"gfx/invobjs/branch\",\"name\":\"Branch\"}"));
        wickerItems.add(new JSONObject("{\"static\":\"gfx/invobjs/branch-olive\",\"name\":\"Olive Branch\"}"));
        wickerItems.add(new JSONObject("{\"static\":\"gfx/invobjs/strawstring\",\"name\":\"Straw Twine\"}"));
        wickerItems.add(new JSONObject("{\"static\":\"gfx/invobjs/toughroot\",\"name\":\"Tough Root\"}"));
        categories.put("Wicker", wickerItems);

        ArrayList<JSONObject> clothItems = new ArrayList<>();
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/erminecloth\",\"name\":\"Ermine Cloth\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/felt\",\"name\":\"Felt\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/goldcloth\",\"name\":\"Golden Cloth\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/hempcloth\",\"name\":\"Hemp Cloth\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/leatherfabric\",\"name\":\"Leather Fabric\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/linencloth\",\"name\":\"Linen Cloth\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/mohaircloth\",\"name\":\"Mohair Cloth\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/primcloth\",\"name\":\"Primitive Cloth\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/silkcloth\",\"name\":\"Silk Cloth\"}"));
        clothItems.add(new JSONObject("{\"static\":\"gfx/invobjs/woolcloth\",\"name\":\"Wool Cloth\"}"));
        categories.put("Cloth", clothItems);

        ArrayList<JSONObject> preciousMetalNuggets = new ArrayList<>();
        preciousMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/goldegg\",\"name\":\"Gold Egg\"}"));
        preciousMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-gold\",\"name\":\"Gold Nugget\"}"));
        preciousMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-rosegold\",\"name\":\"Rose Gold Nugget\"}"));
        preciousMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-silver\",\"name\":\"Silver Nugget\"}"));
        categories.put("Nugget of a Precious Metal", preciousMetalNuggets);

        ArrayList<JSONObject> coal = new ArrayList<>();
        coal.add(new JSONObject("{\"static\":\"gfx/invobjs/coal\",\"name\":\"Coal\"}"));
        coal.add(new JSONObject("{\"static\":\"gfx/invobjs/blackcoal\",\"name\":\"Black Coal\"}"));
        categories.put("Coal", coal);

        ArrayList<JSONObject> hardMetalBars = new ArrayList<>();
        hardMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-bronze\",\"name\":\"Bar of Bronze\"}"));
        hardMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-castiron\",\"name\":\"Bar of Cast Iron\"}"));
//        hardMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-meteoriciron\",\"name\":\"Bar of Meteoric Iron\"}"));
        hardMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-steel\",\"name\":\"Bar of Steel\"}"));
        hardMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-wroughtiron\",\"name\":\"Bar of Wrought Iron\"}"));
        categories.put("Bar of Bronze, Iron or Steel", hardMetalBars);

        ArrayList<JSONObject> hardMetalNuggets = new ArrayList<>();
        hardMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-bronze\",\"name\":\"Bronze Nugget\"}"));
        hardMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-castiron\",\"name\":\"Cast Iron Nugget\"}"));
//        hardMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-meteoriciron\",\"name\":\"Meteoric Iron Nugget\"}"));
        hardMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-steel\",\"name\":\"Steel Nugget\"}"));
        hardMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-wroughtiron\",\"name\":\"Wrought Iron Nugget\"}"));
        categories.put("Nugget of Bronze, Iron or Steel", hardMetalNuggets);

        ArrayList<JSONObject> commonMetalNuggets = new ArrayList<>();
        commonMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-bronze\",\"name\":\"Bronze Nugget\"}"));
        commonMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-castiron\",\"name\":\"Cast Iron Nugget\"}"));
        commonMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-copper\",\"name\":\"Copper Nugget\"}"));
        commonMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-lead\",\"name\":\"Lead Nugget\"}"));
//        commonMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-meteoriciron\",\"name\":\"Meteoric Iron Nugget\"}"));
        commonMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-steel\",\"name\":\"Steel Nugget\"}"));
        commonMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-tin\",\"name\":\"Tin Nugget\"}"));
        commonMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-wroughtiron\",\"name\":\"Wrought Iron Nugget\"}"));
        categories.put("Nugget of Any Common Metal", commonMetalNuggets);

        ArrayList<JSONObject> commonMetalBars = new ArrayList<>();
        commonMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-bronze\",\"name\":\"Bar of Bronze\"}"));
        commonMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-castiron\",\"name\":\"Bar of Cast Iron\"}"));
        commonMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-copper\",\"name\":\"Bar of Copper\"}"));
        commonMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-lead\",\"name\":\"Bar of Lead\"}"));
//        commonMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-meteoriciron\",\"name\":\"Bar of Meteoric Iron\"}"));
        commonMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-steel\",\"name\":\"Bar of Steel\"}"));
        commonMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-tin\",\"name\":\"Bar of Tin\"}"));
        commonMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-wroughtiron\",\"name\":\"Bar of Wrought Iron\"}"));
        categories.put("Bar of Any Common Metal", commonMetalBars);

        ArrayList<JSONObject> anyMetalBars = new ArrayList<>();
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-bronze\",\"name\":\"Bar of Bronze\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-castiron\",\"name\":\"Bar of Cast Iron\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-copper\",\"name\":\"Bar of Copper\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-gold\",\"name\":\"Bar of Gold\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-lead\",\"name\":\"Bar of Lead\"}"));
//        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-meteoriciron\",\"name\":\"Bar of Meteoric Iron\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-rosegold\",\"name\":\"Bar of Rose Gold\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-silver\",\"name\":\"Bar of Silver\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-steel\",\"name\":\"Bar of Steel\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-tin\",\"name\":\"Bar of Tin\"}"));
        anyMetalBars.add(new JSONObject("{\"static\":\"gfx/invobjs/bar-wroughtiron\",\"name\":\"Bar of Wrought Iron\"}"));
        categories.put("Bar of Any Metal", anyMetalBars);

        ArrayList<JSONObject> anyMetalNuggets = new ArrayList<>();
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-bronze\",\"name\":\"Bronze Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-castiron\",\"name\":\"Cast Iron Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-copper\",\"name\":\"Copper Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/goldegg\",\"name\":\"Gold Egg\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-gold\",\"name\":\"Gold Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-lead\",\"name\":\"Lead Nugget\"}"));
//        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-meteoriciron\",\"name\":\"Meteoric Iron Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-rosegold\",\"name\":\"Rose Gold Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-silver\",\"name\":\"Silver Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-steel\",\"name\":\"Steel Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-tin\",\"name\":\"Tin Nugget\"}"));
        anyMetalNuggets.add(new JSONObject("{\"static\":\"gfx/invobjs/nugget-wroughtiron\",\"name\":\"Wrought Iron Nugget\"}"));
        categories.put("Nugget of Any Metal", anyMetalNuggets);

        ArrayList<JSONObject> waxTypes = new ArrayList<>();
        waxTypes.add(new JSONObject("{\"static\":\"gfx/invobjs/beeswax\",\"name\":\"Beeswax\"}"));
        waxTypes.add(new JSONObject("{\"static\":\"gfx/invobjs/candleberrywax\",\"name\":\"Candleberry Wax\"}"));
        waxTypes.add(new JSONObject("{\"static\":\"gfx/invobjs/spermaceti\",\"name\":\"Hvalraf\"}"));
        categories.put("Wax", waxTypes);

        ArrayList<JSONObject> pigments = new ArrayList<>();
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorbeige\",\"name\":\"Beige Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorblack\",\"name\":\"Black Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorblue\",\"name\":\"Blue Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorbrown\",\"name\":\"Brown Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorgray\",\"name\":\"Gray Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorgreen\",\"name\":\"Green Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorlime\",\"name\":\"Lime Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colororange\",\"name\":\"Orange Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorpink\",\"name\":\"Pink Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorpurple\",\"name\":\"Purple Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorred\",\"name\":\"Red Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorteal\",\"name\":\"Teal Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorturquoise\",\"name\":\"Turquoise Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/colorwhite\",\"name\":\"White Pigment\"}"));
        pigments.add(new JSONObject("{\"static\":\"gfx/invobjs/coloryellow\",\"name\":\"Yellow Pigment\"}"));
        categories.put("Pigment", pigments);

        ArrayList<JSONObject> fineClays = new ArrayList<>();
        fineClays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-pit\",\"name\":\"Pit Clay\"}"));
        fineClays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-soap\",\"name\":\"Soap Clay\"}"));
        categories.put("Fine Clay", fineClays);

        ArrayList<JSONObject> anyBricks = new ArrayList<>();
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-ball\",\"name\":\"Brick\"}"));
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-acre\",\"name\":\"Acre Brick\"}"));
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-gray\",\"name\":\"Gray Brick\"}"));
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-cave\",\"name\":\"Cave Brick\"}"));
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-bone\",\"name\":\"Bone Brick\"}"));
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-pit\",\"name\":\"Pit Brick\"}"));
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-potters\",\"name\":\"Potter's Brick\"}"));
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-soap\",\"name\":\"Soap Brick\"}"));
        anyBricks.add(new JSONObject("{\"static\":\"gfx/invobjs/brick-coade\",\"name\":\"Coade Stone Brick\"}"));
        categories.put("Any Brick", anyBricks);

        ArrayList<JSONObject> clays = new ArrayList<>();
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-acre\",\"name\":\"Acre Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-ball\",\"name\":\"Ball Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-sea\",\"name\":\"Sea Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-gray\",\"name\":\"Gray Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-cave\",\"name\":\"Cave Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-pit\",\"name\":\"Pit Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-soap\",\"name\":\"Soap Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-coade\",\"name\":\"Coade Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-bone\",\"name\":\"Bone Clay\"}"));
        clays.add(new JSONObject("{\"static\":\"gfx/invobjs/clay-potters\",\"name\":\"Potter's Clay\"}"));
        categories.put("Clay", clays);

        ArrayList<JSONObject> castingMaterials = new ArrayList<>();
        castingMaterials.addAll(clays);
        castingMaterials.add(new JSONObject("{\"static\":\"gfx/invobjs/sand\",\"name\":\"Sand\"}"));
        categories.put("Casting Material", castingMaterials);

        ArrayList<JSONObject> glass = new ArrayList<>();
        glass.add(new JSONObject("{\"static\":\"gfx/invobjs/moltenglass\",\"name\":\"Molten Glass\"}"));
        glass.add(new JSONObject("{\"static\":\"gfx/invobjs/rawglass\",\"name\":\"Raw Glass\"}"));
        categories.put("Glass", glass);

        ArrayList<JSONObject> hidesFresh = new ArrayList<>();
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/adderhide-blood\",\"name\":\"Fresh Adder Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/aurochshide-blood\",\"name\":\"Fresh Aurochs Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/badgerhide-blood\",\"name\":\"Fresh Badger Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/bathide-blood\",\"name\":\"Fresh Bat Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/bearhide-blood\",\"name\":\"Fresh Bear Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/beaverhide-blood\",\"name\":\"Fresh Beaver Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/boarhide-blood\",\"name\":\"Fresh Boarhide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/borewormhide-blood\",\"name\":\"Fresh Boreworm Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/cowhide-blood\",\"name\":\"Fresh Cattle Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/caveanglerscales-raw\",\"name\":\"Fresh Cave Angler Scales\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/caverathide-blood\",\"name\":\"Fresh Caverat Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/stoathide-winter-blood\",\"name\":\"Fresh Winter Stoat Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/goathide-blood\",\"name\":\"Fresh Goat Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/greysealhide-blood\",\"name\":\"Fresh Grey Seal Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/hedgehoghide-blood\",\"name\":\"Fresh Hedgehog Skin\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/horsehide-blood\",\"name\":\"Fresh Horse Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/lynxhide-blood\",\"name\":\"Fresh Lynx Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/mammothhide-blood\",\"name\":\"Fresh Mammoth Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/molehide-blood\",\"name\":\"Fresh Mole Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/moosehide-blood\",\"name\":\"Fresh Moose Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/mouflonhide-blood\",\"name\":\"Fresh Mouflon Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/otterhide-blood\",\"name\":\"Fresh Otterhide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/pighide-blood\",\"name\":\"Fresh Pig Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/rabbithide-blood\",\"name\":\"Fresh Rabbit Fur\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/reddeerhide-blood\",\"name\":\"Fresh Red Deer Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/reindeerhide-blood\",\"name\":\"Fresh Reindeer Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/roedeerhide-blood\",\"name\":\"Fresh Roe Deer Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/sheephide-blood\",\"name\":\"Fresh Sheepskin\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/squirrelhide-blood\",\"name\":\"Fresh Squirrel Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/squirreltail-blood\",\"name\":\"Fresh Squirrel Tail\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/stoathide-blood\",\"name\":\"Fresh Stoat Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/trollhide-blood\",\"name\":\"Fresh Troll Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/walrushide-blood\",\"name\":\"Fresh Walrus Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/wildgoathide-blood\",\"name\":\"Fresh Wildgoat Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/wildhorsehide-blood\",\"name\":\"Fresh Wildhorse Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/wolfhide-blood\",\"name\":\"Fresh Wolf Hide\"}"));
        hidesFresh.add(new JSONObject("{\"static\":\"gfx/invobjs/wolverinehide-blood\",\"name\":\"Fresh Wolverine Hide\"}"));
        categories.put("Hide Fresh", hidesFresh);

        ArrayList<JSONObject> preparedAnimalHides = new ArrayList<>();
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/adderhide\",\"name\":\"Adder Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/aurochshide\",\"name\":\"Aurochs Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/badgerhide\",\"name\":\"Badger Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/bathide\",\"name\":\"Bat Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/bearhide\",\"name\":\"Bear Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/beaverhide\",\"name\":\"Beaver Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/boarhide\",\"name\":\"Boarhide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/borewormhide\",\"name\":\"Boreworm Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/cowhide\",\"name\":\"Cattle Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/caveanglerscales\",\"name\":\"Cave Angler Scales\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/caverathide\",\"name\":\"Caverat Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/stoathide-winter\",\"name\":\"Ermine\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/foxhide\",\"name\":\"Fox Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/goathide\",\"name\":\"Goat Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/greysealhide\",\"name\":\"Grey Seal Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/hedgehoghide\",\"name\":\"Hedgehog Skin\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/horsehide\",\"name\":\"Horse Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/lynxhide\",\"name\":\"Lynx Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/mammothhide\",\"name\":\"Mammoth Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/molehide\",\"name\":\"Mole Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/moosehide\",\"name\":\"Moose Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/mouflonhide\",\"name\":\"Mouflon Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/otterhide\",\"name\":\"Otterhide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/pighide\",\"name\":\"Pigskin\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/rabbithide\",\"name\":\"Rabbit Fur\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/reddeerhide\",\"name\":\"Red Deer Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/reindeerhide\",\"name\":\"Reindeer Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/roedeerhide\",\"name\":\"Roe Deer Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/sheephide\",\"name\":\"Sheepskin\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/squirrelhide\",\"name\":\"Squirrel Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/squirreltail\",\"name\":\"Squirrel Tail\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/stoathide\",\"name\":\"Stoat Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/trollhide\",\"name\":\"Troll Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/walrushide\",\"name\":\"Walrus Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/wildgoathide\",\"name\":\"Wildgoat Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/wildhorsehide\",\"name\":\"Wildhorse Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/wolfhide\",\"name\":\"Wolf Hide\"}"));
        preparedAnimalHides.add(new JSONObject("{\"static\":\"gfx/invobjs/wolverinehide\",\"name\":\"Wolverine Hide\"}"));
        categories.put("Prepared Animal Hide", preparedAnimalHides);

        ArrayList<JSONObject> boards = new ArrayList<>();
        //boards.add(new JSONObject("{\"static\":\"gfx/invobjs/small/board\",\"name\":\"Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-acacia\",\"name\":\"Acacia Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-alder\",\"name\":\"Alder Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-almondtree\",\"name\":\"Almond Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-appletree\",\"name\":\"Apple Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-ash\",\"name\":\"Ash Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-aspen\",\"name\":\"Aspen Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-baywillow\",\"name\":\"Bay Willow Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-beech\",\"name\":\"Beech Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-birch\",\"name\":\"Birch Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-birdcherrytree\",\"name\":\"Birdcherry Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-blackpine\",\"name\":\"Black Pine Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-blackpoplar\",\"name\":\"Black Poplar Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-treeheath\",\"name\":\"Briarwood Board (Heath)\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-buckthorn\",\"name\":\"Buckthorn Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-carobtree\",\"name\":\"Carob Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-cedar\",\"name\":\"Cedar Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-charredtree\",\"name\":\"Charred Board (Valhalla)\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-chastetree\",\"name\":\"Chaste Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-checkertree\",\"name\":\"Checker Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-cherry\",\"name\":\"Cherry Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-chestnuttree\",\"name\":\"Chestnut Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-conkertree\",\"name\":\"Conker Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-corkoak\",\"name\":\"Cork Oak Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-crabappletree\",\"name\":\"Crabapple Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-cypress\",\"name\":\"Cypress Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-dogwood\",\"name\":\"Dogwood Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-dwarfpine\",\"name\":\"Dwarf Pine Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-elm\",\"name\":\"Elm Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-figtree\",\"name\":\"Fig Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-fir\",\"name\":\"Fir Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-gloomcap\",\"name\":\"Gloomcap Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-gnomeshat\",\"name\":\"Gnome's Hat Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-goldenchain\",\"name\":\"Golden-chain Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-grayalder\",\"name\":\"Gray Alder Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-hazel\",\"name\":\"Hazel Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-hornbeam\",\"name\":\"Hornbeam Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-juniper\",\"name\":\"Juniper Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-kingsoak\",\"name\":\"King's Oak Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-larch\",\"name\":\"Larch Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-laurel\",\"name\":\"Laurel Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-lemontree\",\"name\":\"Lemon Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-linden\",\"name\":\"Linden Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-lotetree\",\"name\":\"Lote Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-maple\",\"name\":\"Maple Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-mayflower\",\"name\":\"Mayflower Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-medlartree\",\"name\":\"Medlar Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-moundtree\",\"name\":\"Mound Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-mulberry\",\"name\":\"Mulberry Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-oak\",\"name\":\"Oak Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-olivetree\",\"name\":\"Olive Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-orangetree\",\"name\":\"Orange Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-osier\",\"name\":\"Osier Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-peartree\",\"name\":\"Pear Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-persimmontree\",\"name\":\"Persimmon Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-pine\",\"name\":\"Pine Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-planetree\",\"name\":\"Plane Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-plumtree\",\"name\":\"Plum Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-poplar\",\"name\":\"Poplar Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-quincetree\",\"name\":\"Quince Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-rowan\",\"name\":\"Rowan Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-sallow\",\"name\":\"Sallow Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-silverfir\",\"name\":\"Silver Fir Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-sorbtree\",\"name\":\"Sorb Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-spruce\",\"name\":\"Spruce Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-stonepine\",\"name\":\"Stone Pine Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-sweetgum\",\"name\":\"Sweetgum Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-sycamore\",\"name\":\"Sycamore Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-tamarisk\",\"name\":\"Tamarisk Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-terebinth\",\"name\":\"Terebinth Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-towercap\",\"name\":\"Towercap Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-trombonechantrelle\",\"name\":\"Trumpet Chantrelle Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-walnuttree\",\"name\":\"Walnut Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-wartybirch\",\"name\":\"Warty Birch Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-whitebeam\",\"name\":\"Whitebeam Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-willow\",\"name\":\"Willow Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-strawberrytree\",\"name\":\"Wood Strawberry Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-wychelm\",\"name\":\"Wych Elm Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-yew\",\"name\":\"Yew Board\"}"));
        boards.add(new JSONObject("{\"static\":\"gfx/invobjs/board-zelkova\",\"name\":\"Zelkova Board\"}"));
        categories.put("Board", boards);

        ArrayList<JSONObject> blocks = new ArrayList<>();
        //blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-driftwood\",\"name\":\"Beaver Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-charredtree\",\"name\":\"Charred Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-driftwood\",\"name\":\"Driftwood Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-mirkwood\",\"name\":\"Mirkwood Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-blackthorn\",\"name\":\"Blackthorn Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-boxwood\",\"name\":\"Boxwood Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-crampbark\",\"name\":\"Crampbark Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-elderberrybush\",\"name\":\"Elderberry Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-woodbine\",\"name\":\"Woodbine Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-gooseberrybush\",\"name\":\"Gooseberry Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-hawthorn\",\"name\":\"Hawthorn Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-hoarwithy\",\"name\":\"Hoarwithy Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-mastic\",\"name\":\"Mastic Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-spindlebush\",\"name\":\"Spindlewood Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-teabush\",\"name\":\"Tea Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-witherstand\",\"name\":\"Witherstand Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-acacia\",\"name\":\"Acacia Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-alder\",\"name\":\"Alder Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-almondtree\",\"name\":\"Almond Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-appletree\",\"name\":\"Apple Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-ash\",\"name\":\"Ash Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-aspen\",\"name\":\"Aspen Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-baywillow\",\"name\":\"Bay Willow Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-beech\",\"name\":\"Beech Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-birch\",\"name\":\"Birch Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-birdcherrytree\",\"name\":\"Bird Cherry Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-blackpine\",\"name\":\"Black Pine Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-blackpoplar\",\"name\":\"Black Poplar Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-treeheath\",\"name\":\"Tree Heath Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-buckthorn\",\"name\":\"Buckthorn Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-carobtree\",\"name\":\"Carob Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-cedar\",\"name\":\"Cedar Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-chastetree\",\"name\":\"Chaste Tree Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-checkertree\",\"name\":\"Checker Tree Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-cherry\",\"name\":\"Cherry Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-chestnuttree\",\"name\":\"Chestnut Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-conkertree\",\"name\":\"Conker Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-corkoak\",\"name\":\"Cork Oak Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-crabappletree\",\"name\":\"Crabapple Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-cypress\",\"name\":\"Cypress Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-dogwood\",\"name\":\"Dogwood Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-dwarfpine\",\"name\":\"Dwarf Pine Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-elm\",\"name\":\"Elm Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-figtree\",\"name\":\"Fig Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-fir\",\"name\":\"Fir Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-gloomcap\",\"name\":\"Gloomcap Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-gnomeshat\",\"name\":\"Gnome's Hat Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-goldenchain\",\"name\":\"Golden Chain Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-grayalder\",\"name\":\"Gray Alder Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-hazel\",\"name\":\"Hazel Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-hornbeam\",\"name\":\"Hornbeam Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-juniper\",\"name\":\"Juniper Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-kingsoak\",\"name\":\"King's Oak Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-larch\",\"name\":\"Larch Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-laurel\",\"name\":\"Laurel Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-lemontree\",\"name\":\"Lemon Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-linden\",\"name\":\"Linden Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-lotetree\",\"name\":\"Lote Tree Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-maple\",\"name\":\"Maple Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-mayflower\",\"name\":\"Mayflower Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-medlartree\",\"name\":\"Medlar Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-moundtree\",\"name\":\"Mound Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-mulberry\",\"name\":\"Mulberry Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-oak\",\"name\":\"Oak Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-olivetree\",\"name\":\"Olive Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-orangetree\",\"name\":\"Orange Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-osier\",\"name\":\"Osier Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-peartree\",\"name\":\"Pear Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-persimmontree\",\"name\":\"Persimmon Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-pine\",\"name\":\"Pine Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-planetree\",\"name\":\"Plane Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-plumtree\",\"name\":\"Plum Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-poplar\",\"name\":\"Poplar Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-quincetree\",\"name\":\"Quince Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-rowan\",\"name\":\"Rowan Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-sallow\",\"name\":\"Sallow Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-silverfir\",\"name\":\"Silver Fir Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-sorbtree\",\"name\":\"Sorb Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-spruce\",\"name\":\"Spruce Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-stonepine\",\"name\":\"Stone Pine Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-sweetgum\",\"name\":\"Sweetgum Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-sycamore\",\"name\":\"Sycamore Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-tamarisk\",\"name\":\"Tamarisk Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-terebinth\",\"name\":\"Terebinth Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-towercap\",\"name\":\"Towercap Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-trombonechantrelle\",\"name\":\"Trombone Chantrelle Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-walnuttree\",\"name\":\"Walnut Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-wartybirch\",\"name\":\"Warty Birch Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-whitebeam\",\"name\":\"Whitebeam Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-willow\",\"name\":\"Willow Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-strawberrytree\",\"name\":\"Wood Strawberry Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-wychelm\",\"name\":\"Wych Elm Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-yew\",\"name\":\"Yew Block\"}"));
        blocks.add(new JSONObject("{\"static\":\"gfx/invobjs/wblock-zelkova\",\"name\":\"Zelkova Block\"}"));
        categories.put("Block of Wood", blocks);

        ArrayList<JSONObject> ore = new ArrayList<>();
        //ore.add(new JSONObject("{\"static\":\"gfx/invobjs/blackcoal\",\"name\":\"Black Coal\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/magnetite\",\"name\":\"Black Ore\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/hematite\",\"name\":\"Bloodstone\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/cassiterite\",\"name\":\"Cassiterite\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/chalcopyrite\",\"name\":\"Chalcopyrite\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/cinnabar\",\"name\":\"Cinnabar\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/petzite\",\"name\":\"Direvein Ore\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/galena\",\"name\":\"Galena\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/ilmenite\",\"name\":\"Heavy Earth Ore\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/hornsilver\",\"name\":\"Horn Silver\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/limonite\",\"name\":\"Iron Ochre Ore\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/leadglance\",\"name\":\"Lead Glance\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/nagyagite\",\"name\":\"Leaf Ore\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/malachite\",\"name\":\"Malachite\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/meteorite\",\"name\":\"Meteorite\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/peacockore\",\"name\":\"Peacock Ore\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/sylvanite\",\"name\":\"Schrifterz Ore\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/argentite\",\"name\":\"Silvershine Ore\"}"));
        ore.add(new JSONObject("{\"static\":\"gfx/invobjs/cuprite\",\"name\":\"Wine Glance\"}"));
        categories.put("Ore", ore);

        ArrayList<JSONObject> stones = new ArrayList<>();
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/alabaster\",\"name\":\"Alabaster\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/apatite\",\"name\":\"Apatite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/arkose\",\"name\":\"Arkose\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/basalt\",\"name\":\"Basalt\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/batrock\",\"name\":\"Bat Rock\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/blackcoal\",\"name\":\"Black Coal\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/magnetite\",\"name\":\"Magnetite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/hematite\",\"name\":\"Bloodstone\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/breccia\",\"name\":\"Breccia\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/cassiterite\",\"name\":\"Cassiterite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/catgold\",\"name\":\"Cat Gold\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/chalcopyrite\",\"name\":\"Chalcopyrite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/chert\",\"name\":\"Chert\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/cinnabar\",\"name\":\"Cinnabar\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/diabase\",\"name\":\"Diabase\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/diorite\",\"name\":\"Diorite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/petzite\",\"name\":\"Direvein Ore\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/dolomite\",\"name\":\"Dolomite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/dross\",\"name\":\"Dross\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/eclogite\",\"name\":\"Eclogite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/feldspar\",\"name\":\"Feldspar\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/flint\",\"name\":\"Flint\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/fluorospar\",\"name\":\"Fluorospar\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/gabbro\",\"name\":\"Gabbro\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/galena\",\"name\":\"Galena\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/gneiss\",\"name\":\"Gneiss\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/granite\",\"name\":\"Granite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/graywacke\",\"name\":\"Graywacke\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/greenschist\",\"name\":\"Greenschist\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/ilmenite\",\"name\":\"Heavy Earth Ore\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/hornsilver\",\"name\":\"Horn Silver\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/hornblende\",\"name\":\"Hornblende\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/limonite\",\"name\":\"Iron Ochre Ore\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/jasper\",\"name\":\"Jasper\"}"));
//        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/korundstone\",\"name\":\"Korund Stone\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/kyanite\",\"name\":\"Kyanite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/lavarock\",\"name\":\"Lava Rock\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/leadglance\",\"name\":\"Lead Glance\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/nagyagite\",\"name\":\"Leaf Ore\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/limestone\",\"name\":\"Limestone\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/malachite\",\"name\":\"Malachite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/marble\",\"name\":\"Marble\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/meteorite\",\"name\":\"Meteorite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/mica\",\"name\":\"Mica\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/microlite\",\"name\":\"Microlite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/obsidian\",\"name\":\"Obsidian\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/olivine\",\"name\":\"Olivine\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/orthoclase\",\"name\":\"Orthoclase\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/peacockore\",\"name\":\"Peacock Ore\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/pegmatite\",\"name\":\"Pegmatite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/porphyry\",\"name\":\"Porphyry\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/pumice\",\"name\":\"Pumice\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/quarryquartz\",\"name\":\"Quarry Quartz\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/quartz\",\"name\":\"Quartz\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/rhyolite\",\"name\":\"Rhyolite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/rockcrystal\",\"name\":\"Rock Crystal\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/sandstone\",\"name\":\"Sandstone\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/schist\",\"name\":\"Schist\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/sylvanite\",\"name\":\"Schrifterz Ore\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/serpentine\",\"name\":\"Serpentine\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/conchshard\",\"name\":\"Shard of Conch\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/argentite\",\"name\":\"Silvershine Ore\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/slag\",\"name\":\"Slag\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/slate\",\"name\":\"Slate\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/soapstone\",\"name\":\"Soapstone\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/sodalite\",\"name\":\"Sodalite\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/sunstone\",\"name\":\"Sunstone\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/cuprite\",\"name\":\"Wine Glance\"}"));
        stones.add(new JSONObject("{\"static\":\"gfx/invobjs/zincspar\",\"name\":\"Zincspar\"}"));
        categories.put("Stone", stones);

        ArrayList<JSONObject> seedsAndBerries = new ArrayList<>();
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackberrybush\",\"name\":\"Blackberry Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackcurrant\",\"name\":\"Blackcurrant Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/cherry\",\"name\":\"Cherry\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-dogrose\",\"name\":\"Dog Rose Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-elderberrybush\",\"name\":\"Elderberry Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-gooseberrybush\",\"name\":\"Gooseberry Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/mulberry\",\"name\":\"Mulberry\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-raspberrybush\",\"name\":\"Raspberry Bush Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-redcurrant\",\"name\":\"Redcurrant Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-sandthorn\",\"name\":\"Seaberry Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackthorn\",\"name\":\"Sloan Berries Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/woodstrawberry\",\"name\":\"Wood Strawberry\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/fig\",\"name\":\"Fig\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/grapes\",\"name\":\"Grapes\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/lemon\",\"name\":\"Lemon\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/medlar\",\"name\":\"Medlar\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/orange\",\"name\":\"Orange\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/pear\",\"name\":\"Pear\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/persimmon\",\"name\":\"Persimmon\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/plum\",\"name\":\"Plum\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/quince\",\"name\":\"Quince\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/apple\",\"name\":\"Red Apple\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-blackcurrant-yester\",\"name\":\"Yesteryear's Blackcurrant Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/cherry-yester\",\"name\":\"Yesteryear's Cherry\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-elderberrybush-yester\",\"name\":\"Yesteryear's Elderberry Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-gooseberrybush-yester\",\"name\":\"Yesteryear's Gooseberry Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/mulberry-yester\",\"name\":\"Yesteryear's Mulberry\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-raspberrybush-yester\",\"name\":\"Yesteryear's Raspberry Bush Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-redcurrant-yester\",\"name\":\"Yesteryear's Redcurrant Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/seed-sandthorn-yester\",\"name\":\"Yesteryear's Seaberry Seed\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/woodstrawberry-yester\",\"name\":\"Yesteryear's Wood Strawberry\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/fig-yester\",\"name\":\"Yesteryear's Fig\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/lemon-yester\",\"name\":\"Yesteryear's Lemon\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/medlar-yester\",\"name\":\"Yesteryear's Medlar\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/orange-yester\",\"name\":\"Yesteryear's Orange\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/pear-yester\",\"name\":\"Yesteryear's Pear\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/persimmon-yester\",\"name\":\"Yesteryear's Persimmon\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/plum-yester\",\"name\":\"Yesteryear's Plum\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/quince-yester\",\"name\":\"Yesteryear's Quince\"}"));
        seedsAndBerries.add(new JSONObject("{\"static\":\"gfx/invobjs/apple-yester\",\"name\":\"Yesteryear's Red Apple\"}"));
        categories.put("Seed of Tree or Bush", seedsAndBerries);

        ArrayList<JSONObject> freshWaterFish = new ArrayList<>();
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-asp\",\"name\":\"Asp\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-brill\",\"name\":\"Brill\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-bream\",\"name\":\"Bream\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-burbot\",\"name\":\"Burbot\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-catfish\",\"name\":\"Catfish\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-carp\",\"name\":\"Carp\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-chub\",\"name\":\"Chub\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-grayling\",\"name\":\"Grayling\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-ide\",\"name\":\"Ide\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-lavaret\",\"name\":\"Lavaret\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-perch\",\"name\":\"Perch\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-pike\",\"name\":\"Pike\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-plaice\",\"name\":\"Plaice\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-roach\",\"name\":\"Roach\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-ruffe\",\"name\":\"Ruffe\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-salmon\",\"name\":\"Salmon\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-smelt\",\"name\":\"Smelt\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-sturgeon\",\"name\":\"Sturgeon\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-tench\",\"name\":\"Tench\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-trout\",\"name\":\"Trout\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-silverbream\",\"name\":\"Silver Bream\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-zander\",\"name\":\"Zander\"}"));
        freshWaterFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-zope\",\"name\":\"Zope\"}"));
        categories.put("Fish Fresh Water", freshWaterFish);

        ArrayList<JSONObject> oceanFish = new ArrayList<>();
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-bass\",\"name\":\"Bass\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-cod\",\"name\":\"Cod\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-eel\",\"name\":\"Eel\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-haddock\",\"name\":\"Haddock\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-herring\",\"name\":\"Herring\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-mackerel\",\"name\":\"Mackerel\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-mullet\",\"name\":\"Mullet\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-pomfret\",\"name\":\"Pomfret\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-rosefish\",\"name\":\"Rose Fish\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-saithe\",\"name\":\"Saithe\"}"));
        //oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/seahorse\",\"name\":\"Seahorse\"}"));
        oceanFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-whiting\",\"name\":\"Whiting\"}"));
        categories.put("Fish Ocean", oceanFish);

        ArrayList<JSONObject> caveFish = new ArrayList<>();
        caveFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-abyssgazer\",\"name\":\"Abyss Gazer\"}"));
        caveFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-cavelacanth\",\"name\":\"Cavelacanth\"}"));
        caveFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-cavesculpin\",\"name\":\"Cave Sculpin\"}"));
        caveFish.add(new JSONObject("{\"static\":\"gfx/invobjs/fish-paleghostfish\",\"name\":\"Pale Ghostfish\"}"));
        categories.put("Fish Cave", caveFish);

        ArrayList<JSONObject> fish = new ArrayList<>();
        fish.addAll(freshWaterFish);
        fish.addAll(oceanFish);
        fish.addAll(caveFish);
        categories.put("Fish", fish);

        ArrayList<JSONObject> fishingLures = new ArrayList<>();
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-coppercomet\",\"name\":\"Copper Comet\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-copperbrushsnapper\",\"name\":\"Copperbrush Snapper\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-featherfly\",\"name\":\"Feather Fly\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-goldspoon\",\"name\":\"Gold Spoon-Lure\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-pineconeplug\",\"name\":\"Pinecone Plug\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-poppywobbler\",\"name\":\"Poppy Wobbler\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-rocklobster\",\"name\":\"Rock Lobster\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-steelbrush\",\"name\":\"Steelbrush Plunger\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-tinfly\",\"name\":\"Tin Fly\"}"));
        fishingLures.add(new JSONObject("{\"static\":\"gfx/invobjs/lure-woodfish\",\"name\":\"Woodfish\"}"));
        categories.put("Lures", fishingLures);

        ArrayList<JSONObject> fishingHooks = new ArrayList<>();
        fishingHooks.add(new JSONObject("{\"static\":\"gfx/invobjs/hook-bone\",\"name\":\"Bone Hook\"}"));
        fishingHooks.add(new JSONObject("{\"static\":\"gfx/invobjs/chitinhook\",\"name\":\"Chitin Hook\"}"));
        fishingHooks.add(new JSONObject("{\"static\":\"gfx/invobjs/hook-gold\",\"name\":\"Gold Hook\"}"));
        fishingHooks.add(new JSONObject("{\"static\":\"gfx/invobjs/hook-metal\",\"name\":\"Metal Hook\"}"));
        categories.put("Hooks", fishingHooks);

        ArrayList<JSONObject> driedFish = new ArrayList<>();
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-abyssgazer\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Abyss Gazer\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-asp\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Asp\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-bass\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Bass\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-bream\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Bream\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-brill\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Brill\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-burbot\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Burbot\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-carp\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Carp\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-catfish\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Catfish\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-caveangler\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Cave Angler\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-cavesculpin\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Cave Sculpin\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-cavelacanth\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Cavelacanth\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-chub\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Chub\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-cod\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Cod\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-eel\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Eel\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-grayling\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Grayling\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-haddock\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Haddock\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-herring\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Herring\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-ide\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Ide\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-lavaret\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Lavaret\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-mackerel\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Mackerel\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-mullet\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Mullet\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-paleghostfish\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Pale Ghostfish\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-perch\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Perch\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-pike\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Pike\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-plaice\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Plaice\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-pomfret\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Pomfret\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-roach\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Roach\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-rosefish\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Rose fish\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-ruffe\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Ruffe\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-saithe\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Saithe\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-salmon\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Salmon\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-silverbream\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Silver Bream\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-smelt\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Smelt\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-sturgeon\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Sturgeon\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-tench\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Tench\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-trout\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Trout\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-whiting\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Whiting\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-zander\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Zander\"}"));
        driedFish.add(new JSONObject("{\"layer\":[\"gfx/invobjs/meat-zope\",\"gfx/invobjs/meat-dfilet\"],\"name\":\"Dried Filet of Zope\"}"));
        categories.put("Dried Fish", driedFish);

        ArrayList<JSONObject> medicine = new ArrayList<>();
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/splint\",\"name\":\"Splint\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/ancientroot\",\"name\":\"Ancient Root\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/antpaste\",\"name\":\"Ant Paste\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/soapbar\",\"name\":\"Bar of Soap\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/camomilecompress\",\"name\":\"Camomile Compress\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/coldcompress\",\"name\":\"Cold Compress\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/coldcut\",\"name\":\"Cold Cut\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/gauze\",\"name\":\"Gauze\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/graygrease\",\"name\":\"Gray Grease\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/hartshornsalve\",\"name\":\"Hartshorn Salve\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/honeybroadaid\",\"name\":\"Honey Wayband\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/kelpcream\",\"name\":\"Kelp Cream\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/leech\",\"name\":\"Leech\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/mudointment\",\"name\":\"Mud Ointment\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/rootfill\",\"name\":\"Rootfill\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/jar-snakejuice\",\"name\":\"Snake Juice\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/stingingpoultice\",\"name\":\"Stinging Poultice\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/stitchpatch\",\"name\":\"Stitch Patch\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/toadbutter\",\"name\":\"Toad Butter\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/waybroad\",\"name\":\"Waybroad\"}"));
        medicine.add(new JSONObject("{\"static\":\"gfx/invobjs/herbs/yarrow\",\"name\":\"Yarrow\"}"));

        categories.put("Medicine", medicine);


    }

    public static NAlias getNamesInCategory(String categoryName) {
        ArrayList<JSONObject> categoryItems = categories.get(categoryName);

        if (categoryItems == null) {
            throw new IllegalArgumentException("Category not found: " + categoryName);
        }

        List<String> names = new ArrayList<>();
        for (JSONObject item : categoryItems) {
            String name = item.optString("name", "");
            if (!name.isEmpty()) {
                names.add(name);
            }
        }

        return new NAlias(names.toArray(new String[0]));
    }

    public static NAlias getAllFish()
    {
        NAlias res = new NAlias();
        for(JSONObject object : categories.get("Fish"))
        {
            res.keys.add(object.optString("name"));
        }
        res.exceptions.add("Filet");
        res.exceptions.add("Roe");
        return res;
    }

    public static HashMap<NStyle.Container, Integer> chest_state = new HashMap<>();

    static {
        chest_state.put(NStyle.Container.FREE, 3);
        chest_state.put(NStyle.Container.FULL, 28);
    }

    public static HashMap<NStyle.Container, Integer> jotun_state = new HashMap<>();

    static {
        jotun_state.put(NStyle.Container.FREE, 3);
        jotun_state.put(NStyle.Container.FULL, 112);
    }

    public static void checkLpExplorer(Gob clickedGob, String name) {
        if(clickedGob!=null) {
            if (clickedGob.ngob.name != null && object.containsKey(clickedGob.ngob.name)) {
                if (object.get(clickedGob.ngob.name).contains(name)) {
                    if (!NUtils.getGameUI().getCharInfo().IsLpExplorerContains(clickedGob.ngob.name)) {

                        NUtils.getGameUI().getCharInfo().LpExplorerAdd(clickedGob.ngob.name,name);
                        NUtils.getGameUI().getCharInfo().newLpExplorer = true;

                    } else {
                        if (NUtils.getGameUI().getCharInfo().LpExplorerGetSize(clickedGob.ngob.name) != object.get(clickedGob.ngob.name).size()) {
                            if (object.get(clickedGob.ngob.name).contains(name) && !NUtils.getGameUI().getCharInfo().IsLpExplorerContains(clickedGob.ngob.name, name)) {
                                NUtils.getGameUI().getCharInfo().LpExplorerAdd(clickedGob.ngob.name,name);
                                NUtils.getGameUI().getCharInfo().newLpExplorer = true;
                            }
                        }
                    }
                }
            }
            NUtils.getGameUI().map.clickedGob = null;
        }
    }

    public static ArrayList<String> getCategory(String name) {
        ArrayList<String> result = new ArrayList<>();
        for(String cat: categories.keySet())
        {
            for(int i = 0 ; i < categories.get(cat).size(); i++)
            {
                if(((String)(categories.get(cat).get(i)).get("name")).equals(name))
                {
                    result.add(cat);
                    break;
                }
            }
        }
        return result;
    }

    public static ArrayList<String> getCategoryContent(String name) {
        ArrayList<JSONObject> jresult = categories.get(name);
        ArrayList<String> result = new ArrayList<>();
        for(JSONObject res: jresult)
        {
            result.add(res.getString("name"));
        }
        return result;
    }
}
