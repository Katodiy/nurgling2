package nurgling.tasks;

public abstract class NTask
{
    public boolean baseCheck()
    {
        if(!infinite)
        {
            if(counter++ >=maxCounter)
            {
                criticalExit = true;
                return true;
            }
        }
        return check();
    }
    public abstract boolean check();

    public boolean criticalExit = false;
    protected int counter = 0;
    protected int maxCounter = 200;
    protected boolean infinite = false;
}
