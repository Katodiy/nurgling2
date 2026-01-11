package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.overlays.BuildGhostPreview;
import nurgling.overlays.NCustomBauble;
import nurgling.tasks.WaitPlob;

import java.awt.image.BufferedImage;

public class SelectAreaWithLiveGhosts extends SelectArea {
    private String buildingName;
    private int rotationCount = 0;
    private NHitBox customHitBox = null;
    public NArea ghostArea;
    NContext context;
    public SelectAreaWithLiveGhosts(NContext context, BufferedImage image, String buildingName) {
        super(image);
        this.buildingName = buildingName;
        this.context = context;
    }
    
    public SelectAreaWithLiveGhosts(NContext context, BufferedImage image, String buildingName, NHitBox customHitBox) {
        super(image);
        this.buildingName = buildingName;
        this.customHitBox = customHitBox;
        this.context = context;
    }
    
    public int getRotationCount() {
        return rotationCount;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        NMapView mapView = (NMapView) NUtils.getGameUI().map;

        // Check if selection is already in progress (another bot is running)
        if (mapView.isAreaSelectionMode.get())
        {
            return Results.ERROR("Area selection already in progress");
        }

        Gob player = NUtils.player();

        // Reset selection state
        mapView.areaSpace = null;
        mapView.currentSelectionCoords = null;
        mapView.rotationRequested = false;
        mapView.isAreaSelectionMode.set(true);

        if (image != null && player != null)
        {
            player.addcustomol(new NCustomBauble(player, image, spr, ((NMapView) NUtils.getGameUI().map).isAreaSelectionMode));
        }

        // Activate build menu to get hitbox
        for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
        {
            if (pag.button() != null && pag.button().name().equals(buildingName))
            {
                pag.button().use(new MenuGrid.Interaction(1, 0));
                break;
            }
        }

        if (NUtils.getGameUI().map.placing == null)
        {
            for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
            {
                if (pag.button() != null && pag.button().name().equals(buildingName))
                {
                    pag.button().use(new MenuGrid.Interaction(1, 0));
                    break;
                }
            }
        }

        NUtils.addTask(new WaitPlob());
        MapView.Plob plob = NUtils.getGameUI().map.placing.get();

        // Clean up any existing ghost preview from previous run
        if (player != null)
        {
            BuildGhostPreview oldGhost = player.getattr(BuildGhostPreview.class);
            if (oldGhost != null)
            {
                oldGhost.dispose();
                player.delattr(BuildGhostPreview.class);
            }
        }

        // Get hitbox, resource, and sprite data from plob
        NHitBox hitBox = plob.ngob.hitBox;
        // Use custom hitbox if plob doesn't have one (e.g., for moundbed)
        if (hitBox == null && customHitBox != null) {
            hitBox = customHitBox;
        }
        
        Indir<Resource> resource = null;
        Message sdt = Message.nil;

        // Try to get resource from plob's ResDrawable first
        ResDrawable rd = plob.getattr(ResDrawable.class);
        if (rd != null && rd.res != null)
        {
            resource = rd.res;
            if (rd.sdt != null) {
                sdt = rd.sdt.clone();
            }
        }
        // Fallback to name-based loading
        else if (plob.ngob.name != null)
        {
            resource = Resource.remote().load(plob.ngob.name);
        }

        // Properly cancel placement cursor BEFORE starting selection
        try
        {
            if (NUtils.getGameUI().map.placing != null)
            {
                plob.delattr(ResDrawable.class);
                NUtils.getGameUI().map.placing.cancel();
                NUtils.getGameUI().map.placing = null;
            }
        } catch (Exception e)
        {
            // Ignore if placing was already cancelled
        }

        // Start the selection task with live ghost preview
        nurgling.tasks.SelectAreaWithLiveGhosts sa;
        NUtils.getUI().core.addTask(sa = new nurgling.tasks.SelectAreaWithLiveGhosts(hitBox, resource, sdt));

        if (sa.getResult() != null)
        {
            rotationCount = sa.getRotationCount();
            String insaId = context.createAreaWithGhost(sa);
            ghostArea = context.getAreaById(insaId);
        } else
        {
            return Results.FAIL();
        }


        return Results.SUCCESS();
    }
}
