package nurgling;

import haven.*;

public class NUI extends UI {
    public NUI(Context uictx, Coord sz, Runner fun) {
        super(uictx, sz, fun);
        if(fun!=null) {
            root.add(core = new NCore());
            bind(core, 7001);
        }
    }
}
