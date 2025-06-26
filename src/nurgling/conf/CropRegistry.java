package nurgling.conf;

import nurgling.tools.NAlias;

import java.util.*;

public class CropRegistry {

    public enum StorageBehavior { BARREL, STOCKPILE }

    public static class CropStage {
        public final int stage;
        public final NAlias result;
        public final StorageBehavior storageBehavior;

        public CropStage(int stage, NAlias result, StorageBehavior storageBehavior) {
            this.stage = stage;
            this.result = result;
            this.storageBehavior = storageBehavior;
        }
    }

    public static final Map<NAlias, List<CropStage>> HARVESTABLE = new HashMap<>();

    static {
        // Turnip
        HARVESTABLE.put(
                new NAlias("plants/turnip"),
                Arrays.asList(
                        new CropStage(1, new NAlias("Turnip Seeds"), StorageBehavior.BARREL),
                        new CropStage(3, new NAlias("Turnip"), StorageBehavior.STOCKPILE)
                )
        );

        // Carrot
        HARVESTABLE.put(
                new NAlias("plants/carrot"),
                Arrays.asList(
                        new CropStage(3, new NAlias("Carrot Seeds"), StorageBehavior.BARREL),
                        new CropStage(4, new NAlias("Carrot"), StorageBehavior.STOCKPILE)
                )
        );

        // Beetroot
        HARVESTABLE.put(
                new NAlias("plants/beet"),
                Arrays.asList(
                        new CropStage(3, new NAlias("Beetroot"), StorageBehavior.STOCKPILE)
                )
        );

        // Red Onion
        HARVESTABLE.put(
                new NAlias("plants/redonion"),
                Arrays.asList(
                        new CropStage(3, new NAlias("Red Onion"), StorageBehavior.STOCKPILE)
                )
        );

        // Yellow Onion
        HARVESTABLE.put(
                new NAlias("plants/yellowonion"),
                Arrays.asList(
                        new CropStage(3, new NAlias("Yellow Onion"), StorageBehavior.STOCKPILE)
                )
        );

        // Garlic
        HARVESTABLE.put(
                new NAlias("plants/garlic"),
                Arrays.asList(
                        new CropStage(4, new NAlias("Garlic"), StorageBehavior.STOCKPILE)
                )
        );

        // Hemp
        HARVESTABLE.put(
                new NAlias("plants/hemp"),
                Arrays.asList(
                        new CropStage(4, new NAlias("Hemp Seeds"), StorageBehavior.BARREL)
                )
        );

        // Flax
        HARVESTABLE.put(
                new NAlias("plants/flax"),
                Arrays.asList(
                        new CropStage(3, new NAlias("Flax Seeds"), StorageBehavior.BARREL)
                )
        );

        // Lettuce
        HARVESTABLE.put(
                new NAlias("plants/lettuce"),
                Arrays.asList(
                        new CropStage(4, new NAlias("Lettuce Seeds"), StorageBehavior.BARREL)
                )
        );

        // Pumpkin
        HARVESTABLE.put(
                new NAlias("plants/pumpkin"),
                Arrays.asList(
                        new CropStage(4, new NAlias("Pumpkin Seeds"), StorageBehavior.BARREL)
                )
        );

        // Barley
        HARVESTABLE.put(
                new NAlias("plants/barley"),
                Arrays.asList(
                        new CropStage(3, new NAlias("Barley Seeds"), StorageBehavior.BARREL)
                )
        );

        // Millet
        HARVESTABLE.put(
                new NAlias("plants/millet"),
                Arrays.asList(
                        new CropStage(3, new NAlias("Millet Seeds"), StorageBehavior.BARREL)
                )
        );

        // Wheat
        HARVESTABLE.put(
                new NAlias("plants/wheat"),
                Arrays.asList(
                        new CropStage(3, new NAlias("Wheat Seeds"), StorageBehavior.BARREL)
                )
        );

        // Poppy
        HARVESTABLE.put(
                new NAlias("plants/poppy"),
                Arrays.asList(
                        new CropStage(4, new NAlias("Poppy Seeds"), StorageBehavior.BARREL)
                )
        );

        // Pipeweed
        HARVESTABLE.put(
                new NAlias("plants/pipeweed"),
                Arrays.asList(
                        new CropStage(4, new NAlias("Pipeweed Seeds"), StorageBehavior.BARREL)
                )
        );
    }
}
