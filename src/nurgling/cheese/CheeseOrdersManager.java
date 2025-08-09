package nurgling.cheese;

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

public class CheeseOrdersManager {
    private final Map<Integer, CheeseOrder> orders = new HashMap<>();

    public CheeseOrdersManager() {
        loadOrders();
    }

    public void loadOrders() {
        orders.clear();
        File file = new File(NConfig.current.path_cheese_orders);
        if (file.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_cheese_orders), StandardCharsets.UTF_8)) {
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
            FileWriter f = new FileWriter(NConfig.current.path_cheese_orders, StandardCharsets.UTF_8);
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
}
