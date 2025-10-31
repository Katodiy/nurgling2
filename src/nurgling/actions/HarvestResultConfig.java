package nurgling.actions;

import nurgling.areas.NArea;
import nurgling.conf.CropRegistry;
import nurgling.tools.NAlias;

public class HarvestResultConfig {
    public final NAlias itemAlias;
    public final NArea targetArea;
    public final int priority;
    public final CropRegistry.StorageBehavior storageBehavior;

    public HarvestResultConfig(NAlias itemAlias, NArea targetArea, int priority, CropRegistry.StorageBehavior storageBehavior) {
        this.itemAlias = itemAlias;
        this.targetArea = targetArea;
        this.priority = priority;
        this.storageBehavior = storageBehavior;
    }
}
