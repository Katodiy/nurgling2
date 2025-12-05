package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.conf.NDiscordNotification;

public class DiscordSettings extends Panel {
    
    private TextEntry webhookUrlEntry;
    private TextEntry webhookUsernameEntry;
    private TextEntry webhookIconEntry;
    private Button testButton;
    
    public DiscordSettings() {
        super("Discord Notifications");
        
        int y = UI.scale(40);
        
        add(new Label("Configure Discord webhook for bot notifications"), UI.scale(10), y);
        y += UI.scale(30);
        
        add(new Label("Webhook URL:"), UI.scale(10), y);
        y += UI.scale(20);
        webhookUrlEntry = add(new TextEntry(UI.scale(560), ""), UI.scale(10), y);
        y += UI.scale(30);
        
        add(new Label("Webhook Username (optional):"), UI.scale(10), y);
        y += UI.scale(20);
        webhookUsernameEntry = add(new TextEntry(UI.scale(560), ""), UI.scale(10), y);
        y += UI.scale(30);
        
        add(new Label("Webhook Icon URL (optional):"), UI.scale(10), y);
        y += UI.scale(20);
        webhookIconEntry = add(new TextEntry(UI.scale(560), ""), UI.scale(10), y);
        y += UI.scale(30);
        
        testButton = add(new Button(UI.scale(150), "Test Notification") {
            @Override
            public void click() {
                testNotification();
            }
        }, UI.scale(10), y);
        
        pack();
    }
    
    private void testNotification() {
        String webhookUrl = webhookUrlEntry.text().trim();
        
        if (webhookUrl.isEmpty()) {
            ui.gui.error("Please enter webhook URL first!");
            return;
        }
        
        NDiscordNotification settings = new NDiscordNotification(
            "general",
            webhookUrl,
            webhookUsernameEntry.text().trim(),
            webhookIconEntry.text().trim()
        );
        
        ui.gui.msgToDiscord(settings, "Test notification from Nurgling2 - Discord integration is working!");
        ui.gui.msg("Test notification sent to Discord");
    }
    
    @Override
    public void load() {
        NDiscordNotification settings = NDiscordNotification.get("general");
        
        if (settings != null) {
            webhookUrlEntry.settext(settings.webhookUrl != null ? settings.webhookUrl : "");
            webhookUsernameEntry.settext(settings.webhookUsername != null ? settings.webhookUsername : "");
            webhookIconEntry.settext(settings.webhookIcon != null ? settings.webhookIcon : "");
        } else {
            webhookUrlEntry.settext("");
            webhookUsernameEntry.settext("");
            webhookIconEntry.settext("");
        }
    }
    
    @Override
    public void save() {
        String webhookUrl = webhookUrlEntry.text().trim();
        String webhookUsername = webhookUsernameEntry.text().trim();
        String webhookIcon = webhookIconEntry.text().trim();
        
        NDiscordNotification settings = new NDiscordNotification(
            "general",
            webhookUrl,
            webhookUsername,
            webhookIcon
        );
        
        NDiscordNotification.set(settings);
        NConfig.needUpdate();
        
        ui.gui.msg("Discord settings saved");
    }
}
