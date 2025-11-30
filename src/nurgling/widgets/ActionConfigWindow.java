package nurgling.widgets;

import haven.*;
import nurgling.routes.ForagerAction;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class ActionConfigWindow extends Window {
    
    private TextEntry objectPatternEntry;
    private Dropbox<ForagerAction.ActionType> actionTypeDropbox;
    private TextEntry actionNameEntry;
    private Label actionNameLabel;
    
    // Notification fields
    private Label notifyTargetLabel;
    private Dropbox<ForagerAction.NotifyTarget> notifyTargetDropbox;
    private Label chatChannelLabel;
    private TextEntry chatChannelEntry;
    
    private Button okButton;
    private Button cancelButton;
    private Consumer<ForagerAction> onSubmit;
    private boolean submitted = false;
    private ForagerAction editingAction = null;
    
    public ActionConfigWindow(Consumer<ForagerAction> onSubmit) {
        this(null, onSubmit);
    }
    
    public ActionConfigWindow(ForagerAction existingAction, Consumer<ForagerAction> onSubmit) {
        super(new Coord(UI.scale(400), UI.scale(320)), "Configure Action");
        this.onSubmit = onSubmit;
        this.editingAction = existingAction;
        
        Widget prev = add(new Label("Object Pattern:"));
        
        prev = add(objectPatternEntry = new TextEntry(UI.scale(380), 
            existingAction != null ? existingAction.targetObjectPattern : "") {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                if (ev.code == KeyEvent.VK_ENTER) {
                    submit();
                    return true;
                } else if (ev.code == KeyEvent.VK_ESCAPE) {
                    close();
                    return true;
                }
                return super.keydown(ev);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        prev = add(new Label("Action Type:"), prev.pos("bl").add(UI.scale(0, 10)));
        
        ForagerAction.ActionType[] actionTypes = ForagerAction.ActionType.values();
        prev = add(actionTypeDropbox = new Dropbox<ForagerAction.ActionType>(
            UI.scale(380), 
            actionTypes.length, 
            UI.scale(20)
        ) {
            @Override
            protected ForagerAction.ActionType listitem(int i) {
                return actionTypes[i];
            }
            
            @Override
            protected int listitems() {
                return actionTypes.length;
            }
            
            @Override
            protected void drawitem(GOut g, ForagerAction.ActionType item, int i) {
                g.text(item.name(), new Coord(5, 1));
            }
            
            @Override
            public void change(ForagerAction.ActionType item) {
                super.change(item);
                updateFieldsVisibility();
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        prev = add(actionNameLabel = new Label("Action Name (for FLOWER_ACTION):"), prev.pos("bl").add(UI.scale(0, 10)));
        
        prev = add(actionNameEntry = new TextEntry(UI.scale(380), 
            existingAction != null && existingAction.actionName != null ? existingAction.actionName : "") {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                if (ev.code == KeyEvent.VK_ENTER) {
                    submit();
                    return true;
                } else if (ev.code == KeyEvent.VK_ESCAPE) {
                    close();
                    return true;
                }
                return super.keydown(ev);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        // Notification target selection
        prev = add(notifyTargetLabel = new Label("Notify Target (for CHAT_NOTIFY):"), prev.pos("bl").add(UI.scale(0, 10)));
        
        ForagerAction.NotifyTarget[] notifyTargets = ForagerAction.NotifyTarget.values();
        prev = add(notifyTargetDropbox = new Dropbox<ForagerAction.NotifyTarget>(
            UI.scale(380), 
            notifyTargets.length, 
            UI.scale(20)
        ) {
            @Override
            protected ForagerAction.NotifyTarget listitem(int i) {
                return notifyTargets[i];
            }
            
            @Override
            protected int listitems() {
                return notifyTargets.length;
            }
            
            @Override
            protected void drawitem(GOut g, ForagerAction.NotifyTarget item, int i) {
                g.text(item.name(), new Coord(5, 1));
            }
            
            @Override
            public void change(ForagerAction.NotifyTarget item) {
                super.change(item);
                updateFieldsVisibility();
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        // Chat channel name
        prev = add(chatChannelLabel = new Label("Chat Channel Name (for CHAT notify):"), prev.pos("bl").add(UI.scale(0, 10)));
        
        prev = add(chatChannelEntry = new TextEntry(UI.scale(380), 
            existingAction != null && existingAction.chatChannelName != null ? existingAction.chatChannelName : "") {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                if (ev.code == KeyEvent.VK_ENTER) {
                    submit();
                    return true;
                } else if (ev.code == KeyEvent.VK_ESCAPE) {
                    close();
                    return true;
                }
                return super.keydown(ev);
            }
        }, prev.pos("bl").add(UI.scale(0, 5)));
        
        // Set initial state after all fields are initialized
        if (existingAction != null) {
            actionTypeDropbox.change(existingAction.actionType);
            if (existingAction.notifyTarget != null) {
                notifyTargetDropbox.change(existingAction.notifyTarget);
            } else {
                notifyTargetDropbox.change(notifyTargets[0]);
            }
        } else {
            actionTypeDropbox.change(actionTypes[0]);
            notifyTargetDropbox.change(notifyTargets[0]);
        }
        
        Widget buttonRow = add(new Widget(new Coord(UI.scale(380), UI.scale(30))), prev.pos("bl").add(UI.scale(0, 10)));
        
        okButton = buttonRow.add(new Button(UI.scale(80), "OK") {
            @Override
            public void click() {
                super.click();
                submit();
            }
        }, new Coord(UI.scale(100), 0));
        
        cancelButton = buttonRow.add(new Button(UI.scale(80), "Cancel") {
            @Override
            public void click() {
                super.click();
                close();
            }
        }, new Coord(UI.scale(200), 0));
        
        pack();
        
        setfocus(objectPatternEntry);
    }
    
    private void updateFieldsVisibility() {
        ForagerAction.ActionType selectedType = actionTypeDropbox.sel;
        
        // Show actionName only for FLOWER_ACTION
        boolean isFlowerAction = selectedType == ForagerAction.ActionType.FLOWER_ACTION;
        actionNameEntry.visible = isFlowerAction;
        actionNameLabel.visible = isFlowerAction;
        
        // Show notification fields only for CHAT_NOTIFY
        boolean isChatNotify = selectedType == ForagerAction.ActionType.CHAT_NOTIFY;
        notifyTargetDropbox.visible = isChatNotify;
        notifyTargetLabel.visible = isChatNotify;
        
        // Show chat channel only for CHAT_NOTIFY with CHAT target
        boolean showChatChannel = isChatNotify && notifyTargetDropbox.sel == ForagerAction.NotifyTarget.CHAT;
        chatChannelEntry.visible = showChatChannel;
        chatChannelLabel.visible = showChatChannel;
    }
    
    private void submit() {
        String pattern = objectPatternEntry.text().trim();
        if (pattern.isEmpty()) {
            return;
        }
        
        ForagerAction.ActionType type = actionTypeDropbox.sel;
        String actionName = null;
        ForagerAction.NotifyTarget notifyTarget = null;
        String chatChannel = null;
        
        // Get action name for FLOWER_ACTION
        if (type == ForagerAction.ActionType.FLOWER_ACTION) {
            actionName = actionNameEntry.text().trim();
            if (actionName.isEmpty()) {
                actionName = null;
            }
        }
        
        // Get notification settings for CHAT_NOTIFY
        if (type == ForagerAction.ActionType.CHAT_NOTIFY) {
            notifyTarget = notifyTargetDropbox.sel;
            if (notifyTarget == ForagerAction.NotifyTarget.CHAT) {
                chatChannel = chatChannelEntry.text().trim();
                if (chatChannel.isEmpty()) {
                    chatChannel = null;
                }
            }
        }
        
        ForagerAction action = new ForagerAction(pattern, type, actionName, notifyTarget, chatChannel);
        
        if (onSubmit != null) {
            submitted = true;
            onSubmit.accept(action);
        }
        close();
    }
    
    private void close() {
        if (!submitted && onSubmit != null) {
            onSubmit.accept(null);
        }
        hide();
        destroy();
    }
    
    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            close();
        } else {
            super.wdgmsg(msg, args);
        }
    }
}
