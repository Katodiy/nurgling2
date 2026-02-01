package nurgling.actions.bots;

import haven.res.gfx.hud.rosters.goat.Goat;
import haven.res.ui.croster.Entry;
import nurgling.actions.bots.animals.AbstractHerdAction;
import nurgling.conf.GoatsHerd;

public class GoatsAction extends AbstractHerdAction<Goat> {

    @Override
    protected Class<? extends Entry> getAnimalClass() {
        return Goat.class;
    }

    @Override
    protected String getSpecialisationName() {
        return "goats";
    }

    @Override
    protected String getAliasName() {
        return "goat";
    }

    private GoatsHerd config() {
        return GoatsHerd.getCurrent();
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
        return !config().skipMilking;
    }

    @Override
    protected boolean isShearingEnabled() {
        return !config().skipShearing;
    }

    @Override
    protected int getAdultFemaleLimit() {
        return config().adultGoats;
    }

    @Override
    protected boolean isMale(Goat animal) {
        return animal.billy;
    }

    @Override
    protected boolean isNotDead(Goat animal) {
        return !animal.dead;
    }

    @Override
    protected boolean isNotChild(Goat animal) {
        return !animal.kid;
    }
}