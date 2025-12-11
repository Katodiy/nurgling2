package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.MCache;
import haven.Resource;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.actions.SelectFlowerAction;
import nurgling.overlays.NCheckResult;
import nurgling.tasks.WaitItemContent;
import nurgling.tools.NAlias;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static haven.OCache.posres;

public class CheckWater implements Action {
    NAlias cups = new NAlias("Wooden Cup", "Kuksa");
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem item = gui.getInventory().getItem(cups);
        Coord pos = item.c.div(Inventory.sqsz);
        NUtils.takeItemToHand(item);
        gui.map.wdgmsg("itemact", Coord.z, NUtils.player().rc.floor(posres), 3, 0);
        NUtils.addTask(new WaitItemContent(gui.vhand));
        String water = ((NGItem)gui.vhand.item).content().get(0).type();
        double quality = ((NGItem)gui.vhand.item).content().get(0).quality();
        NUtils.getGameUI().msg(water + " " + quality);
        NUtils.player().addcustomol(new NCheckResult(NUtils.player(),quality,water));
        
        // Add labeled mark to minimap with quality label (persisted to file)
        addLabeledMinimapMark(gui, quality, water);
        
        gui.getInventory().dropOn(pos,cups);
        for(WItem titem : gui.getInventory().getItems(cups))
        {
            if(!((NGItem)titem.item).content().isEmpty() && ((NGItem)titem.item).content().get(0).type().equals(water))
            {
                new SelectFlowerAction("Empty",titem).run(gui);
            }
        }

        return Results.SUCCESS();
    }
    
    // Cache for water icons loaded from game resources
    private static final Map<String, BufferedImage> waterIconCache = new HashMap<>();
    
    // Mapping of water type names to resource paths
    private static final Map<String, String> WATER_RESOURCE_PATHS = new HashMap<>();
    static {
        WATER_RESOURCE_PATHS.put("Water", "gfx/invobjs/water");
        WATER_RESOURCE_PATHS.put("Saltwater", "gfx/invobjs/saltwater");
    }
    
    /**
     * Get water icon from game resources.
     */
    private BufferedImage getWaterIcon(String waterType) {
        // Check cache first
        if(waterIconCache.containsKey(waterType)) {
            return waterIconCache.get(waterType);
        }
        
        // Get resource path for this water type
        String resourcePath = WATER_RESOURCE_PATHS.get(waterType);
        if(resourcePath == null) {
            resourcePath = WATER_RESOURCE_PATHS.get("Water"); // Default to regular water
        }
        
        try {
            Resource res = Resource.remote().loadwait(resourcePath);
            BufferedImage icon = res.layer(Resource.imgc).img;
            waterIconCache.put(waterType, icon);
            return icon;
        } catch(Exception e) {
            System.err.println("Failed to load water icon: " + resourcePath + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Add a labeled mark to the minimap showing water quality.
     * Uses LabeledMarkService for persistence between sessions.
     */
    private void addLabeledMinimapMark(NGameUI gui, double quality, String waterType) {
        try {
            if(gui.mmap == null || gui.mmap.sessloc == null || gui.labeledMarkService == null) return;
            
            Gob player = NUtils.player();
            if(player == null) return;
            
            // Get segment ID and tile coordinates
            long segmentId = gui.mmap.sessloc.seg.id;
            Coord tileCoords = player.rc.floor(MCache.tilesz).add(gui.mmap.sessloc.tc);
            
            // Create label (e.g., "q20")
            String label = String.format("q%.0f", quality);
            
            // Get water icon from game resources
            BufferedImage iconImage = getWaterIcon(waterType);
            
            // Add mark via service (handles persistence)
            gui.labeledMarkService.addLabeledMark(label, waterType, segmentId, tileCoords, iconImage);
        } catch(Exception e) {
            // Silently ignore errors
        }
    }
}
