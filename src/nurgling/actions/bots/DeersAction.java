package nurgling.actions.bots;

import haven.res.gfx.hud.rosters.teimdeer.Teimdeer;
import haven.res.ui.croster.Entry;
import nurgling.actions.bots.animals.AbstractHerdAction;
import nurgling.conf.TeimDeerHerd;

public class DeersAction extends AbstractHerdAction<Teimdeer> {

    @Override
    protected Class<? extends Entry> getAnimalClass() {
        return Teimdeer.class;
    }

    @Override
    protected String getSpecialisationName() {
        return "deer";
    }

    @Override
    protected String getAliasName() {
        return "deer";
    }

    private TeimDeerHerd config() {
        return TeimDeerHerd.getCurrent();
    }

    @Override
    protected boolean isDisableKilling() {
        return config().disable_killing;
    }

    @Override
    protected boolean isIgnoreChildren() {
        return config().ignoreChildren;
    }

    @Override
    protected boolean isMilkingEnabled() {
        return false; // олени не доятся
    }

    @Override
    protected boolean isShearingEnabled() {
        return false; // шерсть не стригут
    }

    @Override
    protected int getAdultFemaleLimit() {
        return config().adultDeers;
    }

    @Override
    protected boolean isMale(Teimdeer animal) {
        return animal.buck;
    }

    @Override
    protected boolean isNotDead(Teimdeer animal) {
        return !animal.dead;
    }

    @Override
    protected boolean isNotChild(Teimdeer animal) {
        return !animal.fawn;
    }
}