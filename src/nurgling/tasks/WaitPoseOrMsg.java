package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;

import static haven.OptWnd.PointBind.msg;

public class WaitPoseOrMsg implements NTask
{
    public WaitPoseOrMsg(Gob gob, String pose, String msg)
    {
        this.gob = gob;
        this.pose = pose;
        this.msg = msg;
    }


    Gob gob;
    String pose;
    String msg;


    @Override
    public boolean check()
    {
        String cpose = gob.pose();
        String lastMsg = NUtils.getUI().getLastError();
        return (cpose != null && cpose.contains(pose)) || (lastMsg!=null && lastMsg.contains(msg));
    }
}