package nurgling.actions.bots.registry;

public class Setting {
    public final String name;
    public final Class<?> type;

    public Setting(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }
}

