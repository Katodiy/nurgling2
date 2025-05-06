package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Window;
import nurgling.*;
import nurgling.routes.Route;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class RouteSpecialization extends Window {
    private Route route = null;

    public RouteSpecialization() {
        super(UI.scale(200,500), "Route Specialization");
        add(new RouteSpecializationList(UI.scale(200,500)));
    }

    @Override
    public void show() {
        super.show();
        // Center the window relative to its parent
        if (parent != null) {
            Coord sz = parent.sz;
            Coord c = sz.div(2).sub(this.sz.div(2));
            this.c = c;
        }
    }

    public enum SpecName {
        honey
    }

    private static ArrayList<RouteSpecializationItem> specialisation = new ArrayList<>();

    static {
        // Initialize with Honey specialization
        specialisation.add(new RouteSpecializationItem("honey", "Honey Route", Resource.loadsimg("nurgling/categories/bee")));
    }

    public class RouteSpecializationList extends SListBox<RouteSpecializationItem, Widget> {
        RouteSpecializationList(Coord sz) {
            super(sz, UI.scale(24));
        }

        @Override
        public void change(RouteSpecializationItem item) {
            super.change(item);
        }

        protected List<RouteSpecializationItem> items() {
            return new ArrayList<>(specialisation);
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(sz.x, sz.y));
        }

        protected Widget makeitem(RouteSpecializationItem item, int idx, Coord sz) {
            return(new ItemWidget<RouteSpecializationItem>(this, sz, item) {
                {
                    add(item);
                }

                public boolean mousedown(MouseDownEvent ev) {
                    super.mousedown(ev);

                    String value = item.name;
                    boolean isFound = false;
                    for(Route.RouteSpecialization s: route.spec) {
                        if(s.name.equals(item.name))
                            isFound = true;
                    }
                    if(!isFound) {
                        route.spec.add(new Route.RouteSpecialization(value));
                        NConfig.needRoutesUpdate();
                        NUtils.getGameUI().routesWidget.showRoutes();
                        RouteSpecialization.this.hide();
                    } else {
                        NUtils.getGameUI().error("Specialization already selected.");
                    }
                    return(true);
                }
            });
        }

        @Override
        public void wdgmsg(String msg, Object... args) {
            super.wdgmsg(msg, args);
        }

        Color bg = new Color(30,40,40,160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if(msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(msg, args);
        }
    }

    public static class RouteSpecializationItem extends Widget {
        public Label text;
        public String name;
        public String prettyName;
        public BufferedImage image;
        private TexI tex;

        public RouteSpecializationItem(String text, String prettyName, BufferedImage image) {
            this.text = add(new Label(prettyName), UI.scale(30, 4));
            this.name = text;
            this.prettyName = prettyName;
            this.image = image;
            tex = new TexI(image);
            pack();
            sz.y = UI.scale(24);
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
            g.image(tex, Coord.z, UI.scale(24,24));
        }
    }

    public static RouteSpecializationItem findSpecialization(String name) {
        for(RouteSpecializationItem item : specialisation) {
            if(item.name.equals(name))
                return item;
        }
        return null;
    }

    public static void selectSpecialization(Route route) {
        NUtils.getGameUI().routespec.show();
        NUtils.getGameUI().setfocus(NUtils.getGameUI().routespec);
        NUtils.getGameUI().routespec.raise();
        NUtils.getGameUI().routespec.route = route;
    }

    public class SpecList extends SListBox<Route.RouteSpecialization, Widget> {
        public SpecList(Coord sz) {
            super(sz, UI.scale(24));
        }

        @Override
        protected List<Route.RouteSpecialization> items() {
            return route != null ? route.spec : new ArrayList<>();
        }

        @Override
        protected Widget makeitem(Route.RouteSpecialization item, int idx, Coord sz) {
            return new ItemWidget<Route.RouteSpecialization>(this, sz, item) {
                {
                    RouteSpecializationItem specItem = RouteSpecialization.findSpecialization(item.name);
                    if (specItem != null) {
                        add(specItem);
                    }
                }

                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b == 3) { // Right click
                        NFlowerMenu menu = new NFlowerMenu(new String[]{"Delete"}) {
                            @Override
                            public void nchoose(NPetal option) {
                                if (option != null && option.name.equals("Delete")) {
                                    route.removeSpecialization(item.name);
                                    NConfig.needRoutesUpdate();
                                    NUtils.getGameUI().routesWidget.showRoutes();
                                }
                                uimsg("cancel");
                            }
                        };
                        menu.show();
                        return true;
                    }
                    return super.mousedown(ev);
                }
            };
        }

        Color bg = new Color(30,40,40,160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }
    }
} 