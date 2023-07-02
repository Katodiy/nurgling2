package nurgling;

import haven.*;

public class NUI extends UI {
    public NCore core;
    public NUI(Context uictx, Coord sz, Runner fun) {
        super(uictx, sz, fun);
        root.add(core = new NCore());
        bind(core, 7001);
    }
}
