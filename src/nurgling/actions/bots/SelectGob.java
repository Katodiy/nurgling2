package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.overlays.NCustomBauble;

import java.awt.image.BufferedImage;

public class SelectGob implements Action {

    public SelectGob() {

    }

    public SelectGob(BufferedImage image) {
        this.image = image;
    }
    BufferedImage image = null;
    Gob result;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {


        if (!((NMapView) NUtils.getGameUI().map).isGobSelectionMode.get()) {
            Gob player = NUtils.player();
            ((NMapView) NUtils.getGameUI().map).isGobSelectionMode.set(true);
            if(image!=null && player!=null)
            {
                player.addcustomol(new NCustomBauble(player,image,((NMapView) NUtils.getGameUI().map).isGobSelectionMode));
            }
            nurgling.tasks.SelectGob sa;
            NUtils.getUI().core.addTask(sa = new nurgling.tasks.SelectGob());
            if (sa.getResult() != null) {
                result = sa.getResult();
            }
        }
        else
        {
            return Results.FAIL();
        }
        return Results.SUCCESS();
    }


    public Gob getResult() {
        return result;
    }
}
