package nurgling.tasks;

import haven.Fightview;
import nurgling.NUtils;

public class WaitRelationState extends NTask
{
    public WaitRelationState(long id, int state)
    {
        this.id = id;
        this.state = state;
    }


    long id;
    int state;


    @Override
    public boolean check()
    {
        boolean notFound = true;
        for(Fightview.Relation rel : NUtils.getGameUI().fv.lsrel)
        {
            if(rel.gobid==id) {
                notFound = false;
                return rel.gst == state;
            }
        }
        if(notFound)
            return true;
        return false;
    }
}