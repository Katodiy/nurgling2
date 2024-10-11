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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Requestor implements Action {
    final LinkedList<MapperTask> list = new LinkedList<MapperTask>();
    NMappingClient parent;
    public Requestor(NMappingClient parent) {
        this.parent = parent;
    }



    class MapperTask
    {
        String type;
        Object[] args;

        public MapperTask(String type, Object[] args) {
            this.type = type;
            this.args = args;
        }
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (!parent.done.get()) {
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    synchronized (list) {
                        return !list.isEmpty() || parent.done.get();
                    }
                }
            });
            if(parent.done.get())
                return Results.SUCCESS();
            MapperTask task;
            synchronized ( list ) {
                task = list.poll();
            }
            if (task != null) {
                switch (task.type) {
                    case "reqGrid": {
                        String[][] gridMap = NUtils.getGameUI().map.glob.map.constructSection((Coord)task.args[0]);
                        if (gridMap == null) {
                            continue;
                        }
                        else
                        {
                            JSONObject data = new JSONObject();
                            data.put("grids", gridMap);
                            JSONObject msg = new JSONObject();
                            msg.put("data", data);
                            msg.put("reqMethod", "POST");
                            msg.put("url", (String)NConfig.get(NConfig.Key.endpoint) + "/gridUpdate");
                            msg.put("header", "GRIDREQ");
                            synchronized (parent.connector.msgs)
                            {
                                parent.connector.msgs.add(msg);
                            }
                        }
                        break;
                    }
                    case "prepGrid": {
                        MCache.Grid g = (MCache.Grid)task.args[1];
                        if(g != null && NUtils.getGameUI().map.glob != null) {
                            try {
                            BufferedImage image = MinimapImageGenerator.drawmap(NUtils.getGameUI().map.glob.map, g);
                            if(image == null) {
                                synchronized ( list ) {
                                    list.add(task);
                                }
                                continue;
                            }
                            JSONObject extraData = new JSONObject();
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            ImageIO.write(image, "png", outputStream);
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                                JSONObject data = new JSONObject();
                                data.put("inputStream", inputStream);
                                data.put("gridID", (String)task.args[0]);
                                JSONObject msg = new JSONObject();
                                msg.put("data", data);
                                msg.put("reqMethod", "MULTI");
                                msg.put("url", (String)NConfig.get(NConfig.Key.endpoint) + "/gridUpload");
                                msg.put("header", "GRIDUPLOAD");
                                synchronized (parent.connector.msgs)
                                {
                                    parent.connector.msgs.add(msg);
                                }
                            } catch (IOException e) {
                            }
                        }
                        break;
                    }
                    case "track":
                    {
                        Gob player = NUtils.player();

                        if(player != null) {
                            MCache.Grid g = NUtils.getGameUI().map.glob.map.getgrid(NUtils.toGC(player.rc));
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
                            synchronized (parent.connector.msgs)
                            {
                                parent.connector.msgs.add(msg);
                            }
                        }
                        break;
                    }
                    case "processMap":
                    {
                        MapFile mapfile = (MapFile)task.args[0];
                        Predicate<MapFile.Marker> uploadCheck = (Predicate<MapFile.Marker>)task.args[1];
                        if(mapfile.lock.readLock().tryLock()) {
                            List<MarkerData> markers = mapfile.markers.stream().filter(uploadCheck).map(m -> {
                                Coord mgc = new Coord(Math.floorDiv(m.tc.x, 100), Math.floorDiv(m.tc.y, 100));
                                MapFile.Segment.ByCoord indirGrid = (MapFile.Segment.ByCoord)mapfile.segments.get(m.seg).grid(mgc);
                                return new MarkerData(m, indirGrid);
                            }).collect(Collectors.toList());
                            mapfile.lock.readLock().unlock();
                            ArrayList<JSONObject> loadedMarkers = new ArrayList<>();
                            for (MarkerData md : markers)
                            {
                                Coord mgc = new Coord(Math.floorDiv(md.m.tc.x, 100), Math.floorDiv(md.m.tc.y, 100));
                                NUtils.addTask(new NTask() {
                                    @Override
                                    public boolean check() {
                                        return ((MapFile.Segment.ByCoord)md.indirGrid).cur!=null && ((MapFile.Segment.ByCoord)md.indirGrid).cur.loading.done();
                                    }
                                });
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
                            JSONObject msg = new JSONObject();
                            msg.put("data", new JSONArray(loadedMarkers.toArray()));
                            msg.put("reqMethod", "POST");
                            msg.put("url", (String)NConfig.get(NConfig.Key.endpoint) + "/markerUpdate");
                            msg.put("header", "MARKERS");
                            synchronized (parent.connector.msgs)
                            {
                                parent.connector.msgs.add(msg);
                            }
                        }
                        break;
                    }
                }
            }
        }
        return Results.SUCCESS();
    }

    public void senGridRequest(Coord lastGC) {
        synchronized ( list ) {
            list.add(new MapperTask("reqGrid", new Object[]{lastGC}));
        }
    }


    public void prepGrid(String string, MCache.Grid g) {
        synchronized ( list ) {
            list.add(new MapperTask("prepGrid", new Object[]{string, g}));
        }
    }

    public void track() {
        synchronized ( list ) {
            list.add(new MapperTask("track", null));
        }
    }

    public void processMap(MapFile mapfile, Predicate<MapFile.Marker> uploadCheck) {
        synchronized ( list ) {
            list.add(new MapperTask("processMap", new Object[]{mapfile, uploadCheck}));
        }
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
