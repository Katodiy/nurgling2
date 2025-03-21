package nurgling.tasks;

import haven.GItem;
import haven.Gob;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.NUtils;

public class AnimalInRoster extends NTask
{
    Gob animal;
    Class<? extends Entry> cattleRoster;
    RosterWindow rw;
    public AnimalInRoster(Gob animal, Class<? extends Entry> cattleRoster, RosterWindow rw)
    {
        this.animal = animal;
        this.cattleRoster = cattleRoster;
        this.rw = rw;

    }

    @Override
    public boolean check()
    {
        if(animal.getattr(CattleId.class)==null)
            return false;
        if(rw.roster(cattleRoster).entries.get(animal.getattr(CattleId.class).id) == null)
            return false;
        return rw.roster(cattleRoster).entries.get(animal.getattr(CattleId.class).id).getClass() == cattleRoster;
    }

}
