package nurgling.conf;

import nurgling.NConfig;
import nurgling.NUI;
import nurgling.NUtils;
import nurgling.routes.ForagerPath;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NTrufflePigProp implements JConf {

    private final String username;
    private final String chrid;
    public String currentPreset = "Default";
    public HashMap<String, PresetData> presets = new HashMap<>();

    public static class PresetData {
        public String pathFile = "";
        public transient ForagerPath foragerPath = null;

        public PresetData() {}

        public PresetData(String pathFile) {
            this.pathFile = pathFile;
        }
    }

    public NTrufflePigProp(String username, String chrid) {
        this.username = username;
        this.chrid = chrid;
        presets.put("Default", new PresetData());
    }

    @SuppressWarnings("unchecked")
    public NTrufflePigProp(HashMap<String, Object> values) {
        chrid = (String) values.get("chrid");
        username = (String) values.get("username");
        if (values.get("currentPreset") != null)
            currentPreset = (String) values.get("currentPreset");

        presets = new HashMap<>();
        if (values.get("presets") != null) {
            HashMap<String, HashMap<String, Object>> presetsMap =
                (HashMap<String, HashMap<String, Object>>) values.get("presets");
            for (Map.Entry<String, HashMap<String, Object>> entry : presetsMap.entrySet()) {
                PresetData pd = new PresetData();
                if (entry.getValue().get("pathFile") != null)
                    pd.pathFile = (String) entry.getValue().get("pathFile");
                presets.put(entry.getKey(), pd);
            }
        }

        if (presets.isEmpty()) {
            presets.put("Default", new PresetData());
        }
    }

    public static void set(NTrufflePigProp prop) {
        @SuppressWarnings("unchecked")
        ArrayList<NTrufflePigProp> trufflePigProps = ((ArrayList<NTrufflePigProp>) NConfig.get(NConfig.Key.trufflepigprop));
        if (trufflePigProps != null) {
            for (Iterator<NTrufflePigProp> i = trufflePigProps.iterator(); i.hasNext(); ) {
                NTrufflePigProp oldprop = i.next();
                if (oldprop.username.equals(prop.username) && oldprop.chrid.equals(prop.chrid)) {
                    i.remove();
                    break;
                }
            }
        } else {
            trufflePigProps = new ArrayList<>();
        }
        trufflePigProps.add(prop);
        NConfig.set(NConfig.Key.trufflepigprop, trufflePigProps);
    }

    @Override
    public String toString() {
        return "NTrufflePigProp[" + username + "|" + chrid + "]";
    }

    @Override
    public JSONObject toJson() {
        JSONObject jtrufflepig = new JSONObject();
        jtrufflepig.put("type", "NTrufflePigProp");
        jtrufflepig.put("username", username);
        jtrufflepig.put("chrid", chrid);
        jtrufflepig.put("currentPreset", currentPreset);

        JSONObject presetsJson = new JSONObject();
        for (Map.Entry<String, PresetData> entry : presets.entrySet()) {
            JSONObject presetJson = new JSONObject();
            presetJson.put("pathFile", entry.getValue().pathFile);
            presetsJson.put(entry.getKey(), presetJson);
        }
        jtrufflepig.put("presets", presetsJson);

        return jtrufflepig;
    }

    public static NTrufflePigProp get(NUI.NSessInfo sessInfo) {
        if (sessInfo == null || NUtils.getGameUI() == null || NUtils.getGameUI().getCharInfo() == null)
            return null;
        String chrid = NUtils.getGameUI().getCharInfo().chrid;
        @SuppressWarnings("unchecked")
        ArrayList<NTrufflePigProp> trufflePigProps = ((ArrayList<NTrufflePigProp>) NConfig.get(NConfig.Key.trufflepigprop));
        if (trufflePigProps == null)
            trufflePigProps = new ArrayList<>();
        for (NTrufflePigProp prop : trufflePigProps) {
            if (prop.username.equals(sessInfo.username) && prop.chrid.equals(chrid)) {
                return prop;
            }
        }
        return new NTrufflePigProp(sessInfo.username, chrid);
    }
}
