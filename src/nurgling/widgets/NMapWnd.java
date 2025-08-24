package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.ResourceTimerUtils;

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
        // Check if the click is on the view area and it's a shift+right-click
        if(ev.b == 3 && ui.modshift && view.c != null) {
            // Convert global coordinates to view coordinates
            Coord viewCoord = ev.c.sub(view.parentpos(this));
            
            // Check if the click is within the view bounds
            if(viewCoord.x >= 0 && viewCoord.x < view.sz.x && 
               viewCoord.y >= 0 && viewCoord.y < view.sz.y) {
                
                // Check if there's a resource marker at this location
                if(handleResourceTimerClick(viewCoord)) {
                    return true; // Consume the event
                }
            }
        }
        
        return super.mousedown(ev);
    }
    
    private boolean handleResourceTimerClick(Coord c) {
        // Try to find a resource marker at the clicked location
        MiniMap.Location clickLoc = view.xlate(c);
        if(clickLoc == null) return false;
        
        MiniMap.DisplayMarker marker = view.markerat(clickLoc.tc);
        if(marker != null && marker.m instanceof MapFile.SMarker) {
            MapFile.SMarker smarker = (MapFile.SMarker) marker.m;
            
            // Check if this is a localized resource (map resource)
            if(ResourceTimerUtils.isTimerSupportedResource(smarker.res.name)) {
                ResourceTimerUtils.showResourceTimerDialog(smarker, clickLoc);
                return true;
            }
        }
        
        return false;
    }
}
