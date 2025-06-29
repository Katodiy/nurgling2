package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.conf.CropRegistry;
import nurgling.tasks.NoGob;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HarvestCrop implements Action {

    final NArea field;
    final NArea seed;

    final NArea trougha;
    NArea swill = null;

    final NAlias crop;
    boolean isQualityGrid = false;

    public HarvestCrop(NArea field, NArea seed, NArea trough, NAlias crop) {
        this.field = field;
        this.seed = seed;
        this.trougha = trough;
        this.crop = crop;
    }

    public HarvestCrop(NArea field, NArea seed, NArea trough, NArea swill, NAlias crop) {
        this(field,seed,trough,crop, false);
        this.swill = swill;
    }

    public HarvestCrop(NArea field, NArea seed, NArea trough, NAlias crop, boolean isQualityGrid) {
        this(field,seed,trough,crop);
        this.isQualityGrid = isQualityGrid;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        ArrayList<Gob> barrels = Finder.findGobs(seed, new NAlias("barrel"));

        Gob trough = null;

        if(!isQualityGrid) {
            trough = Finder.findGob(trougha, new NAlias("gfx/terobjs/trough"));
        }

        Gob cistern = null;
        if(this.swill!=null)
        {
            cistern = Finder.findGob(swill, new NAlias("gfx/terobjs/cistern"));
        }
        HashMap<Gob, AtomicBoolean> barrelInfo = new HashMap();
        if (barrels.isEmpty() && requiresBarrel(crop)) {
            return Results.ERROR("No barrel for seed");
        }

        for (CropRegistry.CropStage stage : CropRegistry.HARVESTABLE.getOrDefault(crop, Collections.emptyList())) {
            if (stage.storageBehavior == CropRegistry.StorageBehavior.BARREL) {
                // For each barrel, transfer all items of this type
                for (Gob barrel : barrels) {
                    TransferToBarrel tb = new TransferToBarrel(barrel, stage.result);
                    tb.run(gui);
                    barrelInfo.put(barrel, new AtomicBoolean(tb.isFull()));
                }
                // After barrels, transfer leftovers to trough/cistern
                if (!gui.getInventory().getItems(stage.result).isEmpty()) {
                    new TransferToTrough(trough, stage.result, cistern).run(gui);
                }
            }
        }

        Coord start = gui.map.player().rc.dist(field.getArea().br.mul(MCache.tilesz)) < gui.map.player().rc.dist(field.getArea().ul.mul(MCache.tilesz)) ? field.getArea().br.sub(1, 1) : field.getArea().ul;
        Coord pos = new Coord(start);
        boolean rev = (pos.equals(field.getArea().ul));

        boolean revdir = rev;

        while (hasAnyCropStage(field, crop) || !Finder.findGobs(field, new NAlias("gfx/terobjs/plants/fallowplant"), 0).isEmpty()) {
                if (!rev) {
                    while (pos.x >= field.getArea().ul.x) {
                        AtomicBoolean setDir = new AtomicBoolean(true);
                        if (revdir) {
                            while (pos.y <= field.getArea().br.y - 1) {
                                Coord endPos = new Coord(Math.max(pos.x - 2, field.getArea().ul.x), Math.min(pos.y + 1, field.getArea().br.y - 1));
                                Area harea = new Area(pos, endPos, true);

                                Coord2d plantGobEndpoint = harea.ul.mul(MCache.tilesz).add( MCache.tilesz.x + MCache.tilehsz.x, MCache.tilehsz.y).sub(0,MCache.tileqsz.y);
                                Coord2d pathfinderEndpoint = harea.ul.sub(0, 1).mul(MCache.tilesz).add( MCache.tilesz.x + MCache.tilehsz.x, MCache.tilehsz.y  + MCache.tileqsz.y);

                                harvest(gui, barrelInfo, trough, cistern, harea, revdir, pathfinderEndpoint, plantGobEndpoint, setDir);
                                pos.y += 2;
                            }
                            pos.y = field.getArea().br.y - 1;
                        } else {
                            while (pos.y >= field.getArea().ul.y) {
                                Coord endPos = new Coord(Math.max(pos.x - 2, field.getArea().ul.x), Math.max(pos.y - 1, field.getArea().ul.y));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d plantGobEndpoint = harea.br.mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y).sub(MCache.tilesz.x, 0).add(0,MCache.tileqsz.y);
                                Coord2d pathfinderEndpoint = harea.br.mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y).sub(MCache.tilesz.x, 0).add(0,MCache.tileqsz.y);
                                harvest(gui, barrelInfo, trough, cistern, harea, revdir, pathfinderEndpoint , plantGobEndpoint, setDir);
                                pos.y -= 2;
                            }
                            pos.y = field.getArea().ul.y;
                        }
                        revdir = !revdir;
                        pos.x -= 3;
                    }
                } else {
                    while (pos.x <= field.getArea().br.x - 1) {
                        AtomicBoolean setDir = new AtomicBoolean(true);
                        if (revdir) {
                            while (pos.y <= field.getArea().br.y - 1) {
                                Coord endPos = new Coord(Math.min(pos.x + 2, field.getArea().br.x - 1), Math.min(pos.y + 1, field.getArea().br.y - 1));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d plantGobEndpoint = harea.ul.mul(MCache.tilesz).add(MCache.tilehsz.x+MCache.tilesz.x, MCache.tilehqsz.y + MCache.tileqsz.y);
                                Coord2d pathfinderEndpoint = harea.ul.sub(0, 1).mul(MCache.tilesz).add( MCache.tilesz.x + MCache.tilehsz.x, MCache.tilehsz.y + MCache.tileqsz.y);
                                harvest(gui, barrelInfo, trough, cistern, harea, revdir, pathfinderEndpoint, plantGobEndpoint, setDir);
                                pos.y += 2;
                            }
                            pos.y = field.getArea().br.y - 1;
                        } else {
                            while (pos.y >= field.getArea().ul.y) {
                                Coord endPos = new Coord(Math.min(pos.x + 2, field.getArea().br.x - 1), Math.max(pos.y - 1, field.getArea().ul.y));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d plantGobEndpoint = harea.br.mul(MCache.tilesz).add(MCache.tilehsz).sub(MCache.tilesz.x, 0);
                                Coord2d pathfinderEndpoint = harea.br.mul(MCache.tilesz).add(MCache.tilehsz).sub(MCache.tilesz.x, 0).add(0,MCache.tileqsz.y);
                                harvest(gui, barrelInfo, trough, cistern, harea, revdir, pathfinderEndpoint, plantGobEndpoint, setDir);
                                pos.y -= 2;
                            }
                            pos.y = field.getArea().ul.y;
                        }
                        revdir = !revdir;
                        pos.x += 3;
                    }
                }
        }

        finalCleanup(gui, barrelInfo.keySet(), trough, cistern);

        return Results.SUCCESS();
    }


    void harvest(NGameUI gui, HashMap<Gob,AtomicBoolean> barrelInfo, Gob trough, Gob cistern, Area area, boolean rev, Coord2d pathfinderEndpoint, Coord2d plantGobEndpoint, AtomicBoolean setDir) throws InterruptedException {
        dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);

        if(NUtils.getStamina()<0.35) {
            if ((Boolean) NConfig.get(NConfig.Key.harvestautorefill)) {
                if (FillWaterskins.checkIfNeed())
                    if (!(new FillWaterskins(true).run(gui).IsSuccess()))
                        throw new InterruptedException();
            }

            if (!new Drink(0.9, false).run(gui).isSuccess)
                throw new InterruptedException();
        }
        Gob plant;
        plant = null;
        for (CropRegistry.CropStage cropStage : CropRegistry.HARVESTABLE.getOrDefault(crop, Collections.emptyList())) {
            plant = Finder.findGob(plantGobEndpoint.div(MCache.tilesz).floor(), crop, cropStage.stage);
            if(plant != null) {
                break;
            }
        }
        if(plant == null)
        {
            plant = Finder.findGob(plantGobEndpoint.div(MCache.tilesz).floor(),new NAlias("gfx/terobjs/plants/fallowplant"), 0);
        }
        if(plant!=null) {
            dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
            if(PathFinder.isAvailable(pathfinderEndpoint)) {
                new PathFinder(pathfinderEndpoint).run(NUtils.getGameUI());
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
                new PathFinder(plant).run(NUtils.getGameUI());
            }
            new SelectFlowerAction("Harvest", plant).run(gui);
            NUtils.getUI().core.addTask(new NoGob(plant.id));
        }

        ArrayList<Gob> plants;
        List<CropRegistry.CropStage> cropStages = CropRegistry.HARVESTABLE.getOrDefault(crop, Collections.emptyList());
        for (CropRegistry.CropStage cropStage : cropStages) {
            ArrayList<Gob> plantsToHarvest;
            while (!(plantsToHarvest = Finder.findGobs(area, crop, cropStage.stage)).isEmpty()) {
                dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
                Gob plantToHarvest = plantsToHarvest.get(0);
                new PathFinder(plantToHarvest).run(gui);
                new SelectFlowerAction("Harvest", plantToHarvest).run(gui);
                NUtils.getUI().core.addTask(new NoGob(plantToHarvest.id));
                dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
            }
        }

        while (!(plants = Finder.findGobs(area,new NAlias("gfx/terobjs/plants/fallowplant"), 0)).isEmpty())
        {
            dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
            plant = plants.get(0);
            new PathFinder(plant).run(gui);
            new SelectFlowerAction("Harvest", plant).run(gui);
            NUtils.getUI().core.addTask(new NoGob(plant.id));
            dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
        }

        dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
    }

    private void dropOffSeed(NGameUI gui, Set<Gob> barrels, Gob trough, Gob cistern) throws InterruptedException {
        processHarvestedItems(gui, barrels, trough, cistern, true);
    }

    private void finalCleanup(NGameUI gui, Set<Gob> barrels, Gob trough, Gob cistern) throws InterruptedException {
        processHarvestedItems(gui, barrels, trough, cistern, false);
    }

    private void processHarvestedItems(
            NGameUI gui,
            Set<Gob> barrels,
            Gob trough,
            Gob cistern,
            boolean barrelOnlyIfInventoryFull
    ) throws InterruptedException {
        Map<NAlias, CropRegistry.StorageBehavior> resultStorage = new HashMap<>();
        for (CropRegistry.CropStage stage : CropRegistry.HARVESTABLE.getOrDefault(crop, Collections.emptyList())) {
            resultStorage.put(stage.result, stage.storageBehavior);
        }

        List<WItem> barrelItems = new ArrayList<>();
        List<WItem> stockpileItems = new ArrayList<>();

        String name = "";
        for (WItem item : gui.getInventory().getItems()) {
            name = ((NGItem) item.item).name();
            CropRegistry.StorageBehavior behavior = resultStorage.get(new NAlias(name));
            if (behavior == null) continue;
            if (behavior == CropRegistry.StorageBehavior.BARREL) barrelItems.add(item);
            else if (behavior == CropRegistry.StorageBehavior.STOCKPILE) stockpileItems.add(item);
        }

        // In case item ends up in hand - drop it.
        if(NUtils.getGameUI().vhand!=null) {
            NUtils.drop(NUtils.getGameUI().vhand);
        }

        // 1. Always drop stockpile items
        if(!stockpileItems.isEmpty()) {
            dropAllItemsOfExactName(gui, stockpileItems);
        }

        // 2. Transfer barrel items if required
        if(!isQualityGrid) {
            boolean transferBarrel = !barrelItems.isEmpty()
                    && (!barrelOnlyIfInventoryFull || gui.getInventory().getFreeSpace() < 3);

            if (transferBarrel) {
                NAlias seedAlias = new NAlias(((NGItem) barrelItems.get(0).item).name());
                for (Gob barrel : barrels) {
                    TransferToBarrel tb = new TransferToBarrel(barrel, seedAlias);
                    tb.run(gui);
                    if (!tb.isFull()) break;
                }
                // 3. Leftover to trough/cistern
                if (!gui.getInventory().getItems(seedAlias).isEmpty()) {
                    new TransferToTrough(trough, seedAlias, cistern).run(gui);
                }
            }
        } else {
            // Find all containers in the seed area
            ArrayList<Container> containers = new ArrayList<>();
            for (Gob sm : Finder.findGobs(seed.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
                Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }

            if(containers.isEmpty())
                throw new RuntimeException("No container found in seed area!");
            Container container = containers.get(0);

            List<WItem> allItems = new ArrayList<>();
            allItems.addAll(barrelItems);
            allItems.addAll(stockpileItems);

            Set<String> processed = new HashSet<>();
            for (WItem item : allItems) {
                String itemName = ((NGItem)item.item).name();
                if(processed.add(itemName)) {
                    new TransferToContainer(container, new NAlias(itemName)).run(gui);
                }
            }
        }

    }

    private boolean hasAnyCropStage(NArea field, NAlias crop) throws InterruptedException {
        List<CropRegistry.CropStage> cropStages = CropRegistry.HARVESTABLE.getOrDefault(crop, Collections.emptyList());

        if (cropStages.isEmpty())
            return false;

        if (isQualityGrid) {
            // Only check the last (latest) stage. This is done specifically for Turnip and Carrots (eventually maybe
            // leek). We want the plant to get to the last stage to get the quality gain it requires.
            CropRegistry.CropStage lastStage = cropStages.get(cropStages.size() - 1);
            return !Finder.findGobs(field, crop, lastStage.stage).isEmpty();
        } else {
            for (CropRegistry.CropStage cs : cropStages) {
                if (!Finder.findGobs(field, crop, cs.stage).isEmpty())
                    return true;
            }
        }

        return false;
    }

    private boolean requiresBarrel(NAlias crop) {
        if(isQualityGrid) {
            return false;
        }

        for (CropRegistry.CropStage stage : CropRegistry.HARVESTABLE.getOrDefault(crop, Collections.emptyList())) {
            if (stage.storageBehavior == CropRegistry.StorageBehavior.BARREL) {
                return true;
            }
        }
        return false;
    }

    private void dropAllItemsOfExactName(NGameUI gui, List<WItem> targetItems) throws InterruptedException {
        if(!targetItems.isEmpty()) {
            String targetName = ((NGItem) targetItems.get(0).item).name();

            ArrayList<WItem> items = gui.getInventory().getWItems(new NAlias(targetName));

            for (WItem item : items) {
                NUtils.drop(item);
            }
        }
    }
}
