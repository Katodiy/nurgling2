package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NPrepBlocksProp;
import nurgling.i18n.L10n;

public class PrepareBlocks extends Window implements Checkable {

    public String tool = null;

    UsingTools usingTools = null;
    CheckBox checkWounds = null;

    public PrepareBlocks() {
        super(new Coord(200,200), L10n.get("pblocks.wnd_title"));
        NPrepBlocksProp startprop = NPrepBlocksProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label(L10n.get("pblocks.settings")));

        prev = add(usingTools = new UsingTools(UsingTools.Tools.axes), prev.pos("bl").add(UI.scale(0,5)));
        if(startprop != null && startprop.tool!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.axes)
            {
                if (tl.name.equals(startprop.tool)) {
                    usingTools.s = tl;
                    break;
                }
            }

        }

        final boolean initialCheckWounds = startprop != null ? startprop.checkWounds : false;
        prev = add(checkWounds = new CheckBox(L10n.get("botwnd.check_wounds"))
        {
            {
                a = initialCheckWounds;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(new Button(UI.scale(150), L10n.get("botwnd.start")){
            @Override
            public void click() {
                super.click();
                prop = NPrepBlocksProp.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    if(usingTools.s!=null)
                        prop.tool = usingTools.s.name;
                    prop.checkWounds = checkWounds.a;
                    NPrepBlocksProp.set(prop);
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
    public NPrepBlocksProp prop = null;
}
