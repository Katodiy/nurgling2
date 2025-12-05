package nurgling.pf;

import haven.*;
import nurgling.NUtils;

public class Utils
{
    public static Coord toPfGrid(Coord2d coord)
    {
        return coord.div(MCache.tilehsz).round();
    }

    public static Coord2d pfGridToWorld(Coord coord)
    {
        return coord.mul(MCache.tilehsz);
    }

    /**
     * Check if a world coordinate is inside the player's 81-tile visible area.
     * Uses the same calculation as ExploredArea and NMiniMap for consistency.
     * 
     * The visible area is calculated as 9x9 grids (each 100 world units) = 900x900 world units,
     * which equals approximately 81x81 tiles (each tile is 11 world units).
     * 
     * @param coord2d the world coordinate to check
     * @return true if inside visible area, false otherwise
     */
    public static boolean inVisibleArea(Coord2d coord2d) {
        Gob player = NUtils.player();
        if(player == null) {
            return false;
        }
        
        // Use the same calculation as NMiniMap and ExploredArea
        // sgridsz = 100 (world units per grid)
        // visible area = 9 grids = 900 world units in each direction
        Coord2d sgridsz = new Coord2d(100, 100);
        
        // Calculate the upper-left corner of visible area (aligned to grid)
        // player.rc.floor(sgridsz) gives the grid the player is in
        // .sub(4, 4) moves to 4 grids before (so player is roughly centered in 9x9 area)
        // .mul(sgridsz) converts back to world coordinates
        Coord2d ul = player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz);
        
        // Calculate the bottom-right corner
        // 9 grids * 100 units = 900 world units
        Coord2d viewSize = sgridsz.mul(9);
        Coord2d br = ul.add(viewSize);
        
        return coord2d.x >= ul.x && coord2d.x < br.x &&
               coord2d.y >= ul.y && coord2d.y < br.y;
    }
}
