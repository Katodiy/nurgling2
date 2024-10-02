package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NPrepBlocksProp;

public class PrepareBlocks extends Window implements Checkable {

    public String tool = null;

    UsingTools usingTools = null;

    public PrepareBlocks() {
        super(new Coord(200,200), "Prepare Blocks");
        NPrepBlocksProp startprop = NPrepBlocksProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label("Prepare blocks Settings:"));

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


        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
                prop = NPrepBlocksProp.get(NUtils.getUI().sessInfo);
                if(usingTools.s!=null)
                    prop.tool = usingTools.s.name;
                NPrepBlocksProp.set(prop);
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
