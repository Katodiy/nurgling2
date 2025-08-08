package nurgling.actions.bots.silk;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimizes silkmoth pairs across containers using inventory as buffer.
 */
public class ArrangeSilkmothPairs implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        // Get silkmothBreeding area
        NArea breedingArea = context.getSpecArea(Specialisation.SpecName.silkmothBreeding);
        if (breedingArea == null) {
            return Results.ERROR("SilkmothBreeding area not found");
        }

        // Get all containers in the breeding area
        ArrayList<Container> containers = getContainersInArea(breedingArea);
        if (containers.isEmpty()) {
            return Results.ERROR("No containers found in breeding area");
        }

        // Repeat until all containers are optimized
        while (true) {
            // Step 1: Analyze container contents
            ArrayList<ContainerState> containerStates = analyzeContainers(gui, containers);

            // Step 2: Calculate differences (excess and shortage)
            calculateDifferences(containerStates);

            // Check if we're done (all containers are balanced)
            if (allContainersBalanced(containerStates)) {
                break;
            }

            // Step 3: Check inventory space and limit processing if needed
            int inventorySpace = gui.getInventory().getFreeSpace();
            ArrayList<ContainerState> processingBatch = selectProcessingBatch(containerStates, inventorySpace);

            if (processingBatch.isEmpty()) {
                break; // No containers can be processed due to inventory space
            }

            // Step 4: Collect excess moths into inventory
            collectExcessMoths(gui, processingBatch);

            // Step 5: Redistribute moths from inventory
            redistributeMoths(gui, processingBatch);
        }

        return Results.SUCCESS();
    }

    private ArrayList<Container> getContainersInArea(NArea area) throws InterruptedException {
        ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob gob : gobs) {
            Container c = new Container(gob, Context.contcaps.get(gob.ngob.name));
            c.initattr(Container.Space.class);
            containers.add(c);
        }
        return containers;
    }

    private ArrayList<ContainerState> analyzeContainers(NGameUI gui, ArrayList<Container> containers) throws InterruptedException {
        ArrayList<ContainerState> states = new ArrayList<>();
        NAlias femaleMothAlias = new NAlias("Female Silkmoth");
        NAlias maleMothAlias = new NAlias(new ArrayList<>(List.of("Male Silkmoth")), new ArrayList<>(List.of("female")));
        NAlias cocoonAlias = new NAlias("Silkworm Cocoon");

        // First pass: collect all counts
        int totalFemales = 0;
        int totalMales = 0;
        
        for (Container container : containers) {
            new PathFinder(Finder.findGob(container.gobid)).run(gui);
            new OpenTargetContainer(container).run(gui);

            // Count items
            int femaleCount = gui.getInventory(container.cap).getItems(femaleMothAlias).size();
            int maleCount = gui.getInventory(container.cap).getItems(maleMothAlias).size();
            int cocoonCount = gui.getInventory(container.cap).getItems(cocoonAlias).size();

            totalFemales += femaleCount;
            totalMales += maleCount;

            // Calculate max possible pairs for this container based on space
            int freeSlots = 16 - cocoonCount;
            int maxPossiblePairs = Math.min(8, freeSlots / 2);

            ContainerState state = new ContainerState(container, femaleCount, maleCount, cocoonCount, 0, 0); // targets will be set later
            state.maxPossiblePairs = maxPossiblePairs;
            states.add(state);

            new CloseTargetContainer(container).run(gui);
        }
        
        // Calculate global constraints and distribute pairs realistically
        int totalPossiblePairs = Math.min(totalFemales, totalMales);
        distributePairsAcrossContainers(states, totalPossiblePairs, totalFemales, totalMales);

        return states;
    }
    
    private void distributePairsAcrossContainers(ArrayList<ContainerState> states, int totalPossiblePairs, int totalFemales, int totalMales) {
        // Sort containers by their capacity (prefer containers that can hold more pairs)
        states.sort((a, b) -> Integer.compare(b.maxPossiblePairs, a.maxPossiblePairs));
        
        int pairsDistributed = 0;
        
        // Distribute pairs to containers - but respect each container's maxPossiblePairs
        for (ContainerState state : states) {
            // Use the already calculated maxPossiblePairs which accounts for cocoons
            int pairsForThisContainer = Math.min(state.maxPossiblePairs, totalPossiblePairs - pairsDistributed);
            state.targetFemale = pairsForThisContainer;
            state.targetMale = pairsForThisContainer;
            pairsDistributed += pairsForThisContainer;

            if (pairsDistributed >= totalPossiblePairs) {
                break;
            }
        }
        
        // Handle remaining unpaired moths - distribute to containers with available space
        int remainingFemales = totalFemales - pairsDistributed;
        int remainingMales = totalMales - pairsDistributed;
        
        for (ContainerState state : states) {
            // Calculate available space AFTER pairs and cocoons are placed
            int usedSlots = state.cocoonCount + state.targetFemale + state.targetMale;
            int availableSlots = 16 - usedSlots;

            if (availableSlots > 0 && (remainingFemales > 0 || remainingMales > 0)) {
                // Only add moths if there's actual space remaining
                int totalRemainingMoths = remainingFemales + remainingMales;
                int mothsToAdd = Math.min(totalRemainingMoths, availableSlots);
                
                // Distribute proportionally between females and males
                int femalesToAdd = 0;
                int malesToAdd = 0;
                
                if (mothsToAdd > 0) {
                    if (remainingFemales > 0 && remainingMales > 0) {
                        // Both genders available - distribute proportionally
                        femalesToAdd = Math.min(remainingFemales, (mothsToAdd * remainingFemales) / totalRemainingMoths);
                        malesToAdd = Math.min(mothsToAdd - femalesToAdd, remainingMales);
                    } else if (remainingFemales > 0) {
                        // Only females available
                        femalesToAdd = Math.min(remainingFemales, mothsToAdd);
                    } else {
                        // Only males available
                        malesToAdd = Math.min(remainingMales, mothsToAdd);
                    }
                    
                    state.targetFemale += femalesToAdd;
                    state.targetMale += malesToAdd;
                    
                    remainingFemales -= femalesToAdd;
                    remainingMales -= malesToAdd;
                }
            }
            
            if (remainingFemales == 0 && remainingMales == 0) {
                break;
            }
        }
    }

    private void calculateDifferences(ArrayList<ContainerState> containerStates) {
        for (ContainerState state : containerStates) {
            state.excessFemale = Math.max(0, state.femaleCount - state.targetFemale);
            state.excessMale = Math.max(0, state.maleCount - state.targetMale);
            state.shortageFemale = Math.max(0, state.targetFemale - state.femaleCount);
            state.shortageMale = Math.max(0, state.targetMale - state.maleCount);
        }
    }

    private boolean allContainersBalanced(ArrayList<ContainerState> containerStates) {
        for (ContainerState state : containerStates) {
            if (state.femaleCount != state.targetFemale || state.maleCount != state.targetMale) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<ContainerState> selectProcessingBatch(ArrayList<ContainerState> containerStates, int inventorySpace) {
        ArrayList<ContainerState> batch = new ArrayList<>();
        int requiredSpace = 0;

        // Sort by containers closest to target (minimize transfers)
        containerStates.sort((a, b) -> {
            int aDistance = Math.abs(a.femaleCount - a.targetFemale) + Math.abs(a.maleCount - a.targetMale);
            int bDistance = Math.abs(b.femaleCount - b.targetFemale) + Math.abs(b.maleCount - b.targetMale);
            return Integer.compare(aDistance, bDistance);
        });

        for (ContainerState state : containerStates) {
            int stateRequiredSpace = state.excessFemale + state.excessMale;
            if (requiredSpace + stateRequiredSpace <= inventorySpace) {
                batch.add(state);
                requiredSpace += stateRequiredSpace;
            } else {
                break; // Cannot fit more containers in this batch
            }
        }

        return batch;
    }

    private void collectExcessMoths(NGameUI gui, ArrayList<ContainerState> processingBatch) throws InterruptedException {
        NAlias femaleMothAlias = new NAlias("Female Silkmoth");
        NAlias maleMothAlias = new NAlias(new ArrayList<>(List.of("Male Silkmoth")), new ArrayList<>(List.of("female")));

        for (ContainerState state : processingBatch) {
            if (state.excessFemale > 0 || state.excessMale > 0) {
                new PathFinder(Finder.findGob(state.container.gobid)).run(gui);
                new OpenTargetContainer(state.container).run(gui);

                // Take excess female moths
                if (state.excessFemale > 0) {
                    ArrayList<WItem> femaleMoths = gui.getInventory(state.container.cap).getItems(femaleMothAlias);
                    ArrayList<WItem> excessFemales = new ArrayList<>();
                    for (int i = 0; i < Math.min(state.excessFemale, femaleMoths.size()); i++) {
                        excessFemales.add(femaleMoths.get(i));
                    }
                    if (!excessFemales.isEmpty()) {
                        new TakeWItemsFromContainer(state.container, excessFemales).run(gui);
                        // Update state to reflect actual count after taking
                        new OpenTargetContainer(state.container).run(gui);
                        state.femaleCount = gui.getInventory(state.container.cap).getItems(femaleMothAlias).size();
                    }
                }

                // Take excess male moths
                if (state.excessMale > 0) {
                    ArrayList<WItem> maleMoths = gui.getInventory(state.container.cap).getItems(maleMothAlias);
                    ArrayList<WItem> excessMales = new ArrayList<>();
                    for (int i = 0; i < Math.min(state.excessMale, maleMoths.size()); i++) {
                        excessMales.add(maleMoths.get(i));
                    }
                    if (!excessMales.isEmpty()) {
                        new TakeWItemsFromContainer(state.container, excessMales).run(gui);
                        // Update state to reflect actual count after taking
                        new OpenTargetContainer(state.container).run(gui);
                        state.maleCount = gui.getInventory(state.container.cap).getItems(maleMothAlias).size();
                    }
                }

                new CloseTargetContainer(state.container).run(gui);
            }
        }
    }

    private void redistributeMoths(NGameUI gui, ArrayList<ContainerState> processingBatch) throws InterruptedException {
        NAlias femaleMothAlias = new NAlias("Female Silkmoth");
        NAlias maleMothAlias = new NAlias(new ArrayList<>(List.of("Male Silkmoth")), new ArrayList<>(List.of("female")));

        for (ContainerState state : processingBatch) {
            if (state.shortageFemale > 0 || state.shortageMale > 0) {
                new PathFinder(Finder.findGob(state.container.gobid)).run(gui);
                new OpenTargetContainer(state.container).run(gui);

                // Add needed female moths
                if (state.shortageFemale > 0) {
                    ArrayList<WItem> femalesInInventory = gui.getInventory().getItems(femaleMothAlias);
                    if (!femalesInInventory.isEmpty()) {
                        int toTransfer = Math.min(state.shortageFemale, femalesInInventory.size());
                        new SimpleTransferToContainer(gui.getInventory(state.container.cap), femalesInInventory, toTransfer).run(gui);
                        // Update state to reflect actual count after adding
                        state.femaleCount = gui.getInventory(state.container.cap).getItems(femaleMothAlias).size();
                    }
                }

                // Add needed male moths
                if (state.shortageMale > 0) {
                    ArrayList<WItem> malesInInventory = gui.getInventory().getItems(maleMothAlias);
                    if (!malesInInventory.isEmpty()) {
                        int toTransfer = Math.min(state.shortageMale, malesInInventory.size());
                        new SimpleTransferToContainer(gui.getInventory(state.container.cap), malesInInventory, toTransfer).run(gui);
                        // Update state to reflect actual count after adding
                        state.maleCount = gui.getInventory(state.container.cap).getItems(maleMothAlias).size();
                    }
                }

                new CloseTargetContainer(state.container).run(gui);
            }
        }
    }

    // Container state class as specified in silk_task.md
    private static class ContainerState {
        Container container;
        int femaleCount;
        int maleCount;
        int cocoonCount;
        int targetFemale;
        int targetMale;
        int maxPossiblePairs; // Maximum pairs this container can hold based on space

        // Calculated differences
        int excessFemale;
        int excessMale;
        int shortageFemale;
        int shortageMale;

        ContainerState(Container container, int femaleCount, int maleCount, int cocoonCount, int targetFemale, int targetMale) {
            this.container = container;
            this.femaleCount = femaleCount;
            this.maleCount = maleCount;
            this.cocoonCount = cocoonCount;
            this.targetFemale = targetFemale;
            this.targetMale = targetMale;
        }
    }
}