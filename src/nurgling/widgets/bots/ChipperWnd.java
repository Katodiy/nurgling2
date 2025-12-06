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
        if (startprop == null) startprop = new NChipperProp("", "");
        final NChipperProp finalStartprop = startprop;
        prev = add(new Label("Chipper Settings:"));

        prev = add(plateu = new CheckBox("Dig on a mountain plateau"){
            {
                a = finalStartprop.plateu;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                if(!a)
                    usingTools.show();
                else
                    usingTools.hide();
            }

        }, prev.pos("bl").add(UI.scale(0,5)));


        prev = add(nopiles = new CheckBox("Drop stones"){
            {
                a = finalStartprop.nopiles;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(autorefill = new CheckBox("Auto refill water-containers")
        {
            {
                a = finalStartprop.autorefill;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(autoeat = new CheckBox("Eat from inventory")
        {
            {
                a = finalStartprop.autoeat;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(usingTools = new UsingTools(UsingTools.Tools.pickaxe), prev.pos("bl").add(UI.scale(0,5)));
        if(finalStartprop.tool!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.pickaxe)
            {
                if (tl.name.equals(finalStartprop.tool)) {
                    usingTools.s = tl;
                    break;
                }
            }
        }
        usingTools.visible = !finalStartprop.plateu;


        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
                prop = NChipperProp.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    if(usingTools.s!=null)
                        prop.tool = usingTools.s.name;
                    prop.autoeat = autoeat.a;
                    prop.plateu = plateu.a;
                    prop.autorefill = autorefill.a;
                    prop.nopiles = nopiles.a;
                    NChipperProp.set(prop);
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
    public NChipperProp prop = null;
}
