package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;

public class WaitPlayerPose extends NTask
{
    public WaitPlayerPose(String pose)
    {
        this.pose = pose;
    }

    int count = 0;
    Gob gob;
    String pose;


    @Override
    public boolean check()
    {
        this.gob = NUtils.player();

        if(this.gob == null) {
            return false;
        }

        String cpose = gob.pose();
        if(count++ >= 200 &&  cpose != null && cpose.contains("gfx/borka/idle"))
            return true;
        return cpose != null && cpose.contains(pose);
    }
}