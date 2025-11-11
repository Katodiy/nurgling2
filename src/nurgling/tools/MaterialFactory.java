package nurgling.tools;

import haven.*;
import haven.render.Pipe;
import haven.res.lib.vmat.Materials;
import nurgling.NGob;
import nurgling.NStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MaterialFactory {
    // Cache for loaded TexR objects to avoid duplicate loading
    private static final Map<String, TexR> texCache = new ConcurrentHashMap<>();
    
    private static TexR getTexR(String path, int layer) {
        String key = path + "#" + layer;
        return texCache.computeIfAbsent(key, k -> {
            return Resource.local().loadwait(path).layer(TexR.class, layer);
        });
    }
    
    private static TexR getTexR(String path) {
        return texCache.computeIfAbsent(path, k -> {
            return Resource.local().loadwait(path).layer(TexR.class);
        });
    }

    public static Map<Integer, Material> getMaterials(String name, Status status, Material mat) {
        switch (name){
            case "gfx/terobjs/cupboard":
            case "gfx/terobjs/cheeserack":
            case "gfx/terobjs/map/jotunclam":
                switch (status)
                {
                    case FREE: {
                        TexR rt0 = getTexR("nurgling/tex/pinefree-tex", 0);
                        TexR rt1 = getTexR("nurgling/tex/pinefree-tex", 2);
                        Map<Integer, Material> result = new HashMap<>();
                        Material mat0 = constructMaterial(rt0,mat);
                        Material mat1 = constructMaterial(rt1,mat);

                        result.put(0, mat0);
                        result.put(1, mat1);

                        return result;
                    }
                    case FULL: {
                        TexR rt0 = getTexR("nurgling/tex/pinefull-tex", 0);
                        TexR rt1 = getTexR("nurgling/tex/pinefree-tex", 2);
                        Map<Integer, Material> result = new HashMap<>();
                        Material mat0 = constructMaterial(rt0,mat);
                        Material mat1 = constructMaterial(rt1,mat);

                        result.put(0, mat0);
                        result.put(1, mat1);
                        return result;
                    }
                    case NOTFREE: {
                        TexR rt0 = getTexR("nurgling/tex/pinenf-tex", 0);
                        TexR rt1 = getTexR("nurgling/tex/pinefree-tex", 2);
                        Map<Integer, Material> result = new HashMap<>();
                        Material mat0 = constructMaterial(rt0,mat);
                        Material mat1 = constructMaterial(rt1,mat);

                        result.put(0, mat0);
                        result.put(1, mat1);
                        return result;
                    }
                }
                break;
            case "gfx/terobjs/chest":
                    switch (status)
                    {
                        case FREE: {
                            TexR rt0 = Resource.local().loadwait("nurgling/tex/pinefree-tex").layer(TexR.class, 0);
                            TexR rt1 = Resource.remote().loadwait("gfx/terobjs/subst/wroughtiron").layer(TexR.class, 0);
                            Map<Integer, Material> result = new HashMap<>();
                            result.put(0, constructMaterial(rt0,mat));
                            result.put(1, constructMaterial(rt1,mat));
                            return result;
                        }
                        case FULL: {
                            TexR rt0 = Resource.local().loadwait("nurgling/tex/pinefull-tex").layer(TexR.class, 0);
                            TexR rt1 = Resource.remote().loadwait("gfx/terobjs/subst/wroughtiron").layer(TexR.class, 0);
                            Map<Integer, Material> result = new HashMap<>();
                            result.put(0, constructMaterial(rt0,mat));
                            result.put(1, constructMaterial(rt1,mat));
                            return result;
                        }
                        case NOTFREE: {
                            TexR rt0 = Resource.local().loadwait("nurgling/tex/pinenf-tex").layer(TexR.class, 0);
                            TexR rt1 = Resource.remote().loadwait("gfx/terobjs/subst/wroughtiron").layer(TexR.class, 0);
                            Map<Integer, Material> result = new HashMap<>();
                            result.put(0, constructMaterial(rt0,mat));
                            result.put(1, constructMaterial(rt1,mat));
                            return result;
                        }
                    }
                    break;
            case "gfx/terobjs/barrel":
                switch (status) {
                    case FREE: {
                        TexR rt0 = getTexR("alttex/barrel/free");
                        Map<Integer, Material> result = new HashMap<>();
                        result.put(0, constructMaterial(rt0, mat));
                        return result;
                    }
                    case FULL: {
                        TexR rt0 = getTexR("alttex/barrel/full");
                        Map<Integer, Material> result = new HashMap<>();
                        result.put(0, constructMaterial(rt0, mat));
                        return result;
                    }
                }
                break;
            case "gfx/terobjs/dframe":
                switch (status) {
                    case FREE: {
                        TexR rt0 = getTexR("alttex/dframe/free");
                        Map<Integer, Material> result = new HashMap<>();
                        result.put(0, constructMaterial(rt0, mat));
                        return result;
                    }
                    case FULL: {
                        TexR rt0 = getTexR("alttex/dframe/full");
                        Map<Integer, Material> result = new HashMap<>();
                        result.put(0, constructMaterial(rt0, mat));
                        return result;
                    }
                    case NOTFREE: {
                        TexR rt0 = getTexR("alttex/dframe/notfree");
                        Map<Integer, Material> result = new HashMap<>();
                        result.put(0, constructMaterial(rt0, mat));
                        return result;
                    }
                }
                break;
        }
        return null;
    }


    public static Material constructMaterial(TexR texR, Material mat)
    {
        Light.PhongLight lp = new Light.PhongLight(true, new FColor(0, 0, 0, 1.0f),
                new FColor(0.8f, 0.8f, 0.8f, 1.0f),
                new FColor(0.643137f, 0.643137f, 0.643137f, 1.0f),
                new FColor(0.643137f, 0.643137f, 0.643137f, 1.0f), 0.0f);
        Light.CelShade lc = new Light.CelShade(true, false);
        if(mat!=null) {
            if (mat.states instanceof Pipe.Op.Composed)
                for (Pipe.Op p : ((Pipe.Op.Composed) mat.states).ops) {
                    if (p instanceof Light.PhongLight) {
                        lp = (Light.PhongLight) p;
                    } else if (p instanceof Light.CelShade) {
                        lc = (Light.CelShade) p;
                    }
                }
        }
        return new Material(lp, texR.tex().draw, texR.tex().clip,lc);
    }

    public enum Status{
        NOTDEFINED,
        FREE,
        FULL,
        READY,
        INWORK,
        NOTFREE,
        WARNING
    }
    public static final Map<String,Map<Status, Map<Integer,TexR>>> materialCashe = new ConcurrentHashMap<>();
    public static final Map<String,Map<Status, Materials>> materialsCashe = new ConcurrentHashMap<>();
    
    public static void clearCache(String name) {
        // Clear inner maps instead of removing the entire entry
        Map<Status, Map<Integer,TexR>> statusMap = materialCashe.get(name);
        if (statusMap != null) {
            statusMap.clear();
        }
        Map<Status, Materials> materialsMap = materialsCashe.get(name);
        if (materialsMap != null) {
            materialsMap.clear();
        }
    }

    public static Map<Integer,TexR> getMaterial(String name, Status status, Material.Res.Resolver resolver) {
        if (resolver_check(name, resolver)) {
            if (materialCashe.get(name) == null || materialCashe.get(name).get(status) == null) {
                return tryConstruct(name, status);
            } else
                return materialCashe.get(name).get(status);
        }
        return null;
    }

    private static boolean resolver_check(String name, Material.Res.Resolver resolver) {
        if(name.equals("gfx/terobjs/ttub"))
        {
            return resolver.toString().contains("mlink");
        }
        else if(name.equals("gfx/terobjs/map/jotunclam"))
        {
            return !resolver.toString().contains("mlink");
        }
        return true;
    }

    public static Status getStatus(String name, int mask) {
        switch (name)
        {
            case "gfx/terobjs/chest":
            case "gfx/terobjs/cupboard":
            {
                int freeMask = VSpec.chest_state.get(NStyle.Container.FREE);
                int fullMask = VSpec.chest_state.get(NStyle.Container.FULL);

                if((mask & ~freeMask) == 0) {
                    return Status.FREE;
                }
                else if((mask & fullMask) == fullMask)
                {
                    return Status.FULL;
                }
                else
                {
                    return Status.NOTFREE;
                }
            }
            case "gfx/terobjs/map/jotunclam":
            {
                if((mask & ~VSpec.jotun_state.get(NStyle.Container.FREE)) == 0) {
                    return Status.FREE;
                }
                else if((mask & VSpec.jotun_state.get(NStyle.Container.FULL)) == VSpec.jotun_state.get(NStyle.Container.FULL))
                {
                    return Status.FULL;
                }
                else
                {
                    return Status.NOTFREE;
                }
            }
            case "gfx/terobjs/ttub":
            {
                if ((mask & 8) != 0) {
                    return Status.READY;
                } else if ((mask & 4) != 0) {
                    return Status.INWORK;
                } else if ((mask & 2) != 0) {
                    return Status.FREE;
                } else if ((mask & 1) != 0 || mask == 0) {
                    return Status.WARNING;
                }
            }
            case "gfx/terobjs/dframe":
            case "gfx/terobjs/cheeserack": {
                if(mask == 0)
                    return Status.FREE;
                else if(mask == 1)
                    return Status.NOTFREE;
                else if(mask == 2)
                    return Status.FULL;
            }
            case "gfx/terobjs/barrel": {
                if(mask == 4)
                    return Status.FULL;
                else
                    return Status.FREE;
            }
        }
        return Status.NOTDEFINED;
    }

    private static Map<Integer,TexR> tryConstruct(String name, Status status) {
        synchronized (materialCashe) {
            // Ensure the outer map exists
            materialCashe.computeIfAbsent(name, k -> new ConcurrentHashMap<>());
            
            // Check if already cached
            Map<Status, Map<Integer,TexR>> statusMap = materialCashe.get(name);
            if (statusMap.get(status) != null) {
                return statusMap.get(status);
            }
            
            switch (name) {
                case "gfx/terobjs/barrel":
                    switch (status)
                    {
                        case FREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("alttex/barrel/free"));
                            return statusMap.get(status);
                        case FULL:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("alttex/barrel/full"));
                            return statusMap.get(status);
                    }
                    break;
                case "gfx/terobjs/dframe":
                    switch (status)
                    {
                        case FREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("alttex/dframe/free"));
                            return statusMap.get(status);
                        case FULL:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("alttex/dframe/full"));
                            return statusMap.get(status);
                        case NOTFREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("alttex/dframe/notfree"));
                            return statusMap.get(status);
                    }
                    break;
                case "gfx/terobjs/map/jotunclam":
                    switch (status)
                    {
                        case FREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("alttex/jotun/free"));
                            statusMap.get(status).put(2, getTexR("alttex/jotun/free"));
                            return statusMap.get(status);
                        case FULL:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("alttex/jotun/full"));
                            statusMap.get(status).put(2, getTexR("alttex/jotun/free"));
                            return statusMap.get(status);
                        case NOTFREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("alttex/jotun/notfree"));
                            statusMap.get(status).put(2, getTexR("alttex/jotun/free"));
                            return statusMap.get(status);
                    }
                    break;
                case "gfx/terobjs/ttub":
                    switch (status)
                    {
                        case FREE:
                        case WARNING:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(2, getTexR("nurgling/tex/pinefull-tex", 0));
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinefull-tex", 2));
                            return statusMap.get(status);
                        case INWORK:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(2, getTexR("nurgling/tex/pinenf-tex", 0));
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinenf-tex", 2));
                            return statusMap.get(status);
                        case READY:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(2, getTexR("nurgling/tex/pinefree-tex", 0));
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinefree-tex", 2));
                            return statusMap.get(status);
                    }
                    break;
                case "gfx/terobjs/chest":
                    switch (status)
                    {
                        case FREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinefree-tex", 0));
                            statusMap.get(status).put(1, Resource.remote().loadwait("gfx/terobjs/subst/wroughtiron").layer(TexR.class, 0));
                            return statusMap.get(status);
                        case FULL:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinefull-tex", 0));
                            statusMap.get(status).put(1, Resource.remote().loadwait("gfx/terobjs/subst/wroughtiron").layer(TexR.class, 0));
                            return statusMap.get(status);
                        case NOTFREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinenf-tex", 0));
                            statusMap.get(status).put(1, Resource.remote().loadwait("gfx/terobjs/subst/wroughtiron").layer(TexR.class, 0));
                            return statusMap.get(status);
                    }
                    break;
                case "gfx/terobjs/cupboard":
                case "gfx/terobjs/cheeserack":
                    switch (status)
                    {
                        case FREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinefree-tex", 0));
                            statusMap.get(status).put(1, getTexR("nurgling/tex/pinefree-tex", 2));
                            return statusMap.get(status);
                        case FULL:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinefull-tex", 0));
                            statusMap.get(status).put(1, getTexR("nurgling/tex/pinefree-tex", 2));
                            return statusMap.get(status);
                        case NOTFREE:
                            statusMap.put(status, new HashMap<>());
                            statusMap.get(status).put(0, getTexR("nurgling/tex/pinenf-tex", 0));
                            statusMap.get(status).put(1, getTexR("nurgling/tex/pinefree-tex", 2));
                            return statusMap.get(status);
                    }
                    break;
            }
        }
        return null;
    }
}
