package nurgling.widgets.bots;

import haven.*;

public class Chopper extends Window implements Checkable {
    public Chopper() {
        super(new Coord(200,200), "Chopper");

        prev = add(new Label("Chopper Settings:"));
        prev = add(new CheckBox("Uproot stumps"){
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));
        prev = add(new CheckBox("Ignore the growth")
        {
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(new CheckBox("Auto refill water-containers")
        {
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(new CheckBox("Eat from inventory")
        {
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(new UsingTools(), prev.pos("bl").add(UI.scale(0,5)));

        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
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
}
