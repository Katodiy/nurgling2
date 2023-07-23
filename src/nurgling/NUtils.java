package nurgling;

import haven.*;

import java.text.*;
import java.util.*;

public class NUtils
{
    public static long getTickId()
    {
        if(GameUI.getInstance()!= null )
            return  ((NUI)GameUI.getInstance().ui).tickId;
        return -1;
    }

    public static NGameUI getGameUI(){
        return (NGameUI) GameUI.getInstance();
    }

    public static NUI getUI(){
        assert (GameUI.getInstance()!=null);
        return (NUI)GameUI.getInstance().ui;
    }

    public static String timestamp() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }

    public static String timestamp(String text) {
        return String.format("[%s] %s", timestamp(), text);
    }
}
