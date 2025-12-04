package mapv4;

import haven.*;
import haven.res.ui.obj.buddy.Buddy;
import nurgling.NAlarmManager;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.NTask;
import nurgling.tools.Finder;
import nurgling.tools.NParser;
import nurgling.widgets.NAlarmWdg;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Requestor implements Action {
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int PREPGRID_RETRY_LIMIT = 3;
    
    public final BlockingQueue<MapperTask> list = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private final Map<String, Integer> prepGridRetries = new HashMap<>();
    NMappingClient parent;
    
    public Requestor(NMappingClient parent) {
        this.parent = parent;
    }



    public class MapperTask
    {
        String type;
        Object[] args;

        public MapperTask(String type, Object[] args) {
            this.type = type;
            this.args = args;
        }

        @Override
        public String toString() {
            return "MapperTask{" +
                    "type='" + type + '\'' +
                    '}';
        }
    }




    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (!parent.done.get()) {
            MapperTask task = list.poll(1, TimeUnit.SECONDS);
            if (task != null) {
                switch (task.type) {
                    case "reqGrid": {
                        String[][] gridMap = NUtils.getGameUI().map.glob.map.constructSection((Coord)task.args[0]);
                        if (gridMap == null) {
                            continue;
                        }
                        JSONObject data = new JSONObject();
                        data.put("grids", gridMap);
                        JSONObject msg = new JSONObject();
                        msg.put("data", data);
                        msg.put("reqMethod", "POST");
                        msg.put("url", (String)NConfig.get(NConfig.Key.endpoint) + "/gridUpdate");
                        msg.put("header", "GRIDREQ");
                        if (!parent.connector.msgs.offer(msg)) {
                            // Queue is full, drop oldest non-critical message
                        }
                        break;
                    }
                    case "prepGrid": {
                        String gridID = (String)task.args[0];
                        MCache.Grid g = (MCache.Grid)task.args[1];
                        if(g != null && NUtils.getGameUI().map.glob != null) {
                            try {
                                BufferedImage image = MinimapImageGenerator.drawmap(NUtils.getGameUI().map.glob.map, g);
                                if(image == null) {
                                    int retries = prepGridRetries.getOrDefault(gridID, 0);
                                    if (retries < PREPGRID_RETRY_LIMIT) {
                                        prepGridRetries.put(gridID, retries + 1);
                                        list.offer(task);
                                    } else {
                                        prepGridRetries.remove(gridID);
                                    }
                                    continue;
                                }
                                prepGridRetries.remove(gridID);
                                
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                ImageIO.write(image, "png", outputStream);
                                ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                                
                                JSONObject data = new JSONObject();
                                data.put("inputStream", inputStream);
                                data.put("gridID", gridID);
                                JSONObject msg = new JSONObject();
                                msg.put("data", data);
                                msg.put("reqMethod", "MULTI");
                                msg.put("url", (String)NConfig.get(NConfig.Key.endpoint) + "/gridUpload");
                                msg.put("header", "GRIDUPLOAD");
                                
                                if (!parent.connector.msgs.offer(msg)) {
                                    // Queue is full, image generation wasted but avoids blocking
                                }
                            } catch (IOException e) {
                                // Failed to generate image, don't retry
                                prepGridRetries.remove(gridID);
                            }
                        }
                        break;
                    }
                    case "track":
                    {
                        Gob player = NUtils.player();

                        if(player != null) {
                            MCache.Grid g = null;
                            try {
                                g = NUtils.getGameUI().map.glob.map.getgrid(NUtils.toGC(player.rc));

                            }
                            catch (MCache.LoadingMap e) {
                            }

                            if(g == null) {
                                continue;
                            }
                            Coord2d coords = NUtils.gridOffset(player.rc);
                            JSONObject data = new JSONObject();
                            JSONObject prop = new JSONObject();
                            prop.put("name", NUtils.getGameUI().chrid);
                            prop.put("type", "player");
                            prop.put("gridID", String.valueOf(g.id));
                            JSONObject c = new JSONObject();
                            c.put("x", (int) (coords.x / MCache.tilesz.x));
                            c.put("y", (int) (coords.y / MCache.tilesz.y));
                            prop.put("coords", c);
                            data.put(String.valueOf(player.id), prop);

                            for(Long id: NAlarmWdg.borkas)
                            {
                                Gob borka = Finder.findGob(id);
                                if(borka!=null)
                                {
                                    String pose=borka.pose();
                                    if(pose == null)
                                    {
                                        continue;
                                    }
                                    else
                                    {
                                        if(NParser.checkName(pose, "dead"))
                                            continue;
                                    }
                                    MCache.Grid gb = NUtils.getGameUI().map.glob.map.getgrid(NUtils.toGC(player.rc));
                                    if(gb == null) {
                                        continue;
                                    }
                                    JSONObject propb = new JSONObject();
                                    propb.put("name", "???");
                                    propb.put("type", "white");
                                    Buddy buddy = borka.getattr(Buddy.class);
                                    if (buddy != null &&  buddy.b!=null) {
                                        propb.put("name", buddy.b.name);
                                        propb.put("type", Integer.toHexString(BuddyWnd.gc[buddy.b.group].getRGB()));
                                    }
                                    propb.put("gridID", String.valueOf(gb.id));
                                    JSONObject cb = new JSONObject();
                                    Coord2d coordsb = NUtils.gridOffset(borka.rc);
                                    cb.put("x", (int) (coordsb.x / MCache.tilesz.x));
                                    cb.put("y", (int) (coordsb.y / MCache.tilesz.y));
                                    propb.put("coords", cb);
                                    data.put(String.valueOf(borka.id), propb);
                                }
                            }
                            JSONObject msg = new JSONObject();
                            msg.put("data", data);
                            msg.put("reqMethod", "POST");
                            msg.put("url", (String)NConfig.get(NConfig.Key.endpoint) + "/positionUpdate");
                            msg.put("header", "TRACKING");
                            if (!parent.connector.msgs.offer(msg)) {
                                // Queue full, tracking update dropped
                            }
                        }
                        break;
                    }
                    case "processMap":
                    {
                        MapFile mapfile = (MapFile)task.args[0];
                        @SuppressWarnings("unchecked")
                        Predicate<MapFile.Marker> uploadCheck = (Predicate<MapFile.Marker>)task.args[1];
                        if(mapfile.lock.readLock().tryLock()) {
                            List<MarkerData> markers = mapfile.markers.stream().filter(uploadCheck).map(m -> {
                                Coord mgc = new Coord(Math.floorDiv(m.tc.x, 100), Math.floorDiv(m.tc.y, 100));
                                MapFile.Segment.ByCoord indirGrid = (MapFile.Segment.ByCoord)mapfile.segments.get(m.seg).grid(mgc);
                                return new MarkerData(m, indirGrid);
                            }).collect(Collectors.toList());
                            mapfile.lock.readLock().unlock();
                            ArrayList<JSONObject> loadedMarkers = new ArrayList<>();
                            for (int i = 0; i < markers.size(); i++)
                            {
                                try
                                {
                                    MarkerData md = markers.get(i);
                                    if (md.indirGrid.get() == null)
                                        continue;
                                    Coord mgc = new Coord(Math.floorDiv(md.m.tc.x, 100), Math.floorDiv(md.m.tc.y, 100));
                                    NUtils.addTask(new NTask() {
                                        int count = 0;
                                        @Override
                                        public boolean check() {
                                            if(count++>=200)
                                                return true;
                                            return ((MapFile.Segment.ByCoord)md.indirGrid).cur!=null && ((MapFile.Segment.ByCoord)md.indirGrid).cur.loading.done();
                                        }

                                        @Override
                                        public String toString() {
                                            return "Requester2: ((MapFile.Segment.ByCoord)md.indirGrid).cur!=null && ((MapFile.Segment.ByCoord)md.indirGrid).cur.loading.done()" + String.valueOf(((MapFile.Segment.ByCoord)md.indirGrid).cur!=null) +String.valueOf( ((MapFile.Segment.ByCoord)md.indirGrid).cur.loading.done()) ;
                                        }
                                    });
                                    if(!(((MapFile.Segment.ByCoord)md.indirGrid).cur!=null && ((MapFile.Segment.ByCoord)md.indirGrid).cur.loading.done()))
                                        continue;
                                    long gridId = ((MapFile.Segment.ByCoord)md.indirGrid).cur.get().id;
                                    JSONObject o = new JSONObject();
                                    o.put("name", md.m.nm);
                                    o.put("gridID", String.valueOf(gridId));
                                    Coord gridOffset = md.m.tc.sub(mgc.mul(100));
                                    o.put("x", gridOffset.x);
                                    o.put("y", gridOffset.y);

                                    if(md.m instanceof MapFile.SMarker) {
                                        o.put("type", "shared");
                                        o.put("id", ((MapFile.SMarker) md.m).oid);
                                        o.put("image", ((MapFile.SMarker) md.m).res.name);
                                    } else if(md.m instanceof MapFile.PMarker) {
                                        o.put("type", "player");
                                        o.put("color", ((MapFile.PMarker) md.m).color);
                                    }
                                    loadedMarkers.add(o);
                                }
                                catch (Exception ex)
                                {
                                    // maybe some logging here someday...
                                }
                            }
                            JSONObject msg = new JSONObject();
                            msg.put("data", new JSONArray(loadedMarkers.toArray()));
                            msg.put("reqMethod", "POST");
                            msg.put("url", (String)NConfig.get(NConfig.Key.endpoint) + "/markerUpdate");
                            msg.put("header", "MARKERS");
                            if (!parent.connector.msgs.offer(msg)) {
                                // Queue full, markers update dropped
                            }
                        }
                        break;
                    }
                    case "uploadMarker":
                    {
                        Gob gob = (Gob)task.args[0];
                        MapFile.SMarker marker = (MapFile.SMarker)task.args[1];
                        try {
                            MCache.Grid grid = NUtils.getGameUI().map.glob.map.getgrid(NUtils.toGC(gob.rc));
                            Coord offset = NUtils.gridOffset2(gob.rc);

                            JSONObject obj = new JSONObject();
                            obj.put("name", marker.nm);
                            obj.put("gridID", String.valueOf(grid.id));
                            obj.put("x", offset.x);
                            obj.put("y", offset.y);
                            obj.put("type", "shared");
                            obj.put("id", marker.oid);
                            obj.put("image", marker.res.name);

                            JSONObject msg = new JSONObject();
                            msg.put("data", new JSONArray(List.of(obj)));
                            msg.put("reqMethod", "POST");
                            msg.put("url", (String)NConfig.get(NConfig.Key.endpoint) + "/markerUpdate");
                            msg.put("header", "SMARKER");
                            if (!parent.connector.msgs.offer(msg)) {
                                // Queue full, marker update dropped
                            }
                        } catch (Exception ignored) {
                        }
                        break;
                    }
                    case "overlayUpload":
                    {
                        long gridId = (Long) task.args[0];
                        MCache.Grid grid = (MCache.Grid) task.args[1];

                        if (grid == null) {
                            continue;
                        }

                        List<OverlayData> overlays = OverlayExtractor.extractOverlays(grid, gridId);
                        if (overlays.isEmpty()) {
                            continue;
                        }

                        // Check if changed since last send
                        int hash = OverlayExtractor.computeHash(overlays);
                        if (!parent.hasOverlayChanged(gridId, hash)) {
                            continue;
                        }

                        // Build JSON payload
                        JSONObject data = new JSONObject();
                        data.put("gridId", String.valueOf(gridId));
                        JSONArray overlayArray = new JSONArray();
                        for (OverlayData ol : overlays) {
                            overlayArray.put(ol.toJSON());
                        }
                        data.put("overlays", overlayArray);

                        // Queue for sending
                        JSONObject msg = new JSONObject();
                        msg.put("data", data);
                        msg.put("reqMethod", "POST");
                        msg.put("url", (String) NConfig.get(NConfig.Key.endpoint) + "/overlayUpload");
                        msg.put("header", "OVERLAY");

                        if (!parent.connector.msgs.offer(msg)) {
                            // Queue full, overlay update dropped
                        }
                        break;
                    }
                }
            }
        }
        return Results.SUCCESS();
    }

    public void senGridRequest(Coord lastGC) {
        list.offer(new MapperTask("reqGrid", new Object[]{lastGC}));
    }

    public void prepGrid(String string, MCache.Grid g) {
        list.offer(new MapperTask("prepGrid", new Object[]{string, g}));
    }

    public void track() {
        // Check if track task already exists to avoid duplicates
        for(MapperTask task : list) {
            if(task.type.equals("track")) {
                return;
            }
        }
        list.offer(new MapperTask("track", null));
    }

    public void processMap(MapFile mapfile, Predicate<MapFile.Marker> uploadCheck) {
        list.offer(new MapperTask("processMap", new Object[]{mapfile, uploadCheck}));
    }

    public void uploadSMarker(Gob gob, MapFile.SMarker marker) {
        list.offer(new MapperTask("uploadMarker", new Object[]{gob, marker}));
    }

    public void sendOverlayUpdate(long gridId, MCache.Grid grid) {
        // Avoid duplicate tasks for the same grid
        for (MapperTask task : list) {
            if (task.type.equals("overlayUpload") &&
                task.args != null &&
                task.args.length > 0 &&
                Long.valueOf(gridId).equals(task.args[0])) {
                return;
            }
        }
        list.offer(new MapperTask("overlayUpload", new Object[]{gridId, grid}));
    }

    private class MarkerData {
        MapFile.Marker m;
        Indir<MapFile.Grid> indirGrid;

        MarkerData(MapFile.Marker m, Indir<MapFile.Grid> indirGrid) {
            this.m = m;
            this.indirGrid = indirGrid;
        }
    }
}
