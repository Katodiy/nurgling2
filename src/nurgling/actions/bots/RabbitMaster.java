package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;
import nurgling.tasks.NTask;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        ArrayList<Container> containers = Stream.concat(
                breeders.stream().map(h -> h.container),
                incubators.stream().map(h -> h.container)
            )
            .collect(Collectors.toCollection(ArrayList::new));

        new FillFluid(containers, NContext.findSpec(Specialisation.SpecName.swill.toString()).getRCArea(), new NAlias("swill"), 32).run(gui);
        new FillFluid(containers, NContext.findSpec(Specialisation.SpecName.water.toString()).getRCArea(), new NAlias("water"), 4).run(gui);

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
        Container container = new Container(gob, HUTCH_NAME, null);
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

        List<Rabbit> doesToReplace = breeders.stream()
                .flatMap(h -> h.does.stream())
                .sorted(Comparator.<Rabbit>comparingDouble(r -> r.quality))
                .collect(Collectors.toList());

        while (!doesToMove.isEmpty()
                && !doesToReplace.isEmpty()
                && doesToMove.getFirst().quality > doesToReplace.getFirst().quality) {

            Rabbit bestInc = doesToMove.removeFirst();
            Rabbit worstBrd = doesToReplace.removeFirst();
            Hutch incubator = bestInc.sourceHutch;

            replaceDoe(gui, worstBrd, bestInc, worstBrd.sourceHutch, incubator);
            incubator.does.remove(bestInc);
            incubator.does.add(worstBrd);
            bestInc.sourceHutch = worstBrd.sourceHutch;

            worstBrd.sourceHutch.does.remove(worstBrd);
            worstBrd.sourceHutch.does.add(bestInc);
            worstBrd.sourceHutch = incubator;
        }
    }

    private void redistributeBucks(NGameUI gui, List<Hutch> breeders, List<Hutch> incubators) throws InterruptedException {
        List<Rabbit> bucksToMove = incubators.stream()
                .flatMap(h -> h.bucks.stream())
                .sorted(Comparator.<Rabbit>comparingDouble(r -> r.quality).reversed())
                .collect(Collectors.toList());

        List<Rabbit> bucksToReplace = breeders.stream()
                .flatMap(h -> h.bucks.stream())
                .sorted(Comparator.<Rabbit>comparingDouble(r -> r.quality))
                .collect(Collectors.toList());

        while (!bucksToMove.isEmpty()
                && !bucksToReplace.isEmpty()
                && bucksToMove.getFirst().quality > bucksToReplace.getFirst().quality) {

            Rabbit bestInc = bucksToMove.removeFirst();
            Rabbit worstBrd = bucksToReplace.removeFirst();
            Hutch incubator = bestInc.sourceHutch;

            replaceBuck(gui, worstBrd, bestInc, worstBrd.sourceHutch, incubator);
            incubator.bucks.remove(bestInc);
            incubator.bucks.add(worstBrd);
            bestInc.sourceHutch = worstBrd.sourceHutch;

            worstBrd.sourceHutch.bucks.remove(worstBrd);
            worstBrd.sourceHutch.bucks.add(bestInc);
            worstBrd.sourceHutch = incubator;
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
        NContext context = new NContext(gui);
        FreeInventory2 freeInv = new FreeInventory2(context);
        context.getSpecArea(Specialisation.SpecName.rabbit);

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
                    context.getSpecArea(Specialisation.SpecName.rabbit);
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
                    context.getSpecArea(Specialisation.SpecName.rabbit);
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
                    context.getSpecArea(Specialisation.SpecName.rabbit);
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
        context.getSpecArea(Specialisation.SpecName.rabbit);
    }

    private void moveBunniesToIncubators(NGameUI gui, List<Hutch> breeders, List<Hutch> incubators) throws InterruptedException {
        int breederIndex = 0;

        int totalBunnies = Stream.concat(breeders.stream(), incubators.stream())
                .mapToInt(h -> h.bunnies.size())
                .sum();
        int calcNeed = (totalBunnies + incubators.size() - 1) / incubators.size();


        for (Hutch incubator : incubators) {
            int need = Math.min(42, calcNeed) - incubator.bunnies.size();
            if (need <= 0)
                continue;

            while (need > 0 && breederIndex < breeders.size()) {
                Hutch breeder = breeders.get(breederIndex);

                if (breeder.bunnies.isEmpty()) {
                    breederIndex++;
                    continue;
                }
                int moveCount = Math.min(need, Math.min(breeder.bunnies.size(), gui.getInventory().getFreeSpace()));

                transferBunniesBatch(gui, breeder, incubator, moveCount);

                for (int i = 0; i < moveCount; i++) {
                    Rabbit r = breeder.bunnies.remove(0);
                    r.sourceHutch = incubator;
                    incubator.bunnies.add(r);
                }

                need -= moveCount;

                // If breeder is empty, advance to next one
                if (breeder.bunnies.isEmpty()) {
                    breederIndex++;
                }
            }
        }
    }


    private void transferBunniesBatch(NGameUI gui, Hutch from, Hutch to, int count) throws InterruptedException {
        if (count <= 0) return;

        /* TAKE */
        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);

        for (int i = 0; i < count; i++) {
            takeAnyBunny(gui);
        }

        closeContainer(gui, from.container);

        /* DROP */
        moveTo(gui, Finder.findGob(to.container.gobid));
        openContainer(gui, to.container);

        for (int i = 0; i < count; i++) {
            dropBunny(gui);
        }

        closeContainer(gui, to.container);
    }

    private void takeAnyBunny(NGameUI gui) throws InterruptedException {
        WItem bunny = gui.getInventory(HUTCH_NAME).getItem(BUNNY_ALIAS);
        if (bunny == null)
            throw new InterruptedException("No bunny found");

        int oldSize = gui.getInventory().getItems(BUNNY_ALIAS).size();
        bunny.item.wdgmsg("transfer", Coord.z);
        NUtils.addTask(new WaitItems(gui.getInventory(), BUNNY_ALIAS, oldSize + 1));
    }

    private void dropBunny(NGameUI gui) throws InterruptedException {
        WItem bunny = gui.getInventory().getItem(BUNNY_ALIAS);
        if (bunny == null)
            throw new InterruptedException("No bunny found");

        int oldSize = gui.getInventory(HUTCH_NAME).getItems(BUNNY_ALIAS).size();
        bunny.item.wdgmsg("transfer", Coord.z);
        NUtils.addTask(new WaitItems(gui.getInventory(HUTCH_NAME), BUNNY_ALIAS, oldSize + 1));
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
            closeContainer(gui, h.container);
        }
    }

    private void replaceDoe(NGameUI gui, Rabbit oldDoe, Rabbit newDoe, Hutch from, Hutch to) throws InterruptedException {
        replaceRabbit(gui, oldDoe, newDoe, from, to, DOE_ALIAS);
    }

    private void replaceBuck(NGameUI gui, Rabbit oldBuck, Rabbit newBuck, Hutch from, Hutch to) throws InterruptedException {
        replaceRabbit(gui, oldBuck, newBuck, from, to, BUCK_ALIAS);
    }

    private void replaceRabbit(NGameUI gui, Rabbit oldRabbit, Rabbit newRabbit, Hutch from, Hutch to, NAlias alias) throws InterruptedException {
        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        takeRabbit(gui, alias, oldRabbit.quality);
        closeContainer(gui, from.container);

        moveTo(gui, Finder.findGob(to.container.gobid));
        openContainer(gui, to.container);
        swapRabbits(gui, oldRabbit, newRabbit, alias);
        closeContainer(gui, to.container);

        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        dropRabbit(gui, alias);
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

    private void swapRabbits(NGameUI gui, Rabbit oldRabbit, Rabbit newRabbit, NAlias alias) throws InterruptedException {
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
