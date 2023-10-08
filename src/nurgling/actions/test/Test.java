package nurgling.actions.test;

import nurgling.*;
import nurgling.actions.*;

import java.util.*;

public abstract class Test implements Action
{
    protected int num = 10000;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        long start = System.currentTimeMillis();

        int count = 0;
        int total = 0;
        int min = 0;
        for (int i = 0; i < num; i++)
        {
            body(gui);
            count++;
            if (total + count % 200 == 0)
                gui.tickmsg("Total passed:" + String.valueOf(total + count));
            if (System.currentTimeMillis() - start >= 60000)
            {
                start = System.currentTimeMillis();
                gui.tickmsg(String.valueOf(count) + " per min");
                total += count;
                min++;
                gui.tickmsg(String.valueOf(total / min) + " test/min");
                count = 0;
            }
        }
        return Results.SUCCESS();
    }

    public abstract void body(NGameUI gui) throws InterruptedException;
}
