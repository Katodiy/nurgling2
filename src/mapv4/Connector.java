package mapv4;

import haven.Coord;
import haven.MCache;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.NTask;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Connector implements Action {
    NMappingClient parent;
    final LinkedList<JSONObject> msgs = new LinkedList<>();
    public Connector(NMappingClient parent) {
        this.parent = parent;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (!parent.done.get()) {
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    synchronized (msgs) {
                        return !msgs.isEmpty() || parent.done.get();
                    }
                }
            });
            if(parent.done.get())
                return Results.SUCCESS();
            JSONObject msg;
            synchronized ( msgs ) {
                msg = msgs.poll();
            }
            sendMsg(msg);
        }
        return Results.SUCCESS();
    }



    private void sendMsg(JSONObject msg) {
        try {
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
                final HttpURLConnection connection =
                        (HttpURLConnection) new URL((String) msg.get("url")).openConnection();
                String reqMethod = (String) msg.get("reqMethod");
                if (reqMethod.equals("POST")) {
                    connection.setRequestMethod(reqMethod);
                    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connection.setDoOutput(true);
                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    final String json = msg.get("data").toString();
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                } else {
                    connection.setRequestMethod("GET");
                }
                respCode = connection.getResponseCode();
                if (respCode == 200) {
                    if (((String) msg.get("header")).equals("GRIDREQ")) {
                        DataInputStream dio = new DataInputStream(connection.getInputStream());
                        int nRead;
                        byte[] data = new byte[1024];
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        while ((nRead = dio.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                        String response = buffer.toString(StandardCharsets.UTF_8.name());
                        JSONObject jo = new JSONObject(response);
                        JSONArray reqs = jo.optJSONArray("gridRequests");
                        synchronized (parent.cache) {
                            parent.cache.put(Long.valueOf(((String[][]) ((JSONObject) msg.get("data")).get("grids"))[1][1]), new NMappingClient.MapRef(jo.getLong("map"), new Coord(jo.getJSONObject("coords").getInt("x"), jo.getJSONObject("coords").getInt("y"))));
                        }
                        for (int i = 0; reqs != null && i < reqs.length(); i++) {
                            MCache.Grid g = NUtils.getGameUI().map.glob.map.findGrid(Long.valueOf(reqs.getString(i)));
                            if (g != null) {
                                parent.requestor.prepGrid(reqs.getString(i), g);
                            }
                        }
                    } else if (((String) msg.get("header")).equals("LOCATE")) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                            String resp = reader.lines().collect(Collectors.joining());
                            String[] parts = resp.split(";");
                            if (parts.length == 3) {
                                /// TODO
                            }
                        }

                    }
                }
            }
            if (respCode == 200) {
                NUtils.setAutoMapperState(true);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
