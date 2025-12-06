package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NBoughBeeProp;

public class BoughBee extends Window implements Checkable {

    Dropbox<String> onPlayerAction = null;
    Dropbox<String> onAnimalAction = null;
    Dropbox<String> afterHarvestAction = null;

    // Action options
    private static final String[] PLAYER_ACTIONS = {"nothing", "logout", "travel hearth"};
    private static final String[] ANIMAL_ACTIONS = {"logout", "travel hearth"};
    private static final String[] AFTER_HARVEST_ACTIONS = {"nothing", "logout", "travel hearth"};

    private Widget prev;
    
    public BoughBee() {
        super(new Coord(250, 200), "Beehive Smoker");
        NBoughBeeProp startprop = NBoughBeeProp.get(NUtils.getUI().sessInfo);
        if (startprop == null) startprop = new NBoughBeeProp("", "");
        final NBoughBeeProp finalStartprop = startprop;
        
        prev = add(new Label("Beehive Smoker Settings:"));
        
        // Player detection reaction
        prev = add(new Label("On unknown player:"), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(onPlayerAction = new Dropbox<String>(UI.scale(150), PLAYER_ACTIONS.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return PLAYER_ACTIONS[i];
            }

            @Override
            protected int listitems() {
                return PLAYER_ACTIONS.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        // Set initial value for player action
        for (int i = 0; i < PLAYER_ACTIONS.length; i++) {
            if (PLAYER_ACTIONS[i].equals(finalStartprop.onPlayerAction)) {
                onPlayerAction.change(PLAYER_ACTIONS[i]);
                break;
            }
        }

        // Animal detection reaction
        prev = add(new Label("On dangerous animal:"), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(onAnimalAction = new Dropbox<String>(UI.scale(150), ANIMAL_ACTIONS.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return ANIMAL_ACTIONS[i];
            }

            @Override
            protected int listitems() {
                return ANIMAL_ACTIONS.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        // Set initial value for animal action
        for (int i = 0; i < ANIMAL_ACTIONS.length; i++) {
            if (ANIMAL_ACTIONS[i].equals(finalStartprop.onAnimalAction)) {
                onAnimalAction.change(ANIMAL_ACTIONS[i]);
                break;
            }
        }

        // After harvest action
        prev = add(new Label("After harvest complete:"), prev.pos("bl").add(UI.scale(0, 10)));
        prev = add(afterHarvestAction = new Dropbox<String>(UI.scale(150), AFTER_HARVEST_ACTIONS.length, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return AFTER_HARVEST_ACTIONS[i];
            }

            @Override
            protected int listitems() {
                return AFTER_HARVEST_ACTIONS.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        // Set initial value for after harvest action
        for (int i = 0; i < AFTER_HARVEST_ACTIONS.length; i++) {
            if (AFTER_HARVEST_ACTIONS[i].equals(finalStartprop.afterHarvestAction)) {
                afterHarvestAction.change(AFTER_HARVEST_ACTIONS[i]);
                break;
            }
        }

        // Start button
        prev = add(new Button(UI.scale(150), "Start") {
            @Override
            public void click() {
                super.click();
                prop = NBoughBeeProp.get(NUtils.getUI().sessInfo);
                
                if (prop != null) {
                    if (onPlayerAction.sel != null)
                        prop.onPlayerAction = onPlayerAction.sel;
                    else
                        prop.onPlayerAction = "nothing";
                        
                    if (onAnimalAction.sel != null)
                        prop.onAnimalAction = onAnimalAction.sel;
                    else
                        prop.onAnimalAction = "logout";
                        
                    if (afterHarvestAction.sel != null)
                        prop.afterHarvestAction = afterHarvestAction.sel;
                    else
                        prop.afterHarvestAction = "nothing";
                        
                    NBoughBeeProp.set(prop);
                }
                isReady = true;
            }
        }, prev.pos("bl").add(UI.scale(0, 10)));
        
        pack();
    }

    @Override
    public boolean check() {
        return isReady;
    }

    boolean isReady = false;

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            isReady = true;
            hide();
        }
        super.wdgmsg(msg, args);
    }
    
    public NBoughBeeProp prop = null;
}
