package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.Equip;
import nurgling.actions.Results;
import nurgling.overlays.NCheckResult;
import nurgling.tasks.GetCurs;
import nurgling.tasks.WaitItemsOrError;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.bots.UsingTools;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static haven.OCache.posres;

public class CheckClay implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NAlias shovel_tools = new NAlias ();
        for (UsingTools.Tool shovel : UsingTools.Tools.shovels)
        {
            shovel_tools.keys.add(shovel.name);
        }
        shovel_tools.buildCaches(); // Rebuild caches after modifying keys

        new Equip(shovel_tools).run(gui);
        ArrayList<WItem> oldItems = ((NInventory) NUtils.getGameUI().maininv).getItems();
        WaitItemsOrError waitItemsOrError;
        NUtils.dig();
        NUtils.addTask(waitItemsOrError = new WaitItemsOrError((NInventory) NUtils.getGameUI().maininv,new NAlias("clay", "Clay", "Soil", "Moss", "Ash", "Sand"),1,"no clay left", oldItems));

        if(!waitItemsOrError.getResult().isEmpty()) {

            WItem item = waitItemsOrError.getResult().get(0);

            if (item != null) {
                String itemName = ((NGItem) item.item).name();
                double quality = ((NGItem) item.item).quality;
                BufferedImage itemImg = ((StaticGSprite) item.lspr).img.img;
                
                NUtils.getGameUI().msg(itemName + " " + quality);
                NUtils.player().addcustomol(new NCheckResult(NUtils.player(), quality, itemName, itemImg));
                
                // Add labeled mark to minimap with quality label (persisted to file)
                addLabeledMinimapMark(gui, quality, itemName, itemImg);
                
                NUtils.drop(item);
            }
            if (!NParser.checkName(NUtils.getCursorName(), "arw")) {
                NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres), 3, 0);
                NUtils.getUI().core.addTask(new GetCurs("arw"));
            }
            gui.map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres), 1, 0);
        }


        return Results.SUCCESS();
    }
    
    /**
     * Add a labeled mark to the minimap showing soil/clay quality.
     * Uses LabeledMarkService for persistence between sessions.
     */
    private void addLabeledMinimapMark(NGameUI gui, double quality, String itemName, BufferedImage itemImg) {
        try {
            if(gui.mmap == null || gui.mmap.sessloc == null || gui.labeledMarkService == null) return;
            
            Gob player = NUtils.player();
            if(player == null) return;
            
            // Get segment ID and tile coordinates
            long segmentId = gui.mmap.sessloc.seg.id;
            Coord tileCoords = player.rc.floor(MCache.tilesz).add(gui.mmap.sessloc.tc);
            
            // Create label (e.g., "q20")
            String label = String.format("q%.0f", quality);
            
            // Add mark via service (handles persistence)
            gui.labeledMarkService.addLabeledMark(label, itemName, segmentId, tileCoords, itemImg);
        } catch(Exception e) {
            // Silently ignore errors
        }
    }
}
