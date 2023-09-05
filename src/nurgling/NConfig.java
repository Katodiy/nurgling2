package nurgling;

import haven.*;
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
        resizeprop
    }


    public NConfig()
    {
        conf = new HashMap<>();
        conf.put(Key.showVarity, false);
        conf.put(Key.autoFlower, false);
        conf.put(Key.autoSplitter, false);
        conf.put(Key.autoDropper, false);
        conf.put(Key.is_real_time, true);
        conf.put(Key.numbelts, 1);
    }

    HashMap<Key, Object> conf = new HashMap<>();
    private boolean isUpd = false;
    String path = ((HashDirCache) ResCache.global).base + "\\..\\" + "nconfig.nurgling.json";

    public boolean isUpdated()
    {
        return isUpd;
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

    static NConfig current;

    private ArrayList<Object> readArray(ArrayList<HashMap<String, Object>> objs)
    {
        if (objs.size() > 0)
        {
            ArrayList<Object> res = new ArrayList<>();
            for (HashMap<String, Object> obj : objs)
            {
                switch ((String) obj.get("type"))
                {
                    case "NLoginData":
                        res.add(new NLoginData(obj));
                        break;
                    case "NDragProp":
                        res.add(new NDragProp(obj));
                        break;
                    case "NResizeProp":
                        res.add(new NResizeProp(obj));
                        break;
                    case "NToolBeltProp":
                        res.add(new NToolBeltProp(obj));
                        break;
                }
            }
            return res;
        }
        return new ArrayList<>();
    }

    public void read()
    {
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
                if (entry.getValue() instanceof HashMap<?, ?>)
                {
                    HashMap<String, Object> hobj = ((HashMap<String, Object>) entry.getValue());
                    String type;
                    if ((type = (String) hobj.get("type")) != null)
                    {
                        switch (type)
                        {
                            case "NLoginData":
                                conf.put(Key.valueOf(entry.getKey()), new NLoginData(hobj));
                        }
                    }
                }
                else if (entry.getValue() instanceof ArrayList<?>)
                {
                    conf.put(Key.valueOf(entry.getKey()), readArray((ArrayList<HashMap<String, Object>>) entry.getValue()));
                }
                else
                {
                    conf.put(Key.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }

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
            FileWriter f = new FileWriter(path);
            main.write(f);
            f.close();
            current.isUpd = false;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
