package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.*;

/**
 * KFC - Chicken Manager Bot
 * Manages chicken coops and incubators: replaces low-quality roosters/hens with better ones,
 * transfers eggs to incubators, and processes low-quality chickens.
 */
public class KFC implements Action {

    // Coop info class
    private static class CoopInfo {
        String gobHash;
        double roosterQuality;
        ArrayList<Float> henQualities = new ArrayList<>();

        public CoopInfo(String gobHash, double roosterQuality) {
            this.gobHash = gobHash;
            this.roosterQuality = roosterQuality;
        }
    }

    // Incubator info class
    private static class IncubatorInfo {
        String gobHash;
        double chickenQuality;

        public IncubatorInfo(String gobHash, double chickenQuality) {
            this.gobHash = gobHash;
            this.chickenQuality = chickenQuality;
        }
    }

    // Maximum chicks per incubator
    private static final int MAX_CHICKS_PER_INCUBATOR = 24;
    
    // Comparator for sorting incubators by quality
    Comparator<IncubatorInfo> incubatorComparator = (o1, o2) -> Double.compare(o1.chickenQuality, o2.chickenQuality);

    // Comparator for sorting coops
    Comparator<CoopInfo> coopComparator = (o1, o2) -> {
        int res = Double.compare(o1.roosterQuality, o2.roosterQuality);
        if (res == 0) {
            if (!o1.henQualities.isEmpty() && !o2.henQualities.isEmpty()) {
                double avgQuality1 = o1.henQualities.stream().mapToDouble(Float::doubleValue).average().orElse(0);
                double avgQuality2 = o2.henQualities.stream().mapToDouble(Float::doubleValue).average().orElse(0);
                res = Double.compare(avgQuality1, avgQuality2);
            }
        }
        return res;
    };

    NContext context;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        context = new NContext(gui);
        
        // Validate required areas
        NArea.Specialisation chickenSpec = new NArea.Specialisation(Specialisation.SpecName.chicken.toString());
        NArea.Specialisation incubatorSpec = new NArea.Specialisation(Specialisation.SpecName.incubator.toString());
        NArea.Specialisation swillSpec = new NArea.Specialisation(Specialisation.SpecName.swill.toString());
        NArea.Specialisation waterSpec = new NArea.Specialisation(Specialisation.SpecName.water.toString());
        
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(chickenSpec);
        req.add(incubatorSpec);
        
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(swillSpec);
        opt.add(waterSpec);
        
        if (!new Validator(req, opt).run(gui).IsSuccess()) {
            return Results.FAIL();
        }
        
        // Find areas globally
        NArea chickenArea = NContext.findSpecGlobal(Specialisation.SpecName.chicken.toString());
        NArea incubatorArea = NContext.findSpecGlobal(Specialisation.SpecName.incubator.toString());
        NArea swillArea = NContext.findSpecGlobal(Specialisation.SpecName.swill.toString());
        NArea waterArea = NContext.findSpecGlobal(Specialisation.SpecName.water.toString());
        
        if (chickenArea == null) {
            return Results.ERROR("Chicken area not found!");
        }
        if (incubatorArea == null) {
            return Results.ERROR("Incubator area not found!");
        }
        
        // Navigate to chicken area and collect coop hashes
        NUtils.navigateToArea(chickenArea);
        ArrayList<String> coopHashes = new ArrayList<>();
        for (Gob cc : Finder.findGobs(chickenArea, new NAlias("gfx/terobjs/chickencoop"))) {
            if (cc.ngob != null && cc.ngob.hash != null) {
                coopHashes.add(cc.ngob.hash);
            }
        }

        // Navigate to incubator area and collect incubator hashes
        NUtils.navigateToArea(incubatorArea);
        ArrayList<String> incubatorHashes = new ArrayList<>();
        for (Gob cc : Finder.findGobs(incubatorArea, new NAlias("gfx/terobjs/chickencoop"))) {
            if (cc.ngob != null && cc.ngob.hash != null) {
                incubatorHashes.add(cc.ngob.hash);
            }
        }

        // Fill chicken coops and incubators with fluids
        if (swillArea != null || waterArea != null) {
            ArrayList<Container> containers = getContainersFromHashes(coopHashes, chickenArea);
            ArrayList<Container> ccontainers = getContainersFromHashes(incubatorHashes, incubatorArea);
            
            if (swillArea != null) {
                new FillFluid(containers, swillArea.getRCArea(), new NAlias("swill"), 2).run(gui);
                new FillFluid(ccontainers, swillArea.getRCArea(), new NAlias("swill"), 2).run(gui);
            }
            if (waterArea != null) {
                new FillFluid(containers, waterArea.getRCArea(), new NAlias("water"), 1).run(gui);
                new FillFluid(ccontainers, waterArea.getRCArea(), new NAlias("water"), 1).run(gui);
            }
        }

        // Read coop contents and sort them
        ArrayList<CoopInfo> coopInfos = new ArrayList<>();
        ArrayList<IncubatorInfo> qcocks = new ArrayList<>();
        ArrayList<IncubatorInfo> qhens = new ArrayList<>();
        
        // Navigate to chicken area and read coop contents
        NUtils.navigateToArea(chickenArea);
        for (String hash : coopHashes) {
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            double roosterQuality;
            if (gui.getInventory("Chicken Coop").getItem(new NAlias("Cock")) != null) {
                NGItem roost = (NGItem) gui.getInventory("Chicken Coop").getItem(new NAlias("Cock")).item;
                roosterQuality = roost.quality;
            } else {
                roosterQuality = -1;
            }

            CoopInfo coopInfo = new CoopInfo(hash, roosterQuality);

            ArrayList<WItem> hens = gui.getInventory("Chicken Coop").getItems(new NAlias("Hen"));
            for (WItem hen : hens) {
                coopInfo.henQualities.add(((NGItem) hen.item).quality);
            }
            coopInfo.henQualities.sort(Float::compareTo);

            coopInfos.add(coopInfo);

            new CloseTargetContainer("Chicken Coop").run(gui);
        }

        // Sort coops by rooster quality and average hen quality
        coopInfos.sort(coopComparator.reversed());

        // Navigate to incubator area and read contents
        NUtils.navigateToArea(incubatorArea);
        for (String hash : incubatorHashes) {
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            ArrayList<WItem> roosters = gui.getInventory("Chicken Coop").getItems(new NAlias("Cock"));
            for (WItem rooster : roosters) {
                qcocks.add(new IncubatorInfo(hash, ((NGItem) rooster.item).quality));
            }

            ArrayList<WItem> hens = gui.getInventory("Chicken Coop").getItems(new NAlias("Hen"));
            for (WItem hen : hens) {
                qhens.add(new IncubatorInfo(hash, ((NGItem) hen.item).quality));
            }

            new CloseTargetContainer("Chicken Coop").run(gui);
        }

        Results roosterResult = processRoosters(gui, coopInfos, qcocks);
        if (!roosterResult.IsSuccess()) {
            return roosterResult;
        }

        Results henResult = processHens(gui, coopInfos, qhens);
        if (!henResult.IsSuccess()) {
            return henResult;
        }

        // Transfer chicks from chicken coops to incubators
        transferChicks(gui, coopHashes, incubatorHashes);

        // Determine threshold quality for eggs from best coop
        if (coopInfos.isEmpty()) {
            return Results.ERROR("No chicken coops found!");
        }
        
        context.getSpecArea(Specialisation.SpecName.chicken);
        Gob bestCoopGob = Finder.findGob(coopInfos.get(0).gobHash);
        if (bestCoopGob == null) {
            return Results.ERROR("Best coop not found!");
        }
        
        new PathFinder(bestCoopGob).run(gui);
        if (!(new OpenTargetContainer("Chicken Coop", bestCoopGob).run(gui).IsSuccess())) {
            return Results.FAIL();
        }

        // Get quality threshold from top hens
        ArrayList<WItem> topHens = gui.getInventory("Chicken Coop").getItems(new NAlias("Hen"));
        ArrayList<Float> qtop = new ArrayList<>();
        for (WItem top : topHens) {
            qtop.add(((NGItem) top.item).quality);
        }
        
        if (qtop.isEmpty()) {
            gui.msg("No hens found in best coop!");
            return Results.ERROR("No hens in best coop");
        }
        
        qtop.sort(Float::compareTo);
        double chicken_th = qtop.get(0);
        gui.msg("Egg quality threshold: " + chicken_th);
        new CloseTargetContainer("Chicken Coop").run(gui);

        // Collect low quality eggs and dispose via FreeInventory2 (like Butcher)
        collectAndDisposeLowQualityEggs(gui, coopHashes, chicken_th);

        new FreeInventory2(context).run(gui);
        return Results.SUCCESS();
    }
    
    private ArrayList<Container> getContainersFromHashes(ArrayList<String> hashes, NArea area) {
        ArrayList<Container> containers = new ArrayList<>();
        for (String hash : hashes) {
            Gob gob = Finder.findGob(hash);
            if (gob != null) {
                Container cand = new Container(gob, "Chicken Coop", area);
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }
        }
        return containers;
    }
    
    private void transferChicks(NGameUI gui, ArrayList<String> coopHashes, ArrayList<String> incubatorHashes) throws InterruptedException {
        NAlias chickAlias = new NAlias(new ArrayList<>(List.of("Chick")), new ArrayList<>(List.of("Egg")));
        
        // Collect chicks from chicken coops
        context.getSpecArea(Specialisation.SpecName.chicken);
        for (String hash : coopHashes) {
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", gob).run(gui).IsSuccess())) {
                continue;
            }
            
            // Transfer all chicks to inventory (exclude Eggs)
            ArrayList<WItem> chicks = gui.getInventory("Chicken Coop").getItems(chickAlias);
            for (WItem chick : chicks) {
                chick.item.wdgmsg("transfer", Coord.z);
            }
            
            new CloseTargetContainer("Chicken Coop").run(gui);
            
            // If inventory getting full, transfer to incubators (don't kill yet)
            if (shouldDropOffItems(gui)) {
                transferChicksToIncubators(gui, incubatorHashes);
                context.getSpecArea(Specialisation.SpecName.chicken);
            }
        }
        
        // Transfer all remaining chicks to incubators (fills all available space)
        transferChicksToIncubators(gui, incubatorHashes);
        
        // Only after ALL incubators are full, kill excess chicks
        killExcessChicks(gui, chickAlias);
    }
    
    private void transferChicksToIncubators(NGameUI gui, ArrayList<String> incubatorHashes) throws InterruptedException {
        NAlias chickAlias = new NAlias(new ArrayList<>(List.of("Chick")), new ArrayList<>(List.of("Egg")));
        ArrayList<WItem> chicks = gui.getInventory().getItems(chickAlias);
        if (chicks.isEmpty()) return;
        
        context.getSpecArea(Specialisation.SpecName.incubator);
        for (String hash : incubatorHashes) {
            chicks = gui.getInventory().getItems(chickAlias);
            if (chicks.isEmpty()) break;
            
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            // Create container with ItemCount for chick tracking
            Container incubatorContainer = new Container(gob, "Chicken Coop", null);
            Container.ItemCount itemCount = incubatorContainer.initItemCount(chickAlias, MAX_CHICKS_PER_INCUBATOR);
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer(incubatorContainer).run(gui).IsSuccess())) {
                continue;
            }
            
            // Update ItemCount to get current chick count
            itemCount.update();
            int canAdd = itemCount.getNeeded();
            
            if (canAdd <= 0) {
                new CloseTargetContainer(incubatorContainer).run(gui);
                continue;
            }
            
            // Transfer chicks to incubator (up to limit)
            int transferred = 0;
            for (WItem chick : chicks) {
                if (transferred >= canAdd) break;
                if (gui.getInventory("Chicken Coop").getNumberFreeCoord(new Coord(2, 2)) > 0) {
                    chick.item.wdgmsg("transfer", Coord.z);
                    transferred++;
                } else {
                    break;
                }
            }
            
            new CloseTargetContainer(incubatorContainer).run(gui);
        }
    }
    
    /**
     * Kill excess chicks that couldn't fit in incubators.
     * Wring neck -> wait for "A Bloody Mess" -> drop on ground
     */
    private void killExcessChicks(NGameUI gui, NAlias chickAlias) throws InterruptedException {
        ArrayList<WItem> chicks = gui.getInventory().getItems(chickAlias);
        
        while (!chicks.isEmpty()) {
            WItem chick = chicks.get(0);
            
            // Wring neck
            new SelectFlowerAction("Wring neck", chick).run(gui);
            
            // Wait for "A Bloody Mess" to appear
            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("A Bloody Mess"), 1));
            
            // Drop the bloody mess on ground
            WItem bloodyMess = gui.getInventory().getItem(new NAlias("A Bloody Mess"));
            if (bloodyMess != null) {
                NUtils.drop(bloodyMess);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        try {
                            return gui.getInventory().getItems(new NAlias("A Bloody Mess")).isEmpty();
                        } catch (InterruptedException e) {
                            return false;
                        }
                    }
                });
            }
            
            // Get remaining chicks
            chicks = gui.getInventory().getItems(chickAlias);
        }
    }
    
    /**
     * Collect eggs with quality BELOW threshold and dispose them via FreeInventory2 (like Butcher).
     * Good quality eggs stay in coops for hatching.
     */
    private void collectAndDisposeLowQualityEggs(NGameUI gui, ArrayList<String> coopHashes, double qualityThreshold) throws InterruptedException {
        context.getSpecArea(Specialisation.SpecName.chicken);
        for (String hash : coopHashes) {
            Gob gob = Finder.findGob(hash);
            if (gob == null) continue;
            
            new PathFinder(gob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", gob).run(gui).IsSuccess())) {
                continue;
            }
            
            // Collect eggs BELOW quality threshold (bad eggs to dispose)
            ArrayList<WItem> eggs = gui.getInventory("Chicken Coop").getItems(new NAlias("Chicken Egg"));
            for (WItem egg : eggs) {
                if (((NGItem) egg.item).quality < qualityThreshold) {
                    egg.item.wdgmsg("transfer", Coord.z);
                }
            }
            
            new CloseTargetContainer("Chicken Coop").run(gui);
            
            // If inventory getting full, dispose via FreeInventory2 and return to chicken area
            if (shouldDropOffItems(gui)) {
                new FreeInventory2(context).run(gui);
                context.getSpecArea(Specialisation.SpecName.chicken);
            }
        }
    }


    private Results processRoosters(NGameUI gui, ArrayList<CoopInfo> coopInfos, ArrayList<IncubatorInfo> qcocks) throws InterruptedException {
        // Sort roosters by quality (best to worst)
        qcocks.sort(incubatorComparator.reversed());

        for (IncubatorInfo roosterInfo : qcocks) {
            // Navigate to incubator area and open the coop with rooster
            context.getSpecArea(Specialisation.SpecName.incubator);
            
            Gob roosterGob = Finder.findGob(roosterInfo.gobHash);
            if (roosterGob == null) continue;
            
            new PathFinder(roosterGob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", roosterGob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Get rooster from coop inventory
            WItem rooster = gui.getInventory("Chicken Coop").getItem(new NAlias("Cock"));
            if (rooster == null) {
                new CloseTargetContainer("Chicken Coop").run(gui);
                continue;
            }
            double roosterQuality = ((NGItem) rooster.item).quality;

            Coord pos = rooster.c.div(Inventory.sqsz);
            rooster.item.wdgmsg("transfer", Coord.z);
            Coord finalPos1 = pos;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.getInventory("Chicken Coop").isSlotFree(finalPos1);
                }
            });
            new CloseTargetContainer("Chicken Coop").run(gui);

            // Find coop with worse rooster and replace it
            for (CoopInfo coopInfo : coopInfos) {
                if (coopInfo.roosterQuality < roosterQuality && coopInfo.roosterQuality != -1) {
                    rooster = gui.getInventory().getItem(new NAlias("Cock"));
                    if (rooster == null) break;

                    // Navigate to chicken area and open coop for replacement
                    context.getSpecArea(Specialisation.SpecName.chicken);
                    
                    Gob coopGob = Finder.findGob(coopInfo.gobHash);
                    if (coopGob == null) continue;
                    
                    new PathFinder(coopGob).run(gui);
                    if (!(new OpenTargetContainer("Chicken Coop", coopGob).run(gui).IsSuccess())) {
                        return Results.FAIL();
                    }

                    // Get current rooster in coop
                    WItem oldRooster = gui.getInventory("Chicken Coop").getItem(new NAlias("Cock"));
                    if (oldRooster == null) {
                        new CloseTargetContainer("Chicken Coop").run(gui);
                        continue;
                    }

                    // Replace rooster
                    pos = oldRooster.c.div(Inventory.sqsz);
                    oldRooster.item.wdgmsg("transfer", Coord.z);
                    Coord finalPos = pos;
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return gui.getInventory("Chicken Coop").isSlotFree(finalPos);
                        }
                    });

                    NUtils.takeItemToHand(rooster);
                    gui.getInventory("Chicken Coop").dropOn(pos, "Cock");

                    // Update quality
                    coopInfo.roosterQuality = roosterQuality;
                    roosterQuality = ((NGItem) oldRooster.item).quality;
                    new CloseTargetContainer("Chicken Coop").run(gui);
                }
            }

            // Process the rooster (butcher it)
            rooster = gui.getInventory().getItem(new NAlias("Cock"));
            if (rooster != null) {
                butcherChicken(gui, rooster, "Cock", "Dead Cock");
            }
        }
        new FreeInventory2(context).run(gui);
        return Results.SUCCESS();
    }

    private Results processHens(NGameUI gui, ArrayList<CoopInfo> coopInfos, ArrayList<IncubatorInfo> qhens) throws InterruptedException {
        // Sort hens by quality (best to worst)
        qhens.sort(incubatorComparator.reversed());

        for (IncubatorInfo henInfo : qhens) {
            // Navigate to incubator area and open coop with hen
            context.getSpecArea(Specialisation.SpecName.incubator);
            
            Gob henGob = Finder.findGob(henInfo.gobHash);
            if (henGob == null) continue;
            
            new PathFinder(henGob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", henGob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Get hen from coop inventory
            WItem hen = gui.getInventory("Chicken Coop").getItem(new NAlias("Hen"));
            if (hen == null) {
                new CloseTargetContainer("Chicken Coop").run(gui);
                continue;
            }
            float henQuality = ((NGItem) hen.item).quality;

            Coord pos = hen.c.div(Inventory.sqsz);
            hen.item.wdgmsg("transfer", Coord.z);
            Coord finalPos1 = pos;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.getInventory("Chicken Coop").isSlotFree(finalPos1);
                }
            });
            new CloseTargetContainer("Chicken Coop").run(gui);

            // Find coop with worse hen and replace it
            for (CoopInfo coopInfo : coopInfos) {
                for (int i = 0; i < coopInfo.henQualities.size(); i++) {
                    if (coopInfo.henQualities.get(i) < henQuality) {
                        hen = gui.getInventory().getItem(new NAlias("Hen"));
                        if (hen == null) break;

                        // Navigate to chicken area and open coop for replacement
                        context.getSpecArea(Specialisation.SpecName.chicken);
                        
                        Gob coopGob = Finder.findGob(coopInfo.gobHash);
                        if (coopGob == null) continue;
                        
                        new PathFinder(coopGob).run(gui);
                        if (!(new OpenTargetContainer("Chicken Coop", coopGob).run(gui).IsSuccess())) {
                            return Results.FAIL();
                        }

                        // Get current hen in coop
                        WItem oldHen = gui.getInventory("Chicken Coop").getItem(new NAlias("Hen"), coopInfo.henQualities.get(i));
                        if (oldHen == null) {
                            new CloseTargetContainer("Chicken Coop").run(gui);
                            continue;
                        }

                        // Replace hen
                        pos = oldHen.c.div(Inventory.sqsz);
                        oldHen.item.wdgmsg("transfer", Coord.z);
                        Coord finalPos = pos;
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return gui.getInventory("Chicken Coop").isSlotFree(finalPos);
                            }
                        });

                        NUtils.takeItemToHand(hen);
                        gui.getInventory("Chicken Coop").dropOn(pos, "Hen");

                        // Update quality
                        coopInfo.henQualities.set(i, henQuality);
                        henQuality = ((NGItem) oldHen.item).quality;
                        new CloseTargetContainer("Chicken Coop").run(gui);
                        break;
                    }
                }
            }

            // Process the hen (butcher it)
            hen = gui.getInventory().getItem(new NAlias("Hen"));
            if (hen != null) {
                butcherChicken(gui, hen, "Hen", "Dead Hen");
            }
        }
        new FreeInventory2(context).run(gui);
        return Results.SUCCESS();
    }
    
    /**
     * Butcher a chicken - wring neck, pluck, clean, butcher
     * Similar to Butcher bot approach with FreeInventory2 and return to area
     */
    private void butcherChicken(NGameUI gui, WItem chicken, String chickenType, String deadType) throws InterruptedException {
        // Check inventory space before butchering
        if (gui.getInventory().getNumberFreeCoord(new Coord(1, 1)) < 2) {
            new FreeInventory2(context).run(gui);
        }
        
        new SelectFlowerAction("Wring neck", chicken).run(gui);
        NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias(deadType), 1));

        WItem deadChicken = gui.getInventory().getItem(new NAlias(deadType));
        if (deadChicken == null) return;
        
        new SelectFlowerAction("Pluck", deadChicken).run(gui);
        NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Plucked Chicken"), 1));

        WItem plucked = gui.getInventory().getItem(new NAlias("Plucked Chicken"));
        if (plucked == null) return;
        
        new SelectFlowerAction("Clean", plucked).run(gui);
        NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Cleaned Chicken"), 1));

        WItem cleaned = gui.getInventory().getItem(new NAlias("Cleaned Chicken"));
        if (cleaned == null) return;
        
        new SelectFlowerAction("Butcher", cleaned).run(gui);
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                try {
                    return gui.getInventory().getItems(new NAlias("Cleaned Chicken")).isEmpty();
                } catch (InterruptedException e) {
                    return false;
                }
            }
        });

        // Drop off if insufficient space for another chicken
        if (shouldDropOffItems(gui)) {
            new FreeInventory2(context).run(gui);
        }
    }

    /**
     * Checks if inventory drop-off is needed based on available space.
     * Only drops off if insufficient space for another chicken + buffer.
     *
     * @param gui Game UI interface
     * @return true if drop-off needed, false if can continue batching
     */
    private boolean shouldDropOffItems(NGameUI gui) throws InterruptedException {
            // Check available space for 1x1 items (Meat + Bone from chickens)
            // Need space for: 1 more chicken (2 cells) + 4 buffer cells = 8 total cells
            int availableSpaceForChicken = gui.getInventory().getNumberFreeCoord(new Coord(2, 2));
            // Chicken is 2x2 (4 cells) plus 4 extra space for products.
            return availableSpaceForChicken <= 2;
    }
}
