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
    private boolean previewWithinLimits = true; // Cache the limits check result
    
    // Drag radius overlay
    private OCache.Virtual dragRadiusGob = null;

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
            // Use cached limits check result instead of expensive operation
            g.chcolor(128, 255, 128, α); // Green tint for valid position

            g.image(tex, dragPreviewPosition.add(c));
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
        previewWithinLimits = true; // Start assuming valid
        showDragRadiusOverlay();
    }
    
    // Update preview position during drag (screen coordinates)
    public void updateDragPreview(Coord screenPos) {
        if(isDragging) {
            dragPreviewPosition = screenPos;
            // Update the cached limits check result, but avoid expensive operations when out of bounds
            if(screenPos.x >= 0 && screenPos.y >= 0) {
                try {
                    previewWithinLimits = isPreviewWithinLimits(screenPos, null);
                } catch (Exception e) {
                    previewWithinLimits = false;
                }
            } else {
                previewWithinLimits = false;
            }
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
            hideDragRadiusOverlay();
            
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
        previewWithinLimits = true;
        hideDragRadiusOverlay();
    }
    
    // Check if preview position is within drag limits  
    private boolean isPreviewWithinLimits(Coord screenPos, Pipe state) {
        try {
            MCache cache = NUtils.getGameUI().ui.sess.glob.map;
            if(cache == null) return false;
            
            // Don't perform expensive operations if coordinates are clearly invalid
            if(screenPos == null || screenPos.x < 0 || screenPos.y < 0) {
                return false;
            }
            
            // Convert screen position to world coordinate using the same method as the main game
            NMapView mapView = (NMapView) NUtils.getGameUI().map;
            if(mapView == null) return false;
            
            final boolean[] result = {false};
            
            // Add bounds checking for the map view size
            if(screenPos.x >= mapView.sz.x || screenPos.y >= mapView.sz.y) {
                return false;
            }
            
            mapView.new Hittest(screenPos) {
                public void hit(Coord pc, Coord2d mc, ClickData inf) {
                    if(mc != null) {
                        try {
                            Coord tilec = mc.div(MCache.tilesz).floor();
                            MCache.Grid grid = cache.getgridt(tilec);
                            if(grid != null) {
                                long proposedGridId = grid.id;
                                Coord proposedLocalCoord = tilec.sub(grid.ul);
                                result[0] = point.isWithinDragLimit(proposedGridId, proposedLocalCoord, cache);
                            }
                        } catch (Exception e) {
                            result[0] = false;
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
    
    // Show drag radius overlay at original route point position
    private void showDragRadiusOverlay() {
        try {
            if(dragRadiusGob != null) {
                hideDragRadiusOverlay(); // Clean up any existing overlay
            }
            
            MCache cache = NUtils.getGameUI().ui.sess.glob.map;
            if(cache == null) return;
            
            // Get original world position
            Coord2d originalWorldPos = point.getOriginalWorldPosition(cache);
            if(originalWorldPos == null) return;
            
            // Create virtual gob at original position
            NMapView mapView = (NMapView) NUtils.getGameUI().map;
            dragRadiusGob = mapView.glob.oc.new Virtual(originalWorldPos, 0);
            dragRadiusGob.virtual = true;
            
            // Add the drag radius overlay
            Coord3f originalWorldPos3d = new Coord3f((float)originalWorldPos.x, (float)originalWorldPos.y, 0f);
            dragRadiusGob.addcustomol(new DragRadiusOverlay(dragRadiusGob, point, originalWorldPos3d));
            
            // Add to global object cache
            mapView.glob.oc.add(dragRadiusGob);
            
        } catch(Exception e) {
            System.err.println("Error showing drag radius overlay: " + e.getMessage());
        }
    }
    
    // Hide drag radius overlay
    private void hideDragRadiusOverlay() {
        try {
            if(dragRadiusGob != null) {
                NMapView mapView = (NMapView) NUtils.getGameUI().map;
                mapView.glob.oc.remove(dragRadiusGob);
                dragRadiusGob = null;
            }
        } catch(Exception e) {
            System.err.println("Error hiding drag radius overlay: " + e.getMessage());
        }
    }
}
