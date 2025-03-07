package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

public class GetGobName extends NTask
{

    Gob gob;

    public GetGobName(Gob gob)
    {
        this.gob = gob;
    }

    @Override
    public boolean check()
    {
        return gob.ngob.name!=null || gob.updateseq!=0;
    }
}
