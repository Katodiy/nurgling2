package nurgling.widgets;

import haven.*;
import nurgling.LocalizedResourceTimer;
import nurgling.LocalizedResourceTimerService;
import nurgling.NStyle;

import java.util.List;
import java.util.ArrayList;

/**
 * Window for managing resource timers
 */
public class LocalizedResourceTimersWindow extends Window {
    private static final Coord WINDOW_SIZE = UI.scale(new Coord(350, 250));
    
    private final LocalizedResourceTimerService localizedResourceTimerService;
    private final ArrayList<TimerItem> items = new ArrayList<>();

    public LocalizedResourceTimersWindow(LocalizedResourceTimerService localizedResourceTimerService) {
        super(WINDOW_SIZE, "Resource Timers");
        this.localizedResourceTimerService = localizedResourceTimerService;
        
        // Create the timer list
        add(new TimerList(new Coord(WINDOW_SIZE.x - UI.scale(20), WINDOW_SIZE.y - UI.scale(40))),
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
            if(localizedResourceTimerService != null) {
                List<LocalizedResourceTimer> timers = new ArrayList<>(localizedResourceTimerService.getAllTimers());
                // Sort by remaining time (expired first, then by time remaining)
                timers.sort((a, b) -> {
                    boolean aExpired = a.isExpired();
                    boolean bExpired = b.isExpired();

                    if (aExpired != bExpired) {
                        return aExpired ? -1 : 1; // Expired timers first
                    }

                    // Both expired or both active - sort by remaining time
                    return Long.compare(a.getRemainingTime(), b.getRemainingTime());
                });
                
                for(LocalizedResourceTimer timer : timers) {
                    items.add(new TimerItem(timer));
                }
            }
        }
    }
    
    // TimerItem class - represents individual timer entries
    public class TimerItem extends Widget {
        private final LocalizedResourceTimer timer;
        private final Label nameLabel;
        private final Label timeLabel;
        private final IButton removeButton;
        
        public TimerItem(LocalizedResourceTimer timer) {
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
                    if(localizedResourceTimerService != null) {
                        localizedResourceTimerService.removeTimer(TimerItem.this.timer.getResourceId());
                        LocalizedResourceTimersWindow.this.refreshTimers();
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
    
    private void showTimerDetails(LocalizedResourceTimer timer) {
        if(localizedResourceTimerService != null) {
            localizedResourceTimerService.openMapAtLocalizedResourceLocation(timer);
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