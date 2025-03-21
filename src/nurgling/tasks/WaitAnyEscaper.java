package nurgling.tasks;

import haven.Fightview;
import nurgling.NUtils;

import java.util.ArrayList;

public class WaitAnyEscaper extends NTask
{
    public WaitAnyEscaper(ArrayList<Long> ids)
    {
        this.ids = ids;
    }

    ArrayList<Long> ids = new ArrayList<>();
    long res = -1;

    @Override
    public boolean check() {
        for (Long id : ids) {
            boolean isFound = false;
            for (Fightview.Relation rel : NUtils.getGameUI().fv.lsrel) {
                if(rel.gst == 0)
                    return true;
                if (rel.gobid == id) {
                    isFound = true;
                    break;
                }

            }
            if (!isFound) {
                res = id;
                return true;
            }
        }
        return false;
    }

    public long getEscaper() {
        return res;
    }
}