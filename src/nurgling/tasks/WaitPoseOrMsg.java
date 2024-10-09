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
    boolean isError = false;

    @Override
    public boolean check()
    {
        String cpose = gob.pose();
        String lastMsg = NUtils.getUI().getLastError();
        if((cpose != null && cpose.contains(pose)) )
            return true;
        if(lastMsg!=null && lastMsg.contains(msg))
        {
            isError = true;
            return true;
        }
        return false;
    }

    public boolean isError() {
        return isError;
    }
}