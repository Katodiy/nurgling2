package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NAutoFlowerActionProp;

import java.util.ArrayList;
import java.util.List;

public class AutoFlowerAction extends Window implements Checkable {

    public String action = null;
    CheckBox transfer = null;
    Dropbox<String> actionDropbox = null;
    TextEntry customActionInput = null;
    List<String> actionList = new ArrayList<>();

    public AutoFlowerAction() {
        super(new Coord(300, 200), "Auto Flower Action");
        NAutoFlowerActionProp startprop = NAutoFlowerActionProp.get(NUtils.getUI().sessInfo);
        if (startprop == null) startprop = new NAutoFlowerActionProp("", "");
        final NAutoFlowerActionProp finalStartprop = startprop;
        
        actionList = new ArrayList<>(finalStartprop.actionHistory);
        if (!actionList.contains("")) {
            actionList.add(0, "");
        }
        
        prev = add(new Label("Auto Flower Action Settings:"));
        
        prev = add(new Label("Enter custom:"), prev.pos("bl").add(UI.scale(0, 5)));
        
        prev = add(customActionInput = new TextEntry(UI.scale(200), finalStartprop.action) {
            @Override
            protected void changed() {
                super.changed();
                action = text();
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        prev = add(new Label("Or select from recent:"), prev.pos("bl").add(UI.scale(0, 10)));
        
        prev = add(actionDropbox = new Dropbox<String>(UI.scale(200), Math.min(10, actionList.size()), UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return i < actionList.size() ? actionList.get(i) : "";
            }
            
            @Override
            protected int listitems() {
                return actionList.size();
            }
            
            @Override
            protected void drawitem(GOut g, String item, int i) {
                if (item != null && !item.isEmpty()) {
                    g.text(item, Coord.z);
                } else {
                    g.text("<empty>", Coord.z);
                }
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                if (item != null && !item.isEmpty() && customActionInput != null) {
                    customActionInput.settext(item);
                    action = item;
                }
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        if (!finalStartprop.action.isEmpty() && actionList.contains(finalStartprop.action)) {
            actionDropbox.change(finalStartprop.action);
        } else if (!actionList.isEmpty()) {
            actionDropbox.change(actionList.get(0));
        }
        
        prev = add(transfer = new CheckBox("Transfer items") {
            {
                a = finalStartprop.transfer;
            }
            @Override
            public void set(boolean a) {
                super.set(a);
            }
        }, prev.pos("bl").add(UI.scale(0, 10)));

        prev = add(new Button(UI.scale(150), "Start") {
            @Override
            public void click() {
                super.click();
                prop = NAutoFlowerActionProp.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    String selectedAction = customActionInput.text();
                    if (selectedAction == null || selectedAction.trim().isEmpty()) {
                        selectedAction = actionDropbox.sel;
                    }
                    prop.action = selectedAction;
                    prop.transfer = transfer.a;
                    prop.addToHistory(selectedAction);
                    NAutoFlowerActionProp.set(prop);
                }
                isReady = true;
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
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
    
    public NAutoFlowerActionProp prop = null;
}
