package nurgling.actions.bots.registry;

import nurgling.actions.Action;

import java.util.Map;

public class BotDescriptor {
    final static String ICON_BASE_DIR = "nurgling/bots/icons/";

    public final String id;
    public final BotType type;
    public final String displayName;
    public final String description;
    public final boolean allowedAsStepInScenario;
    public final boolean allowedAsItemInBotMenu;
    public final Class<? extends Action> clazz;
    public final String iconPath;
    public final boolean disStacks;

    public enum BotType {
        RESOURCES,
        PRODUCTIONS,
        BATTLE,
        FARMING,
        FARMING_QUALITY,
        LIVESTOCK,
        UTILS,
        BUILD,
        TOOLS
    }

    public BotDescriptor(String id, BotType type, String displayName,  String description, boolean allowedAsStepInScenario, boolean allowedAsItemInBotMenu, Class<? extends Action> clazz, String iconPath, boolean disStacks) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.allowedAsStepInScenario = allowedAsStepInScenario;
        this.allowedAsItemInBotMenu = allowedAsItemInBotMenu;
        this.clazz = clazz;
        this.iconPath = iconPath;
        this.disStacks = disStacks;
    }

    public Action instantiate(Map<String, Object> settings) {
        try {
            try {
                return clazz.getDeclaredConstructor(Map.class).newInstance(settings);
            } catch (NoSuchMethodException e) {
                return clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUpIconPath() {
        return getIconPath("u");
    }

    public String getDownIconPath() {
        return getIconPath("d");
    }

    public String getHoverIconPath() {
        return getIconPath("h");
    }

    public String getIconPath(String state) {
        return ICON_BASE_DIR + iconPath + "/" + state;
    }
}
