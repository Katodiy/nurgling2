package nurgling.actions;

import haven.*;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.GetCurs;
import nurgling.tasks.WaitAnotherAmount;
import nurgling.tasks.WaitGobsInField;
import nurgling.tasks.WaitItems;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SeedCrop implements Action {

    final NArea field;
    final NArea seed;


    final NAlias crop;
    final NAlias iseed;

    final boolean allowedToPlantFromStockpiles;
    boolean isQualityGrid = false;


    public SeedCrop(NArea field, NArea seed, NAlias crop, NAlias iseed, boolean allowedToPlantFromStockpiles) {
        this.field = field;
        this.seed = seed;
        this.crop = crop;
        this.iseed = iseed;
        this.allowedToPlantFromStockpiles = allowedToPlantFromStockpiles;
    }

    public SeedCrop(
            NArea field,
            NArea seed,
            NAlias crop,
            NAlias iseed,
            boolean allowedToPlantFromStockpiles,
            boolean isQualityGrid
    ) {
        this.field = field;
        this.seed = seed;
        this.crop = crop;
        this.iseed = iseed;
        this.allowedToPlantFromStockpiles = allowedToPlantFromStockpiles;
        this.isQualityGrid = isQualityGrid;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if (isQualityGrid) {
            seedForQuality(gui);
            return Results.SUCCESS();
        }

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

        Area.Tile[][] tiles = area.getTiles(area, new NAlias("gfx/terobjs/moundbed"));
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

        if(gui.getInventory().getItems(iseed).size() < 9) {
            for (Gob stockpile : stockPiles) {
                if (gui.getInventory().getFreeSpace() > 0) {
                    new PathFinder(stockpile).run(gui);
                    new OpenTargetContainer("Stockpile", stockpile).run(gui);

                    int numberOfItemsToFetch = gui.getInventory().getFreeSpace();
                    if(((NInventory) NUtils.getGameUI().maininv).bundle.a) {
                        numberOfItemsToFetch = numberOfItemsToFetch * StackSupporter.getMaxStackSize(iseed.getDefault());
                    }

                    new TakeItemsFromPile(stockpile, gui.getStockpile(), numberOfItemsToFetch).run(gui);
                }
            }
        }

        if (gui.getInventory().getItems(iseed).size() < 2) {
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                new TransferToPiles(seed.getRCArea(), iseed).run(gui);
            }
            gui.error("NO SEEDS: ABORT");
            throw new InterruptedException();
        }
    }

    private void seedForQuality(NGameUI gui) throws InterruptedException {
        final int maxTilesToSeed = 4;
        // 1. Find up to 4 empty "field" tiles using Area.Tile[][] and isFree
        Area.Tile[][] tiles = field.getArea().getTiles(field.getArea(), new NAlias("gfx/terobjs/moundbed"));

        ArrayList<Coord> emptyCoords = new ArrayList<>();
        for (int i = 0; i <= field.getArea().br.x - field.getArea().ul.x; i++) {
            for (int j = 0; j <= field.getArea().br.y - field.getArea().ul.y; j++) {
                Area.Tile tile = tiles[i][j];
                if (NParser.checkName(tile.name, "field") && tile.isFree) {
                    emptyCoords.add(new Coord(field.getArea().ul.x + i, field.getArea().ul.y + j));
                }
            }
        }

        if (emptyCoords.isEmpty()) {
            gui.msg("No empty tiles found for seeding!");
            return;
        }

        // Only seed up to 4
        ArrayList<Coord> toSeed = new ArrayList<>(emptyCoords.subList(0, Math.min(maxTilesToSeed, emptyCoords.size())));

        // 2. Find all containers in the seed area (chests, cupboards, etc)
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(seed.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }
        if(containers.isEmpty())
            throw new RuntimeException("No container found in seed area!");
        Container container = containers.get(0);

        // 3. Get all seeds in the container
        new OpenTargetContainer(container).run(gui);
        ArrayList<WItem> seeds = gui.getInventory(container.cap).getItems(iseed);
        if (seeds.size() < toSeed.size()) {
            gui.error("Not enough seeds in container for quality seeding!");
            throw new InterruptedException();
        }

        // 4. Fetch top seeds to inventory
        new TakeAvailableItemsFromContainer(container, iseed, toSeed.size(), NInventory.QualityType.High).run(gui);

        // 5. Seed just those 4 tiles, individually
        for (Coord tile : toSeed) {
            new PathFinder(tile.mul(MCache.tilesz).add(MCache.tilehsz)).run(gui);
            NUtils.getGameUI().getInventory().activateItem(iseed);
            NUtils.getGameUI().map.wdgmsg("sel", tile, tile, 1);
            NUtils.getUI().core.addTask(new WaitGobsInField(new Area(tile, tile), 1));
        }

        // Return seeds to the chest
        for (Gob sm : Finder.findGobs(seed.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }

        seeds = gui.getInventory().getItems(iseed);

        Set<String> processed = new HashSet<>();
        for (WItem item : seeds) {
            String itemName = ((NGItem)item.item).name();
            if(processed.add(itemName)) {
                new TransferToContainer(container, new NAlias(itemName)).run(gui);
            }
        }
    }
}

