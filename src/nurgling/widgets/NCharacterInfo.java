package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class NCharacterInfo extends Widget {

    public String chrid;
    String path;
    public final Set<String> varity = new HashSet<>();
    CharWnd charWnd = null;
    long lastWriting = 0;
    long delta = 300;

    double oldFEPSsize = 0;
    boolean needFEPreset = false;

    boolean isStarted = false;
    public boolean newLpExplorer = false;
    String varCand = null;
    private final HashMap<String,ArrayList<String>> lpExplorer = new HashMap<>();

    public boolean IsLpExplorerContains(String name)
    {
        synchronized(lpExplorer) {
            return lpExplorer.containsKey(name);
        }
    }

    public boolean IsLpExplorerContains(String name, String var)
    {
        synchronized(lpExplorer) {
            if(lpExplorer.containsKey(name))
            {
                return lpExplorer.get(name).contains(var);
            }
            return false;
        }
    }

    public void LpExplorerAdd(String name, String var)
    {
        synchronized(lpExplorer) {
            if(lpExplorer.containsKey(name))
            {
                lpExplorer.get(name).add(var);
            }
            else
            {
                lpExplorer.put(name,new ArrayList<>());
                lpExplorer.get(name).add(var);
            }
        }
    }

    public int LpExplorerGetSize(String name)
    {
        synchronized(lpExplorer) {
            if(lpExplorer.containsKey(name))
                return lpExplorer.get(name).size();
        }
        return 0;
    }

    public NCharacterInfo(String chrid, NUI nui) {
        this.chrid = chrid;
        path = ((HashDirCache) ResCache.global).base + "\\..\\" + (nui.sessInfo==null?"":nui.sessInfo.username) + "_" + chrid.trim() + ".dat";
        read();
    }

    void read() {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(path), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException ignore)
        {
        }

        if (!contentBuilder.toString().isEmpty()) {
            try {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray vararr = (JSONArray) main.get("varity");
                if (vararr != null) {
                    synchronized (varity) {
                        for (int i = 0; i < vararr.length(); i++) {
                            JSONObject jvarity = (JSONObject) vararr.get(i);
                            varity.add(jvarity.getString("name"));
                        }
                    }
                }
                JSONArray lpexplorerarr = (JSONArray) main.get("lpexplorer");
                if (lpexplorerarr != null) {
                    synchronized (lpExplorer) {
                        for (int i = 0; i < lpexplorerarr.length(); i++) {
                            JSONObject jlp = (JSONObject) lpexplorerarr.get(i);

                            if (lpExplorer.containsKey(jlp.getString("name"))) {
                                lpExplorer.get(jlp.getString("name")).add(jlp.getString("key"));
                            } else {
                                lpExplorer.put(jlp.getString("name"), new ArrayList<>());
                                lpExplorer.get(jlp.getString("name")).add(jlp.getString("key"));
                            }
                        }
                    }
                }
            }
            catch (JSONException ignore)
            {

            }
        }
    }

    void write() {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            JSONObject main = new JSONObject();
            JSONArray jvararr = new JSONArray();
            for (String var : varity) {
                JSONObject jvarity = new JSONObject();
                jvarity.put("name",var);
                jvararr.put(jvarity);
            }
            main.put("varity",jvararr);

            JSONArray jlpexplore = new JSONArray();
            synchronized (lpExplorer) {
                if (!lpExplorer.isEmpty()) {
                    for (String key : lpExplorer.keySet()) {
                        ArrayList<String> vals = lpExplorer.get(key);
                        for (String val : vals) {
                            JSONObject jlp = new JSONObject();
                            jlp.put("name",key);
                            jlp.put("key",val);
                            jlpexplore.put(jlp);
                        }
                    }
                }
            }
            main.put("lpexplorer",jlpexplore);

            try
            {
                FileWriter f = new FileWriter(path,StandardCharsets.UTF_8);
                main.write(f);
                f.close();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void setCharWnd(CharWnd charWnd) {
        oldFEPSsize = calcFEPsize(charWnd);
        this.charWnd = charWnd;
    }

    private double calcFEPsize(CharWnd charWnd)
    {
        double len = 0;
        for (BAttrWnd.FoodMeter.El el : charWnd.battr.feps.els){
            len+=el.a;
        }
        return len;
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if(charWnd!=null && NUtils.getUI()!=null) {
            double fepssize = calcFEPsize(charWnd);
            if(Math.abs(oldFEPSsize-fepssize)>0.005) {
                if (varity.size() > 0 && fepssize==0) {
                    varity.clear();

                    oldFEPSsize = 0;
                }
                else
                {
                    if (varCand != null) {
                        varity.add(varCand);
                    }
                    oldFEPSsize = fepssize;
                }
                needFEPreset = true;
                isStarted = true;
            }
            if(varity.size()>0 && oldFEPSsize == 0 && isStarted)
            {
                varity.clear();
                needFEPreset = true;
            }
            if (NUtils.getTickId() - lastWriting > delta && needFEPreset) {
                write();
                lastWriting = NUtils.getTickId();
                needFEPreset = false;
                oldFEPSsize = fepssize;
            }
        }
        if(newLpExplorer)
        {
            write();
            newLpExplorer = false;
        }
    }

    public void setCandidate(String defn) {
        varCand = defn;
    }

    NGItem flowerCand;
    public void setFlowerCandidate(NGItem item) {
        flowerCand = item;
    }
}
