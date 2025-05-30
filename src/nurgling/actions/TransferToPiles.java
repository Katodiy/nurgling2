package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.tools.StackSupporter;

import java.util.ArrayList;

public class TransferToPiles implements Action{

    NAlias items;

    Pair<Coord2d,Coord2d> out;

    int th = 0;
    Coord2d playerc;

    public TransferToPiles(Pair<Coord2d,Coord2d> out, NAlias items) {
        this.out = out;
        this.items = items;
    }

    public TransferToPiles(Pair<Coord2d,Coord2d> out, NAlias items, Coord2d playerc) {
        this.out = out;
        this.items = items;
        this.playerc = playerc;
    }

    public TransferToPiles(Pair<Coord2d,Coord2d> out, NAlias items, int th) {
        this.out = out;
        this.items = items;
        this.th = th;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> witems;
        NAlias pileName;
        if (!(witems = gui.getInventory().getItems(items,th)).isEmpty() ) {
                Gob target = null;
                for (Gob gob : Finder.findGobs(out, pileName = getStockpileName(items))) {
                    if (gob.ngob.getModelAttribute() != 31) {
                        if(PathFinder.isAvailable(gob)) {
                            target = gob;
                            PathFinder pf = new PathFinder(target);
                            pf.isHardMode = true;
                            pf.run(gui);
                            if(NUtils.getGameUI().vhand!=null) {
                                NUtils.activateItem(target, false);
                                NUtils.addTask(new NTask() {
                                    @Override
                                    public boolean check() {
                                        return NUtils.getGameUI().vhand == null;
                                    }
                                });
                            }
                            witems = gui.getInventory().getItems(items,th);
                            int size = witems.size();
                            new OpenTargetContainer("Stockpile", target).run(gui);
                            int target_size = Math.min(size,gui.getStockpile().getFreeSpace());
                            if(target_size>0) {
                                transfer(gui, target_size);
                            }
                            if((witems = gui.getInventory().getItems(items,th)).isEmpty())
                            {
                                new CloseTargetContainer("Stockpile").run(gui);
                                return Results.SUCCESS();
                            }
                        }
                    }
                }

                while(!(gui.getInventory().getItems(items,th)).isEmpty()) {
                    PileMaker pm;

                    if(!(pm = new PileMaker(out, items, pileName, playerc)).run(gui).IsSuccess())
                        return Results.FAIL();
                    Gob pile = pm.getPile();
                    witems = gui.getInventory().getItems(items,th);
                    int size = witems.size();
                    new OpenTargetContainer("Stockpile", pile).run(gui);
                    int target_size = Math.min(size, gui.getStockpile().getFreeSpace());
                    if(target_size>0) {
                        transfer(gui, target_size);
                    }
                }
            }
        return Results.SUCCESS();
        }

    private void transfer(NGameUI gui, int target_size) throws InterruptedException {
        ArrayList<WItem> witems;
        NUtils.addTask(new WaitStockpile(true));
        int fullSize = gui.getInventory().getItems().size();
        if(th>1 || StackSupporter.isSameExist(items, gui.getInventory())) {
            for (int i = 0; i < target_size; i++) {
                {
                    witems = gui.getInventory().getItems(items, th);
                    NUtils.takeItemToHand(witems.get(0));
                    gui.getStockpile().wdgmsg("drop");
                    NUtils.addTask(new WaitFreeHand());
                }
            }
        }
        else
        {
            gui.getStockpile().put(target_size);
        }

        NUtils.getUI().core.addTask(new WaitTargetSize(NUtils.getGameUI().getInventory(), fullSize - target_size));
    }


    NAlias getStockpileName(NAlias items) {
        if (NParser.checkName(items.getDefault(), new NAlias("soil"))) {
            return new NAlias("gfx/terobjs/stockpile-soil");
        } else if (NParser.checkName(items.getDefault(), new NAlias("board"))) {
            return new NAlias("gfx/terobjs/stockpile-board");
        } else if (NParser.checkName(items.getDefault(), new NAlias("pumpkin"))) {
            return new NAlias("gfx/terobjs/stockpile-pumpkin");
        } else if (NParser.checkName(items.getDefault(), new NAlias("metal"))) {
            return new NAlias("gfx/terobjs/stockpile-metal");
        } else if (NParser.checkName(items.getDefault(), new NAlias("brick"))) {
            return new NAlias("gfx/terobjs/stockpile-brick");
        } else if (NParser.checkName(items.getDefault(), new NAlias("fresh leaf of pipeweed"))) {
            return new NAlias("gfx/terobjs/stockpile-pipeleaves");
        } else if (NParser.checkName(items.getDefault(), new NAlias("Hemp Cloth"))) {
            return new NAlias("gfx/terobjs/stockpile-cloth");
        } else if (NParser.checkName(items.getDefault(), new NAlias("Linen Cloth"))) {
            return new NAlias("gfx/terobjs/stockpile-cloth");
        } else if (NParser.checkName(items.getDefault(), new NAlias("coal"))) {
            return new NAlias("gfx/terobjs/stockpile-coal");
        } else if (NParser.checkName(items.getDefault(), new NAlias("onion"))) {
            return new NAlias("gfx/terobjs/stockpile-onion");
        } else
            return new NAlias("stockpile");
    }
}
