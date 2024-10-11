package nurgling.tasks;

import haven.Gob;
import nurgling.tools.Finder;

public class WaitPoseOrNoGob implements NTask
{
    public WaitPoseOrNoGob(Gob gob,Gob target, String pose)
    {
        this.gob = gob;
        this.pose = pose;
        this.target = target;
    }


    Gob gob;
    Gob target;
    String pose;


    @Override
    public boolean check()
    {
        if(Finder.findGob(gob.id)==null || Finder.findGob(target.id)==null)
            return true;
        String cpose = gob.pose();
        return cpose != null && cpose.contains(pose);
    }
}