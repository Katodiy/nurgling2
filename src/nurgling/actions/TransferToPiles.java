package nurgling.actions;

import haven.*;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.tools.StackSupporter;

import java.util.ArrayList;
import java.util.Comparator;

public class TransferToPiles implements Action{

    NAlias items;

    Pair<Coord2d,Coord2d> out;

    int th = 0;

    // When set, use exact name matching instead of NAlias substring matching
    String exactName = null;

    public TransferToPiles(Pair<Coord2d,Coord2d> out, NAlias items) {
        this.out = out;
        this.items = items;
    }

    public TransferToPiles(Pair<Coord2d,Coord2d> out, NAlias items, int th) {
        this.out = out;
        this.items = items;
        this.th = th;
    }

    public TransferToPiles(Pair<Coord2d,Coord2d> out, String exactName, int th) {
        this.out = out;
        this.exactName = exactName;
        this.items = new NAlias(exactName);
        this.th = th;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> witems;
        NAlias pileName;
        if (!(witems = getMatchingItems(gui)).isEmpty() ) {
                Gob target = null;
                for (Gob gob : Finder.findGobs(out, pileName = getStockpileName(items))) {
                    while (gob.ngob.getModelAttribute() != 31) {
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
                            witems = getMatchingItems(gui);
                            int size = witems.size();
                            new OpenTargetContainer("Stockpile", target).run(gui);
                            int target_size = Math.min(size,gui.getStockpile().getFreeSpace());
                            if(target_size>0) {
                                transfer(gui, target_size);
                            }
                            if((witems = getMatchingItems(gui)).isEmpty())
                            {
                                new CloseTargetContainer("Stockpile").run(gui);
                                return Results.SUCCESS();
                            }
                        }
                    }
                }

                while(!getMatchingItems(gui).isEmpty() && out!=null) {
                    PileMaker pm;
                    if (exactName != null) {
                        pm = new PileMaker(out, exactName, pileName, th);
                    } else {
                        pm = new PileMaker(out, items, pileName, th);
                    }
                    if(!pm.run(gui).IsSuccess())
                        return Results.FAIL();
                    Gob pile = pm.getPile();
                    while (pile.ngob.getModelAttribute() != 31) {
                        witems = getMatchingItems(gui);
                        int size = witems.size();
                        new OpenTargetContainer("Stockpile", pile).run(gui);
                        int target_size = Math.min(size, gui.getStockpile().getFreeSpace());
                        if (target_size > 0) {
                            transfer(gui, target_size);
                        }
                        else
                        {
                            break;
                        }
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
                    witems = getMatchingItems(gui);
                    witems.sort(new Comparator<WItem>() {
                        @Override
                        public int compare(WItem o1, WItem o2) {
                            Float q1 = ((NGItem)o1.item).quality;
                            Float q2 = ((NGItem)o2.item).quality;
                            if(q1 == null || q2 == null)
                                return 0;
                            return Float.compare(q1,q2);
                        }
                    });
                    if (witems.isEmpty()) {
                        break;
                    }
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
        if (NParser.checkName(items.getDefault(), new NAlias("Soil"))) {
            return new NAlias("gfx/terobjs/stockpile-soil");
        } else if (NParser.checkName(items.getDefault(), new NAlias("board"))) {
            return new NAlias("gfx/terobjs/stockpile-board");
        } else if (NParser.checkName(items.getDefault(), new NAlias("Pumpkin Flesh"))) {
            return new NAlias("gfx/terobjs/stockpile-trash");
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
        } else if (NParser.checkName(items.getDefault(), new NAlias("bone"))) {
            return new NAlias("gfx/terobjs/stockpile-bone");
        } else
            return new NAlias("stockpile");
    }

    /**
     * Gets items from inventory, using exact name match if exactName is set,
     * otherwise uses NAlias substring matching.
     */
    private ArrayList<WItem> getMatchingItems(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> allItems = gui.getInventory().getItems(items, th);
        if (exactName == null) {
            return allItems;
        }
        ArrayList<WItem> exactMatches = new ArrayList<>();
        for (WItem witem : allItems) {
            if (((NGItem) witem.item).name().equals(exactName)) {
                exactMatches.add(witem);
            }
        }
        return exactMatches;
    }
}
