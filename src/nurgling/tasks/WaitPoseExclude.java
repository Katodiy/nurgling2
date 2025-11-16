package nurgling.tasks;

import haven.Gob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WaitPoseExclude extends NTask {
    
    Gob gob;
    List<String> excludePoses;
    int timeout = 0;
    int maxTimeout = 200;
    
    public WaitPoseExclude(Gob gob, String... excludePoses) {
        this.gob = gob;
        this.excludePoses = new ArrayList<>(Arrays.asList(excludePoses));
    }
    
    public WaitPoseExclude(Gob gob, int maxTimeout, String... excludePoses) {
        this.gob = gob;
        this.excludePoses = new ArrayList<>(Arrays.asList(excludePoses));
        this.maxTimeout = maxTimeout;
    }
    
    @Override
    public boolean check() {
        if (timeout++ >= maxTimeout) {
            return true;
        }
        
        String currentPose = gob.pose();
        if (currentPose == null) {
            return true;
        }
        
        for (String excludePose : excludePoses) {
            if (currentPose.contains(excludePose)) {
                return false;
            }
        }
        
        return true;
    }
}
