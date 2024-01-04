package nurgling.actions;


import haven.Gob;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.overlays.NTestRing;
import nurgling.tasks.AnimalInRoster;
import nurgling.tasks.AnimalIsDead;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

public class AnimalAction <C extends Entry> implements Action {
    @Override
    public Results run(NGameUI gui)
            throws InterruptedException {
        NArea current = NArea.findSpec(this.type);
        if(current==null)
            return Results.ERROR("NO ANIMAL AREA");

        RosterWindow w = NUtils.getRosterWindow(cattleRoster);
        ArrayList<Gob> gobs = Finder.findGobs(current,animal);
        ArrayList<Gob> targets = new ArrayList<>();

        while(memorize(gobs,gui,w,cattleRoster));

        for (Gob gob : gobs) {
            if (gob.getattr(CattleId.class) != null) {
                if (pred.test(gob)) {
                    targets.add(gob);
                }
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
                    if (c <flcount) {
                        forlife.add(gob);
                        c++;
                        last = gob;
                    }
                }
            }

            for (Gob gob : targets) {
                if(!forlife.contains(gob) && comp.compare(last,gob)<=0)
                    forkill.add(gob);
            }
        }


        while (!forkill.isEmpty()) {
            kill(forkill,gui);
        }

        return Results.SUCCESS();
    }



    void kill(ArrayList<Gob> forkill, NGameUI gui) throws InterruptedException {
        if(forkill.isEmpty())
            return;
        forkill.sort(NUtils.d_comp);
        Gob target = forkill.get(0);
        new PathFinder(target).run(gui);
        new SelectFlowerAction("Slaughter", target ).run(gui);
        NUtils.getUI().core.addTask(new AnimalIsDead(target));
        new LiftObject(target).run(gui);
        new FindPlaceAndAction(target,NArea.findSpec("deadkritter")).run(gui);
        Collection<Object> args = new ArrayList<>();
        args.add(Integer.valueOf((int) (((CattleId) target.getattr(CattleId.class)).entry().id & 0x00000000ffffffffl)));
        args.add(Integer.valueOf((int) ((((CattleId) target.getattr(CattleId.class)).entry().id & 0xffffffff00000000l) >> 32)));
        NUtils.getRosterWindow(cattleRoster).roster(cattleRoster).wdgmsg("rm", args.toArray(new Object[0]));
        forkill.remove(target);
    }

    public <C extends Entry> AnimalAction(NAlias animal, String type, Comparator<Gob> wc, Class<C> c, Predicate<Gob> pred, int count) {
        this.animal = animal;
        this.type = type;
        cattleRoster = c;
        comp = wc;
        this.pred = pred;
    }
    public <C extends Entry> AnimalAction(NAlias animal,  String type, Comparator<Gob> wc, Class<C> c, Predicate<Gob> pred, Predicate<Gob> forpred, int flcount) {
        this.animal = animal;
        this.type = type;
        cattleRoster = c;
        comp = wc;
        this.pred = pred;
        this.forpred = forpred;
        this.flcount = flcount;
    }


    NAlias animal;

    String type;

    Class<? extends Entry> cattleRoster;
    Comparator<Gob> comp = null;
    Predicate<Gob> pred = null;
    Predicate<Gob> forpred = null;
    int flcount = 0;

    public static boolean memorize(ArrayList<Gob> gobs, NGameUI gui, RosterWindow w, Class<? extends Entry> cattleRoster) throws InterruptedException {
        gobs.sort(NUtils.d_comp);
        for (Gob gob : gobs) {
            if (gob.getattr(CattleId.class) == null && gob.pose()!=null && !NParser.checkName(gob.pose(),"knocked")) {
                new PathFinder(gob).run(gui);
                new SelectFlowerAction("Memorize", gob).run(gui);
                NUtils.getUI().core.addTask(new AnimalInRoster(gob, cattleRoster, w));
                return true;
            }
        }
        return false;
    }
}
