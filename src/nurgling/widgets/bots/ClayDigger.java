package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NClayDiggerProp;
import nurgling.i18n.L10n;

public class ClayDigger extends Window implements Checkable {

    public String tool = null;
    UsingTools usingSovels = null;

    public ClayDigger() {
        super(new Coord(200,200), L10n.get("clay.wnd_title"));
        NClayDiggerProp startprop = NClayDiggerProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label(L10n.get("clay.settings")));

        prev = add(usingSovels = new UsingTools(UsingTools.Tools.shovels, true), prev.pos("bl").add(UI.scale(0,5)));
        if(startprop != null && startprop.shovel!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.shovels)
            {
                if (tl.name.equals(startprop.shovel)) {
                    usingSovels.s = tl;
                    break;
                }
            }
        }

        prev = add(new Button(UI.scale(150), L10n.get("botwnd.start")){
            @Override
            public void click() {
                super.click();
                prop = NClayDiggerProp.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    if(usingSovels.s!=null)
                        prop.shovel = usingSovels.s.name;
                    NClayDiggerProp.set(prop);
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
    public NClayDiggerProp prop = null;
}
