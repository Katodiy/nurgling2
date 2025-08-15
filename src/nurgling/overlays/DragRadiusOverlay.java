package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import nurgling.routes.RoutePoint;

public class DragRadiusOverlay extends Sprite implements RenderTree.Node, PView.Render2D {
    
    private RoutePoint routePoint;
    private Coord3f originalWorldPos;
    
    public DragRadiusOverlay(Owner owner, RoutePoint routePoint, Coord3f originalWorldPos) {
        super(owner, null);
        this.routePoint = routePoint;
        this.originalWorldPos = originalWorldPos;
        System.out.println("DragRadiusOverlay created at: " + originalWorldPos);
    }

    @Override
    public void draw(GOut g, Pipe state) {
        try {
            // Draw 11x11 grid (5 tiles in each direction from center)
            for(int dx = -5; dx <= 5; dx++) {
                for(int dy = -5; dy <= 5; dy++) {
                    // Calculate position relative to the original position
                    Coord3f tileOffset = new Coord3f(
                        dx * (float)MCache.tilesz.x,
                        dy * (float)MCache.tilesz.y,
                        0f
                    );
                    
                    // Convert to screen coordinates
                    Coord sc = Homo3D.obj2view(tileOffset, state, Area.sized(Coord.z, g.sz())).round2();
                    if(sc == null) continue;
                    
                    // Calculate tile size
                    Coord3f nextTileOffset = new Coord3f(
                        (dx + 1) * (float)MCache.tilesz.x,
                        dy * (float)MCache.tilesz.y,
                        0f
                    );
                    Coord nextSc = Homo3D.obj2view(nextTileOffset, state, Area.sized(Coord.z, g.sz())).round2();
                    if(nextSc == null) continue;
                    
                    int tileSize = (int)sc.dist(nextSc);
                    
                    // Check if this tile is within the 5-tile limit
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    boolean isValidTile = distance <= 5.0;
                    
                    // Only draw valid tiles
                    if(isValidTile) {
                        // Calculate the four corners of this tile in 3D space
                        float halfTile = (float)MCache.tilesz.x / 2;
                        
                        // Tile corners in world coordinates
                        Coord3f topLeft = new Coord3f(
                            tileOffset.x - halfTile, 
                            tileOffset.y - halfTile, 
                            0f
                        );
                        Coord3f topRight = new Coord3f(
                            tileOffset.x + halfTile, 
                            tileOffset.y - halfTile, 
                            0f
                        );
                        Coord3f bottomLeft = new Coord3f(
                            tileOffset.x - halfTile, 
                            tileOffset.y + halfTile, 
                            0f
                        );
                        Coord3f bottomRight = new Coord3f(
                            tileOffset.x + halfTile, 
                            tileOffset.y + halfTile, 
                            0f
                        );
                        
                        // Convert each corner to screen coordinates
                        Coord scTopLeft = Homo3D.obj2view(topLeft, state, Area.sized(Coord.z, g.sz())).round2();
                        Coord scTopRight = Homo3D.obj2view(topRight, state, Area.sized(Coord.z, g.sz())).round2();
                        Coord scBottomLeft = Homo3D.obj2view(bottomLeft, state, Area.sized(Coord.z, g.sz())).round2();
                        Coord scBottomRight = Homo3D.obj2view(bottomRight, state, Area.sized(Coord.z, g.sz())).round2();
                        
                        if(scTopLeft != null && scTopRight != null && scBottomLeft != null && scBottomRight != null) {
                            // Light green fill - draw filled quadrilateral
                            g.chcolor(0, 255, 0, 80);
                            drawQuadrilateral(g, scTopLeft, scTopRight, scBottomRight, scBottomLeft, true);
                            
                            // Darker green border
                            g.chcolor(0, 255, 0, 160);
                            drawQuadrilateral(g, scTopLeft, scTopRight, scBottomRight, scBottomLeft, false);
                            
                            // Highlight center tile (original position)
                            if(dx == 0 && dy == 0) {
                                g.chcolor(255, 255, 0, 150);
                                drawQuadrilateral(g, scTopLeft, scTopRight, scBottomRight, scBottomLeft, true);
                            }
                        }
                    }
                }
            }
            
            g.chcolor(); // Reset color
        } catch (Exception e) {
            System.err.println("Error drawing drag radius overlay: " + e.getMessage());
        }
    }

    public boolean setup(RenderTree.Slot slot) {
        return true;
    }
    
    // Draw a quadrilateral (4-sided polygon) on screen
    private void drawQuadrilateral(GOut g, Coord topLeft, Coord topRight, Coord bottomRight, Coord bottomLeft, boolean filled) {
        if(filled) {
            // Fill the quadrilateral by drawing horizontal lines
            // Find the bounding box
            int minY = Math.min(Math.min(topLeft.y, topRight.y), Math.min(bottomLeft.y, bottomRight.y));
            int maxY = Math.max(Math.max(topLeft.y, topRight.y), Math.max(bottomLeft.y, bottomRight.y));
            
            // For each scanline, find the left and right edges
            for(int y = minY; y <= maxY; y++) {
                // Find intersections with the edges
                Integer leftX = null, rightX = null;
                
                // Check left edge (topLeft to bottomLeft)
                Integer x1 = getLineIntersectionX(topLeft, bottomLeft, y);
                if(x1 != null) {
                    leftX = (leftX == null) ? x1 : Math.min(leftX, x1);
                    rightX = (rightX == null) ? x1 : Math.max(rightX, x1);
                }
                
                // Check right edge (topRight to bottomRight)
                Integer x2 = getLineIntersectionX(topRight, bottomRight, y);
                if(x2 != null) {
                    leftX = (leftX == null) ? x2 : Math.min(leftX, x2);
                    rightX = (rightX == null) ? x2 : Math.max(rightX, x2);
                }
                
                // Check top edge (topLeft to topRight)
                Integer x3 = getLineIntersectionX(topLeft, topRight, y);
                if(x3 != null) {
                    leftX = (leftX == null) ? x3 : Math.min(leftX, x3);
                    rightX = (rightX == null) ? x3 : Math.max(rightX, x3);
                }
                
                // Check bottom edge (bottomLeft to bottomRight)
                Integer x4 = getLineIntersectionX(bottomLeft, bottomRight, y);
                if(x4 != null) {
                    leftX = (leftX == null) ? x4 : Math.min(leftX, x4);
                    rightX = (rightX == null) ? x4 : Math.max(rightX, x4);
                }
                
                // Draw the horizontal line if we found valid intersections
                if(leftX != null && rightX != null && leftX <= rightX) {
                    g.line(new Coord(leftX, y), new Coord(rightX, y), 1);
                }
            }
        } else {
            // Draw outline
            g.line(topLeft, topRight, 2);
            g.line(topRight, bottomRight, 2);
            g.line(bottomRight, bottomLeft, 2);
            g.line(bottomLeft, topLeft, 2);
        }
    }
    
    // Helper method to find X coordinate where a line intersects a horizontal line at Y
    private Integer getLineIntersectionX(Coord p1, Coord p2, int y) {
        if(p1.y == p2.y) {
            // Horizontal line
            if(p1.y == y) {
                return Math.min(p1.x, p2.x); // Return leftmost point
            }
            return null;
        }
        
        // Check if Y is within the line segment
        if((y >= p1.y && y <= p2.y) || (y >= p2.y && y <= p1.y)) {
            // Calculate intersection using linear interpolation
            double t = (double)(y - p1.y) / (p2.y - p1.y);
            int x = (int)(p1.x + t * (p2.x - p1.x));
            return x;
        }
        
        return null;
    }
}