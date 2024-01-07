package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NChopperProp;

public class Chopper extends Window implements Checkable {

    public String tool = null;
    CheckBox autoeat = null;
    CheckBox autorefill = null;
    CheckBox ngrowth = null;
    CheckBox stumps = null;

    UsingTools usingTools = null;
    UsingTools usingSovels = null;

    public Chopper() {
        super(new Coord(200,200), "Chopper");
        NChopperProp startprop = NChopperProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label("Chopper Settings:"));
        prev = add(stumps = new CheckBox("Uproot stumps"){
            {
                a = startprop.stumps;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                if(a)
                    usingSovels.show();
                else
                    usingSovels.hide();
            }

        }, prev.pos("bl").add(UI.scale(0,5)));
        prev = add(ngrowth = new CheckBox("Ignore the growth")
        {
            {
                a = startprop.ngrowth;
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

        prev = add(usingTools = new UsingTools(UsingTools.Tools.axes), prev.pos("bl").add(UI.scale(0,5)));
        if(startprop.tool!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.axes)
            {
                if (tl.name.equals(startprop.tool)) {
                    usingTools.s = tl;
                    break;
                }
            }

        }

        add(usingSovels = new UsingTools(UsingTools.Tools.shovels, false), prev.pos("ur").add(UI.scale(10,0)));
        if(startprop.shovel!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.shovels)
            {
                if (tl.name.equals(startprop.shovel)) {
                    usingSovels.s = tl;
                    break;
                }
            }
        }
        if(!startprop.stumps)
        {
            usingSovels.hide();
        }

        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
                prop = NChopperProp.get(NUtils.getUI().sessInfo);
                prop.autoeat = autoeat.a;
                prop.autorefill = autorefill.a;
                prop.stumps = stumps.a;
                prop.ngrowth = ngrowth.a;
                if(usingTools.s!=null)
                    prop.tool = usingTools.s.name;
                if(prop.stumps && usingSovels.s!=null)
                    prop.shovel = usingSovels.s.name;
                NChopperProp.set(prop);
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
    public NChopperProp prop = null;
}
