package nurgling.actions.bots;

import haven.Coord;
import haven.Fightview;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.FreeInventory2;
import nurgling.actions.Results;
import nurgling.areas.NContext;
import nurgling.tasks.GetCurs;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Arrays;

import static haven.OCache.posres;

public class FreeInvBot implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        new FreeInventory2(new NContext(gui)).run(gui);
        return Results.SUCCESS();
    }
}
