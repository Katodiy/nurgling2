package nurgling.actions.test;

import nurgling.NGameUI;
import nurgling.actions.Results;
import nurgling.notifications.DiscordHookWrapper;

public class TESTdiscordPush extends Test{

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        DiscordHookWrapper.Push("general","This is test notification from Nurgling2");
        return Results.SUCCESS();
    }

    public TESTdiscordPush(){

    }
    @Override
    public void body(NGameUI gui) throws InterruptedException {

    }
}

