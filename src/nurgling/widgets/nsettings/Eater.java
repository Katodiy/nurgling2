package nurgling.widgets.nsettings;

import haven.UI;
import nurgling.NConfig;
import nurgling.widgets.FoodContainer;

public class Eater extends Panel {

    FoodContainer fc;
    public Eater() {
        super("Eating Bot");
        int margin = UI.scale(5);
        add(fc = new FoodContainer(), UI.scale(margin,30));
    }

    @Override
    public void load() {
        fc.load();
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.eatingConf, fc.getFoodsJson());
    }

}