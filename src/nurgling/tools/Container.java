package nurgling.tools;

import haven.Coord;
import haven.GAttrib;
import haven.Gob;
import haven.WItem;
import haven.res.ui.tt.wellmined.WellMined;
import nurgling.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class Container {
    public Gob gob;
    public String cap;

    public Map<Class<? extends Updater>, Updater> updaters = new HashMap<Class<? extends Updater>, Updater>();
    public void update() throws InterruptedException {
        if(NUtils.getGameUI().getInventory(cap)!=null)
        {
            for (Updater upd : updaters.values()) {
                upd.update();
            }
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

    public class Tetris extends Updater{
        public static final String DATA = "data";
        public static final String DONE = "done";
        public static final String VIRTUAL = "virtual";
        public static final String TARGET_COORD = "tc";
        @Override
        public void update()  throws InterruptedException{
            res.put(DATA, NUtils.getGameUI().getInventory(cap).containerMatrix());
            boolean done = true;
            for(Coord coord : (ArrayList<Coord>)res.get(TARGET_COORD))
            {
                if(NUtils.getGameUI().getInventory(cap).getNumberFreeCoord(coord)!=0)
                    done = false;
            }
            res.put(DONE, done);
            res.put(VIRTUAL, done);
        }

        public boolean tryPlace(Coord coord)
        {
            if(!(Boolean) res.get(DONE) && !(Boolean)res.get(VIRTUAL) && placeItem(coord))
            {
                boolean done = true;
                for(Coord cand : (ArrayList<Coord>)res.get(TARGET_COORD))
                {
                    if(calcNumberFreeCoord(cand)!=0) {
                        done = false;
                        break;
                    }
                }
                if(done)
                    res.put(VIRTUAL, done);
                return true;
            }
            return false;
        }

        boolean placeItem(Coord coord)
        {
         short[][] grid = (short[][])res.get(DATA);
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j <  grid[0].length; j++) {
                    if (grid[i][j] == 0) {
                        boolean isFree = true;
                        if (i + coord.x - 1 < grid.length && j + coord.y - 1 < grid[0].length) {
                            for (int k = i; k < i + coord.x; k++) {
                                for (int n = j; n < j + coord.y; n++) {
                                    if (grid[k][n]!=0) {
                                        isFree = false;
                                        break;
                                    }
                                }
                            }
                            if (isFree) {
                                for (int k = i; k < i + coord.x; k++) {
                                    for (int n = j; n < j + coord.y; n++) {
                                        grid[k][n] = 1;
                                    }
                                }
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        public int calcNumberFreeCoord(Coord target_size)
        {
            int count = 0;
            short[][] oldData = (short[][])res.get(DATA);
            short[][] grid = new short[oldData.length][oldData[0].length];
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[0].length; j++) {
                    grid[i][j] = oldData[i][j];
                }
            }

            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[0].length; j++) {
                    if (grid[i][j] == 0) {
                        if (i + target_size.x - 1 < grid.length && j + target_size.y - 1 < grid[0].length) {
                            boolean isFree = true;
                            for (int k = i; k < i + target_size.x; k++) {
                                for (int n = j; n < j + target_size.y; n++) {
                                    if (grid[k][n]!=0) {
                                        isFree = false;
                                        break;
                                    }
                                }
                            }
                            if (isFree) {
                                count += 1;
                                for (int k = i; k < i + target_size.x; k++) {
                                    for (int n = j; n < j + target_size.y; n++) {
                                        grid[k][n] = 1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return count;
        }


    }


    static NAlias ores = new NAlias ( new ArrayList<> (
            Arrays.asList ( "Cassiterite", "Lead Glance", "Wine Glance", "Chalcopyrite", "Malachite", "Peacock Ore", "Cinnabar", "Heavy Earth", "Iron Ochre",
                    "Bloodstone", "Black Ore", "Galena", "Silvershine", "Horn Silver", "Direvein", "Schrifterz", "Leaf Ore", "Meteorite", "Dross") ) );

    public class FuelLvl extends Updater{
        public static final String FUELLVL = "flvl";
        public static final String MAXLVL = "maxlvl";
        public static final String CREDOLVL = "credolvl";
        public static final String FUELTYPE = "fueltype";
        public static final String READY = "ready";
        public static final String NOCREDO = "nocredo";

        @Override
        public void update()  throws InterruptedException{
            res.put(FUELLVL,(int)(30 * NUtils.getFuelLvl(cap, new Color(255, 128, 0))));
            res.remove(NOCREDO);
            if(res.containsKey(CREDOLVL))
            {
                for(WItem item : NUtils.getGameUI().getInventory(cap).getItems(ores))
                {
                    if(((NGItem)item.item).getInfo(WellMined.class)==null && ((NGItem)item.item).name()!=null) {
                        res.put(NOCREDO, true);
                        break;
                    }
                }
                if(!res.containsKey(NOCREDO)) {
                    res.put(NOCREDO, false);
                }
            }
        }

        public void setMaxlvl(int maxLvl){
            res.put(MAXLVL,maxLvl);
        }

        public void setCredolvl(int credolvl){
            res.put(CREDOLVL,credolvl);
        }

        public void setFueltype(String fueltype){
            res.put(FUELTYPE,fueltype);
        }

        public int neededFuel() {
            if (!res.containsKey(NOCREDO) || (boolean) res.get(NOCREDO))
                return (int) res.get(MAXLVL) - (int) res.get(FUELLVL);
            else
                return (int) res.get(CREDOLVL) - (int) res.get(FUELLVL);
        }

    }


    public class TestAttr extends Updater{
        public static final String ATTR = "attr";

        public void SetAttr(String attr){

        }

        @Override
        public void update() throws InterruptedException {

        }


    }



    public class TargetItems extends Updater{
        public static final String TARGETS = "targets";
        public static final String MAXNUM = "maxnum";

        @Override
        public void update()  throws InterruptedException {
            if (res.containsKey(TARGETS)) {
                HashMap<NAlias, Integer> targets = ((HashMap<NAlias, Integer>) res.get(TARGETS));
                for (NAlias target : targets.keySet()) {
                    ((HashMap<NAlias, Integer>) res.get(TARGETS)).put(target, NUtils.getGameUI().getInventory(cap).getItems(target).size());
                }
            }
        }

        public int getTargets(NAlias target) {
            for (NAlias cand : ((HashMap<NAlias, Integer>) res.get(TARGETS)).keySet()) {
                if (NParser.checkName(target, cand)) {
                    return ((HashMap<NAlias, Integer>) res.get(TARGETS)).get(cand);
                }
            }
            return 0;
        }

        public void addTarget(NAlias target) {
            boolean found = false;
            for (NAlias cand : ((HashMap<NAlias, Integer>) res.get(TARGETS)).keySet()) {
                if (NParser.checkName(target, cand)) {
                    found = true;
                }
            }
            if(!found)
                ((HashMap<NAlias, Integer>)res.get(TARGETS)).put(target,0);
        }

        public void addTarget(String target) {
            addTarget(new NAlias(target));
        }

        public TargetItems() {
            res.put(TARGETS,new HashMap<NAlias, Integer>());
        }

        public void setMaxNum(int i) {
            res.put(MAXNUM,i);
        }
    }



    public <C extends Updater> C getattr(Class<C> c) {
        Updater attr = this.updaters.get(attrclass(c));
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
            updaters.put(c,new Space());
        else if(c == FuelLvl.class)
            updaters.put(c,new FuelLvl());
        else if(c == TargetItems.class)
            updaters.put(c,new TargetItems());
        else if(c == Tetris.class)
            updaters.put(c,new Tetris());
    }

}
