package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NCarrierProp;

public class Carrier extends Window implements Checkable {

    TextEntry textEntry;

    public Carrier() {
        super(new Coord(200,200), "Carrier");
        NCarrierProp startprop = NCarrierProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label("Carrier Settings:"));
        prev = add(textEntry = new TextEntry(300,startprop.object == null ? "" : startprop.object), prev.pos("bl").add(UI.scale(2,5)));
        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
                prop = NCarrierProp.get(NUtils.getUI().sessInfo);
                prop.object = textEntry.text();
                NCarrierProp.set(prop);
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
    public NCarrierProp prop = null;
}
