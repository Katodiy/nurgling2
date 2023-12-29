package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class NDiscordNotification implements JConf{

    public String settingsName = "general";
    public String webhookUrl = "";
    public String webhookUsername = "";
    public String webhookIcon = "";

    public NDiscordNotification(String settingsName, String discordWebhookUrl, String WebhookUsername, String discordWebhookIcon ){
        this.settingsName = settingsName;
        this.webhookUrl = discordWebhookUrl;
        this.webhookUsername = WebhookUsername;
        this.webhookIcon = discordWebhookIcon;
    }

    @Override
    public String toString()
    {
        return "NDiscordNotification[" + settingsName + "," + webhookUrl + "," + webhookUsername + "," + webhookIcon + "]";
    }


    public NDiscordNotification(HashMap<String, Object> values) {
        if(values.get("settingsName") != null)
            settingsName = (String) values.get("settingsName");
        if (values.get("webhookUrl") != null)
            webhookUrl = (String) values.get("webhookUrl");
        if (values.get("webhookUsername") != null)
            webhookUsername = (String) values.get("webhookUsername");
        if (values.get("webhookIcon") != null)
            webhookIcon = (String) values.get("webhookIcon");
    }

    @Override
    public JSONObject toJson() {
        JSONObject jdiscord = new JSONObject();
        jdiscord.put("type", "NDiscordNotification");
        jdiscord.put("settingsName", settingsName);
        jdiscord.put("webhookUrl", webhookUrl);
        jdiscord.put("webhookUsername", webhookUsername);
        jdiscord.put("webhookIcon", webhookIcon);
        return jdiscord;
    }

    public static void set(NDiscordNotification prop)
    {
        ArrayList<NDiscordNotification> discordProps = ((ArrayList<NDiscordNotification>) NConfig.get(NConfig.Key.discordNotification));
        if (discordProps != null)
        {
            for (Iterator<NDiscordNotification> i = discordProps.iterator(); i.hasNext(); )
            {
                NDiscordNotification oldprop = i.next();
                if (oldprop.settingsName.equals(prop.settingsName))
                {
                    i.remove();
                    break;
                }
            }

        }
        else
        {
            discordProps = new ArrayList<>();
        }
        discordProps.add(prop);
        NConfig.set(NConfig.Key.discordNotification, discordProps);
    }

    public static NDiscordNotification get(String botKey)
    {
        ArrayList<NDiscordNotification> discordProps = ((ArrayList<NDiscordNotification>) NConfig.get(NConfig.Key.discordNotification));
        if (discordProps == null)
            discordProps = new ArrayList<>();
        for (NDiscordNotification prop : discordProps)
        {
            if (prop.settingsName.equals(botKey))
            {
                return prop;
            }
        }
        return null;
    }
}
