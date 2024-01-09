package nurgling.actions.bots;

import haven.Gob;
import haven.res.gfx.hud.rosters.sheep.Sheep;
import haven.res.ui.croster.CattleId;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.AnimalAction;
import nurgling.actions.Results;
import nurgling.actions.Validator;
import nurgling.areas.NArea;
import nurgling.conf.SheepsHerd;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

public class SheepsAction implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation sheeps = new NArea.Specialisation("sheeps");
        NArea.Specialisation deadkritter = new NArea.Specialisation("deadkritter");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(sheeps);
        req.add(deadkritter);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess()) {

            Comparator<Gob> comparator = new Comparator<Gob>() {
                @Override
                public int compare(Gob o1, Gob o2) {
                    if (o1.getattr(CattleId.class) != null && o2.getattr(CattleId.class) != null) {
                        Sheep p1 = (Sheep) (NUtils.getAnimalEntity(o1, Sheep.class));
                        Sheep p2 = (Sheep) (NUtils.getAnimalEntity(o2, Sheep.class));
                        return Double.compare(p2.rang(), p1.rang());
                    }
                    return 0;
                }
            };

            Predicate<Gob> wpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (SheepsHerd.getCurrent().disable_killing)
                        return false;
                    Sheep p1 = (Sheep) (NUtils.getAnimalEntity(gob, Sheep.class));
                    ;
                    return !p1.ram && !p1.dead && (!p1.lamb || !SheepsHerd.getCurrent().ignoreChildren);
                }
            };
            Predicate<Gob> mpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (SheepsHerd.getCurrent().disable_killing)
                        return false;
                    Sheep p1 = (Sheep) (NUtils.getAnimalEntity(gob, Sheep.class));
                    ;
                    return p1.ram && !p1.dead && (!p1.lamb || !SheepsHerd.getCurrent().ignoreChildren);
                }
            };

            Predicate<Gob> mlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Sheep p1 = (Sheep) (NUtils.getAnimalEntity(gob, Sheep.class));
                    ;
                    return p1.ram && !p1.dead && !p1.lamb;
                }
            };

            Predicate<Gob> wlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Sheep p1 = (Sheep) (NUtils.getAnimalEntity(gob, Sheep.class));
                    ;
                    return !p1.ram && !p1.dead && p1.lactate;
                }
            };
            if(SheepsHerd.getCurrent()!=null) {
                new AnimalAction<Sheep>(new NAlias("sheep"), "sheeps", comparator, Sheep.class, wpred, wlpred, SheepsHerd.getCurrent().adultSheeps).run(gui);
                new AnimalAction<Sheep>(new NAlias("sheep"), "sheeps", comparator, Sheep.class, mpred, mlpred, 1).run(gui);
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