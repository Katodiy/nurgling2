package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.*;
import nurgling.tools.*;

import java.util.*;

/*
* find all chest in area TestArea (req)
* */

public class TESTAvalaible extends Test
{

    public TESTAvalaible()
    {
        this.num = 100;
    }

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        NArea area = NUtils.findArea("TestArea");

        if(area!=null)
        {
            ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias("stockpile"));
            NUtils.getGameUI().tickmsg("Total piles:" + gobs.size());
            int count = 0;
            for(Gob gob : gobs)
            {
                if(PathFinder.isAvailable(gob, true))
                {
                    count++;
                    PathFinder pf = new PathFinder(gob);
                    pf.isHardMode = true;
                    pf.run(gui);
                    new OpenTargetContainer("Stockpile",gob).run(gui);
                }
            }
            NUtils.getGameUI().tickmsg("Available piles:" + count);
        }
    }
}
