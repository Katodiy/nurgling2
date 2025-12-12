package nurgling;

import haven.*;
import haven.Charlist;

import java.util.*;

public class NCharlist extends Charlist {
    public static NCharlist instance;
    private Dropbox<String> worldSelector;
    private String selectedWorld = null; // null means "All"
    private List<Char> filteredChars = new ArrayList<>();
    
    public NCharlist(int height) {
        super(height);
        instance = this;
        
        // Load saved world selection
        selectedWorld = (String) NConfig.get(NConfig.Key.selectedWorld);
        
        // Create world selector combobox
        worldSelector = new Dropbox<String>(UI.scale(150), 10, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                List<String> worlds = getWorlds();
                if(i == 0) return "All Worlds";
                return worlds.get(i - 1);
            }
            
            @Override
            protected int listitems() {
                return getWorlds().size() + 1; // +1 for "All"
            }
            
            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                if("All Worlds".equals(item)) {
                    selectedWorld = null;
                } else {
                    selectedWorld = item;
                }
                // Save selection to config
                NConfig.set(NConfig.Key.selectedWorld, selectedWorld);
                updateFilteredList();
            }
        };
        
        // Position combobox above the character list (inside the charlist widget)
        // Will be repositioned when the widget is resized
    }
    
    @Override
    protected void added() {
        super.added();
        // Add world selector to parent (ccnt container) so it's visible
        if(worldSelector.parent == null) {
            parent.add(worldSelector, c.x, c.y - worldSelector.sz.y - UI.scale(10));
        }
    }
    
    private List<String> getWorlds() {
        Set<String> worlds = new LinkedHashSet<>();
        synchronized(chars) {
            for(Char c : chars) {
                if(c.disc != null && !c.disc.isEmpty()) {
                    worlds.add(c.disc);
                }
            }
        }
        return new ArrayList<>(worlds);
    }
    
    private void updateFilteredList() {
        filteredChars.clear();
        synchronized(chars) {
            for(Char c : chars) {
                if(selectedWorld == null || selectedWorld.equals(c.disc)) {
                    filteredChars.add(c);
                }
            }
        }
        // Select first character if current selection is not in filtered list
        if(list.sel != null && !filteredChars.contains(list.sel)) {
            if(!filteredChars.isEmpty()) {
                list.change(filteredChars.get(0));
            }
        }
    }
    
    // Override to return filtered list
    @Override
    protected List<Char> getDisplayChars() {
        if(selectedWorld == null) {
            return chars;
        }
        return filteredChars;
    }

    public static void play() {
        if(NConfig.botmod != null && instance != null) {
            for(Char c : instance.chars) {
                if(c.name.equals(NConfig.botmod.character)) {
                    instance.wdgmsg("play", NConfig.botmod.character);
                    instance = null;
                    break;
                }
            }
        }
    }

    @Override
    public void uimsg(String msg, Object... args) {
        super.uimsg(msg, args);
        if(msg == "add" || msg == "srv") {
            // Update filtered list when characters are added or world info arrives
            updateFilteredList();
            // Update combobox selection to match saved value
            if(selectedWorld != null && worldSelector.sel == null) {
                List<String> worlds = getWorlds();
                if(worlds.contains(selectedWorld)) {
                    worldSelector.change(selectedWorld);
                }
            }
            NCharlist.play();
        }
    }
}
