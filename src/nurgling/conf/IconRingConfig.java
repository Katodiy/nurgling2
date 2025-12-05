package nurgling.conf;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores ring settings for GobIcons locally in AppData
 */
public class IconRingConfig {
    private final Path configFile;
    private Map<String, Boolean> ringSettings = new HashMap<>();
    
    public IconRingConfig(String genus) {
        // Save to %APPDATA%\Haven and Hearth\data\{genus}\nurgling-icon-rings.json
        String appdata = System.getenv("APPDATA");
        if (appdata == null) {
            appdata = System.getProperty("user.home");
        }
        
        this.configFile = Paths.get(appdata, "Haven and Hearth", "data", genus, "nurgling-icon-rings.json");
        load();
    }
    
    /**
     * Load ring settings from local file
     */
    private void load() {
        try {
            if (Files.exists(configFile)) {
                String content = new String(Files.readAllBytes(configFile));
                JSONObject json = new JSONObject(content);
                
                for (String key : json.keySet()) {
                    ringSettings.put(key, json.getBoolean(key));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Save ring settings to local file
     */
    public void save() {
        try {
            // Create directory if needed
            Files.createDirectories(configFile.getParent());
            
            // Build JSON
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Boolean> entry : ringSettings.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            
            // Write to file
            Files.write(configFile, json.toString(2).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get ring setting for icon resource name
     */
    public boolean getRing(String iconResName) {
        return ringSettings.getOrDefault(iconResName, false);
    }
    
    /**
     * Set ring setting for icon resource name
     */
    public void setRing(String iconResName, boolean enabled) {
        ringSettings.put(iconResName, enabled);
        save();
    }
    
    /**
     * Get all ring settings
     */
    public Map<String, Boolean> getAllSettings() {
        return new HashMap<>(ringSettings);
    }
}
