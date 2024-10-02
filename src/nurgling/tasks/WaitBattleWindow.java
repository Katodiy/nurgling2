package nurgling.tasks;

import haven.Fightview;
import haven.Gob;
import nurgling.NUtils;

import java.util.LinkedList;

public class WaitBattleWindow implements NTask
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

    int counter = 0;
    @Override
    public boolean check()
    {
        counter++;
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
        if(counter>=30)
        {
           return true;
        }
        return false;
    }
}