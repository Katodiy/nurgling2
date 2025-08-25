package nurgling.widgets;

import haven.*;
import nurgling.ResourceTimer;
import nurgling.ResourceTimerService;
import nurgling.NUtils;
import nurgling.NStyle;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Window for managing resource timers
 */
public class ResourceTimersWindow extends Window {
    private static final Coord WINDOW_SIZE = UI.scale(new Coord(350, 250));
    
    private ResourceTimerService service;
    private final ArrayList<TimerItem> items = new ArrayList<>();
    private TimerList timerList;
    
    public ResourceTimersWindow(ResourceTimerService service) {
        super(WINDOW_SIZE, "Resource Timers");
        this.service = service;
        
        // Create the timer list
        timerList = add(new TimerList(new Coord(WINDOW_SIZE.x - UI.scale(20), WINDOW_SIZE.y - UI.scale(40))), 
                       UI.scale(new Coord(10, 30)));
        
        hide(); // Start hidden
    }
    
    @Override
    public void show() {
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
            if(service != null) {
                List<ResourceTimer> timers = new ArrayList<>(service.getAllTimers());
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
                    if(service != null) {
                        service.removeTimer(TimerItem.this.timer.getResourceId());
                        ResourceTimersWindow.this.refreshTimers();
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
        // Show details and navigate to resource
        String details = String.format(
            "Timer: %s | Location: Segment %d (%d, %d) | %s | %s",
            timer.getDescription(),
            timer.getSegmentId(),
            timer.getTileCoords().x, timer.getTileCoords().y,
            timer.getFormattedRemainingTime(),
            timer.isExpired() ? "Ready!" : "Cooling down"
        );
        
        showMessage(details, java.awt.Color.CYAN);
        if(service != null) {
            service.navigateToResourceTimer(timer);
        }
    }
    
    private void showMessage(String message, java.awt.Color color) {
        nurgling.NUI nui = getNUI();
        if(nui != null && nui.gui != null) {
            nui.gui.msg(message, color);
        }
    }
    
    private nurgling.NUI getNUI() {
        return (this.ui instanceof nurgling.NUI) ? (nurgling.NUI)this.ui : null;
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