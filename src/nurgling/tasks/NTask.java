package nurgling.tasks;

public abstract class NTask {
    public boolean baseCheck() {
        if (infinite) {
            return check();
        }

        if (counter++ >= maxCounter) {
            if (!ignoreCriticalExit) {
                criticalExit = true;
            }
            done = false;
            return true;
        }

        return check();
    }

    public abstract boolean check();

    public boolean getResult() {
        return done;
    }

    protected boolean done = false;
    public boolean criticalExit = false;
    public boolean ignoreCriticalExit = true;
    protected int counter = 0;
    protected int maxCounter = 200;
    protected boolean infinite = true;
}
