package nurgling.conf;

import org.json.*;

import java.util.*;

public class NLoginData implements JConf
{
    public String name = "";
    public String pass = "";
    public byte[] token;
    public boolean isTokenUsed = false;

    public NLoginData(String name, String pass)
    {
        this.name = name;
        this.pass = pass;
    }

    public NLoginData(String name, byte[] token)
    {
        this.name = name;
        this.token = token;
        this.isTokenUsed = true;
    }

    public NLoginData(HashMap<String, Object> values)
    {
        name = (String) values.get("user");
        if(values.get("pass")!=null)
            pass = (String) values.get("pass");
        if(values.get("isToken")!=null)
            isTokenUsed = (Boolean) values.get("isToken");
        if(isTokenUsed)
        {
            ArrayList<Object> buft = (ArrayList<Object>) values.get("token");
            token = new byte[buft.size()];
            int count = 0;
            for (Object b : buft)
            {
                token[count++] = ((Integer) b).byteValue();
            }
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof NLoginData)) return false;
        NLoginData ol = (NLoginData) other;
        return ol.name.equals(name) && ol.pass.equals(pass);
    }

    @Override
    public JSONObject toJson()
    {
        JSONObject object = new JSONObject();
        object.put("type", "NLoginData");
        object.put("user", name);
        if(!pass.isEmpty())
            object.put("pass", pass);
        if(isTokenUsed)
        {
            object.put("isToken", isTokenUsed);
            JSONArray jtoken = new JSONArray();
            for (Byte b : token)
            {
                jtoken.put(b);
            }
            object.put("token", jtoken);
        }
        return object;
    }
}