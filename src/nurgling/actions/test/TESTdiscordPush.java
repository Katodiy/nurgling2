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

    @Override
    public void body(NGameUI gui) throws InterruptedException {

    }
}

