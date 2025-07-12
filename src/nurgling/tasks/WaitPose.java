package nurgling.tasks;

import haven.Gob;

public class WaitPose extends NTask
{
    public WaitPose(Gob gob, String pose)
    {
        this.gob = gob;
        this.pose = pose;
    }

    int count = 0;
    Gob gob;
    String pose;


    @Override
    public boolean check()
    {

        String cpose = gob.pose();
        if(count++ >= 200 &&  cpose != null && cpose.contains("gfx/borka/idle"))
            return true;
        return cpose != null && cpose.contains(pose);
    }
}