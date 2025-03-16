package nurgling.tools;

import monitoring.NGlobalSearchItems;
import nurgling.NConfig;
import nurgling.NUtils;

import java.util.ArrayList;

public class NSearchItem
{
    public String name ="";

    public static class Quality{
        public double val;
        public Type type;

        public Quality(double val, Type type) {
            this.val = val;
            this.type = type;
        }

        public enum Type{
            MORE,
            LOW,
            EQ
        }
    }

    public ArrayList<Quality> q= new ArrayList<>();
    public class Stat{
        public String v;
        public double a;
        public boolean isMore = false;

        public Stat(String v, double a, boolean isMore) {
            this.v = v;
            this.a = a;
            this.isMore = isMore;
        }

        public Stat(String v) {
            this.v = v;
            a = 0;
        }
    }

    public final ArrayList<Stat> food = new ArrayList<>();
    public final ArrayList<Stat> gilding = new ArrayList<>();
    public boolean fgs = false;
    private void reset()
    {
        food.clear();
        gilding.clear();
        q.clear();
        fgs = false;
        name = "";
        if((Boolean) NConfig.get(NConfig.Key.globalsearch))
        {
            synchronized (NGlobalSearchItems.containerHashes) {
                NGlobalSearchItems.containerHashes.clear();
            }
        }
    }
    public void install(String value)
    {
        synchronized (gilding) {
            reset();
            if (value.startsWith("$")) {
                String[] items = value.split("\\s*;\\s*");
                for (String val : items) {
                    int pos = val.indexOf(":");
                    if(val.length()>pos+1 && pos!=-1) {
                        if (val.startsWith("$name")) {
                            name = val.substring(pos+1).toLowerCase();
                        } else if (val.startsWith("$fep")) {
                            if (val.contains(":"))
                            {
                                int minpos = val.indexOf("<");
                                int maxpos = val.indexOf(">");
                                if(minpos==maxpos)
                                {
                                    food.add(new Stat (val.substring(pos+1)));
                                }
                                else{
                                    int endpos = Math.max(minpos,maxpos);
                                    if(val.length()>endpos+1)
                                    {
                                        try {
                                            food.add(new Stat(val.substring(pos+1,endpos),Double.parseDouble(val.substring(endpos+1)),maxpos>minpos));
                                        }catch (NumberFormatException e)
                                        {
                                            food.add(new Stat (val.substring(pos+1,endpos)));
                                        }
                                    }
                                    else
                                    {
                                        food.add(new Stat (val.substring(pos+1,endpos)));
                                    }
                                }
                            }
                        } else if (val.startsWith("$gild")) {
                            if (val.contains(":"))
                            {
                                int minpos = val.indexOf("<");
                                int maxpos = val.indexOf(">");
                                if(minpos==maxpos)
                                {
                                    gilding.add(new Stat (val.substring(pos+1)));
                                }
                                else{
                                    int endpos = Math.max(minpos,maxpos);
                                    if(val.length()>endpos+1)
                                    {
                                        try {
                                            gilding.add(new Stat(val.substring(pos+1,endpos),Double.parseDouble(val.substring(endpos+1)),maxpos>minpos));
                                        }catch (NumberFormatException e)
                                        {
                                            gilding.add(new Stat (val.substring(pos+1,endpos)));
                                        }
                                    }
                                    else
                                    {
                                        gilding.add(new Stat (val.substring(pos+1,endpos)));
                                    }
                                }
                            }
                        }
                    }
                    if (val.startsWith("$fgs")) {
                        fgs = true;
                    }
                    else if(val.startsWith("$q"))
                    {
                        int minpos = val.indexOf("<");
                        int maxpos = val.indexOf(">");
                        int eqpos = val.indexOf("=");
                        try {
                            if(minpos!=-1 && val.length()>minpos+1){
                                double d = Double.parseDouble(val.substring(minpos+1));
                                q.add(new Quality(d, Quality.Type.LOW));
                            }
                            else if(maxpos!=-1 && val.length()>maxpos+1){
                                double d = Double.parseDouble(val.substring(maxpos+1));
                                q.add(new Quality(d, Quality.Type.MORE));
                            }
                            else if(eqpos!=-1 && val.length()>eqpos+1){
                                double d = Double.parseDouble(val.substring(eqpos+1));
                                q.add(new Quality(d, Quality.Type.EQ));
                            }
                        }
                        catch (NumberFormatException ignored)
                        {
                        }
                    }
                }
            } else {
                name = value.toLowerCase();
            }
        }
        if((Boolean) NConfig.get(NConfig.Key.globalsearch))
        {
            NUtils.getUI().core.searchContainer(this);
        }
    }

    public boolean isEmpty() {
        synchronized (gilding) {
            return name.isEmpty() && !fgs && gilding.isEmpty() && food.isEmpty() && q.isEmpty();
        }
    }

    public boolean onlyName() {
        synchronized (gilding) {
            return !name.isEmpty() && !fgs && gilding.isEmpty() && food.isEmpty() && q.isEmpty();
        }
    }
}