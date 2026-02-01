package nurgling.actions.bots;

import haven.res.gfx.hud.rosters.pig.Pig;
import haven.res.ui.croster.Entry;
import nurgling.actions.bots.animals.AbstractHerdAction;
import nurgling.conf.PigsHerd;

public class PigsAction extends AbstractHerdAction<Pig> {

    @Override
    protected Class<? extends Entry> getAnimalClass() {
        return Pig.class;
    }

    @Override
    protected String getSpecialisationName() {
        return "pigs";
    }

    @Override
    protected String getAliasName() {
        return "pig";
    }

    private PigsHerd config() {
        return PigsHerd.getCurrent();
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
        return config().adultPigs;
    }

    @Override
    protected boolean isMale(Pig animal) {
        return animal.hog;
    }

    @Override
    protected boolean isNotDead(Pig animal) {
        return !animal.dead;
    }

    @Override
    protected boolean isNotChild(Pig animal) {
        return !animal.piglet;
    }
}