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

public class TESTfindallchest extends Test
{

    public TESTfindallchest()
    {
        this.num = 1;
    }

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        NArea area = NUtils.findArea("TestArea");

        if(area!=null)
        {
            ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias("chest"));
            NUtils.getGameUI().tickmsg("Total chest:" + gobs.size());
            for(Gob gob : gobs)
            {
                new PathFinder(gob).run(gui);
                new OpenTargetContainer("Chest",gob).run(gui);
            }
        }
    }
}
