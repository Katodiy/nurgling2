package nurgling.actions;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.NoGob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

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
        if (barrel == null)
            return Results.ERROR("No barrel for seed");
        if (trough == null)
            return Results.ERROR("No trough for seed");

        TransferToBarrel tb;
        (tb = new TransferToBarrel(barrel, iseed)).run(gui);
        if (!gui.getInventory().getItems(iseed).isEmpty()) {
            new TransferToTrough(trough, iseed).run(gui);
        }

        boolean isFull = tb.isFull();
        Gob plant;
        while ((plant = Finder.findCrop(field, crop, stage)) != null) {
            if (gui.getInventory().getFreeSpace() < 2) {
                if (!isFull) {
                    (tb = new TransferToBarrel(barrel, iseed)).run(gui);
                    isFull = tb.isFull();
                }
                if (!gui.getInventory().getItems(iseed).isEmpty()) {
                    new TransferToTrough(trough, iseed).run(gui);
                }
            }
            if(NUtils.getStamina()<0.35)
                new Drink(0.9).run(gui);

            new PathFinder(plant.rc).run(gui);
            new SelectFlowerAction("Harvest", plant).run(gui);
            NUtils.getUI().core.addTask(new NoGob(plant.id));
        }

        if (!gui.getInventory().getItems(iseed).isEmpty()) {
            if (!isFull) {
                new TransferToBarrel(barrel, iseed).run(gui);
            }
            if (!gui.getInventory().getItems(iseed).isEmpty()) {
                new TransferToTrough(trough, iseed).run(gui);
            }
        }
        return Results.SUCCESS();
    }
}
