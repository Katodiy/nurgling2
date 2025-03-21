package nurgling.tasks;

import haven.Following;
import haven.Gob;
import nurgling.NUtils;

public class WaitPlob extends NTask {


    public WaitPlob() {

    }


    @Override
    public boolean check() {
        return NUtils.getGameUI().map!=null && NUtils.getGameUI().map.placing!= null && NUtils.getGameUI().map.placing.ready();
    }

}


