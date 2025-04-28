package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NHarvestCropProp;

public class HarvestCropWnd extends Window implements Checkable {

    CheckBox autorefill;

    public HarvestCropWnd() {
        super(new Coord(200,200), "Harvest Crop");
        NHarvestCropProp startprop = NHarvestCropProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label("Harvest Settings:"));

        prev = add(autorefill = new CheckBox("Auto refill water-containers")
        {
            {
                a = startprop.autorefill;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));


        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
                prop = NHarvestCropProp.get(NUtils.getUI().sessInfo);
                prop.autorefill = autorefill.a;
                NHarvestCropProp.set(prop);
                isReady = true;
            }
        }, prev.pos("bl").add(UI.scale(0,5)));
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
    public NHarvestCropProp prop = null;
}
