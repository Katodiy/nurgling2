package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.*;

public class RabbitMaster implements Action {
    private final String RABBIT_HUTCH_NAME = "Rabbit Hutch";
    private final String RABBIT_HUTCH_RESOURCE_NAME = "gfx/terobjs/rabbithutch";
    private final String BUCK_NAME = "Rabbit Buck";
    private final String DOE_NAME = "Rabbit Doe";
    private final String BUNNY_NAME = "Bunny";

    // Info about rabbit hutch
    private static class HutchInfo {
        Container container; // Rabbit hutch container
        double buckQuality; // Buck quality
        ArrayList<Float> doeQualities = new ArrayList<>(); // Doe quality list

        public HutchInfo(Container container, double buckQuality) {
            this.container = container;
            this.buckQuality = buckQuality;
        }
    }

    // Info about incubator
    public static class IncubatorInfo {
        Container container; // Incubator container
        double rabbitQuality; // Rabbit quality

        public IncubatorInfo(Container container, double rabbitQuality) {
            this.container = container;
            this.rabbitQuality = rabbitQuality;
        }
    }

    // Comparator for sorting incubators by quality
    Comparator<RabbitMaster.IncubatorInfo> incubatorComparator = new Comparator<RabbitMaster.IncubatorInfo>() {
        @Override
        public int compare(RabbitMaster.IncubatorInfo o1, RabbitMaster.IncubatorInfo o2) {
            return Double.compare(o1.rabbitQuality, o2.rabbitQuality);
        }
    };

    // Comparator for sorting rabbit hutches.
    Comparator<RabbitMaster.HutchInfo> hutchComparator = new Comparator<RabbitMaster.HutchInfo>() {
        @Override
        public int compare(RabbitMaster.HutchInfo o1, RabbitMaster.HutchInfo o2) {
            int res = Double.compare(o1.buckQuality, o2.buckQuality);
            if (res == 0) {
                if (!o1.doeQualities.isEmpty() && !o2.doeQualities.isEmpty()) {
                    double avgQuality1 = o1.doeQualities.stream().mapToDouble(Float::doubleValue).average().orElse(0);
                    double avgQuality2 = o2.doeQualities.stream().mapToDouble(Float::doubleValue).average().orElse(0);
                    res = Double.compare(avgQuality1, avgQuality2);
                }
            }
            return res;
        }
    };

    Context context = new Context();

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Container> rabbitHutchesBreeding = new ArrayList<>();

        for (Gob ttube : Finder.findGobs(NArea.findSpec(Specialisation.SpecName.rabbit.toString()),
                new NAlias(RABBIT_HUTCH_RESOURCE_NAME))) {
            Container cand = new Container();
            cand.gob = ttube;
            cand.cap = RABBIT_HUTCH_NAME;

            cand.initattr(Container.Space.class);

            rabbitHutchesBreeding.add(cand);
        }

        ArrayList<Container> rabbitHutchesIncubators = new ArrayList<>();

        for (Gob ttube : Finder.findGobs(NArea.findSpec(Specialisation.SpecName.rabbitIncubator.toString()),
                new NAlias(RABBIT_HUTCH_RESOURCE_NAME))) {
            Container cand = new Container();
            cand.gob = ttube;
            cand.cap = RABBIT_HUTCH_NAME;

            cand.initattr(Container.Space.class);

            rabbitHutchesIncubators.add(cand);
        }

        // Fill incubators and hutches with liquids
        new FillFluid(rabbitHutchesBreeding, NArea.findSpec(Specialisation.SpecName.swill.toString()).getRCArea(), new NAlias("swill"), 32).run(gui);
        new FillFluid(rabbitHutchesIncubators, NArea.findSpec(Specialisation.SpecName.swill.toString()).getRCArea(), new NAlias("swill"), 32).run(gui);
        new FillFluid(rabbitHutchesBreeding, NArea.findSpec(Specialisation.SpecName.water.toString()).getRCArea(), new NAlias("water"), 4).run(gui);
        new FillFluid(rabbitHutchesIncubators, NArea.findSpec(Specialisation.SpecName.water.toString()).getRCArea(), new NAlias("water"), 4).run(gui);

        // Read contents of rabbit hutches and save them.
        ArrayList<RabbitMaster.HutchInfo> hutchInfos = new ArrayList<>();
        ArrayList<RabbitMaster.IncubatorInfo> qBucks = new ArrayList<>();
        ArrayList<RabbitMaster.IncubatorInfo> qDoes = new ArrayList<>();
        for (Container container : rabbitHutchesBreeding) {
            new PathFinder( container.gob).run(gui);
            if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME,container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            double buckQuality;
            if(gui.getInventory(RABBIT_HUTCH_NAME).getItem(new NAlias(BUCK_NAME))!=null) {
                // Get info about the buck
                NGItem roost = (NGItem) gui.getInventory(RABBIT_HUTCH_NAME).getItem(new NAlias(BUCK_NAME)).item;
                buckQuality = roost.quality;
            }
            else
            {
                buckQuality = -1;
            }


            // Create hutch info object for current hutch.
            RabbitMaster.HutchInfo hutchInfo = new HutchInfo(container, buckQuality);

            // Get info about does.
            ArrayList<WItem> does = gui.getInventory(RABBIT_HUTCH_NAME).getItems(new NAlias(DOE_NAME));
            for (WItem doeItem : does) {
                hutchInfo.doeQualities.add(((NGItem)doeItem.item).quality);
            }

            hutchInfo.doeQualities.sort(Float::compareTo);

            // Add hutch to the list
            hutchInfos.add(hutchInfo);

            new CloseTargetContainer(container).run(gui);
        }

        // Sort hutches by quality of bucks and average quality of does.
        hutchInfos.sort(hutchComparator.reversed());

        for (Container container : rabbitHutchesIncubators) {
            new PathFinder(container.gob).run(gui);
            if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            ArrayList<WItem> bucks = gui.getInventory(RABBIT_HUTCH_NAME).getItems(new NAlias(BUCK_NAME));
            for (WItem buck : bucks) {
                qBucks.add(new IncubatorInfo(container, ((NGItem) buck.item).quality));
            }

            ArrayList<WItem> does = gui.getInventory(RABBIT_HUTCH_NAME).getItems(new NAlias(DOE_NAME));
            for (WItem doe : does) {
                qDoes.add(new IncubatorInfo(container, ((NGItem) doe.item).quality));
            }

            new CloseTargetContainer(container).run(gui);
        }

        Results bucksResult = processBucks(gui, hutchInfos, qBucks);
        if (!bucksResult.IsSuccess()) {
            return bucksResult;
        }

        Results doesResult = processDoes(gui, hutchInfos, qDoes);
        if (!doesResult.IsSuccess()) {
            return doesResult;
        }

        cleanupBunnies(gui, rabbitHutchesIncubators);

        ArrayList<Context.Output> outputs = new ArrayList<>();
        for (Container cc : rabbitHutchesIncubators) {
            Context.OutputContainer container = new Context.OutputContainer(cc.gob, NArea.findSpec(Specialisation.SpecName.incubator.toString()).getRCArea(), 1);
            container.cap = RABBIT_HUTCH_NAME;
            container.initattr(Container.Space.class);
            outputs.add(container);
        }

        context.addOutput(BUNNY_NAME,outputs);
        HashSet<String> bunnies = new HashSet<>();
        bunnies.add(BUNNY_NAME);
        new TransferTargetItemsFromContainers(context, rabbitHutchesBreeding, bunnies, new NAlias(new ArrayList<>(), new ArrayList<>(List.of("Hide", "Entrails", "Meat", "Bone")))).run(gui);

        cleanupBunnies(gui, rabbitHutchesIncubators);

        return Results.SUCCESS();
    }


    private Results processBucks(NGameUI gui, ArrayList<HutchInfo> hutchInfos, ArrayList<IncubatorInfo> qBucks) throws InterruptedException {
        // Sort bucks by quality (from best to worst)
        qBucks.sort(incubatorComparator.reversed());

        for (IncubatorInfo buckInfo : qBucks) {
            // Open hutch with buck
            new PathFinder(buckInfo.container.gob).run(gui);
            if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, buckInfo.container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Get buck from inventory
            WItem buck = (WItem) gui.getInventory(RABBIT_HUTCH_NAME).getItem(new NAlias(BUCK_NAME));
            if (buck == null) {
                return Results.ERROR("NO_BUCK");
            }
            double buckQuality = ((NGItem)buck.item).quality;

            Coord pos = buck.c.div(Inventory.sqsz);
            buck.item.wdgmsg("transfer", Coord.z);
            Coord finalPos1 = pos;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.getInventory(RABBIT_HUTCH_NAME).isSlotFree(finalPos1);
                }
            });

            // Find hutch with worst buck and replace it
            for (HutchInfo hutchInfo : hutchInfos) {
                if (hutchInfo.buckQuality < buckQuality && hutchInfo.buckQuality!=-1) {


                    buck = (WItem) gui.getInventory().getItem(new NAlias(BUCK_NAME));

                    // Open hutch for replacement
                    new PathFinder(hutchInfo.container.gob).run(gui);
                    if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, hutchInfo.container.gob).run(gui).IsSuccess())) {
                        return Results.FAIL();
                    }

                    // Get current buck in the hutch
                    WItem oldBuck = gui.getInventory(RABBIT_HUTCH_NAME).getItem(new NAlias(BUCK_NAME));
                    if (oldBuck == null) {
                        return Results.ERROR("NO_BUCK_IN_COOP");
                    }

                    // Swap buck
                    pos = oldBuck.c.div(Inventory.sqsz);
                    oldBuck.item.wdgmsg("transfer", Coord.z);
                    Coord finalPos = pos;
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return gui.getInventory(RABBIT_HUTCH_NAME).isSlotFree(finalPos);
                        }
                    });

                    NUtils.takeItemToHand(buck);
                    gui.getInventory(RABBIT_HUTCH_NAME).dropOn(pos,BUCK_NAME);

                    // Update quality of buck in the hutch
                    hutchInfo.buckQuality = buckQuality;
                    buckQuality = ((NGItem)oldBuck.item).quality;
                    // Update quality for the next swap
                    new CloseTargetContainer(hutchInfo.container).run(gui);
                }
            }

            // Kill and process bucks
            buck = (WItem) gui.getInventory().getItem(new NAlias(BUCK_NAME));
            new SelectFlowerAction( "Wring neck", buck).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Dead Rabbit"), 1));

            buck = (WItem) gui.getInventory().getItem(new NAlias("Dead Rabbit"));
            new SelectFlowerAction( "Flay", buck).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Rabbit Carcass"), 1));

            buck = (WItem) gui.getInventory().getItem(new NAlias("Rabbit Carcass"));
            new SelectFlowerAction( "Clean", buck).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Clean Rabbit Carcass"), 1));

            buck = (WItem) gui.getInventory().getItem(new NAlias("Clean Rabbit Carcass"));
            new SelectFlowerAction( "Butcher", buck).run(gui);
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    try {
                        return gui.getInventory().getItems(new NAlias("Clean Rabbit Carcass")).isEmpty();
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            });

            new FreeInventory(context).run(gui);
        }
        new FreeInventory(context).run(gui);
        return Results.SUCCESS();
    }

    private Results processDoes(NGameUI gui, ArrayList<RabbitMaster.HutchInfo> hutchInfos, ArrayList<RabbitMaster.IncubatorInfo> qDoes) throws InterruptedException {
        // Sort does by quality from best to worst
        qDoes.sort(incubatorComparator.reversed());

        for (RabbitMaster.IncubatorInfo doeInfo : qDoes) {
            // Open hutch with doe
            new PathFinder(doeInfo.container.gob).run(gui);
            if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, doeInfo.container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Get doe from inventory
            WItem doe = (WItem) gui.getInventory(RABBIT_HUTCH_NAME).getItem(new NAlias(DOE_NAME));
            if (doe == null) {
                return Results.ERROR("NO_DOE");
            }
            float doeQuality = ((NGItem) doe.item).quality;

            Coord pos = doe.c.div(Inventory.sqsz);
            doe.item.wdgmsg("transfer", Coord.z);
            Coord finalPos1 = pos;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.getInventory(RABBIT_HUTCH_NAME).isSlotFree(finalPos1);
                }
            });

            for (HutchInfo hutchInfo : hutchInfos) {
                for (int i = 0; i < hutchInfo.doeQualities.size(); i++) {
                    if (hutchInfo.doeQualities.get(i) < doeQuality) {
                        new PathFinder(hutchInfo.container.gob).run(gui);
                        if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, hutchInfo.container.gob).run(gui).IsSuccess())) {
                            return Results.FAIL();
                        }

                        doe = (WItem) gui.getInventory().getItem(new NAlias(DOE_NAME));

                        WItem oldDoe = gui.getInventory(RABBIT_HUTCH_NAME).getItem(new NAlias(DOE_NAME), hutchInfo.doeQualities.get(i));
                        if (oldDoe == null) {
                            return Results.ERROR("NO_DOE_IN_COOP");
                        }

                        // Swap doe
                        pos = oldDoe.c.div(Inventory.sqsz);
                        oldDoe.item.wdgmsg("transfer", Coord.z);
                        Coord finalPos = pos;
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return gui.getInventory(RABBIT_HUTCH_NAME).isSlotFree(finalPos);
                            }
                        });

                        NUtils.takeItemToHand(doe);
                        gui.getInventory(RABBIT_HUTCH_NAME).dropOn(pos, DOE_NAME);

                        hutchInfo.doeQualities.set(i, doeQuality);
                        doeQuality = ((NGItem) oldDoe.item).quality;
                        new CloseTargetContainer(hutchInfo.container).run(gui);
                        break;
                    }
                }
            }

            // Kill and process does
            doe = (WItem) gui.getInventory().getItem(new NAlias(DOE_NAME));
            new SelectFlowerAction("Wring neck", doe).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Dead Rabbit"), 1));

            doe = (WItem) gui.getInventory().getItem(new NAlias("Dead Rabbit"));
            new SelectFlowerAction("Flay", doe).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Rabbit Carcass"), 1));

            doe = (WItem) gui.getInventory().getItem(new NAlias("Rabbit Carcass"));
            new SelectFlowerAction("Clean", doe).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Clean Rabbit Carcass"), 1));

            doe = (WItem) gui.getInventory().getItem(new NAlias("Clean Rabbit Carcass"));
            new SelectFlowerAction("Butcher", doe).run(gui);
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    try {
                        return gui.getInventory().getItems(new NAlias("Clean Rabbit Carcass")).isEmpty();
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            });

            new FreeInventory(context).run(gui);
        }
        new FreeInventory(context).run(gui);
        return Results.SUCCESS();
    }

    private Results cleanupBunnies(NGameUI gui, ArrayList<Container> rabbitHutchesIncubators) throws InterruptedException {
        ArrayList<RabbitMaster.IncubatorInfo> qBunnies = new ArrayList<>();

        int totalNumberOfPossibleBunnies = 0;

        for (Container container : rabbitHutchesIncubators) {
            new PathFinder(container.gob).run(gui);
            if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            ArrayList<WItem> bunnies = gui.getInventory(RABBIT_HUTCH_NAME).getItems(new NAlias(BUNNY_NAME));
            Coord hutchSize = gui.getInventory(RABBIT_HUTCH_NAME).isz;
            totalNumberOfPossibleBunnies = hutchSize.x * hutchSize.y;

            for (WItem buck : bunnies) {
                qBunnies.add(new RabbitMaster.IncubatorInfo(container, ((NGItem) buck.item).quality));
            }

            new CloseTargetContainer(container).run(gui);
        }

        double averageBunnyQuality = qBunnies.stream()
                .mapToDouble(b -> b.rabbitQuality)
                .average()
                .orElse(0.0);

        for (Container container : rabbitHutchesIncubators) {
            new PathFinder(container.gob).run(gui);
            if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            while(gui.getInventory(RABBIT_HUTCH_NAME).getItems(BUNNY_NAME).size() > totalNumberOfPossibleBunnies/2) {
                ArrayList<WItem> bunnies = gui.getInventory(RABBIT_HUTCH_NAME).getItems(new NAlias(BUNNY_NAME), NInventory.QualityType.Low);
                float worstBunnyQuality = ((NGItem) bunnies.get(0).item).quality;

                if(worstBunnyQuality > averageBunnyQuality) {
                    break;
                }

                WItem bunny = bunnies.get(0);
                if (bunny == null) {
                    return Results.ERROR("NO_BUNNY");
                }

                Coord pos = bunny.c.div(Inventory.sqsz);
                bunny.item.wdgmsg("transfer", Coord.z);
                Coord finalPos1 = pos;
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return gui.getInventory(RABBIT_HUTCH_NAME).isSlotFree(finalPos1);
                    }
                });

                // Kill and drop the bunny
                WItem inventoryBunny = (WItem) gui.getInventory().getItem(new NAlias(BUNNY_NAME));
                new SelectFlowerAction( "Wring neck", inventoryBunny).run(gui);
                NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("A Bloody Mess"), 1));

                WItem bloodyMess = (WItem) gui.getInventory().getItem(new NAlias("A Bloody Mess"));
                NUtils.drop(bloodyMess);
            }

            new CloseTargetContainer(container).run(gui);
        }

        return Results.SUCCESS();
    }
}
