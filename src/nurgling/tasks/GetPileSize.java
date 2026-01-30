package nurgling.tasks;

import nurgling.*;

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

    public int getCount(){
        return result;
    }
}
