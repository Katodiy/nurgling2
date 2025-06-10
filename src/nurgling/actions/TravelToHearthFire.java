package nurgling.actions;

import haven.MenuGrid;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitPlayerNotNull;
import nurgling.tasks.WaitPlayerPose;

public class TravelToHearthFire implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
        {
            if(pag.button()!=null && pag.button().name().equals("Travel to your Hearth Fire"))
            {
                pag.button().use(new MenuGrid.Interaction(1, 0));
                break;
            }
        }

        NUtils.getUI().core.addTask(new WaitPlayerPose("gfx/borka/point"));
        NUtils.getUI().core.addTask(new WaitPlayerPose("gfx/borka/body"));

        NUtils.getUI().core.addTask(new WaitPlayerNotNull());

        return Results.SUCCESS();
    }
}
