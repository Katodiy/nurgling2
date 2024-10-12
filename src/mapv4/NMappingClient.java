package mapv4;

import haven.Coord;
import haven.Gob;
import haven.WebBrowser;
import nurgling.NConfig;
import nurgling.NUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class NMappingClient {
    final HashSet<String> requestedGrid = new HashSet<>();
    Coord lastGC = Coord.z;

    Connector connector;
    public Requestor requestor;
    public AtomicBoolean done = new AtomicBoolean(false);
    private Boolean autoMapper = null;
    public final Map<Long, MapRef> cache = new HashMap<Long, MapRef>();
    long lastTracking = -1;
    public void tick(double dt)
    {
        if(autoMapper!=(Boolean)NConfig.get(NConfig.Key.autoMapper) && NUtils.getGameUI()!=null)
        {
            autoMapper = (Boolean)NConfig.get(NConfig.Key.autoMapper);
            if(autoMapper)
            {
                done.set(false);

                new Thread(new Runnable()
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
                        }
                    }
                }, "requestor").start();

                new Thread(new Runnable()
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
                        }
                    }
                }, "connector").start();
            }
            else
            {
                done = new AtomicBoolean(true);
            }

        }

        Gob player = NUtils.player();
        if(player!=null) {

            Coord current = NUtils.toGC(player.rc);
            if(!current.equals(lastGC)) {
                lastGC = current;
                requestor.senGridRequest(lastGC);
            }

            long currentTime = System.currentTimeMillis();
            if(currentTime-lastTracking>2000)
            {
                lastTracking = currentTime;
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
        Coord gc = NUtils.toGC(player.rc);
        synchronized (cache) {
            long id = NUtils.getGameUI().map.glob.map.getgrid(gc).id;
            return cache.get(id);
        }
    }


    public void OpenMap() {
        MapRef mapRef = GetMapRef();
        if (GetMapRef() != null) {
            try {
                WebBrowser.self.show(new URL(
                        String.format((String) NConfig.get(NConfig.Key.endpoint) + "/#/grid/%d/%d/%d/6", mapRef.mapID, mapRef.gc.x, mapRef.gc.y)));
            } catch (MalformedURLException e) {
            }
        }
        else
        {
            NUtils.getGameUI().error("Can't open Map");
        }
    }
}
