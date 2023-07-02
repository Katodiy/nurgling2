package nurgling;

import haven.*;
import jdk.nashorn.internal.parser.*;
import nurgling.tools.*;
import org.json.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class NConfig {

    HashMap<String, Object> conf = new HashMap<>();

    private boolean isUpd = false;
    String path = ((HashDirCache) ResCache.global).base + "\\..\\" + "nconfig.nurgling.json";
    public boolean isUpdated(){
        return isUpd;
    }

    public static Object get(String name){
        if(current == null)
            return null;
        else
            return current.conf.get(name);
    }

    public static void set(String name, Object val){
        if(current != null) {
            current.isUpd = true;
            current.conf.put(name, val);
        }
    }

    static NConfig current;

    public void read(){
        current = this;
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(path), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            //handle exception
        }

        if(!contentBuilder.toString().isEmpty()) {
            JSONObject main = new JSONObject(contentBuilder.toString());
            Map<String, Object> map = main.toMap();
            conf = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String && ((String) entry.getValue()).contains("NColor")) {
                    conf.put(entry.getKey(), NColor.build((String) entry.getValue()));
                } else {
                    conf.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public void write(){
        Map<String, Object> prep = new HashMap<>();
        for (Map.Entry<String, Object> entry : conf.entrySet()) {
            if (entry.getValue() instanceof NColor) {
                prep.put(entry.getKey(), entry.getValue().toString());
            } else {
                prep.put(entry.getKey(), entry.getValue());
            }
        }
        JSONObject main = new JSONObject(prep);
        try {
            FileWriter f = new FileWriter(path);
            main.write(f);
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
