package mapv4;

import haven.Coord;
import haven.MCache;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.NTask;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Connector implements Action {
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 1000;
    
    NMappingClient parent;
    public final BlockingQueue<JSONObject> msgs = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private long lastErrorTime = 0;
    private int consecutiveErrors = 0;
    
    public Connector(NMappingClient parent) {
        this.parent = parent;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (!parent.done.get()) {
            JSONObject msg = msgs.poll(1, TimeUnit.SECONDS);
            if(msg != null) {
                String header = (String) msg.get("header");
                // Optional features - try once, don't disable automapper on failure
                if (header.equals("OVERLAY")) {
                    sendOptionalMsg(msg);
                } else {
                    sendMsgWithRetry(msg);
                }
            }
        }
        return Results.SUCCESS();
    }

    private void sendOptionalMsg(JSONObject msg) {
        try {
            String urlString = (String) msg.get("url");
            final HttpURLConnection connection =
                    (HttpURLConnection) URI.create(urlString).toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setDoOutput(true);
            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                final String json = msg.get("data").toString();
                out.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int respCode = connection.getResponseCode();
            connection.disconnect();

            // 404 means endpoint not supported - disable for session
            if (respCode == 404) {
                parent.setOverlayUnsupported();
            }
        } catch (Exception ignored) {
            // Network error - silently ignore, will retry on next grid
        }
    }



    private void sendMsgWithRetry(JSONObject msg) {
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                boolean success = sendMsg(msg);
                if (success) {
                    consecutiveErrors = 0;
                    NUtils.setAutoMapperState(true);
                    return;
                }
            } catch (Exception e) {
                handleError(e, attempt);
            }
            
            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                try {
                    long delay = INITIAL_RETRY_DELAY_MS * (1L << attempt);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        
        NUtils.setAutoMapperState(false);
    }
    
    private boolean sendMsg(JSONObject msg) throws IOException {
        int respCode = -1;
        if(((String)msg.get("reqMethod")).equals("MULTI"))
        {
            JSONObject extraData = new JSONObject();
            MultipartUtility multipart = new MultipartUtility((String) msg.get("url"), "utf-8");
            multipart.addFormField("id", (String) ((JSONObject)msg.get("data")).get("gridID"));
            multipart.addFilePart("file", (InputStream) ((JSONObject)msg.get("data")).get("inputStream"), "minimap.png");
            extraData.put("season", NUtils.getGameUI().map.glob.ast.is);
            multipart.addFormField("extraData", extraData.toString());
            MultipartUtility.Response response = multipart.finish();
            respCode = response.statusCode;
        }
        else {
            String urlString = (String) msg.get("url");
            final HttpURLConnection connection =
                    (HttpURLConnection) URI.create(urlString).toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            
            String reqMethod = (String) msg.get("reqMethod");
            String header = (String) msg.get("header");
            
            if (reqMethod.equals("POST")) {
                connection.setRequestMethod(reqMethod);
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                connection.setDoOutput(true);
                final String json = msg.get("data").toString();
                
                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                connection.setRequestMethod("GET");
            }
            
            respCode = connection.getResponseCode();
            if (respCode == 200) {
                if (header.equals("GRIDREQ")) {
                    try (DataInputStream dio = new DataInputStream(connection.getInputStream())) {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] data = new byte[1024];
                        int nRead;
                        while ((nRead = dio.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        String response = buffer.toString(StandardCharsets.UTF_8.name());
                        JSONObject jo = new JSONObject(response);
                        JSONArray reqs = jo.optJSONArray("gridRequests");
                        Long gridId = Long.valueOf(((String[][]) ((JSONObject) msg.get("data")).get("grids"))[1][1]);
                        NMappingClient.MapRef mapRef = new NMappingClient.MapRef(
                            jo.getLong("map"), 
                            new Coord(jo.getJSONObject("coords").getInt("x"), jo.getJSONObject("coords").getInt("y")));
                        parent.cache.put(gridId, new NMappingClient.CacheEntry(mapRef));
                        for (int i = 0; reqs != null && i < reqs.length(); i++) {
                            MCache.Grid g = NUtils.getGameUI().map.glob.map.findGrid(Long.valueOf(reqs.getString(i)));
                            if (g != null) {
                                parent.requestor.prepGrid(reqs.getString(i), g);
                            }
                        }

                        // Trigger overlay upload for all loaded grids (3x3 around player)
                        if ((Boolean) NConfig.get(NConfig.Key.sendOverlays) && parent.isOverlaySupported()) {
                            String[][] allGridIds = (String[][]) ((JSONObject) msg.get("data")).get("grids");
                            for (int row = 0; row < 3; row++) {
                                for (int col = 0; col < 3; col++) {
                                    try {
                                        Long gid = Long.valueOf(allGridIds[row][col]);
                                        MCache.Grid g = NUtils.getGameUI().map.glob.map.findGrid(gid);
                                        if (g != null && g.ols != null && g.ols.length > 0) {
                                            parent.requestor.sendOverlayUpdate(gid, g);
                                        }
                                    } catch (NumberFormatException ignored) {
                                        // Invalid grid ID, skip
                                    }
                                }
                            }
                        }
                    }
                } else if (header.equals("LOCATE")) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String resp = reader.lines().collect(Collectors.joining());
                        String[] parts = resp.split(";");
                        if (parts.length == 3) {
                            // TODO: implement location handling
                        }
                    }
                }
            }
            connection.disconnect();
        }
        return respCode == 200;
    }
    
    private void handleError(Exception e, int attempt) {
        consecutiveErrors++;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastErrorTime > 60000) {
            consecutiveErrors = 1;
        }
        lastErrorTime = currentTime;
        
        if (consecutiveErrors <= 3) {
            String errorMsg = "Map Server error (attempt " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS + ")";
            if (e instanceof SocketTimeoutException) {
                errorMsg += ": timeout";
            } else if (e instanceof IOException) {
                errorMsg += ": connection failed";
            }
            NUtils.getGameUI().error(errorMsg);
        }
    }
}
