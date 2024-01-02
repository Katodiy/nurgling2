package nurgling.actions;

import haven.Gob;
import haven.res.gfx.hud.rosters.cow.Ochs;
import haven.res.ui.croster.CattleId;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.conf.CowsHerd;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

public class CowsAction implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation cows = new NArea.Specialisation("cows");
        NArea.Specialisation deadkritter = new NArea.Specialisation("deadkritter");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cows);
        req.add(deadkritter);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess()) {

            Comparator<Gob> comparator = new Comparator<Gob>() {
                @Override
                public int compare(Gob o1, Gob o2) {
                    if (o1.getattr(CattleId.class) != null && o2.getattr(CattleId.class) != null) {
                        Ochs p1 = (Ochs) (NUtils.getAnimalEntity(o1, Ochs.class));
                        ;
                        Ochs p2 = (Ochs) (NUtils.getAnimalEntity(o2, Ochs.class));
                        ;
                        return Double.compare(p2.rang(), p1.rang());
                    }
                    return 0;
                }
            };

            Predicate<Gob> wpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (CowsHerd.getCurrent().disable_killing)
                        return false;
                    Ochs p1 = (Ochs) (NUtils.getAnimalEntity(gob, Ochs.class));
                    ;
                    return !p1.bull && !p1.dead && (!p1.calf || !CowsHerd.getCurrent().ignoreChildren);
                }
            };
            Predicate<Gob> mpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (CowsHerd.getCurrent().disable_killing)
                        return false;
                    Ochs p1 = (Ochs) (NUtils.getAnimalEntity(gob, Ochs.class));
                    ;
                    return p1.bull && !p1.dead && (!p1.calf || !CowsHerd.getCurrent().ignoreChildren);
                }
            };

            Predicate<Gob> mlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Ochs p1 = (Ochs) (NUtils.getAnimalEntity(gob, Ochs.class));
                    ;
                    return p1.bull && !p1.dead && !p1.calf;
                }
            };

            Predicate<Gob> wlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Ochs p1 = (Ochs) (NUtils.getAnimalEntity(gob, Ochs.class));
                    ;
                    return !p1.bull && !p1.dead && p1.lactate;
                }
            };
            if(CowsHerd.getCurrent()!=null) {
                new AnimalAction<Ochs>(new NAlias("cattle"), "cows", comparator, Ochs.class, wpred, wlpred, CowsHerd.getCurrent().adultCows).run(gui);
                new AnimalAction<Ochs>(new NAlias("cattle"), "cows", comparator, Ochs.class, mpred, mlpred, 1).run(gui);
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