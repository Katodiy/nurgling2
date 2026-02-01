package nurgling.actions.bots;

import haven.res.gfx.hud.rosters.sheep.Sheep;
import nurgling.actions.bots.animals.AbstractHerdAction;
import nurgling.conf.SheepsHerd;

public class SheepsAction extends AbstractHerdAction<Sheep> {

    @Override
    protected Class<Sheep> getAnimalClass() {
        return Sheep.class;
    }

    @Override
    protected String getSpecialisationName() {
        return "sheeps";
    }

    @Override
    protected String getAliasName() {
        return "sheep";
    }

    private SheepsHerd config() {
        return SheepsHerd.getCurrent();
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
        return config().adultSheeps;
    }

    @Override
    protected boolean isMale(Sheep animal) {
        return animal.ram;
    }

    @Override
    protected boolean isNotDead(Sheep animal) {
        return !animal.dead;
    }

    @Override
    protected boolean isNotChild(Sheep animal) {
        return !animal.lamb;
    }
}