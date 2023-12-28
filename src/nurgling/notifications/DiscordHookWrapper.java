package nurgling.notifications;

import nurgling.NUtils;
import nurgling.actions.Results;
import nurgling.conf.NDiscordNotification;
import java.io.IOException;

public class DiscordHookWrapper {
    public void DiscordWebhookWrap(){}
    private static String _discordWebhookUrl = "";
    private static String _discordWebhookUsername = "";
    private static String _discordWebhookIcon = "";
    public static void Push(String bot, String message){
        if(bot.length() == 0){
            throw new RuntimeException("Non existent bot reference");
        }

        if(NDiscordNotification.get(bot) == null){
            throw new NullPointerException("Discord settings bot (" + bot + ") do not exist.");
        }

        _discordWebhookUrl = NDiscordNotification.get(bot).discordWebhookUrl;
        if(_discordWebhookUrl.length() == 0){
            _discordWebhookUrl = "";
        }

        _discordWebhookUsername = NDiscordNotification.get(bot).discordWebhookUsername;
        if(_discordWebhookUsername.length() == 0){
            _discordWebhookUsername = "Default Username";
        }

        _discordWebhookIcon = NDiscordNotification.get(bot).discordWebhookIcon;
        if(_discordWebhookIcon.length() == 0){
            _discordWebhookIcon = "https://raw.githubusercontent.com/Katodiy/nurgling/master/etc/icon.png";
        }

        nurgling.notifications.DiscordHookObject webhook = new nurgling.notifications.DiscordHookObject(_discordWebhookUrl);
        if(message != null && message.length() > 0){
            webhook.setContent(message);
        }else{
            webhook.setContent("Test message. Gimme some real message.");
        }
        webhook.setAvatarUrl(_discordWebhookIcon);
        webhook.setUsername(_discordWebhookUsername);
        webhook.addEmbed(new nurgling.notifications.DiscordHookObject.EmbedObject()
                .setColor(java.awt.Color.RED)
                .setThumbnail(_discordWebhookIcon)
                .setAuthor("Nurgling2", "https://github.com/Katodiy/nurgling", "https://raw.githubusercontent.com/Katodiy/nurgling/master/etc/icon.png")
                .setUrl("https://github.com/Katodiy/nurgling2"));
        try {
            webhook.execute();
        } catch (IOException e) {
            //OIException - thrown when getResponseCode != 200
            throw new RuntimeException(e);
        }
    }
}
