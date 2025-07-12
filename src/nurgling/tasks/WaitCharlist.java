package nurgling.tasks;

import nurgling.NCharlist;

public class WaitCharlist extends NTask{
    @Override
    public boolean check() {
        return NCharlist.instance != null;
    }
}
