package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NChopperProp;
import nurgling.i18n.L10n;

public class Chopper extends Window implements Checkable {

    public String tool = null;
    CheckBox autoeat = null;
    CheckBox autorefill = null;
    CheckBox ngrowth = null;
    CheckBox stumps = null;
    CheckBox bushes = null;
    CheckBox checkWounds = null;

    UsingTools usingTools = null;
    UsingTools usingSovels = null;

    public Chopper() {
        super(new Coord(200,200), L10n.get("chopper.wnd_title"));
        NChopperProp startprop = NChopperProp.get(NUtils.getUI().sessInfo);
        if (startprop == null) startprop = new NChopperProp("", "");
        final NChopperProp finalStartprop = startprop;
        prev = add(new Label(L10n.get("chopper.settings")));
        prev = add(stumps = new CheckBox(L10n.get("chopper.uproot_stumps")){
            {
                a = finalStartprop.stumps;
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

        prev = add(bushes = new CheckBox(L10n.get("chopper.cut_bushes")){
            {
                a = finalStartprop.bushes;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));


        prev = add(ngrowth = new CheckBox(L10n.get("chopper.ignore_growth"))
        {
            {
                a = finalStartprop.ngrowth;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(autorefill = new CheckBox(L10n.get("botwnd.autorefill"))
        {
            {
                a = finalStartprop.autorefill;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(autoeat = new CheckBox(L10n.get("botwnd.autoeat"))
        {
            {
                a = finalStartprop.autoeat;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(checkWounds = new CheckBox(L10n.get("botwnd.check_wounds"))
        {
            {
                a = finalStartprop.checkWounds;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(usingTools = new UsingTools(UsingTools.Tools.axes), prev.pos("bl").add(UI.scale(0,5)));
        if(finalStartprop.tool!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.axes)
            {
                if (tl.name.equals(finalStartprop.tool)) {
                    usingTools.s = tl;
                    break;
                }
            }

        }

        add(usingSovels = new UsingTools(UsingTools.Tools.shovels, false), usingTools.pos("ur").add(UI.scale(10,usingTools.l.sz.y)));
        if(finalStartprop.shovel!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.shovels)
            {
                if (tl.name.equals(finalStartprop.shovel)) {
                    usingSovels.s = tl;
                    break;
                }
            }
        }
        if(!finalStartprop.stumps)
        {
            usingSovels.hide();
        }

        prev = add(new Button(UI.scale(150), L10n.get("botwnd.start")){
            @Override
            public void click() {
                super.click();
                prop = NChopperProp.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    prop.autoeat = autoeat.a;
                    prop.autorefill = autorefill.a;
                    prop.stumps = stumps.a;
                    prop.ngrowth = ngrowth.a;
                    prop.bushes = bushes.a;
                    prop.checkWounds = checkWounds.a;
                    if(usingTools.s!=null)
                        prop.tool = usingTools.s.name;
                    if(prop.stumps && usingSovels.s!=null)
                        prop.shovel = usingSovels.s.name;
                    NChopperProp.set(prop);
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
    public NChopperProp prop = null;
}
