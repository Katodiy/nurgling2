package nurgling.conf;

import haven.Coord;
import nurgling.NConfig;
import nurgling.NUI;
import nurgling.NUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class NBlueprintPlanterProp implements JConf {
    final private String username;
    final private String chrid;
    
    public String blueprintName = null;
    public Long gridId = null;  // Grid ID where blueprint is placed
    public Coord tileCoord = null;  // Tile coordinates within grid
    
    public NBlueprintPlanterProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
    }
    
    public NBlueprintPlanterProp(HashMap<String, Object> values) {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("blueprintName") != null)
            blueprintName = (String) values.get("blueprintName");
        if (values.get("gridId") != null)
            gridId = ((Number) values.get("gridId")).longValue();
        if (values.get("tileX") != null && values.get("tileY") != null)
            tileCoord = new Coord(((Number) values.get("tileX")).intValue(), ((Number) values.get("tileY")).intValue());
    }
    
    public static void set(NBlueprintPlanterProp prop) {
        ArrayList<NBlueprintPlanterProp> props = ((ArrayList<NBlueprintPlanterProp>) NConfig.get(NConfig.Key.blueprintplanterprop));
        if (props != null) {
            for (Iterator<NBlueprintPlanterProp> i = props.iterator(); i.hasNext(); ) {
                NBlueprintPlanterProp oldprop = i.next();
                if (oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid)) {
                    i.remove();
                    break;
                }
            }
        } else {
            props = new ArrayList<>();
        }
        props.add(prop);
        NConfig.set(NConfig.Key.blueprintplanterprop, props);
    }
    
    @Override
    public String toString() {
        return "NBlueprintPlanterProp[" + username + "|" + chrid + "]";
    }
    
    @Override
    public JSONObject toJson() {
        JSONObject jprop = new JSONObject();
        jprop.put("type", "NBlueprintPlanterProp");
        jprop.put("username", username);
        jprop.put("chrid", chrid);
        jprop.put("blueprintName", blueprintName);
        if (gridId != null)
            jprop.put("gridId", gridId);
        if (tileCoord != null) {
            jprop.put("tileX", tileCoord.x);
            jprop.put("tileY", tileCoord.y);
        }
        return jprop;
    }
    
    public static NBlueprintPlanterProp get(NUI.NSessInfo sessInfo) {
        if (sessInfo == null || NUtils.getGameUI() == null || NUtils.getGameUI().getCharInfo() == null)
            return null;
        String chrid = NUtils.getGameUI().getCharInfo().chrid;
        ArrayList<NBlueprintPlanterProp> props = ((ArrayList<NBlueprintPlanterProp>) NConfig.get(NConfig.Key.blueprintplanterprop));
        if (props == null)
            props = new ArrayList<>();
        for (NBlueprintPlanterProp prop : props) {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(chrid)) {
                return prop;
            }
        }
        return new NBlueprintPlanterProp(sessInfo.username, chrid);
    }
}
