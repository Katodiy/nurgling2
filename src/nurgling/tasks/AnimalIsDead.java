package nurgling.tasks;

import haven.Gob;
import haven.MCache;
import haven.res.ui.croster.CattleId;
import nurgling.NUtils;
import nurgling.tools.NParser;

public class AnimalIsDead extends NTask
{
    Gob animal;
    public AnimalIsDead(Gob animal)
    {
        this.animal = animal;
        this.infinite = true;
    }

    boolean res = false;
    @Override
    public boolean check()
    {
        if(animal.pose()!=null && NParser.checkName(animal.pose(), "knock")) {
            res = true;
            return true;
        }
        return (NUtils.getGameUI().prog==null || NUtils.getGameUI().prog.prog < 0) && animal.rc.dist(NUtils.player().rc) > MCache.tilesz.len() * 2;
    }

    public boolean getRes() {
        return res;
    }
}
