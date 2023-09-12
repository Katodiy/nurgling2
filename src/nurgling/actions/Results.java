package nurgling.actions;

import nurgling.NUtils;

public class Results {
    public static Results SUCCESS()
    {
        return new Results(null);
    }

    public static Results ERROR(String msg)
    {
        return new Results(msg);
    }

    private Results(String msg) {
        if(msg!=null)
            NUtils.getGameUI ().error( msg );
    }

}