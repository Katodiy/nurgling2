package nurgling.actions.bots.registry;

import nurgling.NGameUI;
import nurgling.actions.Action;

public class BotDescriptor {
    public final String key;
    public final String displayName;
    public final String iconPath;
    public final String description;
    public final BotFactory factory;

    public BotDescriptor(String key, String displayName, String iconPath, String description, BotFactory factory) {
        this.key = key;
        this.displayName = displayName;
        this.iconPath = iconPath;
        this.description = description;
        this.factory = factory;
    }

    public interface BotFactory {
        Action create();
    }
}
