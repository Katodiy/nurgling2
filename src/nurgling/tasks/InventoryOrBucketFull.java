package nurgling.tasks;

import haven.Coord;
import haven.WItem;
import nurgling.NUtils;

import static nurgling.NUtils.getGameUI;

public class InventoryOrBucketFull extends NTask {

    @Override
    public boolean check() {

        WItem bucket;

        try {
            bucket = NUtils.getEquipment().findBucket("Empty");
            if (bucket == null) bucket = NUtils.getEquipment().findBucket("Honey");
            if (bucket == null) return false;
        } catch (InterruptedException e) {
            getGameUI().msg("No bucket found!");
            return false;
        }

        String contentsOfBucket = NUtils.getContentsOfBucket(bucket);

        return NUtils.getGameUI().getInventory().calcNumberFreeCoord(new Coord(1, 2)) == 0 ||
                contentsOfBucket.contains("10.0");
    }
}