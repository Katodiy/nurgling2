package nurgling.actions.bots;

import haven.Gob;
import haven.res.gfx.hud.rosters.teimdeer.Teimdeer;
import haven.res.ui.croster.CattleId;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.conf.TeimDeerHerd;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

public class DeersAction implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation deers = new NArea.Specialisation("deer");
        NArea.Specialisation deadkritter = new NArea.Specialisation("deadkritter");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(deers);
        req.add(deadkritter);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess()) {

            Comparator<Gob> comparator = new Comparator<Gob>() {
                @Override
                public int compare(Gob o1, Gob o2) {
                    if (o1.getattr(CattleId.class) != null && o2.getattr(CattleId.class) != null) {
                        Teimdeer p1 = (Teimdeer) (NUtils.getAnimalEntity(o1, Teimdeer.class));
                        Teimdeer p2 = (Teimdeer) (NUtils.getAnimalEntity(o2, Teimdeer.class));
                        return Double.compare(p2.rang(), p1.rang());
                    }
                    return 0;
                }
            };

            Predicate<Gob> wpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (TeimDeerHerd.getCurrent().disable_killing)
                        return false;
                    Teimdeer p1 = (Teimdeer) (NUtils.getAnimalEntity(gob, Teimdeer.class));
                    ;
                    return !p1.buck && !p1.dead && (!p1.fawn || !TeimDeerHerd.getCurrent().ignoreChildren);
                }
            };
            Predicate<Gob> mpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    if (TeimDeerHerd.getCurrent().disable_killing)
                        return false;
                    Teimdeer p1 = (Teimdeer) (NUtils.getAnimalEntity(gob, Teimdeer.class));
                    ;
                    return p1.buck && !p1.dead && (!p1.fawn || !TeimDeerHerd.getCurrent().ignoreChildren);
                }
            };

            Predicate<Gob> mlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Teimdeer p1 = (Teimdeer) (NUtils.getAnimalEntity(gob, Teimdeer.class));
                    ;
                    return p1.buck && !p1.dead && !p1.fawn;
                }
            };

            Predicate<Gob> wlpred = new Predicate<Gob>() {
                @Override
                public boolean test(Gob gob) {
                    Teimdeer p1 = (Teimdeer) (NUtils.getAnimalEntity(gob, Teimdeer.class));
                    ;
                    return !p1.buck && !p1.dead && p1.lactate;
                }
            };
            if(TeimDeerHerd.getCurrent()!=null) {
                new MemorizeAnimalsAction(new NAlias("deer"),"deer", Teimdeer.class).run(gui);


                new KillAnimalsAction<Teimdeer>(new NAlias("deer"), "deer", comparator, Teimdeer.class, wpred, wlpred, TeimDeerHerd.getCurrent().adultDeers).run(gui);
                gui.msg("Female deer cycle done!");
                new KillAnimalsAction<Teimdeer>(new NAlias("deer"), "deer", comparator, Teimdeer.class, mpred, mlpred, 1).run(gui);
                gui.msg("Male deer cycle done!");
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