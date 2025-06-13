package nurgling.actions.bots.registry;

public class BotDescriptor {
    public final String key;
    public final int order;
    public final BotType type;
    public final String displayName;
    public final String iconPath;
    public final String description;
    public final BotRegistry.BotFactory factory;

    public enum BotType {
        FARMING,
        LIVESTOCK,
        LABORING,
        UTILS
    }

    public BotDescriptor(String key, int order, BotType type, String displayName, String iconPath, String description, BotRegistry.BotFactory factory) {
        this.key = key;
        this.order = order;
        this.type = type;
        this.displayName = displayName;
        this.iconPath = iconPath;
        this.description = description;
        this.factory = factory;
    }
}
