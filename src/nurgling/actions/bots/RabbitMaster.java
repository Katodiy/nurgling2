package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;
import nurgling.tasks.NTask;

import java.util.*;
import java.util.stream.Collectors;

public class RabbitMaster implements Action {
    private static final String HUTCH_NAME = "Rabbit Hutch";
    private static final NAlias HUTCH_RES = new NAlias("gfx/terobjs/rabbithutch");
    private static final NAlias BUCK_ALIAS = new NAlias("Rabbit Buck");
    private static final NAlias DOE_ALIAS = new NAlias("Rabbit Doe");
    private static final String BUNNY_NAME = "Bunny Rabbit";
    private static final NAlias BUNNY_ALIAS = new NAlias(BUNNY_NAME);

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (gui.getInventory().calcNumberFreeCoord(new Coord(2, 2)) == 0 || gui.getInventory().getFreeSpace() < 5) {
            return Results.ERROR("INVENTORY_FULL");
        }
        List<Hutch> breeders = collectHutches(gui, Specialisation.SpecName.rabbit);
        if (breeders.isEmpty())
            return Results.ERROR("NO_RABBIT_HUTCHES");
        List<Hutch> incubators = collectHutches(gui, Specialisation.SpecName.rabbitIncubator);
        if (incubators.isEmpty())
            return Results.ERROR("NO_RABBIT_INCUBATORS");

        ArrayList<Container> breedContainers = breeders.stream()
                .map(h -> h.container)
                .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Container> incubatorContainers = incubators.stream()
                .map(h -> h.container)
                .collect(Collectors.toCollection(ArrayList::new));
        new FillFluid(breedContainers, NContext.findSpec(Specialisation.SpecName.swill.toString()).getRCArea(),
                new NAlias("swill"), 32).run(gui);
        new FillFluid(incubatorContainers, NContext.findSpec(Specialisation.SpecName.swill.toString()).getRCArea(),
                new NAlias("swill"), 32).run(gui);
        new FillFluid(breedContainers, NContext.findSpec(Specialisation.SpecName.water.toString()).getRCArea(),
                new NAlias("water"), 4).run(gui);
        new FillFluid(incubatorContainers, NContext.findSpec(Specialisation.SpecName.water.toString()).getRCArea(),
                new NAlias("water"), 4).run(gui);

        redistributeDoes(gui, breeders, incubators);
        redistributeBucks(gui, breeders, incubators);

        killRemainingRabbits(gui, incubators);
        moveBunniesToIncubators(gui, breeders, incubators);
        redistributeBunnies(gui, breeders, incubators);
        cullBunnies(gui, breeders);
        return Results.SUCCESS();
    }

    private List<Hutch> collectHutches(NGameUI gui, Specialisation.SpecName spec) throws InterruptedException {
        return Finder.findGobs(NContext.findSpec(spec.toString()), HUTCH_RES).stream()
            .map(gob -> {
                try {
                    return buildHutch(gui, gob);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Failed to collect hutch: " + e.getMessage(), e);
                }
            })
            .collect(Collectors.toList());
    }

    private Hutch buildHutch(NGameUI gui, Gob gob) throws InterruptedException {
        Container container = new Container(gob, HUTCH_NAME);
        container.initattr(Container.Space.class);

        moveTo(gui, gob);
        openContainer(gui, gob);

        NInventory inv = gui.getInventory(HUTCH_NAME);
        Hutch hutch = new Hutch(container);

        hutch.bucks = extractRabbits(inv, BUCK_ALIAS, hutch);
        hutch.does = extractRabbits(inv, DOE_ALIAS, hutch);
        hutch.bunnies = extractRabbits(inv, BUNNY_ALIAS, hutch);

        closeContainer(gui, container);
        return hutch;
    }

    private List<Rabbit> extractRabbits(NInventory inv, NAlias alias, Hutch source) throws InterruptedException {
        return inv.getItems(alias).stream()
                .map(item -> new Rabbit(((NGItem) item.item).quality, source))
                .collect(Collectors.toList());
    }

    private void redistributeDoes(NGameUI gui, List<Hutch> breeders, List<Hutch> incubators) throws InterruptedException {
        List<Rabbit> doesToMove = incubators.stream()
                .flatMap(h -> h.does.stream())
                .sorted(Comparator.<Rabbit>comparingDouble(r -> r.quality).reversed())
                .collect(Collectors.toList());

        for (Hutch breeder : breeders) {
            breeder.does.sort(Comparator.comparingDouble(r -> r.quality));
            while (!doesToMove.isEmpty()
                    && !breeder.does.isEmpty()
                    && doesToMove.get(0).quality > breeder.does.get(0).quality) {

                Rabbit bestInc = doesToMove.remove(0);
                Rabbit worstBrd = breeder.does.remove(0);

                replaceDoe(gui, worstBrd, bestInc, breeder, bestInc.sourceHutch);

                bestInc.sourceHutch.does.remove(bestInc);
                bestInc.sourceHutch.does.add(worstBrd);
                bestInc.sourceHutch = breeder;
                breeder.does.add(bestInc);

                breeder.does.sort(Comparator.comparingDouble(r -> r.quality));
            }
        }
    }

    private void redistributeBucks(NGameUI gui, List<Hutch> breeders, List<Hutch> incubators) throws InterruptedException {
        List<Rabbit> bucksToMove = incubators.stream()
                .flatMap(h -> h.bucks.stream())
                .sorted(Comparator.<Rabbit>comparingDouble(r -> r.quality).reversed())
                .collect(Collectors.toList());

        for (Hutch breeder : breeders) {
            breeder.bucks.sort(Comparator.comparingDouble(r -> r.quality));

            while (!bucksToMove.isEmpty()
                    && !breeder.bucks.isEmpty()
                    && bucksToMove.get(0).quality > breeder.bucks.get(0).quality) {

                Rabbit bestInc = bucksToMove.remove(0);
                Rabbit worstBrd = breeder.bucks.remove(0);

                replaceBuck(gui, worstBrd, bestInc, breeder, bestInc.sourceHutch);

                bestInc.sourceHutch.bucks.remove(bestInc);
                bestInc.sourceHutch.bucks.add(worstBrd);
                bestInc.sourceHutch = breeder;
                breeder.bucks.add(bestInc);

                breeder.bucks.sort(Comparator.comparingDouble(r -> r.quality));
            }
        }
    }

    private void redistributeBunnies(NGameUI gui, List<Hutch> breeders, List<Hutch> incubators) throws InterruptedException {
        List<Rabbit> bunniesToMove = breeders.stream()
                .flatMap(h -> h.bunnies.stream())
                .sorted(Comparator.<Rabbit>comparingDouble(r -> r.quality).reversed())
                .collect(Collectors.toList());

        incubators.sort(Comparator.comparingInt(h -> h.bunnies.size()));
        for (Hutch incubator : incubators) {
            replaceBunnies(gui, incubator, bunniesToMove);
        }
    }

    private void replaceBunnies(NGameUI gui, Hutch incubator, List<Rabbit> pool) throws InterruptedException {
        incubator.bunnies.sort(Comparator.comparingDouble(r -> r.quality));

        while (!pool.isEmpty()
            && !incubator.bunnies.isEmpty()
            && pool.get(0).quality > incubator.bunnies.get(0).quality) {

            Rabbit best = pool.remove(0);
            Rabbit worst = incubator.bunnies.get(0);

            transferIndividualBunny(gui, worst, incubator, best.sourceHutch);
            transferIndividualBunny(gui, best, best.sourceHutch, incubator);

            best.sourceHutch = incubator;
            pool.add(worst);

            incubator.bunnies.sort(Comparator.comparingDouble(r -> r.quality));
            pool.sort(Comparator.<Rabbit>comparingDouble(r -> r.quality).reversed());
        }
    }

    private void killRemainingRabbits(NGameUI gui, List<Hutch> incubators) throws InterruptedException {
        FreeInventory2 freeInv = new FreeInventory2(new NContext(gui));

        for (Hutch h : incubators) {
            if (h.does.isEmpty() && h.bucks.isEmpty()) {
                continue;
            }

            moveTo(gui, Finder.findGob(h.container.gobid));
            openContainer(gui, h.container);

            final NInventory[] invHolder = new NInventory[1];
            invHolder[0] = gui.getInventory(HUTCH_NAME);

            while (true) {
                WItem wi = invHolder[0].getItem(DOE_ALIAS);
                if (wi == null) {
                    wi = invHolder[0].getItem(BUCK_ALIAS);
                }
                if (wi == null) {
                    break;
                }
                new SelectFlowerAction("Wring neck", wi).run(gui);
                NUtils.addTask(new WaitItems(invHolder[0], new NAlias("Dead Rabbit"), 1));

                if (gui.getInventory().getFreeSpace() < 2) {
                    freeInv.run(gui);
                    moveTo(gui, Finder.findGob(h.container.gobid));
                    openContainer(gui, h.container);
                    invHolder[0] = gui.getInventory(HUTCH_NAME);
                }
                wi = invHolder[0].getItem(new NAlias("Dead Rabbit"));
                int oldFurSize = gui.getInventory().getItems(new NAlias("Fresh Rabbit Fur")).size();
                new SelectFlowerAction("Flay", wi).run(gui);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        try {
                            return gui.getInventory().getItems(new NAlias("Fresh Rabbit Fur")).size() > oldFurSize;
                        } catch (InterruptedException e) {
                            return false;
                        }
                    }
                });

                if (gui.getInventory().getFreeSpace() < 1) {
                    freeInv.run(gui);
                    moveTo(gui, Finder.findGob(h.container.gobid));
                    openContainer(gui, h.container);
                    invHolder[0] = gui.getInventory(HUTCH_NAME);
                }
                wi = invHolder[0].getItem(new NAlias("Rabbit Carcass"));
                int oldEntrailsSize = gui.getInventory().getItems(new NAlias("Entrails")).size();
                new SelectFlowerAction("Clean", wi).run(gui);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        try {
                            return gui.getInventory().getItems(new NAlias("Entrails")).size() > oldEntrailsSize;
                        } catch (InterruptedException e) {
                            return false;
                        }
                    }
                });

                if (gui.getInventory().getFreeSpace() < 5) {
                    freeInv.run(gui);
                    moveTo(gui, Finder.findGob(h.container.gobid));
                    openContainer(gui, h.container);
                    invHolder[0] = gui.getInventory(HUTCH_NAME);
                }
                wi = invHolder[0].getItem(new NAlias("Clean Rabbit Carcass"));
                int oldMeatSize = gui.getInventory().getItems(new NAlias("Raw Rabbit")).size();
                int oldBoneSize = gui.getInventory().getItems(new NAlias("Bone Material")).size();
                new SelectFlowerAction("Butcher", wi).run(gui);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        try {
                            return gui.getInventory().getItems(new NAlias("Raw Rabbit")).size() > oldMeatSize
                                && gui.getInventory().getItems(new NAlias("Bone Material")).size() > oldBoneSize;
                        } catch (InterruptedException e) {
                            return false;
                        }
                    }
                });
            }

            closeContainer(gui, h.container);
        }
        freeInv.run(gui);
    }

    private void moveBunniesToIncubators(NGameUI gui, List<Hutch> breeders, List<Hutch> incubators) throws InterruptedException {
        List<Rabbit> pool = breeders.stream()
            .flatMap(h -> h.bunnies.stream())
            .sorted(Comparator.comparingDouble((Rabbit r) -> r.quality).reversed())
            .collect(Collectors.toList());

        for (Hutch inc : incubators) {
            int need = 42 - inc.bunnies.size();
            for (int i = 0; i < need && !pool.isEmpty(); i++) {
                Rabbit bunny = pool.remove(0);
                Hutch from = bunny.sourceHutch;

                transferIndividualBunny(gui, bunny, from, inc);
                bunny.sourceHutch = inc;
            }
        }
    }

    private void cullBunnies(NGameUI gui, List<Hutch> breeders) throws InterruptedException {
        for (Hutch h : breeders) {
            if (h.bunnies.size() == 0)
                continue;
            moveTo(gui, Finder.findGob(h.container.gobid));
            openContainer(gui, h.container);

            NInventory inventory = gui.getInventory(HUTCH_NAME);
            ArrayList<WItem> bunnies = gui.getInventory(HUTCH_NAME).getItems(BUNNY_ALIAS);
            for (WItem bunny : bunnies) {
                new SelectFlowerAction("Wring neck", bunny).run(gui);
                NUtils.addTask(new WaitItems(inventory, new NAlias("A Bloody Mess"), 1));
                WItem bloodyMess = (WItem) inventory.getItem(new NAlias("A Bloody Mess"));
                NUtils.drop(bloodyMess);
            }
        }
    }

    private void replaceDoe(NGameUI gui, Rabbit oldDoe, Rabbit newDoe, Hutch from, Hutch to) throws InterruptedException {
        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        takeRabbit(gui, DOE_ALIAS, oldDoe.quality);
        closeContainer(gui, from.container);

        moveTo(gui, Finder.findGob(to.container.gobid));
        openContainer(gui, to.container);
        replaceRabbit(gui, oldDoe, newDoe, DOE_ALIAS);
        closeContainer(gui, to.container);

        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        dropRabbit(gui, DOE_ALIAS);
        closeContainer(gui, from.container);
    }

    private void replaceBuck(NGameUI gui, Rabbit oldBuck, Rabbit newBuck, Hutch from, Hutch to) throws InterruptedException {
        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        takeRabbit(gui, BUCK_ALIAS, oldBuck.quality);
        closeContainer(gui, from.container);

        moveTo(gui, Finder.findGob(to.container.gobid));
        openContainer(gui, to.container);
        replaceRabbit(gui, oldBuck, newBuck, BUCK_ALIAS);
        closeContainer(gui, to.container);

        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        dropRabbit(gui, BUCK_ALIAS);
        closeContainer(gui, from.container);
    }

    private void transferIndividualBunny(NGameUI gui, Rabbit bunny, Hutch from, Hutch to) throws InterruptedException {
        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        takeRabbit(gui, BUNNY_ALIAS, bunny.quality);
        closeContainer(gui, from.container);

        moveTo(gui, Finder.findGob(to.container.gobid));
        openContainer(gui, to.container);
        dropRabbit(gui, BUNNY_ALIAS);
        closeContainer(gui, to.container);

        from.bunnies.remove(bunny);
        to.bunnies.add(bunny);
    }

    private void moveTo(NGameUI gui, Gob gob) throws InterruptedException {
        new PathFinder(gob).run(gui);
    }

    private void openContainer(NGameUI gui, Object target) throws InterruptedException {
        if (target instanceof Gob) {
            if (!new OpenTargetContainer(HUTCH_NAME, (Gob) target).run(gui).IsSuccess()) {
                throw new InterruptedException("Could not open container on gob");
            }
        } else if (target instanceof Container) {
            if (!new OpenTargetContainer((Container) target).run(gui).IsSuccess()) {
                throw new InterruptedException("Could not reopen container");
            }
        }
    }

    private void closeContainer(NGameUI gui, Container container) throws InterruptedException {
        new CloseTargetContainer(container).run(gui);
    }

    private void takeRabbit(NGameUI gui, NAlias alias, float quality) throws InterruptedException {
        int oldSize = gui.getInventory().getItems(alias).size();
        for (WItem w : gui.getInventory(HUTCH_NAME).getItems(alias)) {
            if (((NGItem) w.item).quality == quality) {
                w.item.wdgmsg("transfer", Coord.z);
                gui.ui.core.addTask(new WaitItems(gui.getInventory(), alias, oldSize + 1));
                return;
            }
        }
        throw new InterruptedException("Desired Doe with quality " + quality + " not found");
    }

    private void dropRabbit(NGameUI gui, NAlias alias) throws InterruptedException {
        ArrayList<WItem> rabbits = gui.getInventory().getItems(alias);
        int oldSize = rabbits.size();
        rabbits.get(0).item.wdgmsg("transfer", Coord.z);
        gui.ui.core.addTask(new WaitItems(gui.getInventory(), alias, oldSize - 1));
    }

    private void replaceRabbit(NGameUI gui, Rabbit oldRabbit, Rabbit newRabbit, NAlias alias) throws InterruptedException {
        WItem oldRabbitItem = null;
        for (WItem w : gui.getInventory().getItems(alias)) {
            if (((NGItem) w.item).quality == oldRabbit.quality) {
                oldRabbitItem = w;
                break;
            }
        }

        WItem newRabbitItem = null;
        for (WItem w : gui.getInventory(HUTCH_NAME).getItems(alias)) {
            if (((NGItem) w.item).quality == newRabbit.quality) {
                newRabbitItem = w;
                break;
            }
        }
        Coord oldRabbitPos = oldRabbitItem.c.div(Inventory.sqsz);
        Coord newRabbitPos = newRabbitItem.c.div(Inventory.sqsz);
        NUtils.takeItemToHand(oldRabbitItem);
        gui.getInventory(HUTCH_NAME).dropOn(newRabbitPos, alias);
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return gui.vhand != null && ((NGItem)gui.vhand.item).quality == newRabbit.quality;
            }
        });
        gui.getInventory().dropOn(oldRabbitPos, alias);
    }

    class Rabbit {
        final float quality;
        Hutch sourceHutch;

        Rabbit(float quality, Hutch sourceHutch) {
            this.quality = quality;
            this.sourceHutch = sourceHutch;
        }
    }

    class Hutch {
        final Container container;
        List<Rabbit> bucks = new ArrayList<>();
        List<Rabbit> does = new ArrayList<>();
        List<Rabbit> bunnies = new ArrayList<>();

        Hutch(Container container) {
            this.container = container;
        }
    }
}
