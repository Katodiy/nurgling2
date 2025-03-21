package nurgling.tasks;

import nurgling.widgets.bots.Checkable;

public class WaitCheckable extends NTask{
    Checkable widget;

    public WaitCheckable(Checkable widget) {
        this.widget = widget;
    }

    @Override
    public boolean check() {
        return widget.check();
    }
}
