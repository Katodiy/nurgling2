package nurgling.tasks;

import haven.*;
import nurgling.*;

public class DrinkToLvl extends NTask
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
        String lastError = NUtils.getUI().getLastError();
        if (lastError!=null && lastError.equals("You have nothing on your hotbelt to drink."))
        {
            no_water = true;
            return true;
        }
        return NUtils.getStamina() >= lvl;
    }

    public boolean isNoWater()
    {
        return no_water;
    }
}