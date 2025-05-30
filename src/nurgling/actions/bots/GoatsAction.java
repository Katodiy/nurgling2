package nurgling.actions.bots;

import haven.Gob;
import haven.res.gfx.hud.rosters.goat.Goat;
import haven.res.ui.croster.CattleId;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.AnimalAction;
import nurgling.actions.Results;
import nurgling.actions.Validator;
import nurgling.areas.NArea;
import nurgling.conf.GoatsHerd;
import nurgling.tools.NAlias;
import nurgling.widgets.settings.Goats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

public class GoatsAction implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation goats = new NArea.Specialisation("goats");
        NArea.Specialisation deadkritter = new NArea.Specialisation("deadkritter");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(goats);
        req.add(deadkritter);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess()) {

            Comparator<Gob> comparator = new Comparator<Gob>() {
                @Override
                public int compare(Gob o1, Gob o2) {
                    if (o1.getattr(CattleId.class) != null && o2.getattr(CattleId.class) != null) {
                        Goat p1 = (Goat) (NUtils.getAnimalEntity(o1, Goat.class));
                        Goat p2 = (Goat) (NUtils.getAnimalEntity(o2, Goat.class));
                        return Double.compare(p2.rang(), p1.rang());
                    }
                    return 0;
                }
            };

            Predicate<Gob> wpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (GoatsHerd.getCurrent().disable_killing)
                        return false;
                    Goat p1 = (Goat) (NUtils.getAnimalEntity(gob, Goat.class));
                    ;
                    return !p1.billy && !p1.dead && (!p1.kid || !GoatsHerd.getCurrent().ignoreChildren);
                }
            };
            Predicate<Gob> mpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (GoatsHerd.getCurrent().disable_killing)
                        return false;
                    Goat p1 = (Goat) (NUtils.getAnimalEntity(gob, Goat.class));
                    ;
                    return p1.billy && !p1.dead && (!p1.kid || !GoatsHerd.getCurrent().ignoreChildren);
                }
            };

            Predicate<Gob> mlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Goat p1 = (Goat) (NUtils.getAnimalEntity(gob, Goat.class));
                    ;
                    return p1.billy && !p1.dead && !p1.kid;
                }
            };

            Predicate<Gob> wlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Goat p1 = (Goat) (NUtils.getAnimalEntity(gob, Goat.class));
                    ;
                    return !p1.billy && !p1.dead && p1.lactate;
                }
            };
            if(GoatsHerd.getCurrent()!=null) {
                new AnimalAction<Goat>(new NAlias("goat"), "goats", comparator, Goat.class, wpred, wlpred, GoatsHerd.getCurrent().adultGoats).run(gui);
                gui.msg("Female goats cycle done!");
                new AnimalAction<Goat>(new NAlias("goat"), "goats", comparator, Goat.class, mpred, mlpred, 1).run(gui);
                gui.msg("Male goats cycle done!");
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