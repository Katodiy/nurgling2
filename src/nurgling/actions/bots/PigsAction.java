package nurgling.actions.bots;

import haven.Gob;
import haven.res.gfx.hud.rosters.pig.Pig;
import haven.res.ui.croster.CattleId;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.AnimalAction;
import nurgling.actions.Results;
import nurgling.actions.Validator;
import nurgling.areas.NArea;
import nurgling.conf.PigsHerd;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

public class PigsAction implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation Pigs = new NArea.Specialisation("pigs");
        NArea.Specialisation deadkritter = new NArea.Specialisation("deadkritter");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(Pigs);
        req.add(deadkritter);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess()) {

            Comparator<Gob> comparator = new Comparator<Gob>() {
                @Override
                public int compare(Gob o1, Gob o2) {
                    if (o1.getattr(CattleId.class) != null && o2.getattr(CattleId.class) != null) {
                        Pig p1 = (Pig) (NUtils.getAnimalEntity(o1, Pig.class));
                        Pig p2 = (Pig) (NUtils.getAnimalEntity(o2, Pig.class));
                        return Double.compare(p2.rang(), p1.rang());
                    }
                    return 0;
                }
            };

            Predicate<Gob> wpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (PigsHerd.getCurrent().disable_killing)
                        return false;
                    Pig p1 = (Pig) (NUtils.getAnimalEntity(gob, Pig.class));
                    ;
                    return !p1.hog && !p1.dead && (!p1.piglet || !PigsHerd.getCurrent().ignoreChildren);
                }
            };
            Predicate<Gob> mpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (PigsHerd.getCurrent().disable_killing)
                        return false;
                    Pig p1 = (Pig) (NUtils.getAnimalEntity(gob, Pig.class));
                    ;
                    return p1.hog && !p1.dead && (!p1.piglet || !PigsHerd.getCurrent().ignoreChildren);
                }
            };

            Predicate<Gob> mlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Pig p1 = (Pig) (NUtils.getAnimalEntity(gob, Pig.class));
                    ;
                    return p1.hog && !p1.dead && !p1.piglet;
                }
            };

            Predicate<Gob> wlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Pig p1 = (Pig) (NUtils.getAnimalEntity(gob, Pig.class));
                    ;
                    return !p1.hog && !p1.dead && p1.lactate;
                }
            };
            if(PigsHerd.getCurrent()!=null) {
                new AnimalAction<Pig>(new NAlias("pig"), "pigs", comparator, Pig.class, wpred, wlpred, PigsHerd.getCurrent().adultPigs).run(gui);
                new AnimalAction<Pig>(new NAlias("pig"), "pigs", comparator, Pig.class, mpred, mlpred, 1).run(gui);
            }
            else
            {
                NUtils.getGameUI().error("Please select rang settings");
            }
            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
}