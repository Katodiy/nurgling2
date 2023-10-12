package nurgling.actions;

import nurgling.NUtils;

public class Results {
    String msg;
    boolean isSuccess = false;
    public static Results SUCCESS()
    {
        Results res =new Results(null);
        res.isSuccess = true;
        return res;
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

    public static Results FAIL()
    {
        Results res =new Results(null);
        res.isSuccess = false;
        return res;
    }

    public boolean IsSuccess()
    {
        return isSuccess;
    }
}