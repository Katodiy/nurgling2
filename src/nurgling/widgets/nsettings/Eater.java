package nurgling.widgets.nsettings;

import haven.CheckBox;
import haven.UI;
import nurgling.NConfig;
import nurgling.widgets.FoodContainer;

public class Eater extends Panel {

//    boolean tempUseRope = false;
//    CheckBox ropeAfterFeeding;
    FoodContainer fc;
    public Eater() {
        super("Eating Bot");
        add(fc = new FoodContainer(), UI.scale(5,30));
        //        ropeAfterFeeding = add(new CheckBox("Tie the animal on a rope after feeding it") {
        //            public void set(boolean val) {
        //                tempUseRope = val;
        //                a = val;
        //            }
        //        });


    }

    @Override
    public void load() {
        fc.load();
//        tempUseRope = (Boolean) NConfig.get(NConfig.Key.ropeAfterFeeding);
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.eatingConf, fc.getFoodsJson());
    }

}