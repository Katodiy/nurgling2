package nurgling.tools;

import haven.GAttrib;
import haven.Gob;
import nurgling.NInventory;
import nurgling.NUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Container {
    public Gob gob;
    public String cap;

    ArrayList<Updater> updaters = new ArrayList<>();

    public void update() throws InterruptedException {
        for(Updater upd: updaters)
        {
            upd.update();
        }
    }

    public abstract class Updater{
        public HashMap<String, Object> getRes()
        {
            return res;
        }
        public abstract void update() throws InterruptedException;
        protected HashMap<String, Object> res = new HashMap<>();

        public boolean isReady()
        {
            return !res.isEmpty();
        }
    }

    public class Space extends Updater{
        public static final String FREESPACE = "freespace";
        public static final String MAXSPACE = "maxSpace";
        @Override
        public void update()  throws InterruptedException{
            res.put(FREESPACE, NUtils.getGameUI().getInventory(cap).getFreeSpace());
            res.put(MAXSPACE, NUtils.getGameUI().getInventory(cap).getTotalSpace());
        }

    }

    public class FuelLvl extends Updater{
        @Override
        public void update()  throws InterruptedException{
            res.put("flvl",(int)(30 * NUtils.getFuelLvl(cap, new Color(255, 128, 0))));
        }
    }


    public Map<Class<? extends Updater>, Updater> res = new HashMap<Class<? extends Updater>, Updater>();
    public <C extends Updater> C getattr(Class<C> c) {
        Updater attr = this.res.get(attrclass(c));
        if(!c.isInstance(attr))
            return(null);
        return(c.cast(attr));
    }

    private Class<? extends Updater> attrclass(Class<? extends Updater> cl) {
        while(true) {
            Class<?> p = cl.getSuperclass();
            if(p == Updater.class)
                return(cl);
            cl = p.asSubclass(Updater.class);
        }
    }

    public <C extends Updater> void initattr(Class<C> c)
    {
        if(c == Space.class)
            res.put(c,new Space());
        else if(c == FuelLvl.class)
            res.put(c,new FuelLvl());
    }
}
