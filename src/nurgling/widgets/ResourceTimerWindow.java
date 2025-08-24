package nurgling.widgets;

import haven.*;
import nurgling.ResourceTimer;
import nurgling.ResourceTimerManager;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NStyle;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Window for managing resource timers
 */
public class ResourceTimerWindow extends Window {
    private static final Coord WINDOW_SIZE = UI.scale(new Coord(350, 250));
    
    private ResourceTimerManager manager;
    private final ArrayList<TimerItem> items = new ArrayList<>();
    private TimerList timerList;
    
    public ResourceTimerWindow() {
        super(WINDOW_SIZE, "Resource Timers");
        
        // Create the timer list
        timerList = add(new TimerList(new Coord(WINDOW_SIZE.x - UI.scale(20), WINDOW_SIZE.y - UI.scale(40))), 
                       UI.scale(new Coord(10, 30)));
        
        hide(); // Start hidden
    }
    
    @Override
    public void show() {
        // Refresh manager reference when showing
        NGameUI gui = (NGameUI) NUtils.getGameUI();
        this.manager = gui != null ? gui.resourceTimerManager : null;
        refreshTimers();
        super.show();
    }
    
    private long lastRefresh = 0;
    
    @Override
    public void tick(double dt) {
        super.tick(dt);
        
        // Refresh timer list every 5 seconds if window is visible
        if(visible() && System.currentTimeMillis() - lastRefresh > 5000) {
            refreshTimers();
            lastRefresh = System.currentTimeMillis();
        }
    }
    
    public void refreshTimers() {
        synchronized (items) {
            items.clear();
            if(manager != null) {
                List<ResourceTimer> timers = new ArrayList<>(manager.getAllTimers());
                // Sort by remaining time (expired first, then by time remaining)
                Collections.sort(timers, new Comparator<ResourceTimer>() {
                    @Override
                    public int compare(ResourceTimer a, ResourceTimer b) {
                        boolean aExpired = a.isExpired();
                        boolean bExpired = b.isExpired();
                        
                        if(aExpired != bExpired) {
                            return aExpired ? -1 : 1; // Expired timers first
                        }
                        
                        // Both expired or both active - sort by remaining time
                        return Long.compare(a.getRemainingTime(), b.getRemainingTime());
                    }
                });
                
                for(ResourceTimer timer : timers) {
                    items.add(new TimerItem(timer));
                }
            }
        }
    }
    
    // TimerItem class - represents individual timer entries
    public class TimerItem extends Widget {
        private final ResourceTimer timer;
        private Label nameLabel;
        private Label timeLabel;
        private IButton removeButton;
        
        public TimerItem(ResourceTimer timer) {
            this.timer = timer;
            
            // Create name label
            String displayName = timer.getDescription();
            if(displayName.length() > 22) {
                displayName = displayName.substring(0, 19) + "...";
            }
            nameLabel = add(new Label(displayName), UI.scale(new Coord(5, 2)));
            
            // Create time label
            String timeText = timer.getFormattedRemainingTime();
            timeLabel = add(new Label(timeText), UI.scale(new Coord(180, 2)));
            
            // Create remove button
            removeButton = add(new IButton(NStyle.removei[0].back, NStyle.removei[1].back, NStyle.removei[2].back) {
                @Override
                public void click() {
                    if(manager != null) {
                        manager.removeTimer(TimerItem.this.timer.getResourceId());
                        ResourceTimerWindow.this.refreshTimers();
                    }
                }
            }, UI.scale(new Coord(280, 0)));
            removeButton.settip("Remove timer");
            
            pack();
        }
        
        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if(ev.b == 1) {
                // Check if click is on the remove button area
                Coord buttonPos = removeButton.c;
                Coord buttonSize = removeButton.sz;
                
                if(ev.c.x >= buttonPos.x && ev.c.x <= buttonPos.x + buttonSize.x &&
                   ev.c.y >= buttonPos.y && ev.c.y <= buttonPos.y + buttonSize.y) {
                    // Let the button handle the click
                    return super.mousedown(ev);
                } else {
                    // Click elsewhere on the row - show timer details
                    showTimerDetails(timer);
                    return true;
                }
            }
            return super.mousedown(ev);
        }
        
        @Override
        public void draw(GOut g) {
            // Highlight expired timers with background
            if(timer.isExpired()) {
                g.chcolor(0, 128, 0, 30);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
            
            // Draw the widget normally first
            super.draw(g);
            
            // Override text color for expired timers
            if(timer.isExpired()) {
                Text.Foundry fnd = new Text.Foundry(Text.dfont, 12).aa(true);
                g.chcolor(java.awt.Color.GREEN);
                
                // Redraw name text in green
                String displayName = timer.getDescription();
                if(displayName.length() > 22) {
                    displayName = displayName.substring(0, 19) + "...";
                }
                g.image(fnd.render(displayName, java.awt.Color.GREEN).tex(), nameLabel.c);
                
                // Redraw time text in green
                String timeText = timer.getFormattedRemainingTime();
                g.image(fnd.render(timeText, java.awt.Color.GREEN).tex(), timeLabel.c);
                
                g.chcolor();
            }
        }
    }
    
    // TimerList class - scrollable list of timers
    public class TimerList extends SListBox<TimerItem, Widget> {
        TimerList(Coord sz) {
            super(sz, UI.scale(25)); // 25 pixel row height
        }
        
        public List<TimerItem> items() {
            synchronized (items) {
                return items;
            }
        }
        
        protected Widget makeitem(TimerItem item, int idx, Coord sz) {
            return new ItemWidget<TimerItem>(this, sz, item) {
                {
                    add(item);
                }
            };
        }
        
        
        @Override
        public void resize(Coord sz) {
            super.resize(sz);
        }
    }
    
    private void showTimerDetails(ResourceTimer timer) {
        // Navigate to resource on click for now, show details in game message
        String details = String.format(
            "Timer: %s | Location: Segment %d (%d, %d) | %s | %s",
            timer.getDescription(),
            timer.getSegmentId(),
            timer.getTileCoords().x, timer.getTileCoords().y,
            timer.getFormattedRemainingTime(),
            timer.isExpired() ? "Ready!" : "Cooling down"
        );
        
        // Show details in game chat
        if(this.ui instanceof nurgling.NUI) {
            nurgling.NUI nui = (nurgling.NUI)this.ui;
            if(nui.gui != null) {
                nui.gui.msg(details, java.awt.Color.CYAN);
            }
        }
        
        // Automatically navigate to the resource
        navigateToResource(timer);
    }
    
    private void navigateToResource(ResourceTimer timer) {
        // Open the map window and center on the resource location
        try {
            if(this.ui != null && this.ui instanceof nurgling.NUI) {
                nurgling.NUI nui = (nurgling.NUI)this.ui;
                if(nui.gui != null) {
                    // Open the map window if it's not already open
                    if(nui.gui.mapfile == null || !nui.gui.mapfile.visible()) {
                        nui.gui.togglewnd(nui.gui.mapfile);
                    }
                    
                    // Center both minimap and main map on the resource
                    if(nui.gui.mmap != null) {
                        // Find the segment and create a location
                        MapFile.Segment segment = nui.gui.mmap.file.segments.get(timer.getSegmentId());
                        if(segment != null) {
                            MiniMap.Location targetLoc = new MiniMap.Location(segment, timer.getTileCoords());
                            
                            // Temporarily disable following to allow manual centering
                            if(nui.gui.mmap instanceof nurgling.widgets.NMiniMapWnd.Map) {
                                nurgling.widgets.NMiniMapWnd.Map miniMapWidget = (nurgling.widgets.NMiniMapWnd.Map) nui.gui.mmap;
                                miniMapWidget.follow(null); // Stop following player
                            }
                            
                            // Center the minimap
                            nui.gui.mmap.center(targetLoc);
                            
                            // Center the main map if it exists
                            if(nui.gui.mapfile != null && nui.gui.mapfile instanceof nurgling.widgets.NMapWnd) {
                                nurgling.widgets.NMapWnd mapWnd = (nurgling.widgets.NMapWnd) nui.gui.mapfile;
                                mapWnd.view.center(targetLoc);
                                
                                // Also disable following on main map view if it has it
                                if(mapWnd.view instanceof MiniMap) {
                                    ((MiniMap) mapWnd.view).follow(null);
                                }
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            // Show error in game message instead of dialog
            if(this.ui instanceof nurgling.NUI) {
                nurgling.NUI nui = (nurgling.NUI)this.ui;
                if(nui.gui != null) {
                    nui.gui.msg("Navigation error: " + e.getMessage(), java.awt.Color.RED);
                }
            }
        }
    }
    
    private String formatTimestamp(long timestamp) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
            instant, java.time.ZoneId.systemDefault());
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
    }
    
    private String formatDuration(long durationMs) {
        long hours = durationMs / (1000 * 60 * 60);
        long minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60);
        
        if(hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    
    @Override
    public void wdgmsg(String msg, Object... args) {
        if(msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(msg, args);
        }
    }
    
    @Override
    public boolean keydown(KeyDownEvent ev) {
        // Handle keyboard shortcuts
        if(ev.code == java.awt.event.KeyEvent.VK_ESCAPE) {
            hide();
            return true;
        } else if(ev.code == java.awt.event.KeyEvent.VK_F5) {
            refreshTimers();
            return true;
        }
        return super.keydown(ev);
    }
}