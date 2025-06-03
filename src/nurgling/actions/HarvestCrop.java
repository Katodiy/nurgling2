package nurgling.actions;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.GetCurs;
import nurgling.tasks.NoGob;
import nurgling.tasks.WaitGobsInField;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static haven.OCache.posres;

public class HarvestCrop implements Action {

    final NArea field;
    final NArea seed;

    final NArea trougha;
    NArea swill = null;

    final NAlias crop;
    final NAlias iseed;

    int stage;
    boolean allowedToPlantFromStockpiles;

    public HarvestCrop(NArea field, NArea seed, NArea trough, NAlias crop, NAlias iseed, int stage) {
        this.field = field;
        this.seed = seed;
        this.trougha = trough;
        this.crop = crop;
        this.iseed = iseed;
        this.stage = stage;
    }

    public HarvestCrop(NArea field, NArea seed, NArea trough, NArea swill, NAlias crop, NAlias iseed, int stage, boolean allowedToPlantFromStockpiles) {
        this(field,seed,trough,crop,iseed,stage);
        this.swill = swill;
        this.allowedToPlantFromStockpiles = allowedToPlantFromStockpiles;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        ArrayList<Gob> barrels = Finder.findGobs(seed, new NAlias("barrel"));
        ArrayList<Gob> stockPiles = Finder.findGobs(seed, new NAlias("stockpile"));
        Gob trough = Finder.findGob(trougha, new NAlias("gfx/terobjs/trough"));
        Gob cistern = null;
        if(this.swill!=null)
        {
            cistern = Finder.findGob(swill, new NAlias("gfx/terobjs/cistern"));
        }
        HashMap<Gob, AtomicBoolean> barrelInfo = new HashMap();
        if (barrels.isEmpty()) {
            if(!allowedToPlantFromStockpiles || stockPiles.isEmpty()) {
                return Results.ERROR("No barrel for seed");
            } else {
                new TransferToPiles(seed.getRCArea(), crop).run(gui);

                if (!gui.getInventory().getItems(crop).isEmpty()) {
                    new TransferToTrough(trough, crop, cistern).run(gui);
                }
            }
        } else {
            for(Gob barrel : barrels) {
                TransferToBarrel tb;
                (tb = new TransferToBarrel(barrel, iseed)).run(gui);
                barrelInfo.put(barrel, new AtomicBoolean(tb.isFull()));
            }
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                new TransferToTrough(trough, iseed, cistern).run(gui);
            }
        }

        Coord start = gui.map.player().rc.dist(field.getArea().br.mul(MCache.tilesz)) < gui.map.player().rc.dist(field.getArea().ul.mul(MCache.tilesz)) ? field.getArea().br.sub(1, 1) : field.getArea().ul;
        Coord pos = new Coord(start);
        boolean rev = (pos.equals(field.getArea().ul));

        boolean revdir = rev;

        while (!Finder.findGobs(field, crop, stage).isEmpty() || !Finder.findGobs(field, new NAlias("gfx/terobjs/plants/fallowplant"), 0).isEmpty() ) {
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

        if(barrels.isEmpty()) {
            if(!gui.getInventory().getItems(iseed).isEmpty()) {
                new TransferToPiles(seed.getRCArea(), iseed).run(gui);

                if (!gui.getInventory().getItems(iseed).isEmpty()) {
                    new TransferToTrough(trough, iseed, cistern).run(gui);
                }
            }
        } else {
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                for(Gob barrel : barrelInfo.keySet()) {
                    if (!gui.getInventory().getItems(iseed).isEmpty()) {
                        if (!barrelInfo.get(barrel).get()) {
                            TransferToBarrel tb;
                            (tb = new TransferToBarrel(barrel, iseed)).run(gui);
                            barrelInfo.put(barrel, new AtomicBoolean(tb.isFull()));
                        }
                    }
                }
                if (!gui.getInventory().getItems(iseed).isEmpty()) {
                    new TransferToTrough(trough, iseed, cistern).run(gui);
                }
            }
        }

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
        plant = Finder.findGob(plantGobEndpoint.div(MCache.tilesz).floor(),crop, stage);
        if(plant == null)
        {
            plant = Finder.findGob(plantGobEndpoint.div(MCache.tilesz).floor(),new NAlias("gfx/terobjs/plants/fallowplant"), 0);
        }
        if(plant!=null) {
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
            dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
            new SelectFlowerAction("Harvest", plant).run(gui);
            NUtils.getUI().core.addTask(new NoGob(plant.id));
        }
        ArrayList<Gob> plants;
        while (!(plants = Finder.findGobs(area,crop,stage)).isEmpty())
        {
            plant = plants.get(0);
            new PathFinder(plant).run(gui);
            new SelectFlowerAction("Harvest", plant).run(gui);
            NUtils.getUI().core.addTask(new NoGob(plant.id));
            dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
        }

        while (!(plants = Finder.findGobs(area,new NAlias("gfx/terobjs/plants/fallowplant"), 0)).isEmpty())
        {
            plant = plants.get(0);
            new PathFinder(plant).run(gui);
            new SelectFlowerAction("Harvest", plant).run(gui);
            NUtils.getUI().core.addTask(new NoGob(plant.id));
            dropOffSeed(gui, barrelInfo.keySet(), trough, cistern);
        }
    }

    private void dropOffSeed(NGameUI gui, Set<Gob> barrels, Gob trough, Gob cistern) throws InterruptedException {
        if(barrels.isEmpty() && allowedToPlantFromStockpiles) {
            for(WItem seedItem : gui.getInventory().getItems(iseed)) {
                NUtils.drop(seedItem);
            }
            return;
        }

        if (gui.getInventory().getFreeSpace() < 3) {
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                for (Gob barrel : barrels) {
                    TransferToBarrel tb;
                    (tb = new TransferToBarrel(barrel, iseed)).run(gui);
                    if (!tb.isFull())
                        break;
                }
            }
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                new TransferToTrough(trough, iseed, cistern).run(gui);
            }
        }
    }
}
