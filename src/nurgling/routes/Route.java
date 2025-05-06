package nurgling.routes;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import nurgling.NMapView;
import nurgling.NUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.*;

public class Route {
    public int id;
    public String name;
    public String path = "";
    public ArrayList<RoutePoint> waypoints = new ArrayList<>();
    public String hash;
    public Color color = Color.BLACK;
    public ArrayList<RouteSpecialization> spec = new ArrayList<>();

    public static class RouteSpecialization {
        public String name;
        public String subtype = null;

        public RouteSpecialization(String name, String subtype) {
            this.name = name;
            this.subtype = subtype;
        }

        public RouteSpecialization(String name) {
            this.name = name;
        }
    }

    public Route(String name) {
        this.name = name;
    }

    public void addWaypoint() {
        Gob player = NUtils.player();
        Coord2d rc = player.rc;

        try {
            RoutePoint rp = new RoutePoint(rc, NUtils.getGameUI().ui.sess.glob.map);
            this.waypoints.add(rp);
            NUtils.getGameUI().msg("Waypoint added: " + rc);
        } catch (Exception e) {
            NUtils.getGameUI().msg("Failed to add waypoint: " + e.getMessage());
        }

        ((NMapView) NUtils.getGameUI().map).createRouteLabel(this.id);
    }

    public Route(JSONObject obj) {
        this.name = obj.getString("name");
        this.id = obj.getInt("id");

        if (obj.has("path")) {
            this.path = obj.getString("path");
        } else if (obj.has("dir")) {
            this.path = "/" + obj.getString("path");
        }

        this.waypoints = new ArrayList<>();
        if (obj.has("waypoints")) {
            JSONArray arr = obj.getJSONArray("waypoints");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject point = arr.getJSONObject(i);
                long gridId = point.getLong("gridId");
                int x = point.getInt("x");
                int y = point.getInt("y");
                waypoints.add(new RoutePoint(gridId, new Coord(x, y)));
            }
        }

        this.spec = new ArrayList<>();
        if (obj.has("specializations")) {
            JSONArray arr = obj.getJSONArray("specializations");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject spec = arr.getJSONObject(i);
                String name = spec.getString("name");
                String subtype = spec.has("subtype") ? spec.getString("subtype") : null;
                this.spec.add(new RouteSpecialization(name, subtype));
            }
        }
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("name", this.name);
        obj.put("id", this.id);
        obj.put("path", this.path);

        JSONArray wpArray = new JSONArray();
        for (RoutePoint rp : waypoints) {
            JSONObject p = new JSONObject();
            p.put("gridId", rp.gridId);
            p.put("x", rp.localCoord.x);
            p.put("y", rp.localCoord.y);
            wpArray.put(p);
        }
        obj.put("waypoints", wpArray);

        JSONArray specArray = new JSONArray();
        for (RouteSpecialization s : spec) {
            JSONObject specObj = new JSONObject();
            specObj.put("name", s.name);
            if (s.subtype != null) {
                specObj.put("subtype", s.subtype);
            }
            specArray.put(specObj);
        }
        obj.put("specializations", specArray);

        return obj;
    }

    public boolean hasSpecialization(String name) {
        for (RouteSpecialization s : spec) {
            if (s.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeSpecialization(String name) {
        for (int i = 0; i < spec.size(); i++) {
            if (spec.get(i).name.equals(name)) {
                spec.remove(i);
                return true;
            }
        }
        return false;
    }
}
