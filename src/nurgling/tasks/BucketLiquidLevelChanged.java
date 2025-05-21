package nurgling.tasks;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.NUI;
import nurgling.NUtils;

public class BucketLiquidLevelChanged extends NTask
{
    public BucketLiquidLevelChanged(WItem res, double initLevel)
    {
        this.bucket = res;
        this.initLevel = initLevel;
    }

    WItem bucket;
    double initLevel;


    @Override
    public boolean check()
    {
        bucket = NUtils.getGameUI().vhand;
        if(((NGItem)bucket.item).content().isEmpty()) return true;
        else if(!((NGItem)bucket.item).content().isEmpty()){
            NGItem.NContent liquid_content = ((NGItem) bucket.item).content().getFirst();
            return (NUtils.parseStartDouble(liquid_content.name()) != initLevel );
            }
        else return false;
    }
}
