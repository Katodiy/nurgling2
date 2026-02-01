package nurgling.actions.bots.animals;

import haven.Gob;
import haven.res.gfx.hud.rosters.Rangable;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.actions.bots.GotoArea;
import nurgling.actions.bots.ShearWool;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.AnimalRangLoad;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

public abstract class AbstractHerdAction<T extends Rangable> implements Action {

    private NArea animalArea;

    protected abstract Class<? extends Entry> getAnimalClass();

    protected abstract String getSpecialisationName();

    protected abstract String getAliasName();

    protected abstract boolean isDisableKilling();

    protected abstract boolean isIgnoreChildren();

    protected abstract boolean isMilkingEnabled();

    protected abstract boolean isShearingEnabled();

    protected abstract int getAdultFemaleLimit();

    protected void onMilkingDone(NGameUI gui) {
        gui.msg("Milking cycle done!");
    }

    protected void onShearingDone(NGameUI gui) {
        gui.msg("Shearing cycle done!");
    }

    protected void onFemaleCycleDone(NGameUI gui) {
        gui.msg("Female cows cycle done!");
    }

    protected void onMaleCycleDone(NGameUI gui) {
        gui.msg("Male cows cycle done!");
    }

    protected abstract boolean isMale(T animal);

    protected abstract boolean isNotDead(T animal);

    protected abstract boolean isNotChild(T animal);

    protected double getRang(T animal) {
        return animal.rang();
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation herd = new NArea.Specialisation(getSpecialisationName());
        NArea.Specialisation deadkritter = new NArea.Specialisation("deadkritter");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(herd);
        req.add(deadkritter);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if (!new Validator(req, opt).run(gui).IsSuccess()) {
            return Results.FAIL();
        }

        this.animalArea = NContext.findSpec(getSpecialisationName());

        Comparator<Gob> comparator = createRangComparator();
        Predicate<Gob> wpred = createFemalePredicate();
        Predicate<Gob> mpred = createMalePredicate();
        Predicate<Gob> mlpred = createAdultMalePredicate();
        Predicate<Gob> wlpred = createAdultFemalePredicate();

        // load animals rang before comparator
        NUtils.addTask(new AnimalRangLoad(NContext.findSpec(getSpecialisationName()), new NAlias(getAliasName()), getAnimalClass()));

        // if we have dead animals in area
        new MoveDeadAnimals().run(gui);

        new MemorizeAnimalsAction(new NAlias(getAliasName()), getSpecialisationName(), getAnimalClass()).run(gui);

        if (isMilkingEnabled()) {
            new MilkAnimalsAction(new NAlias(getAliasName())).run(gui);
            onMilkingDone(gui);
        }

        if (isShearingEnabled()) {
            new ShearWool(
                    nurgling.widgets.Specialisation.SpecName.valueOf(getSpecialisationName()),
                    new NAlias(getAliasName())
            ).run(gui);
            onShearingDone(gui);
        }

        new KillAnimalsAction<>(new NAlias(getAliasName()), getSpecialisationName(), comparator, getAnimalClass(), wpred, wlpred, getAdultFemaleLimit(), animalArea).run(gui);
        onFemaleCycleDone(gui);

        new KillAnimalsAction<>(new NAlias(getAliasName()), getSpecialisationName(), comparator, getAnimalClass(), mpred, mlpred, 1, animalArea).run(gui);
        onMaleCycleDone(gui);

        return Results.SUCCESS();
    }

    private T getAnimal(Gob gob) {
        return (T) NUtils.getAnimalEntity(gob, getAnimalClass());
    }

    protected Comparator<Gob> createRangComparator() {
        return (o1, o2) -> {
            if (o1.getattr(CattleId.class) != null && o2.getattr(CattleId.class) != null) {
                T a1 = getAnimal(o1);
                T a2 = getAnimal(o2);
                return Double.compare(getRang(a2), getRang(a1)); // по убыванию
            }
            return 0;
        };
    }

    protected Predicate<Gob> createFemalePredicate() {
        return gob -> {
            if (isDisableKilling()) return false;
            T a = getAnimal(gob);
            return !isMale(a) && isNotDead(a) && (isNotChild(a) || !isIgnoreChildren());
        };
    }

    protected Predicate<Gob> createMalePredicate() {
        return gob -> {
            if (isDisableKilling()) return false;
            T a = getAnimal(gob);
            return isMale(a) && isNotDead(a) && (isNotChild(a) || !isIgnoreChildren());
        };
    }

    protected Predicate<Gob> createAdultMalePredicate() {
        return gob -> {
            T a = getAnimal(gob);
            return isMale(a) && isNotDead(a) && isNotChild(a);
        };
    }

    protected Predicate<Gob> createAdultFemalePredicate() {
        return gob -> {
            T a = getAnimal(gob);
            return !isMale(a) && isNotDead(a) && isNotChild(a);
        };
    }

    protected class MoveDeadAnimals implements Action {
        @Override
        public Results run(NGameUI gui) throws InterruptedException {
            ArrayList<Gob> gobs = Finder.findGobs(animalArea, new NAlias(getAliasName()));

            if (gobs.isEmpty()) {
                return Results.SUCCESS();
            }

            ArrayList<Gob> targets = new ArrayList<>();
            for (Gob gob : gobs) {
                T animal = getAnimal(gob);
                if (animal != null && gob.getattr(CattleId.class) != null && !isNotDead(animal)) {
                    targets.add(gob);
                }
                if (animal == null && gob.pose().contains("knock")) {
                    targets.add(gob);
                }
            }
            targets.sort(NUtils.d_comp);

            for (Gob target : targets) {
                new GotoArea(animalArea).run(gui);
                new DynamicPf(target).run(gui);
                new LiftObject(target).run(gui);
                new FindPlaceAndAction(target, NContext.findSpec("deadkritter"), true).run(gui);

                CattleId cattleId = target.getattr(CattleId.class);
                if (cattleId != null) {
                    Collection<Object> args = new ArrayList<>();
                    args.add(cattleId.entry().id);
                    NUtils.getRosterWindow(getAnimalClass()).roster(getAnimalClass()).wdgmsg("rm", args.toArray(new Object[0]));
                }
            }
            return Results.SUCCESS();
        }
    }
}