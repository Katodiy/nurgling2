package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.GetCurs;
import nurgling.tasks.WaitGobsInField;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class SeedCrop implements Action{

    final NArea field;
    final NArea seed;

    final NArea trougha;
    NArea swill = null;

    final NAlias crop;
    final NAlias iseed;

    int stage;

    public SeedCrop(NArea field, NArea seed, NArea trough, NAlias crop, NAlias iseed, int stage) {
        this.field = field;
        this.seed = seed;
        this.trougha = trough;
        this.crop = crop;
        this.iseed = iseed;
        this.stage = stage;
    }

    public SeedCrop(NArea field, NArea seed, NArea trough, NArea swill, NAlias crop, NAlias iseed, int stage) {
        this(field,seed,trough,crop,iseed,stage);
        this.swill = swill;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        Gob barrel = Finder.findGob(seed, new NAlias("barrel"));
        if (barrel == null)
            return Results.ERROR("No barrel for seed");


        ArrayList<Coord2d> tiles = field.getTiles(new NAlias("field"));

        Coord start = gui.map.player().rc.dist(field.getArea().br.mul(MCache.tilesz)) < gui.map.player().rc.dist(field.getArea().ul.mul(MCache.tilesz)) ? field.getArea().br.sub(1, 1) : field.getArea().ul;
        Coord pos = new Coord(start);
        boolean rev = (pos.equals(field.getArea().ul));

        boolean revdir = rev;

        do {
            if (!rev) {
                while (pos.x >= field.getArea().ul.x) {
                    boolean setDir = true;
                    if (revdir) {
                        while (pos.y <= field.getArea().br.y - 1) {
                            Coord endPos = new Coord(Math.max(pos.x - 1, field.getArea().ul.x), Math.min(pos.y + 1, field.getArea().br.y - 1));
                            Area harea = new Area(pos, endPos, true);
                            NUtils.getGameUI().msg(harea.space());
                            seedCrop(gui, barrel, harea, revdir, harea.ul.mul(MCache.tilesz));
                            pos.y += 2;
                            if (setDir) {
                                new SetDir(new Coord2d(0, 1)).run(gui);
                                setDir = false;
                            }
                        }
                        pos.y = field.getArea().br.y - 1;
                    } else {
                        while (pos.y >= field.getArea().ul.y) {
                            Coord endPos = new Coord(Math.max(pos.x - 1, field.getArea().ul.x), Math.max(pos.y - 1, field.getArea().ul.y));
                            Area harea = new Area(pos, endPos, true);
                            NUtils.getGameUI().msg(harea.space());
                            seedCrop(gui, barrel, harea, revdir, harea.br.mul(MCache.tilesz).add(MCache.tilehsz.x, MCache.tilehsz.y));
                            pos.y -= 2;
                            if (setDir) {
                                new SetDir(new Coord2d(0, -1)).run(gui);
                                setDir = false;
                            }
                        }
                        pos.y = field.getArea().ul.y;
                    }
                    revdir = !revdir;
                    pos.x -= 2;
                }
            } else {
                while (pos.x <= field.getArea().br.x - 1) {
                    boolean setDir = true;
                    if (revdir) {
                        while (pos.y <= field.getArea().br.y - 1) {
                            Coord endPos = new Coord(Math.min(pos.x + 1, field.getArea().br.x - 1), Math.min(pos.y + 1, field.getArea().br.y - 1));
                            Area harea = new Area(pos, endPos, true);
                            NUtils.getGameUI().msg(harea.space());
                            seedCrop(gui, barrel, harea, revdir, harea.ul.mul(MCache.tilesz));
                            pos.y += 2;
                            if (setDir) {
                                new SetDir(new Coord2d(0, 1)).run(gui);
                                setDir = false;
                            }
                        }
                        pos.y = field.getArea().br.y - 1;
                    } else {
                        while (pos.y >= field.getArea().ul.y) {
                            Coord endPos = new Coord(Math.min(pos.x + 1, field.getArea().br.x - 1), Math.max(pos.y - 1, field.getArea().ul.y));
                            Area harea = new Area(pos, endPos, true);
                            NUtils.getGameUI().msg(harea.space());
                            seedCrop(gui, barrel, harea, revdir, harea.br.mul(MCache.tilesz).add(MCache.tilehsz));
                            pos.y -= 2;
                            if (setDir) {
                                new SetDir(new Coord2d(0, -1)).run(gui);
                                setDir = false;
                            }
                        }
                        pos.y = field.getArea().ul.y;
                    }
                    revdir = !revdir;
                    pos.x += 2;
                }
            }
        } while (Finder.findGobs(field, crop).size() != tiles.size());

        if (!gui.getInventory().getItems(iseed).isEmpty()) {
            new TransferToBarrel(barrel, iseed).run(gui);
        }
        return Results.SUCCESS();
    }

    void  seedCrop(NGameUI gui, Gob barrel, Area area, boolean rev, Coord2d target_coord) throws InterruptedException {
        if (gui.getInventory().getItems(iseed).size() < 2) {
            if (!gui.hand.isEmpty()) {
                NUtils.dropToInv();
            }
            new TakeFromBarrel(barrel, iseed).run(gui);
            if (gui.getInventory().getItems(iseed).size() < 2)
            {
                if (!gui.getInventory().getItems(iseed).isEmpty()) {
                    new TransferToBarrel(barrel, iseed).run(gui);
                }
                throw new InterruptedException("No seeds");
            }
        }

        Area.Tile[][] tiles =  area.getTiles(area, new NAlias("gfx/terobjs/moundbed"));
        int count;
        int total = 0;
        count = 0;
        for (int i = 0; i <= area.br.x - area.ul.x; i++) {
            for (int j = 0; j <= area.br.y - area.ul.y; j++) {
                if ( NParser.checkName(tiles[i][j].name, "field")) {
                    total++;
                    if(tiles[i][j].isFree)
                        count++;
                }
            }
        }

           if(count>0) {
               new PathFinder(target_coord).run(NUtils.getGameUI());
               NUtils.getGameUI().getInventory().activateItem(iseed);
               NUtils.getUI().core.addTask(new GetCurs("harvest"));
               if (rev) {
                   NUtils.getGameUI().map.wdgmsg("sel", area.ul, area.br, 1);
               } else {
                   NUtils.getGameUI().map.wdgmsg("sel", area.br, area.ul, 1);
               }
               NUtils.getUI().core.addTask(new WaitGobsInField(area, total));

           }
       }

//    }
}
