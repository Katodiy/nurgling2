package mapv4;

import haven.Coord;
import haven.Gob;
import haven.MapFile;
import haven.WebBrowser;
import nurgling.NConfig;
import nurgling.NUtils;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class NMappingClient {
    private static final int MAX_CACHE_SIZE = 10000;
    private static final long CACHE_CLEANUP_INTERVAL_MS = 300000; // 5 minutes
    
    Coord lastGC = Coord.z;

    public Connector connector;
    public Requestor requestor;
    public AtomicBoolean done = new AtomicBoolean(false);
    private Boolean autoMapper = null;
    public final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<Long, Integer> overlayHashes = new ConcurrentHashMap<>();
    private volatile boolean overlaySupported = true;
    public Thread reqTread = null;
    public Thread conTread = null;
    long lastTracking = -1;
    private long lastCacheCleanup = System.currentTimeMillis();
    
    static class CacheEntry {
        final MapRef mapRef;
        long lastAccess;
        
        CacheEntry(MapRef mapRef) {
            this.mapRef = mapRef;
            this.lastAccess = System.currentTimeMillis();
        }
    }
    public void tick(double dt)
    {
        if(NUtils.getGameUI()!=null && (autoMapper!=(Boolean)NConfig.get(NConfig.Key.autoMapper) || autoMapper && ((reqTread == null || !reqTread.isAlive()) && (conTread == null || !conTread.isAlive()))))
        {
            Boolean newState = (Boolean)NConfig.get(NConfig.Key.autoMapper);
            if(newState != null && !newState.equals(autoMapper)) {
                autoMapper = newState;
                if(autoMapper)
                {
                    done.set(false);

                    (reqTread = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                requestor.run(NUtils.getGameUI());
                            }
                            catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }, "automapper-requestor")).start();

                    (conTread = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                connector.run(NUtils.getGameUI());
                            }
                            catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }, "automapper-connector")).start();
                }
                else
                {
                    done.set(true);
                    if(reqTread != null) {
                        reqTread.interrupt();
                    }
                    if(conTread != null) {
                        conTread.interrupt();
                    }
                }
            }
        }
        
        // Periodic cache cleanup
        long currentTimeMs = System.currentTimeMillis();
        if(currentTimeMs - lastCacheCleanup > CACHE_CLEANUP_INTERVAL_MS) {
            cleanupCache();
            lastCacheCleanup = currentTimeMs;
        }

        Gob player = NUtils.player();
        if(player!=null) {

            Coord current = NUtils.toGC(player.rc);
            if(!current.equals(lastGC)) {
                lastGC = current;
                requestor.senGridRequest(lastGC);
            }

            if(currentTimeMs-lastTracking>2000)
            {
                lastTracking = currentTimeMs;
                requestor.track();
            }
        }
    }

    public NMappingClient() {
        requestor = new Requestor(this);
        connector = new Connector(this);
        done.set(false);
    }

    public static class MapRef {
        public Coord gc;
        public long mapID;

        MapRef(long mapID, Coord gc) {
            this.gc = gc;
            this.mapID = mapID;
        }

        public String toString() {
            return (gc.toString() + " in map space " + mapID);
        }
    }

    public MapRef GetMapRef() {
        Gob player = NUtils.player();
        if(player == null) return null;
        
        Coord gc = NUtils.toGC(player.rc);
        try {
            long id = NUtils.getGameUI().map.glob.map.getgrid(gc).id;
            CacheEntry entry = cache.get(id);
            if(entry != null) {
                entry.lastAccess = System.currentTimeMillis();
                return entry.mapRef;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private void cleanupCache() {
        if(cache.size() <= MAX_CACHE_SIZE) {
            return;
        }

        // Remove oldest entries
        List<Map.Entry<Long, CacheEntry>> entries = new ArrayList<>(cache.entrySet());
        entries.sort(Comparator.comparingLong(e -> e.getValue().lastAccess));

        int toRemove = cache.size() - (MAX_CACHE_SIZE * 3 / 4);
        for(int i = 0; i < toRemove && i < entries.size(); i++) {
            cache.remove(entries.get(i).getKey());
        }

        // Also cleanup overlay hashes if too large
        if (overlayHashes.size() > MAX_CACHE_SIZE) {
            overlayHashes.clear();
        }
    }

    public boolean hasOverlayChanged(long gridId, int newHash) {
        Integer oldHash = overlayHashes.get(gridId);
        if (oldHash == null || !oldHash.equals(newHash)) {
            overlayHashes.put(gridId, newHash);
            return true;
        }
        return false;
    }

    public boolean isOverlaySupported() {
        return overlaySupported;
    }

    public void setOverlayUnsupported() {
        overlaySupported = false;
    }


    public void OpenMap() {
        MapRef mapRef = GetMapRef();
        if (mapRef != null) {
            try {
                String urlString = String.format((String) NConfig.get(NConfig.Key.endpoint) + "/#/grid/%d/%d/%d/6", 
                    mapRef.mapID, mapRef.gc.x, mapRef.gc.y);
                WebBrowser.self.show(URI.create(urlString).toURL());
            } catch (Exception e) {
                NUtils.getGameUI().error("Invalid URL: " + e.getMessage());
            }
        }
        else
        {
            NUtils.getGameUI().error("Can't open Map");
        }
    }

    public void uploadSMarker(Gob gob, MapFile.SMarker marker) {
        if (requestor != null) {
            requestor.uploadSMarker(gob, marker);
        }
    }
}
