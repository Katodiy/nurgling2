package nurgling.tasks;

import haven.*;
import nurgling.*;

public class DrinkToLvl implements NTask
{
    public DrinkToLvl(double lvl)
    {
        this.lvl = lvl;
    }

    double lvl;

    boolean no_water = false;

    @Override
    public boolean check()
    {
        if (NUtils.getGameUI().getLastError()!=null && NUtils.getGameUI().getLastError().equals("You have nothing on your hotbelt to drink."))
        {
            no_water = true;
            NUtils.getGameUI().dropLastError();
            return true;
        }
        return NUtils.getStamina() >= lvl;
    }

    public boolean isNoWater()
    {
        return no_water;
    }
}