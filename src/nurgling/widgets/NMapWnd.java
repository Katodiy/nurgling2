package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.ResourceTimer;
import nurgling.ResourceTimerManager;

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
        // Check if the click is on the view area and it's a right-click
        if(ev.b == 3 && view.c != null) {
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
            if(smarker.res != null && smarker.res.name.startsWith("gfx/terobjs/mm")) {
                showResourceTimerDialog(smarker, clickLoc);
                return true;
            }
        }
        
        return false;
    }
    
    private void showResourceTimerDialog(MapFile.SMarker marker, MiniMap.Location loc) {
        ResourceTimerManager manager = ResourceTimerManager.getInstance();
        String resourceType = marker.res.name;
        
        // Check if timer already exists
        ResourceTimer existingTimer = manager.getTimer(marker.seg, marker.tc, resourceType);
        
        if(existingTimer != null) {
            // Show options to manage existing timer
            showExistingTimerDialog(existingTimer);
        } else {
            // Show dialog to create new timer
            showCreateTimerDialog(marker, loc);
        }
    }
    
    private void showExistingTimerDialog(ResourceTimer timer) {
        // Create a simple dialog showing timer info with options
        String message = String.format("Timer for %s:\n%s remaining\n\nOptions:",
                                     timer.getDescription(), 
                                     timer.getFormattedRemainingTime());
        
        Object[] options = {"Remove Timer", "Cancel"};
        int choice = javax.swing.JOptionPane.showOptionDialog(
            null, message, "Resource Timer", 
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.INFORMATION_MESSAGE,
            null, options, options[1]);
            
        if(choice == 0) { // Remove Timer
            ResourceTimerManager.getInstance().removeTimer(timer.getResourceId());
        }
    }
    
    private void showCreateTimerDialog(MapFile.SMarker marker, MiniMap.Location loc) {
        // Create input dialog for timer duration
        String input = javax.swing.JOptionPane.showInputDialog(
            null,
            "Enter cooldown time for " + getResourceDisplayName(marker.res.name) + 
            "\n(e.g., \"8 hrs 23 mins\" or \"Come back in 8 hrs and 23 minutes\")",
            "Add Resource Timer",
            javax.swing.JOptionPane.PLAIN_MESSAGE);
            
        if(input != null && !input.trim().isEmpty()) {
            try {
                long duration = ResourceTimer.parseDurationString(input);
                if(duration > 0) {
                    ResourceTimerManager manager = ResourceTimerManager.getInstance();
                    manager.addTimer(marker.seg, marker.tc, marker.nm, marker.res.name, 
                                   duration, getResourceDisplayName(marker.res.name));
                } else {
                    javax.swing.JOptionPane.showMessageDialog(null, 
                        "Could not parse time from: " + input, 
                        "Invalid Input", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            } catch(Exception e) {
                javax.swing.JOptionPane.showMessageDialog(null, 
                    "Error creating timer: " + e.getMessage(), 
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private String getResourceDisplayName(String resourceType) {
        // Convert resource paths to user-friendly names
        String name = resourceType;
        if(name.startsWith("gfx/terobjs/map/")) {
            name = name.substring("gfx/terobjs/map/".length());
        }
        
        // Convert common resource names to proper display names
        switch(name) {
            case "tarpit": return "Tar Pit";
            case "jotunclam": return "Jotun Clam";
            case "cavepuddle": return "Cave Puddle";
            case "squirrelcache": return "Squirrel Cache";
            case "naturalminesupport": return "Natural Mine Support";
            case "saltbasin": return "Salt Basin";
            case "wellspring": return "Wellspring";
            case "coralreef": return "Coral Reef";
            case "claypit": return "Clay Pit";
            case "rockcrystal": return "Rock Crystal";
            case "lilypadlotus": return "Lilypad Lotus";
            case "abyssalchasm": return "Abyssal Chasm";
            case "ancientwindthrow": return "Ancient Windthrow";
            default:
                // Default formatting: capitalize and replace underscores
                name = name.replace("_", " ");
                if(name.length() > 0) {
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                }
                return name;
        }
    }
}
