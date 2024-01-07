package nurgling.actions;

import haven.Gob;
import haven.MenuGrid;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.DrinkToLvl;
import nurgling.tasks.GetCurs;
import nurgling.tasks.WaitPos;
import nurgling.tasks.WaitPose;

public class Destroy implements Action
{
    public Destroy(Gob gob, String pose)

    {
        this.gob = gob;
        this.pose = pose;
    }

    Gob gob;
    String pose;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
        {
            if(pag.button()!=null && pag.button().name().equals("Destroy"))
            {
                pag.button().use(new MenuGrid.Interaction(1, 0));
                break;
            }
        }
        NUtils.getUI().core.addTask(new GetCurs("mine"));
        NUtils.clickGob(gob);
        NUtils.getUI().core.addTask(new WaitPose(NUtils.player(),pose));
        NUtils.rclickGob(gob);
        NUtils.getUI().core.addTask(new GetCurs("arw"));
        return Results.SUCCESS();
    }
}
