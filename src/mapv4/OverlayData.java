package mapv4;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class OverlayData {
    private final String tag;
    private final String resourceName;
    private final int[] tiles;

    public OverlayData(String tag, String resourceName, int[] tiles) {
        this.tag = tag;
        this.resourceName = resourceName;
        this.tiles = tiles;
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("tag", tag);
        obj.put("resourceName", resourceName);
        JSONArray tilesArray = new JSONArray();
        for (int tile : tiles) {
            tilesArray.put(tile);
        }
        obj.put("tiles", tilesArray);
        return obj;
    }

    public int[] getTiles() {
        return tiles;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public int hashCode() {
        int result = resourceName.hashCode();
        result = 31 * result + Arrays.hashCode(tiles);
        return result;
    }
}
