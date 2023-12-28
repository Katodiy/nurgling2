package nurgling.actions.test;

import nurgling.NGameUI;
import nurgling.actions.Results;
import nurgling.conf.NDiscordNotification;
import nurgling.notifications.DiscordHookWrapper;

public class TESTdiscordSave extends Test{

    public String url;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        return Results.SUCCESS();
    }

    public TESTdiscordSave(){
        url = "your_webhook_here";
        NDiscordNotification.set( new NDiscordNotification("general", url, "Usernm",
                "https://raw.githubusercontent.com/Katodiy/nurgling/master/resources/src/bots/icons/build.res/image/image_0.png"));
    }
    @Override
    public void body(NGameUI gui) throws InterruptedException {

    }
}

