package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;

public class MapSettings extends Panel {
    // Temporary settings structure
    private static class MapSettingsData {
        boolean showQuestGiverNames;
        boolean showThingwallNames;
        boolean showPartyMemberNames;
        boolean trackingVectors;
    }

    private final MapSettingsData tempSettings = new MapSettingsData();
    
    // Marker name checkboxes
    private CheckBox showQuestGiverNames;
    private CheckBox showThingwallNames;
    private CheckBox showPartyMemberNames;
    private CheckBox trackingVectors;
    
    private Scrollport scrollport;
    private Widget content;

    public MapSettings() {
        super("");
        int margin = UI.scale(10);

        // Create scrollport to contain all settings
        int scrollWidth = UI.scale(560);
        int scrollHeight = UI.scale(550);
        scrollport = add(new Scrollport(new Coord(scrollWidth, scrollHeight)), new Coord(margin, margin));

        // Create main content container
        content = new Widget(new Coord(scrollWidth - UI.scale(20), UI.scale(50))) {
            @Override
            public void pack() {
                resize(contentsz());
            }
        };
        scrollport.cont.add(content, Coord.z);

        int contentMargin = UI.scale(5);
        
        // Marker names section
        Widget prev = content.add(new Label("● Marker Name Display"), new Coord(contentMargin, contentMargin));
        prev = content.add(new Label("Control which marker names are displayed on the map"), prev.pos("bl").adds(0, 3));
        
        prev = showQuestGiverNames = content.add(new CheckBox("Show quest giver names (Bushes, Bumlings, Giant Toads)") {
            public void set(boolean val) {
                tempSettings.showQuestGiverNames = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 10));
        
        prev = showThingwallNames = content.add(new CheckBox("Show thingwall names") {
            public void set(boolean val) {
                tempSettings.showThingwallNames = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        prev = showPartyMemberNames = content.add(new CheckBox("Show party member names on minimap") {
            public void set(boolean val) {
                tempSettings.showPartyMemberNames = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        // Tracking vectors section
        prev = content.add(new Label("● Tracking Vectors"), prev.pos("bl").adds(0, 15));
        
        prev = trackingVectors = content.add(new CheckBox("Show tracking/dowsing vectors on map") {
            public void set(boolean val) {
                tempSettings.trackingVectors = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 10));
        
        // Pack content and update scrollbar
        content.pack();
        scrollport.cont.update();
        
        pack();
    }

    @Override
    public void load() {
        // Load marker name settings
        tempSettings.showQuestGiverNames = getBool(NConfig.Key.showQuestGiverNames);
        tempSettings.showThingwallNames = getBool(NConfig.Key.showThingwallNames);
        tempSettings.showPartyMemberNames = getBool(NConfig.Key.showPartyMemberNames);
        tempSettings.trackingVectors = getBool(NConfig.Key.trackingVectors);

        // Update UI components
        showQuestGiverNames.a = tempSettings.showQuestGiverNames;
        showThingwallNames.a = tempSettings.showThingwallNames;
        showPartyMemberNames.a = tempSettings.showPartyMemberNames;
        trackingVectors.a = tempSettings.trackingVectors;
    }

    @Override
    public void save() {
        // Save marker name settings
        NConfig.set(NConfig.Key.showQuestGiverNames, tempSettings.showQuestGiverNames);
        NConfig.set(NConfig.Key.showThingwallNames, tempSettings.showThingwallNames);
        NConfig.set(NConfig.Key.showPartyMemberNames, tempSettings.showPartyMemberNames);
        NConfig.set(NConfig.Key.trackingVectors, tempSettings.trackingVectors);
        NConfig.needUpdate();
    }
    
    private boolean getBool(NConfig.Key key) {
        Object val = NConfig.get(key);
        return val instanceof Boolean ? (Boolean) val : false;
    }
}



