package nurgling.tasks;

import haven.Gob;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.NUtils;

public class GetAnimalEntry extends NTask
{
    Gob gob;
    Class<? extends Entry> cattleRoster;

    public GetAnimalEntry(Gob gob, Class<? extends Entry> cattleRoster)
    {
        this.gob = gob;
        this.cattleRoster = cattleRoster;
        this.infinite = false;
        this.maxCounter = 10;
    }

    @Override
    public boolean check()
    {
        if(((RosterWindow) NUtils.getGameUI().getWindow("Cattle Roster"))==null)
            return false;
        if(((RosterWindow) NUtils.getGameUI().getWindow("Cattle Roster")).roster(cattleRoster)==null)
            return false;
        if(((RosterWindow) NUtils.getGameUI().getWindow("Cattle Roster")).roster(cattleRoster).entries==null)
            return false;
        if( gob.getattr(CattleId.class)==null)
            return false;
        result = (Entry) ((RosterWindow) NUtils.getGameUI().getWindow("Cattle Roster")).roster(cattleRoster).entries.get(((CattleId) gob.getattr(CattleId.class)).id);
        return result!=null;
    }

    private Entry result = null;

    public Entry getEntry(){
        return result;
    }
}
