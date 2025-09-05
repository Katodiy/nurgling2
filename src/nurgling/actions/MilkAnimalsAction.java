package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.res.gfx.hud.rosters.cow.Ochs;
import haven.res.gfx.hud.rosters.goat.Goat;
import haven.res.gfx.hud.rosters.sheep.Sheep;
import haven.res.ui.croster.CattleId;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitPose;
import nurgling.tasks.WaitSound;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NArea;
import nurgling.areas.NContext;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

public class MilkAnimalsAction implements Action {
    
    private final NAlias animalAlias;
    private String animalAreaName;
    private String milkAreaName;
    private Predicate<Gob> lactatingPredicate;
    private final NAlias milkContent = new NAlias("Milk");

    public MilkAnimalsAction(NAlias animalAlias) {
        this.animalAlias = animalAlias;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        initBot(animalAlias);

        // Get areas from context
        NArea animalArea = NContext.findSpec(animalAreaName);
        if (animalArea == null) {
            return Results.ERROR("Animal area '" + animalAreaName + "' not found in specializations");
        }
        
        NArea milkArea = NContext.findSpec("cistern", milkAreaName);
        if (milkArea == null) {
            return Results.ERROR("Milk area '" + milkAreaName + "' not found in specializations");
        }
        
        // Find required containers
        Gob barrel = Finder.findGob(milkArea, new NAlias("barrel"));
        if (barrel == null) {
            return Results.ERROR("Barrel not found in milk area");
        }
        
        Gob cistern = Finder.findGob(milkArea, new NAlias("cistern"));
        if (cistern == null) {
            return Results.ERROR("Cistern not found in milk area");
        }
        
        // Find lactating animals
        ArrayList<Gob> lactatingAnimals = findLactatingAnimals(animalArea);
        if (lactatingAnimals.isEmpty()) {
            return Results.SUCCESS(); // No animals to milk, but not an error
        }
        
        // Lift barrel and ensure it has space/is empty
        Coord2d barrelOriginalPos = barrel.rc;
        new LiftObject(barrel).run(gui);
        
        // Check if barrel contains milk - if so, empty it into cistern first
        if (NUtils.isOverlay(barrel, milkContent)) {
            // Barrel has milk - empty it into cistern
            new PathFinder(cistern).run(gui);
            NUtils.activateGob(cistern);
            
            // Wait for barrel to become empty (no longer has milk overlay)
            NUtils.getUI().core.addTask(new NTask() {
                @Override
                public boolean check() {
                    return !NUtils.isOverlay(barrel, milkContent);
                }
            });
        }
        
        // Milk animals in batches
        int milkCount = 0;
        while (!lactatingAnimals.isEmpty()) {
            Gob targetAnimal = findClosestAnimal(lactatingAnimals);
            lactatingAnimals.remove(targetAnimal);
            
            // Navigate to and milk the animal
            new DynamicPf(targetAnimal).run(gui);
            NUtils.activateGob(targetAnimal);

            NUtils.getUI().core.addTask(new WaitSound("sfx/fx/water"));

            milkCount++;
            
            // Empty barrel every 4 milking
            int BATCH_SIZE = 4;
            if (milkCount >= BATCH_SIZE) {
                // Navigate to cistern and empty barrel
                new PathFinder(cistern).run(gui);
                NUtils.activateGob(cistern);
                
                // Wait for barrel to empty
                NUtils.getUI().core.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return !NUtils.isOverlay(barrel, milkContent);
                    }
                });
                
                milkCount = 0;
            }
        }
        
        // Final check - if barrel has milk, empty it into cistern before placing back
        if (NUtils.isOverlay(barrel, milkContent)) {
            new PathFinder(cistern).run(gui);
            NUtils.activateGob(cistern);
            
            // Wait for barrel to empty
            NUtils.getUI().core.addTask(new NTask() {
                @Override
                public boolean check() {
                    return !NUtils.isOverlay(barrel, milkContent);
                }
            });
        }
        
        // Place barrel back
        new PlaceObject(barrel, barrelOriginalPos, 0).run(gui);
        
        return Results.SUCCESS();
    }
    
    private ArrayList<Gob> findLactatingAnimals(NArea animalArea) throws InterruptedException {
        ArrayList<Gob> animals = Finder.findGobs(animalArea, animalAlias);
        ArrayList<Gob> lactatingAnimals = new ArrayList<>();
        
        // Only include animals that are in the roster (have CattleId) and match our predicate
        for (Gob animal : animals) {
            if (animal.getattr(CattleId.class) != null && lactatingPredicate.test(animal)) {
                lactatingAnimals.add(animal);
            }
        }
        
        // Sort by distance for efficiency
        lactatingAnimals.sort(NUtils.d_comp);
        
        return lactatingAnimals;
    }
    
    private Gob findClosestAnimal(ArrayList<Gob> animals) {
        if (animals.isEmpty()) return null;
        
        Gob closest = animals.get(0);
        double closestDist = NUtils.getGameUI().map.player().rc.dist(closest.rc);
        
        for (Gob animal : animals) {
            double dist = NUtils.getGameUI().map.player().rc.dist(animal.rc);
            if (dist < closestDist) {
                closest = animal;
                closestDist = dist;
            }
        }
        
        return closest;
    }

    public void initBot(NAlias animalAlias) {
        if (Objects.equals(animalAlias.getKeys().getFirst(), "cattle")) {
            this.animalAreaName = "cows";
            this.milkAreaName = "Cow Milk";
            this.lactatingPredicate = createCowLactatingPredicate();
        } else if (Objects.equals(animalAlias.getKeys().getFirst(), "goat")) {
            this.animalAreaName = "goats";
            this.milkAreaName = "Goat Milk";
            this.lactatingPredicate = createGoatLactatingPredicate();
        } else if (Objects.equals(animalAlias.getKeys().getFirst(), "sheep")) {
            this.animalAreaName = "sheeps";
            this.milkAreaName = "Sheep Milk";
            this.lactatingPredicate = createSheepLactatingPredicate();
        }
    }

    // Static helper methods for creating common animal milking predicates
    public static Predicate<Gob> createCowLactatingPredicate() {
        return gob -> {
            Ochs cow = (Ochs) NUtils.getAnimalEntity(gob, Ochs.class);
            return cow != null && !cow.bull && !cow.dead && cow.lactate;
        };
    }
    
    public static Predicate<Gob> createSheepLactatingPredicate() {
        return gob -> {
            Sheep sheep = (Sheep) NUtils.getAnimalEntity(gob, Sheep.class);
            return sheep != null && !sheep.ram && !sheep.dead && sheep.lactate;
        };
    }
    
    public static Predicate<Gob> createGoatLactatingPredicate() {
        return gob -> {
            Goat goat = (Goat) NUtils.getAnimalEntity(gob, Goat.class);
            return goat != null && !goat.billy && !goat.dead && goat.lactate;
        };
    }

}