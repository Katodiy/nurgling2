package nurgling.actions;

import nurgling.*;

public interface Action
{
    Results run ( NGameUI gui )
            throws InterruptedException;
}
