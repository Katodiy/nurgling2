package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitItemInInventory;
import nurgling.tasks.WaitItems;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;

import static haven.OCache.posres;

public class HFout implements Action {
    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {


        NUtils.hfout();
        return Results.SUCCESS();
    }
}