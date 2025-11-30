package nurgling.tasks;

public class WaitTicks extends NTask {
    private int ticksToWait;
    private int currentTick = 0;
    
    public WaitTicks(int ticks) {
        this.ticksToWait = ticks;
        this.infinite = true;
    }
    
    @Override
    public boolean check() {
        currentTick++;
        return currentTick >= ticksToWait;
    }
}
