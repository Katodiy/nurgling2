package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NCarrierProp;
import nurgling.i18n.L10n;

public class Carrier extends Window implements Checkable {

    TextEntry textEntry;

    public Carrier() {
        super(new Coord(200,200), L10n.get("carrier.wnd_title"));
        NCarrierProp startprop = NCarrierProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label(L10n.get("carrier.object_name")));
        prev = add(textEntry = new TextEntry(300, startprop == null || startprop.object == null ? "" : startprop.object), prev.pos("bl").add(UI.scale(0,5)));
        prev = add(new Label(L10n.get("carrier.hint")), prev.pos("bl").add(UI.scale(0, 2)));
        prev = add(new Button(UI.scale(150), L10n.get("botwnd.start")){
            @Override
            public void click() {
                super.click();
                prop = NCarrierProp.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    prop.object = textEntry.text();
                    NCarrierProp.set(prop);
                }
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
