package nurgling.actions.bots.registry;

public class BotDescriptor {
    public final String key;
    public final String displayName;
    public final String iconPath;
    public final String description;
    public final BotRegistry.BotFactory factory;

    public BotDescriptor(String key, String displayName, String iconPath, String description, BotRegistry.BotFactory factory) {
        this.key = key;
        this.displayName = displayName;
        this.iconPath = iconPath;
        this.description = description;
        this.factory = factory;
    }
}
