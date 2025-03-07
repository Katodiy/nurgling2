package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import static haven.OptWnd.PointBind.msg;

public class WaitPoseOrMsg extends NTask
{
    public WaitPoseOrMsg(Gob gob, String pose, NAlias msg)
    {
        this.gob = gob;
        this.pose = pose;
        this.msg = msg;
    }


    Gob gob;
    String pose;
    NAlias msg;
    boolean isError = false;
    int count = 0;
    @Override
    public boolean check()
    {
        count++;
        String cpose = gob.pose();
        String lastMsg = NUtils.getUI().getLastError();
        if((cpose != null && cpose.contains(pose)) )
            return true;
        if((lastMsg!=null && NParser.checkName(lastMsg, msg)) || count > 300)
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