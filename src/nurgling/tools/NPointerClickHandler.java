package nurgling.tools;

import haven.*;
import nurgling.NMapView;
import nurgling.NUtils;

/**
 * Handles right-click events on Pointer widgets (quest giver arrows)
 * to create directional lines to the target
 */
public class NPointerClickHandler {

    /**
     * Handle right-click on a pointer widget
     * @param worldCoords World coordinates of the pointer target (session-relative)
     * @param targetName Name/tooltip of the target (quest giver name)
     * @param gobid Gob ID if target is a gob, -1 otherwise
     */
    public static void handleRightClick(Coord2d worldCoords, String targetName, long gobid) {
        try {
            GameUI gui = NUtils.getGameUI();
            if(gui == null || gui.map == null || !(gui.map instanceof NMapView)) {
                return;
            }

            NMapView mapView = (NMapView) gui.map;

            // Get session location for coordinate conversion
            if(gui.mmap == null || gui.mmap.sessloc == null) {
                return;
            }

            MiniMap.Location sessloc = gui.mmap.sessloc;

            // Get player position as the origin for the vector
            Gob player = mapView.player();
            if(player == null) {
                return;
            }

            // Convert player world position to tile coordinates
            Coord playerTileCoords = player.rc.div(MCache.tilesz).floor().add(sessloc.tc);

            // Pointer's tc() returns session-relative world coordinates
            // Convert to segment-relative tile coordinates
            Coord targetTileCoords = worldCoords.div(MCache.tilesz).floor().add(sessloc.tc);

            System.out.println("=== Adding Directional Vector ===");
            System.out.println("Player tile coords: " + playerTileCoords);
            System.out.println("Target tile coords: " + targetTileCoords);
            Coord diff = targetTileCoords.sub(playerTileCoords);
            System.out.println("Direction: " + new Coord2d(diff).norm());
            System.out.println("================================");

            // Add a new directional vector from player position toward target
            mapView.addDirectionalVector(playerTileCoords, targetTileCoords, targetName, gobid);

        } catch(Exception e) {
            System.err.println("Error handling pointer right-click: " + e);
            e.printStackTrace();
        }
    }
}
