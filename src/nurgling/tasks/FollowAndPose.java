package nurgling.tasks;

import haven.*;
import nurgling.*;

public class FollowAndPose implements NTask
{
    public FollowAndPose(Gob gob, String pose)
    {
        this.gob = gob;
        this.pose = pose;
    }


    Gob gob;
    String pose;


    @Override
    public boolean check()
    {
        String cpose = gob.pose();
        return gob.getattr(Following.class) != null && cpose != null && cpose.contains(pose);
    }
}