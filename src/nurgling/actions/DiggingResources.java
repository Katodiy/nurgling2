package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Arrays;


public class DiggingResources implements Action
{
    NAlias items;
    Pair<Coord2d,Coord2d> res;
    Pair<Coord2d,Coord2d> out;
    String shovel_tools;

    public DiggingResources(Pair<Coord2d,Coord2d> res, Pair<Coord2d,Coord2d> out, NAlias items, String shovel_tools)
    {
        this.res = res;
        this.out = out;
        this.items = items;
        this.shovel_tools = shovel_tools;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        new Equip(new NAlias(shovel_tools)).run(gui);

        ArrayList<Coord2d> tiles = null;
        if (NParser.checkName("clay", items)) {
            tiles = Finder.findTilesInArea(new NAlias(new ArrayList<String>(Arrays.asList("water",
                    "dirt")), new ArrayList<String>()), res);
        } else if (NParser.checkName("sand", items)) {
            tiles = Finder.findTilesInArea(new NAlias(new ArrayList<String>(Arrays.asList("beach")), new ArrayList<String>()), res);
        }

        for (Coord2d tile : tiles) {
            Coord2d pos = new Coord2d(tile.x + 5.5, tile.y + 5.5);
            if(NUtils.getGameUI().getInventory().getFreeSpace()==0)
                new TransferToPiles(out, items).run(gui);
            WaitDiggerState wds;
            do {
                new PathFinder(pos).run(gui);
                NUtils.dig();
                NUtils.addTask(new WaitPoseOrMsg(NUtils.player(), "gfx/borka/shoveldig", "no clay left"));
                wds = new WaitDiggerState("no clay left");
                NUtils.addTask(wds);
                if (wds.getState() == WaitDiggerState.State.NOFREESPACE) {
                    new TransferToPiles(out, items).run(gui);
                }
                else if(wds.getState() == WaitDiggerState.State.TIMEFORDRINK) {
                    if(!(new Drink(0.9,false).run(gui)).IsSuccess())
                        return Results.ERROR("Drink is not found");
                }
                else if(wds.getState()== WaitDiggerState.State.DANGER)
                    return Results.ERROR("no energy");
            } while (wds.getState() != WaitDiggerState.State.MSG);
        }
        new TransferToPiles(out, items).run(gui);


        return Results.SUCCESS();
    }
}
