package nurgling.actions.bots.registry;

import nurgling.actions.Action;
import nurgling.i18n.L10n;

import java.util.Map;

public class BotDescriptor {
    final static String ICON_BASE_DIR = "nurgling/bots/icons/";

    public final String id;
    public final BotType type;
    public final String titleKey;      // Localization key for title
    public final String descriptionKey; // Localization key for description
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

    public BotDescriptor(String id, BotType type, String titleKey, String descriptionKey, boolean allowedAsStepInScenario, boolean allowedAsItemInBotMenu, Class<? extends Action> clazz, String iconPath, boolean disStacks) {
        this.id = id;
        this.type = type;
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
        this.allowedAsStepInScenario = allowedAsStepInScenario;
        this.allowedAsItemInBotMenu = allowedAsItemInBotMenu;
        this.clazz = clazz;
        this.iconPath = iconPath;
        this.disStacks = disStacks;
    }

    /**
     * Get localized display name.
     * If titleKey starts with "bot." it's treated as a localization key.
     * Otherwise returns the raw value (for backwards compatibility).
     */
    public String getDisplayName() {
        if (titleKey != null && titleKey.startsWith("bot.")) {
            return L10n.get(titleKey);
        }
        return titleKey;
    }

    /**
     * Get localized description.
     * If descriptionKey starts with "bot." it's treated as a localization key.
     * Otherwise returns the raw value (for backwards compatibility).
     */
    public String getDescription() {
        if (descriptionKey != null && descriptionKey.startsWith("bot.")) {
            return L10n.get(descriptionKey);
        }
        return descriptionKey;
    }

    /**
     * @deprecated Use getDisplayName() instead for localized text
     */
    @Deprecated
    public String displayName() {
        return getDisplayName();
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
