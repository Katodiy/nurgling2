package nurgling.tasks;

import nurgling.NUtils;
import nurgling.tasks.NTask;

public class ISRemoved extends NTask {
    int id;
    public ISRemoved(int id) {
        super();
        this.id = id;
    }

    @Override
    public boolean check() {
        return NUtils.getUI().getwidget(id)==null;
    }
}
