package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.NoGob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class HarvestCropBarrel implements Action{

    final NAlias crop;
    final NAlias iseed;

    final Pair<Coord2d,Coord2d> fieldC;
    final Pair<Coord2d,Coord2d> seedC;
    final Pair<Coord2d,Coord2d> troughC;

    int stage;

    public HarvestCropBarrel(Pair<Coord2d,Coord2d> field, Pair<Coord2d,Coord2d> seed, NAlias crop, NAlias iseed, int stage) {
        this.fieldC = field;
        this.seedC = seed;
        this.troughC = seed;
        this.crop = crop;
        this.iseed = iseed;
        this.stage = stage;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        ArrayList<Gob> barrels = Finder.findGobs(seedC, new NAlias("barrel"));
        Gob trough = Finder.findGob(troughC, new NAlias("gfx/terobjs/trough"));
        Gob cistern = null;

        if (barrels.isEmpty())
            return Results.ERROR("No barrel for seed");
//        if (trough == null)
//            return Results.ERROR("No trough for seed");
        HashMap<Gob, AtomicBoolean> barrelInfo = new HashMap();
        for(Gob barrel : barrels) {
            TransferToBarrel tb;
            (tb = new TransferToBarrel(barrel, iseed)).run(gui);
            barrelInfo.put(barrel, new AtomicBoolean(tb.isFull()));
        }
        if (!gui.getInventory().getItems(iseed).isEmpty()) {
            new TransferToTrough(trough, iseed, cistern).run(gui);
        }

        Area fieldA = new Area(fieldC);

        Coord start = gui.map.player().rc.dist(fieldA.br.mul(MCache.tilesz)) < gui.map.player().rc.dist(fieldA.ul.mul(MCache.tilesz)) ? fieldA.br.sub(1, 1) : fieldA.ul;
        Coord pos = new Coord(start);
        new PathFinder(Coord2d.of(start)).run(NUtils.getGameUI());
        boolean rev = (pos.equals(fieldA.ul));

        boolean revdir = rev;

        while (!Finder.findGobs(fieldC, crop, stage).isEmpty() || !Finder.findGobs(fieldC, new NAlias("gfx/terobjs/plants/fallowplant"), 0).isEmpty() ) {
                if (false) {
                    while (pos.x <= fieldA.ul.x) {
                        AtomicBoolean setDir = new AtomicBoolean(true);
                        if (revdir) {
                            while (pos.y <= fieldA.br.y - 1) {
                                Coord endPos = new Coord(Math.max(pos.x - 2, fieldA.ul.x), Math.min(pos.y + 1, fieldA.br.y - 1));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d endp = harea.ul.mul(MCache.tilesz).add( MCache.tilesz.x + MCache.tilehsz.x, MCache.tilehsz.y).sub(0,MCache.tileqsz.y);
                                harvest(gui, barrelInfo, trough, cistern, harea, revdir, endp, setDir);
                                pos.y += 2;
                            }
                            pos.y = fieldA.br.y - 1;
                        } else {
                            while (pos.y >= fieldA.ul.y) {
                                Coord endPos = new Coord(Math.max(pos.x - 2, fieldA.ul.x), Math.max(pos.y - 1, fieldA.ul.y));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d endp = harea.br.mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y).sub(MCache.tilesz.x, 0).add(0,MCache.tileqsz.y);
                                harvest(gui, barrelInfo, trough, cistern, harea, revdir,endp , setDir);
                                pos.y -= 2;
                            }
                            pos.y = fieldA.ul.y;
                        }
                        revdir = !revdir;
                        pos.x -= 3;
                    }
                } else {
                    while (pos.x >= fieldA.br.x - 1) {
                        AtomicBoolean setDir = new AtomicBoolean(true);
                        if (revdir) {
                            while (pos.y <= fieldA.br.y - 1) {
                                Coord endPos = new Coord(Math.min(pos.x + 2, fieldA.br.x - 1), Math.min(pos.y + 1, fieldA.br.y - 1));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d endp = harea.ul.mul(MCache.tilesz).add(MCache.tilehsz.x+MCache.tilesz.x, MCache.tilehqsz.y + MCache.tileqsz.y);
                                harvest(gui, barrelInfo, trough, cistern, harea, revdir, endp, setDir);
                                pos.y += 2;
                            }
                            pos.y = fieldA.br.y - 1;
                        } else {
                            while (pos.y >= fieldA.ul.y) {
                                Coord endPos = new Coord(Math.min(pos.x + 2, fieldA.br.x - 1), Math.max(pos.y - 1, fieldA.ul.y));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d endp = harea.br.mul(MCache.tilesz).add(MCache.tilehsz).sub(MCache.tilesz.x, 0).add(0,MCache.tileqsz.y);
                                harvest(gui, barrelInfo, trough, cistern, harea, revdir, endp, setDir);
                                pos.y -= 2;
                            }
                            pos.y = fieldA.ul.y;
                        }
                        revdir = !revdir;
                        pos.x += 3;
                    }
                }



        }

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
        return Results.SUCCESS();
    }


    void harvest(NGameUI gui, HashMap<Gob,AtomicBoolean> barrelInfo, Gob trough, Gob cistern, Area area, boolean rev, Coord2d target_coord, AtomicBoolean setDir) throws InterruptedException {
        if (gui.getInventory().getFreeSpace() <= 4) {
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
        if(NUtils.getStamina()<0.35)
            if(!new Drink(0.9,false).run(gui).isSuccess)
                throw new InterruptedException();
        Gob plant;
        plant = Finder.findGob(target_coord.div(MCache.tilesz).floor(),crop, stage);
        if(plant == null)
        {
            plant = Finder.findGob(target_coord.div(MCache.tilesz).floor(),new NAlias("gfx/terobjs/plants/fallowplant"), 0);
        }
        if(plant!=null) {
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
                new PathFinder(plant).run(NUtils.getGameUI());
            }
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
        }

        while (!(plants = Finder.findGobs(area,new NAlias("gfx/terobjs/plants/fallowplant"), 0)).isEmpty())
        {
            plant = plants.get(0);
            new PathFinder(plant).run(gui);
            new SelectFlowerAction("Harvest", plant).run(gui);
            NUtils.getUI().core.addTask(new NoGob(plant.id));
        }
    }
}
