package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Window;
import nurgling.*;
import nurgling.actions.AddHearthFire;
import nurgling.actions.bots.RouteAutoRecorder;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.routes.Route;
import nurgling.routes.RouteGraphManager;
import nurgling.routes.RoutePoint;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RoutesWidget extends Window {
    public String currentPath = "";

    public RouteList routeList;
    private HearthfireWaypointList hearthfireWaypointList;
    private final List<RouteItem> routeItems = new ArrayList<>();
    private WaypointList waypointList;
    private Widget actionContainer;
    private SpecList specList;

    public RoutesWidget() {
        super(UI.scale(new Coord(300, 400)), "Routes");

        Coord p = new Coord(0, UI.scale(5));

        IButton createBtn = add(new IButton(NStyle.add[0].back, NStyle.add[1].back, NStyle.add[2].back) {
            @Override
            public void click() {
                ((NMapView) NUtils.getGameUI().map).addRoute();
                NConfig.needRoutesUpdate();
                showRoutes();
            }
        }, p);
        createBtn.settip("Create new route");

        IButton importBtn = add(new IButton(NStyle.importb[0].back, NStyle.importb[1].back, NStyle.importb[2].back) {
            @Override
            public void click() {
                super.click();
                java.awt.EventQueue.invokeLater(() -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileNameExtensionFilter("Route setting file", "json"));
                    if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                        return;
                    if(fc.getSelectedFile()!=null)
                    {
                        // I think merging won't work for routes. It works for areas, but routes require connection
                        // creation at the time of recording. Existing routes will not connect to new routes leading
                        // to problems. Should we overwrite existing routes with incoming routes? We could warn the user
                        // with a popup.
//                        NUtils.getUI().core.config.mergeRoutes(fc.getSelectedFile());
                    }
                    RoutesWidget.this.hide();
                    RoutesWidget.this.show();
                    NConfig.needRoutesUpdate();
                });

            }
        }, createBtn.pos("ur").adds(UI.scale(5, 0)));
        importBtn.settip("Import route");

        IButton exportBtn;
        add(exportBtn = new IButton(NStyle.exportb[0].back,NStyle.exportb[1].back,NStyle.exportb[2].back){
            @Override
            public void click()
            {
                super.click();
                java.awt.EventQueue.invokeLater(() -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileNameExtensionFilter("Routes setting file", "json"));
                    if(fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                        return;
                    NUtils.getUI().core.config.writeRoutes(fc.getSelectedFile().getAbsolutePath()+".json");
                });
            }
        },importBtn.pos("ur").adds(UI.scale(5,0)));
        exportBtn.settip("Export");

        IButton deleteBtn = add(new IButton(NStyle.remove[0].back, NStyle.remove[1].back, NStyle.remove[2].back) {
            @Override
            public void click() {
                if (routeList.sel != null) {
                    Route toRemove = routeList.sel.route;
                    routeList.sel.deleteSelectedRoute();
                }
            }
        }, exportBtn.pos("ur").adds(UI.scale(5, 0)));
        deleteBtn.settip("Delete selected route");

        Label routeListLabel = add(new Label("Routes:", NStyle.areastitle), createBtn.pos("bl").adds(0, 5));
        routeList = add(new RouteList(UI.scale(new Coord(250, 190))), routeListLabel.pos("bl").adds(0, 5));

        Label actionsListLabel = add(new Label("Actions:", NStyle.areastitle), routeListLabel.pos("ur").add(UI.scale(105, 0)));
        actionContainer = add(new Widget(UI.scale(new Coord(120, 30))), actionsListLabel.pos("bl").adds(0, UI.scale(5)));

        CheckBox useHFinGlobalPFBox = add(new CheckBox("Use Hearth Fires in Global PF") {
            {
                a = (Boolean) NConfig.get(NConfig.Key.useHFinGlobalPF);
            }
            public void set(boolean val) {
                NConfig.set(NConfig.Key.useHFinGlobalPF, val);
                a = val;
            }
        }, actionContainer.pos("bl").adds(0, UI.scale(3)));

        // HearthFires label under actions
        Label hearthfireLabel = add(new Label("Hearth Fires:", NStyle.areastitle), useHFinGlobalPFBox.pos("bl").adds(0, UI.scale(5)));
        // Hearthfire waypoint list (custom version of WaypointList, but read-only)
        hearthfireWaypointList = add(new HearthfireWaypointList(UI.scale(new Coord(200, 100))),
                hearthfireLabel.pos("bl").adds(0, UI.scale(5)));

        Label routeInfoLabel = add(new Label("Route Info:", NStyle.areastitle), routeList.pos("bl").adds(0, UI.scale(5)));
        waypointList = add(new WaypointList(UI.scale(new Coord(350, 140))), routeInfoLabel.pos("bl").adds(0, UI.scale(5)));

        Label specLabel = add(new Label("Specializations:", NStyle.areastitle), waypointList.pos("bl").adds(0, UI.scale(5)));
        specList = add(new SpecList(UI.scale(new Coord(350, 50))), specLabel.pos("bl").adds(0, UI.scale(5)));

        pack();

    }

    @Override
    public void show() {
        super.show();
        // Center the window on screen
        if (parent != null) {
            Coord sz = parent.sz;
            Coord c = sz.div(2).sub(this.sz.div(2));
            this.c = c;
        }
        showRoutes();
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
            for (Route route : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().values()) {
                boolean needToAddroute = true;
                for(Route.RouteSpecialization spec : route.spec) {
                    if (spec.name.contains("HearthFires")) {
                        needToAddroute = false;
                        break;
                    }
                }
                if(needToAddroute) {
                    routeItems.add(new RouteItem(route));
                }
            }
        }
        hearthfireWaypointList.updateHearthfireWaypoints();
        if (!routeItems.isEmpty()) {
            routeList.change(routeItems.get(routeItems.size() - 1));
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
                ((NMapView) NUtils.getGameUI().map).destroyRouteDummys();
                NUtils.getGameUI().map.glob.oc.paths.pflines = null;
            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private void select(int id) {
        Route route = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().get(id);

        if (route == null) return;

        ((NMapView) NUtils.getGameUI().map).initRouteDummys(id);

        int x = 0;

        // Button: Auto waypoint recorder bot
        final RouteAutoRecorder[] recorder = {null};
        final Thread[] thread = {null};
        final boolean[] active = {false};
        final IButton[] recordBtnNormal = new IButton[1];
        final IButton[] recordBtnActive = new IButton[1];

        final Runnable[] showActive = new Runnable[1];
        final Runnable[] showNormal = new Runnable[1];

        showNormal[0] = () -> {
            if (recordBtnActive[0] != null) {
                recordBtnActive[0].reqdestroy();
                recordBtnActive[0] = null;
            }
            recordBtnNormal[0] = new IButton(NStyle.record[0].back, NStyle.record[1].back, NStyle.record[2].back) {
                @Override
                public void click() {
                    // Start recording
                    recorder[0] = new RouteAutoRecorder(route);
                    thread[0] = new Thread(recorder[0], "RouteAutoRecorder");
                    thread[0].start();
                    NUtils.getGameUI().biw.addObserve(thread[0]);
                    NUtils.getGameUI().msg("Started route recording for: " + route.name);
                    active[0] = true;
                    // Swap to active button
                    showActive[0].run();
                }
            };
            actionContainer.add(recordBtnNormal[0], new Coord(0, 0)).settip("Start/Stop Auto Waypoint Bot");
        };

        showActive[0] = () -> {
            if (recordBtnNormal[0] != null) {
                recordBtnNormal[0].reqdestroy();
                recordBtnNormal[0] = null;
            }
            recordBtnActive[0] = new IButton(NStyle.record[3].back, NStyle.record[3].back, NStyle.record[3].back) {
                @Override
                public void click() {
                    // Stop recording
                    if (recorder[0] != null) {
                        recorder[0].stop();
                        thread[0].interrupt();
                        NUtils.getGameUI().msg("Stopped route recording for: " + route.name);
                    }
                    active[0] = false;
                    // Swap back to normal button
                    showNormal[0].run();
                }
            };
            actionContainer.add(recordBtnActive[0], new Coord(0, 0)).settip("Recording... (Click to stop)");
        };

        showNormal[0].run();

        x += UI.scale(36);

        // Button: Manual waypoint recording
        actionContainer.add(new IButton(NStyle.add[0].back, NStyle.add[1].back, NStyle.add[2].back) {
            @Override
            public void click() {
                NUtils.getGameUI().msg("Recording position for: " + route.name);
                route.addRandomWaypoint();
                waypointList.update(route.waypoints);
                hearthfireWaypointList.updateHearthfireWaypoints();
            }
        }, new Coord(x, 0)).settip("Record Position");



        x += UI.scale(36);
        actionContainer.add(new IButton(NStyle.hearthFire[0].back, NStyle.hearthFire[1].back, NStyle.hearthFire[2].back) {
            @Override
            public void click() {
                Thread t = new Thread(() -> {
                    try {
                        new AddHearthFire().run(NUtils.getGameUI());
                        hearthfireWaypointList.updateHearthfireWaypoints();
                    } catch (InterruptedException e) {
                        NUtils.getGameUI().error("Failed to add hearth fire");
                    }
                }, "AddHearthFire");
                t.start();
                NUtils.getGameUI().biw.addObserve(t);
            }
        }, new Coord(x, 0)).settip("Add hearth fire");

        waypointList.update(route.waypoints);
        specList.update(route);
    }

    public int getSelectedRouteId() {
        return this.routeList.selectedRouteId;
    }

    public void updateWaypoints() {
        int routeId = this.routeList.selectedRouteId;
        Route route = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().get(routeId);

        if (route == null) {
            this.waypointList.update(new ArrayList<>());
            return;
        }

        ArrayList<RoutePoint> waypoints = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().get(routeId).waypoints;
        if (waypoints != null) {
            this.waypointList.update(waypoints);
            ((NMapView) NUtils.getGameUI().map).routeGraphManager.updateRoute(route);
            ((NMapView) NUtils.getGameUI().map).routeGraphManager.updateGraph();
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
                menu = new NFlowerMenu(new String[]{"Edit name", "Delete", "Add Specialization"}) {
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
                            } else if (option.name.equals("Add Specialization")) {
                                RouteSpecialization.selectSpecialization(route);
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
            RouteGraphManager routeGraphManager = ((NMapView) NUtils.getGameUI().map).routeGraphManager;
            routeGraphManager.deleteRoute(routeList.sel.route);
            NConfig.needRoutesUpdate();
            showRoutes();
            if (routeGraphManager.getRoutes().isEmpty()) {
                actionContainer.hide();
                waypointList.hide();
            }
            waypointList.update(route.waypoints);
            specList.update(routeList.sel.route);
            hearthfireWaypointList.updateHearthfireWaypoints();
            ((NMapView) NUtils.getGameUI().map).initRouteDummys(routeList.sel.route.id);
        }
    }

    public class WaypointList extends SListBox<CoordItem, Widget> {
        private final List<CoordItem> items = new ArrayList<>();

        WaypointList(Coord sz) {
            super(sz, UI.scale(16));
        }

        NFlowerMenu menu;

        private void startNavigation(RoutePoint point) {
            Thread t = new Thread(() -> {
                try {
                    new RoutePointNavigator(point).run(NUtils.getGameUI());
                } catch (InterruptedException e) {
                    NUtils.getGameUI().error("Navigation interrupted by the user");
                }
            }, "RoutePointNavigator");
            t.start();
            NUtils.getGameUI().biw.addObserve(t);
        }

        @Override
        protected Widget makeitem(CoordItem item, int idx, Coord sz) {
            return new ItemWidget<CoordItem>(this, sz, item) {{
                add(item);
            }
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b == 3) {
                        RoutePoint rp = item.routePoint;
                        menu = new NFlowerMenu(new String[]{"Navigate To", "Delete"}) {
                            @Override
                            public boolean mousedown(MouseDownEvent ev) {
                                if(super.mousedown(ev))
                                    nchoose(null);
                                return true;
                            }

                            public void destroy() {
                                menu = null;
                                super.destroy();
                            }

                            @Override
                            public void nchoose(NPetal option) {
                                if (option != null) {
                                    if (option.name.equals("Navigate To")) {
                                        new Thread(() -> {
                                            try {
                                                new RoutePointNavigator(rp).run(NUtils.getGameUI());
                                            } catch (InterruptedException e) {
                                                NUtils.getGameUI().error("Navigation interrupted: " + e.getMessage());
                                            }
                                        }, "RoutePointNavigator").start();
                                    } else if (option.name.equals("Delete")) {
                                        routeList.sel.route.deleteWaypoint(rp);
                                        waypointList.update(routeList.sel.route.waypoints);
                                        hearthfireWaypointList.updateHearthfireWaypoints();
                                        specList.update(routeList.sel.route);
                                        ((NMapView) NUtils.getGameUI().map).initRouteDummys(routeList.sel.route.id);
                                    }
                                }
                                uimsg("cancel");
                            }
                        };
                        Widget par = parent;
                        Coord pos = ev.c;
                        while (par != null && !(par instanceof GameUI)) {
                            pos = pos.add(par.c);
                            par = par.parent;
                        }
                        ui.root.add(menu, pos.add(UI.scale(25, 38)));
                        return true;
                    } else if (ev.b == 1) {
                        startNavigation(item.routePoint);
                        return true;
                    }
                    return super.mousedown(ev);
                }
            };
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

        Color bg = new Color(30, 40, 40, 160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }
    }

    public class CoordItem extends Widget {
        private final Label label;
        private final RoutePoint routePoint;

        public CoordItem(long gridid, Coord2d coord, RoutePoint routePoint) {
            this.routePoint = routePoint;
            String displayText;

            if (routePoint.hearthFirePlayerName != null && !routePoint.hearthFirePlayerName.isEmpty()) {
                displayText = routePoint.hearthFirePlayerName;
            } else {
                // old logic, or just something generic:
                displayText = String.valueOf(gridid) + " " + routePoint.id;
            }
            
            // Check all connections for door and gobName information
            for (int neighborHash : routePoint.getConnectedNeighbors()) {
                RoutePoint.Connection conn = routePoint.getConnection(neighborHash);
                if (conn != null && conn.gobHash != null) {
                    if (!conn.gobName.isEmpty()) {
                        displayText = conn.gobName + " " + routePoint.id;
                    }
                    if (conn.isDoor) {
                        displayText = "★ " + displayText;
                        break; // Once we find a door connection, we can stop
                    }
                }
            }
            
            this.label = add(new Label(displayText));
            this.sz = label.sz.add(UI.scale(4), UI.scale(4));
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
        }
    }

    public class SpecList extends SListBox<Route.RouteSpecialization, Widget> {
        private Route currentRoute;

        public SpecList(Coord sz) {
            super(sz, UI.scale(24));
        }

        public void update(Route route) {
            this.currentRoute = route;
            change(null);
        }

        @Override
        protected List<Route.RouteSpecialization> items() {
            return currentRoute != null ? currentRoute.spec : new ArrayList<>();
        }

        @Override
        protected Widget makeitem(Route.RouteSpecialization item, int idx, Coord sz) {
            return new ItemWidget<Route.RouteSpecialization>(this, sz, item) {{
                add(new Label(item.name));
            }};
        }

        Color bg = new Color(30, 40, 40, 160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }
    }

    public class HearthfireWaypointList extends SListBox<CoordItem, Widget> {
        private final List<CoordItem> items = new ArrayList<>();

        HearthfireWaypointList(Coord sz) {
            super(sz, UI.scale(16));
            updateHearthfireWaypoints();
        }

        public void updateHearthfireWaypoints() {
            items.clear();
            Route hearthfireRoute = getHearthfireRoute();
            if (hearthfireRoute != null) {
                for (RoutePoint point : hearthfireRoute.waypoints) {
                    items.add(new CoordItem(point.gridId, point.toCoord2d(NUtils.getGameUI().map.glob.map), point));
                }
            }
            if(NUtils.getGameUI() != null) {
                ((NMapView) NUtils.getGameUI().map).routeGraphManager.updateGraph();
            }

            NConfig.needRoutesUpdate();
        }

        @Override
        protected List<CoordItem> items() {
            return items;
        }

        Color bg = new Color(30, 40, 40, 160);
        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }

        @Override
        protected Widget makeitem(CoordItem item, int idx, Coord sz) {
            return new ItemWidget<CoordItem>(this, sz, item) {{
                add(item);
            }
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b == 3) {
                        // Right click: show menu for this hearthfire point
                        RoutePoint rp = item.routePoint;
                        menu = new NFlowerMenu(new String[]{"Navigate To", "Delete"}) {
                            @Override
                            public boolean mousedown(MouseDownEvent ev) {
                                if(super.mousedown(ev))
                                    nchoose(null);
                                return true;
                            }

                            public void destroy() {
                                menu = null;
                                super.destroy();
                            }

                            @Override
                            public void nchoose(NPetal option) {
                                if (option != null) {
                                    Route hearthfireRoute = getHearthfireRoute();
                                    if (option.name.equals("Navigate To")) {
                                        new Thread(() -> {
                                            try {
                                                new RoutePointNavigator(rp).run(NUtils.getGameUI());
                                            } catch (InterruptedException e) {
                                                NUtils.getGameUI().error("Navigation interrupted: " + e.getMessage());
                                            }
                                        }, "RoutePointNavigator").start();
                                    } else if (option.name.equals("Delete")) {
                                        if (hearthfireRoute != null) {
                                            hearthfireRoute.deleteWaypoint(rp);
                                            hearthfireWaypointList.updateHearthfireWaypoints();
                                            ((NMapView) NUtils.getGameUI().map).initRouteDummys(hearthfireRoute.id);
                                        }
                                    }
                                }
                                uimsg("cancel");
                            }
                        };
                        Widget par = parent;
                        Coord pos = ev.c;
                        while (par != null && !(par instanceof GameUI)) {
                            pos = pos.add(par.c);
                            par = par.parent;
                        }
                        ui.root.add(menu, pos.add(UI.scale(25, 38)));
                        return true;
                    } else if (ev.b == 1) {
                        // Left click: start navigation
                        startNavigation(item.routePoint);
                        return true;
                    }
                    return super.mousedown(ev);
                }

                // Helper, same as in WaypointList:
                private void startNavigation(RoutePoint point) {
                    Thread t = new Thread(() -> {
                        try {
                            new RoutePointNavigator(point).run(NUtils.getGameUI());
                        } catch (InterruptedException e) {
                            NUtils.getGameUI().error("Navigation interrupted by the user");
                        }
                    }, "RoutePointNavigator");
                    t.start();
                    NUtils.getGameUI().biw.addObserve(t);
                }

                private NFlowerMenu menu;
            };
        }

    }

    private Route getHearthfireRoute() {
        if(NUtils.getGameUI() != null ) {
            for (Route route : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().values()) {
                for (Route.RouteSpecialization spec : route.spec) {
                    if (spec.name.contains("HearthFires")) {
                        return route;
                    }
                }
            }
        }
        return null;
    }
}
