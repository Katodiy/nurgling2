package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;

public class WaitAutoFlowerActionState extends NTask {
    
    Gob player;
    int freeSpaceThreshold;
    boolean checkInventory;
    
    public enum State {
        WORKING,
        IDLE,
        NEED_TRANSFER
    }
    
    State state = State.WORKING;
    
    public WaitAutoFlowerActionState(Gob player, boolean checkInventory, int freeSpaceThreshold) {
        this.player = player;
        this.checkInventory = checkInventory;
        this.freeSpaceThreshold = freeSpaceThreshold;
    }
    
    @Override
    public boolean check() {
        String currentPose = player.pose();
        
        if (currentPose != null && currentPose.contains("idle")) {
            state = State.IDLE;
            return true;
        }
        
        if (checkInventory && NUtils.getGameUI().getInventory().calcFreeSpace()!=-1 && NUtils.getGameUI().getInventory().calcFreeSpace() < freeSpaceThreshold) {
            state = State.NEED_TRANSFER;
            return true;
        }
        
        return false;
    }
    
    public State getState() {
        return state;
    }
}
