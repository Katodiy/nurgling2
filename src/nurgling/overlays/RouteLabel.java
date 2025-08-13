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
    private static final Coord3f Z_OFFSET = new Coord3f(0, 0, 0); // At ground level
    private TexI label;
    public static final double floaty = UI.scale(5.0);
    Route route;
    public final Tex tex = Resource.loadtex("nurgling/hud/point");
    double a = 0;
    final int sy;
    public RoutePoint point;
    Coord sc;
    
    // Dragging state
    private boolean isDragging = false;
    private Coord dragPreviewPosition = null;

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
        
        Coord c = tex.sz().inv();
        c.x = c.x / 2;
        c.y += cury();
        // Position dot at ground level instead of above it
        
        // Draw the original position (red tint if dragging)
        if(isDragging) {
            g.chcolor(255, 128, 128, (α * 2) / 3); // Red tint, 2/3 brightness
        } else {
            g.chcolor(255, 255, 255, α); // Normal white brightness
        }
        g.image(tex, sc.add(c));
        
        // Draw preview position if dragging
        if(isDragging && dragPreviewPosition != null) {
            // Check if the preview position would be within drag limits
            boolean withinLimits = isPreviewWithinLimits(dragPreviewPosition, state);
            
            if(withinLimits) {
                g.chcolor(128, 255, 128, α); // Green tint for valid position
            } else {
                g.chcolor(255, 128, 128, α); // Red tint for invalid position
            }
            g.image(tex, dragPreviewPosition.add(c));
        }
        
        // Draw drag limit boundary when dragging
        if(isDragging) {
            drawDragLimitBoundary(g, state);
        }
        
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
    
    // Start dragging
    public void startDrag() {
        isDragging = true;
        dragPreviewPosition = null;
    }
    
    // Update preview position during drag (screen coordinates)
    public void updateDragPreview(Coord screenPos) {
        if(isDragging) {
            dragPreviewPosition = screenPos;
        }
    }
    
    // Update actual position during drag
    public void updatePosition(Coord2d worldPos) {
        updateRoutePointPosition(worldPos);
    }
    
    // Finalize the drag operation
    public void finalizeDrag() {
        try {
            isDragging = false;
            dragPreviewPosition = null;
            
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
    
    // Cancel drag operation
    public void cancelDrag() {
        isDragging = false;
        dragPreviewPosition = null;
    }
    
    // Check if preview position is within drag limits  
    private boolean isPreviewWithinLimits(Coord screenPos, Pipe state) {
        try {
            MCache cache = NUtils.getGameUI().ui.sess.glob.map;
            if(cache == null) return false;
            
            // Convert screen position to world coordinate using the same method as the main game
            NMapView mapView = (NMapView) NUtils.getGameUI().map;
            final boolean[] result = {false};
            
            mapView.new Hittest(screenPos) {
                public void hit(Coord pc, Coord2d mc, ClickData inf) {
                    if(mc != null) {
                        Coord tilec = mc.div(MCache.tilesz).floor();
                        MCache.Grid grid = cache.getgridt(tilec);
                        if(grid != null) {
                            long proposedGridId = grid.id;
                            Coord proposedLocalCoord = tilec.sub(grid.ul);
                            result[0] = point.isWithinDragLimit(proposedGridId, proposedLocalCoord, cache);
                        }
                    }
                }
                
                protected void nohit(Coord pc) {
                    result[0] = false;
                }
            }.run();
            
            return result[0];
        } catch (Exception e) {
            return false;
        }
    }
    
    // Draw a circular boundary showing the 5-cell drag limit
    private void drawDragLimitBoundary(GOut g, Pipe state) {
        try {
            MCache cache = NUtils.getGameUI().ui.sess.glob.map;
            if(cache == null) return;
            
            // Get original world position
            Coord2d originalWorldPos = point.getOriginalWorldPosition(cache);
            if(originalWorldPos == null) return;
            
            // Calculate boundary world position (5 tiles away)
            Coord2d boundaryWorldPos = originalWorldPos.add(5 * MCache.tilesz.x, 0);
            
            // Convert both original and boundary positions to screen coordinates
            Coord3f originalWorld3d = new Coord3f((float)originalWorldPos.x, (float)originalWorldPos.y, 0);
            Coord3f boundaryWorld3d = new Coord3f((float)boundaryWorldPos.x, (float)boundaryWorldPos.y, 0);
            
            Coord originalScreen = Homo3D.obj2view(originalWorld3d, state, Area.sized(Coord.z, g.sz())).round2();
            Coord boundaryScreen = Homo3D.obj2view(boundaryWorld3d, state, Area.sized(Coord.z, g.sz())).round2();
            
            if(originalScreen != null && boundaryScreen != null) {
                // Calculate actual radius in screen pixels based on zoom
                int radiusPixels = (int)originalScreen.dist(boundaryScreen);
                
                // Draw a more visible circle boundary
                g.chcolor(255, 255, 0, 150); // Yellow with higher transparency
                
                // Draw circle using more line segments for smoother appearance
                int segments = 64;
                Coord prevPoint = null;
                
                for(int i = 0; i <= segments; i++) {
                    double angle = (2 * Math.PI * i) / segments;
                    int x = originalScreen.x + (int)(Math.cos(angle) * radiusPixels);
                    int y = originalScreen.y + (int)(Math.sin(angle) * radiusPixels);
                    Coord currentPoint = new Coord(x, y);
                    
                    if(prevPoint != null) {
                        g.line(prevPoint, currentPoint, 2);
                    }
                    prevPoint = currentPoint;
                }
                
                // Draw a center dot to mark the original position
                g.chcolor(255, 0, 0, 200); // Red center dot
                g.frect(originalScreen.sub(2, 2), new Coord(4, 4));
            }
        } catch (Exception e) {
            // Ignore drawing errors - but maybe log for debugging
            System.err.println("Error drawing drag boundary: " + e.getMessage());
        }
    }
    
    private void updateRoutePointPosition(Coord2d worldPos) {
        MCache cache = NUtils.getGameUI().ui.sess.glob.map;
        if(cache == null) return;
        
        // Convert world coordinate to tile coordinate
        Coord tilec = worldPos.div(MCache.tilesz).floor();
        MCache.Grid grid = cache.getgridt(tilec);
        
        if(grid != null) {
            long proposedGridId = grid.id;
            Coord proposedLocalCoord = tilec.sub(grid.ul);
            
            // Check if the proposed position is within the 5-cell drag limit
            if(point.isWithinDragLimit(proposedGridId, proposedLocalCoord, cache)) {
                // Update the route point's position only if within limit
                point.gridId = proposedGridId;
                point.localCoord = proposedLocalCoord;
                point.updateHashCode(); // This will update connections if ID changes
            } else {
                // Position is outside the allowed drag area - could show a message or visual indicator
                NUtils.getGameUI().msg("Route point cannot be moved more than 5 tiles from its original position");
            }
        }
    }
}
