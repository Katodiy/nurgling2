package nurgling.tasks;

import haven.Following;
import haven.Gob;

public class WaitPose implements NTask
{
    public WaitPose(Gob gob, String pose)
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
        return cpose != null && cpose.contains(pose);
    }
}