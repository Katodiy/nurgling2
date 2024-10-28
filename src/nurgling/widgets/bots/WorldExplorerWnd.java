package nurgling.widgets.bots;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.conf.NWorldExplorerProp;

public class WorldExplorerWnd extends Window implements Checkable {

    public String tool = null;

    UsingTools usingTools = null;
    CheckBox clockwise;
    CheckBox unclockwise;
    CheckBox deep;
    CheckBox shallow;

    public WorldExplorerWnd() {
        super(new Coord(200,200), "WorldExplorer");
        NWorldExplorerProp startprop = NWorldExplorerProp.get(NUtils.getUI().sessInfo);
        prev = add(new Label("World Explorer Settings:"));

        prev = add(clockwise = new CheckBox("Clockwise direction"){
            {
                a = startprop.clockwise;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                unclockwise.a = !a;
            }

        }, prev.pos("bl").add(UI.scale(0,5)));


        prev = add(unclockwise = new CheckBox("Counterclockwise direction"){
            {
                a = !startprop.clockwise;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                clockwise.a = !a;
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(deep = new CheckBox("Deep and Deeper")
        {
            {
                a = startprop.deeper;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                shallow.a = !a;
            }

        }, prev.pos("bl").add(UI.scale(0,5)));

        prev = add(shallow = new CheckBox("Deep and Shallow")
        {
            {
                a = !startprop.deeper;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
                deep.a = !a;
            }

        }, prev.pos("bl").add(UI.scale(0,5)));


        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
                prop = NWorldExplorerProp.get(NUtils.getUI().sessInfo);
                prop.deeper = deep.a;
                prop.clockwise = clockwise.a;
                NWorldExplorerProp.set(prop);
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
