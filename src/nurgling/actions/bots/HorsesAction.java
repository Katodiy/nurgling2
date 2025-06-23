package nurgling.actions.bots;

import haven.Gob;
import haven.res.gfx.hud.rosters.horse.Horse;
import haven.res.ui.croster.CattleId;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.KillAnimalsAction;
import nurgling.actions.Results;
import nurgling.actions.Validator;
import nurgling.areas.NArea;
import nurgling.conf.HorseHerd;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

public class HorsesAction implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation Horses = new NArea.Specialisation("horses");
        NArea.Specialisation deadkritter = new NArea.Specialisation("deadkritter");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(Horses);
        req.add(deadkritter);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess()) {

            Comparator<Gob> comparator = new Comparator<Gob>() {
                @Override
                public int compare(Gob o1, Gob o2) {
                    if (o1.getattr(CattleId.class) != null && o2.getattr(CattleId.class) != null) {
                        Horse p1 = (Horse) (NUtils.getAnimalEntity(o1, Horse.class));
                        Horse p2 = (Horse) (NUtils.getAnimalEntity(o2, Horse.class));
                        return Double.compare(p2.rang(), p1.rang());
                    }
                    return 0;
                }
            };

            Predicate<Gob> wpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (HorseHerd.getCurrent().disable_killing)
                        return false;
                    Horse p1 = (Horse) (NUtils.getAnimalEntity(gob, Horse.class));
                    ;
                    return !p1.stallion && !p1.dead && (!p1.foal || !HorseHerd.getCurrent().ignoreChildren);
                }
            };
            Predicate<Gob> mpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (HorseHerd.getCurrent().disable_killing)
                        return false;
                    Horse p1 = (Horse) (NUtils.getAnimalEntity(gob, Horse.class));
                    ;
                    return p1.stallion && !p1.dead && (!p1.foal || !HorseHerd.getCurrent().ignoreChildren);
                }
            };

            Predicate<Gob> mlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Horse p1 = (Horse) (NUtils.getAnimalEntity(gob, Horse.class));
                    ;
                    return p1.stallion && !p1.dead && !p1.foal;
                }
            };

            Predicate<Gob> wlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Horse p1 = (Horse) (NUtils.getAnimalEntity(gob, Horse.class));
                    ;
                    return !p1.stallion && !p1.dead && p1.lactate;
                }
            };
            if(HorseHerd.getCurrent()!=null) {
                new KillAnimalsAction<Horse>(new NAlias("horse"), "horses", comparator, Horse.class, wpred, wlpred, HorseHerd.getCurrent().adultHorse).run(gui);
                gui.msg("Female horse cycle done!");
                new KillAnimalsAction<Horse>(new NAlias("horse"), "horses", comparator, Horse.class, mpred, mlpred, 1).run(gui);
                gui.msg("Male horse cycle done!");
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