package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Window;
import nurgling.*;
import nurgling.actions.PathFinder;
import nurgling.actions.bots.RouteAutoRecorder;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;
import nurgling.tools.RouteCreator;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class RoutesWidget extends Window {
    public String currentPath = "";

    public RouteList routeList;
    private final List<RouteItem> routeItems = new ArrayList<>();
    private WaypointList waypointList;
    private Widget actionContainer;
    public final HashMap<Integer, Route> routes = new HashMap<>();

    public RoutesWidget() {
        super(UI.scale(new Coord(300, 400)), "Routes");

        Coord p = new Coord(0, UI.scale(5));

        IButton createBtn = add(new IButton(NStyle.add[0].back, NStyle.add[1].back, NStyle.add[2].back) {
            @Override
            public void click() {
                NUtils.getGameUI().msg("Creating Route");
                new Thread(new RouteCreator()).start();
            }
        }, p);
        createBtn.settip("Create new route");

        IButton importBtn = add(new IButton(NStyle.importb[0].back, NStyle.importb[1].back, NStyle.importb[2].back) {
            @Override
            public void click() {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("Route files", "json"));
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    if (file != null) {
                        NUtils.getGameUI().msg("Loaded route: " + file.getName());
                        // TODO: Actual loading logic
                    }
                }
            }
        }, createBtn.pos("ur").adds(UI.scale(5, 0)));
        importBtn.settip("Import route");

        IButton deleteBtn = add(new IButton(NStyle.remove[0].back, NStyle.remove[1].back, NStyle.remove[2].back) {
            @Override
            public void click() {
                if (routeList.sel != null) {
                    Route toRemove = routeList.sel.route;
                    routeList.sel.deleteSelectedRoute();
                }
            }
        }, importBtn.pos("ur").adds(UI.scale(5, 0)));
        deleteBtn.settip("Delete selected route");

        routeList = add(new RouteList(UI.scale(new Coord(150, 120))), createBtn.pos("bl").adds(0, 10));

        add(new Label("Actions:", NStyle.areastitle), routeList.pos("ur").add(UI.scale(20, 0)));
        actionContainer = add(new Widget(UI.scale(new Coord(20, 120))), routeList.pos("ur").add(UI.scale(20, 20)));

        Label routeInfoLabel = add(new Label("Route Info:", NStyle.areastitle), routeList.pos("bl").adds(0, UI.scale(10)));
        waypointList = add(new WaypointList(UI.scale(new Coord(250, 120))), routeInfoLabel.pos("bl").adds(0, UI.scale(5)));

        pack();
    }

    @Override
    public void show() {
        loadRoutes();
        showRoutes();
        super.show();
    }

    @Override
    public boolean show(boolean show) {
        if (!show && NUtils.getGameUI() != null && NUtils.getGameUI().map != null)
            ((NMapView) NUtils.getGameUI().map).destroyRouteDummys();
        return super.show(show);
    }

    public void showRoutes() {
        synchronized (routeItems) {
            routeItems.clear();
            for (Route route : routes.values()) {
                routeItems.add(new RouteItem(route));
            }
        }

        if (!routeItems.isEmpty()) {
            routeList.change(routeItems.get(routeItems.size() - 1));
        }
    }

    public void loadRoutes() {
        if(new File(NConfig.current.path_routes).exists())
        {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_routes), StandardCharsets.UTF_8))
            {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            }
            catch (IOException ignore)
            {
            }

            if (!contentBuilder.toString().isEmpty())
            {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = (JSONArray) main.get("routes");
                for (int i = 0; i < array.length(); i++)
                {
                    Route route = new Route((JSONObject) array.get(i));
                    routes.put(route.id, route);
                }
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null)
                ((NMapView) NUtils.getGameUI().map).destroyRouteDummys();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private void select(int id) {
        Route route = routes.get(id);

        if (route == null) return;

        ((NMapView) NUtils.getGameUI().map).initRouteDummys(id);

        // Button: Manual waypoint recording
        actionContainer.add(new IButton(NStyle.add[0].back, NStyle.add[1].back, NStyle.add[2].back) {
            @Override
            public void click() {
                NUtils.getGameUI().msg("Recording position for: " + route.name);
                route.addWaypoint();
                waypointList.update(route.waypoints);
            }
        }, Coord.z).settip("Record Position");

        // Button: Auto waypoint recorder bot
        final RouteAutoRecorder[] recorder = {null};
        final Thread[] thread = {null};
        final boolean[] active = {false};

        actionContainer.add(new IButton(NStyle.flipi[0].back, NStyle.flipi[1].back, NStyle.flipi[2].back) {
            @Override
            public void click() {
                if (!active[0]) {
                    recorder[0] = new RouteAutoRecorder(route);
                    thread[0] = new Thread(recorder[0], "RouteAutoRecorder");
                    thread[0].start();
                    NUtils.getGameUI().biw.addObserve(thread[0]);
                    NUtils.getGameUI().msg("Started route recording for: " + route.name);
                } else {
                    if (recorder[0] != null) {
                        recorder[0].stop();
                        thread[0].interrupt();
                        NUtils.getGameUI().msg("Stopped route recording for: " + route.name);
                    }
                }
                active[0] = !active[0];
            }
        }, new Coord(0, UI.scale(25))).settip("Start/Stop Auto Waypoint Bot");

        waypointList.update(route.waypoints);
    }


    public int getSelectedRouteId() {
        return this.routeList.selectedRouteId;
    }

    public void updateWaypoints() {
        int routeId = this.routeList.selectedRouteId;
        Route route = routes.get(routeId);

        if (route == null) {
            this.waypointList.update(new ArrayList<>());
            return;
        }

        ArrayList<RoutePoint> waypoints = routes.get(routeId).waypoints;
        if (waypoints != null) {
            this.waypointList.update(waypoints);
        } else {
            this.waypointList.update(new ArrayList<>());
        }
    }

    public class RouteList extends SListBox<RouteItem, Widget> {
        private int selectedRouteId;

        public RouteList(Coord sz) {
            super(sz, UI.scale(20));
        }

        @Override
        protected List<RouteItem> items() {
            synchronized (routeItems) {
                return routeItems;
            }
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(UI.scale(150) - UI.scale(6), sz.y));
        }

        @Override
        protected Widget makeitem(RouteItem item, int idx, Coord sz) {
            return new ItemWidget<RouteItem>(this, sz, item) {{
                add(item);
            }
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b == 3) {
                        // Right-click anywhere on the line triggers context menu
                        routeList.change(item);
                        item.opts(ev.c);
                        return true;
                    } else if (ev.b == 1) {
                        list.change(item);
                        return true;
                    }
                    return super.mousedown(ev);
                }
            };
        }

        Color bg = new Color(30, 40, 40, 160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }

        @Override
        public void change(RouteItem item) {
            super.change(item);
            if (item != null) {
                actionContainer.show();
                waypointList.show();
                this.selectedRouteId = item.route.id;
                RoutesWidget.this.select(selectedRouteId);
            }
        }
    }

    public class RouteItem extends Widget {
        Label label;
        Route route;
        NFlowerMenu menu;

        public RouteItem(Route route) {
            this.route = route;
            this.label = add(new Label(route.name));
            sz = label.sz.add(UI.scale(6), UI.scale(4));
        }

        @Override
        public void draw(GOut g) {
            if (routeList.sel == this) {
                g.chcolor(0, 0, 0, 0);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
            super.draw(g);
        }

        public void opts(Coord c) {
            if (menu == null) {
                menu = new NFlowerMenu(new String[]{"Edit name", "Delete"}) {
                    @Override
                    public boolean mousedown(MouseDownEvent ev) {
                        if (super.mousedown(ev))
                            nchoose(null);
                        return true;
                    }

                    @Override
                    public void nchoose(NPetal option) {
                        if (option != null) {
                            if (option.name.equals("Edit name")) {
                                NEditRouteName.openChangeName(route, RouteItem.this);
                            } else if (option.name.equals("Delete")) {
                                deleteSelectedRoute();
                            }
                        }
                        uimsg("cancel");
                    }

                    @Override
                    public void destroy() {
                        menu = null;
                        super.destroy();
                    }
                };
            }

            Widget par = parent;
            Coord pos = c;
            while (par != null && !(par instanceof GameUI)) {
                pos = pos.add(par.c);
                par = par.parent;
            }
            ui.root.add(menu, pos.add(UI.scale(25, 38)));
        }

        public void deleteSelectedRoute() {
            routes.remove(routeList.sel.route.id);
            NConfig.needRoutesUpdate();
            showRoutes();
            if (routes.isEmpty()) {
                actionContainer.hide();
                waypointList.hide();
            }
        }
    }

    public class WaypointList extends SListBox<CoordItem, Widget> {
        private final List<CoordItem> items = new ArrayList<>();

        WaypointList(Coord sz) {
            super(sz, UI.scale(16));
        }

        public void update(List<RoutePoint> points) {
            synchronized (items) {
                items.clear();
                for (RoutePoint point : points) {
                    items.add(new CoordItem(point.gridId, point.toCoord2d(NUtils.getGameUI().map.glob.map), point));
                }
                NConfig.needRoutesUpdate();
            }
        }

        @Override
        protected List<CoordItem> items() {
            return items;
        }

        @Override
        protected Widget makeitem(CoordItem item, int idx, Coord sz) {
            return new ItemWidget<CoordItem>(this, sz, item) {{ add(item); }
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b == 1) {
                        Thread t;
                        (t = new Thread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    if (NUtils.getGameUI() != null) {
                                        Coord2d coord = item.routePoint.toCoord2d(NUtils.getGameUI().map.glob.map);
                                        if (coord != null) {
                                            new PathFinder(item.localCoord).run(NUtils.getGameUI());
                                        } else {
                                            NUtils.getGameUI().msg("Coordinates not found. Likely too far.");
                                        }
                                    }
                                }
                                catch (InterruptedException e)
                                {
                                    NUtils.getGameUI().msg("Interrupted");
                                }
                            }
                        }, "Auto craft(BOT)")).start();

                        NUtils.getGameUI().biw.addObserve(t);
                        return true;
                    }
                    return super.mousedown(ev);
                }
            };
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(new Color(30, 40, 40, 160));
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }
    }

    public class CoordItem extends Widget {
        private final Label label;
        private final Coord2d localCoord;
        private final RoutePoint routePoint;

        public CoordItem(long gridid, Coord2d coord, RoutePoint routePoint) {
            this.label = add(new Label(String.valueOf(gridid)));
            this.sz = label.sz.add(UI.scale(4), UI.scale(4));
            this.localCoord = coord;
            this.routePoint = routePoint;
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
        }
    }
}
