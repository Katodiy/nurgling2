package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NAutoFlowerActionProp;

public class AutoFlowerAction extends Window implements Checkable {

    public String action = null;
    CheckBox transfer = null;
    TextEntry actionInput = null;

    public AutoFlowerAction() {
        super(new Coord(250, 150), "Auto Flower Action");
        NAutoFlowerActionProp startprop = NAutoFlowerActionProp.get(NUtils.getUI().sessInfo);
        
        prev = add(new Label("Auto Flower Action Settings:"));
        
        prev = add(new Label("Action name:"), prev.pos("bl").add(UI.scale(0, 5)));
        
        prev = add(actionInput = new TextEntry(UI.scale(200), startprop.action) {
            @Override
            protected void changed() {
                super.changed();
                action = text();
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        prev = add(transfer = new CheckBox("Transfer items") {
            {
                a = startprop.transfer;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));

        prev = add(new Button(UI.scale(150), "Start") {
            @Override
            public void click() {
                super.click();
                prop = NAutoFlowerActionProp.get(NUtils.getUI().sessInfo);
                prop.action = actionInput.text();
                prop.transfer = transfer.a;
                NAutoFlowerActionProp.set(prop);
                isReady = true;
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        pack();
    }

    @Override
    public boolean check() {
        return isReady;
    }

    boolean isReady = false;

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            isReady = true;
            hide();
        }
        super.wdgmsg(msg, args);
    }
    
    public NAutoFlowerActionProp prop = null;
}
