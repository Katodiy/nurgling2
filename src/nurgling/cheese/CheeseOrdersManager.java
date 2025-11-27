package nurgling.cheese;

import nurgling.NConfig;
import nurgling.profiles.ConfigFactory;
import nurgling.profiles.ProfileAwareService;
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

public class CheeseOrdersManager implements ProfileAwareService {
    private final Map<Integer, CheeseOrder> orders = new HashMap<>();
    private String genus;
    private String configPath;

    public CheeseOrdersManager() {
        this.configPath = NConfig.getGlobalInstance().getCheeseOrdersPath();
        loadOrders();
    }

    /**
     * Constructor for profile-aware initialization
     */
    public CheeseOrdersManager(String genus) {
        this.genus = genus;
        initializeForProfile(genus);
    }

    // ProfileAwareService implementation

    @Override
    public void initializeForProfile(String genus) {
        this.genus = genus;
        NConfig config = ConfigFactory.getConfig(genus);
        this.configPath = config.getCheeseOrdersPath();
        load();
    }

    @Override
    public String getGenus() {
        return genus;
    }

    @Override
    public void load() {
        loadOrders();
    }

    @Override
    public void save() {
        writeOrders();
    }

    public void loadOrders() {
        orders.clear();
        File file = new File(configPath);
        if (file.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(configPath), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (IOException ignore) {}

            if (!contentBuilder.toString().isEmpty()) {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = main.getJSONArray("orders");
                for (int i = 0; i < array.length(); i++) {
                    CheeseOrder order = new CheeseOrder(array.getJSONObject(i));
                    orders.put(order.getId(), order);
                }
            }
        }
    }

    public void writeOrders() {
        JSONObject main = new JSONObject();
        JSONArray jorders = new JSONArray();
        for (CheeseOrder order : orders.values()) {
            jorders.put(order.toJson());
        }
        main.put("orders", jorders);

        try {
            FileWriter f = new FileWriter(configPath, StandardCharsets.UTF_8);
            main.write(f);
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addOrUpdateOrder(CheeseOrder order) {
        orders.put(order.getId(), order);
    }

    public void deleteOrder(int orderId) {
        orders.remove(orderId);
    }

    public Map<Integer, CheeseOrder> getOrders() {
        return orders;
    }
    
    public String getOrdersFilePath() {
        return configPath;
    }
}
