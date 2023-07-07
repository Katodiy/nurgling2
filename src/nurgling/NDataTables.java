package nurgling;

import haven.Resource;
import org.json.JSONObject;

import java.util.*;

public class NDataTables
{
    public HashMap<String, String> data_vessel;
    public HashMap<String, List<String>> data_food;
    public HashMap<String, List<String>> data_drinks;
    public HashMap<String, String> vessel_res;

    public NDataTables()
    {
        read_drink_data();
    }

    private void read_drink_data()
    {
        data_vessel = new HashMap<>();
        data_food = new HashMap<>();
        data_drinks = new HashMap<>();
        vessel_res = new HashMap<>();

        Resource.Tooltip drink_data = ((Resource.Tooltip) Resource.remote().loadwait("nurgling/data/drink_data").layer(Resource.Tooltip.class));

        if (drink_data != null)
        {
            JSONObject jdata = new JSONObject(drink_data.t);
            Map<String, Object> data = jdata.toMap();
            ArrayList<HashMap<String, Object>> df = ((ArrayList<HashMap<String, Object>>) data.get("data_food"));
            for (HashMap<String, Object> obj : df)
            {
                data_food.put((String) obj.get("name"), (ArrayList<String>) obj.get("types"));
            }

            ArrayList<HashMap<String, Object>> dd = ((ArrayList<HashMap<String, Object>>) data.get("data_drinks"));
            for (HashMap<String, Object> obj : dd)
            {
                data_drinks.put((String) obj.get("types"), (ArrayList<String>) obj.get("drink"));
            }

            ArrayList<HashMap<String, Object>> dv = ((ArrayList<HashMap<String, Object>>) data.get("data_vessel"));
            for (HashMap<String, Object> obj : dv)
            {
                data_vessel.put((String) obj.get("drink"), (String) obj.get("vessel"));
            }

            ArrayList<HashMap<String, Object>> vr = ((ArrayList<HashMap<String, Object>>) data.get("vessel_res"));
            for (HashMap<String, Object> obj : vr)
            {
                vessel_res.put((String) obj.get("vessel"), (String) obj.get("res"));
            }
        }
    }
}

