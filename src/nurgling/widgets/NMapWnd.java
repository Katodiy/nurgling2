package nurgling.widgets;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;

import java.util.Map;

import static haven.MCache.tilesz;

public class NMapWnd extends MapWnd {
    public String searchPattern = "";
    public Resource.Image searchRes = null;
    public boolean needUpdate = false;
    TextEntry te;

    public NMapWnd(MapFile file, MapView mv, Coord sz, String title) {
        super(file, mv, sz, title);
        searchRes = Resource.local().loadwait("alttex/selectedtex").layer(Resource.imgc);
        add(te = new TextEntry(200,""){
            @Override
            public void done(ReadLine buf) {
                super.done(buf);
                searchPattern = text();
                view.needUpdate = true;
                NUtils.getGameUI().mmap.needUpdate = true;
            }
        }, view.pos("br").sub(UI.scale(200,20)));
    }

    public long playerSegmentId() {
        MiniMap.Location sessloc = view.sessloc;
        if(sessloc == null) {return 0;}
        return sessloc.seg.id;
    }

    public Coord2d findMarkerPosition(String name) {
        MiniMap.Location sessloc = view.sessloc;
        if(sessloc == null) {return null;}
        for (Map.Entry<Long, MapFile.SMarker> e : file.smarkers.entrySet()) {
            MapFile.SMarker m = e.getValue();
            if(m.seg == sessloc.seg.id && m.nm!= null && name!=null && m.nm.contains(name)) {
                return m.tc.sub(sessloc.tc).mul(tilesz);
            }
        }
        return null;
    }

    @Override
    public void resize(Coord sz) {
        super.resize(sz);
        if(te!=null)
            te.c = view.pos("br").sub(UI.scale(200,20));
    }
    
    @Override
    public boolean mousedown(MouseDownEvent ev) {
        // Handle ctrl+left-click for waypoint queueing (on button release handled below)
        // Handle shift+right-click for resource timers
        if(view.c != null) {
            // Convert global coordinates to view coordinates
            Coord viewCoord = ev.c.sub(view.parentpos(this));

            // Check if the click is within the view bounds
            if(viewCoord.x >= 0 && viewCoord.x < view.sz.x &&
               viewCoord.y >= 0 && viewCoord.y < view.sz.y) {

                // Shift+right-click for resource timers
                if(ev.b == 3 && ui.modshift) {
                    // Check if there's a resource marker at this location
                    if(handleResourceTimerClick(viewCoord)) {
                        return true; // Consume the event
                    }
                }

                // Alt+right-click for fish locations
                if(ev.b == 3 && ui.modmeta) {
                    // Check if there's a fish location at this position
                    if(handleFishLocationClick(viewCoord)) {
                        return true; // Consume the event
                    }
                }
            }
        }

        return super.mousedown(ev);
    }

    @Override
    public boolean mouseup(MouseUpEvent ev) {
        if(view.c != null) {
            Coord viewCoord = ev.c.sub(view.parentpos(this));

            // Check if the click is within the view bounds
            if(viewCoord.x >= 0 && viewCoord.x < view.sz.x &&
               viewCoord.y >= 0 && viewCoord.y < view.sz.y) {

                // Ctrl+left-click for waypoint queueing
                if(ev.b == 1 && ui.modctrl) {
                    if(handleWaypointClick(viewCoord)) {
                        return true; // Consume the event
                    }
                }

                // Right-click to clear waypoint queue
                if(ev.b == 3) {
                    NGameUI gui = (NGameUI) NUtils.getGameUI();
                    if(gui != null && gui.waypointMovementService != null) {
                        gui.waypointMovementService.clearQueue();
                    }
                }
            }
        }

        return super.mouseup(ev);
    }

    private boolean handleWaypointClick(Coord c) {
        // Try to get the location at clicked coordinates
        MiniMap.Location clickLoc = view.xlate(c);
        if(clickLoc == null || view.sessloc == null) return false;

        // Only handle if in same segment
        if(clickLoc.seg.id != view.sessloc.seg.id) return false;

        // Use the service to add waypoint
        NGameUI gui = (NGameUI) NUtils.getGameUI();
        if(gui != null && gui.waypointMovementService != null) {
            gui.waypointMovementService.addWaypoint(clickLoc, view.sessloc);
            return true;
        }

        return false;
    }
    
    private boolean handleResourceTimerClick(Coord c) {
        // Try to find a resource marker at the clicked location
        MiniMap.Location clickLoc = view.xlate(c);
        if(clickLoc == null) return false;

        MiniMap.DisplayMarker marker = view.markerat(clickLoc.tc);
        if(marker != null && marker.m instanceof MapFile.SMarker) {
            MapFile.SMarker smarker = (MapFile.SMarker) marker.m;

            // Handle through service
            NGameUI gui = (NGameUI) NUtils.getGameUI();
            if(gui != null && gui.localizedResourceTimerService != null) {
                return gui.localizedResourceTimerService.handleResourceClick(smarker);
            }
        }

        return false;
    }

    private boolean handleFishLocationClick(Coord c) {
        // c is in view coordinates, check for fish locations in screen space
        if(view.sessloc == null || view.dloc == null) return false;

        NGameUI gui = (NGameUI) NUtils.getGameUI();
        if(gui == null || gui.fishLocationService == null) return false;

        // Find fish location at this position (check in screen space)
        java.util.List<nurgling.FishLocation> locations = gui.fishLocationService.getFishLocationsForSegment(view.sessloc.seg.id);
        int threshold = UI.scale(10); // Screen pixels
        Coord hsz = view.sz.div(2);

        for(nurgling.FishLocation loc : locations) {
            // Convert segment-relative coordinates to screen coordinates (same as drawing)
            Coord screenPos = loc.getTileCoords().sub(view.dloc.tc).div(view.scalef()).add(hsz);

            if(c.dist(screenPos) < threshold) {
                gui.fishLocationService.removeFishLocation(loc.getLocationId());
                gui.msg("Removed " + loc.getFishName() + " location", java.awt.Color.YELLOW);
                return true;
            }
        }

        return false;
    }
}
