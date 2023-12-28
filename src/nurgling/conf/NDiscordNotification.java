package nurgling.conf;

import nurgling.NConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class NDiscordNotification implements JConf{

    public static String discordBot = "general";
    public static String discordWebhookUrl = "";
    public static String discordWebhookUsername = "";
    public static String discordWebhookIcon = "";

    public NDiscordNotification(String discordBot, String discordWebhookUrl, String discordWebhookUsername, String discordWebhookIcon ){
        this.discordBot = discordBot;
        this.discordWebhookUrl = discordWebhookUrl;
        this.discordWebhookUsername = discordWebhookUsername;
        this.discordWebhookIcon = discordWebhookIcon;
    }

    public NDiscordNotification(){

    }

    @Override
    public String toString()
    {
        return "NDiscordNotification[" + discordBot + "," + discordWebhookUrl + "," + discordWebhookUsername + "," + discordWebhookIcon + "]";
    }


    public NDiscordNotification(HashMap<String, Object> values) {
        if(values.get("discordBot") != null)
            discordBot = (String) values.get("discordBot");
        if (values.get("discordWebhookUrl") != null)
            discordWebhookUrl = (String) values.get("discordWebhookUrl");
        if (values.get("discordWebhookUsername") != null)
            discordWebhookUsername = (String) values.get("discordWebhookUsername");
        if (values.get("discordWebhookIcon") != null)
            discordWebhookIcon = (String) values.get("discordWebhookIcon");
    }

    @Override
    public JSONObject toJson() {
        JSONObject jdiscord = new JSONObject();
        jdiscord.put("type", "NDiscordNotification");
        jdiscord.put("discordBot", discordBot);
        jdiscord.put("discordWebhookUrl", discordWebhookUrl);
        jdiscord.put("discordWebhookUsername", discordWebhookUsername);
        jdiscord.put("discordWebhookIcon", discordWebhookIcon);
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
                if (oldprop.discordBot == prop.discordBot)
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
            if (prop.discordBot.equals(botKey))
            {
                return prop;
            }
        }
        return null;
    }
}
