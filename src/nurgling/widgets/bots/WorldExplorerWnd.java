package nurgling.widgets.bots;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.conf.NWorldExplorerProp;
import nurgling.i18n.L10n;

public class WorldExplorerWnd extends Window implements Checkable {

    public String tool = null;

    UsingTools usingTools = null;
    CheckBox clockwise;
    CheckBox unclockwise;
    CheckBox deep;
    CheckBox shallow;

    public WorldExplorerWnd() {
        super(new Coord(200,200), L10n.get("explorer.wnd_title"));
        NWorldExplorerProp startprop = NWorldExplorerProp.get(NUtils.getUI().sessInfo);
        if (startprop == null) startprop = new NWorldExplorerProp("", "");
        final NWorldExplorerProp finalStartprop = startprop;
        prev = add(new Label(L10n.get("explorer.settings")));

        prev = add(clockwise = new CheckBox(L10n.get("explorer.clockwise")){
            {
                a = finalStartprop.clockwise;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                unclockwise.a = !a;
            }

        }, prev.pos("bl").add(UI.scale(0,5)));


        prev = add(unclockwise = new CheckBox(L10n.get("explorer.counterclockwise")){
            {
                a = !finalStartprop.clockwise;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                clockwise.a = !a;
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(deep = new CheckBox(L10n.get("explorer.deep_deeper"))
        {
            {
                a = finalStartprop.deeper;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                shallow.a = !a;
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(shallow = new CheckBox(L10n.get("explorer.deep_shallow"))
        {
            {
                a = !finalStartprop.deeper;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                deep.a = !a;
            }

        }, prev.pos("bl").add(UI.scale(0,5)));


        prev = add(new Button(UI.scale(150), L10n.get("botwnd.start")){
            @Override
            public void click() {
                super.click();
                prop = NWorldExplorerProp.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    prop.deeper = deep.a;
                    prop.clockwise = clockwise.a;
                    NWorldExplorerProp.set(prop);
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
    public NWorldExplorerProp prop = null;
}
