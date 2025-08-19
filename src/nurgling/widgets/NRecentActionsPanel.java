package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.ActionWithFinal;
import nurgling.conf.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Panel that displays recently used menu actions in a vertical column of buttons
 */
public class NRecentActionsPanel extends Widget {
    private static final int MAX_RECENT_ACTIONS = 10;
    private static final Coord BUTTON_SIZE = UI.scale(32, 32); // Same as NBotsMenu
    private static final int BUTTON_SPACING = UI.scale(2); // Same as NBotsMenu
    
    private final LinkedList<RecentAction> recentActions = new LinkedList<>();
    private final List<ActionButton> actionButtons = new ArrayList<>();
    
    public NRecentActionsPanel() {
        super(calculateSize());
        updateLayout();
    }
    
    private static Coord calculateSize() {
        // Same width as NBotsMenu (UI.scale(34)) and height for MAX_RECENT_ACTIONS buttons
        return new Coord(UI.scale(34), (BUTTON_SIZE.y + BUTTON_SPACING) * MAX_RECENT_ACTIONS + UI.scale(4));
    }
    
    /**
     * Adds a new action to the top of the recent actions stack
     */
    public synchronized void addRecentAction(MenuGrid.Pagina pagina) {
        if (pagina == null || pagina.res == null) return;
        
        try {
            final String resourceName;
            if (pagina.res instanceof Session.CachedRes.Ref) {
                resourceName = ((Session.CachedRes.Ref) pagina.res).resnm();
            } else {
                return;
            }
            
            // Create new recent action
            RecentAction newAction = new RecentAction(pagina, null, resourceName, null);
            
            // Remove duplicate if it exists
            recentActions.removeIf(action -> action.resourceName.equals(resourceName));
            
            // Add to front
            recentActions.addFirst(newAction);
            
            // Limit size
            while (recentActions.size() > MAX_RECENT_ACTIONS) {
                recentActions.removeLast();
            }
            
            // Update the UI
            updateLayout();
            
        } catch (Exception e) {
            // Silently ignore errors in action tracking
        }
    }

    /**
     * Adds a bot action to the recent actions stack
     */
    public synchronized void addBotAction(String botPath, Action botAction) {
        if (botPath == null || botAction == null) return;
        
        try {
            // Create new recent action for bot
            RecentAction newAction = new RecentAction(null, botAction, botPath, botPath);
            
            // Remove duplicate if it exists
            recentActions.removeIf(action -> action.resourceName.equals(botPath));
            
            // Add to front
            recentActions.addFirst(newAction);
            
            // Limit size
            while (recentActions.size() > MAX_RECENT_ACTIONS) {
                recentActions.removeLast();
            }
            
            // Update the UI
            updateLayout();
            
        } catch (Exception e) {
            // Silently ignore errors in action tracking
        }
    }
    
    /**
     * Removes a specific action from the recent actions list
     */
    public synchronized void removeRecentAction(RecentAction actionToRemove) {
        if (actionToRemove != null && (actionToRemove.pagina != null || actionToRemove.botAction != null)) {
            recentActions.removeIf(action -> action.resourceName.equals(actionToRemove.resourceName));
            updateLayout();
        }
    }
    
    /**
     * Updates the layout with current recent actions
     */
    private void updateLayout() {
        // Clear existing buttons
        for (ActionButton button : actionButtons) {
            if (button.parent == this) {
                button.unlink();
            }
        }
        actionButtons.clear();
        
        // Create buttons for all 10 slots (whether they have actions or not)
        for (int i = 0; i < MAX_RECENT_ACTIONS; i++) {
            Coord buttonPos = new Coord(UI.scale(1), UI.scale(2) + i * (BUTTON_SIZE.y + BUTTON_SPACING));
            
            RecentAction action = (i < recentActions.size()) ? recentActions.get(i) : null;
            ActionButton button = new ActionButton(action);
            actionButtons.add(button);
            add(button, buttonPos);
        }
    }
    
    @Override
    public void draw(GOut g) {
        super.draw(g);
    }
    
    /**
     * Represents a recent action with its associated pagina and metadata
     */
    private static class RecentAction {
        final MenuGrid.Pagina pagina;
        final Action botAction;
        final String resourceName;
        final String botPath;
        final BufferedImage icon;
        
        RecentAction(MenuGrid.Pagina pagina, Action botAction, String resourceName, String botPath) {
            this.pagina = pagina;
            this.botAction = botAction;
            this.resourceName = resourceName != null ? resourceName : "";
            this.botPath = botPath;
            this.icon = createIcon(pagina, botPath);
        }
        
        private BufferedImage createIcon(MenuGrid.Pagina pagina, String botPath) {
            BufferedImage originalIcon = null;
            
            if (pagina != null) {
                try {
                    // Try to get the button and its icon from MenuGrid
                    MenuGrid.PagButton button = pagina.button();
                    if (button != null && button.res != null) {
                        Resource.Image img = button.res.layer(Resource.imgc);
                        if (img != null) {
                            originalIcon = img.img;
                        }
                    }
                } catch (Exception e) {
                    // Fall through to fallback
                }
            } else if (botPath != null) {
                try {
                    // Try to get the icon from bot resources
                    String iconPath = "nurgling/bots/icons/" + botPath + "/u";
                    Resource res = Resource.remote().load(iconPath).get();
                    if (res != null) {
                        Resource.Image img = res.layer(Resource.imgc);
                        if (img != null) {
                            originalIcon = img.img;
                        }
                    }
                } catch (Exception e) {
                    // Fall through to fallback
                }
            }
            
            // Create a properly sized icon
            BufferedImage scaledIcon = new BufferedImage(BUTTON_SIZE.x, BUTTON_SIZE.y, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaledIcon.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw background for cell (similar to action bars and NBotsMenu)
            int bgPadding = UI.scale(1);
            g.setColor(new Color(0, 0, 0, 60)); // Darker background like action bars
            g.fillRect(bgPadding, bgPadding, BUTTON_SIZE.x - bgPadding * 2, BUTTON_SIZE.y - bgPadding * 2);
            g.setColor(new Color(80, 80, 80, 100)); // Subtle border
            g.drawRect(bgPadding, bgPadding, BUTTON_SIZE.x - bgPadding * 2, BUTTON_SIZE.y - bgPadding * 2);
            
            if (originalIcon != null) {
                // Scale the original icon to nearly fill the button (match NBotsMenu size)
                int padding = UI.scale(1); // Minimal padding for maximum icon size
                int targetWidth = BUTTON_SIZE.x - padding * 2;
                int targetHeight = BUTTON_SIZE.y - padding * 2;
                
                // Calculate scaling to maintain aspect ratio
                double scaleX = (double) targetWidth / originalIcon.getWidth();
                double scaleY = (double) targetHeight / originalIcon.getHeight();
                double scale = Math.min(scaleX, scaleY);
                
                int scaledWidth = (int) (originalIcon.getWidth() * scale);
                int scaledHeight = (int) (originalIcon.getHeight() * scale);
                
                // Center the scaled icon
                int x = (BUTTON_SIZE.x - scaledWidth) / 2;
                int y = (BUTTON_SIZE.y - scaledHeight) / 2;
                
                g.drawImage(originalIcon, x, y, scaledWidth, scaledHeight, null);
            }
            
            g.dispose();
            return scaledIcon;
        }
    }
    
    /**
     * Button widget for a recent action using IButton like NBotsMenu
     */
    private class ActionButton extends Widget {
        private final RecentAction recentAction;
        private final IButton button;
        
        ActionButton(RecentAction action) {
            super(BUTTON_SIZE);
            
            // Create empty action for null slots
            this.recentAction = (action != null) ? action : new RecentAction(null, null, "", null);
            
            // Create IButton with the action's icon (already a BufferedImage)
            this.button = new IButton(this.recentAction.icon, this.recentAction.icon, this.recentAction.icon) {
                @Override
                public void click() {
                    try {
                        if (recentAction.pagina != null) {
                            // Handle MenuGrid action
                            MenuGrid.PagButton pagButton = recentAction.pagina.button();
                            if (pagButton != null) {
                                pagButton.use(new MenuGrid.Interaction());
                            }
                        } else if (recentAction.botAction != null) {
                            // Handle bot action - mirror the behavior from NBotsMenu.start()
                            Thread t = new Thread(() -> {
                                ArrayList<Thread> supports = new ArrayList<>();
                                try {
                                    NGameUI gui = NUtils.getGameUI();
                                    if (gui != null) {
                                        // Start support actions first
                                        for (Action sup : recentAction.botAction.getSupp()) {
                                            Thread st = new Thread(() -> {
                                                try {
                                                    sup.run(gui);
                                                } catch (InterruptedException e) {
                                                    // Support action interrupted
                                                }
                                            });
                                            supports.add(st);
                                            st.start();
                                        }
                                        // Run main action
                                        recentAction.botAction.run(gui);
                                    }
                                } catch (InterruptedException e) {
                                    if (NUtils.getGameUI() != null) {
                                        NUtils.getGameUI().msg(recentAction.botPath + ": STOPPED");
                                    }
                                } finally {
                                    // Clean up like the original
                                    if (recentAction.botAction instanceof ActionWithFinal) {
                                        ((ActionWithFinal) recentAction.botAction).endAction();
                                    }
                                    for (Thread st : supports) {
                                        st.interrupt();
                                    }
                                }
                            }, recentAction.botPath + "-RecentAction");
                            
                            // Add to bot interrupt widget so the gear appears
                            NGameUI gui = NUtils.getGameUI();
                            if (gui != null && gui.biw != null) {
                                gui.biw.addObserve(t);
                            }
                            t.start();
                        }
                    } catch (Exception e) {
                        // Silently ignore errors in action execution
                    }
                }

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    if (recentAction.pagina != null || recentAction.botAction != null) {
                        return "Right-click to remove";
                    }
                    return null;
                }
            };
            
            add(button, Coord.z);
        }
        
        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 3 && (recentAction.pagina != null || recentAction.botAction != null)) { // Right click on any action
                // Remove this action from the recent actions list
                removeRecentAction(recentAction);
                return true;
            }
            return super.mousedown(ev);
        }
    }
}
