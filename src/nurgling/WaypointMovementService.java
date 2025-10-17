package nurgling;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;

import java.util.LinkedList;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;

/**
 * Centralized service for managing waypoint-based movement queues.
 * Both the full map window and corner minimap use this service to share the same movement queue.
 */
public class WaypointMovementService {
    private final NGameUI gui;

    // Shared movement queue state
    public final LinkedList<MiniMap.Location> movementQueue = new LinkedList<>();
    public MiniMap.Location currentTarget = null;
    public Coord2d currentTargetWorld = null;
    public Coord lastPlayerPos = null;
    public double lastMovementTime = 0;

    public WaypointMovementService(NGameUI gui) {
        this.gui = gui;
    }

    /**
     * Add a waypoint to the movement queue.
     * If no movement is in progress, starts moving to the new waypoint.
     */
    public void addWaypoint(MiniMap.Location loc, MiniMap.Location sessloc) {
        synchronized(movementQueue) {
            // Check if we need to start movement (no current target)
            boolean startMovement = (currentTarget == null);
            movementQueue.add(loc);

            // Only start movement if we weren't already moving
            if(startMovement) {
                currentTarget = movementQueue.poll();
                if(currentTarget != null && sessloc != null) {
                    sendMovementCommand(currentTarget, sessloc);
                }
            }
        }
    }

    /**
     * Clear the movement queue and stop current movement.
     */
    public void clearQueue() {
        synchronized(movementQueue) {
            movementQueue.clear();
            currentTarget = null;
            currentTargetWorld = null;
            lastPlayerPos = null;
        }
    }

    /**
     * Process the movement queue - should be called from tick().
     * Advances to next waypoint when current one is reached.
     */
    public void processMovementQueue(MapFile file, MiniMap.Location sessloc) {
        synchronized(movementQueue) {
            // Check if we have a current target and if we're close to it
            if(currentTarget != null && sessloc != null && currentTarget.seg.id == sessloc.seg.id) {
                try {
                    MapView mv = gui.map;
                    if(mv == null) return;

                    // Get player's current location in the same coordinate system as the target
                    Coord mc = new Coord2d(mv.getcc()).floor(tilesz);
                    MCache.Grid plg = mv.ui.sess.glob.map.getgrid(mc.div(cmaps));
                    MapFile.GridInfo info = file.gridinfo.get(plg.id);

                    if(info != null && info.seg == currentTarget.seg.id) {
                        // Convert to segment-relative tile coordinates
                        Coord playerTc = info.sc.mul(cmaps).add(mc.sub(plg.ul));

                        // Track player movement for interruption detection
                        double currentTime = Utils.rtime();
                        if(lastPlayerPos == null || !lastPlayerPos.equals(playerTc)) {
                            // Player moved
                            lastPlayerPos = playerTc;
                            lastMovementTime = currentTime;
                        } else {
                            // Player hasn't moved - check if stuck
                            double timeSinceMove = currentTime - lastMovementTime;
                            if(timeSinceMove > 2.0) {  // 2 seconds without movement
                                // Check config to determine retry behavior
                                boolean shouldRetry = (Boolean) NConfig.get(NConfig.Key.waypointRetryOnStuck);

                                if(shouldRetry) {
                                    sendMovementCommand(currentTarget, sessloc);
                                    lastMovementTime = currentTime;  // Reset timer after retry
                                } else {
                                    movementQueue.clear();
                                    currentTarget = null;
                                    currentTargetWorld = null;
                                    lastPlayerPos = null;
                                    return;  // Exit immediately after clearing
                                }
                            }
                        }

                        // Calculate distance in tile coordinates
                        double dx = currentTarget.tc.x - playerTc.x;
                        double dy = currentTarget.tc.y - playerTc.y;
                        double dist = Math.sqrt(dx * dx + dy * dy);

                        // If we're within 5 tiles of the target, consider it reached
                        if(dist < 1.0) {
                            currentTarget = null;
                            currentTargetWorld = null;
                            lastPlayerPos = null;
                        }
                    }
                } catch(Loading l) {
                    // Player position not available yet, skip this tick
                }
            }

            // If no current target, get next from queue
            if(currentTarget == null && !movementQueue.isEmpty()) {
                currentTarget = movementQueue.poll();
                if(currentTarget != null && sessloc != null && currentTarget.seg.id == sessloc.seg.id) {
                    // Reset movement tracking
                    lastPlayerPos = null;
                    lastMovementTime = Utils.rtime();
                    sendMovementCommand(currentTarget, sessloc);
                }
            }
        }
    }

    /**
     * Send a movement command to the specified location.
     */
    private void sendMovementCommand(MiniMap.Location target, MiniMap.Location sessloc) {
        MapView mv = gui.map;
        if(mv == null || gui.ui == null) return;

        Coord mc = gui.ui.mc;
        mv.wdgmsg("click", mc,
                  target.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)).floor(posres),
                  1, 0);  // button=1, modflags=0
    }
}
