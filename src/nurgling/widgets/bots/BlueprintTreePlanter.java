package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import org.json.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BlueprintTreePlanter extends Window implements Checkable {

    public enum State {
        NONE,
        PLACE_BLUEPRINT,
        CLEAR_BLUEPRINT,
        START_BOT,
        CANCELLED
    }

    private State currentState = State.NONE;
    private boolean isReady = false;
    private Dropbox<String> blueprintDropbox = null;
    private List<String> blueprintNames = new ArrayList<>();
    private String selectedBlueprint = null;

    public BlueprintTreePlanter() {
        super(new Coord(250, 200), L10n.get("blueprint.wnd_title"));
        
        loadBlueprintNames();
        
        Widget prev = add(new Label(L10n.get("blueprint.heading")), new Coord(UI.scale(10), UI.scale(10)));
        
        prev = add(new Label(L10n.get("blueprint.select")), prev.pos("bl").add(UI.scale(0, 10)));
        
        prev = add(blueprintDropbox = new Dropbox<String>(UI.scale(220), Math.min(10, blueprintNames.size()), UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return i < blueprintNames.size() ? blueprintNames.get(i) : "";
            }
            
            @Override
            protected int listitems() {
                return blueprintNames.size();
            }
            
            @Override
            protected void drawitem(GOut g, String item, int i) {
                if (item != null && !item.isEmpty()) {
                    g.text(item, Coord.z);
                } else {
                    g.text(L10n.get("blueprint.no_blueprints"), Coord.z);
                }
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                selectedBlueprint = item;
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        if (!blueprintNames.isEmpty()) {
            String current = getCurrentBlueprintName();
            if (current != null && blueprintNames.contains(current)) {
                blueprintDropbox.change(current);
            } else {
                blueprintDropbox.change(blueprintNames.get(0));
            }
        }
        
        prev = add(new Button(UI.scale(220), L10n.get("blueprint.place")) {
            @Override
            public void click() {
                super.click();
                if (selectedBlueprint == null || selectedBlueprint.isEmpty()) {
                    NUtils.getUI().msg(L10n.get("blueprint.select_first"));
                    return;
                }
                currentState = State.PLACE_BLUEPRINT;
                isReady = true;
            }
        }, prev.pos("bl").add(UI.scale(0, 15)));
        
        prev = add(new Button(UI.scale(220), L10n.get("blueprint.start")) {
            @Override
            public void click() {
                super.click();
                currentState = State.START_BOT;
                isReady = true;
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        prev = add(new Button(UI.scale(220), L10n.get("blueprint.clear")) {
            @Override
            public void click() {
                super.click();
                currentState = State.CLEAR_BLUEPRINT;
                isReady = true;
            }
        }, prev.pos("bl").add(UI.scale(0, 10)));
        
        pack();
    }
    
    private void loadBlueprintNames() {
        blueprintNames.clear();
        try {
            File configFile = new File("tree_garden_blueprints.json");
            if (configFile.exists()) {
                String content = new String(Files.readAllBytes(configFile.toPath()));
                JSONObject root = new JSONObject(content);
                JSONArray blueprintArray = root.getJSONArray("blueprints");
                
                for (int i = 0; i < blueprintArray.length(); i++) {
                    JSONObject bp = blueprintArray.getJSONObject(i);
                    String name = bp.getString("name");
                    blueprintNames.add(name);
                }
            }
        } catch (Exception e) {
            System.out.println("[BlueprintTreePlanter] Error loading blueprint names: " + e.getMessage());
        }
        
        if (blueprintNames.isEmpty()) {
            blueprintNames.add(L10n.get("blueprint.no_blueprints"));
        }
    }
    
    private String getCurrentBlueprintName() {
        try {
            File configFile = new File("tree_garden_blueprints.json");
            if (configFile.exists()) {
                String content = new String(Files.readAllBytes(configFile.toPath()));
                JSONObject root = new JSONObject(content);
                return root.optString("current", null);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    @Override
    public boolean check() {
        return isReady;
    }

    public State getState() {
        return currentState;
    }
    
    public String getSelectedBlueprint() {
        return selectedBlueprint;
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            currentState = State.CANCELLED;
            isReady = true;
            hide();
        }
        super.wdgmsg(msg, args);
    }
}
