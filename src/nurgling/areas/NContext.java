package nurgling.areas;

import haven.*;
import nurgling.*;
import nurgling.actions.PathFinder;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.actions.bots.SelectArea;
import nurgling.routes.RoutePoint;
import nurgling.tools.*;
import org.json.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static haven.MCache.cmaps;
import static haven.OCache.posres;

public class NContext {
    
    private HashMap<NAlias, NArea> inAreas = new HashMap<>();
    private HashMap<NAlias, TreeMap<Double,NArea>> outAreas = new HashMap<>();
    private HashMap<NArea.Specialisation, NArea> specArea = new HashMap<>();
    private HashMap<String, NArea> areas = new HashMap<>();
    private HashMap<String, RoutePoint> rps = new HashMap<>();
    int counter = 0;
    private NGameUI gui;

    private NGlobalCoord lastcoord;
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
        if(!area.isVisible()) {
            new RoutePointNavigator(rps.get(areaId)).run(gui);
        }
    }

    public String createArea(String msg, BufferedImage loadsimg) throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg(msg);
        (insa = new SelectArea(loadsimg)).run(gui);
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
            if(size > path.size()) {
                target = point;
                size = path.size();
            }
        }
        rps.put(id,target);
        return id;
    }
    
    
    
    
    
    public static NArea findIn(String name) {
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
                        areas.add(new TestedArea(cand, th));
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
                        areas.add(new TestedArea(cand, th));
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
                        areas.add(new TestedArea(cand, th));
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
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containOut(String.valueOf(name))) {
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

    public static ArrayList<NArea> getAllVisible() throws InterruptedException {
        double dist = 10000;
        ArrayList<NArea> res = new ArrayList<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>=0) {
                    NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                    if(test.isVisible()) {
                        Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                        if(testrc != null) {
                            Coord2d playerRelativeCoord = NUtils.player().rc;
                            ArrayList<Gob> gobs = Finder.findGobs(test);
                            boolean isReachable = false;

                            if(gobs.isEmpty()) {
                                isReachable = PathFinder.isAvailable(testrc.a, playerRelativeCoord, false) || PathFinder.isAvailable(testrc.b, playerRelativeCoord, false);
                            } else {
                                for(Gob gob : gobs) {
                                    if (PathFinder.isAvailable(gob)) {
                                        isReachable = true;
                                        break;
                                    }
                                }
                            }

                            if (testrc.a.dist(playerRelativeCoord) + testrc.b.dist(playerRelativeCoord) < dist && isReachable) {
                                res.add(test);
                            }
                        }
                    }
                }
            }
        }
        return res;
    }
}