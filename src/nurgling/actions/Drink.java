package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

public class Drink implements Action
{
    public Drink(double lvl)
    {
        this.lvl = lvl;
    }
    double lvl;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(NUtils.getStamina()<lvl)
        {
            NUtils.getGameUI().dropLastError();
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
