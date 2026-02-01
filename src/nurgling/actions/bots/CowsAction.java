package nurgling.actions.bots;

import haven.res.gfx.hud.rosters.cow.Ochs;
import haven.res.ui.croster.Entry;
import nurgling.actions.bots.animals.AbstractHerdAction;
import nurgling.conf.CowsHerd;

public class CowsAction extends AbstractHerdAction<Ochs> {

    @Override
    protected Class<? extends Entry> getAnimalClass() {
        return Ochs.class;
    }

    @Override
    protected String getSpecialisationName() {
        return "cows";
    }

    @Override
    protected String getAliasName() {
        return "cattle";
    }

    private CowsHerd config() {
        return CowsHerd.getCurrent();
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
        return false;
    }

    @Override
    protected int getAdultFemaleLimit() {
        return config().adultCows;
    }

    // Модель-специфичные методы
    @Override
    protected boolean isMale(Ochs animal) {
        return animal.bull;
    }

    @Override
    protected boolean isNotDead(Ochs animal) {
        return !animal.dead;
    }

    @Override
    protected boolean isNotChild(Ochs animal) {
        return !animal.calf;
    }
}