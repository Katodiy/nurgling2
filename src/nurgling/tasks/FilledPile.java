package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

public class FilledPile implements NTask
{

    final Gob gob;
    final NAlias items;
    final int size;
    final int oldSize;

    public FilledPile(Gob gob, NAlias items, int size, int oldSize)
    {
        this.gob = gob;
        this.items = items;
        this.size = size;
        this.oldSize = oldSize;
    }

    @Override
    public boolean check()
    {
        try {
            return NUtils.getGameUI().getInventory().getItems(items).size() == oldSize - size;
        }
        catch (InterruptedException ignore)
        {
        }
        return false;
    }
}
