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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public class RabbitMaster implements Action {
    private final String RABBIT_HUTCH_NAME = "Rabbit Hutch";
    private final String RABBIT_HUTCH_RESOURCE_NAME = "gfx/terobjs/rabbithutch";
    private final String BUCK_NAME = "Rabbit Buck";
    private final String DOE_NAME = "Rabbit Doe";
    private final String BUNNY_NAME = "Bunny";

    Instant sevenDaysAgo = Instant.now().minusSeconds(7 * 24 * 60 * 60);

    private ArrayList<Doe> does = new ArrayList<>();

    // Info about rabbit hutch
    private class HutchInfo {
        Container container; // Rabbit hutch container
        double buckQuality; // Buck quality
        ArrayList<Float> doeQualities = new ArrayList<>(); // Doe quality list

        public HutchInfo(Container container, double buckQuality) {
            this.container = container;
            this.buckQuality = buckQuality;
        }
    }

    private class Doe {
        String hutchHash;
        Float quality;
        Coord pos;
        Instant timeMoved;

        public Doe(String hutchHash, Float quality, Coord pos) {
            this.hutchHash = hutchHash;
            this.quality = quality;
            this.pos = pos;
        }

        public Doe(JSONObject obj) {
            // Quality: store as Float
            this.quality = obj.has("quality") ? (float) obj.getDouble("quality") : null;

            this.hutchHash = obj.has("hutchHash") ? obj.getString("hutchHash") : null;

            // Position: assuming pos is an object with x and y
            if (obj.has("pos")) {
                JSONObject posObj = obj.getJSONObject("pos");
                int x = posObj.getInt("x");
                int y = posObj.getInt("y");
                this.pos = new Coord(x, y);
            }

            // timeMoved: try to parse as ISO string first, fallback to millis
            if (obj.has("timeMoved")) {
                Object t = obj.get("timeMoved");
                if (t instanceof Number) {
                    this.timeMoved = Instant.ofEpochMilli(((Number) t).longValue());
                } else if (t instanceof String) {
                    this.timeMoved = Instant.parse((String) t);
                }
            }
        }

        public void setTimeMoved(Instant timeMoved) {
            this.timeMoved = timeMoved;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();

            if (hutchHash != null)
                obj.put("hutchHash", hutchHash);

            if (quality != null)
                obj.put("quality", quality);

            if (pos != null) {
                JSONObject posObj = new JSONObject();
                posObj.put("x", pos.x);
                posObj.put("y", pos.y);
                obj.put("pos", posObj);
            }

            if (timeMoved != null)
                obj.put("timeMoved", timeMoved.toString()); // ISO 8601 format

            return obj;
        }
    }

    // Info about incubator
    private class IncubatorInfo {
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
    Comparator<RabbitMaster.HutchInfo> coopComparator = new Comparator<RabbitMaster.HutchInfo>() {
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
        loadRabbits();

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
            RabbitMaster.HutchInfo hutchInfo = new RabbitMaster.HutchInfo(container, buckQuality);

            // Get info about does.
            ArrayList<WItem> does = gui.getInventory(RABBIT_HUTCH_NAME).getItems(new NAlias(DOE_NAME));
            for (WItem doeItem : does) {
                float quality = ((NGItem)doeItem.item).quality;
                Coord pos = doeItem.c.div(Inventory.sqsz);
                String hutchHash = container.gob.ngob.hash;

                // Search for existing Doe by hutch+pos
                Doe match = null;
                for (Doe d : this.does) {
                    if (Objects.equals(d.hutchHash, hutchHash) && d.pos.equals(pos)) {
                        match = d;
                        break;
                    }
                }

                if (match != null) {
                    // Update quality, optionally update timeMoved
                    match.quality = quality;
                } else {
                    // Add new Doe. We've never seen this doe so we assume this is either the first time running the bot
                    // or the doe was added sometime in between runs. If that is the case we want to consider this doe
                    // safe to swap out, so we set the time it was moved to today minus 7 days.
                    Doe doe = new Doe(hutchHash, quality, pos);
                    doe.setTimeMoved(sevenDaysAgo);
                    this.does.add(doe);
                }

                hutchInfo.doeQualities.add(((NGItem)doeItem.item).quality);
            }

            hutchInfo.doeQualities.sort(Float::compareTo);

            // Add hutch to the list
            hutchInfos.add(hutchInfo);

            new CloseTargetContainer(container).run(gui);
        }

        // Sort does from best to worst
        this.does.sort(Comparator.comparing((Doe d) -> d.quality).reversed());

        // Sort hutches by quality of bucks and average quality of does.
        hutchInfos.sort(coopComparator.reversed());

        for (Container container : rabbitHutchesIncubators) {
            new PathFinder(container.gob).run(gui);
            if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            ArrayList<WItem> bucks = gui.getInventory(RABBIT_HUTCH_NAME).getItems(new NAlias(BUCK_NAME));
            for (WItem buck : bucks) {
                qBucks.add(new RabbitMaster.IncubatorInfo(container, ((NGItem) buck.item).quality));
            }

            ArrayList<WItem> hens = gui.getInventory(RABBIT_HUTCH_NAME).getItems(new NAlias(DOE_NAME));
            for (WItem hen : hens) {
                qDoes.add(new RabbitMaster.IncubatorInfo(container, ((NGItem) hen.item).quality));
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

        writeRabbits();
        return Results.SUCCESS();
    }


    private Results processBucks(NGameUI gui, ArrayList<HutchInfo> hutchInfos, ArrayList<IncubatorInfo> qBucks) throws InterruptedException {
        // Sort bucks by quality (from best to worst)
        qBucks.sort(incubatorComparator.reversed());

        for (IncubatorInfo roosterInfo : qBucks) {
            // Open hutch with buck
            new PathFinder(roosterInfo.container.gob).run(gui);
            if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, roosterInfo.container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Get buck from intentory
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

            // TODO KILLING AND PROCESSING
//            buck = (WItem) gui.getInventory().getItem(new NAlias(BUCK_NAME));
//            new SelectFlowerAction( "Wring neck", buck).run(gui);
//            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Dead Rabbit"), 1));
//
//            buck = (WItem) gui.getInventory().getItem(new NAlias("Dead Rabbit"));
//            new SelectFlowerAction( "Fray", buck).run(gui);
//            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Rabbit Carcass"), 1));
//
//            buck = (WItem) gui.getInventory().getItem(new NAlias("Rabbit Carcass"));
//            new SelectFlowerAction( "Clean", buck).run(gui);
//            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Clean Rabbit Carcass"), 1));
//
//            buck = (WItem) gui.getInventory().getItem(new NAlias("Clean Rabbit Carcass"));
//            new SelectFlowerAction( "Butcher", buck).run(gui);
//            NUtils.addTask(new NTask() {
//                @Override
//                public boolean check() {
//                    try {
//                        return gui.getInventory().getItems(new NAlias("Clean Rabbit Carcass")).isEmpty();
//                    } catch (InterruptedException e) {
//                        return false;
//                    }
//                }
//            });
//
//            new FreeInventory(context).run(gui);
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

            // Find the worst doe and swap it out
            for (Doe candidate : this.does) {
                if(candidate.quality >= doeQuality) {
                    continue;
                }
                // Check if candidate is in a current hutch
                HutchInfo hutchInfo = hutchInfos.stream()
                        .filter(h -> h.container.gob.ngob.hash.equals(candidate.hutchHash))
                        .findFirst()
                        .orElse(null);

                if (hutchInfo == null) continue; // skip does in hutches not currently found

                if (candidate.timeMoved == null || candidate.timeMoved.isBefore(sevenDaysAgo)) {
                    // Open hutch for the swap
                    new PathFinder(hutchInfo.container.gob).run(gui);
                    if (!(new OpenTargetContainer(RABBIT_HUTCH_NAME, hutchInfo.container.gob).run(gui).IsSuccess())) {
                        return Results.FAIL();
                    }

                    // Get the current doe in the inventory
                    doe = (WItem) gui.getInventory().getItem(new NAlias(DOE_NAME));

                    // Get all does in the hutch
                    ArrayList<WItem> currentDoesInHutch = gui.getInventory(RABBIT_HUTCH_NAME).getItems();

                    // Get the doe we want to swap from the hutch
                    WItem oldDoe = null;

                    for (WItem item : currentDoesInHutch) {
                        if (item.c.div(Inventory.sqsz).equals(candidate.pos)) {
                            oldDoe = item;
                            break;
                        }
                    }
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

                    // Update doe quality
                    candidate.quality = ((NGItem)doe.item).quality;
                    candidate.setTimeMoved(Instant.now());

                    new CloseTargetContainer(hutchInfo.container).run(gui);
                    writeRabbits();
                    break;
                }
            }

            // Убиваем курицу и обрабатываем её
            // TODO KILLING AND PROCESSING
//            doe = (WItem) gui.getInventory().getItem(new NAlias(DOE_NAME));
//            new SelectFlowerAction("Wring neck", doe).run(gui);
//            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Dead Rabbit"), 1));
//
//            doe = (WItem) gui.getInventory().getItem(new NAlias("Dead Rabbit"));
//            new SelectFlowerAction("Fray", doe).run(gui);
//            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Rabbit Carcass"), 1));
//
//            doe = (WItem) gui.getInventory().getItem(new NAlias("Rabbit Carcass"));
//            new SelectFlowerAction("Clean", doe).run(gui);
//            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Clean Rabbit Carcass"), 1));
//
//            doe = (WItem) gui.getInventory().getItem(new NAlias("Clean Rabbit Carcass"));
//            new SelectFlowerAction("Butcher", doe).run(gui);
//            NUtils.addTask(new NTask() {
//                @Override
//                public boolean check() {
//                    try {
//                        return gui.getInventory().getItems(new NAlias("Clean Rabbit Carcass")).isEmpty();
//                    } catch (InterruptedException e) {
//                        return false;
//                    }
//                }
//            });
//
//            new FreeInventory(context).run(gui);
        }
        new FreeInventory(context).run(gui);
        return Results.SUCCESS();
    }

    public void loadRabbits() {
        if (new File(NConfig.current.path_rabbits).exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_rabbits), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (IOException ignore) {
            }

            if (!contentBuilder.toString().isEmpty()) {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = (JSONArray) main.get("does");
                for (int i = 0; i < array.length(); i++) {
                    Doe doe = new Doe((JSONObject) array.get(i));
                    does.add(doe);
                }
            }
        }
    }

    public void writeRabbits()
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            JSONObject main = new JSONObject();
            JSONArray jdoes = new JSONArray();
            for(Doe doe : does)
            {
                jdoes.put(doe.toJson());
            }
            main.put("does",jdoes);
            try
            {
                FileWriter f = new FileWriter(NConfig.current.path_rabbits,StandardCharsets.UTF_8);
                main.write(f);
                f.close();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

}
