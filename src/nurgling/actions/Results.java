package nurgling.actions;

import nurgling.NUtils;

public class Results {
    String msg;
    public static Results SUCCESS()
    {
        return new Results(null);
    }

    public static Results ERROR(String msg)
    {
        return new Results(msg);
    }

    private Results(String msg) {
        this.msg = msg;
        if(msg!=null)
            NUtils.getGameUI ().error( msg );
    }

    public boolean equal(Results l)
    {
        return msg == null || l.msg.equals(msg);
    }
}