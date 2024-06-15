package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.GetCurs;
import nurgling.tasks.NoGob;
import nurgling.tasks.WaitGobsInField;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static haven.OCache.posres;

public class HarvestCrop implements Action{

    final NArea field;
    final NArea seed;

    final NArea trougha;
    NArea swill = null;

    final NAlias crop;
    final NAlias iseed;

    int stage;

    public HarvestCrop(NArea field, NArea seed, NArea trough, NAlias crop, NAlias iseed, int stage) {
        this.field = field;
        this.seed = seed;
        this.trougha = trough;
        this.crop = crop;
        this.iseed = iseed;
        this.stage = stage;
    }

    public HarvestCrop(NArea field, NArea seed, NArea trough, NArea swill, NAlias crop, NAlias iseed, int stage) {
        this(field,seed,trough,crop,iseed,stage);
        this.swill = swill;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        Gob barrel = Finder.findGob(seed, new NAlias("barrel"));
        Gob trough = Finder.findGob(trougha, new NAlias("gfx/terobjs/trough"));
        Gob cistern = null;
        if(this.swill!=null)
        {
            cistern = Finder.findGob(swill, new NAlias("gfx/terobjs/cistern"));
        }
        if (barrel == null)
            return Results.ERROR("No barrel for seed");
        if (trough == null)
            return Results.ERROR("No trough for seed");

        TransferToBarrel tb;
        (tb = new TransferToBarrel(barrel, iseed)).run(gui);
        if (!gui.getInventory().getItems(iseed).isEmpty()) {
            new TransferToTrough(trough, iseed, cistern).run(gui);
        }

        AtomicBoolean isFull = new AtomicBoolean(tb.isFull());
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
                                Coord2d endp = harea.ul.mul(MCache.tilesz).add( MCache.tilesz.x + MCache.tilehsz.x, MCache.tilehsz.y).sub(0,MCache.tilehqsz.y);
                                harvest(gui, barrel, trough, cistern, harea, revdir, endp, isFull, setDir);
                                pos.y += 2;
                            }
                            pos.y = field.getArea().br.y - 1;
                        } else {
                            while (pos.y >= field.getArea().ul.y) {
                                Coord endPos = new Coord(Math.max(pos.x - 2, field.getArea().ul.x), Math.max(pos.y - 1, field.getArea().ul.y));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d endp = harea.br.mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y).sub(MCache.tilesz.x, 0).add(0,MCache.tilehqsz.y);
                                harvest(gui, barrel, trough, cistern, harea, revdir,endp , isFull, setDir);
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
                                Coord2d endp = harea.ul.mul(MCache.tilesz).add(MCache.tilehsz.x+MCache.tilesz.x, MCache.tilehqsz.y + MCache.tileqsz.y);
                                harvest(gui, barrel, trough, cistern, harea, revdir, endp, isFull ,setDir);
                                pos.y += 2;
                            }
                            pos.y = field.getArea().br.y - 1;
                        } else {
                            while (pos.y >= field.getArea().ul.y) {
                                Coord endPos = new Coord(Math.min(pos.x + 2, field.getArea().br.x - 1), Math.max(pos.y - 1, field.getArea().ul.y));
                                Area harea = new Area(pos, endPos, true);
                                Coord2d endp = harea.br.mul(MCache.tilesz).add(MCache.tilehsz).sub(MCache.tilesz.x, 0).add(0,MCache.tilehqsz.y);
                                harvest(gui, barrel, trough, cistern, harea, revdir, endp, isFull, setDir);
                                pos.y -= 2;
                            }
                            pos.y = field.getArea().ul.y;
                        }
                        revdir = !revdir;
                        pos.x += 3;
                    }
                }



        }

        if (!gui.getInventory().getItems(iseed).isEmpty()) {
            if (!isFull.get()) {
                new TransferToBarrel(barrel, iseed).run(gui);
            }
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                new TransferToTrough(trough, iseed, cistern).run(gui);
            }
        }
        return Results.SUCCESS();
    }


    void harvest(NGameUI gui, Gob barrel, Gob trough, Gob cistern, Area area, boolean rev, Coord2d target_coord, AtomicBoolean isFull, AtomicBoolean setDir) throws InterruptedException {
        if (gui.getInventory().getFreeSpace() < 3) {
            TransferToBarrel tb;
            if (!isFull.get()) {
                (tb = new TransferToBarrel(barrel, iseed)).run(gui);
                isFull.set(tb.isFull());
            }
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                new TransferToTrough(trough, iseed, cistern).run(gui);
            }
        }
        if(NUtils.getStamina()<0.35)
            new Drink(0.9).run(gui);
        Gob plant;
        plant = Finder.findGob(target_coord.div(MCache.tilesz).floor(),crop, stage);
        if(plant == null)
        {
            plant = Finder.findGob(target_coord.div(MCache.tilesz).floor(),new NAlias("gfx/terobjs/plants/fallowplant"), 0);
        }
        if(plant!=null) {
            new PathFinder(target_coord).run(gui);
            if (setDir.get()) {
                if(rev)
                    new SetDir(new Coord2d(0, 1)).run(gui);
                else
                    new SetDir(new Coord2d(0, -1)).run(gui);
                setDir.set(false);
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
