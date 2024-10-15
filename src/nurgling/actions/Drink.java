package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;
import nurgling.tools.NParser;

import static haven.OCache.posres;

public class Drink implements Action
{
    public Drink(double lvl, boolean withStop)
    {
        this.lvl = lvl;
        this.withStop = withStop;
    }
    double lvl;
    boolean withStop;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(NUtils.getStamina()<lvl)
        {
            Gob player = NUtils.player();
            if(withStop && player!=null) {
                if(!NParser.checkName(NUtils.getCursorName(), "arw")) {
                    NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres),3, 0);
                    NUtils.getUI().core.addTask(new GetCurs("arw"));
                }
                NUtils.lclick(player.rc);
                NUtils.addTask(new WaitPose(player, "gfx/borka/idle"));
            }
            NUtils.getUI().dropLastError();
            for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
            {
                if(pag.button()!=null && pag.button().name().equals("Drink"))
                {
                    while (NUtils.getStamina()<lvl)
                    {
                        pag.button().use(new MenuGrid.Interaction(1, 0));
                        DrinkToLvl dtlvl = new DrinkToLvl(lvl);
                        NUtils.getUI().core.addTask(dtlvl);
                        if (dtlvl.isNoWater())
                            return Results.ERROR("NO WATER");
                    }
                    break;
                }
            }
        }
        return Results.SUCCESS();
    }
}
