package nurgling.actions.test;

import nurgling.*;
import nurgling.actions.Results;
import nurgling.conf.*;

public class TESTdiscordPush extends Test{
    public String url;
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        NUtils.getGameUI().msgToDiscord(NDiscordNotification.get("general"),"This is test notification from Nurgling2");
        return Results.SUCCESS();
    }

    public TESTdiscordPush(){
        url = "https://discord.com/api/webhooks/1190053666640646286/xRZ5Wgwp_ZOkavHvfKrhsMBM-I8nu3LT0ix2RO8_QBQHcMHGQz_dSNwM86WpuLsRw_2U";
        NDiscordNotification.set( new NDiscordNotification("general", url, "Captain",
                ""));
        num = 1;

    }
    @Override
    public void body(NGameUI gui) throws InterruptedException {

    }
}

