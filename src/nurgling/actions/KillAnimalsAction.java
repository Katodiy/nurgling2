package nurgling.actions;

import haven.Gob;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import nurgling.NGameUI;
import nurgling.NUtils;
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
            // Select best adults (those matching forpred) up to flcount
            for (Gob gob : targets) {
                if (forpred.test(gob)) {
                    if (c < flcount) {
                        forlife.add(gob);
                        c++;
                        last = gob;
                    }
                }
            }
            // Kill all animals that are worse than the worst kept adult
            // This preserves young females that are better than the worst adult female
            for (Gob gob : targets) {
                if (!forlife.contains(gob) && last != null && comp.compare(last, gob) <= 0)
                    forkill.add(gob);
            }
        } else {
            forkill.addAll(targets);
        }

        new Equip(new NAlias("Butcher's cleaver"), new NAlias("Traveller's Sack", "Wanderer's Bindle")).run(gui);
        new Equip(new NAlias("Traveller's Sack", "Wanderer's Bindle"), new NAlias("Butcher's cleaver")).run(gui);

        // Mark animals for kill in roster (red highlight)
        for(Gob gob : forkill) {
            CattleId cattleId = gob.getattr(CattleId.class);
            if(cattleId != null && cattleId.entry() != null) {
                Entry.killList.add(cattleId.entry().id);
            }
        }

        try {
            while (!forkill.isEmpty()) {
                kill(forkill, gui);
            }
        } finally {
            // Clear kill list when done
            Entry.killList.clear();
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
        new FindPlaceAndAction(target, NContext.findSpec("deadkritter"), true).run(gui);
        CattleId cattleId = (CattleId) target.getattr(CattleId.class);
        Collection<Object> args = new ArrayList<>();
        args.add(cattleId.entry().id);
        // Remove from kill list highlight
        Entry.killList.remove(cattleId.entry().id);
        NUtils.getRosterWindow(cattleRoster).roster(cattleRoster).wdgmsg("rm", args.toArray(new Object[0]));
        forkill.remove(target);
    }
}