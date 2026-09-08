package nurgling.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Herbalist Table drying times from the Ring of Brodgar wiki
 * (Herbalist Table page, Time table).
 */
public final class HTableTimesCatalog {
    public static final class Entry {
        public final String item;
        public final String product;
        public final String realTime;
        public final String inGameTime;

        Entry(String item, String product, String realTime, String inGameTime) {
            this.item = item;
            this.product = product;
            this.realTime = realTime;
            this.inGameTime = inGameTime;
        }
    }

    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry("Bat Wings", "Dried Batwings", "21:53:04", "72:00:00"),
            new Entry("Boiled Pepper Drupes", "Dried Pepper Drupes", "36:28:27", "120:00:00"),
            new Entry("Camomile", "Dried Camomile", "58:21:31", "192:00:00"),
            new Entry("Fresh Hemp Bud", "Cured Hemp Bud", "36:28:27", "120:00:00"),
            new Entry("Fresh Leaf of Pipeweed", "Cured Pipeweed", "36:28:27", "120:00:00"),
            new Entry("Grapes", "Raisins", "00:50:27", "02:46:00"),
            new Entry("Green Tea Leaves", "Black Tea Leaves", "14:35:23", "48:00:00"),
            new Entry("Morels", "Dried Morels", "29:10:46", "96:00:00"),
            new Entry("Seeds of Barley", "Seeds of Sprouted Barley", "14:35:23", "48:00:00"),
            new Entry("Seeds of Wheat", "Seeds of Sprouted Wheat", "14:35:23", "48:00:00"),
            new Entry("Seeds of Millet", "Seeds of Sprouted Millet", "14:35:23", "48:00:00"),
            new Entry("Silkworm Egg", "Silkworm", "07:17:41", "24:00:00"),
            new Entry("Seasponge", "Sponge", "29:11:00", "96:00:00"),
            new Entry("Tea Leaves", "Green Tea Leaves", "00:36:28", "02:00:00"),
            new Entry("Treeplanter's Pot", "Sprouted Sapling", "01:12:57", "04:00:00"),
            new Entry("Wild Windsown Weed", "Seed", "02:25:54", "08:00:00"),
            new Entry("Wet Pearl Glue", "Pearl Glue", "51:03:00", "168:00:00")
    ));

    private HTableTimesCatalog() {
    }

    public static List<Entry> all() {
        return ENTRIES;
    }

    public static Entry find(String item) {
        if (item == null)
            return null;
        for (Entry entry : ENTRIES) {
            if (entry.item.equals(item))
                return entry;
        }
        return null;
    }
}
