package nurgling.tasks;

import haven.Widget;
import haven.Window;
import haven.res.ui.relcnt.RelCont;
import nurgling.NUtils;
import nurgling.tools.Finder;

public class NoGob implements NTask
{

    final long id;
    public NoGob(long id)
    {
        this.id = id;
    }


    @Override
    public boolean check()
    {
        return Finder.findGob(id) == null;
    }
}
