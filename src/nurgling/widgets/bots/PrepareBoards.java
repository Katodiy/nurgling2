package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NPrepBoardsProp;
import nurgling.i18n.L10n;

public class PrepareBoards extends Window implements Checkable {

    public String tool = null;

    UsingTools usingTools = null;

    public PrepareBoards() {
        super(new Coord(200,200), L10n.get("pboards.wnd_title"));
        NPrepBoardsProp startprop = NPrepBoardsProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label(L10n.get("pboards.settings")));

        prev = add(usingTools = new UsingTools(UsingTools.Tools.saw), prev.pos("bl").add(UI.scale(0,5)));
        if(startprop != null && startprop.tool!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.saw)
            {
                if (tl.name.equals(startprop.tool)) {
                    usingTools.s = tl;
                    break;
                }
            }

        }


        prev = add(new Button(UI.scale(150), L10n.get("botwnd.start")){
            @Override
            public void click() {
                super.click();
                prop = NPrepBoardsProp.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    if(usingTools.s!=null)
                        prop.tool = usingTools.s.name;
                    NPrepBoardsProp.set(prop);
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
    public NPrepBoardsProp prop = null;
}
