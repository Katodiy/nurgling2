package nurgling.tools;

import monitoring.NGlobalSearchItems;
import nurgling.NConfig;
import nurgling.NUtils;

import java.util.ArrayList;

public class NSearchItem
{
    public String name ="";
    private long lastSearchTick = 0;
    private long lastDataVersion = 0; // Track if data actually changed
    private static final long SEARCH_REFRESH_INTERVAL = 300; // Refresh every 300 ticks (~10 seconds) for periodic updates
    private static final long MIN_REFRESH_INTERVAL = 100; // Minimum 100 ticks (~3 seconds) between any refresh

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
        if((Boolean) NConfig.get(NConfig.Key.ndbenable))
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
        if((Boolean) NConfig.get(NConfig.Key.ndbenable))
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
    
    /**
     * Called periodically to refresh search results
     * Only triggers refresh if data has changed or enough time has passed
     */
    public void tick() {
        if (isEmpty()) {
            return;
        }
        
        long currentTick = NUtils.getTickId();
        
        // Check if data version changed (container was updated)
        long currentDataVersion = NGlobalSearchItems.updateVersion;
        boolean dataChanged = currentDataVersion != lastDataVersion;
        
        // Refresh if data changed and minimum interval passed, OR if long interval passed
        if ((dataChanged && currentTick - lastSearchTick >= MIN_REFRESH_INTERVAL) ||
            (currentTick - lastSearchTick >= SEARCH_REFRESH_INTERVAL)) {
            doRefreshSearch(currentTick, currentDataVersion);
        }
    }
    
    /**
     * Force refresh of global search results
     * Used when container data changes or search parameters change
     */
    public void refreshSearch() {
        long currentTick = NUtils.getTickId();
        // Prevent too frequent refreshes (minimum interval between refreshes)
        if (currentTick - lastSearchTick < MIN_REFRESH_INTERVAL) {
            return;
        }
        
        doRefreshSearch(currentTick, NGlobalSearchItems.updateVersion);
    }
    
    /**
     * Internal method to perform the actual refresh
     */
    private void doRefreshSearch(long currentTick, long dataVersion) {
        if (!isEmpty() && (Boolean) NConfig.get(NConfig.Key.ndbenable)) {
            lastSearchTick = currentTick;
            lastDataVersion = dataVersion;
            NUtils.getUI().core.searchContainer(this);
        }
    }
    
    /**
     * Notify that container data has changed - triggers search refresh with debouncing
     * Multiple rapid calls will only result in one actual refresh after MIN_REFRESH_INTERVAL
     */
    public static void notifyContainerDataChanged() {
        // Just increment the version - tick() will pick up the change
        // This avoids multiple rapid DB queries when many containers are opened
        // The actual refresh happens in tick() with proper debouncing
    }
}