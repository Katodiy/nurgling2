package nurgling.widgets;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.FogArea;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class NCornerMiniMap extends NMiniMap implements Console.Directory {

    public NCornerMiniMap(Coord sz, MapFile file) {
        super(sz, file);
        follow(new MapLocator(NUtils.getGameUI().map));
    }

    public NCornerMiniMap(MapFile file) {
        super(file);
    }

    public boolean dragp(int button) {
        return(false);
    }

    public boolean clickmarker(DisplayMarker mark, Location loc, int button, boolean press) {
        if(mark.m instanceof MapFile.SMarker) {
            Gob gob = MarkerID.find(ui.sess.glob.oc, (MapFile.SMarker)mark.m);
            if(gob != null)
                mvclick(NUtils.getGameUI().map, null, loc, gob, button);
        }
        return(false);
    }

    public boolean clickicon(DisplayIcon icon, Location loc, int button, boolean press) {
        if(press) {
            mvclick(NUtils.getGameUI().map, null, loc, icon.gob, button);
            return(true);
        }
        return(false);
    }

    public boolean clickloc(Location loc, int button, boolean press) {
        if(!press && (sessloc != null) && (loc.seg.id == sessloc.seg.id)) {
            // Handle ctrl+click for queued movement
            if(ui.modctrl && (button == 1)) {
                synchronized(movementQueue) {
                    System.out.println("Ctrl+click (minimap): adding waypoint to queue. Current queue size: " + movementQueue.size());
                    // Check if we need to start movement (no current target)
                    boolean startMovement = (currentTarget == null);
                    movementQueue.add(loc);

                    // Only start movement if we weren't already moving
                    if(startMovement) {
                        currentTarget = movementQueue.poll();
                        if(currentTarget != null) {
                            System.out.println("Starting movement to first waypoint. Queue now has: " + movementQueue.size() + " remaining");
                            // Call wdgmsg directly with modflags=0 to avoid ctrl flag
                            Coord mc = ui.mc;
                            NUtils.getGameUI().map.wdgmsg("click", mc,
                                      currentTarget.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)).floor(posres),
                                      button, 0);
                        }
                    } else {
                        System.out.println("Waypoint queued. Total in queue: " + movementQueue.size() + ", already moving to current target");
                    }
                }
                return(true);
            } else {
                // Normal click - clear queue and move to location
                synchronized(movementQueue) {
                    System.out.println("Normal click (minimap): clearing queue");
                    movementQueue.clear();
                    currentTarget = null;
                    currentTargetWorld = null;
                }
                mvclick(NUtils.getGameUI().map, null, loc, null, button);
                return(true);
            }
        }
        return(false);
    }

    public void draw(GOut g) {
        // TODO подложка для карты
        //g.image(bg, Coord.z, UI.scale(bg.sz()));
        super.draw(g);
    }





    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
        cmdmap.put("rmseg", new Console.Command() {
            public void run(Console cons, String[] args) {
                MiniMap.Location loc = curloc;
                if(loc != null) {
                    try(Locked lk = new Locked(file.lock.writeLock())) {
                        file.segments.remove(loc.seg.id);
                    }
                }
            }
        });
    }
    public Map<String, Console.Command> findcmds() {
        return(cmdmap);
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        if (dloc != null) {
            DisplayIcon icon = iconat(c);
            if (icon != null) {
                Resource res = icon.icon.res;
                if(res == null)
                    return null;
                Resource.Tooltip tt = res.layer(Resource.tooltip);
                if (tt != null) {
                    String name = tt.t;
                    return Text.render(name).tex();
                }
            }
            Coord tc = c.sub(sz.div(2)).mul(scalef()).add(dloc.tc);
            DisplayMarker mark = markerat(tc);
            if (mark != null) {
                return (mark.tip);
            }
        }
        return (super.tooltip(c, prev));
    }

    @Override
    protected void processMovementQueue() {
        synchronized(movementQueue) {
            // Check if we have a current target and if we're close to it
            if(currentTarget != null && sessloc != null && currentTarget.seg.id == sessloc.seg.id) {
                try {
                    MapView mv = NUtils.getGameUI().map;
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
                                System.out.println("Player stuck! Retrying movement command...");
                                // Retry the movement command
                                Coord mc2 = ui.mc;
                                mv.wdgmsg("click", mc2,
                                          currentTarget.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)).floor(posres),
                                          1, 0);
                                lastMovementTime = currentTime;  // Reset timer after retry
                            }
                        }

                        // Calculate distance in tile coordinates
                        double dx = currentTarget.tc.x - playerTc.x;
                        double dy = currentTarget.tc.y - playerTc.y;
                        double dist = Math.sqrt(dx * dx + dy * dy);

                        // Debug: print distance occasionally
                        if(Math.random() < 0.016) {
                            System.out.println("Distance to waypoint: " + String.format("%.2f", dist) + " tiles (threshold: 5.0)");
                            System.out.println("  Player: " + playerTc + ", Target: " + currentTarget.tc);
                        }

                        // If we're within 5 tiles of the target, consider it reached
                        if(dist < 5.0) {
                            System.out.println("Reached waypoint at distance " + String.format("%.2f", dist) + " tiles! Advancing to next...");
                            currentTarget = null;
                            currentTargetWorld = null;
                            lastPlayerPos = null;  // Reset tracking for next waypoint
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
                    System.out.println("Moving to next waypoint in queue. Remaining: " + movementQueue.size());
                    // Reset movement tracking
                    lastPlayerPos = null;
                    lastMovementTime = Utils.rtime();
                    // Send movement command to next waypoint - use modflags=0
                    MapView mv = NUtils.getGameUI().map;
                    if(mv != null) {
                        Coord mc = ui.mc;
                        mv.wdgmsg("click", mc,
                                  currentTarget.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)).floor(posres),
                                  1, 0);
                    }
                }
            }
        }
    }
}