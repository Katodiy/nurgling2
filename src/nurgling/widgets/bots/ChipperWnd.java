package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NChipperProp;

public class ChipperWnd extends Window implements Checkable {

    public String tool = null;

    UsingTools usingTools = null;
    CheckBox plateu;
    CheckBox nopiles;
    CheckBox autorefill;
    CheckBox autoeat;

    public ChipperWnd() {
        super(new Coord(200,200), "Chipper");
        NChipperProp startprop = NChipperProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label("Chipper Settings:"));

        prev = add(usingTools = new UsingTools(UsingTools.Tools.pickaxe), prev.pos("bl").add(UI.scale(0,5)));
        if(startprop.tool!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.pickaxe)
            {
                if (tl.name.equals(startprop.tool)) {
                    usingTools.s = tl;
                    break;
                }
            }
        }

        prev = add(plateu = new CheckBox("Dig on a mountain plateau"){
            {
                a = startprop.plateu;
                usingTools.visible = !a;
                ChipperWnd.this.pack();
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));
        usingTools.visible = !startprop.plateu;

        prev = add(nopiles = new CheckBox("Drop stones"){
            {
                a = startprop.nopiles;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

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

        prev = add(autoeat = new CheckBox("Eat from inventory")
        {
            {
                a = startprop.autoeat;
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
                prop = NChipperProp.get(NUtils.getUI().sessInfo);
                if(usingTools.s!=null)
                    prop.tool = usingTools.s.name;
                NChipperProp.set(prop);
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
    public NChipperProp prop = null;
}
