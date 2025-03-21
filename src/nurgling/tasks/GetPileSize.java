package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.*;

public class GetPileSize extends NTask
{
    NISBox pile;


    public GetPileSize(NISBox pile)
    {
        this.pile = pile;
    }


    @Override
    public boolean check()
    {
        return ((result = pile.calcCount())!=-1);
    }

    private int result = -1;

    public int getResult(){
        return result;
    }
}
