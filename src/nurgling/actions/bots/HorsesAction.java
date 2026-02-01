package nurgling.actions.bots;

import haven.res.gfx.hud.rosters.horse.Horse;
import haven.res.ui.croster.Entry;
import nurgling.actions.bots.animals.AbstractHerdAction;
import nurgling.conf.HorseHerd;

public class HorsesAction extends AbstractHerdAction<Horse> {

    @Override
    protected Class<? extends Entry> getAnimalClass() {
        return Horse.class;
    }

    @Override
    protected String getSpecialisationName() {
        return "horses";
    }

    @Override
    protected String getAliasName() {
        return "horse";
    }

    private HorseHerd config() {
        return HorseHerd.getCurrent();
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
        return false;
    }

    @Override
    protected boolean isShearingEnabled() {
        return false;
    }

    @Override
    protected int getAdultFemaleLimit() {
        return config().adultHorse;
    }

    @Override
    protected boolean isMale(Horse animal) {
        return animal.stallion;
    }

    @Override
    protected boolean isNotDead(Horse animal) {
        return !animal.dead;
    }

    @Override
    protected boolean isNotChild(Horse animal) {
        return !animal.foal;
    }
}