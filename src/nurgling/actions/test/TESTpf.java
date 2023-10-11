package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.pf.*;

/*
* You need one branch in your inventory
* */

public class TESTpf extends Test
{

    public TESTpf()
    {
        this.name = "cart";
        num = 1;
    }

    String name;

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        new PathFinder(gui.map.player().rc,TestUtils.findGob(name).rc).run(gui);
    }
}
