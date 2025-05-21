package nurgling.tasks;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.NUtils;

public class BucketLiquidLevelChangedTo extends NTask
{
    public BucketLiquidLevelChangedTo(WItem res, double filledToLevel)
    {
        this.bucket = res;
        this.filledToLevel = filledToLevel;
    }

    NInventory inventory;
    WItem bucket;
    double filledToLevel;


    @Override
    public boolean check()
    {
        if( bucket.item != null && !((NGItem)bucket.item).content().isEmpty()){
            NGItem.NContent liquid_content = ((NGItem) bucket.item).content().getFirst();
            return (!((NGItem)bucket.item).content().isEmpty() && NUtils.parseStartDouble(liquid_content.name()) == filledToLevel );
        } else return false;
    }
}
