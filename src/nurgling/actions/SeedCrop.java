package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.GetCurs;
import nurgling.tasks.WaitAnotherAmount;
import nurgling.tasks.WaitGobsInField;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class SeedCrop implements Action {

    final NArea field;
    final NArea seed;


    final NAlias crop;
    final NAlias iseed;

    final boolean allowedToPlantFromStockpiles;


    public SeedCrop(NArea field, NArea seed, NAlias crop, NAlias iseed, boolean allowedToPlantFromStockpiles) {
        this.field = field;
        this.seed = seed;
        this.crop = crop;
        this.iseed = iseed;
        this.allowedToPlantFromStockpiles = allowedToPlantFromStockpiles;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        ArrayList<Gob> barrels = Finder.findGobs(seed, new NAlias("barrel"));
        ArrayList<Gob> stockPiles = Finder.findGobs(seed, new NAlias("stockpile"));

        if (barrels.isEmpty() && !allowedToPlantFromStockpiles)
            return Results.ERROR("No barrel for seed");

        if (stockPiles.isEmpty() && allowedToPlantFromStockpiles)
            return Results.ERROR("No stockpiles for seed");


        ArrayList<Coord2d> tiles = field.getTiles(new NAlias("field"));

        Coord start = gui.map.player().rc.dist(field.getArea().br.mul(MCache.tilesz)) < gui.map.player().rc.dist(field.getArea().ul.mul(MCache.tilesz)) ? field.getArea().br.sub(1, 1) : field.getArea().ul;
        Coord pos = new Coord(start);
        boolean rev = (pos.equals(field.getArea().ul));

        boolean revdir = rev;

        do {
            if (!rev) {
                while (pos.x >= field.getArea().ul.x) {
                    AtomicBoolean setDir = new AtomicBoolean(true);
                    if (revdir) {
                        while (pos.y <= field.getArea().br.y - 1) {
                            Coord endPos = new Coord(Math.max(pos.x - 1, field.getArea().ul.x), Math.min(pos.y + 1, field.getArea().br.y - 1));
                            Area harea = new Area(pos, endPos, true);
                            Coord2d endp = harea.ul.sub(0, 1).mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y + MCache.tileqsz.y);
                            seedCrop(gui, barrels, stockPiles, harea, revdir, endp, setDir);
                            pos.y += 2;

                        }
                        pos.y = field.getArea().br.y - 1;
                    } else {
                        while (pos.y >= field.getArea().ul.y) {
                            Coord endPos = new Coord(Math.max(pos.x - 1, field.getArea().ul.x), Math.max(pos.y - 1, field.getArea().ul.y));
                            Area harea = new Area(pos, endPos, true);
                            Coord2d endp = harea.br.mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y).add(0, MCache.tileqsz.y);
                            seedCrop(gui, barrels, stockPiles, harea, revdir, endp, setDir);
                            pos.y -= 2;
                        }
                        pos.y = field.getArea().ul.y;
                    }
                    revdir = !revdir;
                    pos.x -= 2;
                }
            } else {
                while (pos.x <= field.getArea().br.x - 1) {
                    AtomicBoolean setDir = new AtomicBoolean(true);
                    if (revdir) {
                        while (pos.y <= field.getArea().br.y - 1) {
                            Coord endPos = new Coord(Math.min(pos.x + 1, field.getArea().br.x - 1), Math.min(pos.y + 1, field.getArea().br.y - 1));
                            Area harea = new Area(pos, endPos, true);
                            Coord2d endp = harea.ul.sub(0, 1).mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y + MCache.tileqsz.y);
                            seedCrop(gui, barrels, stockPiles, harea, revdir, endp, setDir);
                            pos.y += 2;

                        }
                        pos.y = field.getArea().br.y - 1;
                    } else {
                        while (pos.y >= field.getArea().ul.y) {
                            Coord endPos = new Coord(Math.min(pos.x + 1, field.getArea().br.x - 1), Math.max(pos.y - 1, field.getArea().ul.y));
                            Area harea = new Area(pos, endPos, true);
                            Coord2d endp = harea.br.mul(MCache.tilesz).add(MCache.tilehsz).add(0, MCache.tileqsz.y);
                            seedCrop(gui, barrels, stockPiles, harea, revdir, endp, setDir);
                            pos.y -= 2;
                        }
                        pos.y = field.getArea().ul.y;
                    }
                    revdir = !revdir;
                    pos.x += 2;
                }
            }
        } while (Finder.findGobs(field, crop).size() != tiles.size());

        dropOffSeeds(gui, barrels);

        return Results.SUCCESS();
    }

    private void dropOffSeeds(NGameUI gui, ArrayList<Gob> barrels) throws InterruptedException {
        if(barrels.isEmpty() && allowedToPlantFromStockpiles) {
            new TransferToPiles(seed.getRCArea(), iseed).run(gui);
        } else {
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                for (Gob barrel : barrels) {
                    TransferToBarrel tb;
                    (tb = new TransferToBarrel(barrel, iseed)).run(gui);
                    if (!tb.isFull())
                        break;
                }
            }
        }
    }

    void seedCrop(NGameUI gui, ArrayList<Gob> barrels, ArrayList<Gob> stockpiles, Area area, boolean rev, Coord2d target_coord, AtomicBoolean setDir) throws InterruptedException {
        if (gui.getInventory().getItems(iseed).size() < 5) {
            if (!gui.hand.isEmpty()) {
                NUtils.dropToInv();
            }

            if (!barrels.isEmpty() && gui.getInventory().getItems(iseed).size() < 2)
                fetchSeedsFromBarrel(gui, barrels);
            else if (!stockpiles.isEmpty())
                fetchSeedsFromStockpiles(gui);
            else
                NUtils.getGameUI().msg("No items for seeding");

        }

        //Area.Tile[][] tiles = area.getTiles(area, new NAlias("gfx/terobjs/moundbed"));
        Area.Tile[][] tiles = area.getTiles(area, new NAlias(new ArrayList<String>(Arrays.asList("gfx/terobjs/moundbed", "straw"))));
        int count;
        int total = 0;
        count = 0;
        for (int i = 0; i <= area.br.x - area.ul.x; i++) {
            for (int j = 0; j <= area.br.y - area.ul.y; j++) {
                if (NParser.checkName(tiles[i][j].name, "field")) {
                    total++;
                    if (tiles[i][j].isFree)
                        count++;
                }
            }
        }
        if(gui.getInventory().getItems(iseed).size() < 2) {
            NUtils.getGameUI().msg("No items for seeding");
            return;
        }
        if (count > 0) {
            if(PathFinder.isAvailable(target_coord)) {
                new PathFinder(target_coord).run(NUtils.getGameUI());
                if (setDir.get()) {
                    if (rev)
                        new SetDir(new Coord2d(0, 1)).run(gui);
                    else
                        new SetDir(new Coord2d(0, -1)).run(gui);
                    setDir.set(false);
                }
            }
            else
            {
                for (int i = 0; i <= area.br.x - area.ul.x; i++) {
                    for (int j = 0; j <= area.br.y - area.ul.y; j++) {
                        if (NParser.checkName(tiles[i][j].name, "field")) {
                            new PathFinder(new Coord(area.ul.x+i,area.ul.y+j).mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y)).run(gui);
                        }
                    }
                }
            }
//            int stacks_size = NUtils.getGameUI().getInventory().getTotalAmountItems(iseed);
//            if(stacks_size >=20){
            int stacks_size = 0;
            if(!barrels.isEmpty()) {
                stacks_size = NUtils.getGameUI().getInventory().getTotalAmountItems(iseed);
            } else {
                stacks_size = NUtils.getGameUI().getInventory().getItems(iseed).size();
            }

            NUtils.getGameUI().getInventory().activateItem(iseed);
            NUtils.getUI().core.addTask(new GetCurs("harvest"));

            if (rev) {
                NUtils.getGameUI().map.wdgmsg("sel", area.ul, area.br, 1);
            } else {
                NUtils.getGameUI().map.wdgmsg("sel", area.br, area.ul, 1);
            }
            NUtils.getUI().core.addTask(new WaitGobsInField(area, total));

            if(!barrels.isEmpty()) {
                NUtils.getUI().core.addTask(new WaitAnotherAmount(NUtils.getGameUI().getInventory(),iseed,stacks_size));
            } else if (!stockpiles.isEmpty()) {
                fetchSeedsFromStockpiles(gui);
            }
        }

    }

    private void fetchSeedsFromBarrel(NGameUI gui, ArrayList<Gob> barrels) throws InterruptedException {
        for (Gob barrel : barrels) {
            if (gui.getInventory().getItems(iseed).size() < 2 && NUtils.barrelHasContent(barrel)) {
                new TakeFromBarrel(barrel, iseed).run(gui);
            }
        }

        if (gui.getInventory().getItems(iseed).size() < 2) {
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                for (Gob barrel : barrels) {
                    TransferToBarrel tb;
                    (tb = new TransferToBarrel(barrel, iseed)).run(gui);
                    if (!tb.isFull())
                        break;
                }
            }
            gui.error("NO SEEDS: ABORT");
            throw new InterruptedException();
        }
    }

    private void fetchSeedsFromStockpiles(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> stockPiles = Finder.findGobs(seed, new NAlias("stockpile"));

        if(gui.getInventory().getItems(iseed).size() < 4) {
            for (Gob stockpile : stockPiles) {
                if (gui.getInventory().getFreeSpace() > 0) {
                    new PathFinder(stockpile).run(gui);
                    new OpenTargetContainer("Stockpile", stockpile).run(gui);
                    new TakeItemsFromPile(stockpile, gui.getStockpile(), gui.getInventory().getFreeSpace()).run(gui);
                }
            }
        }

        if (gui.getInventory().getItems(iseed).size() < 5) {
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                new TransferToPiles(seed.getRCArea(), iseed).run(gui);
            }
            gui.error("NO SEEDS: ABORT");
            throw new InterruptedException();
        }
    }
}

