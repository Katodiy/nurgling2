package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NChopperProp;

import java.util.concurrent.atomic.AtomicBoolean;

public class NomadCalibrator extends Window implements Checkable {

    public String tool = null;


    public NomadCalibrator(AtomicBoolean stop) {
        super(new Coord(200,200), "Nomad");
        NChopperProp startprop = NChopperProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label("Start nomad calibration:"));

        prev = add(new Button(UI.scale(150), "Stop"){
            @Override
            public void click() {
                super.click();

                isReady = true;
                stop.set(false);
            }
        }, prev.pos("bl").add(UI.scale(0,5)));

//        prev = add(new Button(UI.scale(150), "Stop"){
//            @Override
//            public void click() {
//                super.click();
//
//                isReady = false;
//            }
//        }, prev.pos("bl").add(UI.scale(0,5)));

        pack();
    }

    @Override
    public boolean check() {
        return isReady;
    }

    boolean isReady = false;

    @Override
    public void wdgmsg(String msg, Object... args) {
        if(msg.equals("close")) {
            isReady = true;
            hide();
        }
        super.wdgmsg(msg, args);
    }
}
