package nurgling.tasks;

import haven.Fightview;
import haven.Gob;
import nurgling.NUtils;

import java.util.LinkedList;

public class WaitBattleWindow extends NTask
{
    public WaitBattleWindow(long id, boolean noWait)
    {
        this.id = id;
    }

    public WaitBattleWindow()
    {
        this.id = -1;
    }

    long id;

    @Override
    public boolean check()
    {
        if(id==-1)
        {
            return !NUtils.getGameUI().fv.lsrel.isEmpty();
        }
        else
        {
            for(Fightview.Relation rel : NUtils.getGameUI().fv.lsrel)
            {
                if(rel.gobid == id)
                    return true;
            }
        }
        return false;
    }
}