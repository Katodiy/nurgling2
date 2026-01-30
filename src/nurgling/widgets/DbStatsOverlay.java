package nurgling.widgets;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.db.DatabaseManager;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug overlay that displays database operation statistics, FPS, and active tasks.
 * Toggle with F11.
 */
public class DbStatsOverlay extends Widget {
    private static final Color BG_COLOR = new Color(0, 0, 0, 200);
    private static final Color TEXT_COLOR = new Color(0, 255, 100);
    private static final Color WARN_COLOR = new Color(255, 200, 0);
    private static final Color ERROR_COLOR = new Color(255, 80, 80);
    private static final Color HEADER_COLOR = new Color(100, 200, 255);
    private static final Color TASK_COLOR = new Color(200, 200, 200);
    
    private double updateTimer = 0;
    private static final double UPDATE_INTERVAL = 0.2; // Update 5 times per second
    
    // Cached data
    private List<String> lines = new ArrayList<>();
    private List<Color> lineColors = new ArrayList<>();
    
    public DbStatsOverlay() {
        super(new Coord(300, 200));
    }
    
    @Override
    public void tick(double dt) {
        super.tick(dt);
        updateTimer += dt;
        
        if (updateTimer >= UPDATE_INTERVAL) {
            updateTimer = 0;
            updateStats();
            // Resize based on content
            int newHeight = Math.max(100, lines.size() * 12 + 15);
            if (sz.y != newHeight) {
                resize(new Coord(sz.x, newHeight));
            }
        }
    }
    
    private void updateStats() {
        lines.clear();
        lineColors.clear();
        
        try {
            // === FPS ===
            int fps = getFps();
            addLine("=== DEBUG OVERLAY ===", HEADER_COLOR);
            addLine(String.format("FPS: %d", fps), fps < 30 ? WARN_COLOR : TEXT_COLOR);
            
            // === ACTIVE TASKS ===
            if (NUtils.getUI() != null && NUtils.getUI().core != null) {
                String[] taskNames = NUtils.getUI().core.getActiveTaskNames();
                int taskCount = taskNames.length;
                addLine(String.format("--- TASKS: %d ---", taskCount), HEADER_COLOR);
                
                if (taskCount > 0) {
                    // Show up to 5 tasks
                    int shown = Math.min(taskCount, 5);
                    for (int i = 0; i < shown; i++) {
                        addLine("  " + taskNames[i], TASK_COLOR);
                    }
                    if (taskCount > 5) {
                        addLine(String.format("  ... +%d more", taskCount - 5), TASK_COLOR);
                    }
                }
            }
            
            // === DATABASE ===
            if ((Boolean) NConfig.get(NConfig.Key.ndbenable)) {
                DatabaseManager.DbStats stats = DatabaseManager.getStats();
                int containerCacheSize = monitoring.ItemWatcher.getContainerCacheSize();
                int recipeCacheSize = nurgling.NCore.getRecipeCacheSize();
                int quickCacheSize = nurgling.NCore.getRecipeQuickCacheSize();
                int pendingRecipeTasks = nurgling.NCore.getPendingRecipeTasks();
                
                addLine("--- DATABASE ---", HEADER_COLOR);
                addLine(String.format("Status: %s | Ops/s: %d | Total: %d", 
                    stats.isReady ? "OK" : "ERR", stats.opsPerSecond, stats.totalOps),
                    stats.isReady ? TEXT_COLOR : ERROR_COLOR);
                addLine(String.format("Pending: %d | Queue: %d | RecTasks: %d", 
                    stats.pending, stats.queueSize, pendingRecipeTasks),
                    stats.pending > 5 ? WARN_COLOR : TEXT_COLOR);
                addLine(String.format("Skip: C:%d R:%d S:%d", 
                    stats.skippedContainer, stats.skippedRecipe, stats.skippedSearch), TEXT_COLOR);
                addLine(String.format("Cache: C:%d Q:%d R:%d", 
                    containerCacheSize, quickCacheSize, recipeCacheSize), TEXT_COLOR);
            } else {
                addLine("--- DATABASE: OFF ---", WARN_COLOR);
            }
            
        } catch (Exception e) {
            addLine("Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown"), ERROR_COLOR);
        }
    }
    
    private void addLine(String text, Color color) {
        lines.add(text);
        lineColors.add(color);
    }
    
    private int getFps() {
        try {
            if (ui != null && ui.root != null) {
                // Get FPS from GLPanel via reflection or public field
                Widget root = ui.root;
                if (root.parent instanceof GLPanel) {
                    GLPanel panel = (GLPanel) root.parent;
                    // fps is protected, try to access it
                    java.lang.reflect.Field fpsField = GLPanel.class.getDeclaredField("fps");
                    fpsField.setAccessible(true);
                    return fpsField.getInt(panel);
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return -1;
    }
    
    @Override
    public void draw(GOut g) {
        // Background
        g.chcolor(BG_COLOR);
        g.frect(Coord.z, sz);
        g.chcolor();
        
        // Border
        g.chcolor(HEADER_COLOR);
        g.rect(Coord.z, sz);
        
        // Draw lines
        int y = 5;
        int lineHeight = 12;
        
        for (int i = 0; i < lines.size(); i++) {
            g.chcolor(lineColors.get(i));
            g.text(lines.get(i), new Coord(5, y));
            y += lineHeight;
        }
        
        g.chcolor();
    }
    
    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if (ev.b == 3) { // Right-click to hide
            hide();
            return true;
        }
        return super.mousedown(ev);
    }
    
    /**
     * Check if overlay should be visible based on config
     */
    public static boolean shouldShow() {
        return (Boolean) NConfig.get(NConfig.Key.ndbenable) && 
               (Boolean) NConfig.get(NConfig.Key.dbStatsOverlay);
    }
}
