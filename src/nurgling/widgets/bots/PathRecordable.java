package nurgling.widgets.bots;

import nurgling.routes.ForagerPath;
import nurgling.routes.ForagerWaypoint;

/**
 * Interface for bot widgets that support path recording via map clicks.
 * Widgets implementing this interface can have waypoints added by clicking on the minimap.
 */
public interface PathRecordable {

    /**
     * Returns true if the widget is currently in recording mode.
     */
    boolean isRecording();

    /**
     * Adds a waypoint to the path being recorded.
     */
    void addWaypointToRecording(ForagerWaypoint wp);

    /**
     * Returns the path currently being recorded or loaded for display.
     */
    ForagerPath getCurrentLoadedPath();
}
