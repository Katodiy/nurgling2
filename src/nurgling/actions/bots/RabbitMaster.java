package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Context;
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
        if (gui.getInventory().calcNumberFreeCoord(new Coord(2, 2)) == 0) {
            return Results.ERROR("INVENTORY_FULL");
        }
        List<Hutch> breeders = collectHutches(gui, Specialisation.SpecName.rabbit);
        if (breeders.isEmpty())
            return Results.ERROR("NO_RABBIT_HUTCHES");
        List<Hutch> incubators = collectHutches(gui, Specialisation.SpecName.rabbitIncubator);
        if (incubators.isEmpty())
            return Results.ERROR("NO_RABBIT_INCUBATORS");

        ArrayList<Container> breedContainers = breeders.stream()
                .filter(h -> h.bunnies.size() > 0)
                .map(h -> h.container)
                .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Container> incubatorContainers = incubators.stream()
                .filter(h -> h.bunnies.size() > 0)
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
        moveBunniesToIncubators(gui, breedContainers, incubators);
        cullBunnies(gui, incubators);
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
        Hutch hutch = new Hutch(container, inv.getFreeSpace());

        hutch.bucks = extractRabbits(inv, BUCK_ALIAS, hutch);
        hutch.does = extractRabbits(inv, DOE_ALIAS, hutch);
        hutch.bunnies = extractRabbits(inv, BUNNY_ALIAS, hutch);

        closeContainer(gui, container);
        return hutch;
    }

    private List<Rabbit> extractRabbits(NInventory inv, NAlias alias, Hutch source) throws InterruptedException {
        return inv.getItems(alias).stream()
                .map(item -> new Rabbit(item, ((NGItem) item.item).quality, source))
                .collect(Collectors.toList());
    }

    private void redistributeDoes(NGameUI gui, List<Hutch> breeders, List<Hutch> incubators) throws InterruptedException {
        List<Rabbit> doesToMove = incubators.stream()
                .flatMap(h -> h.does.stream())
                .sorted(Comparator.<Rabbit>comparingDouble(r -> r.quality).reversed())
                .collect(Collectors.toList());

        breeders.sort(Comparator.comparingInt(h -> h.does.size()));
        for (Hutch breeder : breeders) {
            fillAndReplaceDoes(gui, breeder, doesToMove, 11);
        }
    }

    private void redistributeBucks(NGameUI gui, List<Hutch> breeders, List<Hutch> incubators) throws InterruptedException {
        List<Rabbit> bucksToMove = incubators.stream()
                .flatMap(h -> h.bucks.stream())
                .sorted(Comparator.<Rabbit>comparingDouble(r -> r.quality).reversed())
                .collect(Collectors.toList());

        breeders.sort(Comparator.comparingInt(h -> h.bucks.size()));
        for (Hutch breeder : breeders) {
            fillAndReplaceBucks(gui, breeder, bucksToMove, 1);
        }
    }

    private void fillAndReplaceDoes(NGameUI gui, Hutch breeder, List<Rabbit> pool, int targetSize) throws InterruptedException {
        while (breeder.does.size() < targetSize && !pool.isEmpty()) {
            Rabbit next = pool.remove(0);
            transferIndividualDoe(gui, next, next.sourceHutch, breeder);
            breeder.does.add(next);
        }

        breeder.does.sort(Comparator.comparingDouble(r -> r.quality));
        while (!pool.isEmpty()
                && !breeder.does.isEmpty()
                && pool.get(0).quality > breeder.does.get(0).quality) {
            Rabbit bestInc = pool.remove(0);
            Rabbit worstBrd = breeder.does.remove(0);

            transferIndividualDoe(gui, worstBrd, breeder, bestInc.sourceHutch);
            transferIndividualDoe(gui, bestInc, bestInc.sourceHutch, breeder);

            bestInc.sourceHutch.does.remove(bestInc);
            breeder.does.add(bestInc);
        }
    }

    private void fillAndReplaceBucks(NGameUI gui, Hutch breeder, List<Rabbit> pool, int targetSize) throws InterruptedException {
        while (breeder.bucks.size() < targetSize && !pool.isEmpty()) {
            Rabbit next = pool.remove(0);
            transferIndividualBuck(gui, next, next.sourceHutch, breeder);
            breeder.bucks.add(next);
        }
        breeder.bucks.sort(Comparator.comparingDouble(r -> r.quality));
        while (!pool.isEmpty()
                && !breeder.bucks.isEmpty()
                && pool.get(0).quality > breeder.bucks.get(0).quality) {
            Rabbit bestInc = pool.remove(0);
            Rabbit worstBrd = breeder.bucks.remove(0);

            transferIndividualBuck(gui, worstBrd, breeder, bestInc.sourceHutch);
            transferIndividualBuck(gui, bestInc, bestInc.sourceHutch, breeder);

            bestInc.sourceHutch.bucks.remove(bestInc);
            breeder.bucks.add(bestInc);
        }
    }

    private void killRemainingRabbits(NGameUI gui, List<Hutch> incubators) throws InterruptedException {
        for (Hutch h : incubators) {
            if (h.does.isEmpty() && h.bucks.isEmpty())
                continue;
            moveTo(gui, Finder.findGob(h.container.gobid));
            openContainer(gui, h.container);

            NInventory inv = gui.getInventory(HUTCH_NAME);
            List<WItem> items = new ArrayList<WItem>();
            items.addAll(inv.getItems(DOE_ALIAS));
            items.addAll(inv.getItems(BUCK_ALIAS));
            for (WItem wi : items) {
                new SelectFlowerAction("Wring neck", wi).run(gui);
                NUtils.addTask(new WaitItems((NInventory) inv, new NAlias("Dead Rabbit"), 1));

                wi = (WItem) inv.getItem(new NAlias("Dead Rabbit"));
                new SelectFlowerAction("Flay", wi).run(gui);
                NUtils.addTask(new WaitItems((NInventory) inv, new NAlias("Rabbit Carcass"), 1));

                wi = (WItem) inv.getItem(new NAlias("Rabbit Carcass"));
                new SelectFlowerAction("Clean", wi).run(gui);
                NUtils.addTask(new WaitItems((NInventory) inv, new NAlias("Clean Rabbit Carcass"), 1));

                wi = (WItem) inv.getItem(new NAlias("Clean Rabbit Carcass"));
                new SelectFlowerAction("Butcher", wi).run(gui);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        try {
                            return inv.getItems(new NAlias("Clean Rabbit Carcass")).isEmpty();
                        } catch (InterruptedException e) {
                            return false;
                        }
                    }
                });
            }

            closeContainer(gui, h.container);
        }
        new FreeInventory(new Context()).run(gui);
    }

    private void moveBunniesToIncubators(NGameUI gui, ArrayList<Container> breedContainers, List<Hutch> incubators) throws InterruptedException {
        Context context = new Context();
        ArrayList<Context.Output> outputs = new ArrayList<>();
        for (Hutch inc : incubators) {
            Context.OutputContainer container = new Context.OutputContainer(Finder.findGob(inc.container.gobid),
                    NContext.findSpec(Specialisation.SpecName.rabbitIncubator.toString()).getRCArea(), 1);
            container.cap = HUTCH_NAME;
            container.initattr(Container.Space.class);
            container.initattr(Container.TargetItems.class);
            container.getattr(Container.TargetItems.class).setMaxNum(63);
            outputs.add(container);
        }

        context.addOutput(BUNNY_NAME, outputs);
        HashSet<String> bunnies = new HashSet<>();
        bunnies.add(BUNNY_NAME);
        new TransferTargetItemsFromContainers(context, breedContainers, bunnies, new NAlias()).run(gui);
    }

    private void cullBunnies(NGameUI gui, List<Hutch> incubators) throws InterruptedException {
        for (Hutch h : incubators) {
            if (h.bunnies.size() <= 42)
                continue;

            h.bunnies.sort(Comparator.comparingDouble(r -> r.quality));
            int toKill = h.bunnies.size() - 42;
            moveTo(gui, Finder.findGob(h.container.gobid));
            openContainer(gui, h.container);

            for (int i = 0; i < toKill; i++) {
                Rabbit bunny = h.bunnies.remove(0);
                takeRabbit(gui, BUNNY_ALIAS, bunny.quality);
                NInventory pinv = gui.getInventory();
                WItem wi = pinv.getItem(BUNNY_ALIAS);
                new SelectFlowerAction("Wring neck", wi).run(gui);
                NUtils.addTask(new WaitItems(pinv, new NAlias("A Bloody Mess"), 1));
                WItem bloodyMess = (WItem) pinv.getItem(new NAlias("A Bloody Mess"));
                NUtils.drop(bloodyMess);
            }
        }
        new FreeInventory(new Context()).run(gui);
    }

    private void transferIndividualDoe(NGameUI gui, Rabbit doe, Hutch from, Hutch to) throws InterruptedException {
        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        takeRabbit(gui, DOE_ALIAS, doe.quality);
        closeContainer(gui, from.container);

        moveTo(gui, Finder.findGob(to.container.gobid));
        openContainer(gui, to.container);
        dropRabbit(gui, DOE_ALIAS);
        closeContainer(gui, to.container);

        from.does.remove(doe);
        to.does.add(doe);
    }

    private void transferIndividualBuck(NGameUI gui, Rabbit buck, Hutch from, Hutch to) throws InterruptedException {
        moveTo(gui, Finder.findGob(from.container.gobid));
        openContainer(gui, from.container);
        takeRabbit(gui, BUCK_ALIAS, buck.quality);
        closeContainer(gui, from.container);

        moveTo(gui, Finder.findGob(to.container.gobid));
        openContainer(gui, to.container);
        dropRabbit(gui, BUCK_ALIAS);
        closeContainer(gui, to.container);

        from.bucks.remove(buck);
        to.bucks.add(buck);
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
        WItem item = gui.getInventory().getItem(alias);
        item.item.wdgmsg("transfer", Coord.z);
    }

    class Rabbit {
        final WItem widget;
        final float quality;
        final Hutch sourceHutch;

        Rabbit(WItem widget, float quality, Hutch sourceHutch) {
            this.widget = widget;
            this.quality = quality;
            this.sourceHutch = sourceHutch;
        }
    }

    class Hutch {
        final Container container;
        int freeSpace;
        List<Rabbit> bucks = new ArrayList<>();
        List<Rabbit> does = new ArrayList<>();
        List<Rabbit> bunnies = new ArrayList<>();

        Hutch(Container container, int freeSpace) {
            this.container = container;
            this.freeSpace = freeSpace;
        }
    }
}
