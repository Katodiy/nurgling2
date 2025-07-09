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
import nurgling.tools.*;

import java.util.*;
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
        if (barrels.isEmpty() && allowedToPlantFromStockpiles) {
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
        if (gui.getInventory().getItems(iseed).size() < 2) {
            NUtils.getGameUI().msg("No items for seeding");
            return;
        }
        if (count > 0) {
            if (PathFinder.isAvailable(target_coord)) {
                new PathFinder(target_coord).run(NUtils.getGameUI());
                if (setDir.get()) {
                    if (rev)
                        new SetDir(new Coord2d(0, 1)).run(gui);
                    else
                        new SetDir(new Coord2d(0, -1)).run(gui);
                    setDir.set(false);
                }
            } else {
                for (int i = 0; i <= area.br.x - area.ul.x; i++) {
                    for (int j = 0; j <= area.br.y - area.ul.y; j++) {
                        if (NParser.checkName(tiles[i][j].name, "field")) {
                            new PathFinder(new Coord(area.ul.x + i, area.ul.y + j).mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y)).run(gui);
                        }
                    }
                }
            }
            int stacks_size = 0;
            if (!barrels.isEmpty()) {
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

            if (!barrels.isEmpty()) {
                NUtils.getUI().core.addTask(new WaitAnotherAmount(NUtils.getGameUI().getInventory(), iseed, stacks_size));
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

        if (gui.getInventory().getItems(iseed).size() < 9) {
            for (Gob stockpile : stockPiles) {
                if (gui.getInventory().getFreeSpace() > 0) {
                    new PathFinder(stockpile).run(gui);
                    new OpenTargetContainer("Stockpile", stockpile).run(gui);

                    int numberOfItemsToFetch = gui.getInventory().getFreeSpace();
                    if (((NInventory) NUtils.getGameUI().maininv).bundle.a) {
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
        int[] seedingPattern = getQualitySeedingPattern();
        int patX = seedingPattern[0];
        int patY = seedingPattern[1];

        Area.Tile[][] tiles = field.getArea().getTiles(field.getArea(), new NAlias("gfx/terobjs/moundbed"));

        // Try both orientations
        int patchesXY = countPossiblePatches(field.getArea(), tiles, patX, patY);
        int patchesYX = countPossiblePatches(field.getArea(), tiles, patY, patX);

        boolean useRotated = patchesYX > patchesXY;

        int useX = useRotated ? patY : patX;
        int useY = useRotated ? patX : patY;

        ArrayList<Coord> toSeed = findFirstFreePatch(field.getArea(), tiles, useX, useY);

        if (toSeed == null) {
            gui.msg("No empty patch of " + useX + "x" + useY + " found for quality seeding!");
            return;
        }

        // 2. Find all containers in the seed area (chests, cupboards, etc)
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(seed.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }
        if (containers.isEmpty())
            throw new RuntimeException("No container found in seed area!");
        Container container = containers.get(0);

        new PathFinder(Finder.findGob(container.gobid)).run(gui);

        // 3. Get all seeds in the container
        new OpenTargetContainer(container).run(gui);
        ArrayList<WItem> seeds = gui.getInventory(container.cap).getItems(iseed);

        int canSeedCells = getSeedingCapacity(seeds);

        if (canSeedCells < toSeed.size()) {
            gui.error("Not enough seeds in container for quality seeding!");
            throw new InterruptedException();
        }

        int fetchCount = Math.min(seeds.size(), gui.getInventory().getFreeSpace());

        // 4. Fetch top seeds to inventory
        new TakeAvailableItemsFromContainer(container, iseed, fetchCount, NInventory.QualityType.High).run(gui);

        List<WItem> plantingOrder = getPlantingOrder(NUtils.getGameUI().getInventory().getItems(iseed), toSeed.size());

        // 5. Seed just those x*y tiles, individually
        for (int i = 0; i < toSeed.size(); i++) {
            WItem itemToPlant = plantingOrder.get(i);
            Coord tile = toSeed.get(i);

            new PathFinder(tile.mul(MCache.tilesz).add(MCache.tilehsz)).run(gui);
            NUtils.getGameUI().getInventory().activateItem(itemToPlant);
            NUtils.getGameUI().map.wdgmsg("sel", tile, tile, 1);
            NUtils.getUI().core.addTask(new WaitGobsInField(new Area(tile, tile), 1));
        }

        // Return seeds to the chest
        for (Gob sm : Finder.findGobs(seed.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }

        List<WItem> crops = new ArrayList<>();
        List<WItem> seedsList = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        for (WItem item : seeds) {
            String name = ((NGItem) item.item).name().toLowerCase();
            if (name.contains("seed")) {
                seedsList.add(item);
            } else {
                crops.add(item);
            }
        }

        // We have to return crops before seeds (example Carrot before Carrot Seed) because otherwise it will try to
        // stack crop into seed.

        // Crops first (exclude seed items)
        for (WItem item : crops) {
            String itemName = ((NGItem) item.item).name();
            if (processed.add(itemName)) {
                new TransferToContainer(
                        container,
                        new NAlias(Collections.singletonList(itemName), Collections.singletonList("seed"))
                ).run(gui);
            }
        }

        // Seeds second
        for (WItem item : seedsList) {
            String itemName = ((NGItem) item.item).name();
            if (processed.add(itemName)) {
                new TransferToContainer(
                        container,
                        new NAlias(Collections.singletonList(itemName), Collections.emptyList())
                ).run(gui);
            }
        }

        new CloseTargetContainer(container).run(gui);
    }

    private int[] getQualitySeedingPattern() {
        String pat = (String) nurgling.NConfig.get(nurgling.NConfig.Key.qualityGrindSeedingPatter);
        if (pat == null || !pat.matches("\\d+x\\d+")) return new int[]{1, 4}; // default
        String[] parts = pat.split("x");
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            return new int[]{x, y};
        } catch (Exception e) {
            return new int[]{1, 4};
        }
    }

    // Counts how many non-overlapping patches of size patX x patY fit in the area
    private int countPossiblePatches(Area area, Area.Tile[][] tiles, int patX, int patY) {
        int patches = 0;
        boolean[][] used = new boolean[tiles.length][tiles[0].length];
        for (int i = 0; i <= tiles.length - patX; i++) {
            for (int j = 0; j <= tiles[0].length - patY; j++) {
                boolean ok = true;
                for (int dx = 0; dx < patX; dx++) {
                    for (int dy = 0; dy < patY; dy++) {
                        if (used[i + dx][j + dy]) ok = false;
                        Area.Tile tile = tiles[i + dx][j + dy];
                        if (!nurgling.tools.NParser.checkName(tile.name, "field") || !tile.isFree)
                            ok = false;
                    }
                }
                if (ok) {
                    patches++;
                    for (int dx = 0; dx < patX; dx++)
                        for (int dy = 0; dy < patY; dy++)
                            used[i + dx][j + dy] = true;
                }
            }
        }
        return patches;
    }

    private ArrayList<Coord> findFirstFreePatch(Area area, Area.Tile[][] tiles, int patX, int patY) {
        for (int i = 0; i <= tiles.length - patX; i++) {
            for (int j = 0; j <= tiles[0].length - patY; j++) {
                boolean ok = true;
                ArrayList<Coord> patch = new ArrayList<>();
                for (int dx = 0; dx < patX; dx++) {
                    for (int dy = 0; dy < patY; dy++) {
                        Area.Tile tile = tiles[i + dx][j + dy];
                        if (!nurgling.tools.NParser.checkName(tile.name, "field") || !tile.isFree)
                            ok = false;
                        patch.add(new Coord(area.ul.x + i + dx, area.ul.y + j + dy));
                    }
                }
                if (ok) return patch;
            }
        }
        return null;
    }

    private int getSeedingCapacity(List<WItem> seeds) {
        int totalCells = 0;
        for (WItem item : seeds) {
            int itemCount = 1; // Default: crops count as 1

            for (ItemInfo info : item.item.info()) {
                if (info instanceof GItem.Amount) {
                    int qty = ((GItem.Amount) info).itemnum();
                    itemCount = qty / 5; // Each 5 seeds = 1 cell

                    break;
                }
            }
            // If it's a crop, itemCount remains 1
            totalCells += itemCount;
        }
        return totalCells;
    }

    private List<WItem> getPlantingOrder(List<WItem> seeds, int tilesToPlant) {
        List<WItem> order = new ArrayList<>();
        int planted = 0;

        // Make a mutable list of seed quantities (parallel to seeds list)
        List<Integer> amounts = new ArrayList<>();
        for (WItem item : seeds) {
            int amt = -1;
            List<ItemInfo> infoList = item.item.info;
            if (infoList != null) {
                for (ItemInfo info : infoList) {
                    if (info instanceof GItem.Amount) {
                        amt = ((GItem.Amount) info).itemnum();
                        break;
                    }
                }
            }
            amounts.add(amt); // amt == -1 means it's a non-stackable crop
        }

        for (int i = 0; i < seeds.size() && planted < tilesToPlant; i++) {
            WItem item = seeds.get(i);
            int amt = amounts.get(i);

            if (amt == -1) {
                // Non-stackable: plant once
                order.add(item);
                planted++;
            } else {
                // Stackable: plant as many as possible in batches of 5
                while (amt >= 5 && planted < tilesToPlant) {
                    order.add(item);
                    amt -= 5;
                    planted++;
                }
            }
        }
        return order;
    }

}

