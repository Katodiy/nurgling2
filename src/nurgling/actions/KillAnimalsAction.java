package nurgling.actions;

import haven.Gob;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.AnimalIsDead;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

public class KillAnimalsAction<C extends Entry> implements Action {
    private final NAlias animal;
    private final String type;
    private final Class<? extends Entry> cattleRoster;
    private final Comparator<Gob> comp;
    private final Predicate<Gob> pred;
    private final Predicate<Gob> forpred;
    private final int flcount;

    public KillAnimalsAction(NAlias animal, String type, Comparator<Gob> comp,
                             Class<? extends Entry> cattleRoster, Predicate<Gob> pred) {
        this(animal, type, comp, cattleRoster, pred, null, 0);
    }

    public KillAnimalsAction(NAlias animal, String type, Comparator<Gob> comp,
                             Class<? extends Entry> cattleRoster, Predicate<Gob> pred,
                             Predicate<Gob> forpred, int flcount) {
        this.animal = animal;
        this.type = type;
        this.cattleRoster = cattleRoster;
        this.comp = comp;
        this.pred = pred;
        this.forpred = forpred;
        this.flcount = flcount;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea current = NContext.findSpec(this.type);
        if (current == null)
            return Results.ERROR("No animal area set. (Sheeps, Cows, Pigs, Goats in area specialization)");

        ArrayList<Gob> gobs = Finder.findGobs(current, animal);
        if (gobs.isEmpty()) {
            return Results.ERROR("Area (" + animal.getDefault() + ") has no animals. Check that it covers animals pen.");
        }

        ArrayList<Gob> targets = new ArrayList<>();
        for (Gob gob : gobs) {
            if (gob.getattr(CattleId.class) != null && pred.test(gob)) {
                targets.add(gob);
            }
        }
        targets.sort(comp);

        ArrayList<Gob> forlife = new ArrayList<>();
        ArrayList<Gob> forkill = new ArrayList<>();

        if (forpred != null) {
            int c = 0;
            Gob last = null;
            for (Gob gob : targets) {
                if (forpred.test(gob)) {
                    if (c < flcount) {
                        forlife.add(gob);
                        c++;
                        last = gob;
                    }
                }
            }
            if(forlife.size()<flcount) {
                for (Gob gob : targets) {
                    if (!forlife.contains(gob)) {
                        if (c < flcount) {
                            forlife.add(gob);
                            c++;
                            last = gob;
                        }
                    }
                }
            }
            for (Gob gob : targets) {
                if (!forlife.contains(gob) && last != null && comp.compare(last, gob) <= 0)
                    forkill.add(gob);
            }
        } else {
            forkill.addAll(targets);
        }

        while (!forkill.isEmpty()) {
            kill(forkill, gui);
        }

        return Results.SUCCESS();
    }

    private void kill(ArrayList<Gob> forkill, NGameUI gui) throws InterruptedException {
        if (forkill.isEmpty())
            return;
        forkill.sort(NUtils.d_comp);
        Gob target = forkill.get(0);
        boolean res = false;
        while (!res) {
            new DynamicPf(target).run(gui);
            new SelectFlowerAction("Slaughter", target).run(gui);
            AnimalIsDead aid = new AnimalIsDead(target);
            NUtils.getUI().core.addTask(aid);
            res = aid.getRes();
        }
        new LiftObject(target).run(gui);
        new FindPlaceAndAction(target, NContext.findSpec("deadkritter")).run(gui);
        Collection<Object> args = new ArrayList<>();
        args.add(((CattleId) target.getattr(CattleId.class)).entry().id);
        NUtils.getRosterWindow(cattleRoster).roster(cattleRoster).wdgmsg("rm", args.toArray(new Object[0]));
        forkill.remove(target);
    }
}