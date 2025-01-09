package nurgling.tools;

import haven.*;
import haven.render.Pipe;
import haven.res.lib.vmat.Materials;
import nurgling.NGob;
import nurgling.NStyle;

import java.util.HashMap;
import java.util.Map;

public class MaterialFactory {


    public static Map<Integer,Material> getMaterials(String name, Status status, Material mat) {
        switch (name){
            case "gfx/terobjs/cupboard":
            case "gfx/terobjs/cheeserack":
                switch (status)
                {
                    case FREE: {
                        TexR rt0 = Resource.local().loadwait("nurgling/tex/pinefree-tex").layer(TexR.class, 0);
                        TexR rt1 = Resource.local().loadwait("nurgling/tex/pinefree-tex").layer(TexR.class, 2);
                        Map<Integer, Material> result = new HashMap<>();
                        result.put(0, constructMaterial(rt0,mat));
                        result.put(1, constructMaterial(rt1,mat));
                        return result;
                    }
                    case FULL: {
                        TexR rt0 = Resource.local().loadwait("nurgling/tex/pinefull-tex").layer(TexR.class, 0);
                        TexR rt1 = Resource.local().loadwait("nurgling/tex/pinefree-tex").layer(TexR.class, 2);
                        Map<Integer, Material> result = new HashMap<>();
                        result.put(0, constructMaterial(rt0,mat));
                        result.put(1, constructMaterial(rt1,mat));
                        return result;
                    }
                    case NOTFREE: {
                        TexR rt0 = Resource.local().loadwait("nurgling/tex/pinenf-tex").layer(TexR.class, 0);
                        TexR rt1 = Resource.local().loadwait("nurgling/tex/pinefree-tex").layer(TexR.class, 2);
                        Map<Integer, Material> result = new HashMap<>();
                        result.put(0, constructMaterial(rt0,mat));
                        result.put(1, constructMaterial(rt1,mat));
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
    public static final HashMap<String,HashMap<Status, Map<Integer,TexR>>> materialCashe = new HashMap<>();
    public static HashMap<String,HashMap<Status, Materials>> materialsCashe = new HashMap<>();

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
        return true;
    }

    public static Status getStatus(String name, int mask) {
        switch (name)
        {
            case "gfx/terobjs/chest":
            case "gfx/terobjs/cupboard":
            {
                if((mask & ~VSpec.chest_state.get(NStyle.Container.FREE)) == 0) {
                    return Status.FREE;
                }
                else if((mask & VSpec.chest_state.get(NStyle.Container.FULL)) == VSpec.chest_state.get(NStyle.Container.FULL))
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
            if (materialCashe.get(name) == null)
                materialCashe.put(name, new HashMap<Status, Map<Integer,TexR>>());
            switch (name) {
                case "gfx/terobjs/barrel":
                    switch (status)
                    {
                        case FREE:
                            materialCashe.get(name).put(status,new HashMap<>());
                            materialCashe.get(name).get(status).put(0,Resource.local().loadwait("alttex/barrel/free").layer(TexR.class));
                            return materialCashe.get(name).get(status);
                        case FULL:
                            materialCashe.get(name).put(status,new HashMap<>());
                            materialCashe.get(name).get(status).put(0,Resource.local().loadwait("alttex/barrel/full").layer(TexR.class));
                            return materialCashe.get(name).get(status);
                    }
                case "gfx/terobjs/dframe":
                    switch (status)
                    {
                        case FREE:
                            materialCashe.get(name).put(status,new HashMap<>());
                            materialCashe.get(name).get(status).put(0,Resource.local().loadwait("alttex/dframe/free").layer(TexR.class));
                            return materialCashe.get(name).get(status);
                        case FULL:
                            materialCashe.get(name).put(status,new HashMap<>());
                            materialCashe.get(name).get(status).put(0,Resource.local().loadwait("alttex/dframe/full").layer(TexR.class));
                            return materialCashe.get(name).get(status);
                        case NOTFREE:
                            materialCashe.get(name).put(status,new HashMap<>());
                            materialCashe.get(name).get(status).put(0,Resource.local().loadwait("alttex/dframe/notfree").layer(TexR.class));
                            return materialCashe.get(name).get(status);
                    }
                case "gfx/terobjs/ttub":
                    switch (status)
                    {
                        case FREE:
                        case WARNING:
                            materialCashe.get(name).put(status,new HashMap<>());
                            materialCashe.get(name).get(status).put(2,Resource.local().loadwait("nurgling/tex/pinefull-tex").layer(TexR.class,0));
                            materialCashe.get(name).get(status).put(0,Resource.local().loadwait("nurgling/tex/pinefull-tex").layer(TexR.class,2));
                            return materialCashe.get(name).get(status);
                        case INWORK:
                            materialCashe.get(name).put(status,new HashMap<>());
                            materialCashe.get(name).get(status).put(2,Resource.local().loadwait("nurgling/tex/pinenf-tex").layer(TexR.class,0));
                            materialCashe.get(name).get(status).put(0,Resource.local().loadwait("nurgling/tex/pinenf-tex").layer(TexR.class,2));
                            return materialCashe.get(name).get(status);
                        case READY:
                            materialCashe.get(name).put(status,new HashMap<>());
                            materialCashe.get(name).get(status).put(2,Resource.local().loadwait("nurgling/tex/pinefree-tex").layer(TexR.class,0));
                            materialCashe.get(name).get(status).put(0,Resource.local().loadwait("nurgling/tex/pinefree-tex").layer(TexR.class,2));
                            return materialCashe.get(name).get(status);
                    }
            }
        }
        return null;
    }
}
