package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.Equip;
import nurgling.actions.Results;
import nurgling.actions.SelectFlowerAction;
import nurgling.overlays.NCheckResult;
import nurgling.tasks.GetCurs;
import nurgling.tasks.WaitItemContent;
import nurgling.tasks.WaitItems;
import nurgling.tasks.WaitItemsOrError;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.bots.UsingTools;

import java.util.ArrayList;
import java.util.Arrays;

import static haven.OCache.posres;

public class CheckClay implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NAlias shovel_tools = new NAlias ();
        for (UsingTools.Tool shovel : UsingTools.Tools.shovels)
        {
            shovel_tools.keys.add(shovel.name);
        }

        new Equip(shovel_tools).run(gui);
        WaitItemsOrError waitItemsOrError;
        NUtils.dig();
        NUtils.addTask(waitItemsOrError = new WaitItemsOrError((NInventory) NUtils.getGameUI().maininv,new NAlias("clay", "Clay", "Soil", "Moss", "Ash", "Sand"),1,"no clay left", ((NInventory) NUtils.getGameUI().maininv).getItems()));

        if(!waitItemsOrError.getResult().isEmpty()) {

            WItem item = waitItemsOrError.getResult().get(0);

            if (item != null) {
                NUtils.getGameUI().msg(((NGItem) item.item).name() + " " + ((NGItem) item.item).quality);
                NUtils.player().addcustomol(new NCheckResult(NUtils.player(), ((NGItem) item.item).quality, ((NGItem) item.item).name(), ((StaticGSprite) item.lspr).img.img));
                NUtils.drop(item);
            }
            if (!NParser.checkName(NUtils.getCursorName(), "arw")) {
                NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres), 3, 0);
                NUtils.getUI().core.addTask(new GetCurs("arw"));
            }
            gui.map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres), 1, 0);
        }


        return Results.SUCCESS();
    }
}
