package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import nurgling.NMapView;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;

import java.awt.image.BufferedImage;

public class RouteLabel extends Sprite implements RenderTree.Node, PView.Render2D {
    private static final Coord3f Z_OFFSET = new Coord3f(0, 0, 0); // Slightly above the ground
    private TexI label;
    public static final double floaty = UI.scale(5.0);
    Route route;
    public final Tex tex = Resource.loadtex("nurgling/hud/point");
    double a = 0;
    final int sy;
    public RoutePoint point;
    Coord sc;

    public RouteLabel(Owner owner, Route route, RoutePoint point) {
        super(owner, null);
        this.route = route;
        this.point = point;
        this.sy = place(owner.context(Gob.class), tex.sz().y);
        update();
    }

    private static int place(Gob gob, int h) {
        int y = 0;
        trying: while(true) {
            for(Gob.Overlay ol : gob.ols) {
                if(ol.spr instanceof RouteLabel) {
                    RouteLabel f = (RouteLabel)ol.spr;
                    int y2 = f.cury();
                    int h2 = f.tex.sz().y;
                    if(((y2 >= y) && (y2 < y + h)) ||
                            ((y >= y2) && (y < y2 + h2))) {
                        y = y2 - h;
                        continue trying;
                    }
                }
            }
            return(y);
        }
    }

    public int cury() {
        return(sy - (int)(floaty * a));
    }

    private void update() {
        // Create a simple icon or label (e.g. a dot or waypoint marker)
        BufferedImage img = NStyle.openings.render(route.name).img;;
        label = new TexI(img);
    }

    @Override
    public void draw(GOut g, Pipe state) {
        sc = Homo3D.obj2view(Coord3f.o, state, Area.sized(Coord.z, g.sz())).round2();
        if(sc == null)
            return;
        int α;
        if(a < 0.75)
            α = 255;
        else
            α = (int)Utils.clip(255 * ((1 - a) / 0.25), 0, 255);
        g.chcolor(255, 255, 255, α);
        Coord c = tex.sz().inv();
        c.x = c.x / 2;
        c.y += cury();
        c.y -= 15;
        g.image(tex, sc.add(c));
        g.chcolor();
    }

    public boolean isect(Coord pc) {
        if(sc==null)
            return false;
        Coord ul = sc.sub(tex.sz().div(2));
        return pc.isect(ul, tex.sz());
    }
    
    // Check if this route label is being clicked for drag
    public boolean checkDragStart(Coord screenCoord) {
        return sc != null && isect(screenCoord);
    }
    
    // Update position during drag
    public void updatePosition(Coord2d worldPos) {
        updateRoutePointPosition(worldPos);
    }
    
    // Finalize the drag operation
    public void finalizeDrag() {
        try {
            // Update the route graph and connections
            NMapView mapView = (NMapView) NUtils.getGameUI().map;
            mapView.routeGraphManager.updateRoute(route);
            mapView.routeGraphManager.updateGraph();
            
            // Recreate the visual representation
            mapView.destroyRouteDummys();
            mapView.createRouteLabel(route.id);
            
            NUtils.getGameUI().msg("Route point moved to: " + point.gridId + "," + point.localCoord);
            
        } catch(Exception e) {
            NUtils.getGameUI().error("Failed to move route point: " + e.getMessage());
        }
    }
    
    private void updateRoutePointPosition(Coord2d worldPos) {
        MCache cache = NUtils.getGameUI().ui.sess.glob.map;
        if(cache == null) return;
        
        // Convert world coordinate to tile coordinate
        Coord tilec = worldPos.div(MCache.tilesz).floor();
        MCache.Grid grid = cache.getgridt(tilec);
        
        if(grid != null) {
            // Update the route point's position
            point.gridId = grid.id;
            point.localCoord = tilec.sub(grid.ul);
            point.updateHashCode(); // This will update connections if ID changes
        }
    }
}
