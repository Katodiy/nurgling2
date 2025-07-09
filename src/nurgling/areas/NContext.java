package nurgling.areas;

import haven.*;
import nurgling.*;
import nurgling.actions.LiftObject;
import nurgling.actions.PathFinder;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.actions.bots.SelectArea;
import nurgling.routes.RoutePoint;
import nurgling.tools.*;
import nurgling.tools.Container;
import nurgling.widgets.Specialisation;
import org.json.*;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NContext {

    public final static AtomicBoolean waitBot = new AtomicBoolean(false);
    private HashMap<String, String> inAreas = new HashMap<>();
    private HashMap<String, TreeMap<Double,String>> outAreas = new HashMap<>();
    private HashMap<String, String> barrels = new HashMap<>();
    private HashMap<String, BarrelStorage> barrelstorage = new HashMap<>();
    private HashMap<NArea.Specialisation, String> specArea = new HashMap<>();
    private HashMap<String, NArea> areas = new HashMap<>();
    private HashMap<String, RoutePoint> rps = new HashMap<>();
    private HashMap<String, ObjectStorage> containers = new HashMap<>();
    public HashSet<Long> barrelsid = new HashSet<>();

    public boolean bwaused = false;
    int counter = 0;
    private NGameUI gui;

    private NGlobalCoord lastcoord;

    public static HashMap<String, String> contcaps = new HashMap<>();
    static {
        contcaps.put("gfx/terobjs/chest", "Chest");
        contcaps.put("gfx/terobjs/crate", "Crate");
        contcaps.put("gfx/terobjs/kiln", "Kiln");
        contcaps.put("gfx/terobjs/cupboard", "Cupboard");
        contcaps.put("gfx/terobjs/shed", "Shed");
        contcaps.put("gfx/terobjs/largechest", "Large Chest");
        contcaps.put("gfx/terobjs/metalcabinet", "Metal Cabinet");
        contcaps.put("gfx/terobjs/strawbasket", "Straw Basket");
        contcaps.put("gfx/terobjs/bonechest", "Bone Chest");
        contcaps.put("gfx/terobjs/coffer", "Coffer");
        contcaps.put("gfx/terobjs/leatherbasket", "Leather Basket");
        contcaps.put("gfx/terobjs/woodbox", "Woodbox");
        contcaps.put("gfx/terobjs/linencrate", "Linen Crate");
        contcaps.put("gfx/terobjs/stonecasket", "Stone Casket");
        contcaps.put("gfx/terobjs/birchbasket", "Birch Basket");
        contcaps.put("gfx/terobjs/wbasket", "Basket");
        contcaps.put("gfx/terobjs/exquisitechest", "Exquisite Chest");
        contcaps.put("gfx/terobjs/furn/table-stone", "Table");
        contcaps.put("gfx/terobjs/furn/table-rustic", "Table");
        contcaps.put("gfx/terobjs/furn/table-elegant", "Table");
        contcaps.put("gfx/terobjs/furn/table-cottage", "Table");
        contcaps.put("gfx/terobjs/map/jotunclam", "Jotun Clam");
        contcaps.put("gfx/terobjs/studydesk", "Study Desk");
    }

    public static HashMap<String, String> customTool = new HashMap<>();
    static {
        customTool.put("Clay Jar", "paginae/bld/potterswheel");
        customTool.put("Garden Pot", "paginae/bld/potterswheel");
        customTool.put("Pot", "paginae/bld/potterswheel");
        customTool.put("Treeplanter's Pot", "paginae/bld/potterswheel");
        customTool.put("Urn", "paginae/bld/potterswheel");
        customTool.put("Teapot", "paginae/bld/potterswheel");
        customTool.put("Mug", "paginae/bld/potterswheel");
        customTool.put("Stoneware Vase", "paginae/bld/potterswheel");
    }

    static HashMap<String, String> equip_map;
    static {
        equip_map = new HashMap<>();
        equip_map.put("gfx/invobjs/small/fryingpan", "Frying Pan");
        equip_map.put("gfx/invobjs/small/glassrod", "Glass Blowing Rod");
        equip_map.put("gfx/invobjs/smithshammer", "Smithy's Hammer");
    }

    static HashMap<String, NContext.Workstation> workstation_map;
    static {
        workstation_map = new HashMap<>();
        workstation_map.put("paginae/bld/meatgrinder",new NContext.Workstation("gfx/terobjs/meatgrinder", "gfx/borka/idle"));
        workstation_map.put("paginae/bld/churn",new NContext.Workstation("gfx/terobjs/churn", "gfx/borka/churnan-idle"));
        workstation_map.put("paginae/bld/loom",new NContext.Workstation("gfx/terobjs/loom", "gfx/borka/loomsit"));
        workstation_map.put("paginae/bld/ropewalk",new NContext.Workstation("gfx/terobjs/ropewalk", "gfx/borka/idle"));
        workstation_map.put("paginae/bld/crucible",new NContext.Workstation("gfx/terobjs/crucible", null));
        workstation_map.put("gfx/invobjs/fire",new NContext.Workstation("gfx/terobjs/pow", null));
        workstation_map.put("gfx/invobjs/cauldron",new NContext.Workstation("gfx/terobjs/cauldron", null));
        workstation_map.put("paginae/bld/potterswheel",new NContext.Workstation("gfx/terobjs/potterswheel", "gfx/borka/pwheelidle"));
        workstation_map.put("paginae/bld/swheel",new NContext.Workstation("gfx/terobjs/swheel", "gfx/borka/swheelan-idle"));
        workstation_map.put("paginae/bld/anvil",new NContext.Workstation("gfx/terobjs/anvil", null));
    }

    static HashMap<String, Specialisation.SpecName> workstation_spec_map;
    static {
        workstation_spec_map = new HashMap<>();
        workstation_spec_map.put("gfx/terobjs/meatgrinder", Specialisation.SpecName.meatgrinder);
        workstation_spec_map.put("gfx/terobjs/churn", Specialisation.SpecName.churn);
        workstation_spec_map.put("gfx/terobjs/loom",Specialisation.SpecName.loom);
        workstation_spec_map.put("gfx/terobjs/swheel",Specialisation.SpecName.swheel);
        workstation_spec_map.put("gfx/terobjs/ropewalk",Specialisation.SpecName.ropewalk);
        workstation_spec_map.put("gfx/terobjs/crucible",Specialisation.SpecName.crucible);
        workstation_spec_map.put("gfx/terobjs/pow",Specialisation.SpecName.pow);
        workstation_spec_map.put("gfx/terobjs/cauldron",Specialisation.SpecName.boiler);
        workstation_spec_map.put("gfx/terobjs/potterswheel",Specialisation.SpecName.potterswheel);
        workstation_spec_map.put("gfx/terobjs/anvil",Specialisation.SpecName.anvil);
    }


    public static class Barter implements ObjectStorage
    {
        public long barter;
        public long chest;

        public Barter(Gob barter, Gob chest)
        {
            this.barter = barter.id;
            this.chest = chest.id;
        }
    }

    public static class Barrel implements ObjectStorage
    {
        public long barrel;

        public Barrel(Gob barrel)
        {
            this.barrel = barrel.id;
        }
    }



    public interface ObjectStorage
    {
        default double getTh(){
            return 1;
        }
    }

    public static class Pile implements ObjectStorage{
        public Gob pile;
        public Pile(Gob gob)
        {
            this.pile = gob;
        }
    }


    public void addCustomTool(String resName) {
        String cust = customTool.get(resName);
        if(cust != null) {
            NContext.Workstation workstation_cand = workstation_map.get(cust);
            if(workstation_cand!=null)
            {
                workstation = workstation_cand;
            }
        }
    }



    public TreeMap<Double,String> getOutAreas(String item) {
        return outAreas.get(item);
    }

    public NArea getSpecArea(NContext.Workstation workstation) throws InterruptedException {
        if(!areas.containsKey(workstation.station)) {
            NArea area = findSpec(workstation_spec_map.get(workstation.station).toString());
            if (area == null) {
                area = findSpecGlobal(workstation_spec_map.get(workstation.station).toString());
            }
            if (area != null) {
                areas.put(String.valueOf(workstation.station), area);
                List<RoutePoint> pointList = ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(area));
                if(pointList!=null && !pointList.isEmpty())
                    rps.put(String.valueOf(workstation.station),pointList.get(pointList.size()-1));
            }
            else
            {
                return null;
            }
        }
        navigateToAreaIfNeeded(workstation.station);
        return areas.get(workstation.station);
    }

    public static class BarrelStorage
    {
        public NGlobalCoord coord;
        public String olname;

        public BarrelStorage(NGlobalCoord coord, String olname) {
            this.coord = coord;
            this.olname = olname;
        }
    }

    public BarrelStorage getBarrelStorage(String item)
    {
        return barrelstorage.get(item);
    }

    public Gob getBarrelInArea(String item) throws InterruptedException {
        if(!barrels.containsKey(item)) {
            NArea area = findIn(item);
            if (area == null) {
                area = findInGlobal(item);
            }
            if (area != null) {
                areas.put(String.valueOf(area.id), area);
                List<RoutePoint> pointList = (((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(area)));
                if(pointList!=null && !pointList.isEmpty())
                    rps.put(String.valueOf(area.id),pointList.get(pointList.size()-1));
                barrels.put(item, String.valueOf(area.id));
            }
            if(area == null)
                return null;
        }
        String areaid = barrels.get(item);
        navigateToAreaIfNeeded(areaid);
        for(Gob gob: Finder.findGobs(areas.get(areaid), new NAlias("barrel")))
        {
            if(NUtils.barrelHasContent(gob))
            {
                barrelstorage.put(item,new BarrelStorage(new NGlobalCoord(gob.rc), NUtils.getContentsOfBarrel(gob)));
                return gob;
            }
        }
        return null;

    }

    public void navigateToBarrelArea(String item) throws InterruptedException {
        String areaid = barrels.get(item);
        navigateToAreaIfNeeded(areaid);
    }

    public NArea getSpecArea(Specialisation.SpecName name) throws InterruptedException {
        if(!areas.containsKey(name.toString())) {
            NArea area = findSpec(name.toString());
            if (area == null) {
                area = findSpecGlobal(name.toString());
            }
            if (area != null) {
                areas.put(String.valueOf(name.toString()), area);
                List<RoutePoint> pointList = (((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(area)));
                if(pointList!=null && !pointList.isEmpty())
                    rps.put(String.valueOf(name.toString()),pointList.get(pointList.size()-1));
            }
            else
            {
                return null;
            }
        }
        navigateToAreaIfNeeded(name.toString());
        return areas.get(name.toString());
    }

    public NArea getSpecArea(Specialisation.SpecName name, String sub) throws InterruptedException {
        if(!areas.containsKey(name.toString())) {
            NArea area = findSpec(name.toString(),sub);
            if (area == null) {
                area = findSpecGlobal(name.toString(),sub);
            }
            if (area != null) {
                areas.put(String.valueOf(name.toString()), area);
                List<RoutePoint> pointList = (((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(area)));
                if(pointList!=null && !pointList.isEmpty())
                    rps.put(String.valueOf(name.toString()),pointList.get(pointList.size()-1));
            }
            else
            {
                return null;
            }
        }
        navigateToAreaIfNeeded(name.toString());
        return areas.get(name.toString());
    }

    public ArrayList<ObjectStorage> getInStorages(String item) throws InterruptedException {

        ArrayList<ObjectStorage> inputs = new ArrayList<>();
        String id = inAreas.get(item);
        if(id!=null) {
            navigateToAreaIfNeeded(inAreas.get(item));

            NArea area = areas.get(id);
            NArea.Ingredient ingredient = area.getInput(item);
            switch (ingredient.type) {
                case BARTER:
                    inputs.add(new Barter(Finder.findGob(area, new NAlias("gfx/terobjs/barterstand")),
                            Finder.findGob(area, new NAlias("gfx/terobjs/chest"))));
                    break;
                case CONTAINER: {
                    for (Gob gob : Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()), new ArrayList<>()))) {
                        String hash = gob.ngob.hash;
                        if(containers.containsKey(hash))
                        {
                            inputs.add(containers.get(hash));
                        }
                        else {
                            Container ic = new Container(gob, contcaps.get(gob.ngob.name));
                            containers.put(gob.ngob.hash, ic);
                            inputs.add(ic);
                        }
                    }
                    for (Gob gob : Finder.findGobs(area, new NAlias("stockpile"))) {
                        inputs.add(new Pile(gob));
                    }

                }
            }
            inputs.sort(new Comparator<ObjectStorage>() {
                @Override
                public int compare(ObjectStorage o1, ObjectStorage o2) {
                    if (o1 instanceof Pile && o2 instanceof Pile)
                        return NUtils.d_comp.compare(((Pile) o1).pile, ((Pile) o2).pile);
                    return 0;
                }
            });
        }
        return inputs;
    }

    public ArrayList<ObjectStorage> getOutStorages(String item, double q)  throws InterruptedException
    {
        ArrayList<ObjectStorage> outputs = new ArrayList<>();
        TreeMap<Double,String> thmap =  outAreas.get(item);
        String id = null;
        for(Double key: thmap.descendingKeySet())
        {
            if(q>=key)
            {
                id = thmap.get(key);
                break;
            }
        }
        if(id!=null) {
            navigateToAreaIfNeeded(id);

            NArea area = areas.get(id);
            NArea.Ingredient ingredient = area.getOutput(item);
            if (ingredient != null) {
                switch (ingredient.type) {
                    case BARTER:
                        outputs.add(new Barter(Finder.findGob(area, new NAlias("gfx/terobjs/barterstand")),
                                Finder.findGob(area, new NAlias("gfx/terobjs/chest"))));
                        break;
                    case CONTAINER: {

                        for (Gob gob : Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()), new ArrayList<>()))) {
                            String hash = gob.ngob.hash;
                            if(containers.containsKey(hash))
                            {
                                outputs.add(containers.get(hash));
                            }
                            else {
                                Container ic = new Container(gob, contcaps.get(gob.ngob.name));
                                ic.initattr(Container.Space.class);
                                containers.put(gob.ngob.hash, ic);
                                outputs.add(ic);
                            }
                        }
                        for (Gob gob : Finder.findGobs(area, new NAlias("stockpile"))) {
                            outputs.add(new Pile(gob));
                        }
                        if (outputs.isEmpty()) {
                            outputs.add(new Pile(null));
                        }
                        break;
                    }
                    case BARREL: {
                        for (Gob gob : Finder.findGobs(area, new NAlias("barrel"))) {
                            outputs.add(new Barrel(gob));
                        }
                    }
                }
            }
            else
            {
                for (Gob gob : Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()), new ArrayList<>()))) {
                    String hash = gob.ngob.hash;
                    if(containers.containsKey(hash))
                    {
                        outputs.add(containers.get(hash));
                    }
                    else {
                        Container ic = new Container(gob, contcaps.get(gob.ngob.name));
                        ic.initattr(Container.Space.class);
                        containers.put(gob.ngob.hash, ic);
                        outputs.add(ic);
                    }
                }
                for (Gob gob : Finder.findGobs(area, new NAlias("stockpile"))) {
                    outputs.add(new Pile(gob));
                }
                if (outputs.isEmpty()) {
                    outputs.add(new Pile(null));
                }
            }
        }
        return outputs;
    }

    public static class Workstation
    {
        public String station;
        public String pose;
        public long selected = -1;

        public NGlobalCoord targetPoint = null;

        public Workstation(String station, String pose)
        {
            this.station = station;
            this.pose = pose;
        }
    }

    public void addTools(List<Indir<Resource>> tools)
    {
        for (Indir<Resource> res : tools)
        {
            String equip_cand = equip_map.get(res.get().name);
            if(equip_cand!=null)
            {
                equip = equip_cand;
            }
            NContext.Workstation workstation_cand = workstation_map.get(res.get().name);
            if(workstation_cand!=null)
            {
                workstation = workstation_cand;
            }
        }
    }

    public String equip = null;
    public NContext.Workstation workstation = null;
    public NContext(NGameUI gui)
    {
        this.gui = gui;
    }

    public void setLastPos(Coord2d pos)
    {
        lastcoord = new NGlobalCoord(pos);
    }

    public ArrayList<Gob> getGobs(String areaId, NAlias pattern) throws InterruptedException {
        navigateToAreaIfNeeded(areaId);
        return Finder.findGobs(areas.get(areaId), pattern);
    }

    public Gob getGob(String areaId, NAlias pattern) throws InterruptedException {
        navigateToAreaIfNeeded(areaId);
        return Finder.findGob(areas.get(areaId), pattern);
    }

    public Gob getGob(String areaId, long id) throws InterruptedException {
        navigateToAreaIfNeeded(areaId);
        return Finder.findGob(id);
    }

    private void navigateToAreaIfNeeded(String areaId) throws InterruptedException {
        NArea area = areas.get(areaId);
        if(area == null) {
            gui.msg(areaId + " Not found!");
            return;
        }
        if((!area.isVisible() || area.getCenter2d().dist(NUtils.player().rc)>450) && rps.containsKey(areaId)) {
            new RoutePointNavigator(rps.get(areaId), area.id).run(gui);
        }
    }
    public String createArea(String msg, BufferedImage bauble) throws InterruptedException {
        return createArea(msg, bauble,null);
    }

    public String createArea(String msg, BufferedImage bauble, BufferedImage custom) throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg(msg);
        if(custom==null)
            (insa = new SelectArea(bauble)).run(gui);
        else
            (insa = new SelectArea(bauble,custom)).run(gui);
        String id = "temp"+counter++;
        NArea tempArea = new NArea(id);
        tempArea.space = insa.result;
        tempArea.grids_id.clear();
        tempArea.grids_id.addAll(tempArea.space.space.keySet());
        areas.put(id, tempArea);
        int size = 10000;
        RoutePoint target = null;
        for(RoutePoint point : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestRoutePoints(tempArea)) {
            List<RoutePoint> path = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(NUtils.findNearestPoint(), point);
            if(path!=null) {
                if (size > path.size()) {
                    target = point;
                    size = path.size();
                }
            }
        }
        rps.put(id,target);
        return id;
    }

    public boolean isInBarrel(String item) {
        NArea area = findIn(item);
        if (area == null) {
            area = findInGlobal(item);
        }
        if(area!=null)
        {
            return area.getInput(item).type == NArea.Ingredient.Type.BARREL;
        }
        return false;
    }

    public void addInItem(String name, BufferedImage loadsimg) throws InterruptedException {
        NArea area = findIn(name);
        if (area == null) {
            area = findInGlobal(name);
        }
        if(area!=null)
        {
            areas.put(String.valueOf(area.id),area);
            List<RoutePoint> pointList = (((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(area)));
            if(pointList!=null && !pointList.isEmpty())
                rps.put(String.valueOf(area.id),pointList.get(pointList.size()-1));
            inAreas.put(name, String.valueOf(area.id));
        }
        if (loadsimg!=null && area == null) {
            inAreas.put(name, createArea("Please select area with:" + name, Resource.loadsimg("baubles/custom"), loadsimg));
        }
    }

    public boolean addOutItem(String name, BufferedImage loadsimg, double th) throws InterruptedException {
        if(!outAreas.containsKey(name))
        {
            outAreas.put(name,new TreeMap<>());
        }
        else
        {
            for(Double key :outAreas.get(name).descendingKeySet())
            {
                if(th>key)
                    return true;
            }
        }
        NArea area = findOut(name,th);
        if (area == null) {
            area = findOutGlobal(name, th, gui);
        }
        if(area!=null)
        {
            areas.put(String.valueOf(area.id),area);
            List<RoutePoint> pointList = (((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(area)));
            if(pointList!=null && !pointList.isEmpty())
                rps.put(String.valueOf(area.id),pointList.get(pointList.size()-1));
            outAreas.get(name).put(Math.abs((double)area.getOutput(name).th), String.valueOf(area.id));
        }
        if (loadsimg!=null && area == null) {
            outAreas.get(name).put(Math.abs(th), createArea("Please select area for:" + name, Resource.loadsimg("baubles/custom"), loadsimg));
        }
        else
        {
            if(area == null)
                return false;
        }
        return true;
    }

    
    public static NArea findIn(String name) {
        double dist = 10000;
        Gob player = NUtils.player();
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>0 && player!=null) {
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containIn(name)) {
                        NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                        Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                        if(test.getRCArea()!=null) {
                            double testdist;
                            if ((testdist = (testrc.a.dist(player.rc) + testrc.b.dist(player.rc))) < dist) {
                                res = test;
                                dist = testdist;
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static NArea findIn(NAlias name) {
        double dist = 10000;
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>0) {
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containIn(name)) {
                        NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                        Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                        if(test.getRCArea()!=null) {
                            double testdist;
                            if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                                res = test;
                                dist = testdist;
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static ArrayList<NArea> findAllIn(NAlias name) {
        ArrayList<NArea> results = new ArrayList<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>0) {
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containIn(name)) {
                        NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                        if(test.getRCArea()!=null) {
                            results.add(test);
                        }
                    }
                }
            }
        }
        return results;
    }

    public Coord2d getLastPosCoord(String areaId) throws InterruptedException {
        navigateToAreaIfNeeded(areaId);

        return lastcoord.getCurrentCoord();
    }

    public Pair<Coord2d, Coord2d> getRCArea(String marea) throws InterruptedException {
        NArea area = areas.get(marea);
        if(!area.isVisible())
        {
            navigateToAreaIfNeeded(marea);
        }
        return area.getRCArea();
    }




    private static class TestedArea {
        NArea area;
        double th;

        public TestedArea(NArea area, double th) {
            this.area = area;
            this.th = th;
        }
    }

    static Comparator<TestedArea> ta_comp = new Comparator<TestedArea>(){
        @Override
        public int compare(TestedArea o1, TestedArea o2) {
            return Double.compare(o1.th, o2.th);
        }
    };

    public static NArea findOut(NAlias name, double th) {
        double dist = 10000;
        NArea res = null;

        ArrayList<TestedArea> areas = new ArrayList<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0) {
                    NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                    if (cand.isVisible() && cand.containOut(name.getDefault(), th)) {
                        areas.add(new TestedArea(cand, cand.getOutput(name.getDefault()).th));
                    }
                }
            }
        }

        areas.sort(ta_comp);

        double tth = 1;
        for (TestedArea area : areas) {
            if(area.th<=th) {
                res = area.area;
                tth = area.th;
            }
        }

        ArrayList<NArea> targets = new ArrayList<>();
        for(TestedArea area :areas) {
            if(area.th ==tth)
                targets.add(area.area);
        }

        if(targets.size()>1) {
            for (NArea test: targets) {
                Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                double testdist;
                if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                    res = test;
                    dist = testdist;
                }
            }
        }
        return res;
    }

    public static NArea findOut(String name, double th) {
        double dist = 10000;
        NArea res = null;

        ArrayList<TestedArea> areas = new ArrayList<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0) {
                    NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                    if (cand.isVisible() && cand.containOut(name, th) && cand.getRCArea()!=null) {
                        areas.add(new TestedArea(cand, cand.getOutput(name).th));
                    }
                }
            }
        }

        areas.sort(ta_comp);

        double tth = 1;
        for (TestedArea area : areas) {
            if(area.th<=th) {
                res = area.area;
                tth = area.th;
            }
        }

        ArrayList<NArea> targets = new ArrayList<>();
        for(TestedArea area :areas) {
            if(area.th == tth)
                targets.add(area.area);
        }

        if(targets.size()>1) {
            for (NArea test: targets) {
                Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                double testdist;
                if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                    res = test;
                    dist = testdist;
                }
            }
        }
        return res;
    }

    public static NArea findInGlobal(String name) {
        return findInGlobal(new NAlias(name));
    }

    public static NArea findInGlobal(NAlias name) {
        double dist = 10000;
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>0) {
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containIn(name)) {
                        NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                        List<RoutePoint> routePoints = ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(cand));
                        if(routePoints!=null) {
                            if(routePoints.size() <dist) {
                                res = cand;
                                dist = routePoints.size();
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static NArea findSpecGlobal(String name, String sub) {
        int dist = 10000;
        NArea target = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0) {
                    for (NArea.Specialisation s : NUtils.getGameUI().map.glob.map.areas.get(id).spec) {
                        if (s.name.equals(name)  && ((sub == null || sub.isEmpty()) || s.subtype != null && s.subtype.toLowerCase().equals(sub.toLowerCase()))) {
                            NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                            List<RoutePoint> routePoints = ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI()), ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(cand));
                            if(routePoints!=null) {
                                if(routePoints.size() <dist) {
                                    target = cand;
                                    dist = routePoints.size();
                                }
                            }
                        }
                    }
                }
            }
        }
        return target;
    }

    public static NArea findSpecGlobal(NArea.Specialisation spec) {
        return findSpecGlobal(spec.name, spec.subtype);
    }

    public static NArea findSpecGlobal(String name) {
        return findSpecGlobal(name, null);
    }

    public static NArea findOutGlobal(String name, double th, NGameUI gui) {
        NArea res = null;
        ArrayList<TestedArea> areas = new ArrayList<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0) {
                    NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                    if (cand.containOut(name) && ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findPath(((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(gui), ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph().findAreaRoutePoint(cand)) != null) {
                        areas.add(new TestedArea(cand, cand.getOutput(name).th));
                    }
                }
            }
        }

        areas.sort(ta_comp);

        double tth = 1;
        for (TestedArea area : areas) {
            if(area.th<=th) {
                res = area.area;
                tth = area.th;
            }
        }

        ArrayList<NArea> targets = new ArrayList<>();
        for(TestedArea area :areas) {
            if(area.th == tth)
                targets.add(area.area);
        }

        if(targets.size()>1) {
            for (NArea test: targets) {
                res = test;
            }
        }
        return res;
    }

    public static NArea findAreaById(int areaId) {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            Map<Integer, NArea> areas = NUtils.getGameUI().map.glob.map.areas;
            return areas.get(areaId);
        }
        return null;
    }

    public static TreeMap<Integer,NArea> findOuts(NAlias name) {
        TreeMap<Integer,NArea> areas = new TreeMap<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0)
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containOut(name.getDefault())) {
                        NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                        if(cand.getRCArea()!=null) {
                            for (int i = 0; i < cand.jout.length(); i++) {
                                if (NParser.checkName((String) ((JSONObject) cand.jout.get(i)).get("name"), name)) {
                                    Integer th = (((JSONObject) cand.jout.get(i)).has("th")) ? ((Integer) ((JSONObject) cand.jout.get(i)).get("th")) : 1;
                                    areas.put(th, cand);
                                }
                            }
                        }
                    }
            }
        }
        return areas;
    }

    public static TreeMap<Integer,NArea> findOutsGlobal(String name) {
        TreeMap<Integer,NArea> areas = new TreeMap<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0)
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containOut(name)) {
                        NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                        if(!cand.hide) {
                            for (int i = 0; i < cand.jout.length(); i++) {
                                if (NParser.checkName((String) ((JSONObject) cand.jout.get(i)).get("name"), name)) {
                                    Integer th = (((JSONObject) cand.jout.get(i)).has("th")) ? ((Integer) ((JSONObject) cand.jout.get(i)).get("th")) : 1;
                                    areas.put(th, cand);
                                }
                            }
                        }
                    }
            }
        }
        return areas;
    }

    public static NArea findSpec(NArea.Specialisation spec) {
        if(spec.subtype==null)
            return findSpec(spec.name);
        else
            return findSpec(spec.name, spec.subtype);
    }

    public static NArea findSpec(String name) {
        double dist = 10000;
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>=0) {
                    for (NArea.Specialisation s : NUtils.getGameUI().map.glob.map.areas.get(id).spec) {
                        if (s.name.equals(name)) {
                            NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                            if(test.isVisible()) {
                                Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                                if(testrc != null) {
                                    double testdist;
                                    if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                                        res = test;
                                        dist = testdist;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static NArea findSpec(String name, String sub) {
        double dist = 10000;
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>=0) {
                    for (NArea.Specialisation s : NUtils.getGameUI().map.glob.map.areas.get(id).spec) {
                        if (s.name.equals(name) && s.subtype != null && s.subtype.toLowerCase().equals(sub.toLowerCase())) {
                            NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                            if(test.isVisible()) {
                                Pair<Coord2d,Coord2d> testrc = test.getRCArea();
                                if(testrc!=null) {
                                    double testdist;
                                    if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                                        res = test;
                                        dist = testdist;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }
}