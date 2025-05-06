package nurgling.tasks;

import haven.WItem;
import nurgling.NUtils;

public class WaitBucketInHandContentQuantityChange extends NTask {
    String originalContents;

    public WaitBucketInHandContentQuantityChange(WItem bucket)
    {
        this.originalContents = NUtils.getContentsOfBucket(bucket);
    }

    @Override
    public boolean check() {
        WItem bucket = NUtils.getGameUI().vhand;

        if (bucket == null) {
            return false;
        }

        return !originalContents.equals(NUtils.getContentsOfBucket(bucket));
    }
}
