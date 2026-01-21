package nurgling.widgets;

import nurgling.NConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages custom icons - loading, saving, and accessing them.
 * Icons are stored in custom_icons.nurgling.json.
 */
public class CustomIconManager {
    private static CustomIconManager instance;
    private final Map<String, CustomIcon> icons = new LinkedHashMap<>();

    public CustomIconManager() {
        loadIcons();
    }

    public static CustomIconManager getInstance() {
        if (instance == null) {
            instance = new CustomIconManager();
        }
        return instance;
    }

    public void loadIcons() {
        icons.clear();
        File file = new File(NConfig.current.getCustomIconsPath());
        if (file.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.getCustomIconsPath()), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (IOException ignore) {}

            if (!contentBuilder.toString().isEmpty()) {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = main.getJSONArray("icons");
                for (int i = 0; i < array.length(); i++) {
                    CustomIcon icon = new CustomIcon(array.getJSONObject(i));
                    icons.put(icon.getId(), icon);
                }
            }
        }
    }

    public void writeIcons() {
        JSONObject main = new JSONObject();
        JSONArray jicons = new JSONArray();
        for (CustomIcon icon : icons.values()) {
            jicons.put(icon.toJson());
        }
        main.put("icons", jicons);

        try {
            FileWriter f = new FileWriter(NConfig.current.getCustomIconsPath(), StandardCharsets.UTF_8);
            main.write(f);
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addOrUpdateIcon(CustomIcon icon) {
        icons.put(icon.getId(), icon);
        writeIcons();
    }

    public void deleteIcon(String iconId) {
        icons.remove(iconId);
        writeIcons();
    }

    public CustomIcon getIcon(String id) {
        return icons.get(id);
    }

    public Map<String, CustomIcon> getIcons() {
        return icons;
    }

    public List<CustomIcon> getIconList() {
        return new ArrayList<>(icons.values());
    }
}
