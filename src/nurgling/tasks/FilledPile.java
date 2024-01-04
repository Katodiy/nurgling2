package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

public class FilledPile implements NTask
{

    final Gob gob;
    final NAlias items;
    public FilledPile(Gob gob, NAlias items)
    {
        this.gob = gob;
        this.items = items;
    }

    @Override
    public boolean check()
    {
        try {
            return gob.ngob.getModelAttribute() == 31 || NUtils.getGameUI().getInventory().getItems(items).isEmpty();
        }
        catch (InterruptedException ignore)
        {
        }
        return false;
    }
}
